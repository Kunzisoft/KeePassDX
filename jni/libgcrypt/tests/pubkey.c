/* pubkey.c - Public key encryption/decryption tests
 *	Copyright (C) 2001, 2002, 2003, 2005 Free Software Foundation, Inc.
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
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include "../src/gcrypt.h"

/* Sample RSA keys, taken from basic.c.  */

static const char sample_private_key_1[] =
"(private-key\n"
" (openpgp-rsa\n"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
      "2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
      "ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
      "891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
"  (e #010001#)\n"
"  (d #046129F2489D71579BE0A75FE029BD6CDB574EBF57EA8A5B0FDA942CAB943B11"
      "7D7BB95E5D28875E0F9FC5FCC06A72F6D502464DABDED78EF6B716177B83D5BD"
      "C543DC5D3FED932E59F5897E92E6F58A0F33424106A3B6FA2CBF877510E4AC21"
      "C3EE47851E97D12996222AC3566D4CCB0B83D164074ABF7DE655FC2446DA1781#)\n"
"  (p #00e861b700e17e8afe6837e7512e35b6ca11d0ae47d8b85161c67baf64377213"
      "fe52d772f2035b3ca830af41d8a4120e1c1c70d12cc22f00d28d31dd48a8d424f1#)\n"
"  (q #00f7a7ca5367c661f8e62df34f0d05c10c88e5492348dd7bddc942c9a8f369f9"
      "35a07785d2db805215ed786e4285df1658eed3ce84f469b81b50d358407b4ad361#)\n"
"  (u #304559a9ead56d2309d203811a641bb1a09626bc8eb36fffa23c968ec5bd891e"
      "ebbafc73ae666e01ba7c8990bae06cc2bbe10b75e69fcacb353a6473079d8e9b#)\n"
" )\n"
")\n";

/* The same key as above but without p, q and u to test the non CRT case. */
static const char sample_private_key_1_1[] =
"(private-key\n"
" (openpgp-rsa\n"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
      "2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
      "ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
      "891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
"  (e #010001#)\n"
"  (d #046129F2489D71579BE0A75FE029BD6CDB574EBF57EA8A5B0FDA942CAB943B11"
      "7D7BB95E5D28875E0F9FC5FCC06A72F6D502464DABDED78EF6B716177B83D5BD"
      "C543DC5D3FED932E59F5897E92E6F58A0F33424106A3B6FA2CBF877510E4AC21"
      "C3EE47851E97D12996222AC3566D4CCB0B83D164074ABF7DE655FC2446DA1781#)\n"
" )\n"
")\n";

/* The same key as above but just without q to test the non CRT case.  This
   should fail. */
static const char sample_private_key_1_2[] =
"(private-key\n"
" (openpgp-rsa\n"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
      "2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
      "ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
      "891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
"  (e #010001#)\n"
"  (d #046129F2489D71579BE0A75FE029BD6CDB574EBF57EA8A5B0FDA942CAB943B11"
      "7D7BB95E5D28875E0F9FC5FCC06A72F6D502464DABDED78EF6B716177B83D5BD"
      "C543DC5D3FED932E59F5897E92E6F58A0F33424106A3B6FA2CBF877510E4AC21"
      "C3EE47851E97D12996222AC3566D4CCB0B83D164074ABF7DE655FC2446DA1781#)\n"
"  (p #00e861b700e17e8afe6837e7512e35b6ca11d0ae47d8b85161c67baf64377213"
      "fe52d772f2035b3ca830af41d8a4120e1c1c70d12cc22f00d28d31dd48a8d424f1#)\n"
"  (u #304559a9ead56d2309d203811a641bb1a09626bc8eb36fffa23c968ec5bd891e"
      "ebbafc73ae666e01ba7c8990bae06cc2bbe10b75e69fcacb353a6473079d8e9b#)\n"
" )\n"
")\n";

static const char sample_public_key_1[] =
"(public-key\n"
" (rsa\n"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
      "2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
      "ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
      "891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)\n"
"  (e #010001#)\n"
" )\n"
")\n";


static int verbose;

