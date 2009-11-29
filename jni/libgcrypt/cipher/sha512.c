/* sha512.c - SHA384 and SHA512 hash functions
 *	Copyright (C) 2003, 2008 Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
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


/*  Test vectors from FIPS-180-2:
 *
 *  "abc"
 * 384:
 *  CB00753F 45A35E8B B5A03D69 9AC65007 272C32AB 0EDED163
 *  1A8B605A 43FF5BED 8086072B A1E7CC23 58BAECA1 34C825A7
 * 512:
 *  DDAF35A1 93617ABA CC417349 AE204131 12E6FA4E 89A97EA2 0A9EEEE6 4B55D39A
 *  2192992A 274FC1A8 36BA3C23 A3FEEBBD 454D4423 643CE80E 2A9AC94F A54CA49F
 *
 *  "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"
 * 384:
 *  09330C33 F71147E8 3D192FC7 82CD1B47 53111B17 3B3B05D2
 *  2FA08086 E3B0F712 FCC7C71A 557E2DB9 66C3E9FA 91746039
 * 512:
 *  8E959B75 DAE313DA 8CF4F728 14FC143F 8F7779C6 EB9F7FA1 7299AEAD B6889018
 *  501D289E 4900F7E4 331B99DE C4B5433A C7D329EE B6DD2654 5E96E55B 874BE909
 *
 *  "a" x 1000000
 * 384:
 *  9D0E1809 716474CB 086E834E 310A4A1C ED149E9C 00F24852
 *  7972CEC5 704C2A5B 07B8B3DC 38ECC4EB AE97DDD8 7F3D8985
 * 512:
 *  E718483D 0CE76964 4E2E42C7 BC15B463 8E1F98B1 3B204428 5632A803 AFA973EB
 *  DE0FF244 877EA60A 4CB0432C E577C31B EB009C5C 2C49AA2E 4EADB217 AD8CC09B
 */


#include <config.h>
#include <string.h>
#include "g10lib.h"
#include "bithelp.h"
#include "cipher.h"
#include "hash-common.h"

typedef struct
{
  u64 h0, h1, h2, h3, h4, h5, h6, h7;
  u64 nblocks;
  byte buf[128];
  int count;
} SHA512_CONTEXT;

static void
sha512_init (void *context)
{
  SHA512_CONTEXT *hd = context;

  hd->h0 = U64_C(0x6a09e667f3bcc908);
  hd->h1 = U64_C(0xbb67ae8584caa73b);
  hd->h2 = U64_C(0x3c6ef372fe94f82b);
  hd->h3 = U64_C(0xa54ff53a5f1d36f1);
  hd->h4 = U64_C(0x510e527fade682d1);
  hd->h5 = U64_C(0x9b05688c2b3e6c1f);
  hd->h6 = U64_C(0x1f83d9abfb41bd6b);
  hd->h7 = U64_C(0x5be0cd19137e2179);

  hd->nblocks = 0;
  hd->count = 0;
}

static void
sha384_init (void *context)
{
  SHA512_CONTEXT *hd = context;

  hd->h0 = U64_C(0xcbbb9d5dc1059ed8);
  hd->h1 = U64_C(0x629a292a367cd507);
  hd->h2 = U64_C(0x9159015a3070dd17);
  hd->h3 = U64_C(0x152fecd8f70e5939);
  hd->h4 = U64_C(0x67332667ffc00b31);
  hd->h5 = U64_C(0x8eb44a8768581511);
  hd->h6 = U64_C(0xdb0c2e0d64f98fa7);
  hd->h7 = U64_C(0x47b5481dbefa4fa4);

  hd->nblocks = 0;
  hd->count = 0;
}


/****************
 * Transform the message W which consists of 16 64-bit-words
 */
