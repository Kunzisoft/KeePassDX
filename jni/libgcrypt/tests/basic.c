/* basic.c  -  basic regression tests
 * Copyright (C) 2001, 2002, 2003, 2005, 2008 Free Software Foundation, Inc.
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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include "../src/gcrypt.h"

typedef struct test_spec_pubkey_key
{
  const char *secret;
  const char *public;
  const char *grip;
}
test_spec_pubkey_key_t;

typedef struct test_spec_pubkey
{
  int id;
  int flags;
  test_spec_pubkey_key_t key;
}
test_spec_pubkey_t;

#define FLAG_CRYPT (1 << 0)
#define FLAG_SIGN  (1 << 1)
#define FLAG_GRIP  (1 << 2)

static int verbose;
static int error_count;
static int in_fips_mode;

static void
fail (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  error_count++;
}

static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}

#define MAX_DATA_LEN 100

void
progress_handler (void *cb_data, const char *what, int printchar,
		  int current, int total)
{
  (void)cb_data;
  (void)what;
  (void)current;
  (void)total;
  
  if (printchar == '\n')
    fputs ( "<LF>", stdout);
  else
    putchar (printchar);
  fflush (stdout);
}

static void
check_cbc_mac_cipher (void)
{
  struct tv
  {
    int algo;
    char key[MAX_DATA_LEN];
    unsigned char plaintext[MAX_DATA_LEN];
    size_t plaintextlen;
    char mac[MAX_DATA_LEN];
  }
  tv[] =
    {
      { GCRY_CIPHER_AES,
	"chicken teriyaki",
	"This is a sample plaintext for CBC MAC of sixtyfour bytes.......",
	0, "\x23\x8f\x6d\xc7\x53\x6a\x62\x97\x11\xc4\xa5\x16\x43\xea\xb0\xb6" },
      { GCRY_CIPHER_3DES,
	"abcdefghABCDEFGH01234567",
	"This is a sample plaintext for CBC MAC of sixtyfour bytes.......",
	0, "\x5c\x11\xf0\x01\x47\xbd\x3d\x3a" },
      { GCRY_CIPHER_DES,
	"abcdefgh",
	"This is a sample plaintext for CBC MAC of sixtyfour bytes.......",
	0, "\xfa\x4b\xdf\x9d\xfa\xab\x01\x70" }
    };
  gcry_cipher_hd_t hd;
  unsigned char out[MAX_DATA_LEN];
  int i, blklen, keylen;
  gcry_error_t err = 0;

  if (verbose)
    fprintf (stderr, "Starting CBC MAC checks.\n");

  for (i = 0; i < sizeof (tv) / sizeof (tv[0]); i++)
    {
      if (gcry_cipher_test_algo (tv[i].algo) && in_fips_mode)
        {
          if (verbose)
            fprintf (stderr, "  algorithm %d not available in fips mode\n",
                     tv[i].algo);
          continue;
        }

      err = gcry_cipher_open (&hd,
			      tv[i].algo,
			      GCRY_CIPHER_MODE_CBC, GCRY_CIPHER_CBC_MAC);
      if (!hd)
	{
	  fail ("cbc-mac algo %d, grcy_open_cipher failed: %s\n",
		tv[i].algo, gpg_strerror (err));
	  return;
	}

      blklen = gcry_cipher_get_algo_blklen(tv[i].algo);
      if (!blklen)
	{
	  fail ("cbc-mac algo %d, gcry_cipher_get_algo_blklen failed\n",
		 tv[i].algo);
	  gcry_cipher_close (hd);
	  return;
	}

      keylen = gcry_cipher_get_algo_keylen (tv[i].algo);
      if (!keylen)
	{
	  fail ("cbc-mac algo %d, gcry_cipher_get_algo_keylen failed\n",
		tv[i].algo);
	  return;
	}

      err = gcry_cipher_setkey (hd, tv[i].key, keylen);
      if (err)
	{
	  fail ("cbc-mac algo %d, gcry_cipher_setkey failed: %s\n",
		tv[i].algo, gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

      err = gcry_cipher_setiv (hd, NULL, 0);
      if (err)
	{
	  fail ("cbc-mac algo %d, gcry_cipher_setiv failed: %s\n",
		tv[i].algo, gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

      if (verbose)
	fprintf (stderr, "  checking CBC MAC for %s [%i]\n", 
		 gcry_cipher_algo_name (tv[i].algo),
		 tv[i].algo);
      err = gcry_cipher_encrypt (hd,
				 out, blklen,
				 tv[i].plaintext,
				 tv[i].plaintextlen ?
				 tv[i].plaintextlen :
				 strlen ((char*)tv[i].plaintext));
      if (err)
	{
	  fail ("cbc-mac algo %d, gcry_cipher_encrypt failed: %s\n",
		tv[i].algo, gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

#if 0
      {
	int j;
	for (j = 0; j < gcry_cipher_get_algo_blklen (tv[i].algo); j++)
	  printf ("\\x%02x", out[j] & 0xFF);
	printf ("\n");
      }
#endif

      if (memcmp (tv[i].mac, out, blklen))
	fail ("cbc-mac algo %d, encrypt mismatch entry %d\n", tv[i].algo, i);

      gcry_cipher_close (hd);
    }
  if (verbose)
    fprintf (stderr, "Completed CBC MAC checks.\n");
}

static void
check_aes128_cbc_cts_cipher (void)
{
  char key[128 / 8] = "chicken teriyaki";
  unsigned char plaintext[] =
    "I would like the General Gau's Chicken, please, and wonton soup.";
  struct tv
  {
    unsigned char out[MAX_DATA_LEN];
    int inlen;
  } tv[] =
    {
      { "\xc6\x35\x35\x68\xf2\xbf\x8c\xb4\xd8\xa5\x80\x36\x2d\xa7\xff\x7f"
	"\x97",
	17 },
      { "\xfc\x00\x78\x3e\x0e\xfd\xb2\xc1\xd4\x45\xd4\xc8\xef\xf7\xed\x22"
	"\x97\x68\x72\x68\xd6\xec\xcc\xc0\xc0\x7b\x25\xe2\x5e\xcf\xe5",
	31 },
      { "\x39\x31\x25\x23\xa7\x86\x62\xd5\xbe\x7f\xcb\xcc\x98\xeb\xf5\xa8"
	"\x97\x68\x72\x68\xd6\xec\xcc\xc0\xc0\x7b\x25\xe2\x5e\xcf\xe5\x84",
	32 },
      { "\x97\x68\x72\x68\xd6\xec\xcc\xc0\xc0\x7b\x25\xe2\x5e\xcf\xe5\x84"
	"\xb3\xff\xfd\x94\x0c\x16\xa1\x8c\x1b\x55\x49\xd2\xf8\x38\x02\x9e"
	"\x39\x31\x25\x23\xa7\x86\x62\xd5\xbe\x7f\xcb\xcc\x98\xeb\xf5",
	47 },
      { "\x97\x68\x72\x68\xd6\xec\xcc\xc0\xc0\x7b\x25\xe2\x5e\xcf\xe5\x84"
	"\x9d\xad\x8b\xbb\x96\xc4\xcd\xc0\x3b\xc1\x03\xe1\xa1\x94\xbb\xd8"
	"\x39\x31\x25\x23\xa7\x86\x62\xd5\xbe\x7f\xcb\xcc\x98\xeb\xf5\xa8",
	48 },
      { "\x97\x68\x72\x68\xd6\xec\xcc\xc0\xc0\x7b\x25\xe2\x5e\xcf\xe5\x84"
	"\x39\x31\x25\x23\xa7\x86\x62\xd5\xbe\x7f\xcb\xcc\x98\xeb\xf5\xa8"
	"\x48\x07\xef\xe8\x36\xee\x89\xa5\x26\x73\x0d\xbc\x2f\x7b\xc8\x40"
	"\x9d\xad\x8b\xbb\x96\xc4\xcd\xc0\x3b\xc1\x03\xe1\xa1\x94\xbb\xd8",
	64 },
    };
  gcry_cipher_hd_t hd;
  unsigned char out[MAX_DATA_LEN];
  int i;
  gcry_error_t err = 0;

  if (verbose)
    fprintf (stderr, "Starting AES128 CBC CTS checks.\n");
  err = gcry_cipher_open (&hd,
			  GCRY_CIPHER_AES,
			  GCRY_CIPHER_MODE_CBC, GCRY_CIPHER_CBC_CTS);
  if (err)
    {
      fail ("aes-cbc-cts, grcy_open_cipher failed: %s\n", gpg_strerror (err));
      return;
    }

  err = gcry_cipher_setkey (hd, key, 128 / 8);
  if (err)
    {
      fail ("aes-cbc-cts, gcry_cipher_setkey failed: %s\n",
	    gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  for (i = 0; i < sizeof (tv) / sizeof (tv[0]); i++)
    {
      err = gcry_cipher_setiv (hd, NULL, 0);
      if (err)
	{
	  fail ("aes-cbc-cts, gcry_cipher_setiv failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

      if (verbose)
	fprintf (stderr, "  checking encryption for length %i\n", tv[i].inlen);
      err = gcry_cipher_encrypt (hd, out, MAX_DATA_LEN,
				 plaintext, tv[i].inlen);
      if (err)
	{
	  fail ("aes-cbc-cts, gcry_cipher_encrypt failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

      if (memcmp (tv[i].out, out, tv[i].inlen))
	fail ("aes-cbc-cts, encrypt mismatch entry %d\n", i);

      err = gcry_cipher_setiv (hd, NULL, 0);
      if (err)
	{
	  fail ("aes-cbc-cts, gcry_cipher_setiv failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}
      if (verbose)
	fprintf (stderr, "  checking decryption for length %i\n", tv[i].inlen);
      err = gcry_cipher_decrypt (hd, out, tv[i].inlen, NULL, 0);
      if (err)
	{
	  fail ("aes-cbc-cts, gcry_cipher_decrypt failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hd);
	  return;
	}

      if (memcmp (plaintext, out, tv[i].inlen))
	fail ("aes-cbc-cts, decrypt mismatch entry %d\n", i);
    }

  gcry_cipher_close (hd);
  if (verbose)
    fprintf (stderr, "Completed AES128 CBC CTS checks.\n");
}

static void
check_ctr_cipher (void)
{
  struct tv
  {
    int algo;
    char key[MAX_DATA_LEN];
    char ctr[MAX_DATA_LEN];
    struct data
    {
      unsigned char plaintext[MAX_DATA_LEN];
      int inlen;
      char out[MAX_DATA_LEN];
    }
    data[MAX_DATA_LEN];
  } tv[] =
    {
      /* http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf */
      {	GCRY_CIPHER_AES,
	"\x2b\x7e\x15\x16\x28\xae\xd2\xa6\xab\xf7\x15\x88\x09\xcf\x4f\x3c",
	"\xf0\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xfb\xfc\xfd\xfe\xff",
	{ { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
	    16,
	    "\x87\x4d\x61\x91\xb6\x20\xe3\x26\x1b\xef\x68\x64\x99\x0d\xb6\xce" },
	  { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
	    16,
	    "\x98\x06\xf6\x6b\x79\x70\xfd\xff\x86\x17\x18\x7b\xb9\xff\xfd\xff" },
	  { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
	    16,
	    "\x5a\xe4\xdf\x3e\xdb\xd5\xd3\x5e\x5b\x4f\x09\x02\x0d\xb0\x3e\xab" },
	  { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
	    16,
	    "\x1e\x03\x1d\xda\x2f\xbe\x03\xd1\x79\x21\x70\xa0\xf3\x00\x9c\xee" },
	}
      },
      {	GCRY_CIPHER_AES192,
	"\x8e\x73\xb0\xf7\xda\x0e\x64\x52\xc8\x10\xf3\x2b"
	"\x80\x90\x79\xe5\x62\xf8\xea\xd2\x52\x2c\x6b\x7b",
	"\xf0\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xfb\xfc\xfd\xfe\xff",
	{ { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
	    16,
	    "\x1a\xbc\x93\x24\x17\x52\x1c\xa2\x4f\x2b\x04\x59\xfe\x7e\x6e\x0b" },
	  { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
	    16,
	    "\x09\x03\x39\xec\x0a\xa6\xfa\xef\xd5\xcc\xc2\xc6\xf4\xce\x8e\x94" },
	  { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
	    16,
	    "\x1e\x36\xb2\x6b\xd1\xeb\xc6\x70\xd1\xbd\x1d\x66\x56\x20\xab\xf7" },
	  { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
	    16,
	    "\x4f\x78\xa7\xf6\xd2\x98\x09\x58\x5a\x97\xda\xec\x58\xc6\xb0\x50" },
	}
      },
      {	GCRY_CIPHER_AES256,
	"\x60\x3d\xeb\x10\x15\xca\x71\xbe\x2b\x73\xae\xf0\x85\x7d\x77\x81"
	"\x1f\x35\x2c\x07\x3b\x61\x08\xd7\x2d\x98\x10\xa3\x09\x14\xdf\xf4",
	"\xf0\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xfb\xfc\xfd\xfe\xff",
	{ { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
	    16,
	    "\x60\x1e\xc3\x13\x77\x57\x89\xa5\xb7\xa7\xf5\x04\xbb\xf3\xd2\x28" },
	  { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
	    16,
	    "\xf4\x43\xe3\xca\x4d\x62\xb5\x9a\xca\x84\xe9\x90\xca\xca\xf5\xc5" },
	  { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
	    16,
	    "\x2b\x09\x30\xda\xa2\x3d\xe9\x4c\xe8\x70\x17\xba\x2d\x84\x98\x8d" },
	  { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
	    16,
	    "\xdf\xc9\xc5\x8d\xb6\x7a\xad\xa6\x13\xc2\xdd\x08\x45\x79\x41\xa6" }
	}
      }
    };
  gcry_cipher_hd_t hde, hdd;
  unsigned char out[MAX_DATA_LEN];
  int i, j, keylen, blklen;
  gcry_error_t err = 0;

  if (verbose)
    fprintf (stderr, "Starting CTR cipher checks.\n");
  for (i = 0; i < sizeof (tv) / sizeof (tv[0]); i++)
    {
      err = gcry_cipher_open (&hde, tv[i].algo, GCRY_CIPHER_MODE_CTR, 0);
      if (!err)
	err = gcry_cipher_open (&hdd, tv[i].algo, GCRY_CIPHER_MODE_CTR, 0);
      if (err)
	{
	  fail ("aes-ctr, grcy_open_cipher failed: %s\n", gpg_strerror (err));
	  return;
	}

      keylen = gcry_cipher_get_algo_keylen(tv[i].algo);
      if (!keylen)
	{
	  fail ("aes-ctr, gcry_cipher_get_algo_keylen failed\n");
	  return;
	}

      err = gcry_cipher_setkey (hde, tv[i].key, keylen);
      if (!err)
	err = gcry_cipher_setkey (hdd, tv[i].key, keylen);
      if (err)
	{
	  fail ("aes-ctr, gcry_cipher_setkey failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hde);
	  gcry_cipher_close (hdd);
	  return;
	}

      blklen = gcry_cipher_get_algo_blklen(tv[i].algo);
      if (!blklen)
	{
	  fail ("aes-ctr, gcry_cipher_get_algo_blklen failed\n");
	  return;
	}

      err = gcry_cipher_setctr (hde, tv[i].ctr, blklen);
      if (!err)
	err = gcry_cipher_setctr (hdd, tv[i].ctr, blklen);
      if (err)
	{
	  fail ("aes-ctr, gcry_cipher_setctr failed: %s\n",
		gpg_strerror (err));
	  gcry_cipher_close (hde);
	  gcry_cipher_close (hdd);
	  return;
	}

      if (verbose)
	fprintf (stderr, "  checking CTR mode for for %s [%i]\n", 
		 gcry_cipher_algo_name (tv[i].algo),
		 tv[i].algo);
      for (j = 0; tv[i].data[j].inlen; j++)
	{
	  err = gcry_cipher_encrypt (hde, out, MAX_DATA_LEN,
				     tv[i].data[j].plaintext,
				     tv[i].data[j].inlen == -1 ?
				     strlen ((char*)tv[i].data[j].plaintext) :
				     tv[i].data[j].inlen);
	  if (err)
	    {
	      fail ("aes-ctr, gcry_cipher_encrypt (%d, %d) failed: %s\n",
		    i, j, gpg_strerror (err));
	      gcry_cipher_close (hde);
	      gcry_cipher_close (hdd);
	      return;
	    }

	  if (memcmp (tv[i].data[j].out, out, tv[i].data[j].inlen))
	    fail ("aes-ctr, encrypt mismatch entry %d:%d\n", i, j);

	  err = gcry_cipher_decrypt (hdd, out, tv[i].data[j].inlen, NULL, 0);
	  if (err)
	    {
	      fail ("aes-ctr, gcry_cipher_decrypt (%d, %d) failed: %s\n",
		    i, j, gpg_strerror (err));
	      gcry_cipher_close (hde);
	      gcry_cipher_close (hdd);
	      return;
	    }

	  if (memcmp (tv[i].data[j].plaintext, out, tv[i].data[j].inlen))
	    fail ("aes-ctr, decrypt mismatch entry %d:%d\n", i, j);
	}

      gcry_cipher_close (hde);
      gcry_cipher_close (hdd);
    }
  if (verbose)
    fprintf (stderr, "Completed CTR cipher checks.\n");
}

