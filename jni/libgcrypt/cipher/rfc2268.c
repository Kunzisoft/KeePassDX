/* rfc2268.c  - The cipher described in rfc2268; aka Ron's Cipher 2.
 * Copyright (C) 2003 Nikos Mavroyanopoulos
 * Copyright (C) 2004 Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt
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
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */

/* This implementation was written by Nikos Mavroyanopoulos for GNUTLS
 * as a Libgcrypt module (gnutls/lib/x509/rc2.c) and later adapted for
 * direct use by Libgcrypt by Werner Koch.  This implementation is
 * only useful for pkcs#12 descryption.
 *
 * The implementation here is based on Peter Gutmann's RRC.2 paper.
 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "g10lib.h"
#include "types.h"
#include "cipher.h"

#define RFC2268_BLOCKSIZE 8

typedef struct 
{
  u16 S[64];
} RFC2268_context;

static const unsigned char rfc2268_sbox[] = { 
  217, 120, 249, 196,  25, 221, 181, 237, 
   40, 233, 253, 121,  74, 160, 216, 157,
  198, 126,  55, 131,  43, 118,  83, 142, 
   98,  76, 100, 136,  68, 139, 251, 162,
   23, 154,  89, 245, 135, 179,  79,  19,
   97,  69, 109, 141,   9, 129, 125,  50,
  189, 143,  64, 235, 134, 183, 123,  11,
  240, 149,  33,  34,  92, 107,  78, 130,
   84, 214, 101, 147, 206,  96, 178,  28,
  115,  86, 192,  20, 167, 140, 241, 220,
   18, 117, 202,  31,  59, 190, 228, 209,
   66,  61, 212,  48, 163,  60, 182,  38,
  111, 191,  14, 218,  70, 105,   7,  87,
   39, 242,  29, 155, 188, 148,  67,   3,
  248,  17, 199, 246, 144, 239,  62, 231,
    6, 195, 213,  47, 200, 102,  30, 215,
    8, 232, 234, 222, 128,  82, 238, 247,
  132, 170, 114, 172,  53,  77, 106,  42,
  150,  26, 210, 113,  90,  21,  73, 116,
   75, 159, 208,  94,   4,  24, 164, 236,
  194, 224,  65, 110,  15,  81, 203, 204,
   36, 145, 175,  80, 161, 244, 112,  57,
  153, 124,  58, 133,  35, 184, 180, 122,
  252,   2,  54,  91,  37,  85, 151,  49,
   45,  93, 250, 152, 227, 138, 146, 174,
    5, 223,  41,  16, 103, 108, 186, 201,
  211,   0, 230, 207, 225, 158, 168,  44,
   99,  22,   1,  63,  88, 226, 137, 169,
   13,  56,  52,  27, 171,  51, 255, 176,
  187,  72,  12,  95, 185, 177, 205,  46,
  197, 243, 219,  71, 229, 165, 156, 119,
   10, 166,  32, 104, 254, 127, 193, 173
};

#define rotl16(x,n)   (((x) << ((u16)(n))) | ((x) >> (16 - (u16)(n))))
#define rotr16(x,n)   (((x) >> ((u16)(n))) | ((x) << (16 - (u16)(n))))

static const char *selftest (void);


static void
do_encrypt (void *context, unsigned char *outbuf, const unsigned char *inbuf)
{
  RFC2268_context *ctx = context;
  register int i, j;
  u16 word0 = 0, word1 = 0, word2 = 0, word3 = 0;

  word0 = (word0 << 8) | inbuf[1];
  word0 = (word0 << 8) | inbuf[0];
  word1 = (word1 << 8) | inbuf[3];
  word1 = (word1 << 8) | inbuf[2];
  word2 = (word2 << 8) | inbuf[5];
  word2 = (word2 << 8) | inbuf[4];
  word3 = (word3 << 8) | inbuf[7];
  word3 = (word3 << 8) | inbuf[6];

  for (i = 0; i < 16; i++)
    {
      j = i * 4;
      /* For some reason I cannot combine those steps. */
      word0 += (word1 & ~word3) + (word2 & word3) + ctx->S[j];
      word0 = rotl16(word0, 1);
		
      word1 += (word2 & ~word0) + (word3 & word0) + ctx->S[j + 1];
      word1 = rotl16(word1, 2);
		
      word2 += (word3 & ~word1) + (word0 & word1) + ctx->S[j + 2];
      word2 = rotl16(word2, 3);

      word3 += (word0 & ~word2) + (word1 & word2) + ctx->S[j + 3];
      word3 = rotl16(word3, 5);

      if (i == 4 || i == 10)
        {
          word0 += ctx->S[word3 & 63];
          word1 += ctx->S[word0 & 63];
          word2 += ctx->S[word1 & 63];
          word3 += ctx->S[word2 & 63];
        }

    }

  outbuf[0] = word0 & 255;
  outbuf[1] = word0 >> 8;
  outbuf[2] = word1 & 255;
  outbuf[3] = word1 >> 8;
  outbuf[4] = word2 & 255;
  outbuf[5] = word2 >> 8;
  outbuf[6] = word3 & 255;
  outbuf[7] = word3 >> 8;
}

