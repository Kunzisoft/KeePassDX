/* fipsdrv.c  -  A driver to help with FIPS CAVS tests.
   Copyright (C) 2008 Free Software Foundation, Inc.

   This file is part of Libgcrypt.
  
   Libgcrypt is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
  
   Libgcrypt is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.
  
   You should have received a copy of the GNU Lesser General Public
   License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <errno.h>
#include <ctype.h>
#ifdef HAVE_W32_SYSTEM
# include <fcntl.h> /* We need setmode().  */
#else
# include <signal.h>
#endif
#include <assert.h>
#include <unistd.h>

#ifdef _GCRYPT_IN_LIBGCRYPT
# include "../src/gcrypt.h"
#else
# include <gcrypt.h>
# define PACKAGE_BUGREPORT "devnull@example.org"
# define PACKAGE_VERSION "[build on " __DATE__ " " __TIME__ "]"
#endif


#define PGM "fipsdrv"

#define my_isascii(c) (!((c) & 0x80))
#define digitp(p)   (*(p) >= '0' && *(p) <= '9')
#define hexdigitp(a) (digitp (a)                     \
                      || (*(a) >= 'A' && *(a) <= 'F')  \
                      || (*(a) >= 'a' && *(a) <= 'f'))
#define xtoi_1(p)   (*(p) <= '9'? (*(p)- '0'): \
                     *(p) <= 'F'? (*(p)-'A'+10):(*(p)-'a'+10))
#define xtoi_2(p)   ((xtoi_1(p) * 16) + xtoi_1((p)+1))
#define DIM(v)               (sizeof(v)/sizeof((v)[0]))
#define DIMof(type,member)   DIM(((type *)0)->member)


#define PRIV_CTL_INIT_EXTRNG_TEST   58
#define PRIV_CTL_RUN_EXTRNG_TEST    59
#define PRIV_CTL_DEINIT_EXTRNG_TEST 60
#define PRIV_CTL_DISABLE_WEAK_KEY   61
#define PRIV_CTL_GET_INPUT_VECTOR   62


/* Verbose mode flag.  */
static int verbose;

/* Binary input flag.  */
static int binary_input;

/* Binary output flag.  */
static int binary_output;

/* Base64 output flag.  */
static int base64_output;

/* We need to know whether we are in loop_mode.  */
static int loop_mode;

/* If true some functions are modified to print the output in the CAVS
   response file format.  */
static int standalone_mode;


/* ASN.1 classes.  */
enum
{
  UNIVERSAL = 0,
  APPLICATION = 1,
  ASNCONTEXT = 2,
  PRIVATE = 3
};


/* ASN.1 tags.  */
enum
{
  TAG_NONE = 0,
  TAG_BOOLEAN = 1,
  TAG_INTEGER = 2,
  TAG_BIT_STRING = 3,
  TAG_OCTET_STRING = 4,
  TAG_NULL = 5,
  TAG_OBJECT_ID = 6,
  TAG_OBJECT_DESCRIPTOR = 7,
  TAG_EXTERNAL = 8,
  TAG_REAL = 9,
  TAG_ENUMERATED = 10,
  TAG_EMBEDDED_PDV = 11,
  TAG_UTF8_STRING = 12,
  TAG_REALTIVE_OID = 13,
  TAG_SEQUENCE = 16,
  TAG_SET = 17,
  TAG_NUMERIC_STRING = 18,
  TAG_PRINTABLE_STRING = 19,
  TAG_TELETEX_STRING = 20,
  TAG_VIDEOTEX_STRING = 21,
  TAG_IA5_STRING = 22,
  TAG_UTC_TIME = 23,
  TAG_GENERALIZED_TIME = 24,
  TAG_GRAPHIC_STRING = 25,
  TAG_VISIBLE_STRING = 26,
  TAG_GENERAL_STRING = 27,
  TAG_UNIVERSAL_STRING = 28,
  TAG_CHARACTER_STRING = 29,
  TAG_BMP_STRING = 30
};

/* ASN.1 Parser object.  */
struct tag_info 
{
  int class;             /* Object class.  */
  unsigned long tag;     /* The tag of the object.  */
  unsigned long length;  /* Length of the values.  */
  int nhdr;              /* Length of the header (TL).  */
  unsigned int ndef:1;   /* The object has an indefinite length.  */
  unsigned int cons:1;   /* This is a constructed object.  */ 
};



/* Print a error message and exit the process with an error code.  */
static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  fputs (PGM ": ", stderr);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}


static void
showhex (const char *prefix, const void *buffer, size_t length)
{
  const unsigned char *p = buffer;

  if (prefix)
    fprintf (stderr, PGM ": %s: ", prefix);
  while (length-- )
    fprintf (stderr, "%02X", *p++);
  if (prefix)
    putc ('\n', stderr);
}

/* static void */
/* show_sexp (const char *prefix, gcry_sexp_t a) */
/* { */
/*   char *buf; */
/*   size_t size; */

/*   if (prefix) */
/*     fputs (prefix, stderr); */
/*   size = gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, NULL, 0); */
/*   buf = gcry_xmalloc (size); */

/*   gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, buf, size); */
/*   fprintf (stderr, "%.*s", (int)size, buf); */
/*   gcry_free (buf); */
/* } */


/* Convert STRING consisting of hex characters into its binary
   representation and store that at BUFFER.  BUFFER needs to be of
   LENGTH bytes.  The function checks that the STRING will convert
   exactly to LENGTH bytes. The string is delimited by either end of
   string or a white space character.  The function returns -1 on
   error or the length of the parsed string.  */
static int
hex2bin (const char *string, void *buffer, size_t length)
{
  int i;
  const char *s = string;

  for (i=0; i < length; )
    {
      if (!hexdigitp (s) || !hexdigitp (s+1))
        return -1;           /* Invalid hex digits. */
      ((unsigned char*)buffer)[i++] = xtoi_2 (s);
      s += 2;
    }
  if (*s && (!my_isascii (*s) || !isspace (*s)) )
    return -1;             /* Not followed by Nul or white space.  */
  if (i != length)
    return -1;             /* Not of expected length.  */
  if (*s)
    s++; /* Skip the delimiter. */
  return s - string;
}


/* Convert STRING consisting of hex characters into its binary
   representation and return it as an allocated buffer. The valid
   length of the buffer is returned at R_LENGTH.  The string is
   delimited by end of string.  The function returns NULL on
   error.  */
static void *
hex2buffer (const char *string, size_t *r_length)
{
  const char *s;
  unsigned char *buffer;
  size_t length;

  buffer = gcry_xmalloc (strlen(string)/2+1);
  length = 0;
  for (s=string; *s; s +=2 )
    {
      if (!hexdigitp (s) || !hexdigitp (s+1))
        return NULL;           /* Invalid hex digits. */
      ((unsigned char*)buffer)[length++] = xtoi_2 (s);
    }
  *r_length = length;
  return buffer;
}


static char *
read_textline (FILE *fp)
{
  char line[256];
  char *p;
  int any = 0;

  /* Read line but skip over initial empty lines.  */
  do
    {
      do 
        {
          if (!fgets (line, sizeof line, fp))
            {
              if (feof (fp))
                return NULL;
              die ("error reading input line: %s\n", strerror (errno));
            }
          p = strchr (line, '\n');
          if (p)
            *p = 0;
          p = line + (*line? (strlen (line)-1):0);
          for ( ;p > line; p--)
            if (my_isascii (*p) && isspace (*p))
              *p = 0;
        }
      while (!any && !*line);
      any = 1;
    }
  while (*line == '#');  /* Always skip comment lines.  */
  if (verbose > 1)
    fprintf (stderr, PGM ": received line: %s\n", line);
  return gcry_xstrdup (line);
}

static char *
read_hexline (FILE *fp, size_t *retlen)
{
  char *line, *p;

  line = read_textline (fp);
  if (!line)
    return NULL;
  p = hex2buffer (line, retlen);
  if (!p)
    die ("error decoding hex string on input\n");
  gcry_free (line);
  return p;
}

static void
skip_to_empty_line (FILE *fp)
{
  char line[256];
  char *p;

  do
    {
      if (!fgets (line, sizeof line, fp))
        {
          if (feof (fp))
            return;
          die ("error reading input line: %s\n", strerror (errno));
        }
      p = strchr (line, '\n');
      if (p)
        *p =0;
    }
  while (*line);
}



/* Read a file from stream FP into a newly allocated buffer and return
   that buffer.  The valid length of the buffer is stored at R_LENGTH.
   Returns NULL on failure.  If decode is set, the file is assumed to
   be hex encoded and the decoded content is returned. */
static void *
read_file (FILE *fp, int decode, size_t *r_length)
{
  char *buffer;
  size_t buflen;
  size_t nread, bufsize = 0;

  *r_length = 0;
#define NCHUNK 8192
#ifdef HAVE_DOSISH_SYSTEM
  setmode (fileno(fp), O_BINARY);
#endif
  buffer = NULL;
  buflen = 0;
  do 
    {
      bufsize += NCHUNK;
      if (!buffer)
        buffer = gcry_xmalloc (bufsize);
      else
        buffer = gcry_xrealloc (buffer, bufsize);
      
      nread = fread (buffer + buflen, 1, NCHUNK, fp);
      if (nread < NCHUNK && ferror (fp))
        {
          gcry_free (buffer);
          return NULL;
        }
      buflen += nread;
    }
  while (nread == NCHUNK);
#undef NCHUNK
  if (decode)
    {
      const char *s;
      char *p;

      for (s=buffer,p=buffer,nread=0; nread+1 < buflen; s += 2, nread +=2 )
        {
          if (!hexdigitp (s) || !hexdigitp (s+1))
            {
              gcry_free (buffer);
              return NULL;  /* Invalid hex digits. */
            }
          *(unsigned char*)p++ = xtoi_2 (s);
        }
      if (nread != buflen)
        {
          gcry_free (buffer);
          return NULL;  /* Odd number of hex digits. */
        }
      buflen = p - buffer;
    }

  *r_length = buflen;
  return buffer;
}