static void
transform (SHA512_CONTEXT *hd, const unsigned char *data)
{
  u64 a, b, c, d, e, f, g, h;
  u64 w[80];
  int t;
  static const u64 k[] =
    {
      U64_C(0x428a2f98d728ae22), U64_C(0x7137449123ef65cd),
      U64_C(0xb5c0fbcfec4d3b2f), U64_C(0xe9b5dba58189dbbc),
      U64_C(0x3956c25bf348b538), U64_C(0x59f111f1b605d019),
      U64_C(0x923f82a4af194f9b), U64_C(0xab1c5ed5da6d8118),
      U64_C(0xd807aa98a3030242), U64_C(0x12835b0145706fbe),
      U64_C(0x243185be4ee4b28c), U64_C(0x550c7dc3d5ffb4e2),
      U64_C(0x72be5d74f27b896f), U64_C(0x80deb1fe3b1696b1),
      U64_C(0x9bdc06a725c71235), U64_C(0xc19bf174cf692694),
      U64_C(0xe49b69c19ef14ad2), U64_C(0xefbe4786384f25e3),
      U64_C(0x0fc19dc68b8cd5b5), U64_C(0x240ca1cc77ac9c65),
      U64_C(0x2de92c6f592b0275), U64_C(0x4a7484aa6ea6e483),
      U64_C(0x5cb0a9dcbd41fbd4), U64_C(0x76f988da831153b5),
      U64_C(0x983e5152ee66dfab), U64_C(0xa831c66d2db43210),
      U64_C(0xb00327c898fb213f), U64_C(0xbf597fc7beef0ee4),
      U64_C(0xc6e00bf33da88fc2), U64_C(0xd5a79147930aa725),
      U64_C(0x06ca6351e003826f), U64_C(0x142929670a0e6e70),
      U64_C(0x27b70a8546d22ffc), U64_C(0x2e1b21385c26c926),
      U64_C(0x4d2c6dfc5ac42aed), U64_C(0x53380d139d95b3df),
      U64_C(0x650a73548baf63de), U64_C(0x766a0abb3c77b2a8),
      U64_C(0x81c2c92e47edaee6), U64_C(0x92722c851482353b),
      U64_C(0xa2bfe8a14cf10364), U64_C(0xa81a664bbc423001),
      U64_C(0xc24b8b70d0f89791), U64_C(0xc76c51a30654be30),
      U64_C(0xd192e819d6ef5218), U64_C(0xd69906245565a910),
      U64_C(0xf40e35855771202a), U64_C(0x106aa07032bbd1b8),
      U64_C(0x19a4c116b8d2d0c8), U64_C(0x1e376c085141ab53),
      U64_C(0x2748774cdf8eeb99), U64_C(0x34b0bcb5e19b48a8),
      U64_C(0x391c0cb3c5c95a63), U64_C(0x4ed8aa4ae3418acb),
      U64_C(0x5b9cca4f7763e373), U64_C(0x682e6ff3d6b2b8a3),
      U64_C(0x748f82ee5defb2fc), U64_C(0x78a5636f43172f60),
      U64_C(0x84c87814a1f0ab72), U64_C(0x8cc702081a6439ec),
      U64_C(0x90befffa23631e28), U64_C(0xa4506cebde82bde9),
      U64_C(0xbef9a3f7b2c67915), U64_C(0xc67178f2e372532b),
      U64_C(0xca273eceea26619c), U64_C(0xd186b8c721c0c207),
      U64_C(0xeada7dd6cde0eb1e), U64_C(0xf57d4f7fee6ed178),
      U64_C(0x06f067aa72176fba), U64_C(0x0a637dc5a2c898a6),
      U64_C(0x113f9804bef90dae), U64_C(0x1b710b35131c471b),
      U64_C(0x28db77f523047d84), U64_C(0x32caab7b40c72493),
      U64_C(0x3c9ebe0a15c9bebc), U64_C(0x431d67c49c100d4c),
      U64_C(0x4cc5d4becb3e42b6), U64_C(0x597f299cfc657e2a),
      U64_C(0x5fcb6fab3ad6faec), U64_C(0x6c44198c4a475817)
    };

  /* get values from the chaining vars */
  a = hd->h0;
  b = hd->h1;
  c = hd->h2;
  d = hd->h3;
  e = hd->h4;
  f = hd->h5;
  g = hd->h6;
  h = hd->h7;

#ifdef WORDS_BIGENDIAN
  memcpy (w, data, 128);
#else
  {
    int i;
    byte *p2;

    for (i = 0, p2 = (byte *) w; i < 16; i++, p2 += 8)
      {
	p2[7] = *data++;
	p2[6] = *data++;
	p2[5] = *data++;
	p2[4] = *data++;
	p2[3] = *data++;
	p2[2] = *data++;
	p2[1] = *data++;
	p2[0] = *data++;
      }
  }
#endif

#define ROTR(x,n) (((x)>>(n)) | ((x)<<(64-(n))))
#define Ch(x,y,z) (((x) & (y)) ^ ((~(x)) & (z)))
#define Maj(x,y,z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define Sum0(x) (ROTR((x),28) ^ ROTR((x),34) ^ ROTR((x),39))
#define Sum1(x) (ROTR((x),14) ^ ROTR((x),18) ^ ROTR((x),41))
#define S0(x) (ROTR((x),1) ^ ROTR((x),8) ^ ((x)>>7))
#define S1(x) (ROTR((x),19) ^ ROTR((x),61) ^ ((x)>>6))

  for (t = 16; t < 80; t++)
    w[t] = S1 (w[t - 2]) + w[t - 7] + S0 (w[t - 15]) + w[t - 16];

  for (t = 0; t < 80; t++)
    {
      u64 t1, t2;

      t1 = h + Sum1 (e) + Ch (e, f, g) + k[t] + w[t];
      t2 = Sum0 (a) + Maj (a, b, c);
      h = g;
      g = f;
      f = e;
      e = d + t1;
      d = c;
      c = b;
      b = a;
      a = t1 + t2;

      /* printf("t=%d a=%016llX b=%016llX c=%016llX d=%016llX "
          "e=%016llX f=%016llX g=%016llX h=%016llX\n",t,a,b,c,d,e,f,g,h); */
    }

  /* update chaining vars */
  hd->h0 += a;
  hd->h1 += b;
  hd->h2 += c;
  hd->h3 += d;
  hd->h4 += e;
  hd->h5 += f;
  hd->h6 += g;
  hd->h7 += h;
}