static void
do_decrypt (void *context, unsigned char *outbuf, const unsigned char *inbuf)
{
  RFC2268_context *ctx = context;
  register int i, j;
  u16 word0 = 0, word1 = 0, word2 = 0, word3 = 0;

  word0 = (word0 << 8) | inbuf[1];
  word0 = (word0 << 8) | inbuf[0];
  word1 = (word1 << 8) | inbuf[3];
  word1 = (word1 << 8) | inbuf[2];
  word2 = (word2 << 8) | inbuf[5];
  word2 = (word2 << 8) | inbuf[4];
  word3 = (word3 << 8) | inbuf[7];
  word3 = (word3 << 8) | inbuf[6];

  for (i = 15; i >= 0; i--) 
    {
      j = i * 4;

      word3 = rotr16(word3, 5);
      word3 -= (word0 & ~word2) + (word1 & word2) + ctx->S[j + 3];

      word2 = rotr16(word2, 3);
      word2 -= (word3 & ~word1) + (word0 & word1) + ctx->S[j + 2];

      word1 = rotr16(word1, 2);
      word1 -= (word2 & ~word0) + (word3 & word0) + ctx->S[j + 1];

      word0 = rotr16(word0, 1);
      word0 -= (word1 & ~word3) + (word2 & word3) + ctx->S[j];

      if (i == 5 || i == 11) 
        {
          word3 = word3 - ctx->S[word2 & 63];
          word2 = word2 - ctx->S[word1 & 63];
          word1 = word1 - ctx->S[word0 & 63];
          word0 = word0 - ctx->S[word3 & 63];
        }

    }

  outbuf[0] = word0 & 255;
  outbuf[1] = word0 >> 8;
  outbuf[2] = word1 & 255;
  outbuf[3] = word1 >> 8;
  outbuf[4] = word2 & 255;
  outbuf[5] = word2 >> 8;
  outbuf[6] = word3 & 255;
  outbuf[7] = word3 >> 8;
}


static gpg_err_code_t
setkey_core (void *context, const unsigned char *key, unsigned int keylen, int with_phase2)
{
  static int initialized;
  static const char *selftest_failed;
  RFC2268_context *ctx = context;
  unsigned int i;
  unsigned char *S, x;
  int len;
  int bits = keylen * 8;

  if (!initialized)
    {
      initialized = 1;
      selftest_failed = selftest ();
      if (selftest_failed)
        log_error ("RFC2268 selftest failed (%s).\n", selftest_failed);
    }
  if (selftest_failed)
    return GPG_ERR_SELFTEST_FAILED;

  if (keylen < 40 / 8)	/* We want at least 40 bits. */
    return GPG_ERR_INV_KEYLEN;

  S = (unsigned char *) ctx->S;
  
  for (i = 0; i < keylen; i++)
    S[i] = key[i];

  for (i = keylen; i < 128; i++)
    S[i] = rfc2268_sbox[(S[i - keylen] + S[i - 1]) & 255];

  S[0] = rfc2268_sbox[S[0]];

  /* Phase 2 - reduce effective key size to "bits". This was not
   * discussed in Gutmann's paper. I've copied that from the public
   * domain code posted in sci.crypt. */
  if (with_phase2)
    {
      len = (bits + 7) >> 3;
      i = 128 - len;
      x = rfc2268_sbox[S[i] & (255 >> (7 & -bits))];
      S[i] = x;
      
      while (i--) 
        {
          x = rfc2268_sbox[x ^ S[i + len]];
          S[i] = x;
        }
    }

  /* Make the expanded key, endian independent. */
  for (i = 0; i < 64; i++) 
    ctx->S[i] = ( (u16) S[i * 2] | (((u16) S[i * 2 + 1]) << 8));

  return 0;
}