/* Do in-place decoding of base-64 data of LENGTH in BUFFER.  Returns
   the new length of the buffer.  Dies on error.  */
static size_t
base64_decode (char *buffer, size_t length)
{
  static unsigned char const asctobin[128] = 
    {
      0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
      0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
      0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
      0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x3e, 0xff, 0xff, 0xff, 0x3f,
      0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0xff, 0xff,
      0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
      0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12,
      0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0xff, 0xff, 0xff, 0xff, 0xff,
      0xff, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23, 0x24,
      0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30,
      0x31, 0x32, 0x33, 0xff, 0xff, 0xff, 0xff, 0xff 
    };

  int idx = 0;
  unsigned char val = 0;
  int c = 0;
  char *d, *s;
  int lfseen = 1;

  /* Find BEGIN line.  */
  for (s=buffer; length; length--, s++)
    {
      if (lfseen && *s == '-' && length > 11 && !memcmp (s, "-----BEGIN ", 11))
        {
          for (; length && *s != '\n'; length--, s++)
            ;
          break;
        } 
      lfseen = (*s == '\n');
    }

  /* Decode until pad character or END line.  */
  for (d=buffer; length; length--, s++)
    {
      if (lfseen && *s == '-' && length > 9 && !memcmp (s, "-----END ", 9))
        break;
      if ((lfseen = (*s == '\n')) || *s == ' ' || *s == '\r' || *s == '\t')
        continue;
      if (*s == '=')
        {
          /* Pad character: stop */
          if (idx == 1)
            *d++ = val;
          break;
        }

      if ( (*s & 0x80) || (c = asctobin[*(unsigned char *)s]) == 0xff)
        die ("invalid base64 character %02X at pos %d detected\n",
             *(unsigned char*)s, (int)(s-buffer));

      switch (idx)
        {
        case 0:
          val = c << 2;
          break;
        case 1:
          val |= (c>>4)&3;
          *d++ = val;
          val = (c<<4)&0xf0;
          break;
        case 2:
          val |= (c>>2)&15;
          *d++ = val;
          val = (c<<6)&0xc0;
          break;
        case 3:
          val |= c&0x3f;
          *d++ = val;
          break;
        }
      idx = (idx+1) % 4;
    }

  return d - buffer;
}


/* Parse the buffer at the address BUFFER which consists of the number
   of octets as stored at BUFLEN.  Return the tag and the length part
   from the TLV triplet.  Update BUFFER and BUFLEN on success.  Checks
   that the encoded length does not exhaust the length of the provided
   buffer. */
static int 
parse_tag (unsigned char const **buffer, size_t *buflen, struct tag_info *ti)
{
  int c;
  unsigned long tag;
  const unsigned char *buf = *buffer;
  size_t length = *buflen;

  ti->length = 0;
  ti->ndef = 0;
  ti->nhdr = 0;

  /* Get the tag */
  if (!length)
    return -1; /* Premature EOF.  */
  c = *buf++; length--;
  ti->nhdr++;

  ti->class = (c & 0xc0) >> 6;
  ti->cons  = !!(c & 0x20);
  tag       = (c & 0x1f);

  if (tag == 0x1f)
    {
      tag = 0;
      do
        {
          tag <<= 7;
          if (!length)
            return -1; /* Premature EOF.  */
          c = *buf++; length--;
          ti->nhdr++;
          tag |= (c & 0x7f);
        }
      while ( (c & 0x80) );
    }
  ti->tag = tag;

  /* Get the length */
  if (!length)
    return -1; /* Premature EOF. */
  c = *buf++; length--;
  ti->nhdr++;

  if ( !(c & 0x80) )
    ti->length = c;
  else if (c == 0x80)
    ti->ndef = 1;
  else if (c == 0xff)
    return -1; /* Forbidden length value.  */
  else
    {
      unsigned long len = 0;
      int count = c & 0x7f;

      for (; count; count--)
        {
          len <<= 8;
          if (!length)
            return -1; /* Premature EOF.  */
          c = *buf++; length--;
          ti->nhdr++;
          len |= (c & 0xff);
        }
      ti->length = len;
    }
  
  if (ti->class == UNIVERSAL && !ti->tag)
    ti->length = 0;

  if (ti->length > length)
    return -1; /* Data larger than buffer.  */
  
  *buffer = buf;
  *buflen = length;
  return 0;
}


/* Read the file FNAME assuming it is a PEM encoded private key file
   and return an S-expression.  With SHOW set, the key parameters are
   printed.  */
static gcry_sexp_t
read_private_key_file (const char *fname, int show)
{
  gcry_error_t err;
  FILE *fp;
  char *buffer;
  size_t buflen;
  const unsigned char *der;
  size_t derlen;
  struct tag_info ti;
  gcry_mpi_t keyparms[8];
  int n_keyparms = 8;
  int idx;
  gcry_sexp_t s_key;

  fp = fopen (fname, binary_input?"rb":"r");
  if (!fp)
    die ("can't open `%s': %s\n", fname, strerror (errno));
  buffer = read_file (fp, 0, &buflen);
  if (!buffer)
    die ("error reading `%s'\n", fname);
  fclose (fp);

  buflen = base64_decode (buffer, buflen);
  
  /* Parse the ASN.1 structure.  */
  der = (const unsigned char*)buffer;
  derlen = buflen;
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_SEQUENCE || ti.class || !ti.cons || ti.ndef)
    goto bad_asn1;
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_INTEGER || ti.class || ti.cons || ti.ndef)
    goto bad_asn1;
  if (ti.length != 1 || *der)
    goto bad_asn1;  /* The value of the first integer is no 0. */
  der += ti.length; derlen -= ti.length;

  for (idx=0; idx < n_keyparms; idx++)
    {
      if ( parse_tag (&der, &derlen, &ti)
           || ti.tag != TAG_INTEGER || ti.class || ti.cons || ti.ndef)
        goto bad_asn1;
      if (show)
        {
          char prefix[2];

          prefix[0] = idx < 8? "nedpq12u"[idx] : '?';
          prefix[1] = 0;
          showhex (prefix, der, ti.length);
        }
      err = gcry_mpi_scan (keyparms+idx, GCRYMPI_FMT_USG, der, ti.length,NULL);
      if (err)
        die ("error scanning RSA parameter %d: %s\n", idx, gpg_strerror (err));
      der += ti.length; derlen -= ti.length;
    }
  if (idx != n_keyparms)
    die ("not enough RSA key parameters\n");

  gcry_free (buffer);

  /* Convert from OpenSSL parameter ordering to the OpenPGP order. */
  /* First check that p < q; if not swap p and q and recompute u.  */ 
  if (gcry_mpi_cmp (keyparms[3], keyparms[4]) > 0)
    {
      gcry_mpi_swap (keyparms[3], keyparms[4]);
      gcry_mpi_invm (keyparms[7], keyparms[3], keyparms[4]);
    }
  
  /* Build the S-expression.  */
  err = gcry_sexp_build (&s_key, NULL,
                         "(private-key(rsa(n%m)(e%m)"
                         /**/            "(d%m)(p%m)(q%m)(u%m)))",
                         keyparms[0], keyparms[1], keyparms[2],
                         keyparms[3], keyparms[4], keyparms[7] );
  if (err)
    die ("error building S-expression: %s\n", gpg_strerror (err));
  
  for (idx=0; idx < n_keyparms; idx++)
    gcry_mpi_release (keyparms[idx]);
  
  return s_key;
  
 bad_asn1:
  die ("invalid ASN.1 structure in `%s'\n", fname);
  return NULL; /*NOTREACHED*/
}


/* Read the file FNAME assuming it is a PEM encoded public key file
   and return an S-expression.  With SHOW set, the key parameters are
   printed.  */
static gcry_sexp_t
read_public_key_file (const char *fname, int show)
{
  gcry_error_t err;
  FILE *fp;
  char *buffer;
  size_t buflen;
  const unsigned char *der;
  size_t derlen;
  struct tag_info ti;
  gcry_mpi_t keyparms[2];
  int n_keyparms = 2;
  int idx;
  gcry_sexp_t s_key;

  fp = fopen (fname, binary_input?"rb":"r");
  if (!fp)
    die ("can't open `%s': %s\n", fname, strerror (errno));
  buffer = read_file (fp, 0, &buflen);
  if (!buffer)
    die ("error reading `%s'\n", fname);
  fclose (fp);

  buflen = base64_decode (buffer, buflen);
  
  /* Parse the ASN.1 structure.  */
  der = (const unsigned char*)buffer;
  derlen = buflen;
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_SEQUENCE || ti.class || !ti.cons || ti.ndef)
    goto bad_asn1;
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_SEQUENCE || ti.class || !ti.cons || ti.ndef)
    goto bad_asn1;
  /* We skip the description of the key parameters and assume it is RSA.  */
  der += ti.length; derlen -= ti.length;
  
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_BIT_STRING || ti.class || ti.cons || ti.ndef)
    goto bad_asn1;
  if (ti.length < 1 || *der)
    goto bad_asn1;  /* The number of unused bits needs to be 0. */
  der += 1; derlen -= 1;

  /* Parse the BIT string.  */
  if ( parse_tag (&der, &derlen, &ti)
       || ti.tag != TAG_SEQUENCE || ti.class || !ti.cons || ti.ndef)
    goto bad_asn1;

  for (idx=0; idx < n_keyparms; idx++)
    {
      if ( parse_tag (&der, &derlen, &ti)
           || ti.tag != TAG_INTEGER || ti.class || ti.cons || ti.ndef)
        goto bad_asn1;
      if (show)
        {
          char prefix[2];

          prefix[0] = idx < 2? "ne"[idx] : '?';
          prefix[1] = 0;
          showhex (prefix, der, ti.length);
        }
      err = gcry_mpi_scan (keyparms+idx, GCRYMPI_FMT_USG, der, ti.length,NULL);
      if (err)
        die ("error scanning RSA parameter %d: %s\n", idx, gpg_strerror (err));
      der += ti.length; derlen -= ti.length;
    }
  if (idx != n_keyparms)
    die ("not enough RSA key parameters\n");

  gcry_free (buffer);

  /* Build the S-expression.  */
  err = gcry_sexp_build (&s_key, NULL,
                         "(public-key(rsa(n%m)(e%m)))",
                         keyparms[0], keyparms[1] );
  if (err)
    die ("error building S-expression: %s\n", gpg_strerror (err));
  
  for (idx=0; idx < n_keyparms; idx++)
    gcry_mpi_release (keyparms[idx]);
  
  return s_key;
  
 bad_asn1:
  die ("invalid ASN.1 structure in `%s'\n", fname);
  return NULL; /*NOTREACHED*/
}



