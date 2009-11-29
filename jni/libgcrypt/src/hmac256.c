/* hmac256.c - Standalone HMAC implementation
 * Copyright (C) 2003, 2006, 2008  Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Libgcrypt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/* 
    This is a standalone HMAC-SHA-256 implementation based on the code
    from ../cipher/sha256.c.  It is a second implementation to allow
    comparing against the standard implementations and to be used for
    internal consistency checks.  It should not be used for sensitive
    data because no mechanisms to clear the stack etc are used.

    This module may be used standalone and requires only a few
    standard definitions to be provided in a config.h file.

    Types:

     u32 - unsigned 32 bit type.

    Constants:

     WORDS_BIGENDIAN       Defined to 1 on big endian systems.      
     inline                If defined, it should yield the keyword used
                           to inline a function.
     HAVE_U32_TYPEDEF      Defined if the u32 type is available.
     SIZEOF_UNSIGNED_INT   Defined to the size in bytes of an unsigned int.
     SIZEOF_UNSIGNED_LONG  Defined to the size in bytes of an unsigned long.

     STANDALONE            Compile a test driver similar to the
                           sha1sum tool.  This driver uses a self-test
                           identically to the one used by Libcgrypt
                           for testing this included module.
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <errno.h>
#if defined(__WIN32) && defined(STANDALONE)
# include <fcntl.h> /* We need setmode().  */
#endif

#include "hmac256.h"



#ifndef HAVE_U32_TYPEDEF
# undef u32 /* Undef a possible macro with that name.  */
# if SIZEOF_UNSIGNED_INT == 4
   typedef unsigned int u32;
# elif SIZEOF_UNSIGNED_LONG == 4
   typedef unsigned long u32;
# else
#  error no typedef for u32
# endif
# define HAVE_U32_TYPEDEF
#endif




/* The context used by this module.  */
struct hmac256_context
{
  u32  h0, h1, h2, h3, h4, h5, h6, h7;
  u32  nblocks;
  int  count;
  int  finalized:1;
  int  use_hmac:1; 
  unsigned char buf[64];
  unsigned char opad[64];
};


/* Rotate a 32 bit word.  */
#if defined(__GNUC__) && defined(__i386__)
static inline u32
ror(u32 x, int n)
{
	__asm__("rorl %%cl,%0"
		:"=r" (x)
		:"0" (x),"c" (n));
	return x;
}
#else
#define ror(x,n) ( ((x) >> (n)) | ((x) << (32-(n))) )
#endif

#define my_wipememory2(_ptr,_set,_len) do { \
              volatile char *_vptr=(volatile char *)(_ptr); \
              size_t _vlen=(_len); \
              while(_vlen) { *_vptr=(_set); _vptr++; _vlen--; } \
                  } while(0)
#define my_wipememory(_ptr,_len) my_wipememory2(_ptr,0,_len)




/* 
    The SHA-256 core: Transform the message X which consists of 16
    32-bit-words. See FIPS 180-2 for details.
 */