static void
check_cfb_cipher (void)
{
  struct tv
  {
    int algo;
    char key[MAX_DATA_LEN];
    char iv[MAX_DATA_LEN];
    struct data
    {
      unsigned char plaintext[MAX_DATA_LEN];
      int inlen;
      char out[MAX_DATA_LEN];
    }
    data[MAX_DATA_LEN];
  } tv[] =
    {
      /* http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf */
      { GCRY_CIPHER_AES,
        "\x2b\x7e\x15\x16\x28\xae\xd2\xa6\xab\xf7\x15\x88\x09\xcf\x4f\x3c",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\x3b\x3f\xd9\x2e\xb7\x2d\xad\x20\x33\x34\x49\xf8\xe8\x3c\xfb\x4a" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\xc8\xa6\x45\x37\xa0\xb3\xa9\x3f\xcd\xe3\xcd\xad\x9f\x1c\xe5\x8b"},
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\x26\x75\x1f\x67\xa3\xcb\xb1\x40\xb1\x80\x8c\xf1\x87\xa4\xf4\xdf" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\xc0\x4b\x05\x35\x7c\x5d\x1c\x0e\xea\xc4\xc6\x6f\x9f\xf7\xf2\xe6" },
        }
      },
      { GCRY_CIPHER_AES192,
        "\x8e\x73\xb0\xf7\xda\x0e\x64\x52\xc8\x10\xf3\x2b"
        "\x80\x90\x79\xe5\x62\xf8\xea\xd2\x52\x2c\x6b\x7b",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\xcd\xc8\x0d\x6f\xdd\xf1\x8c\xab\x34\xc2\x59\x09\xc9\x9a\x41\x74" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\x67\xce\x7f\x7f\x81\x17\x36\x21\x96\x1a\x2b\x70\x17\x1d\x3d\x7a" },
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\x2e\x1e\x8a\x1d\xd5\x9b\x88\xb1\xc8\xe6\x0f\xed\x1e\xfa\xc4\xc9" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\xc0\x5f\x9f\x9c\xa9\x83\x4f\xa0\x42\xae\x8f\xba\x58\x4b\x09\xff" },
        }
      },
      { GCRY_CIPHER_AES256,
        "\x60\x3d\xeb\x10\x15\xca\x71\xbe\x2b\x73\xae\xf0\x85\x7d\x77\x81"
        "\x1f\x35\x2c\x07\x3b\x61\x08\xd7\x2d\x98\x10\xa3\x09\x14\xdf\xf4",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\xdc\x7e\x84\xbf\xda\x79\x16\x4b\x7e\xcd\x84\x86\x98\x5d\x38\x60" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\x39\xff\xed\x14\x3b\x28\xb1\xc8\x32\x11\x3c\x63\x31\xe5\x40\x7b" },
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\xdf\x10\x13\x24\x15\xe5\x4b\x92\xa1\x3e\xd0\xa8\x26\x7a\xe2\xf9" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\x75\xa3\x85\x74\x1a\xb9\xce\xf8\x20\x31\x62\x3d\x55\xb1\xe4\x71" }
        }
      }
    };
  gcry_cipher_hd_t hde, hdd;
  unsigned char out[MAX_DATA_LEN];
  int i, j, keylen, blklen;
  gcry_error_t err = 0;

  for (i = 0; i < sizeof (tv) / sizeof (tv[0]); i++)
    {
      err = gcry_cipher_open (&hde, tv[i].algo, GCRY_CIPHER_MODE_CFB, 0);
      if (!err)
        err = gcry_cipher_open (&hdd, tv[i].algo, GCRY_CIPHER_MODE_CFB, 0);
      if (err)
        {
          fail ("aes-cfb, grcy_open_cipher failed: %s\n", gpg_strerror (err));
          return;
        }

      keylen = gcry_cipher_get_algo_keylen(tv[i].algo);
      if (!keylen)
        {
          fail ("aes-cfb, gcry_cipher_get_algo_keylen failed\n");
          return;
        }

      err = gcry_cipher_setkey (hde, tv[i].key, keylen);
      if (!err)
        err = gcry_cipher_setkey (hdd, tv[i].key, keylen);
      if (err)
        {
          fail ("aes-cfb, gcry_cipher_setkey failed: %s\n",
                gpg_strerror (err));
          gcry_cipher_close (hde);
          gcry_cipher_close (hdd);
          return;
        }

      blklen = gcry_cipher_get_algo_blklen(tv[i].algo);
      if (!blklen)
        {
          fail ("aes-cfb, gcry_cipher_get_algo_blklen failed\n");
          return;
        }

      err = gcry_cipher_setiv (hde, tv[i].iv, blklen);
      if (!err)
        err = gcry_cipher_setiv (hdd, tv[i].iv, blklen);
      if (err)
        {
          fail ("aes-cfb, gcry_cipher_setiv failed: %s\n",
                gpg_strerror (err));
          gcry_cipher_close (hde);
          gcry_cipher_close (hdd);
          return;
        }

      for (j = 0; tv[i].data[j].inlen; j++)
        {
          err = gcry_cipher_encrypt (hde, out, MAX_DATA_LEN,
                                     tv[i].data[j].plaintext,
                                     tv[i].data[j].inlen);
          if (err)
            {
              fail ("aes-cfb, gcry_cipher_encrypt (%d, %d) failed: %s\n",
                    i, j, gpg_strerror (err));
              gcry_cipher_close (hde);
              gcry_cipher_close (hdd);
              return;
            }

          if (memcmp (tv[i].data[j].out, out, tv[i].data[j].inlen)) {
            fail ("aes-cfb, encrypt mismatch entry %d:%d\n", i, j);
	  }
          err = gcry_cipher_decrypt (hdd, out, tv[i].data[j].inlen, NULL, 0);
          if (err)
            {
              fail ("aes-cfb, gcry_cipher_decrypt (%d, %d) failed: %s\n",
                    i, j, gpg_strerror (err));
              gcry_cipher_close (hde);
              gcry_cipher_close (hdd);
              return;
            }

          if (memcmp (tv[i].data[j].plaintext, out, tv[i].data[j].inlen))
            fail ("aes-cfb, decrypt mismatch entry %d:%d\n", i, j);
        }

      gcry_cipher_close (hde);
      gcry_cipher_close (hdd);
    }
}

