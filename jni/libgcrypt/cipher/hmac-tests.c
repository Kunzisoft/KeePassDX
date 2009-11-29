/* hmac-tests.c - HMAC selftests.
 * Copyright (C) 2008 Free Software Foundation, Inc.
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
   Although algorithm self-tests are usually implemented in the module
   implementing the algorithm, the case for HMAC is different because
   HMAC is implemnetd on a higher level using a special feature of the
   gcry_md_ functions.  It would be possible to do this also in the
   digest algorithm modules, but that would blow up the code too much
   and spread the hmac tests over several modules.

   Thus we implement all HMAC tests in this test module and provide a
   function to run the tests.
*/

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef HAVE_STDINT_H 
# include <stdint.h>
#endif

#include "g10lib.h"
#include "cipher.h"
#include "hmac256.h"

/* Check one HMAC with digest ALGO using the regualr HAMC
   API. (DATA,DATALEN) is the data to be MACed, (KEY,KEYLEN) the key
   and (EXPECT,EXPECTLEN) the expected result.  Returns NULL on
   succdess or a string describing the failure.  */
static const char *
check_one (int algo,
           const void *data, size_t datalen, 
           const void *key, size_t keylen,
           const void *expect, size_t expectlen)
{
  gcry_md_hd_t hd;
  const unsigned char *digest;

/*   printf ("HMAC algo %d\n", algo); */
  if (_gcry_md_get_algo_dlen (algo) != expectlen)
    return "invalid tests data";
  if (_gcry_md_open (&hd, algo, GCRY_MD_FLAG_HMAC))
    return "gcry_md_open failed";
  if (_gcry_md_setkey (hd, key, keylen))
    {
      _gcry_md_close (hd);
      return "gcry_md_setkey failed";
    }
  _gcry_md_write (hd, data, datalen);
  digest = _gcry_md_read (hd, algo);
  if (!digest)
    {
      _gcry_md_close (hd);
      return "gcry_md_read failed";
    }
  if (memcmp (digest, expect, expectlen))
    {
/*       int i; */

/*       fputs ("        {", stdout); */
/*       for (i=0; i < expectlen-1; i++) */
/*         { */
/*           if (i && !(i % 8)) */
/*             fputs ("\n         ", stdout); */
/*           printf (" 0x%02x,", digest[i]); */
/*         } */
/*       printf (" 0x%02x } },\n", digest[i]); */

      _gcry_md_close (hd);
      return "does not match";
    }
  _gcry_md_close (hd);
  return NULL;  
}


