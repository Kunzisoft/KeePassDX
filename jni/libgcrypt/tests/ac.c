/* pubkey.c - Public key encryption/decryption tests
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
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "../src/gcrypt.h"

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

void
key_copy (gcry_ac_handle_t handle,
	  gcry_ac_key_type_t type,
	  gcry_ac_key_t *key_cp, gcry_ac_key_t key)
{
  gcry_error_t err = 0;

  err = gcry_ac_key_init (key_cp, handle, type,
			  gcry_ac_key_data_get (key));

  assert (! err);
}

void
check_one (gcry_mpi_t x)
{
  gcry_ac_handle_t handle;
  gcry_ac_key_pair_t key_pair;
  gcry_ac_key_t key_sec, key_sec_cp, key_pub, key_pub_cp;
  gcry_error_t err = 0;
  gcry_mpi_t x2;
  gcry_ac_data_t data, data2;
  gcry_ac_key_spec_rsa_t rsa_spec;

  rsa_spec.e = gcry_mpi_new (0);
  gcry_mpi_set_ui (rsa_spec.e, 1);

  err = gcry_ac_open (&handle, GCRY_AC_RSA, 0);
  assert (! err);

  err = gcry_ac_key_pair_generate (handle, 1024, &rsa_spec, &key_pair, NULL);
  assert (! err);

  key_sec = gcry_ac_key_pair_extract (key_pair, GCRY_AC_KEY_SECRET);
  key_copy (handle, GCRY_AC_KEY_SECRET, &key_sec_cp, key_sec);

  key_pub = gcry_ac_key_pair_extract (key_pair, GCRY_AC_KEY_PUBLIC);
  key_copy (handle, GCRY_AC_KEY_PUBLIC, &key_pub_cp, key_pub);

  err = gcry_ac_data_encrypt (handle, GCRY_AC_FLAG_NO_BLINDING, key_pub_cp, x, &data);
  assert (! err);

  err = gcry_ac_data_decrypt (handle, GCRY_AC_FLAG_NO_BLINDING, key_sec_cp, &x2, data);
  assert (! err);

  assert (! gcry_mpi_cmp (x, x2));

  gcry_ac_data_destroy (data);

  err = gcry_ac_data_sign (handle, key_sec, x, &data);
  assert (! err);
  err = gcry_ac_data_copy (&data2, data);
  assert (! err);
  gcry_ac_data_destroy (data);
  err = gcry_ac_data_copy (&data, data2);
  assert (! err);
  gcry_ac_data_destroy (data2);

  err = gcry_ac_data_verify (handle, key_pub, x, data);
  assert (! err);

  gcry_ac_data_destroy (data);

  err = gcry_ac_data_sign (handle, key_sec, x, &data);
  assert (! err);
  {
    const char *label;
    gcry_mpi_t y;

    err = gcry_ac_data_get_index (data, 0, 0, &label, &y);
    assert (! err);
    gcry_mpi_add_ui (y, y, 1);
    
    err = gcry_ac_data_verify (handle, key_pub, x, data);
    assert (gcry_err_code (err) == GPG_ERR_BAD_SIGNATURE);
  }

  gcry_ac_close (handle);
}

void
check_run (void)
{
  /*const char *s = "All Hail Discordia."; -- not used */
  unsigned int a = 0x4223;
  gcry_mpi_t x;

  x = gcry_mpi_new (0);
  gcry_mpi_set_ui (x, a);
  check_one (x);
  gcry_mpi_release (x);
}

int
main (int argc, char **argv)
{
  int debug = 0;
  int i = 1;

  if (argc > 1 && !strcmp (argv[1], "--verbose"))
    verbose = 1;
  else if (argc > 1 && !strcmp (argv[1], "--debug"))
    verbose = debug = 1;

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (!gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");
  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u , 0);
  /* No valuable keys are create, so we can speed up our RNG. */
  gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);

  for (; i > 0; i--)
    check_run ();
  
  return 0;
}