static void
die (const char *format, ...)
{
  va_list arg_ptr ;

  va_start( arg_ptr, format ) ;
  vfprintf (stderr, format, arg_ptr );
  va_end(arg_ptr);
  exit (1);
}

static void
show_sexp (const char *prefix, gcry_sexp_t a)
{
  char *buf;
  size_t size;

  if (prefix)
    fputs (prefix, stderr);
  size = gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, NULL, 0);
  buf = gcry_xmalloc (size);

  gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, buf, size);
  fprintf (stderr, "%.*s", (int)size, buf);
  gcry_free (buf);
}


static void
check_keys_crypt (gcry_sexp_t pkey, gcry_sexp_t skey,
		  gcry_sexp_t plain0, gpg_err_code_t decrypt_fail_code)
{
  gcry_sexp_t plain1, cipher, l;
  gcry_mpi_t x0, x1;
  int rc;
  int have_flags;

  /* Extract data from plaintext.  */
  l = gcry_sexp_find_token (plain0, "value", 0);
  x0 = gcry_sexp_nth_mpi (l, 1, GCRYMPI_FMT_USG);

  /* Encrypt data.  */
  rc = gcry_pk_encrypt (&cipher, plain0, pkey);
  if (rc)
    die ("encryption failed: %s\n", gcry_strerror (rc));

  l = gcry_sexp_find_token (cipher, "flags", 0);
  have_flags = !!l;
  gcry_sexp_release (l);

  /* Decrypt data.  */
  rc = gcry_pk_decrypt (&plain1, cipher, skey);
  gcry_sexp_release (cipher);
  if (rc)
    {
      if (decrypt_fail_code && gpg_err_code (rc) == decrypt_fail_code)
        return; /* This is the expected failure code.  */
      die ("decryption failed: %s\n", gcry_strerror (rc));
    }

  /* Extract decrypted data.  Note that for compatibility reasons, the
     output of gcry_pk_decrypt depends on whether a flags lists (even
     if empty) occurs in its input data.  Because we passed the output
     of encrypt directly to decrypt, such a flag value won't be there
     as of today.  We check it anyway. */
  l = gcry_sexp_find_token (plain1, "value", 0);
  if (l)
    {
      if (!have_flags)
        die ("compatibility mode of pk_decrypt broken\n");
      gcry_sexp_release (plain1);
      x1 = gcry_sexp_nth_mpi (l, 1, GCRYMPI_FMT_USG);
      gcry_sexp_release (l);
    }
  else
    {
      if (have_flags)
        die ("compatibility mode of pk_decrypt broken\n");
      x1 = gcry_sexp_nth_mpi (plain1, 0, GCRYMPI_FMT_USG);
      gcry_sexp_release (plain1);
    }

  /* Compare.  */
  if (gcry_mpi_cmp (x0, x1))
    die ("data corrupted\n");
}

static void
check_keys (gcry_sexp_t pkey, gcry_sexp_t skey, unsigned int nbits_data,
            gpg_err_code_t decrypt_fail_code)
{
  gcry_sexp_t plain;
  gcry_mpi_t x;
  int rc;
  
  /* Create plain text.  */
  x = gcry_mpi_new (nbits_data);
  gcry_mpi_randomize (x, nbits_data, GCRY_WEAK_RANDOM);
  
  rc = gcry_sexp_build (&plain, NULL, "(data (flags raw) (value %m))", x);
  if (rc)
    die ("converting data for encryption failed: %s\n",
	 gcry_strerror (rc));

  check_keys_crypt (pkey, skey, plain, decrypt_fail_code);
  gcry_sexp_release (plain);
  gcry_mpi_release (x);

  /* Create plain text.  */
  x = gcry_mpi_new (nbits_data);
  gcry_mpi_randomize (x, nbits_data, GCRY_WEAK_RANDOM);
  
  rc = gcry_sexp_build (&plain, NULL, 
                        "(data (flags raw no-blinding) (value %m))", x);
  if (rc)
    die ("converting data for encryption failed: %s\n",
	 gcry_strerror (rc));

  check_keys_crypt (pkey, skey, plain, decrypt_fail_code);
  gcry_sexp_release (plain);
}