/* Read the file FNAME assuming it is a binary signature result and
   return an an S-expression suitable for gcry_pk_verify.  */
static gcry_sexp_t
read_sig_file (const char *fname)
{
  gcry_error_t err;
  FILE *fp;
  char *buffer;
  size_t buflen;
  gcry_mpi_t tmpmpi;
  gcry_sexp_t s_sig;

  fp = fopen (fname, "rb");
  if (!fp)
    die ("can't open `%s': %s\n", fname, strerror (errno));
  buffer = read_file (fp, 0, &buflen);
  if (!buffer)
    die ("error reading `%s'\n", fname);
  fclose (fp);

  err = gcry_mpi_scan (&tmpmpi, GCRYMPI_FMT_USG, buffer, buflen, NULL);
  if (!err)
    err = gcry_sexp_build (&s_sig, NULL,
                           "(sig-val(rsa(s %m)))", tmpmpi);
  if (err)
    die ("error building S-expression: %s\n", gpg_strerror (err));
  gcry_mpi_release (tmpmpi);
  gcry_free (buffer);

  return s_sig;
}


/* Read an S-expression from FNAME.  */
static gcry_sexp_t
read_sexp_from_file (const char *fname)
{
  gcry_error_t err;
  FILE *fp;
  char *buffer;
  size_t buflen;
  gcry_sexp_t sexp;

  fp = fopen (fname, "rb");
  if (!fp)
    die ("can't open `%s': %s\n", fname, strerror (errno));
  buffer = read_file (fp, 0, &buflen);
  if (!buffer)
    die ("error reading `%s'\n", fname);
  fclose (fp);
  if (!buflen)
    die ("error: file `%s' is empty\n", fname);

  err = gcry_sexp_create (&sexp, buffer, buflen, 1, gcry_free);
  if (err)
    die ("error parsing `%s': %s\n", fname, gpg_strerror (err));

  return sexp;
}


static void
print_buffer (const void *buffer, size_t length)
{
  int writerr = 0;

  if (base64_output)
    {
      static const unsigned char bintoasc[64+1] = 
        ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" 
         "abcdefghijklmnopqrstuvwxyz" 
         "0123456789+/"); 
      const unsigned char *p;
      unsigned char inbuf[4];
      char outbuf[4];
      int idx, quads;

      idx = quads = 0;
      for (p = buffer; length; p++, length--)
        {
          inbuf[idx++] = *p;
          if (idx > 2)
            {
              outbuf[0] = bintoasc[(*inbuf>>2)&077];
              outbuf[1] = bintoasc[(((*inbuf<<4)&060)
                                    |((inbuf[1] >> 4)&017))&077];
              outbuf[2] = bintoasc[(((inbuf[1]<<2)&074)
                                    |((inbuf[2]>>6)&03))&077];
              outbuf[3] = bintoasc[inbuf[2]&077];
              if (fwrite (outbuf, 4, 1, stdout) != 1)
                writerr = 1;
              idx = 0;
              if (++quads >= (64/4)) 
                {
                  if (fwrite ("\n", 1, 1, stdout) != 1)
                    writerr = 1;
                  quads = 0;
                }
            }
        }
      if (idx)
        {
          outbuf[0] = bintoasc[(*inbuf>>2)&077];
          if (idx == 1)
            {
              outbuf[1] = bintoasc[((*inbuf<<4)&060)&077];
              outbuf[2] = outbuf[3] = '=';
            }
          else 
            { 
              outbuf[1] = bintoasc[(((*inbuf<<4)&060)
                                    |((inbuf[1]>>4)&017))&077];
              outbuf[2] = bintoasc[((inbuf[1]<<2)&074)&077];
              outbuf[3] = '=';
            }
          if (fwrite (outbuf, 4, 1, stdout) != 1)
            writerr = 1;
          quads++;
        }
      if (quads && fwrite ("\n", 1, 1, stdout) != 1)
        writerr = 1;
    }
  else if (binary_output)
    {
      if (fwrite (buffer, length, 1, stdout) != 1)
        writerr++;
    }
  else
    {
      const unsigned char *p = buffer;
      
      if (verbose > 1)
        showhex ("sent line", buffer, length);
      while (length-- && !ferror (stdout) )
        printf ("%02X", *p++);
      if (ferror (stdout))
        writerr++;
    }
  if (!writerr && fflush (stdout) == EOF)
    writerr++;
  if (writerr)
    {
#ifndef HAVE_W32_SYSTEM
      if (loop_mode && errno == EPIPE)
        loop_mode = 0;
      else
#endif
        die ("writing output failed: %s\n", strerror (errno));
    }
}


/* Print an MPI on a line.  */
static void
print_mpi_line (gcry_mpi_t a, int no_lz)
{
  unsigned char *buf, *p;
  gcry_error_t err;
  int writerr = 0;

  err = gcry_mpi_aprint (GCRYMPI_FMT_HEX, &buf, NULL, a);
  if (err)
    die ("gcry_mpi_aprint failed: %s\n", gpg_strerror (err));

  p = buf;
  if (no_lz && p[0] == '0' && p[1] == '0' && p[2])
    p += 2;
    
  printf ("%s\n", p);
  if (ferror (stdout))
    writerr++;
  if (!writerr && fflush (stdout) == EOF)
    writerr++;
  if (writerr)
    die ("writing output failed: %s\n", strerror (errno));
  gcry_free (buf);
}


/* Print some data on hex format on a line.  */
static void
print_data_line (const void *data, size_t datalen)
{
  const unsigned char *p = data;
  int writerr = 0;
      
  while (data && datalen-- && !ferror (stdout) )
    printf ("%02X", *p++);
  putchar ('\n');
  if (ferror (stdout))
    writerr++;
  if (!writerr && fflush (stdout) == EOF)
    writerr++;
  if (writerr)
    die ("writing output failed: %s\n", strerror (errno));
}

/* Print the S-expression A to the stream FP.  */
static void
print_sexp (gcry_sexp_t a, FILE *fp)
{
  char *buf;
  size_t size;

  size = gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, NULL, 0);
  buf = gcry_xmalloc (size);
  gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, buf, size);
  if (fwrite (buf, size, 1, fp) != 1)
    die ("error writing to stream: %s\n", strerror (errno));
  gcry_free (buf);
}




static gcry_error_t
init_external_rng_test (void **r_context, 
                    unsigned int flags,
                    const void *key, size_t keylen,
                    const void *seed, size_t seedlen,
                    const void *dt, size_t dtlen)
{
  return gcry_control (PRIV_CTL_INIT_EXTRNG_TEST, 
                       r_context, flags,
                       key, keylen,
                       seed, seedlen,
                       dt, dtlen);
}

static gcry_error_t
run_external_rng_test (void *context, void *buffer, size_t buflen)
{
  return gcry_control (PRIV_CTL_RUN_EXTRNG_TEST, context, buffer, buflen);
}

static void
deinit_external_rng_test (void *context)
{
  gcry_control (PRIV_CTL_DEINIT_EXTRNG_TEST, context);
}


/* Given an OpenSSL cipher name NAME, return the Libgcrypt algirithm
   identified and store the libgcrypt mode at R_MODE.  Returns 0 on
   error.  */
