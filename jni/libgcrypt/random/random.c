/* random.c - Random number switch
 * Copyright (C) 2008  Free Software Foundation, Inc.
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
  This module switches between different implementations of random
  number generators and provides a few help functions.
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include "g10lib.h"
#include "random.h"
#include "rand-internal.h"
#include "ath.h"


/* If not NULL a progress function called from certain places and the
   opaque value passed along.  Registred by
   _gcry_register_random_progress (). */
static void (*progress_cb) (void *,const char*,int,int, int );
static void *progress_cb_data;




/* ---  Functions  --- */


/* Used to register a progress callback.  This needs to be called
   before any threads are created. */
void
_gcry_register_random_progress (void (*cb)(void *,const char*,int,int,int),
                                void *cb_data )
{
  progress_cb = cb;
  progress_cb_data = cb_data;
}


/* This progress function is currently used by the random modules to
   give hints on how much more entropy is required. */
void
_gcry_random_progress (const char *what, int printchar, int current, int total)
{
  if (progress_cb)
    progress_cb (progress_cb_data, what, printchar, current, total);
}



/* Initialize this random subsystem.  If FULL is false, this function
   merely calls the basic initialization of the module and does not do
   anything more.  Doing this is not really required but when running
   in a threaded environment we might get a race condition
   otherwise. */
void
_gcry_random_initialize (int full)
{
  if (fips_mode ())
    _gcry_rngfips_initialize (full);
  else
    _gcry_rngcsprng_initialize (full);
}


void
_gcry_random_dump_stats (void)
{
  if (fips_mode ())
    _gcry_rngfips_dump_stats ();
  else
    _gcry_rngcsprng_dump_stats ();
}


/* This function should be called during initialization and beore
   intialization of this module to place the random pools into secure
   memory.  */
void
_gcry_secure_random_alloc (void)
{
  if (fips_mode ())
    ;  /* Not used; the fips rng is allows in secure mode.  */
  else
    _gcry_rngcsprng_secure_alloc ();
}


/* This may be called before full initialization to degrade the
   quality of the RNG for the sake of a faster running test suite.  */
void
_gcry_enable_quick_random_gen (void)
{
  if (fips_mode ())
    ;  /* Not used.  */
  else
    _gcry_rngcsprng_enable_quick_gen ();
}


void
_gcry_set_random_daemon_socket (const char *socketname)
{
  if (fips_mode ())
    ;  /* Not used.  */
  else
    _gcry_rngcsprng_set_daemon_socket (socketname);
}

/* With ONOFF set to 1, enable the use of the daemon.  With ONOFF set
   to 0, disable the use of the daemon.  With ONOF set to -1, return
   whether the daemon has been enabled. */
int
_gcry_use_random_daemon (int onoff)
{
  if (fips_mode ())
    return 0; /* Never enabled in fips mode.  */
  else
    return _gcry_rngcsprng_use_daemon (onoff);
}


/* This function returns true if no real RNG is available or the
   quality of the RNG has been degraded for test purposes.  */
int
_gcry_random_is_faked (void)
{
  if (fips_mode ())
    return _gcry_rngfips_is_faked ();
  else
    return _gcry_rngcsprng_is_faked ();
}


/* Add BUFLEN bytes from BUF to the internal random pool.  QUALITY
   should be in the range of 0..100 to indicate the goodness of the
   entropy added, or -1 for goodness not known.  */
gcry_error_t
gcry_random_add_bytes (const void *buf, size_t buflen, int quality)
{
  if (fips_mode ())
    return 0; /* No need for this in fips mode.  */
  else
    return _gcry_rngcsprng_add_bytes (buf, buflen, quality);
}   

  
/* Helper function.  */ 
static void
do_randomize (void *buffer, size_t length, enum gcry_random_level level)
{
  if (fips_mode ())
    _gcry_rngfips_randomize (buffer, length, level);
  else
    _gcry_rngcsprng_randomize (buffer, length, level);
}

/* The public function to return random data of the quality LEVEL.
   Returns a pointer to a newly allocated and randomized buffer of
   LEVEL and NBYTES length.  Caller must free the buffer.  */
void *
gcry_random_bytes (size_t nbytes, enum gcry_random_level level)
{
  void *buffer;

  buffer = gcry_xmalloc (nbytes);
  do_randomize (buffer, nbytes, level);
  return buffer;
}


