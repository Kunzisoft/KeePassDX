/* sha1.c - SHA1 hash function
 * Copyright (C) 1998, 2001, 2002, 2003, 2008 Free Software Foundation, Inc.
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


/*  Test vectors:
 *
 *  "abc"
 *  A999 3E36 4706 816A BA3E  2571 7850 C26C 9CD0 D89D
 *
 *  "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
 *  8498 3E44 1C3B D26E BAAE  4AA1 F951 29E5 E546 70F1
 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef HAVE_STDINT_H 
# include <stdint.h>
#endif

#include "g10lib.h"
#include "memory.h"
#include "bithelp.h"
#include "cipher.h"
#include "hash-common.h"


/* A macro to test whether P is properly aligned for an u32 type.
   Note that config.h provides a suitable replacement for uintptr_t if
   it does not exist in stdint.h.  */
/* #if __GNUC__ >= 2 */
/* # define U32_ALIGNED_P(p) (!(((uintptr_t)p) % __alignof__ (u32))) */
/* #else */
/* # define U32_ALIGNED_P(p) (!(((uintptr_t)p) % sizeof (u32))) */
/* #endif */

#define TRANSFORM(x,d,n) transform ((x), (d), (n))


typedef struct 
{
  u32           h0,h1,h2,h3,h4;
  u32           nblocks;
  unsigned char buf[64];
  int           count;
} SHA1_CONTEXT;



static void
sha1_init (void *context)
{
  SHA1_CONTEXT *hd = context;

  hd->h0 = 0x67452301;
  hd->h1 = 0xefcdab89;
  hd->h2 = 0x98badcfe;
  hd->h3 = 0x10325476;
  hd->h4 = 0xc3d2e1f0;
  hd->nblocks = 0;
  hd->count = 0;
}


/* Round function macros. */
#define K1  0x5A827999L
#define K2  0x6ED9EBA1L
#define K3  0x8F1BBCDCL
#define K4  0xCA62C1D6L
#define F1(x,y,z)   ( z ^ ( x & ( y ^ z ) ) )
#define F2(x,y,z)   ( x ^ y ^ z )
#define F3(x,y,z)   ( ( x & y ) | ( z & ( x | y ) ) )
#define F4(x,y,z)   ( x ^ y ^ z )
#define M(i) ( tm =    x[ i    &0x0f]  \
                     ^ x[(i-14)&0x0f]  \
	 	     ^ x[(i-8) &0x0f]  \
                     ^ x[(i-3) &0x0f], \
                     (x[i&0x0f] = rol(tm, 1)))
#define R(a,b,c,d,e,f,k,m)  do { e += rol( a, 5 )     \
	                              + f( b, c, d )  \
		 		      + k	      \
			 	      + m;	      \
				 b = rol( b, 30 );    \
			       } while(0)


/*
 * Transform NBLOCKS of each 64 bytes (16 32-bit words) at DATA.
 */