static void
transform (hmac256_context_t hd, const void *data_arg)
{
  const unsigned char *data = data_arg;

#define Cho(x,y,z) (z ^ (x & (y ^ z)))      /* (4.2) same as SHA-1's F1 */
#define Maj(x,y,z) ((x & y) | (z & (x|y)))  /* (4.3) same as SHA-1's F3 */
#define Sum0(x) (ror ((x), 2) ^ ror ((x), 13) ^ ror ((x), 22))  /* (4.4) */
#define Sum1(x) (ror ((x), 6) ^ ror ((x), 11) ^ ror ((x), 25))  /* (4.5) */
#define S0(x) (ror ((x), 7) ^ ror ((x), 18) ^ ((x) >> 3))       /* (4.6) */
#define S1(x) (ror ((x), 17) ^ ror ((x), 19) ^ ((x) >> 10))     /* (4.7) */
#define R(a,b,c,d,e,f,g,h,k,w) do                                 \
          {                                                       \
            t1 = (h) + Sum1((e)) + Cho((e),(f),(g)) + (k) + (w);  \
            t2 = Sum0((a)) + Maj((a),(b),(c));                    \
            h = g;                                                \
            g = f;                                                \
            f = e;                                                \
            e = d + t1;                                           \
            d = c;                                                \
            c = b;                                                \
            b = a;                                                \
            a = t1 + t2;                                          \
          } while (0)
 
  static const u32 K[64] = 
    {
      0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
      0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
      0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
      0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 
      0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
      0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
      0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
      0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
      0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
      0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
      0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
      0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
      0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
      0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
      0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
      0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

  u32 a, b, c, d, e, f, g, h, t1, t2;
  u32 x[16];
  u32 w[64];
  int i;
  
  a = hd->h0;
  b = hd->h1;
  c = hd->h2;
  d = hd->h3;
  e = hd->h4;
  f = hd->h5;
  g = hd->h6;
  h = hd->h7;
  
#ifdef WORDS_BIGENDIAN
  memcpy (x, data, 64);
#else /*!WORDS_BIGENDIAN*/
  { 
    unsigned char *p2;
  
    for (i=0, p2=(unsigned char*)x; i < 16; i++, p2 += 4 ) 
      {
        p2[3] = *data++;
        p2[2] = *data++;
        p2[1] = *data++;
        p2[0] = *data++;
      }
  }
#endif /*!WORDS_BIGENDIAN*/

  for (i=0; i < 16; i++)
    w[i] = x[i];
  for (; i < 64; i++)
    w[i] = S1(w[i-2]) + w[i-7] + S0(w[i-15]) + w[i-16];

  for (i=0; i < 64; i++)
    R(a,b,c,d,e,f,g,h,K[i],w[i]);

  hd->h0 += a;
  hd->h1 += b;
  hd->h2 += c;
  hd->h3 += d;
  hd->h4 += e;
  hd->h5 += f;
  hd->h6 += g;
  hd->h7 += h;
}
#undef Cho
#undef Maj
#undef Sum0
#undef Sum1
#undef S0
#undef S1
#undef R


/*  Finalize the current SHA256 calculation.  */
static void
finalize (hmac256_context_t hd)
{
  u32 t, msb, lsb;
  unsigned char *p;
  
  if (hd->finalized)
    return; /* Silently ignore a finalized context.  */

  _gcry_hmac256_update (hd, NULL, 0); /* Flush.  */

  t = hd->nblocks;
  /* Multiply by 64 to make a byte count. */
  lsb = t << 6;
  msb = t >> 26;
  /* Add the count. */
  t = lsb;
  if ((lsb += hd->count) < t)
    msb++;
  /* Multiply by 8 to make a bit count. */
  t = lsb;
  lsb <<= 3;
  msb <<= 3;
  msb |= t >> 29;

  if (hd->count < 56)
    { /* Enough room.  */
      hd->buf[hd->count++] = 0x80; /* pad */
      while (hd->count < 56)
        hd->buf[hd->count++] = 0;  /* pad */
    }
  else
    { /* Need one extra block. */
      hd->buf[hd->count++] = 0x80; /* pad character */
      while (hd->count < 64)
        hd->buf[hd->count++] = 0;
      _gcry_hmac256_update (hd, NULL, 0);  /* Flush.  */;
      memset (hd->buf, 0, 56 ); /* Zero out next next block.  */
    }
  /* Append the 64 bit count. */
  hd->buf[56] = msb >> 24;
  hd->buf[57] = msb >> 16;
  hd->buf[58] = msb >>  8;
  hd->buf[59] = msb;
  hd->buf[60] = lsb >> 24;
  hd->buf[61] = lsb >> 16;
  hd->buf[62] = lsb >>  8;
  hd->buf[63] = lsb;
  transform (hd, hd->buf);

  /* Store the digest into hd->buf.  */
  p = hd->buf;
#define X(a) do { *p++ = hd->h##a >> 24; *p++ = hd->h##a >> 16;	 \
		  *p++ = hd->h##a >> 8; *p++ = hd->h##a; } while(0)
  X(0);
  X(1);
  X(2);
  X(3);
  X(4);
  X(5);
  X(6);
  X(7);
#undef X
  hd->finalized = 1;
}



/* Create a new context.  On error NULL is returned and errno is set
   appropriately.  If KEY is given the fucntion computes HMAC using
   this key; with KEY given as NULL, a plain SHA-256 digest is
   computed.  */
hmac256_context_t
_gcry_hmac256_new (const void *key, size_t keylen)
{
  hmac256_context_t hd;

  hd = malloc (sizeof *hd);
  if (!hd)
    return NULL;

  hd->h0 = 0x6a09e667;
  hd->h1 = 0xbb67ae85;
  hd->h2 = 0x3c6ef372;
  hd->h3 = 0xa54ff53a;
  hd->h4 = 0x510e527f;
  hd->h5 = 0x9b05688c;
  hd->h6 = 0x1f83d9ab;
  hd->h7 = 0x5be0cd19;
  hd->nblocks = 0;
  hd->count = 0;
  hd->finalized = 0;
  hd->use_hmac = 0;

  if (key)
    {
      int i;
      unsigned char ipad[64];

      memset (ipad, 0, 64);
      memset (hd->opad, 0, 64);
      if (keylen <= 64)
        {
          memcpy (ipad, key, keylen);
          memcpy (hd->opad, key, keylen);
        }
      else
        {
          hmac256_context_t tmphd;
          
          tmphd = _gcry_hmac256_new (NULL, 0);
          if (!tmphd)
            {
              free (hd);
              return NULL;
            }
          _gcry_hmac256_update (tmphd, key, keylen);
          finalize (tmphd);
          memcpy (ipad, tmphd->buf, 32);
          memcpy (hd->opad, tmphd->buf, 32);
          _gcry_hmac256_release (tmphd);
        }
      for (i=0; i < 64; i++) 
        {
          ipad[i] ^= 0x36;
          hd->opad[i] ^= 0x5c;
        }
      hd->use_hmac = 1;
      _gcry_hmac256_update (hd, ipad, 64);
      my_wipememory (ipad, 64);
    }
 
  return hd;
}

/* Release a context created by _gcry_hmac256_new.  CTX may be NULL
   in which case the function does nothing.  */
void
_gcry_hmac256_release (hmac256_context_t ctx)
{
  if (ctx)
    {
      /* Note: We need to take care not to modify errno.  */
      if (ctx->use_hmac)
        my_wipememory (ctx->opad, 64);
      free (ctx);
    }
}


/* Update the message digest with the contents of BUFFER containing
   LENGTH bytes.  */
void
_gcry_hmac256_update (hmac256_context_t hd,
                        const void *buffer, size_t length)
{
  const unsigned char *inbuf = buffer;

  if (hd->finalized)
    return; /* Silently ignore a finalized context.  */

  if (hd->count == 64)
    { 
      /* Flush the buffer. */
      transform (hd, hd->buf);
      hd->count = 0;
      hd->nblocks++;
    }
  if (!inbuf)
    return;  /* Only flushing was requested. */
  if (hd->count)
    {
      for (; length && hd->count < 64; length--)
        hd->buf[hd->count++] = *inbuf++;
      _gcry_hmac256_update (hd, NULL, 0); /* Flush.  */
      if (!length)
        return;
    }


  while (length >= 64)
    {
      transform (hd, inbuf);
      hd->count = 0;
      hd->nblocks++;
      length -= 64;
      inbuf += 64;
    }
  for (; length && hd->count < 64; length--)
    hd->buf[hd->count++] = *inbuf++;
}


/* Finalize an operation and return the digest.  If R_DLEN is not NULL
   the length of the digest will be stored at that address.  The
   returned value is valid as long as the context exists.  On error
   NULL is returned. */
const void *
_gcry_hmac256_finalize (hmac256_context_t hd, size_t *r_dlen)
{
  finalize (hd);
  if (hd->use_hmac)
    {
      hmac256_context_t tmphd;
      
      tmphd = _gcry_hmac256_new (NULL, 0);
      if (!tmphd)
        {
          free (hd);
          return NULL;
        }
      _gcry_hmac256_update (tmphd, hd->opad, 64);
      _gcry_hmac256_update (tmphd, hd->buf, 32);
      finalize (tmphd);
      memcpy (hd->buf, tmphd->buf, 32);
      _gcry_hmac256_release (tmphd);
    }
  if (r_dlen)
    *r_dlen = 32;
  return (void*)hd->buf;
}


/* Convenience function to compute the HMAC-SHA256 of one file.  The
   user needs to provide a buffer RESULT of at least 32 bytes, he
   needs to put the size of the buffer into RESULTSIZE and the
   FILENAME.  KEY and KEYLEN are as described for _gcry_hmac256_new.
   On success the function returns the valid length of the result
   buffer (which will be 32) or -1 on error.  On error ERRNO is set
   appropriate.  */ 
int
_gcry_hmac256_file (void *result, size_t resultsize, const char *filename,
                    const void *key, size_t keylen)
{
  FILE *fp;
  hmac256_context_t hd;
  size_t buffer_size, nread, digestlen;
  char *buffer;
  const unsigned char *digest;
  
  fp = fopen (filename, "rb");
  if (!fp)
    return -1;

  hd = _gcry_hmac256_new (key, keylen);
  if (!hd)
    {
      fclose (fp);
      return -1;
    }

  buffer_size = 32768;
  buffer = malloc (buffer_size);
  if (!buffer)
    {
      fclose (fp);
      _gcry_hmac256_release (hd);
      return -1;
    }

  while ( (nread = fread (buffer, 1, buffer_size, fp)))
    _gcry_hmac256_update (hd, buffer, nread);

  free (buffer);

  if (ferror (fp))
    {
      fclose (fp);
      _gcry_hmac256_release (hd);
      return -1;
    }

  fclose (fp);

  digest = _gcry_hmac256_finalize (hd, &digestlen);
  if (!digest)
    {
      _gcry_hmac256_release (hd);
      return -1;
    }
  
  if (digestlen > resultsize)
    {
      _gcry_hmac256_release (hd);
      errno = EINVAL;
      return -1;
    }
  memcpy (result, digest, digestlen);
  _gcry_hmac256_release (hd);

  return digestlen;
}



#ifdef STANDALONE
static int
selftest (void)
{
  static struct 
  {
    const char * const desc;
    const char * const data;
    const char * const key;
    const unsigned char expect[32];
  } tv[] =
    {
      { "data-28 key-4",
        "what do ya want for nothing?", 
        "Jefe",
	{ 0x5b, 0xdc, 0xc1, 0x46, 0xbf, 0x60, 0x75, 0x4e,
          0x6a, 0x04, 0x24, 0x26, 0x08, 0x95, 0x75, 0xc7,
          0x5a, 0x00, 0x3f, 0x08, 0x9d, 0x27, 0x39, 0x83,
          0x9d, 0xec, 0x58, 0xb9, 0x64, 0xec, 0x38, 0x43 } },

      { "data-9 key-20",
        "Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
        "\x0b\x0b\x0b\x0b",
        { 0xb0, 0x34, 0x4c, 0x61, 0xd8, 0xdb, 0x38, 0x53,
          0x5c, 0xa8, 0xaf, 0xce, 0xaf, 0x0b, 0xf1, 0x2b,
          0x88, 0x1d, 0xc2, 0x00, 0xc9, 0x83, 0x3d, 0xa7,
          0x26, 0xe9, 0x37, 0x6c, 0x2e, 0x32, 0xcf, 0xf7 } },

      { "data-50 key-20",
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa",
        { 0x77, 0x3e, 0xa9, 0x1e, 0x36, 0x80, 0x0e, 0x46,
          0x85, 0x4d, 0xb8, 0xeb, 0xd0, 0x91, 0x81, 0xa7,
          0x29, 0x59, 0x09, 0x8b, 0x3e, 0xf8, 0xc1, 0x22,
          0xd9, 0x63, 0x55, 0x14, 0xce, 0xd5, 0x65, 0xfe } },

      { "data-50 key-26",
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10"
        "\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	{ 0x82, 0x55, 0x8a, 0x38, 0x9a, 0x44, 0x3c, 0x0e,
          0xa4, 0xcc, 0x81, 0x98, 0x99, 0xf2, 0x08, 0x3a,
          0x85, 0xf0, 0xfa, 0xa3, 0xe5, 0x78, 0xf8, 0x07,
          0x7a, 0x2e, 0x3f, 0xf4, 0x67, 0x29, 0x66, 0x5b } },

      { "data-54 key-131",
        "Test Using Larger Than Block-Size Key - Hash Key First",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa",
	{ 0x60, 0xe4, 0x31, 0x59, 0x1e, 0xe0, 0xb6, 0x7f,
          0x0d, 0x8a, 0x26, 0xaa, 0xcb, 0xf5, 0xb7, 0x7f,
          0x8e, 0x0b, 0xc6, 0x21, 0x37, 0x28, 0xc5, 0x14,
          0x05, 0x46, 0x04, 0x0f, 0x0e, 0xe3, 0x7f, 0x54 } },

      { "data-152 key-131",
        "This is a test using a larger than block-size key and a larger "
        "than block-size data. The key needs to be hashed before being "
        "used by the HMAC algorithm.",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa",
	{ 0x9b, 0x09, 0xff, 0xa7, 0x1b, 0x94, 0x2f, 0xcb,
          0x27, 0x63, 0x5f, 0xbc, 0xd5, 0xb0, 0xe9, 0x44,
          0xbf, 0xdc, 0x63, 0x64, 0x4f, 0x07, 0x13, 0x93,
          0x8a, 0x7f, 0x51, 0x53, 0x5c, 0x3a, 0x35, 0xe2 } },

      { NULL }
    };
  int tvidx;
  
  for (tvidx=0; tv[tvidx].desc; tvidx++)
    {
      hmac256_context_t hmachd;
      const unsigned char *digest;
      size_t dlen;

      hmachd = _gcry_hmac256_new (tv[tvidx].key, strlen (tv[tvidx].key));
      if (!hmachd)
        return -1;
      _gcry_hmac256_update (hmachd, tv[tvidx].data, strlen (tv[tvidx].data));
      digest = _gcry_hmac256_finalize (hmachd, &dlen);
      if (!digest)
        {
          _gcry_hmac256_release (hmachd);
          return -1;
        }
      if (dlen != sizeof (tv[tvidx].expect)
          || memcmp (digest, tv[tvidx].expect, sizeof (tv[tvidx].expect)))
        {
          _gcry_hmac256_release (hmachd);
          return -1;
        }
      _gcry_hmac256_release (hmachd);
    }
  
  return 0; /* Succeeded. */
}


