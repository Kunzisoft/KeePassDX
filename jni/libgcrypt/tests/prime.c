/* prime.c - part of the Libgcrypt test suite.
   Copyright (C) 2001, 2002, 2003, 2005 Free Software Foundation, Inc.

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
   USA.  */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "../src/gcrypt.h"

static int verbose;

static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}

static void
check_primes (void)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  gcry_mpi_t *factors = NULL;
  gcry_mpi_t prime = NULL;
  gcry_mpi_t g;
  unsigned int i = 0;
  struct prime_spec
  {
    unsigned int prime_bits;
    unsigned int factor_bits;
    unsigned int flags;
  } prime_specs[] =
    {
      { 1024, 100, GCRY_PRIME_FLAG_SPECIAL_FACTOR },
      { 128, 0, 0 },
      { 0 },
    };

  for (i = 0; prime_specs[i].prime_bits; i++)
    {
      err = gcry_prime_generate (&prime,
				 prime_specs[i].prime_bits,
				 prime_specs[i].factor_bits,
				 &factors,
				 NULL, NULL,
				 GCRY_WEAK_RANDOM,
				 prime_specs[i].flags);
      assert (! err);
      if (verbose)
        {
          fprintf (stderr, "test %d: p = ", i);
          gcry_mpi_dump (prime);
          putc ('\n', stderr);
        }

      err = gcry_prime_check (prime, 0);
      assert (! err);

      err = gcry_prime_group_generator (&g, prime, factors, NULL);
      assert (!err);
      gcry_prime_release_factors (factors); factors = NULL;
      
      if (verbose)
        {
          fprintf (stderr, "     %d: g = ", i);
          gcry_mpi_dump (g);
          putc ('\n', stderr);
        }
      gcry_mpi_release (g);


      gcry_mpi_add_ui (prime, prime, 1);
      err = gcry_prime_check (prime, 0);
      assert (err);
    }
}

int
main (int argc, char **argv)
{
  int debug = 0;

  if ((argc > 1) && (! strcmp (argv[1], "--verbose")))
    verbose = 1;
  else if ((argc > 1) && (! strcmp (argv[1], "--debug")))
    verbose = debug = 1;

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (! gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");

  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u, 0);

  check_primes ();

  return 0;
}
