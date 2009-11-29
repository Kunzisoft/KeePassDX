/* sha256.c - SHA256 hash function
 *	Copyright (C) 2003, 2006, 2008 Free Software Foundation, Inc.
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
    
    "abc"
    SHA224: 23097d22 3405d822 8642a477 bda255b3 2aadbce4 bda0b3f7 e36c9da7
    SHA256: ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad

    "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
    SHA224: 75388b16 512776cc 5dba5da1 fd890150 b0c6455c b4f58b19 52522525
    SHA256: 248d6a61 d20638b8 e5c02693 0c3e6039 a33ce459 64ff2167 f6ecedd4 19db06c1
 
    "a" one million times
    SHA224: 20794655 980c91d8 bbb4c1ea 97618a4b f03f4258 1948b2ee 4ee7ad67
    SHA256: cdc76e5c 9914fb92 81a1c7e2 84d73e67 f1809a48 a497200e 046d39cc c7112cd0

 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "g10lib.h"
#include "memory.h"
#include "bithelp.h"
#include "cipher.h"
#include "hash-common.h"

typedef struct {
  u32  h0,h1,h2,h3,h4,h5,h6,h7;
  u32  nblocks;
  byte buf[64];
  int  count;
} SHA256_CONTEXT;


static void
sha256_init (void *context)
{
  SHA256_CONTEXT *hd = context;

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
}


static void
sha224_init (void *context)
{
  SHA256_CONTEXT *hd = context;

  hd->h0 = 0xc1059ed8;
  hd->h1 = 0x367cd507;
  hd->h2 = 0x3070dd17;
  hd->h3 = 0xf70e5939;
  hd->h4 = 0xffc00b31;
  hd->h5 = 0x68581511;
  hd->h6 = 0x64f98fa7;
  hd->h7 = 0xbefa4fa4;

  hd->nblocks = 0;
  hd->count = 0;
}


/*
  Transform the message X which consists of 16 32-bit-words. See FIPS
  180-2 for details.  */
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
 
static void
transform (SHA256_CONTEXT *hd, const unsigned char *data)
{
  static const u32 K[64] = {
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

  u32 a,b,c,d,e,f,g,h,t1,t2;
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
#else
  { 
    byte *p2;
  
    for (i=0, p2=(byte*)x; i < 16; i++, p2 += 4 ) 
      {
        p2[3] = *data++;
        p2[2] = *data++;
        p2[1] = *data++;
        p2[0] = *data++;
      }
  }
#endif

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


/* Update the message digest with the contents of INBUF with length
  INLEN.  */
static void
sha256_write (void *context, const void *inbuf_arg, size_t inlen)
{
  const unsigned char *inbuf = inbuf_arg;
  SHA256_CONTEXT *hd = context;

  if (hd->count == 64)
    { /* flush the buffer */
      transform (hd, hd->buf);
      _gcry_burn_stack (74*4+32);
      hd->count = 0;
      hd->nblocks++;
    }
  if (!inbuf)
    return;
  if (hd->count)
    {
      for (; inlen && hd->count < 64; inlen--)
        hd->buf[hd->count++] = *inbuf++;
      sha256_write (hd, NULL, 0);
      if (!inlen)
        return;
    }

  while (inlen >= 64)
    {
      transform (hd, inbuf);
      hd->count = 0;
      hd->nblocks++;
      inlen -= 64;
      inbuf += 64;
    }
  _gcry_burn_stack (74*4+32);
  for (; inlen && hd->count < 64; inlen--)
    hd->buf[hd->count++] = *inbuf++;
}


/*
   The routine finally terminates the computation and returns the
   digest.  The handle is prepared for a new cycle, but adding bytes
   to the handle will the destroy the returned buffer.  Returns: 32
   bytes with the message the digest.  */
static void
sha256_final(void *context)
{
  SHA256_CONTEXT *hd = context;
  u32 t, msb, lsb;
  byte *p;
  
  sha256_write (hd, NULL, 0); /* flush */;

  t = hd->nblocks;
  /* multiply by 64 to make a byte count */
  lsb = t << 6;
  msb = t >> 26;
  /* add the count */
  t = lsb;
  if ((lsb += hd->count) < t)
    msb++;
  /* multiply by 8 to make a bit count */
  t = lsb;
  lsb <<= 3;
  msb <<= 3;
  msb |= t >> 29;

  if (hd->count < 56)
    { /* enough room */
      hd->buf[hd->count++] = 0x80; /* pad */
      while (hd->count < 56)
        hd->buf[hd->count++] = 0;  /* pad */
    }
  else
    { /* need one extra block */
      hd->buf[hd->count++] = 0x80; /* pad character */
      while (hd->count < 64)
        hd->buf[hd->count++] = 0;
      sha256_write (hd, NULL, 0);  /* flush */;
      memset (hd->buf, 0, 56 ); /* fill next block with zeroes */
    }
  /* append the 64 bit count */
  hd->buf[56] = msb >> 24;
  hd->buf[57] = msb >> 16;
  hd->buf[58] = msb >>  8;
  hd->buf[59] = msb;
  hd->buf[60] = lsb >> 24;
  hd->buf[61] = lsb >> 16;
  hd->buf[62] = lsb >>  8;
  hd->buf[63] = lsb;
  transform (hd, hd->buf);
  _gcry_burn_stack (74*4+32);

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
  X(5);
  X(6);
  X(7);
#undef X
}

static byte *
sha256_read (void *context)
{
  SHA256_CONTEXT *hd = context;

  return hd->buf;
}



/* 
     Self-test section.
 */


static gpg_err_code_t
selftests_sha224 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  what = "short string";
  errtxt = _gcry_hash_selftest_check_one
    (GCRY_MD_SHA224, 0, 
     "abc", 3,
     "\x23\x09\x7d\x22\x34\x05\xd8\x22\x86\x42\xa4\x77\xbd\xa2\x55\xb3"
     "\x2a\xad\xbc\xe4\xbd\xa0\xb3\xf7\xe3\x6c\x9d\xa7", 28);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "long string";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA224, 0, 
         "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", 56,
         "\x75\x38\x8b\x16\x51\x27\x76\xcc\x5d\xba\x5d\xa1\xfd\x89\x01\x50"
         "\xb0\xc6\x45\x5c\xb4\xf5\x8b\x19\x52\x52\x25\x25", 28);
      if (errtxt)
        goto failed;
      
      what = "one million \"a\"";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA224, 1,
         NULL, 0,
         "\x20\x79\x46\x55\x98\x0c\x91\xd8\xbb\xb4\xc1\xea\x97\x61\x8a\x4b"
         "\xf0\x3f\x42\x58\x19\x48\xb2\xee\x4e\xe7\xad\x67", 28);
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("digest", GCRY_MD_SHA224, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}