static void
get_keys_sample (gcry_sexp_t *pkey, gcry_sexp_t *skey, int secret_variant)
{
  gcry_sexp_t pub_key, sec_key;
  int rc;
  static const char *secret;


  switch (secret_variant)
    {
    case 0: secret = sample_private_key_1; break;
    case 1: secret = sample_private_key_1_1; break;
    case 2: secret = sample_private_key_1_2; break;
    default: die ("BUG\n");
    }

  rc = gcry_sexp_sscan (&pub_key, NULL, sample_public_key_1,
			strlen (sample_public_key_1));
  if (!rc)
    rc = gcry_sexp_sscan (&sec_key, NULL, secret, strlen (secret));
  if (rc)
    die ("converting sample keys failed: %s\n", gcry_strerror (rc));

  *pkey = pub_key;
  *skey = sec_key;
}

static void
get_keys_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;
  
  rc = gcry_sexp_new (&key_spec,
		      "(genkey (rsa (nbits 4:1024)))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating RSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated RSA key:\n", key);

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
get_keys_x931_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;
  
  rc = gcry_sexp_new (&key_spec,
		      "(genkey (rsa (nbits 4:1024)(use-x931)))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating RSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated RSA (X9.31) key:\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
get_elg_key_new (gcry_sexp_t *pkey, gcry_sexp_t *skey, int fixed_x)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new 
    (&key_spec, 
     (fixed_x
      ? "(genkey (elg (nbits 4:1024)(xvalue my.not-so-secret.key)))"
      : "(genkey (elg (nbits 3:512)))"),
     0, 1);

  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating Elgamal key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated ELG key:\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
get_dsa_key_new (gcry_sexp_t *pkey, gcry_sexp_t *skey, int transient_key)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new (&key_spec, 
                      transient_key
                      ? "(genkey (dsa (nbits 4:1024)(transient-key)))"
                      : "(genkey (dsa (nbits 4:1024)))",
                      0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating DSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated DSA key:\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
get_dsa_key_fips186_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new 
    (&key_spec, "(genkey (dsa (nbits 4:1024)(use-fips186)))",  0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating DSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated DSA key (fips 186):\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
get_dsa_key_with_domain_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new 
    (&key_spec, 
     "(genkey (dsa (transient-key)(domain"
     "(p #d3aed1876054db831d0c1348fbb1ada72507e5fbf9a62cbd47a63aeb7859d6921"
     "4adeb9146a6ec3f43520f0fd8e3125dd8bbc5d87405d1ac5f82073cd762a3f8d7"
     "74322657c9da88a7d2f0e1a9ceb84a39cb40876179e6a76e400498de4bb9379b0"
     "5f5feb7b91eb8fea97ee17a955a0a8a37587a272c4719d6feb6b54ba4ab69#)"
     "(q #9c916d121de9a03f71fb21bc2e1c0d116f065a4f#)"
     "(g #8157c5f68ca40b3ded11c353327ab9b8af3e186dd2e8dade98761a0996dda99ab"
     "0250d3409063ad99efae48b10c6ab2bba3ea9a67b12b911a372a2bba260176fad"
     "b4b93247d9712aad13aa70216c55da9858f7a298deb670a403eb1e7c91b847f1e"
     "ccfbd14bd806fd42cf45dbb69cd6d6b43add2a78f7d16928eaa04458dea44#)"
     ")))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating DSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated DSA key:\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}

static void
get_dsa_key_fips186_with_domain_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new 
    (&key_spec, 
     "(genkey (dsa (transient-key)(use-fips186)(domain"
     "(p #d3aed1876054db831d0c1348fbb1ada72507e5fbf9a62cbd47a63aeb7859d6921"
     "4adeb9146a6ec3f43520f0fd8e3125dd8bbc5d87405d1ac5f82073cd762a3f8d7"
     "74322657c9da88a7d2f0e1a9ceb84a39cb40876179e6a76e400498de4bb9379b0"
     "5f5feb7b91eb8fea97ee17a955a0a8a37587a272c4719d6feb6b54ba4ab69#)"
     "(q #9c916d121de9a03f71fb21bc2e1c0d116f065a4f#)"
     "(g #8157c5f68ca40b3ded11c353327ab9b8af3e186dd2e8dade98761a0996dda99ab"
     "0250d3409063ad99efae48b10c6ab2bba3ea9a67b12b911a372a2bba260176fad"
     "b4b93247d9712aad13aa70216c55da9858f7a298deb670a403eb1e7c91b847f1e"
     "ccfbd14bd806fd42cf45dbb69cd6d6b43add2a78f7d16928eaa04458dea44#)"
     ")))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating DSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated DSA key:\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
get_dsa_key_fips186_with_seed_new (gcry_sexp_t *pkey, gcry_sexp_t *skey)
{
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  int rc;

  rc = gcry_sexp_new 
    (&key_spec,
     "(genkey"
     "  (dsa" 
     "    (nbits 4:1024)"
     "    (use-fips186)"
     "    (transient-key)"
     "    (derive-parms"
     "      (seed #0cb1990c1fd3626055d7a0096f8fa99807399871#))))",
     0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gcry_strerror (rc));
  rc = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (rc)
    die ("error generating DSA key: %s\n", gcry_strerror (rc));
    
  if (verbose > 1)
    show_sexp ("generated DSA key (fips 186 with seed):\n", key);

  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key\n");

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key\n");

  gcry_sexp_release (key);
  *pkey = pub_key;
  *skey = sec_key;
}


static void
check_run (void)
{
  gpg_error_t err;
  gcry_sexp_t pkey, skey;
  int variant;

  for (variant=0; variant < 3; variant++)
    {
      if (verbose)
        fprintf (stderr, "Checking sample key (%d).\n", variant);
      get_keys_sample (&pkey, &skey, variant);
      /* Check gcry_pk_testkey which requires all elements.  */
      err = gcry_pk_testkey (skey);
      if ((variant == 0 && err)
          || (variant > 0 && gpg_err_code (err) != GPG_ERR_NO_OBJ))
          die ("gcry_pk_testkey failed: %s\n", gpg_strerror (err));
      /* Run the usual check but expect an error from variant 2.  */
      check_keys (pkey, skey, 800, variant == 2? GPG_ERR_NO_OBJ : 0);
      gcry_sexp_release (pkey);
      gcry_sexp_release (skey);
    }

  if (verbose)
    fprintf (stderr, "Checking generated RSA key.\n");
  get_keys_new (&pkey, &skey);
  check_keys (pkey, skey, 800, 0);
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Checking generated RSA key (X9.31).\n");
  get_keys_x931_new (&pkey, &skey);
  check_keys (pkey, skey, 800, 0);
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Checking generated Elgamal key.\n");
  get_elg_key_new (&pkey, &skey, 0);
  check_keys (pkey, skey, 400, 0);
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Checking passphrase generated Elgamal key.\n");
  get_elg_key_new (&pkey, &skey, 1);
  check_keys (pkey, skey, 800, 0);
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Generating DSA key.\n");
  get_dsa_key_new (&pkey, &skey, 0);
  /* Fixme:  Add a check function for DSA keys.  */
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (!gcry_fips_mode_active ())
    {
      if (verbose)
        fprintf (stderr, "Generating transient DSA key.\n");
      get_dsa_key_new (&pkey, &skey, 1);
      /* Fixme:  Add a check function for DSA keys.  */
      gcry_sexp_release (pkey);
      gcry_sexp_release (skey);
    }

  if (verbose)
    fprintf (stderr, "Generating DSA key (FIPS 186).\n");
  get_dsa_key_fips186_new (&pkey, &skey);
  /* Fixme:  Add a check function for DSA keys.  */
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Generating DSA key with given domain.\n");
  get_dsa_key_with_domain_new (&pkey, &skey);
  /* Fixme:  Add a check function for DSA keys.  */
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Generating DSA key with given domain (FIPS 186).\n");
  get_dsa_key_fips186_with_domain_new (&pkey, &skey);
  /* Fixme:  Add a check function for DSA keys.  */
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);

  if (verbose)
    fprintf (stderr, "Generating DSA key with given seed (FIPS 186).\n");
  get_dsa_key_fips186_with_seed_new (&pkey, &skey);
  /* Fixme:  Add a check function for DSA keys.  */
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);
}



static gcry_mpi_t
key_param_from_sexp (gcry_sexp_t sexp, const char *topname, const char *name)
{
  gcry_sexp_t l1, l2;
  gcry_mpi_t result;

  l1 = gcry_sexp_find_token (sexp, topname, 0);
  if (!l1)
    return NULL;

  l2 = gcry_sexp_find_token (l1, name, 0);
  if (!l2)
    {
      gcry_sexp_release (l1);
      return NULL;
    }

  result = gcry_sexp_nth_mpi (l2, 1, GCRYMPI_FMT_USG);
  gcry_sexp_release (l2);
  gcry_sexp_release (l1);
  return result;
}


static void
check_x931_derived_key (int what)
{
  static struct {
    const char *param;
    const char *expected_d;
  } testtable[] = {
    { /* First example from X9.31 (D.1.1).  */
      "(genkey\n"
      "  (rsa\n"
      "    (nbits 4:1024)\n"
      "    (rsa-use-e 1:3)\n"
      "    (derive-parms\n"
      "      (Xp1 #1A1916DDB29B4EB7EB6732E128#)\n"
      "      (Xp2 #192E8AAC41C576C822D93EA433#)\n"
      "      (Xp  #D8CD81F035EC57EFE822955149D3BFF70C53520D\n"
      "            769D6D76646C7A792E16EBD89FE6FC5B605A6493\n"
      "            39DFC925A86A4C6D150B71B9EEA02D68885F5009\n"
      "            B98BD984#)\n"
      "      (Xq1 #1A5CF72EE770DE50CB09ACCEA9#)\n"
      "      (Xq2 #134E4CAA16D2350A21D775C404#)\n"
      "      (Xq  #CC1092495D867E64065DEE3E7955F2EBC7D47A2D\n"
      "            7C9953388F97DDDC3E1CA19C35CA659EDC2FC325\n"
      "            6D29C2627479C086A699A49C4C9CEE7EF7BD1B34\n"
      "            321DE34A#))))\n",
      "1CCDA20BCFFB8D517EE9666866621B11822C7950D55F4BB5BEE37989A7D173"
      "12E326718BE0D79546EAAE87A56623B919B1715FFBD7F16028FC4007741961"
      "C88C5D7B4DAAAC8D36A98C9EFBB26C8A4A0E6BC15B358E528A1AC9D0F042BE"
      "B93BCA16B541B33F80C933A3B769285C462ED5677BFE89DF07BED5C127FD13"
      "241D3C4B"
    },
    
    { /* Second example from X9.31 (D.2.1).  */
      "(genkey\n"
      "  (rsa\n"
      "    (nbits 4:1536)\n"
      "    (rsa-use-e 1:3)\n"
      "    (derive-parms\n"
      "      (Xp1 #18272558B61316348297EACA74#)\n"
      "      (Xp2 #1E970E8C6C97CEF91F05B0FA80#)\n"
      "      (Xp  #F7E943C7EF2169E930DCF23FE389EF7507EE8265\n"
      "            0D42F4A0D3A3CEFABE367999BB30EE680B2FE064\n"
      "            60F707F46005F8AA7CBFCDDC4814BBE7F0F8BC09\n"
      "            318C8E51A48D134296E40D0BBDD282DCCBDDEE1D\n"
      "            EC86F0B1C96EAFF5CDA70F9AEB6EE31E#)\n"
      "      (Xq1 #11FDDA6E8128DC1629F75192BA#)\n"
      "      (Xq2 #18AB178ECA907D72472F65E480#)\n"
      "      (Xq  #C47560011412D6E13E3E7D007B5C05DBF5FF0D0F\n"
      "            CFF1FA2070D16C7ABA93EDFB35D8700567E5913D\n"
      "            B734E3FBD15862EBC59FA0425DFA131E549136E8\n"
      "            E52397A8ABE4705EC4877D4F82C4AAC651B33DA6\n"
      "            EA14B9D5F2A263DC65626E4D6CEAC767#))))\n",
      "1FB56069985F18C4519694FB71055721A01F14422DC901C35B03A64D4A5BD1"
      "259D573305F5B056AC931B82EDB084E39A0FD1D1A86CC5B147A264F7EF4EB2"
      "0ED1E7FAAE5CAE4C30D5328B7F74C3CAA72C88B70DED8EDE207B8629DA2383"
      "B78C3CE1CA3F9F218D78C938B35763AF2A8714664CC57F5CECE2413841F5E9"
      "EDEC43B728E25A41BF3E1EF8D9EEE163286C9F8BF0F219D3B322C3E4B0389C"
      "2E8BB28DC04C47DA2BF38823731266D2CF6CC3FC181738157624EF051874D0"
      "BBCCB9F65C83"
      /* Note that this example in X9.31 gives this value for D:

        "7ED581A6617C6311465A53EDC4155C86807C5108B724070D6C0E9935296F44"
        "96755CCC17D6C15AB24C6E0BB6C2138E683F4746A1B316C51E8993DFBD3AC8"
        "3B479FEAB972B930C354CA2DFDD30F2A9CB222DC37B63B7881EE18A7688E0E"
        "DE30F38728FE7C8635E324E2CD5D8EBCAA1C51993315FD73B38904E107D7A7"
        "B7B10EDCA3896906FCF87BE367BB858CA1B27E2FC3C8674ECC8B0F92C0E270"
        "BA2ECA3701311F68AFCE208DCC499B4B3DB30FF0605CE055D893BC1461D342"
        "EF32E7D9720B" 

        This is a bug in X9.31, obviously introduced by using

           d = e^{-1} mod (p-1)(q-1)
          
         instead of using the universal exponent as required by 4.1.3:

           d = e^{-1} mod lcm(p-1,q-1)

         The examples in X9.31 seem to be pretty buggy, see
         cipher/primegen.c for another bug.  Not only that I had to
         spend 100 USD for the 66 pages of the document, it also took
         me several hours to figure out that the bugs are in the
         document and not in my code.
       */
    },  

    { /* First example from NIST RSAVS (B.1.1).  */
      "(genkey\n"
      "  (rsa\n"
      "    (nbits 4:1024)\n"
      "    (rsa-use-e 1:3)\n"
      "    (derive-parms\n"
      "      (Xp1 #1ed3d6368e101dab9124c92ac8#)\n"
      "      (Xp2 #16e5457b8844967ce83cab8c11#)\n"
      "      (Xp  #b79f2c2493b4b76f329903d7555b7f5f06aaa5ea\n"
      "            ab262da1dcda8194720672a4e02229a0c71f60ae\n"
      "            c4f0d2ed8d49ef583ca7d5eeea907c10801c302a\n"
      "            cab44595#)\n"
      "      (Xq1 #1a5d9e3fa34fb479bedea412f6#)\n"
      "      (Xq2 #1f9cca85f185341516d92e82fd#)\n"
      "      (Xq  #c8387fd38fa33ddcea6a9de1b2d55410663502db\n"
      "            c225655a9310cceac9f4cf1bce653ec916d45788\n"
      "            f8113c46bc0fa42bf5e8d0c41120c1612e2ea8bb\n"
      "            2f389eda#))))\n",
      "17ef7ad4fd96011b62d76dfb2261b4b3270ca8e07bc501be954f8719ef586b"
      "f237e8f693dd16c23e7adecc40279dc6877c62ab541df5849883a5254fccfd"
      "4072a657b7f4663953930346febd6bbd82f9a499038402cbf97fd5f068083a"
      "c81ad0335c4aab0da19cfebe060a1bac7482738efafea078e21df785e56ea0"
      "dc7e8feb"
    },

    { /* Second example from NIST RSAVS (B.1.1).  */
      "(genkey\n"
      "  (rsa\n"
      "    (nbits 4:1536)\n"
      "    (rsa-use-e 1:3)\n"
      "    (derive-parms\n"
      "      (Xp1 #1e64c1af460dff8842c22b64d0#)\n"
      "      (Xp2 #1e948edcedba84039c81f2ac0c#)\n"
      "      (Xp  #c8c67df894c882045ede26a9008ab09ea0672077\n"
      "            d7bc71d412511cd93981ddde8f91b967da404056\n"
      "            c39f105f7f239abdaff92923859920f6299e82b9\n"
      "            5bd5b8c959948f4a034d81613d6235a3953b49ce\n"
      "            26974eb7bb1f14843841281b363b9cdb#)\n"
      "      (Xq1 #1f3df0f017ddd05611a97b6adb#)\n"
      "      (Xq2 #143edd7b22d828913abf24ca4d#)\n"
      "      (Xq  #f15147d0e7c04a1e3f37adde802cdc610999bf7a\n"
      "            b0088434aaeda0c0ab3910b14d2ce56cb66bffd9\n"
      "            7552195fae8b061077e03920814d8b9cfb5a3958\n"
      "            b3a82c2a7fc97e55db543948d3396289245336ec\n"
      "            9e3cb308cc655aebd766340da8921383#))))\n",
      "1f8b19f3f5f2ac9fc599f110cad403dcd9bdf5f7f00fb2790e78e820398184"
      "1f3fb3dd230fb223d898f45719d9b2d3525587ff2b8bcc7425e40550a5b536"
      "1c8e9c1d26e83fbd9c33c64029c0e878b829d55def12912b73d94fd758c461"
      "0f473e230c41b5e4c86e27c5a5029d82c811c88525d0269b95bd2ff272994a"
      "dbd80f2c2ecf69065feb8abd8b445b9c6d306b1585d7d3d7576d49842bc7e2"
      "8b4a2f88f4a47e71c3edd35fdf83f547ea5c2b532975c551ed5268f748b2c4"
      "2ccf8a84835b"
    }
  };
  gpg_error_t err;
  gcry_sexp_t key_spec, key, pub_key, sec_key;
  gcry_mpi_t d_expected, d_have;

  if (what < 0 && what >= sizeof testtable)
    die ("invalid WHAT value\n");

  err = gcry_sexp_new (&key_spec, testtable[what].param, 0, 1);
  if (err)
    die ("error creating S-expression [%d]: %s\n", what, gpg_strerror (err));

  err = gcry_pk_genkey (&key, key_spec);
  gcry_sexp_release (key_spec);
  if (err)
    die ("error generating RSA key [%d]: %s\n", what, gpg_strerror (err));
    
  pub_key = gcry_sexp_find_token (key, "public-key", 0);
  if (!pub_key)
    die ("public part missing in key [%d]\n", what);

  sec_key = gcry_sexp_find_token (key, "private-key", 0);
  if (!sec_key)
    die ("private part missing in key [%d]\n", what);

  err = gcry_mpi_scan 
    (&d_expected, GCRYMPI_FMT_HEX, testtable[what].expected_d, 0, NULL);
  if (err)
    die ("error converting string [%d]\n", what);

  if (verbose > 1)
    show_sexp ("generated key:\n", key);

  d_have = key_param_from_sexp (sec_key, "rsa", "d");
  if (!d_have)
    die ("parameter d not found in RSA secret key [%d]\n", what);
  if (gcry_mpi_cmp (d_expected, d_have))
    {
      show_sexp (NULL, sec_key);
      die ("parameter d does match expected value [%d]\n", what); 
    }
  gcry_mpi_release (d_expected);
  gcry_mpi_release (d_have);

  gcry_sexp_release (key);
  gcry_sexp_release (pub_key);
  gcry_sexp_release (sec_key);
}




int
main (int argc, char **argv)
{
  int debug = 0;
  int i;

  if (argc > 1 && !strcmp (argv[1], "--verbose"))
    verbose = 1;
  else if (argc > 1 && !strcmp (argv[1], "--debug"))
    {
      verbose = 2;
      debug = 1;
    }

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (!gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");
  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u , 0);
  /* No valuable keys are create, so we can speed up our RNG. */
  gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);

  for (i=0; i < 2; i++)
    check_run ();

  for (i=0; i < 4; i++)
    check_x931_derived_key (i);
  
  return 0;
}