static void
transform (SHA1_CONTEXT *hd, const unsigned char *data, size_t nblocks)
{
  register u32 a, b, c, d, e; /* Local copies of the chaining variables.  */
  register u32 tm;            /* Helper.  */
  u32 x[16];                  /* The array we work on. */
  
  /* Loop over all blocks.  */
  for ( ;nblocks; nblocks--)
    {
#ifdef WORDS_BIGENDIAN
      memcpy (x, data, 64);
      data += 64;
#else
      {
        int i;
        unsigned char *p;

        for(i=0, p=(unsigned char*)x; i < 16; i++, p += 4 )
          {
            p[3] = *data++;
            p[2] = *data++;
            p[1] = *data++;
            p[0] = *data++;
          }
      }
#endif
      /* Get the values of the chaining variables. */
      a = hd->h0;
      b = hd->h1;
      c = hd->h2;
      d = hd->h3;
      e = hd->h4;

      /* Transform. */
      R( a, b, c, d, e, F1, K1, x[ 0] );
      R( e, a, b, c, d, F1, K1, x[ 1] );
      R( d, e, a, b, c, F1, K1, x[ 2] );
      R( c, d, e, a, b, F1, K1, x[ 3] );
      R( b, c, d, e, a, F1, K1, x[ 4] );
      R( a, b, c, d, e, F1, K1, x[ 5] );
      R( e, a, b, c, d, F1, K1, x[ 6] );
      R( d, e, a, b, c, F1, K1, x[ 7] );
      R( c, d, e, a, b, F1, K1, x[ 8] );
      R( b, c, d, e, a, F1, K1, x[ 9] );
      R( a, b, c, d, e, F1, K1, x[10] );
      R( e, a, b, c, d, F1, K1, x[11] );
      R( d, e, a, b, c, F1, K1, x[12] );
      R( c, d, e, a, b, F1, K1, x[13] );
      R( b, c, d, e, a, F1, K1, x[14] );
      R( a, b, c, d, e, F1, K1, x[15] );
      R( e, a, b, c, d, F1, K1, M(16) );
      R( d, e, a, b, c, F1, K1, M(17) );
      R( c, d, e, a, b, F1, K1, M(18) );
      R( b, c, d, e, a, F1, K1, M(19) );
      R( a, b, c, d, e, F2, K2, M(20) );
      R( e, a, b, c, d, F2, K2, M(21) );
      R( d, e, a, b, c, F2, K2, M(22) );
      R( c, d, e, a, b, F2, K2, M(23) );
      R( b, c, d, e, a, F2, K2, M(24) );
      R( a, b, c, d, e, F2, K2, M(25) );
      R( e, a, b, c, d, F2, K2, M(26) );
      R( d, e, a, b, c, F2, K2, M(27) );
      R( c, d, e, a, b, F2, K2, M(28) );
      R( b, c, d, e, a, F2, K2, M(29) );
      R( a, b, c, d, e, F2, K2, M(30) );
      R( e, a, b, c, d, F2, K2, M(31) );
      R( d, e, a, b, c, F2, K2, M(32) );
      R( c, d, e, a, b, F2, K2, M(33) );
      R( b, c, d, e, a, F2, K2, M(34) );
      R( a, b, c, d, e, F2, K2, M(35) );
      R( e, a, b, c, d, F2, K2, M(36) );
      R( d, e, a, b, c, F2, K2, M(37) );
      R( c, d, e, a, b, F2, K2, M(38) );
      R( b, c, d, e, a, F2, K2, M(39) );
      R( a, b, c, d, e, F3, K3, M(40) );
      R( e, a, b, c, d, F3, K3, M(41) );
      R( d, e, a, b, c, F3, K3, M(42) );
      R( c, d, e, a, b, F3, K3, M(43) );
      R( b, c, d, e, a, F3, K3, M(44) );
      R( a, b, c, d, e, F3, K3, M(45) );
      R( e, a, b, c, d, F3, K3, M(46) );
      R( d, e, a, b, c, F3, K3, M(47) );
      R( c, d, e, a, b, F3, K3, M(48) );
      R( b, c, d, e, a, F3, K3, M(49) );
      R( a, b, c, d, e, F3, K3, M(50) );
      R( e, a, b, c, d, F3, K3, M(51) );
      R( d, e, a, b, c, F3, K3, M(52) );
      R( c, d, e, a, b, F3, K3, M(53) );
      R( b, c, d, e, a, F3, K3, M(54) );
      R( a, b, c, d, e, F3, K3, M(55) );
      R( e, a, b, c, d, F3, K3, M(56) );
      R( d, e, a, b, c, F3, K3, M(57) );
      R( c, d, e, a, b, F3, K3, M(58) );
      R( b, c, d, e, a, F3, K3, M(59) );
      R( a, b, c, d, e, F4, K4, M(60) );
      R( e, a, b, c, d, F4, K4, M(61) );
      R( d, e, a, b, c, F4, K4, M(62) );
      R( c, d, e, a, b, F4, K4, M(63) );
      R( b, c, d, e, a, F4, K4, M(64) );
      R( a, b, c, d, e, F4, K4, M(65) );
      R( e, a, b, c, d, F4, K4, M(66) );
      R( d, e, a, b, c, F4, K4, M(67) );
      R( c, d, e, a, b, F4, K4, M(68) );
      R( b, c, d, e, a, F4, K4, M(69) );
      R( a, b, c, d, e, F4, K4, M(70) );
      R( e, a, b, c, d, F4, K4, M(71) );
      R( d, e, a, b, c, F4, K4, M(72) );
      R( c, d, e, a, b, F4, K4, M(73) );
      R( b, c, d, e, a, F4, K4, M(74) );
      R( a, b, c, d, e, F4, K4, M(75) );
      R( e, a, b, c, d, F4, K4, M(76) );
      R( d, e, a, b, c, F4, K4, M(77) );
      R( c, d, e, a, b, F4, K4, M(78) );
      R( b, c, d, e, a, F4, K4, M(79) );

      /* Update the chaining variables. */
      hd->h0 += a;
      hd->h1 += b;
      hd->h2 += c;
      hd->h3 += d;
      hd->h4 += e;
    }
}