/* Update the message digest with the contents
 * of INBUF with length INLEN.
 */
static void
sha512_write (void *context, const void *inbuf_arg, size_t inlen)
{
  const unsigned char *inbuf = inbuf_arg;
  SHA512_CONTEXT *hd = context;

  if (hd->count == 128)
    {				/* flush the buffer */
      transform (hd, hd->buf);
      _gcry_burn_stack (768);
      hd->count = 0;
      hd->nblocks++;
    }
  if (!inbuf)
    return;
  if (hd->count)
    {
      for (; inlen && hd->count < 128; inlen--)
	hd->buf[hd->count++] = *inbuf++;
      sha512_write (context, NULL, 0);
      if (!inlen)
	return;
    }

  while (inlen >= 128)
    {
      transform (hd, inbuf);
      hd->count = 0;
      hd->nblocks++;
      inlen -= 128;
      inbuf += 128;
    }
  _gcry_burn_stack (768);
  for (; inlen && hd->count < 128; inlen--)
    hd->buf[hd->count++] = *inbuf++;
}


/* The routine final terminates the computation and
 * returns the digest.
 * The handle is prepared for a new cycle, but adding bytes to the
 * handle will the destroy the returned buffer.
 * Returns: 64 bytes representing the digest.  When used for sha384,
 * we take the leftmost 48 of those bytes.
 */