static gpg_err_code_t
selftests_sha1 (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  unsigned char key[128];
  int i, j;

  what = "FIPS-198a, A.1";
  for (i=0; i < 64; i++)
    key[i] = i;
  errtxt = check_one (GCRY_MD_SHA1,
                      "Sample #1", 9,
                      key, 64,
                      "\x4f\x4c\xa3\xd5\xd6\x8b\xa7\xcc\x0a\x12"
                      "\x08\xc9\xc6\x1e\x9c\x5d\xa0\x40\x3c\x0a", 20);
  if (errtxt)
    goto failed;

  if (extended)
    {
      what = "FIPS-198a, A.2";
      for (i=0, j=0x30; i < 20; i++)
        key[i] = j++;
      errtxt = check_one (GCRY_MD_SHA1,
                          "Sample #2", 9,
                          key, 20,
                          "\x09\x22\xd3\x40\x5f\xaa\x3d\x19\x4f\x82"
                          "\xa4\x58\x30\x73\x7d\x5c\xc6\xc7\x5d\x24", 20);
      if (errtxt)
        goto failed;
      
      what = "FIPS-198a, A.3";
      for (i=0, j=0x50; i < 100; i++)
        key[i] = j++;
      errtxt = check_one (GCRY_MD_SHA1,
                          "Sample #3", 9,
                          key, 100,
                          "\xbc\xf4\x1e\xab\x8b\xb2\xd8\x02\xf3\xd0"
                          "\x5c\xaf\x7c\xb0\x92\xec\xf8\xd1\xa3\xaa", 20 );
      if (errtxt)
        goto failed;
      
      what = "FIPS-198a, A.4";
      for (i=0, j=0x70; i < 49; i++)
        key[i] = j++;
      errtxt = check_one (GCRY_MD_SHA1,
                          "Sample #4", 9,
                          key, 49,
                          "\x9e\xa8\x86\xef\xe2\x68\xdb\xec\xce\x42"
                          "\x0c\x75\x24\xdf\x32\xe0\x75\x1a\x2a\x26", 20 );
      if (errtxt)
        goto failed;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("hmac", GCRY_MD_SHA1, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}



static gpg_err_code_t
selftests_sha224 (int extended, selftest_report_func_t report)
{
  static struct 
  {
    const char * const desc;
    const char * const data;
    const char * const key;
    const char expect[28];
  } tv[] =
    {
      { "data-28 key-4",
        "what do ya want for nothing?", 
        "Jefe",
        { 0xa3, 0x0e, 0x01, 0x09, 0x8b, 0xc6, 0xdb, 0xbf,
          0x45, 0x69, 0x0f, 0x3a, 0x7e, 0x9e, 0x6d, 0x0f,
          0x8b, 0xbe, 0xa2, 0xa3, 0x9e, 0x61, 0x48, 0x00,
          0x8f, 0xd0, 0x5e, 0x44 } },

      { "data-9 key-20",
        "Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
        "\x0b\x0b\x0b\x0b",
        { 0x89, 0x6f, 0xb1, 0x12, 0x8a, 0xbb, 0xdf, 0x19,
          0x68, 0x32, 0x10, 0x7c, 0xd4, 0x9d, 0xf3, 0x3f,
          0x47, 0xb4, 0xb1, 0x16, 0x99, 0x12, 0xba, 0x4f,
          0x53, 0x68, 0x4b, 0x22 } },

      { "data-50 key-20",
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa",
        { 0x7f, 0xb3, 0xcb, 0x35, 0x88, 0xc6, 0xc1, 0xf6,
          0xff, 0xa9, 0x69, 0x4d, 0x7d, 0x6a, 0xd2, 0x64,
          0x93, 0x65, 0xb0, 0xc1, 0xf6, 0x5d, 0x69, 0xd1,
          0xec, 0x83, 0x33, 0xea } },

      { "data-50 key-26",
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10"
        "\x11\x12\x13\x14\x15\x16\x17\x18\x19",
        { 0x6c, 0x11, 0x50, 0x68, 0x74, 0x01, 0x3c, 0xac,
          0x6a, 0x2a, 0xbc, 0x1b, 0xb3, 0x82, 0x62, 0x7c,
          0xec, 0x6a, 0x90, 0xd8, 0x6e, 0xfc, 0x01, 0x2d,
          0xe7, 0xaf, 0xec, 0x5a } },

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
        { 0x95, 0xe9, 0xa0, 0xdb, 0x96, 0x20, 0x95, 0xad,
          0xae, 0xbe, 0x9b, 0x2d, 0x6f, 0x0d, 0xbc, 0xe2,
          0xd4, 0x99, 0xf1, 0x12, 0xf2, 0xd2, 0xb7, 0x27,
          0x3f, 0xa6, 0x87, 0x0e } },

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
        { 0x3a, 0x85, 0x41, 0x66, 0xac, 0x5d, 0x9f, 0x02,
          0x3f, 0x54, 0xd5, 0x17, 0xd0, 0xb3, 0x9d, 0xbd,
          0x94, 0x67, 0x70, 0xdb, 0x9c, 0x2b, 0x95, 0xc9,
          0xf6, 0xf5, 0x65, 0xd1 } },

      { NULL }
    };
  const char *what;
  const char *errtxt;
  int tvidx;
  
  for (tvidx=0; tv[tvidx].desc; tvidx++)
    {
      what = tv[tvidx].desc;
      errtxt = check_one (GCRY_MD_SHA224,
                          tv[tvidx].data, strlen (tv[tvidx].data),
                          tv[tvidx].key, strlen (tv[tvidx].key),
                          tv[tvidx].expect, DIM (tv[tvidx].expect) );
      if (errtxt)
        goto failed;
      if (!extended)
        break;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("hmac", GCRY_MD_SHA224, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


static gpg_err_code_t
selftests_sha256 (int extended, selftest_report_func_t report)
{
  static struct 
  {
    const char * const desc;
    const char * const data;
    const char * const key;
    const char expect[32];
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
  const char *what;
  const char *errtxt;
  int tvidx;
  
  for (tvidx=0; tv[tvidx].desc; tvidx++)
    {
      hmac256_context_t hmachd;
      const unsigned char *digest;
      size_t dlen;

      what = tv[tvidx].desc;
      errtxt = check_one (GCRY_MD_SHA256,
                          tv[tvidx].data, strlen (tv[tvidx].data),
                          tv[tvidx].key, strlen (tv[tvidx].key),
                          tv[tvidx].expect, DIM (tv[tvidx].expect) );
      if (errtxt)
        goto failed;

      hmachd = _gcry_hmac256_new (tv[tvidx].key, strlen (tv[tvidx].key));
      if (!hmachd)
        {
          errtxt = "_gcry_hmac256_new failed";
          goto failed;
        }
      _gcry_hmac256_update (hmachd, tv[tvidx].data, strlen (tv[tvidx].data));
      digest = _gcry_hmac256_finalize (hmachd, &dlen);
      if (!digest)
        {
          errtxt = "_gcry_hmac256_finalize failed";
          _gcry_hmac256_release (hmachd);
          goto failed;
        }
      if (dlen != DIM (tv[tvidx].expect)
          || memcmp (digest, tv[tvidx].expect, DIM (tv[tvidx].expect)))
        {
          errtxt = "does not match in second implementation";
          _gcry_hmac256_release (hmachd);
          goto failed;
        }
      _gcry_hmac256_release (hmachd);

      if (!extended)
        break;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("hmac", GCRY_MD_SHA256, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


static gpg_err_code_t
selftests_sha384 (int extended, selftest_report_func_t report)
{
  static struct 
  {
    const char * const desc;
    const char * const data;
    const char * const key;
    const char expect[48];
  } tv[] =
    {
      { "data-28 key-4",
        "what do ya want for nothing?", 
        "Jefe",
        { 0xaf, 0x45, 0xd2, 0xe3, 0x76, 0x48, 0x40, 0x31,
          0x61, 0x7f, 0x78, 0xd2, 0xb5, 0x8a, 0x6b, 0x1b,
          0x9c, 0x7e, 0xf4, 0x64, 0xf5, 0xa0, 0x1b, 0x47,
          0xe4, 0x2e, 0xc3, 0x73, 0x63, 0x22, 0x44, 0x5e,
          0x8e, 0x22, 0x40, 0xca, 0x5e, 0x69, 0xe2, 0xc7,
          0x8b, 0x32, 0x39, 0xec, 0xfa, 0xb2, 0x16, 0x49 } },

      { "data-9 key-20",
        "Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
        "\x0b\x0b\x0b\x0b",
        { 0xaf, 0xd0, 0x39, 0x44, 0xd8, 0x48, 0x95, 0x62,
          0x6b, 0x08, 0x25, 0xf4, 0xab, 0x46, 0x90, 0x7f,
          0x15, 0xf9, 0xda, 0xdb, 0xe4, 0x10, 0x1e, 0xc6,
          0x82, 0xaa, 0x03, 0x4c, 0x7c, 0xeb, 0xc5, 0x9c,
          0xfa, 0xea, 0x9e, 0xa9, 0x07, 0x6e, 0xde, 0x7f,
          0x4a, 0xf1, 0x52, 0xe8, 0xb2, 0xfa, 0x9c, 0xb6 } },

      { "data-50 key-20",
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa",
        { 0x88, 0x06, 0x26, 0x08, 0xd3, 0xe6, 0xad, 0x8a,
          0x0a, 0xa2, 0xac, 0xe0, 0x14, 0xc8, 0xa8, 0x6f,
          0x0a, 0xa6, 0x35, 0xd9, 0x47, 0xac, 0x9f, 0xeb,
          0xe8, 0x3e, 0xf4, 0xe5, 0x59, 0x66, 0x14, 0x4b,
          0x2a, 0x5a, 0xb3, 0x9d, 0xc1, 0x38, 0x14, 0xb9,
          0x4e, 0x3a, 0xb6, 0xe1, 0x01, 0xa3, 0x4f, 0x27 } },

      { "data-50 key-26",
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10"
        "\x11\x12\x13\x14\x15\x16\x17\x18\x19",
        { 0x3e, 0x8a, 0x69, 0xb7, 0x78, 0x3c, 0x25, 0x85,
          0x19, 0x33, 0xab, 0x62, 0x90, 0xaf, 0x6c, 0xa7,
          0x7a, 0x99, 0x81, 0x48, 0x08, 0x50, 0x00, 0x9c,
          0xc5, 0x57, 0x7c, 0x6e, 0x1f, 0x57, 0x3b, 0x4e,
          0x68, 0x01, 0xdd, 0x23, 0xc4, 0xa7, 0xd6, 0x79,
          0xcc, 0xf8, 0xa3, 0x86, 0xc6, 0x74, 0xcf, 0xfb } },

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
        { 0x4e, 0xce, 0x08, 0x44, 0x85, 0x81, 0x3e, 0x90,
          0x88, 0xd2, 0xc6, 0x3a, 0x04, 0x1b, 0xc5, 0xb4,
          0x4f, 0x9e, 0xf1, 0x01, 0x2a, 0x2b, 0x58, 0x8f,
          0x3c, 0xd1, 0x1f, 0x05, 0x03, 0x3a, 0xc4, 0xc6,
          0x0c, 0x2e, 0xf6, 0xab, 0x40, 0x30, 0xfe, 0x82,
          0x96, 0x24, 0x8d, 0xf1, 0x63, 0xf4, 0x49, 0x52 } },

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
        { 0x66, 0x17, 0x17, 0x8e, 0x94, 0x1f, 0x02, 0x0d,
          0x35, 0x1e, 0x2f, 0x25, 0x4e, 0x8f, 0xd3, 0x2c,
          0x60, 0x24, 0x20, 0xfe, 0xb0, 0xb8, 0xfb, 0x9a,
          0xdc, 0xce, 0xbb, 0x82, 0x46, 0x1e, 0x99, 0xc5,
          0xa6, 0x78, 0xcc, 0x31, 0xe7, 0x99, 0x17, 0x6d,
          0x38, 0x60, 0xe6, 0x11, 0x0c, 0x46, 0x52, 0x3e } },

      { NULL }
    };
  const char *what;
  const char *errtxt;
  int tvidx;
  
  for (tvidx=0; tv[tvidx].desc; tvidx++)
    {
      what = tv[tvidx].desc;
      errtxt = check_one (GCRY_MD_SHA384,
                          tv[tvidx].data, strlen (tv[tvidx].data),
                          tv[tvidx].key, strlen (tv[tvidx].key),
                          tv[tvidx].expect, DIM (tv[tvidx].expect) );
      if (errtxt)
        goto failed;
      if (!extended)
        break;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("hmac", GCRY_MD_SHA384, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


static gpg_err_code_t
selftests_sha512 (int extended, selftest_report_func_t report)
{
  static struct 
  {
    const char * const desc;
    const char * const data;
    const char * const key;
    const char expect[64];
  } tv[] =
    {
      { "data-28 key-4",
        "what do ya want for nothing?", 
        "Jefe",
        { 0x16, 0x4b, 0x7a, 0x7b, 0xfc, 0xf8, 0x19, 0xe2,
          0xe3, 0x95, 0xfb, 0xe7, 0x3b, 0x56, 0xe0, 0xa3,
          0x87, 0xbd, 0x64, 0x22, 0x2e, 0x83, 0x1f, 0xd6,
          0x10, 0x27, 0x0c, 0xd7, 0xea, 0x25, 0x05, 0x54,
          0x97, 0x58, 0xbf, 0x75, 0xc0, 0x5a, 0x99, 0x4a,
          0x6d, 0x03, 0x4f, 0x65, 0xf8, 0xf0, 0xe6, 0xfd,
          0xca, 0xea, 0xb1, 0xa3, 0x4d, 0x4a, 0x6b, 0x4b,
          0x63, 0x6e, 0x07, 0x0a, 0x38, 0xbc, 0xe7, 0x37 } },

      { "data-9 key-20",
        "Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
        "\x0b\x0b\x0b\x0b",
        { 0x87, 0xaa, 0x7c, 0xde, 0xa5, 0xef, 0x61, 0x9d,
          0x4f, 0xf0, 0xb4, 0x24, 0x1a, 0x1d, 0x6c, 0xb0,
          0x23, 0x79, 0xf4, 0xe2, 0xce, 0x4e, 0xc2, 0x78,
          0x7a, 0xd0, 0xb3, 0x05, 0x45, 0xe1, 0x7c, 0xde,
          0xda, 0xa8, 0x33, 0xb7, 0xd6, 0xb8, 0xa7, 0x02,
          0x03, 0x8b, 0x27, 0x4e, 0xae, 0xa3, 0xf4, 0xe4,
          0xbe, 0x9d, 0x91, 0x4e, 0xeb, 0x61, 0xf1, 0x70,
          0x2e, 0x69, 0x6c, 0x20, 0x3a, 0x12, 0x68, 0x54 } },

      { "data-50 key-20",
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
        "\xdd\xdd",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
        "\xaa\xaa\xaa\xaa",
        { 0xfa, 0x73, 0xb0, 0x08, 0x9d, 0x56, 0xa2, 0x84,
          0xef, 0xb0, 0xf0, 0x75, 0x6c, 0x89, 0x0b, 0xe9,
          0xb1, 0xb5, 0xdb, 0xdd, 0x8e, 0xe8, 0x1a, 0x36,
          0x55, 0xf8, 0x3e, 0x33, 0xb2, 0x27, 0x9d, 0x39,
          0xbf, 0x3e, 0x84, 0x82, 0x79, 0xa7, 0x22, 0xc8,
          0x06, 0xb4, 0x85, 0xa4, 0x7e, 0x67, 0xc8, 0x07,
          0xb9, 0x46, 0xa3, 0x37, 0xbe, 0xe8, 0x94, 0x26,
          0x74, 0x27, 0x88, 0x59, 0xe1, 0x32, 0x92, 0xfb } },

      { "data-50 key-26",
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
        "\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10"
        "\x11\x12\x13\x14\x15\x16\x17\x18\x19",
        { 0xb0, 0xba, 0x46, 0x56, 0x37, 0x45, 0x8c, 0x69,
          0x90, 0xe5, 0xa8, 0xc5, 0xf6, 0x1d, 0x4a, 0xf7,
          0xe5, 0x76, 0xd9, 0x7f, 0xf9, 0x4b, 0x87, 0x2d,
          0xe7, 0x6f, 0x80, 0x50, 0x36, 0x1e, 0xe3, 0xdb,
          0xa9, 0x1c, 0xa5, 0xc1, 0x1a, 0xa2, 0x5e, 0xb4,
          0xd6, 0x79, 0x27, 0x5c, 0xc5, 0x78, 0x80, 0x63,
          0xa5, 0xf1, 0x97, 0x41, 0x12, 0x0c, 0x4f, 0x2d,
          0xe2, 0xad, 0xeb, 0xeb, 0x10, 0xa2, 0x98, 0xdd } },

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
        { 0x80, 0xb2, 0x42, 0x63, 0xc7, 0xc1, 0xa3, 0xeb,
          0xb7, 0x14, 0x93, 0xc1, 0xdd, 0x7b, 0xe8, 0xb4,
          0x9b, 0x46, 0xd1, 0xf4, 0x1b, 0x4a, 0xee, 0xc1,
          0x12, 0x1b, 0x01, 0x37, 0x83, 0xf8, 0xf3, 0x52,
          0x6b, 0x56, 0xd0, 0x37, 0xe0, 0x5f, 0x25, 0x98,
          0xbd, 0x0f, 0xd2, 0x21, 0x5d, 0x6a, 0x1e, 0x52,
          0x95, 0xe6, 0x4f, 0x73, 0xf6, 0x3f, 0x0a, 0xec,
          0x8b, 0x91, 0x5a, 0x98, 0x5d, 0x78, 0x65, 0x98 } },

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
        { 0xe3, 0x7b, 0x6a, 0x77, 0x5d, 0xc8, 0x7d, 0xba,
          0xa4, 0xdf, 0xa9, 0xf9, 0x6e, 0x5e, 0x3f, 0xfd,
          0xde, 0xbd, 0x71, 0xf8, 0x86, 0x72, 0x89, 0x86,
          0x5d, 0xf5, 0xa3, 0x2d, 0x20, 0xcd, 0xc9, 0x44,
          0xb6, 0x02, 0x2c, 0xac, 0x3c, 0x49, 0x82, 0xb1,
          0x0d, 0x5e, 0xeb, 0x55, 0xc3, 0xe4, 0xde, 0x15,
          0x13, 0x46, 0x76, 0xfb, 0x6d, 0xe0, 0x44, 0x60,
          0x65, 0xc9, 0x74, 0x40, 0xfa, 0x8c, 0x6a, 0x58 } },

      { NULL }
    };
  const char *what;
  const char *errtxt;
  int tvidx;
  
  for (tvidx=0; tv[tvidx].desc; tvidx++)
    {
      what = tv[tvidx].desc;
      errtxt = check_one (GCRY_MD_SHA512,
                          tv[tvidx].data, strlen (tv[tvidx].data),
                          tv[tvidx].key, strlen (tv[tvidx].key),
                          tv[tvidx].expect, DIM (tv[tvidx].expect) );
      if (errtxt)
        goto failed;
      if (!extended)
        break;
    }

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("hmac", GCRY_MD_SHA512, what, errtxt);
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
    case GCRY_MD_SHA224:
      ec = selftests_sha224 (extended, report);
      break;
    case GCRY_MD_SHA256:
      ec = selftests_sha256 (extended, report);
      break;
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




/* Run the selftests for HMAC with digest algorithm ALGO with optional
   reporting function REPORT.  */
gpg_error_t
_gcry_hmac_selftest (int algo, int extended, selftest_report_func_t report)
{
  gcry_err_code_t ec = 0;

  if (!gcry_md_test_algo (algo))
    {
      ec = run_selftests (algo, extended, report);
    }
  else
    {
      ec = GPG_ERR_DIGEST_ALGO;
      if (report)
        report ("hmac", algo, "module", "algorithm not available");
    }
  return gpg_error (ec);
}