int 
main (int argc, char **argv)
{
  const char *pgm;
  int last_argc = -1;
  const char *key;
  size_t keylen;
  FILE *fp;
  hmac256_context_t hd;
  const unsigned char *digest;
  char buffer[4096];
  size_t n, dlen, idx;
  int use_stdin = 0;
  int use_binary = 0;

  assert (sizeof (u32) == 4);
#ifdef __WIN32
  setmode (fileno (stdin), O_BINARY);
#endif

  if (argc)
    {
      pgm = strrchr (*argv, '/');
      if (pgm)
        pgm++;
      else
        pgm = *argv;
      argc--; argv++;
    }
  else
    pgm = "?";

  while (argc && last_argc != argc )
    {
      last_argc = argc;
      if (!strcmp (*argv, "--"))
        {
          argc--; argv++;
          break;
        }
      else if (!strcmp (*argv, "--version"))
        {
          fputs ("hmac256 (Libgcrypt) " VERSION "\n"
                 "Copyright (C) 2008 Free Software Foundation, Inc.\n"
                 "License LGPLv2.1+: GNU LGPL version 2.1 or later "
                 "<http://gnu.org/licenses/old-licenses/lgpl-2.1.html>\n"
                 "This is free software: you are free to change and "
                 "redistribute it.\n"
                 "There is NO WARRANTY, to the extent permitted by law.\n",
                 stdout);
          exit (0);
        }
      else if (!strcmp (*argv, "--binary"))
        {
          argc--; argv++;
          use_binary = 1;
        }
    }          

  if (argc < 1)
    {
      fprintf (stderr, "usage: %s [--binary] key [filename]\n", pgm);
      exit (1);
    }

#ifdef __WIN32
  if (use_binary)
    setmode (fileno (stdout), O_BINARY);
#endif

  key = *argv;
  argc--, argv++;
  keylen = strlen (key);
  use_stdin = !argc;

  if (selftest ())
    {
      fprintf (stderr, "%s: fatal error: self-test failed\n", pgm);
      exit (2);
    }

  for (; argc || use_stdin; argv++, argc--)
    {
      const char *fname = use_stdin? "-" : *argv;
      fp = use_stdin? stdin : fopen (fname, "rb");
      if (!fp)
        {
          fprintf (stderr, "%s: can't open `%s': %s\n", 
                   pgm, fname, strerror (errno));
          exit (1);
        }
      hd = _gcry_hmac256_new (key, keylen);
      if (!hd)
        {
          fprintf (stderr, "%s: can't allocate context: %s\n", 
                   pgm, strerror (errno));
          exit (1);
        }
      while ( (n = fread (buffer, 1, sizeof buffer, fp)))
        _gcry_hmac256_update (hd, buffer, n);
      if (ferror (fp))
        {
          fprintf (stderr, "%s: error reading `%s': %s\n", 
                   pgm, fname, strerror (errno));
          exit (1);
        }
      if (!use_stdin)
        fclose (fp);

      digest = _gcry_hmac256_finalize (hd, &dlen);
      if (!digest)
        {
          fprintf (stderr, "%s: error computing HMAC: %s\n",
                   pgm, strerror (errno));
          exit (1);
        }
      if (use_binary)
        {
          if (fwrite (digest, dlen, 1, stdout) != 1)
            {
              fprintf (stderr, "%s: error writing output: %s\n", 
                       pgm, strerror (errno));
              exit (1);
            }
        }
      else
        {
          for (idx=0; idx < dlen; idx++)
            printf ("%02x", digest[idx]);
          _gcry_hmac256_release (hd);
          if (use_stdin)
            {
              putchar ('\n');
              break;
            }
          printf ("  %s\n", fname);
        }
    }

  return 0;
}
#endif /*STANDALONE*/


/*
Local Variables:
compile-command: "cc -Wall -g -I.. -DSTANDALONE -o hmac256 hmac256.c"
End:
*/