static void
sha512_final (void *context)
{
  SHA512_CONTEXT *hd = context;
  u64 t, msb, lsb;
  byte *p;

  sha512_write (context, NULL, 0); /* flush */ ;

  t = hd->nblocks;
  /* multiply by 128 to make a byte count */
  lsb = t << 7;
  msb = t >> 57;
  /* add the count */
  t = lsb;
  if ((lsb += hd->count) < t)
    msb++;
  /* multiply by 8 to make a bit count */
  t = lsb;
  lsb <<= 3;
  msb <<= 3;
  msb |= t >> 61;

  if (hd->count < 112)
    {				/* enough room */
      hd->buf[hd->count++] = 0x80;	/* pad */
      while (hd->count < 112)
	hd->buf[hd->count++] = 0;	/* pad */
    }
  else
    {				/* need one extra block */
      hd->buf[hd->count++] = 0x80;	/* pad character */
      while (hd->count < 128)
	hd->buf[hd->count++] = 0;
      sha512_write (context, NULL, 0); /* flush */ ;
      memset (hd->buf, 0, 112);	/* fill next block with zeroes */
    }
  /* append the 128 bit count */
  hd->buf[112] = msb >> 56;
  hd->buf[113] = msb >> 48;
  hd->buf[114] = msb >> 40;
  hd->buf[115] = msb >> 32;
  hd->buf[116] = msb >> 24;
  hd->buf[117] = msb >> 16;
  hd->buf[118] = msb >> 8;
  hd->buf[119] = msb;

  hd->buf[120] = lsb >> 56;
  hd->buf[121] = lsb >> 48;
  hd->buf[122] = lsb >> 40;
  hd->buf[123] = lsb >> 32;
  hd->buf[124] = lsb >> 24;
  hd->buf[125] = lsb >> 16;
  hd->buf[126] = lsb >> 8;
  hd->buf[127] = lsb;
  transform (hd, hd->buf);
  _gcry_burn_stack (768);

  p = hd->buf;
#ifdef WORDS_BIGENDIAN
#define X(a) do { *(u64*)p = hd->h##a ; p += 8; } while (0)
#else /* little endian */
#define X(a) do { *p++ = hd->h##a >> 56; *p++ = hd->h##a >> 48;	      \
                  *p++ = hd->h##a >> 40; *p++ = hd->h##a >> 32;	      \
                  *p++ = hd->h##a >> 24; *p++ = hd->h##a >> 16;	      \
                  *p++ = hd->h##a >> 8;  *p++ = hd->h##a; } while (0)
#endif
  X (0);
  X (1);
  X (2);
  X (3);
  X (4);
  X (5);
  /* Note that these last two chunks are included even for SHA384.
     We just ignore them. */
  X (6);
  X (7);
#undef X
}

static byte *
sha512_read (void *context)
{
  SHA512_CONTEXT *hd = (SHA512_CONTEXT *) context;
  return hd->buf;
}



/* 
     Self-test section.
 */


static gpg_err_code_t
selftests_sha384 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  what = "short string";
  errtxt = _gcry_hash_selftest_check_one
    (GCRY_MD_SHA384, 0, 
     "abc", 3,
     "\xcb\x00\x75\x3f\x45\xa3\x5e\x8b\xb5\xa0\x3d\x69\x9a\xc6\x50\x07"
     "\x27\x2c\x32\xab\x0e\xde\xd1\x63\x1a\x8b\x60\x5a\x43\xff\x5b\xed"
     "\x80\x86\x07\x2b\xa1\xe7\xcc\x23\x58\xba\xec\xa1\x34\xc8\x25\xa7", 48);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "long string";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA384, 0, 
         "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn"
         "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu", 112, 
         "\x09\x33\x0C\x33\xF7\x11\x47\xE8\x3D\x19\x2F\xC7\x82\xCD\x1B\x47"
         "\x53\x11\x1B\x17\x3B\x3B\x05\xD2\x2F\xA0\x80\x86\xE3\xB0\xF7\x12"
         "\xFC\xC7\xC7\x1A\x55\x7E\x2D\xB9\x66\xC3\xE9\xFA\x91\x74\x60\x39",
         48);
      if (errtxt)
        goto failed;

      what = "one million \"a\"";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA384, 1,
         NULL, 0,
         "\x9D\x0E\x18\x09\x71\x64\x74\xCB\x08\x6E\x83\x4E\x31\x0A\x4A\x1C"
         "\xED\x14\x9E\x9C\x00\xF2\x48\x52\x79\x72\xCE\xC5\x70\x4C\x2A\x5B"
         "\x07\xB8\xB3\xDC\x38\xEC\xC4\xEB\xAE\x97\xDD\xD8\x7F\x3D\x89\x85",
         48);
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("digest", GCRY_MD_SHA384, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}