static int 
map_openssl_cipher_name (const char *name, int *r_mode)
{
  static struct {
    const char *name;
    int algo;
    int mode;
  } table[] = 
    {
      { "bf-cbc",       GCRY_CIPHER_BLOWFISH, GCRY_CIPHER_MODE_CBC },
      { "bf",           GCRY_CIPHER_BLOWFISH, GCRY_CIPHER_MODE_CBC },
      { "bf-cfb",       GCRY_CIPHER_BLOWFISH, GCRY_CIPHER_MODE_CFB },
      { "bf-ecb",       GCRY_CIPHER_BLOWFISH, GCRY_CIPHER_MODE_ECB },
      { "bf-ofb",       GCRY_CIPHER_BLOWFISH, GCRY_CIPHER_MODE_OFB },

      { "cast-cbc",     GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_CBC },
      { "cast",         GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_CBC },
      { "cast5-cbc",    GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_CBC },
      { "cast5-cfb",    GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_CFB },
      { "cast5-ecb",    GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_ECB },
      { "cast5-ofb",    GCRY_CIPHER_CAST5, GCRY_CIPHER_MODE_OFB },

      { "des-cbc",      GCRY_CIPHER_DES, GCRY_CIPHER_MODE_CBC },
      { "des",          GCRY_CIPHER_DES, GCRY_CIPHER_MODE_CBC },
      { "des-cfb",      GCRY_CIPHER_DES, GCRY_CIPHER_MODE_CFB },
      { "des-ofb",      GCRY_CIPHER_DES, GCRY_CIPHER_MODE_OFB },
      { "des-ecb",      GCRY_CIPHER_DES, GCRY_CIPHER_MODE_ECB },

      { "des-ede3-cbc", GCRY_CIPHER_3DES, GCRY_CIPHER_MODE_CBC },
      { "des-ede3",     GCRY_CIPHER_3DES, GCRY_CIPHER_MODE_ECB },
      { "des3",         GCRY_CIPHER_3DES, GCRY_CIPHER_MODE_CBC },
      { "des-ede3-cfb", GCRY_CIPHER_3DES, GCRY_CIPHER_MODE_CFB },
      { "des-ede3-ofb", GCRY_CIPHER_3DES, GCRY_CIPHER_MODE_OFB },

      { "rc4",          GCRY_CIPHER_ARCFOUR, GCRY_CIPHER_MODE_STREAM },

      { "aes-128-cbc",  GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_CBC },
      { "aes-128",      GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_CBC },
      { "aes-128-cfb",  GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_CFB },
      { "aes-128-ecb",  GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_ECB },
      { "aes-128-ofb",  GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_OFB },

      { "aes-192-cbc",  GCRY_CIPHER_AES192, GCRY_CIPHER_MODE_CBC },
      { "aes-192",      GCRY_CIPHER_AES192, GCRY_CIPHER_MODE_CBC },
      { "aes-192-cfb",  GCRY_CIPHER_AES192, GCRY_CIPHER_MODE_CFB },
      { "aes-192-ecb",  GCRY_CIPHER_AES192, GCRY_CIPHER_MODE_ECB },
      { "aes-192-ofb",  GCRY_CIPHER_AES192, GCRY_CIPHER_MODE_OFB },
      
      { "aes-256-cbc",  GCRY_CIPHER_AES256, GCRY_CIPHER_MODE_CBC },
      { "aes-256",      GCRY_CIPHER_AES256, GCRY_CIPHER_MODE_CBC },
      { "aes-256-cfb",  GCRY_CIPHER_AES256, GCRY_CIPHER_MODE_CFB },
      { "aes-256-ecb",  GCRY_CIPHER_AES256, GCRY_CIPHER_MODE_ECB },
      { "aes-256-ofb",  GCRY_CIPHER_AES256, GCRY_CIPHER_MODE_OFB },

      { NULL, 0 , 0 }
    };
  int idx;

  for (idx=0; table[idx].name; idx++)
    if (!strcmp (name, table[idx].name))
      {
        *r_mode = table[idx].mode;
        return table[idx].algo;
      }
  *r_mode = 0;
  return 0;
}



/* Run an encrypt or decryption operations.  If DATA is NULL the
   function reads its input in chunks of size DATALEN from fp and
   processes it and writes it out until EOF.  */
static void
run_encrypt_decrypt (int encrypt_mode,
                     int cipher_algo, int cipher_mode, 
                     const void *iv_buffer, size_t iv_buflen,
                     const void *key_buffer, size_t key_buflen,
                     const void *data, size_t datalen, FILE *fp)
{
  gpg_error_t err;
  gcry_cipher_hd_t hd;
  void *outbuf;
  size_t outbuflen;
  void *inbuf;
  size_t inbuflen;
  size_t blocklen;

  err = gcry_cipher_open (&hd, cipher_algo, cipher_mode, 0);
  if (err)
    die ("gcry_cipher_open failed for algo %d, mode %d: %s\n", 
         cipher_algo, cipher_mode, gpg_strerror (err));

  blocklen = gcry_cipher_get_algo_blklen (cipher_algo);
  assert (blocklen);

  gcry_cipher_ctl (hd, PRIV_CTL_DISABLE_WEAK_KEY, NULL, 0);

  err = gcry_cipher_setkey (hd, key_buffer, key_buflen);
  if (err)
    die ("gcry_cipher_setkey failed with keylen %u: %s\n",
         (unsigned int)key_buflen, gpg_strerror (err));

  if (iv_buffer)
    {
      err = gcry_cipher_setiv (hd, iv_buffer, iv_buflen);
      if (err)
        die ("gcry_cipher_setiv failed with ivlen %u: %s\n",
             (unsigned int)iv_buflen, gpg_strerror (err));
    }

  inbuf = data? NULL : gcry_xmalloc (datalen);
  outbuflen = datalen;
  outbuf = gcry_xmalloc (outbuflen < blocklen? blocklen:outbuflen);

  do
    {
      if (inbuf)
        {
          int nread = fread (inbuf, 1, datalen, fp);
          if (nread < (int)datalen && ferror (fp))
            die ("error reading input\n");
          data = inbuf;
          inbuflen = nread;
        }
      else
        inbuflen = datalen;

      if (encrypt_mode)
        err = gcry_cipher_encrypt (hd, outbuf, outbuflen, data, inbuflen);
      else
        err = gcry_cipher_decrypt (hd, outbuf, outbuflen, data, inbuflen);
      if (err)
        die ("gcry_cipher_%scrypt failed: %s\n",
             encrypt_mode? "en":"de", gpg_strerror (err));
      
      print_buffer (outbuf, outbuflen);
    }
  while (inbuf);

  gcry_cipher_close (hd);
  gcry_free (outbuf);
  gcry_free (inbuf);
}


static void
get_current_iv (gcry_cipher_hd_t hd, void *buffer, size_t buflen)
{
  unsigned char tmp[17];

  if (gcry_cipher_ctl (hd, PRIV_CTL_GET_INPUT_VECTOR, tmp, sizeof tmp))
    die ("error getting current input vector\n");
  if (buflen > *tmp)
    die ("buffer too short to store the current input vector\n");
  memcpy (buffer, tmp+1, *tmp);
}

/* Run the inner loop of the CAVS monte carlo test.  */
static void
run_cipher_mct_loop (int encrypt_mode, int cipher_algo, int cipher_mode, 
                     const void *iv_buffer, size_t iv_buflen,
                     const void *key_buffer, size_t key_buflen,
                     const void *data, size_t datalen, int iterations)
{
  gpg_error_t err;
  gcry_cipher_hd_t hd;
  size_t blocklen;
  int count;
  char input[16];
  char output[16];
  char last_output[16];
  char last_last_output[16];
  char last_iv[16]; 


  err = gcry_cipher_open (&hd, cipher_algo, cipher_mode, 0);
  if (err)
    die ("gcry_cipher_open failed for algo %d, mode %d: %s\n", 
         cipher_algo, cipher_mode, gpg_strerror (err));

  blocklen = gcry_cipher_get_algo_blklen (cipher_algo);
  if (!blocklen || blocklen > sizeof output)
    die ("invalid block length %d\n", blocklen);


  gcry_cipher_ctl (hd, PRIV_CTL_DISABLE_WEAK_KEY, NULL, 0);

  err = gcry_cipher_setkey (hd, key_buffer, key_buflen);
  if (err)
    die ("gcry_cipher_setkey failed with keylen %u: %s\n",
         (unsigned int)key_buflen, gpg_strerror (err));

  if (iv_buffer)
    {
      err = gcry_cipher_setiv (hd, iv_buffer, iv_buflen);
      if (err)
        die ("gcry_cipher_setiv failed with ivlen %u: %s\n",
             (unsigned int)iv_buflen, gpg_strerror (err));
    }

  if (datalen != blocklen)
    die ("length of input (%u) does not match block length (%u)\n", 
         (unsigned int)datalen, (unsigned int)blocklen);
  memcpy (input, data, datalen);
  memset (output, 0, sizeof output);
  for (count=0; count < iterations; count++)
    {
      memcpy (last_last_output, last_output, sizeof last_output);
      memcpy (last_output, output, sizeof output);

      get_current_iv (hd, last_iv, blocklen);

      if (encrypt_mode)
        err = gcry_cipher_encrypt (hd, output, blocklen, input, blocklen);
      else
        err = gcry_cipher_decrypt (hd, output, blocklen, input, blocklen);
      if (err)
        die ("gcry_cipher_%scrypt failed: %s\n",
             encrypt_mode? "en":"de", gpg_strerror (err));

  
      if (encrypt_mode && (cipher_mode == GCRY_CIPHER_MODE_CFB
                           || cipher_mode == GCRY_CIPHER_MODE_CBC))
        memcpy (input, last_iv, blocklen);
      else if (cipher_mode == GCRY_CIPHER_MODE_OFB)
        memcpy (input, last_iv, blocklen);
      else if (!encrypt_mode && cipher_mode == GCRY_CIPHER_MODE_CFB)
        {
          /* Reconstruct the output vector.  */
          int i;
          for (i=0; i < blocklen; i++)
            input[i] ^= output[i];
        }
      else
        memcpy (input, output, blocklen);
    }

  print_buffer (output, blocklen);
  putchar ('\n');
  print_buffer (last_output, blocklen);
  putchar ('\n');
  print_buffer (last_last_output, blocklen);
  putchar ('\n');
  get_current_iv (hd, last_iv, blocklen);
  print_buffer (last_iv, blocklen); /* Last output vector.  */
  putchar ('\n');
  print_buffer (input, blocklen);   /* Next input text. */
  putchar ('\n');
  if (verbose > 1)
    showhex ("sent line", "", 0);
  putchar ('\n');
  fflush (stdout);

  gcry_cipher_close (hd);
}



/* Run a digest operation.  */
static void
run_digest (int digest_algo,  const void *data, size_t datalen)
{
  gpg_error_t err;
  gcry_md_hd_t hd;
  const unsigned char *digest;
  unsigned int digestlen;

  err = gcry_md_open (&hd, digest_algo, 0);
  if (err)
    die ("gcry_md_open failed for algo %d: %s\n", 
         digest_algo,  gpg_strerror (err));

  gcry_md_write (hd, data, datalen);
  digest = gcry_md_read (hd, digest_algo);
  digestlen = gcry_md_get_algo_dlen (digest_algo);
  print_buffer (digest, digestlen);
  gcry_md_close (hd);
}