/* Update the message digest with the contents
 * of INBUF with length INLEN.
 */
static void
sha1_write( void *context, const void *inbuf_arg, size_t inlen)
{
  const unsigned char *inbuf = inbuf_arg;
  SHA1_CONTEXT *hd = context;
  size_t nblocks;

  if (hd->count == 64)  /* Flush the buffer. */
    {
      TRANSFORM( hd, hd->buf, 1 );
      _gcry_burn_stack (88+4*sizeof(void*));
      hd->count = 0;
      hd->nblocks++;
    }
  if (!inbuf)
    return;

  if (hd->count)
    {
      for (; inlen && hd->count < 64; inlen--)
        hd->buf[hd->count++] = *inbuf++;
      sha1_write (hd, NULL, 0);
      if (!inlen)
        return;
    }

  nblocks = inlen / 64;
  if (nblocks)
    {
      TRANSFORM (hd, inbuf, nblocks);
      hd->count = 0;
      hd->nblocks += nblocks;
      inlen -= nblocks * 64;
      inbuf += nblocks * 64;
    }
  _gcry_burn_stack (88+4*sizeof(void*));

  /* Save remaining bytes.  */
  for (; inlen && hd->count < 64; inlen--)
    hd->buf[hd->count++] = *inbuf++;
}


/* The routine final terminates the computation and
 * returns the digest.
 * The handle is prepared for a new cycle, but adding bytes to the
 * handle will the destroy the returned buffer.
 * Returns: 20 bytes representing the digest.
 */

static void
sha1_final(void *context)
{
  SHA1_CONTEXT *hd = context;
  
  u32 t, msb, lsb;
  unsigned char *p;

  sha1_write(hd, NULL, 0); /* flush */;

  t = hd->nblocks;
  /* multiply by 64 to make a byte count */
  lsb = t << 6;
  msb = t >> 26;
  /* add the count */
  t = lsb;
  if( (lsb += hd->count) < t )
    msb++;
  /* multiply by 8 to make a bit count */
  t = lsb;
  lsb <<= 3;
  msb <<= 3;
  msb |= t >> 29;

  if( hd->count < 56 )  /* enough room */
    {
      hd->buf[hd->count++] = 0x80; /* pad */
      while( hd->count < 56 )
        hd->buf[hd->count++] = 0;  /* pad */
    }
  else  /* need one extra block */
    {
      hd->buf[hd->count++] = 0x80; /* pad character */
      while( hd->count < 64 )
        hd->buf[hd->count++] = 0;
      sha1_write(hd, NULL, 0);  /* flush */;
      memset(hd->buf, 0, 56 ); /* fill next block with zeroes */
    }
  /* append the 64 bit count */
  hd->buf[56] = msb >> 24;
  hd->buf[57] = msb >> 16;
  hd->buf[58] = msb >>  8;
  hd->buf[59] = msb	   ;
  hd->buf[60] = lsb >> 24;
  hd->buf[61] = lsb >> 16;
  hd->buf[62] = lsb >>  8;
  hd->buf[63] = lsb	   ;
  TRANSFORM( hd, hd->buf, 1 );
  _gcry_burn_stack (88+4*sizeof(void*));

  p = hd->buf;
#ifdef WORDS_BIGENDIAN
#define X(a) do { *(u32*)p = hd->h##a ; p += 4; } while(0)
#else /* little endian */
#define X(a) do { *p++ = hd->h##a >> 24; *p++ = hd->h##a >> 16;	 \
                  *p++ = hd->h##a >> 8; *p++ = hd->h##a; } while(0)
#endif
  X(0);
  X(1);
  X(2);
  X(3);
  X(4);
#undef X

}