static gpg_err_code_t
selftests_sha256 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  what = "short string";
  errtxt = _gcry_hash_selftest_check_one
    (GCRY_MD_SHA256, 0, 
     "abc", 3,
     "\xba\x78\x16\xbf\x8f\x01\xcf\xea\x41\x41\x40\xde\x5d\xae\x22\x23"
     "\xb0\x03\x61\xa3\x96\x17\x7a\x9c\xb4\x10\xff\x61\xf2\x00\x15\xad", 32);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "long string";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA256, 0, 
         "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", 56,
         "\x24\x8d\x6a\x61\xd2\x06\x38\xb8\xe5\xc0\x26\x93\x0c\x3e\x60\x39"
         "\xa3\x3c\xe4\x59\x64\xff\x21\x67\xf6\xec\xed\xd4\x19\xdb\x06\xc1",
         32);
      if (errtxt)
        goto failed;
      
      what = "one million \"a\"";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA256, 1,
         NULL, 0,
         "\xcd\xc7\x6e\x5c\x99\x14\xfb\x92\x81\xa1\xc7\xe2\x84\xd7\x3e\x67"
         "\xf1\x80\x9a\x48\xa4\x97\x20\x0e\x04\x6d\x39\xcc\xc7\x11\x2c\xd0",
         32);
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("digest", GCRY_MD_SHA256, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


/* Run a full self-test for ALGO and return 0 on success.  */
static gpg_err_code_t
run_selftests (int algo, int extended, selftest_report_func_t report)
{
  gpg_err_code_t ec;

  switch (algo)
    {
    case GCRY_MD_SHA224:
      ec = selftests_sha224 (extended, report);
      break;
    case GCRY_MD_SHA256:
      ec = selftests_sha256 (extended, report);
      break;
    default:
      ec = GPG_ERR_DIGEST_ALGO;
      break;
        
    }
  return ec;
}




static byte asn224[19] = /* Object ID is 2.16.840.1.101.3.4.2.4 */
  { 0x30, 0x2D, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86, 0x48,
    0x01, 0x65, 0x03, 0x04, 0x02, 0x04, 0x05, 0x00, 0x04,
    0x1C
  };

static gcry_md_oid_spec_t oid_spec_sha224[] =
  {
    /* From RFC3874, Section 4 */
    { "2.16.840.1.101.3.4.2.4" }, 
    { NULL },
  };

static byte asn256[19] = /* Object ID is  2.16.840.1.101.3.4.2.1 */
  { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86,
    0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05,
    0x00, 0x04, 0x20 };

static gcry_md_oid_spec_t oid_spec_sha256[] =
  {
    /* According to the OpenPGP draft rfc2440-bis06 */
    { "2.16.840.1.101.3.4.2.1" }, 
    /* PKCS#1 sha256WithRSAEncryption */
    { "1.2.840.113549.1.1.11" },

    { NULL },
  };

gcry_md_spec_t _gcry_digest_spec_sha224 =
  {
    "SHA224", asn224, DIM (asn224), oid_spec_sha224, 28,
    sha224_init, sha256_write, sha256_final, sha256_read,
    sizeof (SHA256_CONTEXT)
  };
md_extra_spec_t _gcry_digest_extraspec_sha224 = 
  {
    run_selftests
  };

gcry_md_spec_t _gcry_digest_spec_sha256 =
  {
    "SHA256", asn256, DIM (asn256), oid_spec_sha256, 32,
    sha256_init, sha256_write, sha256_final, sha256_read,
    sizeof (SHA256_CONTEXT)
  };
md_extra_spec_t _gcry_digest_extraspec_sha256 = 
  {
    run_selftests
  };