/* Run a HMAC operation.  */
static void
run_hmac (int digest_algo, const void *key, size_t keylen, 
          const void *data, size_t datalen)
{
  gpg_error_t err;
  gcry_md_hd_t hd;
  const unsigned char *digest;
  unsigned int digestlen;

  err = gcry_md_open (&hd, digest_algo, GCRY_MD_FLAG_HMAC);
  if (err)
    die ("gcry_md_open failed for HMAC algo %d: %s\n", 
         digest_algo,  gpg_strerror (err));

  gcry_md_setkey (hd, key, keylen);
  if (err)
    die ("gcry_md_setkey failed for HMAC algo %d: %s\n", 
         digest_algo,  gpg_strerror (err));

  gcry_md_write (hd, data, datalen);
  digest = gcry_md_read (hd, digest_algo);
  digestlen = gcry_md_get_algo_dlen (digest_algo);
  print_buffer (digest, digestlen);
  gcry_md_close (hd);
}



/* Derive an RSA key using the S-expression in (DATA,DATALEN).  This
   S-expression is used directly as input to gcry_pk_genkey.  The
   result is printed to stdout with one parameter per line in hex
   format and in this order: p, q, n, d.  */
static void
run_rsa_derive (const void *data, size_t datalen)
{
  gpg_error_t err;
  gcry_sexp_t s_keyspec, s_key, s_top, l1;
  gcry_mpi_t mpi;
  const char *parmlist;
  int idx;

  if (!datalen)
    err = gpg_error (GPG_ERR_NO_DATA);
  else
    err = gcry_sexp_new (&s_keyspec, data, datalen, 1);
  if (err)
    die ("gcry_sexp_new failed for RSA key derive: %s\n",
         gpg_strerror (err));

  err = gcry_pk_genkey (&s_key, s_keyspec);
  if (err)
    die ("gcry_pk_genkey failed for RSA: %s\n", gpg_strerror (err));

  gcry_sexp_release (s_keyspec);

  /* P and Q might have been swapped but we need to to return them in
     the proper order.  Build the parameter list accordingly.  */
  parmlist = "pqnd";
  s_top = gcry_sexp_find_token (s_key, "misc-key-info", 0);
  if (s_top)
    {
      l1 = gcry_sexp_find_token (s_top, "p-q-swapped", 0);
      if (l1)
        parmlist = "qpnd";
      gcry_sexp_release (l1);
      gcry_sexp_release (s_top);
    }

  /* Parse and print the parameters.  */
  l1 = gcry_sexp_find_token (s_key, "private-key", 0);
  s_top = gcry_sexp_find_token (l1, "rsa", 0);
  gcry_sexp_release (l1);
  if (!s_top)
    die ("private-key part not found in result\n");

  for (idx=0; parmlist[idx]; idx++)
    {
      l1 = gcry_sexp_find_token (s_top, parmlist+idx, 1);
      mpi = gcry_sexp_nth_mpi (l1, 1, GCRYMPI_FMT_USG);
      gcry_sexp_release (l1);
      if (!mpi)
        die ("parameter %c missing in private-key\n", parmlist[idx]);
      print_mpi_line (mpi, 1);
      gcry_mpi_release (mpi);
    }

  gcry_sexp_release (s_top);
  gcry_sexp_release (s_key);
}



static size_t
compute_tag_length (size_t n)
{     
  int needed = 0;

  if (n < 128)
    needed += 2; /* Tag and one length byte.  */
  else if (n < 256)
    needed += 3; /* Tag, number of length bytes, 1 length byte.  */
  else if (n < 65536)
    needed += 4; /* Tag, number of length bytes, 2 length bytes.  */
  else
    die ("DER object too long to encode\n");

  return needed;
}

static unsigned char *
store_tag_length (unsigned char *p, int tag, size_t n)
{     
  if (tag == TAG_SEQUENCE)
    tag |= 0x20; /* constructed */

  *p++ = tag;
  if (n < 128)
    *p++ = n;
  else if (n < 256)
    {
      *p++ = 0x81;
      *p++ = n;
    }
  else if (n < 65536)
    {
      *p++ = 0x82;
      *p++ = n >> 8;
      *p++ = n;
    }

  return p;
}


/* Generate an RSA key of size KEYSIZE using the public exponent
   PUBEXP and print it to stdout in the OpenSSL format.  The format
   is:

       SEQUENCE {
         INTEGER (0)  -- Unknown constant. 
         INTEGER      -- n
         INTEGER      -- e
         INTEGER      -- d
         INTEGER      -- p     
         INTEGER      -- q      (with p < q)
         INTEGER      -- dmp1 = d mod (p-1)
         INTEGER      -- dmq1 = d mod (q-1)
         INTEGER      -- u    = p^{-1} mod q
       }

*/
static void
run_rsa_gen (int keysize, int pubexp)
{
  gpg_error_t err;
  gcry_sexp_t keyspec, key, l1;
  const char keyelems[] = "nedpq..u";
  gcry_mpi_t keyparms[8];
  size_t     keyparmslen[8];
  int idx;
  size_t derlen, needed, n;
  unsigned char *derbuf, *der;

  err = gcry_sexp_build (&keyspec, NULL, 
                         "(genkey (rsa (nbits %d)(rsa-use-e %d)))",
                         keysize, pubexp);
  if (err)
    die ("gcry_sexp_build failed for RSA key generation: %s\n",
         gpg_strerror (err));

  err = gcry_pk_genkey (&key, keyspec);
  if (err)
    die ("gcry_pk_genkey failed for RSA: %s\n", gpg_strerror (err));
  
  gcry_sexp_release (keyspec);

  l1 = gcry_sexp_find_token (key, "private-key", 0);
  if (!l1)
    die ("private key not found in genkey result\n");
  gcry_sexp_release (key);
  key = l1;

  l1 = gcry_sexp_find_token (key, "rsa", 0);
  if (!l1)
    die ("returned private key not formed as expected\n");
  gcry_sexp_release (key);
  key = l1;

  /* Extract the parameters from the S-expression and store them in a
     well defined order in KEYPARMS.  */
  for (idx=0; idx < DIM(keyparms); idx++) 
    {
      if (keyelems[idx] == '.')
        {
          keyparms[idx] = gcry_mpi_new (0);
          continue;
        }
      l1 = gcry_sexp_find_token (key, keyelems+idx, 1);
      if (!l1)
        die ("no %c parameter in returned private key\n", keyelems[idx]);
      keyparms[idx] = gcry_sexp_nth_mpi (l1, 1, GCRYMPI_FMT_USG);
      if (!keyparms[idx])
        die ("no value for %c parameter in returned private key\n",
             keyelems[idx]);
      gcry_sexp_release (l1);
    }

  gcry_sexp_release (key);

  /* Check that p < q; if not swap p and q and recompute u.  */ 
  if (gcry_mpi_cmp (keyparms[3], keyparms[4]) > 0)
    {
      gcry_mpi_swap (keyparms[3], keyparms[4]);
      gcry_mpi_invm (keyparms[7], keyparms[3], keyparms[4]);
    }

  /* Compute the additional parameters.  */
  gcry_mpi_sub_ui (keyparms[5], keyparms[3], 1);
  gcry_mpi_mod (keyparms[5], keyparms[2], keyparms[5]);
  gcry_mpi_sub_ui (keyparms[6], keyparms[4], 1);
  gcry_mpi_mod (keyparms[6], keyparms[2], keyparms[6]);

  /* Compute the length of the DER encoding.  */
  needed = compute_tag_length (1) + 1;  
  for (idx=0; idx < DIM(keyparms); idx++) 
    {
      err = gcry_mpi_print (GCRYMPI_FMT_STD, NULL, 0, &n, keyparms[idx]);
      if (err)
        die ("error formatting parameter: %s\n", gpg_strerror (err));
      keyparmslen[idx] = n;
      needed += compute_tag_length (n) + n;
    }
  
  /* Store the key parameters. */
  derlen = compute_tag_length (needed) + needed;
  der = derbuf = gcry_xmalloc (derlen);

  der = store_tag_length (der, TAG_SEQUENCE, needed);
  der = store_tag_length (der, TAG_INTEGER, 1);
  *der++ = 0;
  for (idx=0; idx < DIM(keyparms); idx++) 
    {
      der = store_tag_length (der, TAG_INTEGER, keyparmslen[idx]);
      err = gcry_mpi_print (GCRYMPI_FMT_STD, der, 
                           keyparmslen[idx], NULL, keyparms[idx]);
      if (err)
        die ("error formatting parameter: %s\n", gpg_strerror (err));
      der += keyparmslen[idx];
    }

  /* Print the stuff.  */
  for (idx=0; idx < DIM(keyparms); idx++) 
    gcry_mpi_release (keyparms[idx]);

  assert (der - derbuf == derlen);

  if (base64_output)
    puts ("-----BEGIN RSA PRIVATE KEY-----");
  print_buffer (derbuf, derlen);
  if (base64_output)
    puts ("-----END RSA PRIVATE KEY-----");

  gcry_free (derbuf);
}



/* Sign DATA of length DATALEN using the key taken from the PEM
   encoded KEYFILE and the hash algorithm HASHALGO.  */
static void
run_rsa_sign (const void *data, size_t datalen,
              int hashalgo, int pkcs1, const char *keyfile)