static unsigned char *
sha1_read( void *context )
{
  SHA1_CONTEXT *hd = context;

  return hd->buf;
}

/****************
 * Shortcut functions which puts the hash value of the supplied buffer
 * into outbuf which must have a size of 20 bytes.
 */
void
_gcry_sha1_hash_buffer (void *outbuf, const void *buffer, size_t length)
{
  SHA1_CONTEXT hd;

  sha1_init (&hd);
  sha1_write (&hd, buffer, length);
  sha1_final (&hd);
  memcpy (outbuf, hd.buf, 20);
}



/* 
     Self-test section.
 */


static gpg_err_code_t
selftests_sha1 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  what = "short string";
  errtxt = _gcry_hash_selftest_check_one
    (GCRY_MD_SHA1, 0, 
     "abc", 3,
     "\xA9\x99\x3E\x36\x47\x06\x81\x6A\xBA\x3E"
     "\x25\x71\x78\x50\xC2\x6C\x9C\xD0\xD8\x9D", 20);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "long string";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA1, 0, 
         "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", 56,
         "\x84\x98\x3E\x44\x1C\x3B\xD2\x6E\xBA\xAE"
         "\x4A\xA1\xF9\x51\x29\xE5\xE5\x46\x70\xF1", 20);
      if (errtxt)
        goto failed;
      
      what = "one million \"a\"";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA1, 1,
         NULL, 0,
         "\x34\xAA\x97\x3C\xD4\xC4\xDA\xA4\xF6\x1E"
         "\xEB\x2B\xDB\xAD\x27\x31\x65\x34\x01\x6F", 20);
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("digest", GCRY_MD_SHA1, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


/* Run a full self-test for ALGO and return 0 on success.  */
static gpg_err_code_t
run_selftests (int algo, int extended, selftest_report_func_t report)
{
  gpg_err_code_t ec;

  switch (algo)
    {
    case GCRY_MD_SHA1:
      ec = selftests_sha1 (extended, report);
      break;
    default:
      ec = GPG_ERR_DIGEST_ALGO;
      break;
        
    }
  return ec;
}




static unsigned char asn[15] = /* Object ID is 1.3.14.3.2.26 */
  { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03,
    0x02, 0x1a, 0x05, 0x00, 0x04, 0x14 };

static gcry_md_oid_spec_t oid_spec_sha1[] =
  {
    /* iso.member-body.us.rsadsi.pkcs.pkcs-1.5 (sha1WithRSAEncryption) */
    { "1.2.840.113549.1.1.5" },
    /* iso.member-body.us.x9-57.x9cm.3 (dsaWithSha1)*/
    { "1.2.840.10040.4.3" },
    /* from NIST's OIW  (sha1) */
    { "1.3.14.3.2.26" },
    /* from NIST OIW (sha-1WithRSAEncryption) */
    { "1.3.14.3.2.29" },
    /* iso.member-body.us.ansi-x9-62.signatures.ecdsa-with-sha1 */
    { "1.2.840.10045.4.1" },
    { NULL },
  };

gcry_md_spec_t _gcry_digest_spec_sha1 =
  {
    "SHA1", asn, DIM (asn), oid_spec_sha1, 20,
    sha1_init, sha1_write, sha1_final, sha1_read,
    sizeof (SHA1_CONTEXT)
  };
md_extra_spec_t _gcry_digest_extraspec_sha1 = 
  {
    run_selftests
  };