static void
check_ofb_cipher (void)
{
  struct tv
  {
    int algo;
    char key[MAX_DATA_LEN];
    char iv[MAX_DATA_LEN];
    struct data
    {
      unsigned char plaintext[MAX_DATA_LEN];
      int inlen;
      char out[MAX_DATA_LEN];
    }
    data[MAX_DATA_LEN];
  } tv[] =
    {
      /* http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf */
      { GCRY_CIPHER_AES,
        "\x2b\x7e\x15\x16\x28\xae\xd2\xa6\xab\xf7\x15\x88\x09\xcf\x4f\x3c",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\x3b\x3f\xd9\x2e\xb7\x2d\xad\x20\x33\x34\x49\xf8\xe8\x3c\xfb\x4a" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\x77\x89\x50\x8d\x16\x91\x8f\x03\xf5\x3c\x52\xda\xc5\x4e\xd8\x25"},
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\x97\x40\x05\x1e\x9c\x5f\xec\xf6\x43\x44\xf7\xa8\x22\x60\xed\xcc" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\x30\x4c\x65\x28\xf6\x59\xc7\x78\x66\xa5\x10\xd9\xc1\xd6\xae\x5e" },
        }
      },
      { GCRY_CIPHER_AES192,
        "\x8e\x73\xb0\xf7\xda\x0e\x64\x52\xc8\x10\xf3\x2b"
        "\x80\x90\x79\xe5\x62\xf8\xea\xd2\x52\x2c\x6b\x7b",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\xcd\xc8\x0d\x6f\xdd\xf1\x8c\xab\x34\xc2\x59\x09\xc9\x9a\x41\x74" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\xfc\xc2\x8b\x8d\x4c\x63\x83\x7c\x09\xe8\x17\x00\xc1\x10\x04\x01" },
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\x8d\x9a\x9a\xea\xc0\xf6\x59\x6f\x55\x9c\x6d\x4d\xaf\x59\xa5\xf2" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\x6d\x9f\x20\x08\x57\xca\x6c\x3e\x9c\xac\x52\x4b\xd9\xac\xc9\x2a" },
        }
      },
      { GCRY_CIPHER_AES256,
        "\x60\x3d\xeb\x10\x15\xca\x71\xbe\x2b\x73\xae\xf0\x85\x7d\x77\x81"
        "\x1f\x35\x2c\x07\x3b\x61\x08\xd7\x2d\x98\x10\xa3\x09\x14\xdf\xf4",
        "\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f",
        { { "\x6b\xc1\xbe\xe2\x2e\x40\x9f\x96\xe9\x3d\x7e\x11\x73\x93\x17\x2a",
            16,
            "\xdc\x7e\x84\xbf\xda\x79\x16\x4b\x7e\xcd\x84\x86\x98\x5d\x38\x60" },
          { "\xae\x2d\x8a\x57\x1e\x03\xac\x9c\x9e\xb7\x6f\xac\x45\xaf\x8e\x51",
            16,
            "\x4f\xeb\xdc\x67\x40\xd2\x0b\x3a\xc8\x8f\x6a\xd8\x2a\x4f\xb0\x8d" },
          { "\x30\xc8\x1c\x46\xa3\x5c\xe4\x11\xe5\xfb\xc1\x19\x1a\x0a\x52\xef",
            16,
            "\x71\xab\x47\xa0\x86\xe8\x6e\xed\xf3\x9d\x1c\x5b\xba\x97\xc4\x08" },
          { "\xf6\x9f\x24\x45\xdf\x4f\x9b\x17\xad\x2b\x41\x7b\xe6\x6c\x37\x10",
            16,
            "\x01\x26\x14\x1d\x67\xf3\x7b\xe8\x53\x8f\x5a\x8b\xe7\x40\xe4\x84" }
        }
      }
    };
  gcry_cipher_hd_t hde, hdd;
  unsigned char out[MAX_DATA_LEN];
  int i, j, keylen, blklen;
  gcry_error_t err = 0;

  for (i = 0; i < sizeof (tv) / sizeof (tv[0]); i++)
    {
      err = gcry_cipher_open (&hde, tv[i].algo, GCRY_CIPHER_MODE_OFB, 0);
      if (!err)
        err = gcry_cipher_open (&hdd, tv[i].algo, GCRY_CIPHER_MODE_OFB, 0);
      if (err)
        {
          fail ("aes-ofb, grcy_open_cipher failed: %s\n", gpg_strerror (err));
          return;
        }

      keylen = gcry_cipher_get_algo_keylen(tv[i].algo);
      if (!keylen)
        {
          fail ("aes-ofb, gcry_cipher_get_algo_keylen failed\n");
          return;
        }

      err = gcry_cipher_setkey (hde, tv[i].key, keylen);
      if (!err)
        err = gcry_cipher_setkey (hdd, tv[i].key, keylen);
      if (err)
        {
          fail ("aes-ofb, gcry_cipher_setkey failed: %s\n",
                gpg_strerror (err));
          gcry_cipher_close (hde);
          gcry_cipher_close (hdd);
          return;
        }

      blklen = gcry_cipher_get_algo_blklen(tv[i].algo);
      if (!blklen)
        {
          fail ("aes-ofb, gcry_cipher_get_algo_blklen failed\n");
          return;
        }

      err = gcry_cipher_setiv (hde, tv[i].iv, blklen);
      if (!err)
        err = gcry_cipher_setiv (hdd, tv[i].iv, blklen);
      if (err)
        {
          fail ("aes-ofb, gcry_cipher_setiv failed: %s\n",
                gpg_strerror (err));
          gcry_cipher_close (hde);
          gcry_cipher_close (hdd);
          return;
        }

      for (j = 0; tv[i].data[j].inlen; j++)
        {
          err = gcry_cipher_encrypt (hde, out, MAX_DATA_LEN,
                                     tv[i].data[j].plaintext,
                                     tv[i].data[j].inlen);
          if (err)
            {
              fail ("aes-ofb, gcry_cipher_encrypt (%d, %d) failed: %s\n",
                    i, j, gpg_strerror (err));
              gcry_cipher_close (hde);
              gcry_cipher_close (hdd);
              return;
            }

          if (memcmp (tv[i].data[j].out, out, tv[i].data[j].inlen))
            fail ("aes-ofb, encrypt mismatch entry %d:%d\n", i, j);

          err = gcry_cipher_decrypt (hdd, out, tv[i].data[j].inlen, NULL, 0);
          if (err)
            {
              fail ("aes-ofb, gcry_cipher_decrypt (%d, %d) failed: %s\n",
                    i, j, gpg_strerror (err));
              gcry_cipher_close (hde);
              gcry_cipher_close (hdd);
              return;
            }

          if (memcmp (tv[i].data[j].plaintext, out, tv[i].data[j].inlen))
            fail ("aes-ofb, decrypt mismatch entry %d:%d\n", i, j);
        }

      err = gcry_cipher_reset(hde);
      if (!err)
	err = gcry_cipher_reset(hdd);
      if (err)
	{
	  fail ("aes-ofb, gcry_cipher_reset (%d, %d) failed: %s\n",
		i, j, gpg_strerror (err));
	  gcry_cipher_close (hde);
	  gcry_cipher_close (hdd);
	  return;
	}

      /* gcry_cipher_reset clears the IV */
      err = gcry_cipher_setiv (hde, tv[i].iv, blklen);
      if (!err)
        err = gcry_cipher_setiv (hdd, tv[i].iv, blklen);
      if (err)
        {
          fail ("aes-ofb, gcry_cipher_setiv failed: %s\n",
                gpg_strerror (err));
          gcry_cipher_close (hde);
          gcry_cipher_close (hdd);
          return;
        }

      /* this time we encrypt and decrypt one byte at a time */
      for (j = 0; tv[i].data[j].inlen; j++)
        {
	  int byteNum;
	  for (byteNum = 0; byteNum < tv[i].data[j].inlen; ++byteNum) 
	    {
	      err = gcry_cipher_encrypt (hde, out+byteNum, 1,
					 (tv[i].data[j].plaintext) + byteNum,
					 1);
	      if (err)
		{
		  fail ("aes-ofb, gcry_cipher_encrypt (%d, %d) failed: %s\n",
			i, j, gpg_strerror (err));
		  gcry_cipher_close (hde);
		  gcry_cipher_close (hdd);
		  return;
		}
	    }

          if (memcmp (tv[i].data[j].out, out, tv[i].data[j].inlen))
            fail ("aes-ofb, encrypt mismatch entry %d:%d\n", i, j);

	  for (byteNum = 0; byteNum < tv[i].data[j].inlen; ++byteNum) 
	    {
	      err = gcry_cipher_decrypt (hdd, out+byteNum, 1, NULL, 0);
	      if (err)
		{
		  fail ("aes-ofb, gcry_cipher_decrypt (%d, %d) failed: %s\n",
			i, j, gpg_strerror (err));
		  gcry_cipher_close (hde);
		  gcry_cipher_close (hdd);
		  return;
		}
	    }

          if (memcmp (tv[i].data[j].plaintext, out, tv[i].data[j].inlen))
            fail ("aes-ofb, decrypt mismatch entry %d:%d\n", i, j);
        }

      gcry_cipher_close (hde);
      gcry_cipher_close (hdd);
    }
}