{
  gpg_error_t err;
  gcry_sexp_t s_data, s_key, s_sig, s_tmp;
  gcry_mpi_t sig_mpi = NULL;
  unsigned char *outbuf;
  size_t outlen;
  
/*   showhex ("D", data, datalen); */
  if (pkcs1)
    {
      unsigned char hash[64];
      unsigned int hashsize;

      hashsize = gcry_md_get_algo_dlen (hashalgo);
      if (!hashsize || hashsize > sizeof hash)
        die ("digest too long for buffer or unknown hash algorithm\n");
      gcry_md_hash_buffer (hashalgo, hash, data, datalen);
      err = gcry_sexp_build (&s_data, NULL,
                             "(data (flags pkcs1)(hash %s %b))",
                             gcry_md_algo_name (hashalgo),
                             (int)hashsize, hash);
    }
  else
    {
      gcry_mpi_t tmp;

      err = gcry_mpi_scan (&tmp, GCRYMPI_FMT_USG, data, datalen,NULL);
      if (!err)
        {
          err = gcry_sexp_build (&s_data, NULL,
                                 "(data (flags raw)(value %m))", tmp);
          gcry_mpi_release (tmp);
        }
    }
  if (err)
    die ("gcry_sexp_build failed for RSA data input: %s\n",
         gpg_strerror (err));

  s_key = read_private_key_file (keyfile, 0);

  err = gcry_pk_sign (&s_sig, s_data, s_key);
  if (err)
    {
      gcry_sexp_release (read_private_key_file (keyfile, 1));
      die ("gcry_pk_signed failed (datalen=%d,keyfile=%s): %s\n",
           (int)datalen, keyfile, gpg_strerror (err));
    }
  gcry_sexp_release (s_key);
  gcry_sexp_release (s_data);

  s_tmp = gcry_sexp_find_token (s_sig, "sig-val", 0);
  if (s_tmp)
    {
      gcry_sexp_release (s_sig);
      s_sig = s_tmp;
      s_tmp = gcry_sexp_find_token (s_sig, "rsa", 0);
      if (s_tmp)
        {
          gcry_sexp_release (s_sig);
          s_sig = s_tmp;
          s_tmp = gcry_sexp_find_token (s_sig, "s", 0);
          if (s_tmp)
            {
              gcry_sexp_release (s_sig);
              s_sig = s_tmp;
              sig_mpi = gcry_sexp_nth_mpi (s_sig, 1, GCRYMPI_FMT_USG);
            }
        }
    }
  gcry_sexp_release (s_sig);
              
  if (!sig_mpi)
    die ("no value in returned S-expression\n");
  err = gcry_mpi_aprint (GCRYMPI_FMT_STD, &outbuf, &outlen, sig_mpi);
  if (err)
    die ("gcry_mpi_aprint failed: %s\n", gpg_strerror (err));
  gcry_mpi_release (sig_mpi);

  print_buffer (outbuf, outlen);
  gcry_free (outbuf);
}



/* Verify DATA of length DATALEN using the public key taken from the
   PEM encoded KEYFILE and the hash algorithm HASHALGO against the
   binary signature in SIGFILE.  */
static void
run_rsa_verify (const void *data, size_t datalen, int hashalgo, int pkcs1,
                const char *keyfile, const char *sigfile)

{
  gpg_error_t err;
  gcry_sexp_t s_data, s_key, s_sig;
  
  if (pkcs1)
    {
      unsigned char hash[64];
      unsigned int hashsize;

      hashsize = gcry_md_get_algo_dlen (hashalgo);
      if (!hashsize || hashsize > sizeof hash)
        die ("digest too long for buffer or unknown hash algorithm\n");
      gcry_md_hash_buffer (hashalgo, hash, data, datalen);
      err = gcry_sexp_build (&s_data, NULL,
                             "(data (flags pkcs1)(hash %s %b))",
                             gcry_md_algo_name (hashalgo),
                             (int)hashsize, hash);
    }
  else
    {
      gcry_mpi_t tmp;

      err = gcry_mpi_scan (&tmp, GCRYMPI_FMT_USG, data, datalen,NULL);
      if (!err)
        {
          err = gcry_sexp_build (&s_data, NULL,
                                 "(data (flags raw)(value %m))", tmp);
          gcry_mpi_release (tmp);
        }
    }
  if (err)
    die ("gcry_sexp_build failed for RSA data input: %s\n",
         gpg_strerror (err));

  s_key = read_public_key_file (keyfile, 0);

  s_sig = read_sig_file (sigfile);

  err = gcry_pk_verify (s_sig, s_data, s_key);
  if (!err)
    puts ("GOOD signature");
  else if (gpg_err_code (err) == GPG_ERR_BAD_SIGNATURE)
    puts ("BAD signature");
  else
    printf ("ERROR (%s)\n", gpg_strerror (err));

  gcry_sexp_release (s_sig);
  gcry_sexp_release (s_key);
  gcry_sexp_release (s_data);
}



/* Generate a DSA key of size KEYSIZE and return the complete
   S-expression.  */
static gcry_sexp_t
dsa_gen (int keysize)
{
  gpg_error_t err;
  gcry_sexp_t keyspec, key;

  err = gcry_sexp_build (&keyspec, NULL, 
                         "(genkey (dsa (nbits %d)(use-fips186-2)))",
                         keysize);
  if (err)
    die ("gcry_sexp_build failed for DSA key generation: %s\n",
         gpg_strerror (err));

  err = gcry_pk_genkey (&key, keyspec);
  if (err)
    die ("gcry_pk_genkey failed for DSA: %s\n", gpg_strerror (err));
  
  gcry_sexp_release (keyspec);

  return key;
}


/* Generate a DSA key of size KEYSIZE and return the complete
   S-expression.  */
static gcry_sexp_t
dsa_gen_with_seed (int keysize, const void *seed, size_t seedlen)
{
  gpg_error_t err;
  gcry_sexp_t keyspec, key;

  err = gcry_sexp_build (&keyspec, NULL, 
                         "(genkey"
                         "  (dsa"
                         "    (nbits %d)"
                         "    (use-fips186-2)"
                         "    (derive-parms"
                         "      (seed %b))))",
                         keysize, (int)seedlen, seed);
  if (err)
    die ("gcry_sexp_build failed for DSA key generation: %s\n",
         gpg_strerror (err));

  err = gcry_pk_genkey (&key, keyspec);
  if (err)
    die ("gcry_pk_genkey failed for DSA: %s\n", gpg_strerror (err));
  
  gcry_sexp_release (keyspec);

  return key;
}


/* Print the domain parameter as well as the derive information.  KEY
   is the complete key as returned by dsa_gen.  We print to stdout
   with one parameter per line in hex format using this order: p, q,
   g, seed, counter, h. */
static void 
print_dsa_domain_parameters (gcry_sexp_t key)
{
  gcry_sexp_t l1, l2;
  gcry_mpi_t mpi;
  int idx;
  const void *data;
  size_t datalen;
  char *string;

  l1 = gcry_sexp_find_token (key, "public-key", 0);
  if (!l1)
    die ("public key not found in genkey result\n");

  l2 = gcry_sexp_find_token (l1, "dsa", 0);
  if (!l2)
    die ("returned public key not formed as expected\n");
  gcry_sexp_release (l1);
  l1 = l2;

  /* Extract the parameters from the S-expression and print them to stdout.  */
  for (idx=0; "pqg"[idx]; idx++) 
    {
      l2 = gcry_sexp_find_token (l1, "pqg"+idx, 1);
      if (!l2)
        die ("no %c parameter in returned public key\n", "pqg"[idx]);
      mpi = gcry_sexp_nth_mpi (l2, 1, GCRYMPI_FMT_USG);
      if (!mpi)
        die ("no value for %c parameter in returned public key\n","pqg"[idx]);
      gcry_sexp_release (l2);
      if (standalone_mode)
        printf ("%c = ", "PQG"[idx]);
      print_mpi_line (mpi, 1);
      gcry_mpi_release (mpi);
    }
  gcry_sexp_release (l1);

  /* Extract the seed values.  */
  l1 = gcry_sexp_find_token (key, "misc-key-info", 0);
  if (!l1)
    die ("misc-key-info not found in genkey result\n");

  l2 = gcry_sexp_find_token (l1, "seed-values", 0);
  if (!l2)
    die ("no seed-values in returned key\n");
  gcry_sexp_release (l1);
  l1 = l2;

  l2 = gcry_sexp_find_token (l1, "seed", 0);
  if (!l2)
    die ("no seed value in returned key\n");
  data = gcry_sexp_nth_data (l2, 1, &datalen);
  if (!data)
    die ("no seed value in returned key\n");
  if (standalone_mode)
    printf ("Seed = ");
  print_data_line (data, datalen);
  gcry_sexp_release (l2);

  l2 = gcry_sexp_find_token (l1, "counter", 0);
  if (!l2)
    die ("no counter value in returned key\n");
  string = gcry_sexp_nth_string (l2, 1);
  if (!string)
    die ("no counter value in returned key\n");
  if (standalone_mode)
    printf ("c = %ld\n", strtoul (string, NULL, 10));
  else
    printf ("%lX\n", strtoul (string, NULL, 10));
  gcry_free (string);
  gcry_sexp_release (l2);

  l2 = gcry_sexp_find_token (l1, "h", 0);
  if (!l2)
    die ("no n value in returned key\n");
  mpi = gcry_sexp_nth_mpi (l2, 1, GCRYMPI_FMT_USG);
  if (!mpi)
    die ("no h value in returned key\n");
  if (standalone_mode)
    printf ("H = ");
  print_mpi_line (mpi, 1);
  gcry_mpi_release (mpi);
  gcry_sexp_release (l2);

  gcry_sexp_release (l1);
}


/* Generate DSA domain parameters for a modulus size of KEYSIZE.  The
   result is printed to stdout with one parameter per line in hex
   format and in this order: p, q, g, seed, counter, h.  If SEED is
   not NULL this seed value will be used for the generation.  */
static void
run_dsa_pqg_gen (int keysize, const void *seed, size_t seedlen)
{
  gcry_sexp_t key;

  if (seed)
    key = dsa_gen_with_seed (keysize, seed, seedlen);
  else
    key = dsa_gen (keysize);
  print_dsa_domain_parameters (key);
  gcry_sexp_release (key);
}


/* Generate a DSA key of size of KEYSIZE and write the private key to
   FILENAME.  Also write the parameters to stdout in the same way as
   run_dsa_pqg_gen.  */
static void
run_dsa_gen (int keysize, const char *filename)
{
  gcry_sexp_t key, private_key;
  FILE *fp;

  key = dsa_gen (keysize);
  private_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!private_key)
    die ("private key not found in genkey result\n");
  print_dsa_domain_parameters (key);

  fp = fopen (filename, "wb");
  if (!fp)
    die ("can't create `%s': %s\n", filename, strerror (errno));
  print_sexp (private_key, fp);
  fclose (fp);

  gcry_sexp_release (private_key);
  gcry_sexp_release (key);
}



