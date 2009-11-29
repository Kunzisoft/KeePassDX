/* ac-schemes.c - Tests for ES/SSA
   Copyright (C) 2003, 2005 Free Software Foundation, Inc.

   This file is part of Libgcrypt.

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
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <errno.h>

#include "../src/gcrypt.h"

static unsigned int verbose;

static void
die (const char *format, ...)
{
  va_list arg_ptr ;

  va_start( arg_ptr, format ) ;
  vfprintf (stderr, format, arg_ptr );
  va_end(arg_ptr);
  exit (1);
}

typedef struct scheme_spec
{
  unsigned int idx;
  gcry_ac_scheme_t scheme;
  unsigned int flags;
  const char *m;
  size_t m_n;
} scheme_spec_t;

#define SCHEME_SPEC_FLAG_GET_OPTS (1 << 0)

#define FILL(idx, scheme, flags, m) \
    { idx, GCRY_AC_##scheme, flags, m, sizeof (m) }

scheme_spec_t es_specs[] =
  {
    FILL (0, ES_PKCS_V1_5, 0, "foobar"),
    FILL (1, ES_PKCS_V1_5, 0, "")
  };

scheme_spec_t ssa_specs[] =
  {
    FILL (0, SSA_PKCS_V1_5, SCHEME_SPEC_FLAG_GET_OPTS, "foobar")
  };

#undef FILL

gcry_err_code_t
scheme_get_opts (scheme_spec_t specs, void **opts)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  void *opts_new = NULL;

  switch (specs.scheme)
    {
    case GCRY_AC_SSA_PKCS_V1_5:
      {
	gcry_ac_ssa_pkcs_v1_5_t *opts_pkcs_v1_5 = NULL;
	
	opts_new = gcry_malloc (sizeof (gcry_ac_ssa_pkcs_v1_5_t));
	if (! opts_new)
	  err = gpg_err_code_from_errno (ENOMEM);
	else
	  {
	    opts_pkcs_v1_5 = (gcry_ac_ssa_pkcs_v1_5_t *) opts_new;

	    switch (specs.idx)
	      {
	      case 0:
		opts_pkcs_v1_5->md = GCRY_MD_SHA1;
		break;
	      case 1:
		opts_pkcs_v1_5->md = GCRY_MD_MD5;
		break;
	      }
	  }
      }
    case GCRY_AC_ES_PKCS_V1_5:
      break;
    }

  if (! err)
    *opts = opts_new;

  return err;
}

gcry_error_t
es_check (gcry_ac_handle_t handle, scheme_spec_t spec,
	  gcry_ac_key_t key_public, gcry_ac_key_t key_secret)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  char *c = NULL;
  char *m2 = NULL;
  size_t c_n = 0;
  size_t m2_n = 0;
  void *opts = NULL;
  gcry_ac_io_t io_m;
  gcry_ac_io_t io_c;
  gcry_ac_io_t io_m2;
  
  if (spec.flags & SCHEME_SPEC_FLAG_GET_OPTS)
    err = scheme_get_opts (spec, &opts);
  if (! err)
    {
      c = NULL;
      m2 = NULL;

      gcry_ac_io_init (&io_m, GCRY_AC_IO_READABLE,
		       GCRY_AC_IO_STRING, spec.m, spec.m_n);
      gcry_ac_io_init (&io_c, GCRY_AC_IO_WRITABLE,
		       GCRY_AC_IO_STRING, &c, &c_n);

      err = gcry_ac_data_encrypt_scheme (handle, GCRY_AC_ES_PKCS_V1_5, 0, opts, key_public,
					 &io_m, &io_c);
      if (! err)
	{
	  gcry_ac_io_init (&io_c, GCRY_AC_IO_READABLE,
			   GCRY_AC_IO_STRING, c, c_n);
	  gcry_ac_io_init (&io_m2, GCRY_AC_IO_WRITABLE,
			   GCRY_AC_IO_STRING, &m2, &m2_n);

	  err = gcry_ac_data_decrypt_scheme (handle, GCRY_AC_ES_PKCS_V1_5, 0,
					     opts, key_secret, &io_c, &io_m2);
	}
      if (! err)
	assert ((spec.m_n == m2_n) && (! strncmp (spec.m, m2, spec.m_n)));

      if (c)
	gcry_free (c);
      if (m2)
	gcry_free (m2);
    }

  if (opts)
    gcry_free (opts);

  return err;
}

gcry_error_t
ssa_check (gcry_ac_handle_t handle, scheme_spec_t spec,
	   gcry_ac_key_t key_public, gcry_ac_key_t key_secret)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  unsigned char *s = NULL;
  size_t s_n = 0;
  void *opts = NULL;
  gcry_ac_io_t io_m;
  gcry_ac_io_t io_s;
  
  if (spec.flags & SCHEME_SPEC_FLAG_GET_OPTS)
    err = scheme_get_opts (spec, &opts);
  if (! err)
    {
      gcry_ac_io_init (&io_m, GCRY_AC_IO_READABLE,
		       GCRY_AC_IO_STRING, spec.m, spec.m_n);
      gcry_ac_io_init (&io_s, GCRY_AC_IO_WRITABLE,
		       GCRY_AC_IO_STRING, &s, &s_n);

      err = gcry_ac_data_sign_scheme (handle, GCRY_AC_SSA_PKCS_V1_5, 0, opts, key_secret,
				      &io_m, &io_s);
      if (! err)
	{
	  gcry_ac_io_init (&io_m, GCRY_AC_IO_READABLE,
			   GCRY_AC_IO_STRING, spec.m, spec.m_n);
	  gcry_ac_io_init (&io_s, GCRY_AC_IO_READABLE,
			   GCRY_AC_IO_STRING, s, s_n);
	  err = gcry_ac_data_verify_scheme (handle, GCRY_AC_SSA_PKCS_V1_5, 0, opts, key_public,
					    &io_m, &io_s);
	}
      assert (! err);

      if (s)
	gcry_free (s);
    }

  if (opts)
    gcry_free (opts);

  return err;
}

void
es_checks (gcry_ac_handle_t handle, gcry_ac_key_t key_public, gcry_ac_key_t key_secret)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  unsigned int i = 0;

  for (i = 0; (i < (sizeof (es_specs) / sizeof (*es_specs))) && (! err); i++)
    err = es_check (handle, es_specs[i], key_public, key_secret);

  assert (! err);
}

void
ssa_checks (gcry_ac_handle_t handle, gcry_ac_key_t key_public, gcry_ac_key_t key_secret)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  unsigned int i = 0;

  for (i = 0; (i < (sizeof (ssa_specs) / sizeof (*ssa_specs))) && (! err); i++)
    err = ssa_check (handle, ssa_specs[i], key_public, key_secret);

  assert (! err);
}

#define KEY_TYPE_PUBLIC (1 << 0)
#define KEY_TYPE_SECRET (1 << 1)

typedef struct key_spec
{
  const char *name;
  unsigned int flags;
  const char *mpi_string;
} key_spec_t;

key_spec_t key_specs[] =
  {
    { "n", KEY_TYPE_PUBLIC | KEY_TYPE_SECRET,
      "e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
      "2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
      "ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
      "891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251" },
    { "e", KEY_TYPE_PUBLIC | KEY_TYPE_SECRET,
      "010001" },
    { "d", KEY_TYPE_SECRET,
      "046129F2489D71579BE0A75FE029BD6CDB574EBF57EA8A5B0FDA942CAB943B11"
      "7D7BB95E5D28875E0F9FC5FCC06A72F6D502464DABDED78EF6B716177B83D5BD"
      "C543DC5D3FED932E59F5897E92E6F58A0F33424106A3B6FA2CBF877510E4AC21"
      "C3EE47851E97D12996222AC3566D4CCB0B83D164074ABF7DE655FC2446DA1781" },
    { "p", KEY_TYPE_SECRET,
      "00e861b700e17e8afe6837e7512e35b6ca11d0ae47d8b85161c67baf64377213"
      "fe52d772f2035b3ca830af41d8a4120e1c1c70d12cc22f00d28d31dd48a8d424f1" },
    { "q", KEY_TYPE_SECRET,
      "00f7a7ca5367c661f8e62df34f0d05c10c88e5492348dd7bddc942c9a8f369f9"
      "35a07785d2db805215ed786e4285df1658eed3ce84f469b81b50d358407b4ad361" },
    { "u", KEY_TYPE_SECRET,
      "304559a9ead56d2309d203811a641bb1a09626bc8eb36fffa23c968ec5bd891e"
      "ebbafc73ae666e01ba7c8990bae06cc2bbe10b75e69fcacb353a6473079d8e9b" },
    { NULL },
  };

gcry_error_t
key_init (gcry_ac_key_type_t type, gcry_ac_key_t *key)
{
  gcry_error_t err = GPG_ERR_NO_ERROR;
  gcry_ac_data_t key_data = NULL;
  gcry_ac_key_t key_new = NULL;
  gcry_mpi_t mpi = NULL;
  unsigned int i = 0;

  err = gcry_ac_data_new (&key_data);
  for (i = 0; key_specs[i].name && (! err); i++)
    {
      if (((type == GCRY_AC_KEY_PUBLIC) && (key_specs[i].flags & KEY_TYPE_PUBLIC))
	  || ((type == GCRY_AC_KEY_SECRET) && (key_specs[i].flags & KEY_TYPE_SECRET)))
	{
	  err = gcry_mpi_scan (&mpi, GCRYMPI_FMT_HEX, key_specs[i].mpi_string, 0, NULL);
	  if (! err)
	    {
	      gcry_ac_data_set (key_data, GCRY_AC_FLAG_COPY | GCRY_AC_FLAG_DEALLOC,
				key_specs[i].name, mpi);
	      gcry_mpi_release (mpi);
	    }
	}
    }
  if (! err)
    err = gcry_ac_key_init (&key_new, NULL, type, key_data);

  if (key_data)
    gcry_ac_data_destroy (key_data);
  
  if (! err)
    *key = key_new;

  return err;
}

static void
check_run (void)
{
  gcry_ac_handle_t handle = NULL;
  gcry_error_t err = GPG_ERR_NO_ERROR;
  gcry_ac_key_t key_public = NULL, key_secret = NULL;

  err = key_init (GCRY_AC_KEY_PUBLIC, &key_public);
  if (! err)
    err = key_init (GCRY_AC_KEY_SECRET, &key_secret);
  
  if (! err)
    err = gcry_ac_open (&handle, GCRY_AC_RSA, 0);
  if (! err)
    {
      es_checks (handle, key_public, key_secret);
      ssa_checks (handle, key_public, key_secret);
    }
  
  assert (! err);
}

int
main (int argc, char **argv)
{
  unsigned int debug = 0;

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

  check_run ();
  
  return 0;
}