static void
check_one_cipher (int algo, int mode, int flags)
{
  gcry_cipher_hd_t hd;
  char key[32];
  unsigned char plain[16], in[16], out[16];
  int keylen;
  gcry_error_t err = 0;

  memcpy (key, "0123456789abcdef.,;/[]{}-=ABCDEF", 32);
  memcpy (plain, "foobar42FOOBAR17", 16);

  keylen = gcry_cipher_get_algo_keylen (algo);
  if (!keylen)
    {
      fail ("algo %d, mode %d, gcry_cipher_get_algo_keylen failed\n",
	    algo, mode);
      return;
    }

  if (keylen < 40 / 8 || keylen > 32)
    {
      fail ("algo %d, mode %d, keylength problem (%d)\n", algo, mode, keylen);
      return;
    }

  err = gcry_cipher_open (&hd, algo, mode, flags);
  if (err)
    {
      fail ("algo %d, mode %d, grcy_open_cipher failed: %s\n",
	    algo, mode, gpg_strerror (err));
      return;
    }

  err = gcry_cipher_setkey (hd, key, keylen);
  if (err)
    {
      fail ("algo %d, mode %d, gcry_cipher_setkey failed: %s\n",
	    algo, mode, gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  err = gcry_cipher_encrypt (hd, out, 16, plain, 16);
  if (err)
    {
      fail ("algo %d, mode %d, gcry_cipher_encrypt failed: %s\n",
	    algo, mode, gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  gcry_cipher_reset (hd);

  err = gcry_cipher_decrypt (hd, in, 16, out, 16);
  if (err)
    {
      fail ("algo %d, mode %d, gcry_cipher_decrypt failed: %s\n",
	    algo, mode, gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  if (memcmp (plain, in, 16))
    fail ("algo %d, mode %d, encrypt-decrypt mismatch\n", algo, mode);

  /* Again, using in-place encryption.  */
  gcry_cipher_reset (hd);

  memcpy (out, plain, 16);
  err = gcry_cipher_encrypt (hd, out, 16, NULL, 0);
  if (err)
    {
      fail ("algo %d, mode %d, in-place, gcry_cipher_encrypt failed: %s\n",
	    algo, mode, gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  gcry_cipher_reset (hd);

  err = gcry_cipher_decrypt (hd, out, 16, NULL, 0);
  if (err)
    {
      fail ("algo %d, mode %d, in-place, gcry_cipher_decrypt failed: %s\n",
	    algo, mode, gpg_strerror (err));
      gcry_cipher_close (hd);
      return;
    }

  if (memcmp (plain, out, 16))
    fail ("algo %d, mode %d, in-place, encrypt-decrypt mismatch\n",algo, mode);


  gcry_cipher_close (hd);

}


static void
check_ciphers (void)
{
  static int algos[] = {
#if USE_BLOWFISH
    GCRY_CIPHER_BLOWFISH,
#endif
#if USE_DES
    GCRY_CIPHER_DES,
    GCRY_CIPHER_3DES,
#endif
#if USE_CAST5
    GCRY_CIPHER_CAST5,
#endif
#if USE_AES
    GCRY_CIPHER_AES,
    GCRY_CIPHER_AES192,
    GCRY_CIPHER_AES256,
#endif
#if USE_TWOFISH
    GCRY_CIPHER_TWOFISH,
    GCRY_CIPHER_TWOFISH128,
#endif
#if USE_SERPENT
    GCRY_CIPHER_SERPENT128,
    GCRY_CIPHER_SERPENT192,
    GCRY_CIPHER_SERPENT256,
#endif
#if USE_RFC2268
    GCRY_CIPHER_RFC2268_40,
#endif
#if USE_SEED
    GCRY_CIPHER_SEED,
#endif
#if USE_CAMELLIA
    GCRY_CIPHER_CAMELLIA128,
    GCRY_CIPHER_CAMELLIA192,
    GCRY_CIPHER_CAMELLIA256,
#endif
    0
  };
  static int algos2[] = {
#if USE_ARCFOUR
    GCRY_CIPHER_ARCFOUR,
#endif
    0
  };
  int i;

  if (verbose)
    fprintf (stderr, "Starting Cipher checks.\n");
  for (i = 0; algos[i]; i++)
    {
      if (gcry_cipher_test_algo (algos[i]) && in_fips_mode)
        {
          if (verbose)
            fprintf (stderr, "  algorithm %d not available in fips mode\n",
		     algos[i]);
          continue;
        }
      if (verbose)
	fprintf (stderr, "  checking %s [%i]\n",
		 gcry_cipher_algo_name (algos[i]),
		 gcry_cipher_map_name (gcry_cipher_algo_name (algos[i])));

      check_one_cipher (algos[i], GCRY_CIPHER_MODE_ECB, 0);
      check_one_cipher (algos[i], GCRY_CIPHER_MODE_CFB, 0);
      check_one_cipher (algos[i], GCRY_CIPHER_MODE_OFB, 0);
      check_one_cipher (algos[i], GCRY_CIPHER_MODE_CBC, 0);
      check_one_cipher (algos[i], GCRY_CIPHER_MODE_CBC, GCRY_CIPHER_CBC_CTS);
      check_one_cipher (algos[i], GCRY_CIPHER_MODE_CTR, 0);
    }

  for (i = 0; algos2[i]; i++)
    {
      if (gcry_cipher_test_algo (algos[i]) && in_fips_mode)
        {
          if (verbose)
            fprintf (stderr, "  algorithm %d not available in fips mode\n",
		     algos[i]);
          continue;
        }
      if (verbose)
	fprintf (stderr, "  checking `%s'\n",
		 gcry_cipher_algo_name (algos2[i]));

      check_one_cipher (algos2[i], GCRY_CIPHER_MODE_STREAM, 0);
    }
  /* we have now run all cipher's selftests */

  if (verbose)
    fprintf (stderr, "Completed Cipher checks.\n");

  /* TODO: add some extra encryption to test the higher level functions */
}



static void
check_one_md (int algo, const char *data, int len, const char *expect)
{
  gcry_md_hd_t hd, hd2;
  unsigned char *p;
  int mdlen;
  int i;
  gcry_error_t err = 0;

  err = gcry_md_open (&hd, algo, 0);
  if (err)
    {
      fail ("algo %d, grcy_md_open failed: %s\n", algo, gpg_strerror (err));
      return;
    }

  mdlen = gcry_md_get_algo_dlen (algo);
  if (mdlen < 1 || mdlen > 500)
    {
      fail ("algo %d, grcy_md_get_algo_dlen failed: %d\n", algo, mdlen);
      return;
    }

  if (*data == '!' && !data[1])
    {				/* hash one million times a "a" */
      char aaa[1000];

      /* Write in odd size chunks so that we test the buffering.  */
      memset (aaa, 'a', 1000);
      for (i = 0; i < 1000; i++)
        gcry_md_write (hd, aaa, 1000);
    }
  else
    gcry_md_write (hd, data, len);

  err = gcry_md_copy (&hd2, hd);
  if (err)
    {
      fail ("algo %d, gcry_md_copy failed: %s\n", algo, gpg_strerror (err));
    }

  gcry_md_close (hd);

  p = gcry_md_read (hd2, algo);

  if (memcmp (p, expect, mdlen))
    {
      printf ("computed: ");
      for (i = 0; i < mdlen; i++)
	printf ("%02x ", p[i] & 0xFF);
      printf ("\nexpected: ");
      for (i = 0; i < mdlen; i++)
	printf ("%02x ", expect[i] & 0xFF);
      printf ("\n");

      fail ("algo %d, digest mismatch\n", algo);
    }

  gcry_md_close (hd2);
}


static void
check_digests (void)
{
  static struct algos
  {
    int md;
    const char *data;
    const char *expect;
  } algos[] =
    {
      { GCRY_MD_MD4, "",
	"\x31\xD6\xCF\xE0\xD1\x6A\xE9\x31\xB7\x3C\x59\xD7\xE0\xC0\x89\xC0" },
      { GCRY_MD_MD4, "a",
	"\xbd\xe5\x2c\xb3\x1d\xe3\x3e\x46\x24\x5e\x05\xfb\xdb\xd6\xfb\x24" },
      {	GCRY_MD_MD4, "message digest",
	"\xd9\x13\x0a\x81\x64\x54\x9f\xe8\x18\x87\x48\x06\xe1\xc7\x01\x4b" },
      {	GCRY_MD_MD5, "",
	"\xD4\x1D\x8C\xD9\x8F\x00\xB2\x04\xE9\x80\x09\x98\xEC\xF8\x42\x7E" },
      {	GCRY_MD_MD5, "a",
	"\x0C\xC1\x75\xB9\xC0\xF1\xB6\xA8\x31\xC3\x99\xE2\x69\x77\x26\x61" },
      { GCRY_MD_MD5, "abc",
	"\x90\x01\x50\x98\x3C\xD2\x4F\xB0\xD6\x96\x3F\x7D\x28\xE1\x7F\x72" },
      { GCRY_MD_MD5, "message digest",
	"\xF9\x6B\x69\x7D\x7C\xB7\x93\x8D\x52\x5A\x2F\x31\xAA\xF1\x61\xD0" },
      { GCRY_MD_SHA1, "abc",
	"\xA9\x99\x3E\x36\x47\x06\x81\x6A\xBA\x3E"
	"\x25\x71\x78\x50\xC2\x6C\x9C\xD0\xD8\x9D" },
      {	GCRY_MD_SHA1,
	"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
	"\x84\x98\x3E\x44\x1C\x3B\xD2\x6E\xBA\xAE"
	"\x4A\xA1\xF9\x51\x29\xE5\xE5\x46\x70\xF1" },
      { GCRY_MD_SHA1, "!" /* kludge for "a"*1000000 */ ,
	"\x34\xAA\x97\x3C\xD4\xC4\xDA\xA4\xF6\x1E"
	"\xEB\x2B\xDB\xAD\x27\x31\x65\x34\x01\x6F" },
      /* From RFC3874 */
      {	GCRY_MD_SHA224, "abc",
	"\x23\x09\x7d\x22\x34\x05\xd8\x22\x86\x42\xa4\x77\xbd\xa2\x55\xb3"
	"\x2a\xad\xbc\xe4\xbd\xa0\xb3\xf7\xe3\x6c\x9d\xa7" },
      {	GCRY_MD_SHA224,
	"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
	"\x75\x38\x8b\x16\x51\x27\x76\xcc\x5d\xba\x5d\xa1\xfd\x89\x01\x50"
	"\xb0\xc6\x45\x5c\xb4\xf5\x8b\x19\x52\x52\x25\x25" },
      {	GCRY_MD_SHA224, "!",
	"\x20\x79\x46\x55\x98\x0c\x91\xd8\xbb\xb4\xc1\xea\x97\x61\x8a\x4b"
	"\xf0\x3f\x42\x58\x19\x48\xb2\xee\x4e\xe7\xad\x67" },
      {	GCRY_MD_SHA256, "abc",
	"\xba\x78\x16\xbf\x8f\x01\xcf\xea\x41\x41\x40\xde\x5d\xae\x22\x23"
	"\xb0\x03\x61\xa3\x96\x17\x7a\x9c\xb4\x10\xff\x61\xf2\x00\x15\xad" },
      {	GCRY_MD_SHA256,
	"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
	"\x24\x8d\x6a\x61\xd2\x06\x38\xb8\xe5\xc0\x26\x93\x0c\x3e\x60\x39"
	"\xa3\x3c\xe4\x59\x64\xff\x21\x67\xf6\xec\xed\xd4\x19\xdb\x06\xc1" },
      {	GCRY_MD_SHA256, "!",
	"\xcd\xc7\x6e\x5c\x99\x14\xfb\x92\x81\xa1\xc7\xe2\x84\xd7\x3e\x67"
	"\xf1\x80\x9a\x48\xa4\x97\x20\x0e\x04\x6d\x39\xcc\xc7\x11\x2c\xd0" },
      {	GCRY_MD_SHA384, "abc",
	"\xcb\x00\x75\x3f\x45\xa3\x5e\x8b\xb5\xa0\x3d\x69\x9a\xc6\x50\x07"
	"\x27\x2c\x32\xab\x0e\xde\xd1\x63\x1a\x8b\x60\x5a\x43\xff\x5b\xed"
	"\x80\x86\x07\x2b\xa1\xe7\xcc\x23\x58\xba\xec\xa1\x34\xc8\x25\xa7" },
      {	GCRY_MD_SHA512, "abc",
	"\xDD\xAF\x35\xA1\x93\x61\x7A\xBA\xCC\x41\x73\x49\xAE\x20\x41\x31"
	"\x12\xE6\xFA\x4E\x89\xA9\x7E\xA2\x0A\x9E\xEE\xE6\x4B\x55\xD3\x9A"
	"\x21\x92\x99\x2A\x27\x4F\xC1\xA8\x36\xBA\x3C\x23\xA3\xFE\xEB\xBD"
	"\x45\x4D\x44\x23\x64\x3C\xE8\x0E\x2A\x9A\xC9\x4F\xA5\x4C\xA4\x9F" },
      {	GCRY_MD_RMD160, "",
	"\x9c\x11\x85\xa5\xc5\xe9\xfc\x54\x61\x28"
	"\x08\x97\x7e\xe8\xf5\x48\xb2\x25\x8d\x31" },
      {	GCRY_MD_RMD160, "a",
	"\x0b\xdc\x9d\x2d\x25\x6b\x3e\xe9\xda\xae"
	"\x34\x7b\xe6\xf4\xdc\x83\x5a\x46\x7f\xfe" },
      {	GCRY_MD_RMD160, "abc",
	"\x8e\xb2\x08\xf7\xe0\x5d\x98\x7a\x9b\x04"
	"\x4a\x8e\x98\xc6\xb0\x87\xf1\x5a\x0b\xfc" },
      {	GCRY_MD_RMD160, "message digest",
	"\x5d\x06\x89\xef\x49\xd2\xfa\xe5\x72\xb8"
	"\x81\xb1\x23\xa8\x5f\xfa\x21\x59\x5f\x36" },
      {	GCRY_MD_CRC32, "", "\x00\x00\x00\x00" },
      {	GCRY_MD_CRC32, "foo", "\x8c\x73\x65\x21" },
      { GCRY_MD_CRC32_RFC1510, "", "\x00\x00\x00\x00" },
      {	GCRY_MD_CRC32_RFC1510, "foo", "\x73\x32\xbc\x33" },
      {	GCRY_MD_CRC32_RFC1510, "test0123456789", "\xb8\x3e\x88\xd6" },
      {	GCRY_MD_CRC32_RFC1510, "MASSACHVSETTS INSTITVTE OF TECHNOLOGY",
	"\xe3\x41\x80\xf7" },
#if 0
      {	GCRY_MD_CRC32_RFC1510, "\x80\x00", "\x3b\x83\x98\x4b" },
      {	GCRY_MD_CRC32_RFC1510, "\x00\x08", "\x0e\xdb\x88\x32" },
      {	GCRY_MD_CRC32_RFC1510, "\x00\x80", "\xed\xb8\x83\x20" },
#endif
      {	GCRY_MD_CRC32_RFC1510, "\x80", "\xed\xb8\x83\x20" },
#if 0
      {	GCRY_MD_CRC32_RFC1510, "\x80\x00\x00\x00", "\xed\x59\xb6\x3b" },
      {	GCRY_MD_CRC32_RFC1510, "\x00\x00\x00\x01", "\x77\x07\x30\x96" },
#endif
      {	GCRY_MD_CRC24_RFC2440, "", "\xb7\x04\xce" },
      {	GCRY_MD_CRC24_RFC2440, "foo", "\x4f\xc2\x55" },
      {	GCRY_MD_TIGER, "",
	"\x24\xF0\x13\x0C\x63\xAC\x93\x32\x16\x16\x6E\x76"
	"\xB1\xBB\x92\x5F\xF3\x73\xDE\x2D\x49\x58\x4E\x7A" },
      {	GCRY_MD_TIGER, "abc",
	"\xF2\x58\xC1\xE8\x84\x14\xAB\x2A\x52\x7A\xB5\x41"
	"\xFF\xC5\xB8\xBF\x93\x5F\x7B\x95\x1C\x13\x29\x51" },
      {	GCRY_MD_TIGER, "Tiger",
	"\x9F\x00\xF5\x99\x07\x23\x00\xDD\x27\x6A\xBB\x38"
	"\xC8\xEB\x6D\xEC\x37\x79\x0C\x11\x6F\x9D\x2B\xDF" },
      {	GCRY_MD_TIGER, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefg"
	"hijklmnopqrstuvwxyz0123456789+-",
	"\x87\xFB\x2A\x90\x83\x85\x1C\xF7\x47\x0D\x2C\xF8"
	"\x10\xE6\xDF\x9E\xB5\x86\x44\x50\x34\xA5\xA3\x86" },
      {	GCRY_MD_TIGER, "ABCDEFGHIJKLMNOPQRSTUVWXYZ=abcdef"
	"ghijklmnopqrstuvwxyz+0123456789",
	"\x46\x7D\xB8\x08\x63\xEB\xCE\x48\x8D\xF1\xCD\x12"
	"\x61\x65\x5D\xE9\x57\x89\x65\x65\x97\x5F\x91\x97" },
      {	GCRY_MD_TIGER, "Tiger - A Fast New Hash Function, "
	"by Ross Anderson and Eli Biham",
	"\x0C\x41\x0A\x04\x29\x68\x86\x8A\x16\x71\xDA\x5A"
	"\x3F\xD2\x9A\x72\x5E\xC1\xE4\x57\xD3\xCD\xB3\x03" },
      {	GCRY_MD_TIGER, "Tiger - A Fast New Hash Function, "
	"by Ross Anderson and Eli Biham, proceedings of Fa"
	"st Software Encryption 3, Cambridge.",
	"\xEB\xF5\x91\xD5\xAF\xA6\x55\xCE\x7F\x22\x89\x4F"
	"\xF8\x7F\x54\xAC\x89\xC8\x11\xB6\xB0\xDA\x31\x93" },
      {	GCRY_MD_TIGER, "Tiger - A Fast New Hash Function, "
	"by Ross Anderson and Eli Biham, proceedings of Fa"
	"st Software Encryption 3, Cambridge, 1996.",
	"\x3D\x9A\xEB\x03\xD1\xBD\x1A\x63\x57\xB2\x77\x4D"
	"\xFD\x6D\x5B\x24\xDD\x68\x15\x1D\x50\x39\x74\xFC" },
      {	GCRY_MD_TIGER, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefgh"
	"ijklmnopqrstuvwxyz0123456789+-ABCDEFGHIJKLMNOPQRS"
	"TUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-",
	"\x00\xB8\x3E\xB4\xE5\x34\x40\xC5\x76\xAC\x6A\xAE"
	"\xE0\xA7\x48\x58\x25\xFD\x15\xE7\x0A\x59\xFF\xE4" },
      { GCRY_MD_WHIRLPOOL, "",
	"\x19\xFA\x61\xD7\x55\x22\xA4\x66\x9B\x44\xE3\x9C\x1D\x2E\x17\x26"
	"\xC5\x30\x23\x21\x30\xD4\x07\xF8\x9A\xFE\xE0\x96\x49\x97\xF7\xA7"
	"\x3E\x83\xBE\x69\x8B\x28\x8F\xEB\xCF\x88\xE3\xE0\x3C\x4F\x07\x57"
	"\xEA\x89\x64\xE5\x9B\x63\xD9\x37\x08\xB1\x38\xCC\x42\xA6\x6E\xB3" },
      { GCRY_MD_WHIRLPOOL, "a",
	"\x8A\xCA\x26\x02\x79\x2A\xEC\x6F\x11\xA6\x72\x06\x53\x1F\xB7\xD7"
	"\xF0\xDF\xF5\x94\x13\x14\x5E\x69\x73\xC4\x50\x01\xD0\x08\x7B\x42"
	"\xD1\x1B\xC6\x45\x41\x3A\xEF\xF6\x3A\x42\x39\x1A\x39\x14\x5A\x59"
	"\x1A\x92\x20\x0D\x56\x01\x95\xE5\x3B\x47\x85\x84\xFD\xAE\x23\x1A" },
      { GCRY_MD_WHIRLPOOL, "a",
	"\x8A\xCA\x26\x02\x79\x2A\xEC\x6F\x11\xA6\x72\x06\x53\x1F\xB7\xD7"
	"\xF0\xDF\xF5\x94\x13\x14\x5E\x69\x73\xC4\x50\x01\xD0\x08\x7B\x42"
	"\xD1\x1B\xC6\x45\x41\x3A\xEF\xF6\x3A\x42\x39\x1A\x39\x14\x5A\x59"
	"\x1A\x92\x20\x0D\x56\x01\x95\xE5\x3B\x47\x85\x84\xFD\xAE\x23\x1A" },
      { GCRY_MD_WHIRLPOOL,
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
	"\xDC\x37\xE0\x08\xCF\x9E\xE6\x9B\xF1\x1F\x00\xED\x9A\xBA\x26\x90"
	"\x1D\xD7\xC2\x8C\xDE\xC0\x66\xCC\x6A\xF4\x2E\x40\xF8\x2F\x3A\x1E"
	"\x08\xEB\xA2\x66\x29\x12\x9D\x8F\xB7\xCB\x57\x21\x1B\x92\x81\xA6"
	"\x55\x17\xCC\x87\x9D\x7B\x96\x21\x42\xC6\x5F\x5A\x7A\xF0\x14\x67" },
      { GCRY_MD_WHIRLPOOL,
	"!",
	"\x0C\x99\x00\x5B\xEB\x57\xEF\xF5\x0A\x7C\xF0\x05\x56\x0D\xDF\x5D"
	"\x29\x05\x7F\xD8\x6B\x20\xBF\xD6\x2D\xEC\xA0\xF1\xCC\xEA\x4A\xF5"
	"\x1F\xC1\x54\x90\xED\xDC\x47\xAF\x32\xBB\x2B\x66\xC3\x4F\xF9\xAD"
	"\x8C\x60\x08\xAD\x67\x7F\x77\x12\x69\x53\xB2\x26\xE4\xED\x8B\x01" },
      {	0 },
    };
  int i;

  if (verbose)
    fprintf (stderr, "Starting hash checks.\n");

  for (i = 0; algos[i].md; i++)
    {
      if ((gcry_md_test_algo (algos[i].md) || algos[i].md == GCRY_MD_MD5)
          && in_fips_mode)
        {
          if (verbose)
            fprintf (stderr, "  algorithm %d not available in fips mode\n",
		     algos[i].md);
          continue;
        }
      if (verbose)
	fprintf (stderr, "  checking %s [%i] for length %zi\n", 
		 gcry_md_algo_name (algos[i].md),
		 algos[i].md,
                 !strcmp (algos[i].data, "!")? 
                 1000000 : strlen(algos[i].data));

      check_one_md (algos[i].md, algos[i].data, strlen (algos[i].data),
		    algos[i].expect);
    }

  if (verbose)
    fprintf (stderr, "Completed hash checks.\n");
}

static void
check_one_hmac (int algo, const char *data, int datalen, 
		const char *key, int keylen, const char *expect)
{
  gcry_md_hd_t hd, hd2;
  unsigned char *p;
  int mdlen;
  int i;
  gcry_error_t err = 0;

  err = gcry_md_open (&hd, algo, GCRY_MD_FLAG_HMAC);
  if (err)
    {
      fail ("algo %d, grcy_md_open failed: %s\n", algo, gpg_strerror (err));
      return;
    }

  mdlen = gcry_md_get_algo_dlen (algo);
  if (mdlen < 1 || mdlen > 500)
    {
      fail ("algo %d, grcy_md_get_algo_dlen failed: %d\n", algo, mdlen);
      return;
    }

  gcry_md_setkey( hd, key, keylen );

  gcry_md_write (hd, data, datalen);

  err = gcry_md_copy (&hd2, hd);
  if (err)
    {
      fail ("algo %d, gcry_md_copy failed: %s\n", algo, gpg_strerror (err));
    }

  gcry_md_close (hd);

  p = gcry_md_read (hd2, algo);
  if (!p)
    fail("algo %d, hmac gcry_md_read failed\n", algo);

  if (memcmp (p, expect, mdlen))
    {
      printf ("computed: ");
      for (i = 0; i < mdlen; i++)
	printf ("%02x ", p[i] & 0xFF);
      printf ("\nexpected: ");
      for (i = 0; i < mdlen; i++)
	printf ("%02x ", expect[i] & 0xFF);
      printf ("\n");

      fail ("algo %d, digest mismatch\n", algo);
    }

  gcry_md_close (hd2);
}

static void
check_hmac (void)
{
  static struct algos
  {
    int md;
    const char *data;
    const char *key;
    const char *expect;
  } algos[] =
    {
      { GCRY_MD_MD5, "what do ya want for nothing?", "Jefe",
	"\x75\x0c\x78\x3e\x6a\xb0\xb5\x03\xea\xa8\x6e\x31\x0a\x5d\xb7\x38" },
      { GCRY_MD_MD5,
	"Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b",
	"\x92\x94\x72\x7a\x36\x38\xbb\x1c\x13\xf4\x8e\xf8\x15\x8b\xfc\x9d" },
      { GCRY_MD_MD5,
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd",
	"\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA",
	"\x56\xbe\x34\x52\x1d\x14\x4c\x88\xdb\xb8\xc7\x33\xf0\xe8\xb3\xf6" },
      { GCRY_MD_MD5,
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
	"\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	"\x69\x7e\xaf\x0a\xca\x3a\x3a\xea\x3a\x75\x16\x47\x46\xff\xaa\x79" },
      { GCRY_MD_MD5, "Test With Truncation",
	"\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c",
	"\x56\x46\x1e\xf2\x34\x2e\xdc\x00\xf9\xba\xb9\x95\x69\x0e\xfd\x4c" },
      { GCRY_MD_MD5, "Test Using Larger Than Block-Size Key - Hash Key First",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa",
	"\x6b\x1a\xb7\xfe\x4b\xd7\xbf\x8f\x0b\x62\xe6\xce\x61\xb9\xd0\xcd" },
      { GCRY_MD_MD5,
	"Test Using Larger Than Block-Size Key and Larger Than One Block-Size Data",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa",
	"\x6f\x63\x0f\xad\x67\xcd\xa0\xee\x1f\xb1\xf5\x62\xdb\x3a\xa5\x3e", },
      { GCRY_MD_SHA256, "what do ya want for nothing?", "Jefe",
	"\x5b\xdc\xc1\x46\xbf\x60\x75\x4e\x6a\x04\x24\x26\x08\x95\x75\xc7\x5a"
	"\x00\x3f\x08\x9d\x27\x39\x83\x9d\xec\x58\xb9\x64\xec\x38\x43" },
      { GCRY_MD_SHA256,
	"Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
	"\x0b\x0b\x0b",
	"\xb0\x34\x4c\x61\xd8\xdb\x38\x53\x5c\xa8\xaf\xce\xaf\x0b\xf1\x2b\x88"
	"\x1d\xc2\x00\xc9\x83\x3d\xa7\x26\xe9\x37\x6c\x2e\x32\xcf\xf7" },
      { GCRY_MD_SHA256,
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd",
	"\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA"
	"\xAA\xAA\xAA\xAA",
	"\x77\x3e\xa9\x1e\x36\x80\x0e\x46\x85\x4d\xb8\xeb\xd0\x91\x81\xa7"
	"\x29\x59\x09\x8b\x3e\xf8\xc1\x22\xd9\x63\x55\x14\xce\xd5\x65\xfe" },
      { GCRY_MD_SHA256,
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
	"\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	"\x82\x55\x8a\x38\x9a\x44\x3c\x0e\xa4\xcc\x81\x98\x99\xf2\x08"
	"\x3a\x85\xf0\xfa\xa3\xe5\x78\xf8\x07\x7a\x2e\x3f\xf4\x67\x29\x66\x5b" },
      { GCRY_MD_SHA256, 
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
	"\x60\xe4\x31\x59\x1e\xe0\xb6\x7f\x0d\x8a\x26\xaa\xcb\xf5\xb7\x7f"
	"\x8e\x0b\xc6\x21\x37\x28\xc5\x14\x05\x46\x04\x0f\x0e\xe3\x7f\x54" },
      { GCRY_MD_SHA256, 
	"This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa",
	"\x9b\x09\xff\xa7\x1b\x94\x2f\xcb\x27\x63\x5f\xbc\xd5\xb0\xe9\x44"
	"\xbf\xdc\x63\x64\x4f\x07\x13\x93\x8a\x7f\x51\x53\x5c\x3a\x35\xe2" },
      { GCRY_MD_SHA224, "what do ya want for nothing?", "Jefe",
	"\xa3\x0e\x01\x09\x8b\xc6\xdb\xbf\x45\x69\x0f\x3a\x7e\x9e\x6d\x0f"
	"\x8b\xbe\xa2\xa3\x9e\x61\x48\x00\x8f\xd0\x5e\x44" },
      { GCRY_MD_SHA224,
	"Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
	"\x0b\x0b\x0b",
	"\x89\x6f\xb1\x12\x8a\xbb\xdf\x19\x68\x32\x10\x7c\xd4\x9d\xf3\x3f\x47"
	"\xb4\xb1\x16\x99\x12\xba\x4f\x53\x68\x4b\x22" },
      { GCRY_MD_SHA224,
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd",
	"\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA"
	"\xAA\xAA\xAA\xAA",
	"\x7f\xb3\xcb\x35\x88\xc6\xc1\xf6\xff\xa9\x69\x4d\x7d\x6a\xd2\x64"
	"\x93\x65\xb0\xc1\xf6\x5d\x69\xd1\xec\x83\x33\xea" },
      { GCRY_MD_SHA224,
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
	"\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	"\x6c\x11\x50\x68\x74\x01\x3c\xac\x6a\x2a\xbc\x1b\xb3\x82\x62"
	"\x7c\xec\x6a\x90\xd8\x6e\xfc\x01\x2d\xe7\xaf\xec\x5a" },
      { GCRY_MD_SHA224, 
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
	"\x95\xe9\xa0\xdb\x96\x20\x95\xad\xae\xbe\x9b\x2d\x6f\x0d\xbc\xe2"
	"\xd4\x99\xf1\x12\xf2\xd2\xb7\x27\x3f\xa6\x87\x0e" },
      { GCRY_MD_SHA224, 
	"This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa",
	"\x3a\x85\x41\x66\xac\x5d\x9f\x02\x3f\x54\xd5\x17\xd0\xb3\x9d\xbd"
	"\x94\x67\x70\xdb\x9c\x2b\x95\xc9\xf6\xf5\x65\xd1" },
      { GCRY_MD_SHA384, "what do ya want for nothing?", "Jefe",
	"\xaf\x45\xd2\xe3\x76\x48\x40\x31\x61\x7f\x78\xd2\xb5\x8a\x6b\x1b"
	"\x9c\x7e\xf4\x64\xf5\xa0\x1b\x47\xe4\x2e\xc3\x73\x63\x22\x44\x5e"
	"\x8e\x22\x40\xca\x5e\x69\xe2\xc7\x8b\x32\x39\xec\xfa\xb2\x16\x49" },
      { GCRY_MD_SHA384,
	"Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
	"\x0b\x0b\x0b",
	"\xaf\xd0\x39\x44\xd8\x48\x95\x62\x6b\x08\x25\xf4\xab\x46\x90\x7f\x15"
	"\xf9\xda\xdb\xe4\x10\x1e\xc6\x82\xaa\x03\x4c\x7c\xeb\xc5\x9c\xfa\xea"
	"\x9e\xa9\x07\x6e\xde\x7f\x4a\xf1\x52\xe8\xb2\xfa\x9c\xb6" },
      { GCRY_MD_SHA384,
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd",
	"\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA"
	"\xAA\xAA\xAA\xAA",
	"\x88\x06\x26\x08\xd3\xe6\xad\x8a\x0a\xa2\xac\xe0\x14\xc8\xa8\x6f"
	"\x0a\xa6\x35\xd9\x47\xac\x9f\xeb\xe8\x3e\xf4\xe5\x59\x66\x14\x4b"
	"\x2a\x5a\xb3\x9d\xc1\x38\x14\xb9\x4e\x3a\xb6\xe1\x01\xa3\x4f\x27" },
      { GCRY_MD_SHA384,
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
	"\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	"\x3e\x8a\x69\xb7\x78\x3c\x25\x85\x19\x33\xab\x62\x90\xaf\x6c\xa7"
	"\x7a\x99\x81\x48\x08\x50\x00\x9c\xc5\x57\x7c\x6e\x1f\x57\x3b\x4e"
	"\x68\x01\xdd\x23\xc4\xa7\xd6\x79\xcc\xf8\xa3\x86\xc6\x74\xcf\xfb" },
      { GCRY_MD_SHA384, 
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
	"\x4e\xce\x08\x44\x85\x81\x3e\x90\x88\xd2\xc6\x3a\x04\x1b\xc5\xb4"
	"\x4f\x9e\xf1\x01\x2a\x2b\x58\x8f\x3c\xd1\x1f\x05\x03\x3a\xc4\xc6"
	"\x0c\x2e\xf6\xab\x40\x30\xfe\x82\x96\x24\x8d\xf1\x63\xf4\x49\x52" },
      { GCRY_MD_SHA384, 
	"This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa",
	"\x66\x17\x17\x8e\x94\x1f\x02\x0d\x35\x1e\x2f\x25\x4e\x8f\xd3\x2c"
	"\x60\x24\x20\xfe\xb0\xb8\xfb\x9a\xdc\xce\xbb\x82\x46\x1e\x99\xc5"
	"\xa6\x78\xcc\x31\xe7\x99\x17\x6d\x38\x60\xe6\x11\x0c\x46\x52\x3e" },
      { GCRY_MD_SHA512, "what do ya want for nothing?", "Jefe",
	"\x16\x4b\x7a\x7b\xfc\xf8\x19\xe2\xe3\x95\xfb\xe7\x3b\x56\xe0\xa3"
	"\x87\xbd\x64\x22\x2e\x83\x1f\xd6\x10\x27\x0c\xd7\xea\x25\x05\x54"
	"\x97\x58\xbf\x75\xc0\x5a\x99\x4a\x6d\x03\x4f\x65\xf8\xf0\xe6\xfd"
	"\xca\xea\xb1\xa3\x4d\x4a\x6b\x4b\x63\x6e\x07\x0a\x38\xbc\xe7\x37" },
      { GCRY_MD_SHA512,
	"Hi There",
	"\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
	"\x0b\x0b\x0b",
	"\x87\xaa\x7c\xde\xa5\xef\x61\x9d\x4f\xf0\xb4\x24\x1a\x1d\x6c\xb0"
	"\x23\x79\xf4\xe2\xce\x4e\xc2\x78\x7a\xd0\xb3\x05\x45\xe1\x7c\xde"
	"\xda\xa8\x33\xb7\xd6\xb8\xa7\x02\x03\x8b\x27\x4e\xae\xa3\xf4\xe4"
	"\xbe\x9d\x91\x4e\xeb\x61\xf1\x70\x2e\x69\x6c\x20\x3a\x12\x68\x54" },
      { GCRY_MD_SHA512,
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
	"\xdd\xdd\xdd\xdd\xdd",
	"\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA\xAA"
	"\xAA\xAA\xAA\xAA",
	"\xfa\x73\xb0\x08\x9d\x56\xa2\x84\xef\xb0\xf0\x75\x6c\x89\x0b\xe9"
	"\xb1\xb5\xdb\xdd\x8e\xe8\x1a\x36\x55\xf8\x3e\x33\xb2\x27\x9d\x39"
	"\xbf\x3e\x84\x82\x79\xa7\x22\xc8\x06\xb4\x85\xa4\x7e\x67\xc8\x07"
	"\xb9\x46\xa3\x37\xbe\xe8\x94\x26\x74\x27\x88\x59\xe1\x32\x92\xfb"  },
      { GCRY_MD_SHA512,
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
	"\xcd\xcd\xcd\xcd\xcd",
	"\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
	"\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
	"\xb0\xba\x46\x56\x37\x45\x8c\x69\x90\xe5\xa8\xc5\xf6\x1d\x4a\xf7"
	"\xe5\x76\xd9\x7f\xf9\x4b\x87\x2d\xe7\x6f\x80\x50\x36\x1e\xe3\xdb"
	"\xa9\x1c\xa5\xc1\x1a\xa2\x5e\xb4\xd6\x79\x27\x5c\xc5\x78\x80\x63"
	"\xa5\xf1\x97\x41\x12\x0c\x4f\x2d\xe2\xad\xeb\xeb\x10\xa2\x98\xdd" },
      { GCRY_MD_SHA512, 
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
	"\x80\xb2\x42\x63\xc7\xc1\xa3\xeb\xb7\x14\x93\xc1\xdd\x7b\xe8\xb4"
	"\x9b\x46\xd1\xf4\x1b\x4a\xee\xc1\x12\x1b\x01\x37\x83\xf8\xf3\x52"
	"\x6b\x56\xd0\x37\xe0\x5f\x25\x98\xbd\x0f\xd2\x21\x5d\x6a\x1e\x52"
	"\x95\xe6\x4f\x73\xf6\x3f\x0a\xec\x8b\x91\x5a\x98\x5d\x78\x65\x98" },
      { GCRY_MD_SHA512, 
	"This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.",
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
	"\xaa\xaa\xaa",
	"\xe3\x7b\x6a\x77\x5d\xc8\x7d\xba\xa4\xdf\xa9\xf9\x6e\x5e\x3f\xfd"
	"\xde\xbd\x71\xf8\x86\x72\x89\x86\x5d\xf5\xa3\x2d\x20\xcd\xc9\x44"
	"\xb6\x02\x2c\xac\x3c\x49\x82\xb1\x0d\x5e\xeb\x55\xc3\xe4\xde\x15"
	"\x13\x46\x76\xfb\x6d\xe0\x44\x60\x65\xc9\x74\x40\xfa\x8c\x6a\x58" },
      {	0 },
    };
  int i;

  if (verbose)
    fprintf (stderr, "Starting hashed MAC checks.\n");

  for (i = 0; algos[i].md; i++)
    {
      if ((gcry_md_test_algo (algos[i].md) || algos[i].md == GCRY_MD_MD5)
          && in_fips_mode)
        {
          if (verbose)
            fprintf (stderr, "  algorithm %d not available in fips mode\n",
		     algos[i].md);
          continue;
        }
      if (verbose)
	fprintf (stderr, 
                 "  checking %s [%i] for %zi byte key and %zi byte data\n", 
		 gcry_md_algo_name (algos[i].md),
		 algos[i].md,
		 strlen(algos[i].key), strlen(algos[i].data));

      check_one_hmac (algos[i].md, algos[i].data, strlen (algos[i].data),
		      algos[i].key, strlen(algos[i].key),
		      algos[i].expect);
    }

  if (verbose)
    fprintf (stderr, "Completed hashed MAC checks.\n");
 }

/* Check that the signature SIG matches the hash HASH. PKEY is the
   public key used for the verification. BADHASH is a hasvalue which
   should; result in a bad signature status. */
static void
verify_one_signature (gcry_sexp_t pkey, gcry_sexp_t hash,
		      gcry_sexp_t badhash, gcry_sexp_t sig)
{
  gcry_error_t rc;

  rc = gcry_pk_verify (sig, hash, pkey);
  if (rc)
    fail ("gcry_pk_verify failed: %s\n", gpg_strerror (rc));
  rc = gcry_pk_verify (sig, badhash, pkey);
  if (gcry_err_code (rc) != GPG_ERR_BAD_SIGNATURE)
    fail ("gcry_pk_verify failed to detect a bad signature: %s\n",
	  gpg_strerror (rc));
}


/* Test the public key sign function using the private ket SKEY. PKEY
   is used for verification. */
static void
check_pubkey_sign (int n, gcry_sexp_t skey, gcry_sexp_t pkey)
{
  gcry_error_t rc;
  gcry_sexp_t sig, badhash, hash;
  int dataidx;
  static const char baddata[] =
    "(data\n (flags pkcs1)\n"
    " (hash sha1 #11223344556677889900AABBCCDDEEFF10203041#))\n";
  static struct
  {
    const char *data;
    int expected_rc;
  } datas[] =
    {
      { "(data\n (flags pkcs1)\n"
	" (hash sha1 #11223344556677889900AABBCCDDEEFF10203040#))\n",
	0 },
      /* This test is to see whether hash algorithms not hard wired in
         pubkey.c are detected:  */
      { "(data\n (flags pkcs1)\n"
	" (hash oid.1.3.14.3.2.29 "
        "       #11223344556677889900AABBCCDDEEFF10203040#))\n",
	0 },
      {	"(data\n (flags )\n"
	" (hash sha1 #11223344556677889900AABBCCDDEEFF10203040#))\n",
	GPG_ERR_CONFLICT },
      {	"(data\n (flags pkcs1)\n"
	" (hash foo #11223344556677889900AABBCCDDEEFF10203040#))\n",
	GPG_ERR_DIGEST_ALGO },
      {	"(data\n (flags )\n" " (value #11223344556677889900AA#))\n",
	0 },
      {	"(data\n (flags )\n" " (value #0090223344556677889900AA#))\n",
	0 },
      { "(data\n (flags raw)\n" " (value #11223344556677889900AA#))\n",
	0 },
      {	"(data\n (flags pkcs1)\n"
	" (value #11223344556677889900AA#))\n",
	GPG_ERR_CONFLICT },
      { "(data\n (flags raw foo)\n"
	" (value #11223344556677889900AA#))\n",
	GPG_ERR_INV_FLAG },
      { NULL }
    };

  (void)n;

  rc = gcry_sexp_sscan (&badhash, NULL, baddata, strlen (baddata));
  if (rc)
    die ("converting data failed: %s\n", gpg_strerror (rc));

  for (dataidx = 0; datas[dataidx].data; dataidx++)
    {
      if (verbose)
	fprintf (stderr, "  signature test %d\n", dataidx);

      rc = gcry_sexp_sscan (&hash, NULL, datas[dataidx].data,
			    strlen (datas[dataidx].data));
      if (rc)
	die ("converting data failed: %s\n", gpg_strerror (rc));

      rc = gcry_pk_sign (&sig, hash, skey);
      if (gcry_err_code (rc) != datas[dataidx].expected_rc)
	fail ("gcry_pk_sign failed: %s\n", gpg_strerror (rc));

      if (!rc)
	verify_one_signature (pkey, hash, badhash, sig);

      gcry_sexp_release (sig);
      sig = NULL;
      gcry_sexp_release (hash);
      hash = NULL;
    }

  gcry_sexp_release (badhash);
}

static void
check_pubkey_grip (int n, const unsigned char *grip,
		   gcry_sexp_t skey, gcry_sexp_t pkey)
{
  unsigned char sgrip[20], pgrip[20];

  if (!gcry_pk_get_keygrip (skey, sgrip))
    die ("get keygrip for private RSA key failed\n");
  if (!gcry_pk_get_keygrip (pkey, pgrip))
    die ("[%i] get keygrip for public RSA key failed\n", n);
  if (memcmp (sgrip, pgrip, 20))
    fail ("[%i] keygrips don't match\n", n);
  if (memcmp (sgrip, grip, 20))
    fail ("wrong keygrip for RSA key\n");
}

static void
do_check_one_pubkey (int n, gcry_sexp_t skey, gcry_sexp_t pkey,
		     const unsigned char *grip, int flags)
{
 if (flags & FLAG_SIGN)
    check_pubkey_sign (n, skey, pkey);
 if (grip && (flags & FLAG_GRIP))
   check_pubkey_grip (n, grip, skey, pkey);
}

static void
check_one_pubkey (int n, test_spec_pubkey_t spec)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  gcry_sexp_t skey, pkey;

  err = gcry_sexp_sscan (&skey, NULL, spec.key.secret,
			 strlen (spec.key.secret));
  if (!err)
    err = gcry_sexp_sscan (&pkey, NULL, spec.key.public,
			   strlen (spec.key.public));
  if (err)
    die ("converting sample key failed: %s\n", gpg_strerror (err));

  do_check_one_pubkey (n, skey, pkey,
                       (const unsigned char*)spec.key.grip, spec.flags);
 
  gcry_sexp_release (skey);
  gcry_sexp_release (pkey);
}

static void
get_keys_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;
  if (verbose)
    fprintf (stderr, "  generating RSA key:");  
  rc = gcry_sexp_new (&key_spec,
		      in_fips_mode ? "(genkey (rsa (nbits 4:1024)))"
                      : "(genkey (rsa (nbits 4:1024)(transient-key)))", 
                      0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gpg_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating RSA key: %s\n", gpg_strerror (rc));
    
  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (! pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (! sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}

static void
check_one_pubkey_new (int n)
{
  gcry_sexp_t skey, pkey;

  get_keys_new (&pkey, &skey);
  do_check_one_pubkey (n, skey, pkey, NULL, FLAG_SIGN | FLAG_CRYPT);
}

/* Run all tests for the public key functions. */
static void
check_pubkey (void)
{
  test_spec_pubkey_t pubkeys[] =
    {
      {
	GCRY_PK_RSA, FLAG_CRYPT | FLAG_SIGN,

	{ "(private-key\n"
	  " (rsa\n"
	  "  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
	  "      2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
	  "      ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
	  "      891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
	  "  (e #010001#)\n"
	  "  (d #046129F2489D71579BE0A75FE029BD6CDB574EBF57EA8A5B0FDA942CAB943B11"
	  "      7D7BB95E5D28875E0F9FC5FCC06A72F6D502464DABDED78EF6B716177B83D5BD"
	  "      C543DC5D3FED932E59F5897E92E6F58A0F33424106A3B6FA2CBF877510E4AC21"
	  "      C3EE47851E97D12996222AC3566D4CCB0B83D164074ABF7DE655FC2446DA1781#)\n"
	  "  (p #00e861b700e17e8afe6837e7512e35b6ca11d0ae47d8b85161c67baf64377213"
	  "      fe52d772f2035b3ca830af41d8a4120e1c1c70d12cc22f00d28d31dd48a8d424f1#)\n"
	  "  (q #00f7a7ca5367c661f8e62df34f0d05c10c88e5492348dd7bddc942c9a8f369f9"
	  "      35a07785d2db805215ed786e4285df1658eed3ce84f469b81b50d358407b4ad361#)\n"
	  "  (u #304559a9ead56d2309d203811a641bb1a09626bc8eb36fffa23c968ec5bd891e"
	  "      ebbafc73ae666e01ba7c8990bae06cc2bbe10b75e69fcacb353a6473079d8e9b#)))\n",

	  "(public-key\n"
	  " (rsa\n"
	  "  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
	  "      2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
	  "      ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
	  "      891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
	  "  (e #010001#)))\n",

	  "\x32\x10\x0c\x27\x17\x3e\xf6\xe9\xc4\xe9"
	  "\xa2\x5d\x3d\x69\xf8\x6d\x37\xa4\xf9\x39"}
      },
      {
	GCRY_PK_DSA, FLAG_SIGN,

	{ "(private-key\n"
	  " (DSA\n"
	  "  (p #00AD7C0025BA1A15F775F3F2D673718391D00456978D347B33D7B49E7F32EDAB"
	  "      96273899DD8B2BB46CD6ECA263FAF04A28903503D59062A8865D2AE8ADFB5191"
	  "      CF36FFB562D0E2F5809801A1F675DAE59698A9E01EFE8D7DCFCA084F4C6F5A44"
	  "      44D499A06FFAEA5E8EF5E01F2FD20A7B7EF3F6968AFBA1FB8D91F1559D52D8777B#)\n"
	  "  (q #00EB7B5751D25EBBB7BD59D920315FD840E19AEBF9#)\n"
	  "  (g #1574363387FDFD1DDF38F4FBE135BB20C7EE4772FB94C337AF86EA8E49666503"
	  "      AE04B6BE81A2F8DD095311E0217ACA698A11E6C5D33CCDAE71498ED35D13991E"
	  "      B02F09AB40BD8F4C5ED8C75DA779D0AE104BC34C960B002377068AB4B5A1F984"
	  "      3FBA91F537F1B7CAC4D8DD6D89B0D863AF7025D549F9C765D2FC07EE208F8D15#)\n"
	  "  (y #64B11EF8871BE4AB572AA810D5D3CA11A6CDBC637A8014602C72960DB135BF46"
	  "      A1816A724C34F87330FC9E187C5D66897A04535CC2AC9164A7150ABFA8179827"
	  "      6E45831AB811EEE848EBB24D9F5F2883B6E5DDC4C659DEF944DCFD80BF4D0A20"
	  "      42CAA7DC289F0C5A9D155F02D3D551DB741A81695B74D4C8F477F9C7838EB0FB#)\n"
	  "  (x #11D54E4ADBD3034160F2CED4B7CD292A4EBF3EC0#)))\n",

	  "(public-key\n"
	  " (DSA\n"
	  "  (p #00AD7C0025BA1A15F775F3F2D673718391D00456978D347B33D7B49E7F32EDAB"
	  "      96273899DD8B2BB46CD6ECA263FAF04A28903503D59062A8865D2AE8ADFB5191"
	  "      CF36FFB562D0E2F5809801A1F675DAE59698A9E01EFE8D7DCFCA084F4C6F5A44"
	  "      44D499A06FFAEA5E8EF5E01F2FD20A7B7EF3F6968AFBA1FB8D91F1559D52D8777B#)\n"
	  "  (q #00EB7B5751D25EBBB7BD59D920315FD840E19AEBF9#)\n"
	  "  (g #1574363387FDFD1DDF38F4FBE135BB20C7EE4772FB94C337AF86EA8E49666503"
	  "      AE04B6BE81A2F8DD095311E0217ACA698A11E6C5D33CCDAE71498ED35D13991E"
	  "      B02F09AB40BD8F4C5ED8C75DA779D0AE104BC34C960B002377068AB4B5A1F984"
	  "      3FBA91F537F1B7CAC4D8DD6D89B0D863AF7025D549F9C765D2FC07EE208F8D15#)\n"
	  "  (y #64B11EF8871BE4AB572AA810D5D3CA11A6CDBC637A8014602C72960DB135BF46"
	  "      A1816A724C34F87330FC9E187C5D66897A04535CC2AC9164A7150ABFA8179827"
	  "      6E45831AB811EEE848EBB24D9F5F2883B6E5DDC4C659DEF944DCFD80BF4D0A20"
	  "      42CAA7DC289F0C5A9D155F02D3D551DB741A81695B74D4C8F477F9C7838EB0FB#)))\n",

	  "\xc6\x39\x83\x1a\x43\xe5\x05\x5d\xc6\xd8"
	  "\x4a\xa6\xf9\xeb\x23\xbf\xa9\x12\x2d\x5b" }
      },
      {
	GCRY_PK_ELG, FLAG_SIGN | FLAG_CRYPT,

	{ "(private-key\n"
	  " (ELG\n"
	  "  (p #00B93B93386375F06C2D38560F3B9C6D6D7B7506B20C1773F73F8DE56E6CD65D"
	  "      F48DFAAA1E93F57A2789B168362A0F787320499F0B2461D3A4268757A7B27517"
	  "      B7D203654A0CD484DEC6AF60C85FEB84AAC382EAF2047061FE5DAB81A20A0797"
	  "      6E87359889BAE3B3600ED718BE61D4FC993CC8098A703DD0DC942E965E8F18D2A7#)\n"
	  "  (g #05#)\n"
	  "  (y #72DAB3E83C9F7DD9A931FDECDC6522C0D36A6F0A0FEC955C5AC3C09175BBFF2B"
	  "      E588DB593DC2E420201BEB3AC17536918417C497AC0F8657855380C1FCF11C5B"
	  "      D20DB4BEE9BDF916648DE6D6E419FA446C513AAB81C30CB7B34D6007637BE675"
	  "      56CE6473E9F9EE9B9FADD275D001563336F2186F424DEC6199A0F758F6A00FF4#)\n"
	  "  (x #03C28900087B38DABF4A0AB98ACEA39BB674D6557096C01D72E31C16BDD32214#)))\n",

	  "(public-key\n"
	  " (ELG\n"
	  "  (p #00B93B93386375F06C2D38560F3B9C6D6D7B7506B20C1773F73F8DE56E6CD65D"
	  "      F48DFAAA1E93F57A2789B168362A0F787320499F0B2461D3A4268757A7B27517"
	  "      B7D203654A0CD484DEC6AF60C85FEB84AAC382EAF2047061FE5DAB81A20A0797"
	  "      6E87359889BAE3B3600ED718BE61D4FC993CC8098A703DD0DC942E965E8F18D2A7#)\n"
	  "  (g #05#)\n"
	  "  (y #72DAB3E83C9F7DD9A931FDECDC6522C0D36A6F0A0FEC955C5AC3C09175BBFF2B"
	  "      E588DB593DC2E420201BEB3AC17536918417C497AC0F8657855380C1FCF11C5B"
	  "      D20DB4BEE9BDF916648DE6D6E419FA446C513AAB81C30CB7B34D6007637BE675"
	  "      56CE6473E9F9EE9B9FADD275D001563336F2186F424DEC6199A0F758F6A00FF4#)))\n",

	  "\xa7\x99\x61\xeb\x88\x83\xd2\xf4\x05\xc8"
	  "\x4f\xba\x06\xf8\x78\x09\xbc\x1e\x20\xe5" }
      },
    };
  int i;
  if (verbose)
    fprintf (stderr, "Starting public key checks.\n");
  for (i = 0; i < sizeof (pubkeys) / sizeof (*pubkeys); i++)
    if (pubkeys[i].id)
      {
        if (gcry_pk_test_algo (pubkeys[i].id) && in_fips_mode)
          {
            if (verbose)
              fprintf (stderr, "  algorithm %d not available in fips mode\n",
                       pubkeys[i].id);
            continue;
          }
        check_one_pubkey (i, pubkeys[i]);
      }
  if (verbose)
    fprintf (stderr, "Completed public key checks.\n");

  if (verbose)
    fprintf (stderr, "Starting additional public key checks.\n");
  for (i = 0; i < sizeof (pubkeys) / sizeof (*pubkeys); i++)
    if (pubkeys[i].id)
      {
        if (gcry_pk_test_algo (pubkeys[i].id) && in_fips_mode)
          {
            if (verbose)
              fprintf (stderr, "  algorithm %d not available in fips mode\n",
                       pubkeys[i].id);
            continue;
          }
        check_one_pubkey_new (i);
      }
  if (verbose)
    fprintf (stderr, "Completed additional public key checks.\n");

}

int
main (int argc, char **argv)
{ 
  gpg_error_t err;
  int last_argc = -1;
  int debug = 0;
  int use_fips = 0;
  int selftest_only = 0;

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
      else if (!strcmp (*argv, "--verbose"))
        {
          verbose++;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--debug"))
        {
          verbose = debug = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--fips"))
        {
          use_fips = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--selftest"))
        {
          selftest_only = 1;
          verbose += 2;
          argc--; argv++;
        }
    }          

  gcry_control (GCRYCTL_SET_VERBOSITY, (int)verbose);

  if (use_fips)
    gcry_control (GCRYCTL_FORCE_FIPS_MODE, 0);

  if (!gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");

  if ( gcry_fips_mode_active () )
    in_fips_mode = 1;

  if (!in_fips_mode)
    gcry_control (GCRYCTL_DISABLE_SECMEM, 0);

  if (verbose)
    gcry_set_progress_handler (progress_handler, NULL);

  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u, 0);
  /* No valuable keys are create, so we can speed up our RNG. */
  gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);

  if (!selftest_only)
    {
      check_ciphers ();
      check_aes128_cbc_cts_cipher ();
      check_cbc_mac_cipher ();
      check_ctr_cipher ();
      check_cfb_cipher ();
      check_ofb_cipher ();
      check_digests ();
      check_hmac ();
      check_pubkey ();
    }


  if (in_fips_mode && !selftest_only)
    {
      /* If we are in fips mode do some more tests. */
      gcry_md_hd_t md;

      /* First trigger a self-test.  */
      gcry_control (GCRYCTL_FORCE_FIPS_MODE, 0);
      if (!gcry_control (GCRYCTL_OPERATIONAL_P, 0))
        fail ("not in operational state after self-test\n");
      
      /* Get us into the error state.  */
      err = gcry_md_open (&md, GCRY_MD_SHA1, 0);
      if (err)
        fail ("failed to open SHA-1 hash context: %s\n", gpg_strerror (err));
      else
        {
          err = gcry_md_enable (md, GCRY_MD_SHA256);
          if (err)
            fail ("failed to add SHA-256 hash context: %s\n",
                  gpg_strerror (err));
          else
            {
              /* gcry_md_get_algo is only defined for a context with
                 just one digest algorithm.  With our setup it should
                 put the oibrary intoerror state.  */
              fputs ("Note: Two lines with error messages follow "
                     "- this is expected\n", stderr);
              gcry_md_get_algo (md);
              gcry_md_close (md);
              if (gcry_control (GCRYCTL_OPERATIONAL_P, 0))
                fail ("expected error state but still in operational state\n");
              else
                {
                  /* Now run a self-test and to get back into
                     operational state.  */
                  gcry_control (GCRYCTL_FORCE_FIPS_MODE, 0);
                  if (!gcry_control (GCRYCTL_OPERATIONAL_P, 0))
                    fail ("did not reach operational after error "
                          "and self-test\n");
                }
            }
        }
      
    }
  else
    {
      /* If in standard mode, run selftests.  */
      if (gcry_control (GCRYCTL_SELFTEST, 0))
        fail ("running self-test failed\n");
    }

  if (verbose)
    fprintf (stderr, "\nAll tests completed. Errors: %i\n", error_count);

  if (in_fips_mode && !gcry_fips_mode_active ())
    fprintf (stderr, "FIPS mode is not anymore active\n");

  return error_count ? 1 : 0;
}