/* Sign DATA of length DATALEN using the key taken from the S-expression
   encoded KEYFILE. */
static void
run_dsa_sign (const void *data, size_t datalen, const char *keyfile)

{
  gpg_error_t err;
  gcry_sexp_t s_data, s_key, s_sig, s_tmp, s_tmp2;
  char hash[20];
  gcry_mpi_t tmpmpi;

  gcry_md_hash_buffer (GCRY_MD_SHA1, hash, data, datalen);
  err = gcry_mpi_scan (&tmpmpi, GCRYMPI_FMT_USG, hash, 20, NULL);
  if (!err)
    {
      err = gcry_sexp_build (&s_data, NULL,
                             "(data (flags raw)(value %m))", tmpmpi);
      gcry_mpi_release (tmpmpi);
    }
  if (err)
    die ("gcry_sexp_build failed for DSA data input: %s\n",
         gpg_strerror (err));

  s_key = read_sexp_from_file (keyfile);

  err = gcry_pk_sign (&s_sig, s_data, s_key);
  if (err)
    {
      gcry_sexp_release (read_private_key_file (keyfile, 1));
      die ("gcry_pk_signed failed (datalen=%d,keyfile=%s): %s\n",
           (int)datalen, keyfile, gpg_strerror (err));
    }
  gcry_sexp_release (s_data);

  /* We need to return the Y parameter first.  */
  s_tmp = gcry_sexp_find_token (s_key, "private-key", 0);
  if (!s_tmp)
    die ("private key part not found in provided key\n");

  s_tmp2 = gcry_sexp_find_token (s_tmp, "dsa", 0);
  if (!s_tmp2)
    die ("private key part is not a DSA key\n");
  gcry_sexp_release (s_tmp);

  s_tmp = gcry_sexp_find_token (s_tmp2, "y", 0);
  tmpmpi = gcry_sexp_nth_mpi (s_tmp, 1, GCRYMPI_FMT_USG);
  if (!tmpmpi)
    die ("no y parameter in DSA key\n");
  print_mpi_line (tmpmpi, 1);
  gcry_mpi_release (tmpmpi);
  gcry_sexp_release (s_tmp);

  gcry_sexp_release (s_key);


  /* Now return the actual signature.  */
  s_tmp = gcry_sexp_find_token (s_sig, "sig-val", 0);
  if (!s_tmp)
    die ("no sig-val element in returned S-expression\n");

  gcry_sexp_release (s_sig);
  s_sig = s_tmp;
  s_tmp = gcry_sexp_find_token (s_sig, "dsa", 0);
  if (!s_tmp)
    die ("no dsa element in returned S-expression\n");

  gcry_sexp_release (s_sig);
  s_sig = s_tmp;

  s_tmp = gcry_sexp_find_token (s_sig, "r", 0);
  tmpmpi = gcry_sexp_nth_mpi (s_tmp, 1, GCRYMPI_FMT_USG);
  if (!tmpmpi)
    die ("no r parameter in returned S-expression\n");
  print_mpi_line (tmpmpi, 1);
  gcry_mpi_release (tmpmpi);
  gcry_sexp_release (s_tmp);
    
  s_tmp = gcry_sexp_find_token (s_sig, "s", 0);
  tmpmpi = gcry_sexp_nth_mpi (s_tmp, 1, GCRYMPI_FMT_USG);
  if (!tmpmpi)
    die ("no s parameter in returned S-expression\n");
  print_mpi_line (tmpmpi, 1);
  gcry_mpi_release (tmpmpi);
  gcry_sexp_release (s_tmp);

  gcry_sexp_release (s_sig);
}



/* Verify DATA of length DATALEN using the public key taken from the
   S-expression in KEYFILE against the S-expression formatted
   signature in SIGFILE.  */
static void
run_dsa_verify (const void *data, size_t datalen,
                const char *keyfile, const char *sigfile)

{
  gpg_error_t err;
  gcry_sexp_t s_data, s_key, s_sig;
  char hash[20];
  gcry_mpi_t tmpmpi;

  gcry_md_hash_buffer (GCRY_MD_SHA1, hash, data, datalen);
  /* Note that we can't simply use %b with HASH to build the
     S-expression, because that might yield a negative value.  */
  err = gcry_mpi_scan (&tmpmpi, GCRYMPI_FMT_USG, hash, 20, NULL);
  if (!err)
    {
      err = gcry_sexp_build (&s_data, NULL,
                             "(data (flags raw)(value %m))", tmpmpi);
      gcry_mpi_release (tmpmpi);
    }
  if (err)
    die ("gcry_sexp_build failed for DSA data input: %s\n",
         gpg_strerror (err));

  s_key = read_sexp_from_file (keyfile);
  s_sig = read_sexp_from_file (sigfile);

  err = gcry_pk_verify (s_sig, s_data, s_key);
  if (!err)
    puts ("GOOD signature");
  else if (gpg_err_code (err) == GPG_ERR_BAD_SIGNATURE)
    puts ("BAD signature");
  else
    printf ("ERROR (%s)\n", gpg_strerror (err));

  gcry_sexp_release (s_sig);
  gcry_sexp_release (s_key);
  gcry_sexp_release (s_data);
}




static void
usage (int show_help)
{
  if (!show_help)
    {
      fputs ("usage: " PGM 
             " [OPTION] [FILE] (try --help for more information)\n", stderr);
      exit (2);
    }
  fputs
    ("Usage: " PGM " [OPTIONS] MODE [FILE]\n"
     "Run a crypto operation using hex encoded input and output.\n"
     "MODE:\n"
     "  encrypt, decrypt, digest, random, hmac-sha,\n"
     "  rsa-{derive,gen,sign,verify}, dsa-{pqg-gen,gen,sign,verify}\n"
     "OPTIONS:\n"
     "  --verbose        Print additional information\n"
     "  --binary         Input and output is in binary form\n"
     "  --no-fips        Do not force FIPS mode\n"
     "  --key KEY        Use the hex encoded KEY\n"
     "  --iv IV          Use the hex encoded IV\n"
     "  --dt DT          Use the hex encoded DT for the RNG\n"
     "  --algo NAME      Use algorithm NAME\n"
     "  --keysize N      Use a keysize of N bits\n"
     "  --signature NAME Take signature from file NAME\n"
     "  --chunk N        Read in chunks of N bytes (implies --binary)\n"
     "  --pkcs1          Use PKCS#1 encoding\n"
     "  --mct-server     Run a monte carlo test server\n"
     "  --loop           Enable random loop mode\n"
     "  --progress       Print pogress indicators\n"
     "  --help           Print this text\n"
     "With no FILE, or when FILE is -, read standard input.\n"
     "Report bugs to " PACKAGE_BUGREPORT ".\n" , stdout);
  exit (0);
}

