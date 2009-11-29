/* keygen.c  -  key generation regression tests
 *	Copyright (C) 2003, 2005 Free Software Foundation, Inc.
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
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include "../src/gcrypt.h"



static int verbose;
static int debug;
static int error_count;

static void
fail ( const char *format, ... )
{
    va_list arg_ptr ;

    va_start( arg_ptr, format ) ;
    vfprintf (stderr, format, arg_ptr );
    va_end(arg_ptr);
    error_count++;
}

static void
die ( const char *format, ... )
{
    va_list arg_ptr ;

    va_start( arg_ptr, format ) ;
    vfprintf (stderr, format, arg_ptr );
    va_end(arg_ptr);
    exit (1);
}


static void
print_mpi (const char *text, gcry_mpi_t a)
{
  char *buf;
  void *bufaddr = &buf;
  gcry_error_t rc;

  rc = gcry_mpi_aprint (GCRYMPI_FMT_HEX, bufaddr, NULL, a);
  if (rc)
    fprintf (stderr, "%s=[error printing number: %s]\n",
             text, gpg_strerror (rc));
  else
    {
      fprintf (stderr, "%s=0x%s\n", text, buf);
      gcry_free (buf);
    }
}


static void
check_generated_rsa_key (gcry_sexp_t key, unsigned long expected_e)
{
  gcry_sexp_t skey, pkey, list;

  pkey = gcry_sexp_find_token (key, "public-key", 0);
  if (!pkey)
    fail ("public part missing in return value\n");
  else
    {
      gcry_mpi_t e = NULL;

      list = gcry_sexp_find_token (pkey, "e", 0);
      if (!list || !(e=gcry_sexp_nth_mpi (list, 1, 0)) )
        fail ("public exponent not found\n");
      else if (!expected_e)
        {
          if (verbose)
            print_mpi ("e", e);
        }
      else if ( gcry_mpi_cmp_ui (e, expected_e)) 
        {
          print_mpi ("e", e);
          fail ("public exponent is not %lu\n", expected_e);
        }
      gcry_sexp_release (list);
      gcry_mpi_release (e);
      gcry_sexp_release (pkey);
    }
 
  skey = gcry_sexp_find_token (key, "private-key", 0);
  if (!skey)
    fail ("private part missing in return value\n");
  else
    {
      int rc = gcry_pk_testkey (skey);
      if (rc)
        fail ("gcry_pk_testkey failed: %s\n", gpg_strerror (rc));
      gcry_sexp_release (skey);
    }

 }

static void
check_rsa_keys (void)
{
  gcry_sexp_t keyparm, key;
  int rc;
  int i;

  /* Check that DSA generation works and that it can grok the qbits
     argument. */
  if (verbose)
    fprintf (stderr, "creating 5 1024 bit DSA keys\n");
  for (i=0; i < 5; i++)
    {
      rc = gcry_sexp_new (&keyparm, 
                          "(genkey\n"
                          " (dsa\n"
                          "  (nbits 4:1024)\n"
                          " ))", 0, 1);
      if (rc)
        die ("error creating S-expression: %s\n", gpg_strerror (rc));
      rc = gcry_pk_genkey (&key, keyparm);
      gcry_sexp_release (keyparm);
      if (rc)
        die ("error generating DSA key: %s\n", gpg_strerror (rc));
      gcry_sexp_release (key);
      if (verbose)
        fprintf (stderr, "  done\n");
    }

  if (verbose)
    fprintf (stderr, "creating 1536 bit DSA key\n");
  rc = gcry_sexp_new (&keyparm, 
                      "(genkey\n"
                      " (dsa\n"
                      "  (nbits 4:1536)\n"
                      "  (qbits 3:224)\n"
                      " ))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gpg_strerror (rc));
  rc = gcry_pk_genkey (&key, keyparm);
  gcry_sexp_release (keyparm);
  if (rc)
    die ("error generating DSA key: %s\n", gpg_strerror (rc));
  if (debug)
    {
      char buffer[20000];
      gcry_sexp_sprint (key, GCRYSEXP_FMT_ADVANCED, buffer, sizeof buffer);
      if (verbose)
        printf ("=============================\n%s\n"
                "=============================\n", buffer);
    }
  gcry_sexp_release (key);

  if (verbose)
    fprintf (stderr, "creating 1024 bit RSA key\n");
  rc = gcry_sexp_new (&keyparm, 
                      "(genkey\n"
                      " (rsa\n"
                      "  (nbits 4:1024)\n"
                      " ))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gpg_strerror (rc));
  rc = gcry_pk_genkey (&key, keyparm);
  gcry_sexp_release (keyparm);
  if (rc)
    die ("error generating RSA key: %s\n", gpg_strerror (rc));

  check_generated_rsa_key (key, 65537);
  gcry_sexp_release (key);


  if (verbose)
    fprintf (stderr, "creating 512 bit RSA key with e=257\n");
  rc = gcry_sexp_new (&keyparm, 
                      "(genkey\n"
                      " (rsa\n"
                      "  (nbits 3:512)\n"
                      "  (rsa-use-e 3:257)\n"
                      " ))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gpg_strerror (rc));
  rc = gcry_pk_genkey (&key, keyparm);
  gcry_sexp_release (keyparm);
  if (rc)
    die ("error generating RSA key: %s\n", gpg_strerror (rc));

  check_generated_rsa_key (key, 257);
  gcry_sexp_release (key);

  if (verbose)
    fprintf (stderr, "creating 512 bit RSA key with default e\n");
  rc = gcry_sexp_new (&keyparm, 
                      "(genkey\n"
                      " (rsa\n"
                      "  (nbits 3:512)\n"
                      "  (rsa-use-e 1:0)\n"
                      " ))", 0, 1);
  if (rc)
    die ("error creating S-expression: %s\n", gpg_strerror (rc));
  rc = gcry_pk_genkey (&key, keyparm);
  gcry_sexp_release (keyparm);
  if (rc)
    die ("error generating RSA key: %s\n", gpg_strerror (rc));

  check_generated_rsa_key (key, 0); /* We don't expect a constant exponent. */
  gcry_sexp_release (key);

}


static void
check_nonce (void)
{
  char a[32], b[32];
  int i,j;
  int oops=0;

  if (verbose)
    fprintf (stderr, "checking gcry_create_nonce\n");

  gcry_create_nonce (a, sizeof a);
  for (i=0; i < 10; i++)
    {
      gcry_create_nonce (b, sizeof b);
      if (!memcmp (a, b, sizeof a))
        die ("identical nounce found\n");
    }
  for (i=0; i < 10; i++)
    {
      gcry_create_nonce (a, sizeof a);
      if (!memcmp (a, b, sizeof a))
        die ("identical nounce found\n");
    }

 again:
  for (i=1,j=0; i < sizeof a; i++)
    if (a[0] == a[i])
      j++;
  if (j+1 == sizeof (a))
    {
      if (oops)
        die ("impossible nonce found\n");
      oops++;
      gcry_create_nonce (a, sizeof a);
      goto again;
    }
}


static void
progress_cb (void *cb_data, const char *what, int printchar,
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


int
main (int argc, char **argv)
{
  if (argc > 1 && !strcmp (argv[1], "--verbose"))
    verbose = 1;
  else if (argc > 1 && !strcmp (argv[1], "--debug"))
    verbose = debug = 1;

  if (!gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");
  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u , 0);
  /* No valuable keys are create, so we can speed up our RNG. */
  gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);
  if (verbose)
    gcry_set_progress_handler ( progress_cb, NULL );

  check_rsa_keys ();
  check_nonce ();
  
  return error_count? 1:0;
}