/* The public function to return random data of the quality LEVEL;
   this version of the function returns the random in a buffer allocated
   in secure memory.  Caller must free the buffer. */
void *
gcry_random_bytes_secure (size_t nbytes, enum gcry_random_level level)
{
  void *buffer;

  /* Historical note (1.3.0--1.4.1): The buffer was only allocated
     in secure memory if the pool in random-csprng.c was also set to
     use secure memory.  */
  buffer = gcry_xmalloc_secure (nbytes);
  do_randomize (buffer, nbytes, level);
  return buffer;
}


/* Public function to fill the buffer with LENGTH bytes of
   cryptographically strong random bytes.  Level GCRY_WEAK_RANDOM is
   not very strong, GCRY_STRONG_RANDOM is strong enough for most
   usage, GCRY_VERY_STRONG_RANDOM is good for key generation stuff but
   may be very slow.  */
void
gcry_randomize (void *buffer, size_t length, enum gcry_random_level level)
{
  do_randomize (buffer, length, level);
}


/* This function may be used to specify the file to be used as a seed
   file for the PRNG.  This fucntion should be called prior to the
   initialization of the random module.  NAME may not be NULL.  */
void
_gcry_set_random_seed_file (const char *name)
{
  if (fips_mode ())
    ; /* No need for this in fips mode.  */
  else
    _gcry_rngcsprng_set_seed_file (name);
}


/* If a seed file has been setup, this function may be used to write
   back the random numbers entropy pool.  */
void
_gcry_update_random_seed_file (void)
{
  if (fips_mode ())
    ; /* No need for this in fips mode.  */
  else
    _gcry_rngcsprng_update_seed_file ();
}



/* The fast random pool function as called at some places in
   libgcrypt.  This is merely a wrapper to make sure that this module
   is initalized and to lock the pool.  Note, that this function is a
   NOP unless a random function has been used or _gcry_initialize (1)
   has been used.  We use this hack so that the internal use of this
   function in cipher_open and md_open won't start filling up the
   random pool, even if no random will be required by the process. */
void
_gcry_fast_random_poll (void)
{
  if (fips_mode ())
    ; /* No need for this in fips mode.  */
  else
    _gcry_rngcsprng_fast_poll ();
}



/* Create an unpredicable nonce of LENGTH bytes in BUFFER. */
void
gcry_create_nonce (void *buffer, size_t length)
{
  if (fips_mode ())
    _gcry_rngfips_create_nonce (buffer, length);
  else
    _gcry_rngcsprng_create_nonce (buffer, length);
}


/* Run the self-tests for the RNG.  This is currently only implemented
   for the FIPS generator.  */
gpg_error_t
_gcry_random_selftest (selftest_report_func_t report)
{
  if (fips_mode ())
    return _gcry_rngfips_selftest (report);
  else
    return 0; /* No selftests yet.  */
}


/* Create a new test context for an external RNG test driver.  On
   success the test context is stored at R_CONTEXT; on failure NULL is
   stored at R_CONTEXT and an error code is returned.  */
gcry_err_code_t
_gcry_random_init_external_test (void **r_context, 
                                 unsigned int flags,
                                 const void *key, size_t keylen,
                                 const void *seed, size_t seedlen,
                                 const void *dt, size_t dtlen)
{
  (void)flags;
  if (fips_mode ())
    return _gcry_rngfips_init_external_test (r_context, flags, key, keylen,
                                             seed, seedlen,
                                             dt, dtlen);
  else
    return GPG_ERR_NOT_SUPPORTED;
}

/* Get BUFLEN bytes from the RNG using the test CONTEXT and store them
   at BUFFER.  Return 0 on success or an error code.  */
gcry_err_code_t
_gcry_random_run_external_test (void *context, char *buffer, size_t buflen)
{
  if (fips_mode ())
    return _gcry_rngfips_run_external_test (context, buffer, buflen);
  else
    return GPG_ERR_NOT_SUPPORTED;
}

/* Release the test CONTEXT.  */
void
_gcry_random_deinit_external_test (void *context)
{
  if (fips_mode ())
    _gcry_rngfips_deinit_external_test (context);
}