int
main (int argc, char **argv)
{
  int last_argc = -1;
  gpg_error_t err;
  int no_fips = 0;
  int progress = 0;
  int use_pkcs1 = 0;
  const char *mode_string;
  const char *key_string = NULL;
  const char *iv_string = NULL;
  const char *dt_string = NULL;
  const char *algo_string = NULL;
  const char *keysize_string = NULL;
  const char *signature_string = NULL;
  FILE *input;
  void *data;
  size_t datalen;
  size_t chunksize = 0;
  int mct_server = 0;


  if (argc)
    { argc--; argv++; }

  while (argc && last_argc != argc )
    {
      last_argc = argc;
      if (!strcmp (*argv, "--"))
        {
          argc--; argv++;
          break;
        }
      else if (!strcmp (*argv, "--help"))
        {
          usage (1);
        }
      else if (!strcmp (*argv, "--version"))
        {
          fputs (PGM " (Libgcrypt) " PACKAGE_VERSION "\n", stdout);
          exit (0);
        }
      else if (!strcmp (*argv, "--verbose"))
        {
          verbose++;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--binary"))
        {
          binary_input = binary_output = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--no-fips"))
        {
          no_fips++;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--loop"))
        {
          loop_mode = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--progress"))
        {
          progress = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--key"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          key_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--iv"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          iv_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--dt"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          dt_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--algo"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          algo_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--keysize"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          keysize_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--signature"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          signature_string = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--chunk"))
        {
          argc--; argv++;
          if (!argc)
            usage (0);
          chunksize = atoi (*argv);
          binary_input = binary_output = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--pkcs1"))
        {
          use_pkcs1 = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--mct-server"))
        {
          mct_server = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--standalone"))
        {
          standalone_mode = 1;
          argc--; argv++;
        }
    }          

  if (!argc || argc > 2)
    usage (0);
  mode_string = *argv;

  if (!strcmp (mode_string, "rsa-derive"))
    binary_input = 1;

  if (argc == 2 && strcmp (argv[1], "-"))
    {
      input = fopen (argv[1], binary_input? "rb":"r");
      if (!input)
        die ("can't open `%s': %s\n", argv[1], strerror (errno));
    }
  else
    input = stdin;

#ifndef HAVE_W32_SYSTEM
  if (loop_mode)
    signal (SIGPIPE, SIG_IGN);
#endif

  if (verbose)
    fprintf (stderr, PGM ": started (mode=%s)\n", mode_string);

  gcry_control (GCRYCTL_SET_VERBOSITY, (int)verbose);
  if (!no_fips)
    gcry_control (GCRYCTL_FORCE_FIPS_MODE, 0);
  if (!gcry_check_version ("1.4.3"))
    die ("Libgcrypt is not sufficient enough\n");
  if (verbose)
    fprintf (stderr, PGM ": using Libgcrypt %s\n", gcry_check_version (NULL));
  if (no_fips)
    gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);

  /* Most operations need some input data.  */
  if (!chunksize
      && !mct_server
      && strcmp (mode_string, "random")
      && strcmp (mode_string, "rsa-gen")
      && strcmp (mode_string, "dsa-gen") )
    {
      data = read_file (input, !binary_input, &datalen);
      if (!data)
        die ("error reading%s input\n", binary_input?"":" and decoding");
      if (verbose)
        fprintf (stderr, PGM ": %u bytes of input data\n", 
                 (unsigned int)datalen);
    }
  else
    {
      data = NULL;
      datalen = 0;
    }


  if (!strcmp (mode_string, "encrypt") || !strcmp (mode_string, "decrypt"))
    {
      int cipher_algo, cipher_mode;
      void  *iv_buffer = NULL;
      void *key_buffer = NULL;
      size_t iv_buflen,  key_buflen;

      if (!algo_string)
        die ("option --algo is required in this mode\n");
      cipher_algo = map_openssl_cipher_name (algo_string, &cipher_mode);
      if (!cipher_algo)
        die ("cipher algorithm `%s' is not supported\n", algo_string);
      if (mct_server)
        {
          int iterations;

          for (;;)
            {
              gcry_free (key_buffer); key_buffer = NULL;
              gcry_free (iv_buffer); iv_buffer = NULL;
              gcry_free (data); data = NULL;
              if (!(key_buffer = read_textline (input)))
                {
                  if (feof (input))
                    break;
                  die ("no version info in input\n");
                }
              if (atoi (key_buffer) != 1)
                die ("unsupported input version %s\n", key_buffer);
              gcry_free (key_buffer);
              if (!(key_buffer = read_textline (input)))
                die ("no iteration count in input\n");
              iterations = atoi (key_buffer);
              gcry_free (key_buffer);
              if (!(key_buffer = read_hexline (input, &key_buflen)))
                die ("no key in input\n");
              if (!(iv_buffer = read_hexline (input, &iv_buflen)))
                die ("no IV in input\n");
              if (!(data = read_hexline (input, &datalen)))
                die ("no data in input\n");
              skip_to_empty_line (input);
              
              run_cipher_mct_loop ((*mode_string == 'e'),
                                   cipher_algo, cipher_mode,
                                   iv_buffer, iv_buflen,
                                   key_buffer, key_buflen,
                                   data, datalen, iterations);
            }
        }
      else
        {
          if (cipher_mode != GCRY_CIPHER_MODE_ECB)
            {
              if (!iv_string)
                die ("option --iv is required in this mode\n");
              iv_buffer = hex2buffer (iv_string, &iv_buflen);
              if (!iv_buffer)
                die ("invalid value for IV\n");
            }
          else
            {
              iv_buffer = NULL;
              iv_buflen = 0;
            }
          if (!key_string)
            die ("option --key is required in this mode\n");
          key_buffer = hex2buffer (key_string, &key_buflen);
          if (!key_buffer)
            die ("invalid value for KEY\n");

          run_encrypt_decrypt ((*mode_string == 'e'),
                               cipher_algo, cipher_mode,
                               iv_buffer, iv_buflen,
                               key_buffer, key_buflen,
                               data, data? datalen:chunksize, input);
        }
      gcry_free (key_buffer);
      gcry_free (iv_buffer);
    }
  else if (!strcmp (mode_string, "digest"))
    {
      int algo;

      if (!algo_string)
        die ("option --algo is required in this mode\n");
      algo = gcry_md_map_name (algo_string);
      if (!algo)
        die ("digest algorithm `%s' is not supported\n", algo_string);
      if (!data)
        die ("no data available (do not use --chunk)\n");

      run_digest (algo, data, datalen);
    }
  else if (!strcmp (mode_string, "random"))
    {
      void *context;
      unsigned char key[16];
      unsigned char seed[16];
      unsigned char dt[16];
      unsigned char buffer[16];
      size_t count = 0;

      if (hex2bin (key_string, key, 16) < 0 )
        die ("value for --key are not 32 hex digits\n");
      if (hex2bin (iv_string, seed, 16) < 0 )
        die ("value for --iv are not 32 hex digits\n");
      if (hex2bin (dt_string, dt, 16) < 0 )
        die ("value for --dt are not 32 hex digits\n");

      /* The flag value 1 disables the dup check, so that the RNG
         returns all generated data.  */
      err = init_external_rng_test (&context, 1, key, 16, seed, 16, dt, 16);
      if (err)
        die ("init external RNG test failed: %s\n", gpg_strerror (err));

      do 
        {
          err = run_external_rng_test (context, buffer, sizeof buffer);
          if (err)
            die ("running external RNG test failed: %s\n", gpg_strerror (err));
          print_buffer (buffer, sizeof buffer);
          if (progress)
            {
              if (!(++count % 1000))
                fprintf (stderr, PGM ": %lu random bytes so far\n", 
                         (unsigned long int)count * sizeof buffer);
            }
        }
      while (loop_mode);
      
      if (progress)
        fprintf (stderr, PGM ": %lu random bytes\n",
                         (unsigned long int)count * sizeof buffer);

      deinit_external_rng_test (context);
    }
  else if (!strcmp (mode_string, "hmac-sha"))
    {
      int algo;
      void  *key_buffer;
      size_t key_buflen;

      if (!data)
        die ("no data available (do not use --chunk)\n");
      if (!algo_string)
        die ("option --algo is required in this mode\n");
      switch (atoi (algo_string))
        {
        case 1:   algo = GCRY_MD_SHA1; break;
        case 224: algo = GCRY_MD_SHA224; break;
        case 256: algo = GCRY_MD_SHA256; break;
        case 384: algo = GCRY_MD_SHA384; break;
        case 512: algo = GCRY_MD_SHA512; break;
        default:  algo = 0; break;
        }
      if (!algo)
        die ("no digest algorithm found for hmac type `%s'\n", algo_string);
      if (!key_string)
        die ("option --key is required in this mode\n");
      key_buffer = hex2buffer (key_string, &key_buflen);
      if (!key_buffer)
        die ("invalid value for KEY\n");

      run_hmac (algo, key_buffer, key_buflen, data, datalen);

      gcry_free (key_buffer);
    }
  else if (!strcmp (mode_string, "rsa-derive"))
    {
      if (!data)
        die ("no data available (do not use --chunk)\n");
      run_rsa_derive (data, datalen);
    }
  else if (!strcmp (mode_string, "rsa-gen"))
    {
      int keysize;
      
      if (!binary_output)
        base64_output = 1;

      keysize = keysize_string? atoi (keysize_string) : 0;
      if (keysize < 128 || keysize > 16384)
        die ("invalid keysize specified; needs to be 128 .. 16384\n");
      run_rsa_gen (keysize, 65537);
    }
  else if (!strcmp (mode_string, "rsa-sign"))
    {
      int algo;

      if (!key_string)
        die ("option --key is required in this mode\n");
      if (access (key_string, R_OK))
        die ("option --key needs to specify an existing keyfile\n");
      if (!algo_string)
        die ("option --algo is required in this mode\n");
      algo = gcry_md_map_name (algo_string);
      if (!algo)
        die ("digest algorithm `%s' is not supported\n", algo_string);
      if (!data)
        die ("no data available (do not use --chunk)\n");

      run_rsa_sign (data, datalen, algo, use_pkcs1, key_string);

    }
  else if (!strcmp (mode_string, "rsa-verify"))
    {
      int algo;

      if (!key_string)
        die ("option --key is required in this mode\n");
      if (access (key_string, R_OK))
        die ("option --key needs to specify an existing keyfile\n");
      if (!algo_string)
        die ("option --algo is required in this mode\n");
      algo = gcry_md_map_name (algo_string);
      if (!algo)
        die ("digest algorithm `%s' is not supported\n", algo_string);
      if (!data)
        die ("no data available (do not use --chunk)\n");
      if (!signature_string)
        die ("option --signature is required in this mode\n");
      if (access (signature_string, R_OK))
        die ("option --signature needs to specify an existing file\n");

      run_rsa_verify (data, datalen, algo, use_pkcs1, key_string,
                      signature_string);

    }
  else if (!strcmp (mode_string, "dsa-pqg-gen"))
    {
      int keysize;
      
      keysize = keysize_string? atoi (keysize_string) : 0;
      if (keysize < 1024 || keysize > 3072)
        die ("invalid keysize specified; needs to be 1024 .. 3072\n");
      run_dsa_pqg_gen (keysize, datalen? data:NULL, datalen);
    }
  else if (!strcmp (mode_string, "dsa-gen"))
    {
      int keysize;
      
      keysize = keysize_string? atoi (keysize_string) : 0;
      if (keysize < 1024 || keysize > 3072)
        die ("invalid keysize specified; needs to be 1024 .. 3072\n");
      if (!key_string)
        die ("option --key is required in this mode\n");
      run_dsa_gen (keysize, key_string);
    }
  else if (!strcmp (mode_string, "dsa-sign"))
    {
      if (!key_string)
        die ("option --key is required in this mode\n");
      if (access (key_string, R_OK))
        die ("option --key needs to specify an existing keyfile\n");
      if (!data)
        die ("no data available (do not use --chunk)\n");

      run_dsa_sign (data, datalen, key_string);
    }
  else if (!strcmp (mode_string, "dsa-verify"))
    {
      if (!key_string)
        die ("option --key is required in this mode\n");
      if (access (key_string, R_OK))
        die ("option --key needs to specify an existing keyfile\n");
      if (!data)
        die ("no data available (do not use --chunk)\n");
      if (!signature_string)
        die ("option --signature is required in this mode\n");
      if (access (signature_string, R_OK))
        die ("option --signature needs to specify an existing file\n");

      run_dsa_verify (data, datalen, key_string, signature_string);
    }
  else
    usage (0);

  gcry_free (data);

  /* Because Libgcrypt does not enforce FIPS mode in all cases we let
     the process die if Libgcrypt is not anymore in FIPS mode after
     the actual operation.  */
  if (!no_fips && !gcry_fips_mode_active ())
    die ("FIPS mode is not anymore active\n");

  if (verbose)
    fputs (PGM ": ready\n", stderr);

  return 0;
}