static gpg_err_code_t
do_setkey (void *context, const unsigned char *key, unsigned int keylen)
{
  return setkey_core (context, key, keylen, 1);
}

static const char *
selftest (void)
{
  RFC2268_context ctx;
  unsigned char scratch[16];

  /* Test vectors from Peter Gutmann's paper. */
  static unsigned char key_1[] =
    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
  static unsigned char plaintext_1[] =
    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  static const unsigned char ciphertext_1[] =
    { 0x1C, 0x19, 0x8A, 0x83, 0x8D, 0xF0, 0x28, 0xB7 };

  static unsigned char key_2[] =
    { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
  static unsigned char plaintext_2[] =
    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  static unsigned char ciphertext_2[] =
    { 0x50, 0xDC, 0x01, 0x62, 0xBD, 0x75, 0x7F, 0x31 };

  /* This one was checked against libmcrypt's RFC2268. */
  static unsigned char key_3[] =
    { 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
  static unsigned char plaintext_3[] =
    { 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  static unsigned char ciphertext_3[] =
    { 0x8f, 0xd1, 0x03, 0x89, 0x33, 0x6b, 0xf9, 0x5e };


  /* First test. */
  setkey_core (&ctx, key_1, sizeof(key_1), 0);
  do_encrypt (&ctx, scratch, plaintext_1);

  if (memcmp (scratch, ciphertext_1, sizeof(ciphertext_1)))
    return "RFC2268 encryption test 1 failed.";

  setkey_core (&ctx, key_1, sizeof(key_1), 0);
  do_decrypt (&ctx, scratch, scratch); 
  if (memcmp (scratch, plaintext_1, sizeof(plaintext_1)))
    return "RFC2268 decryption test 1 failed.";

  /* Second test. */
  setkey_core (&ctx, key_2, sizeof(key_2), 0);
  do_encrypt (&ctx, scratch, plaintext_2);
  if (memcmp (scratch, ciphertext_2, sizeof(ciphertext_2)))
    return "RFC2268 encryption test 2 failed.";

  setkey_core (&ctx, key_2, sizeof(key_2), 0);
  do_decrypt (&ctx, scratch, scratch); 
  if (memcmp (scratch, plaintext_2, sizeof(plaintext_2)))
    return "RFC2268 decryption test 2 failed.";

  /* Third test. */
  setkey_core(&ctx, key_3, sizeof(key_3), 0);
  do_encrypt(&ctx, scratch, plaintext_3);

  if (memcmp(scratch, ciphertext_3, sizeof(ciphertext_3)))
    return "RFC2268 encryption test 3 failed.";

  setkey_core (&ctx, key_3, sizeof(key_3), 0);
  do_decrypt (&ctx, scratch, scratch); 
  if (memcmp(scratch, plaintext_3, sizeof(plaintext_3)))
    return "RFC2268 decryption test 3 failed.";

  return NULL;
}



static gcry_cipher_oid_spec_t oids_rfc2268_40[] =
  {
    /*{ "1.2.840.113549.3.2", GCRY_CIPHER_MODE_CBC },*/
    /* pbeWithSHAAnd40BitRC2_CBC */
    { "1.2.840.113549.1.12.1.6", GCRY_CIPHER_MODE_CBC },
    { NULL }
  };

gcry_cipher_spec_t _gcry_cipher_spec_rfc2268_40 = {
  "RFC2268_40", NULL, oids_rfc2268_40,
  RFC2268_BLOCKSIZE, 40, sizeof(RFC2268_context),
  do_setkey, do_encrypt, do_decrypt
};