static gpg_err_code_t
selftests_sha512 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  what = "short string";
  errtxt = _gcry_hash_selftest_check_one
    (GCRY_MD_SHA512, 0, 
     "abc", 3,
     "\xDD\xAF\x35\xA1\x93\x61\x7A\xBA\xCC\x41\x73\x49\xAE\x20\x41\x31"
     "\x12\xE6\xFA\x4E\x89\xA9\x7E\xA2\x0A\x9E\xEE\xE6\x4B\x55\xD3\x9A"
     "\x21\x92\x99\x2A\x27\x4F\xC1\xA8\x36\xBA\x3C\x23\xA3\xFE\xEB\xBD"
     "\x45\x4D\x44\x23\x64\x3C\xE8\x0E\x2A\x9A\xC9\x4F\xA5\x4C\xA4\x9F", 64);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "long string";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA512, 0, 
         "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn"
         "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu", 112, 
         "\x8E\x95\x9B\x75\xDA\xE3\x13\xDA\x8C\xF4\xF7\x28\x14\xFC\x14\x3F"
         "\x8F\x77\x79\xC6\xEB\x9F\x7F\xA1\x72\x99\xAE\xAD\xB6\x88\x90\x18"
         "\x50\x1D\x28\x9E\x49\x00\xF7\xE4\x33\x1B\x99\xDE\xC4\xB5\x43\x3A"
         "\xC7\xD3\x29\xEE\xB6\xDD\x26\x54\x5E\x96\xE5\x5B\x87\x4B\xE9\x09",
         64);
      if (errtxt)
        goto failed;
      
      what = "one million \"a\"";
      errtxt = _gcry_hash_selftest_check_one
        (GCRY_MD_SHA512, 1,
         NULL, 0,
         "\xE7\x18\x48\x3D\x0C\xE7\x69\x64\x4E\x2E\x42\xC7\xBC\x15\xB4\x63"
         "\x8E\x1F\x98\xB1\x3B\x20\x44\x28\x56\x32\xA8\x03\xAF\xA9\x73\xEB"
         "\xDE\x0F\xF2\x44\x87\x7E\xA6\x0A\x4C\xB0\x43\x2C\xE5\x77\xC3\x1B"
         "\xEB\x00\x9C\x5C\x2C\x49\xAA\x2E\x4E\xAD\xB2\x17\xAD\x8C\xC0\x9B",
         64);
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("digest", GCRY_MD_SHA512, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


/* Run a full self-test for ALGO and return 0 on success.  */
static gpg_err_code_t
run_selftests (int algo, int extended, selftest_report_func_t report)
{
  gpg_err_code_t ec;

  switch (algo)
    {
    case GCRY_MD_SHA384:
      ec = selftests_sha384 (extended, report);
      break;
    case GCRY_MD_SHA512:
      ec = selftests_sha512 (extended, report);
      break;
    default:
      ec = GPG_ERR_DIGEST_ALGO;
      break;
        
    }
  return ec;
}




static byte sha512_asn[] =	/* Object ID is 2.16.840.1.101.3.4.2.3 */
  {
    0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86,
    0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05,
    0x00, 0x04, 0x40
  };

static gcry_md_oid_spec_t oid_spec_sha512[] =
  {
    { "2.16.840.1.101.3.4.2.3" },

    /* PKCS#1 sha512WithRSAEncryption */
    { "1.2.840.113549.1.1.13" },

    { NULL }
  };

gcry_md_spec_t _gcry_digest_spec_sha512 = 
  {
    "SHA512", sha512_asn, DIM (sha512_asn), oid_spec_sha512, 64,
    sha512_init, sha512_write, sha512_final, sha512_read,
    sizeof (SHA512_CONTEXT),
  };
md_extra_spec_t _gcry_digest_extraspec_sha512 = 
  {
    run_selftests
  };

static byte sha384_asn[] =	/* Object ID is 2.16.840.1.101.3.4.2.2 */
  {
    0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86,
    0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x05,
    0x00, 0x04, 0x30
  };

static gcry_md_oid_spec_t oid_spec_sha384[] =
  {
    { "2.16.840.1.101.3.4.2.2" }, 

    /* PKCS#1 sha384WithRSAEncryption */
    { "1.2.840.113549.1.1.12" },

    { NULL },
  };

gcry_md_spec_t _gcry_digest_spec_sha384 = 
  {
    "SHA384", sha384_asn, DIM (sha384_asn), oid_spec_sha384, 48,
    sha384_init, sha512_write, sha512_final, sha512_read,
    sizeof (SHA512_CONTEXT),
  };
md_extra_spec_t _gcry_digest_extraspec_sha384 = 
  {
    run_selftests
  };
