/* random-fips.c - FIPS style random number generator
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
   The core of this deterministic random number generator is
   implemented according to the document "NIST-Recommended Random
   Number Generator Based on ANSI X9.31 Appendix A.2.4 Using the 3-Key
   Triple DES and AES Algorithms" (2005-01-31).  This implementation
   uses the AES variant.

   There are 3 random context which map to the different levels of
   random quality:

   Generator                Seed and Key        Kernel entropy (init/reseed)
   ------------------------------------------------------------
   GCRY_VERY_STRONG_RANDOM  /dev/random         256/128 bits
   GCRY_STRONG_RANDOM       /dev/random         256/128 bits
   gcry_create_nonce        GCRY_STRONG_RANDOM  n/a

   All random generators return their data in 128 bit blocks.  If the
   caller requested less bits, the extra bits are not used.  The key
   for each generator is only set once at the first time a generator
   is used.  The seed value is set with the key and again after 1000
   (SEED_TTL) output blocks; the re-seeding is disabled in test mode.

   The GCRY_VERY_STRONG_RANDOM and GCRY_STRONG_RANDOM generators are
   keyed and seeded from the /dev/random device.  Thus these
   generators may block until the kernel has collected enough entropy.

   The gcry_create_nonce generator is keyed and seeded from the
   GCRY_STRONG_RANDOM generator.  It may also block if the
   GCRY_STRONG_RANDOM generator has not yet been used before and thus
   gets initialized on the first use by gcry_create_nonce.  This
   special treatment is justified by the weaker requirements for a
   nonce generator and to save precious kernel entropy for use by the
   real random generators.

 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <unistd.h>
#ifdef HAVE_GETTIMEOFDAY
#include <sys/time.h>
#endif

#include "g10lib.h"
#include "random.h"
#include "rand-internal.h"
#include "ath.h"

/* This is the lock we use to serialize access to this RNG.  The extra
   integer variable is only used to check the locking state; that is,
   it is not meant to be thread-safe but merely as a failsafe feature
   to assert proper locking.  */
static ath_mutex_t fips_rng_lock = ATH_MUTEX_INITIALIZER;
static int fips_rng_is_locked;


/* The required size for the temporary buffer of the x931_aes_driver
   function and the buffer itself which will be allocated in secure
   memory.  This needs to be global variable for proper initialization
   and to allow shutting down the RNG without leaking memory.  May
   only be used while holding the FIPS_RNG_LOCK.

   This variable is also used to avoid duplicate initialization.  */
#define TEMPVALUE_FOR_X931_AES_DRIVER_SIZE 48
static unsigned char *tempvalue_for_x931_aes_driver;


/* After having retrieved this number of blocks from the RNG, we want
   to do a reseeding.  */
#define SEED_TTL 1000


/* The length of the key we use:  16 bytes (128 bit) for AES128.  */
#define X931_AES_KEYLEN  16
/* A global buffer used to communicate between the x931_generate_key
   and x931_generate_seed functions and the entropy_collect_cb
   function.  It may only be used by these functions. */
static unsigned char *entropy_collect_buffer;  /* Buffer.  */
static size_t entropy_collect_buffer_len;      /* Used length.  */
static size_t entropy_collect_buffer_size;     /* Allocated length.  */


/* This random context type is used to track properties of one random
   generator. Thee context are usually allocated in secure memory so
   that the seed value is well protected.  There are a couble of guard
   fields to help detecting applications accidently overwriting parts
   of the memory. */
struct rng_context
{
  unsigned char guard_0[1];

  /* The handle of the cipher used by the RNG.  If this one is not
     NULL a cipher handle along with a random key has been
     established.  */
  gcry_cipher_hd_t cipher_hd;

  /* If this flag is true, the SEED_V buffer below carries a valid
     seed.  */
  int is_seeded:1;

  /* The very first block generated is used to compare the result
     against the last result.  This flag indicates that such a block
     is available.  */
  int compare_value_valid:1;

  /* A counter used to trigger re-seeding.  */
  unsigned int use_counter;

  unsigned char guard_1[1];

  /* The buffer containing the seed value V.  */
  unsigned char seed_V[16];

  unsigned char guard_2[1];

  /* The last result from the x931_aes fucntion.  Only valid if
     compare_value_valid is set.  */
  unsigned char compare_value[16];

  unsigned char guard_3[1];

  /* The external test may want to suppress the duplicate bock check.
     This is done if the this flag is set.  */
  unsigned char test_no_dup_check;
  /* To implement a KAT we need to provide a know DT value.  To
     accomplish this the x931_get_dt function checks whether this
     field is not NULL and then uses the 16 bytes at this address for
     the DT value.  However the last 4 bytes are replaced by the
     value of field TEST_DT_COUNTER which will be incremented after
     each invocation of x931_get_dt. We use a pointer and not a buffer
     because there is no need to put this value into secure memory.  */
  const unsigned char *test_dt_ptr;
  u32 test_dt_counter;

  /* We need to keep track of the process which did the initialization
     so that we can detect a fork.  The volatile modifier is required
     so that the compiler does not optimize it away in case the getpid
     function is badly attributed.  */ 
  pid_t key_init_pid;
  pid_t seed_init_pid;
};
typedef struct rng_context *rng_context_t;


/* The random context used for the nonce generator.  May only be used
   while holding the FIPS_RNG_LOCK.  */
static rng_context_t nonce_context;
/* The random context used for the standard random generator.  May
   only be used while holding the FIPS_RNG_LOCK.  */
static rng_context_t std_rng_context;
/* The random context used for the very strong random generator.  May
   only be used while holding the FIPS_RNG_LOCK.  */
static rng_context_t strong_rng_context;


/* --- Local prototypes ---  */
static void x931_reseed (rng_context_t rng_ctx);
static void get_random (void *buffer, size_t length, rng_context_t rng_ctx);




/* --- Functions  --- */

/* Basic initialization is required to initialize mutexes and
   do a few checks on the implementation.  */
static void
basic_initialization (void)
{
  static int initialized;
  int my_errno;

  if (!initialized)
    return;
  initialized = 1;

  my_errno = ath_mutex_init (&fips_rng_lock);
  if (my_errno)
    log_fatal ("failed to create the RNG lock: %s\n", strerror (my_errno));
  fips_rng_is_locked = 0;
      
  /* Make sure that we are still using the values we have
     traditionally used for the random levels.  */
  gcry_assert (GCRY_WEAK_RANDOM == 0 
               && GCRY_STRONG_RANDOM == 1
               && GCRY_VERY_STRONG_RANDOM == 2);

}


/* Acquire the fips_rng_lock.  */
static void
lock_rng (void)
{
  int my_errno;

  my_errno = ath_mutex_lock (&fips_rng_lock);
  if (my_errno)
    log_fatal ("failed to acquire the RNG lock: %s\n", strerror (my_errno));
  fips_rng_is_locked = 1;
}


/* Release the fips_rng_lock.  */
static void
unlock_rng (void)
{
  int my_errno;

  fips_rng_is_locked = 0;
  my_errno = ath_mutex_unlock (&fips_rng_lock);
  if (my_errno)
    log_fatal ("failed to release the RNG lock: %s\n", strerror (my_errno));
}

static void
setup_guards (rng_context_t rng_ctx)
{
  /* Set the guards to some arbitrary values.  */
  rng_ctx->guard_0[0] = 17;
  rng_ctx->guard_1[0] = 42;
  rng_ctx->guard_2[0] = 137;
  rng_ctx->guard_3[0] = 252;
}

static void
check_guards (rng_context_t rng_ctx)
{
  if ( rng_ctx->guard_0[0] != 17
       || rng_ctx->guard_1[0] != 42
       || rng_ctx->guard_2[0] != 137
       || rng_ctx->guard_3[0] != 252 )
    log_fatal ("memory corruption detected in RNG context %p\n", rng_ctx);
}


/* Get the DT vector for use with the core PRNG function.  Buffer
   needs to be provided by the caller with a size of at least LENGTH
   bytes. RNG_CTX needs to be passed to allow for a KAT.  The 16 byte
   timestamp we construct is made up the real time and three counters:

   Buffer:       00112233445566778899AABBCCDDEEFF
                 !--+---!!-+-!!+!!--+---!!--+---!     
   seconds ---------/      |   |    |       |
   microseconds -----------/   |    |       |
   counter2 -------------------/    |       |
   counter1 ------------------------/       |
   counter0 --------------------------------/

   Counter 2 is just 12 bits wide and used to track fractions of
   milliseconds whereas counters 1 and 0 are combined to a free
   running 64 bit counter.  */
static void 
x931_get_dt (unsigned char *buffer, size_t length, rng_context_t rng_ctx)
{
  gcry_assert (length == 16); /* This length is required for use with AES.  */
  gcry_assert (fips_rng_is_locked);

  /* If the random context indicates that a test DT should be used,
     take the DT value from the context.  For safety reasons we do
     this only if the context is not one of the regular contexts.  */
  if (rng_ctx->test_dt_ptr 
      && rng_ctx != nonce_context
      && rng_ctx != std_rng_context
      && rng_ctx != strong_rng_context)
    {
      memcpy (buffer, rng_ctx->test_dt_ptr, 16);
      buffer[12] = (rng_ctx->test_dt_counter >> 24);
      buffer[13] = (rng_ctx->test_dt_counter >> 16);
      buffer[14] = (rng_ctx->test_dt_counter >> 8);
      buffer[15] = rng_ctx->test_dt_counter;
      rng_ctx->test_dt_counter++;
      return;
    }


#if HAVE_GETTIMEOFDAY
  {
    static u32 last_sec, last_usec;
    static u32 counter1, counter0;
    static u16 counter2;
    
    unsigned int usec;
    struct timeval tv;

    if (!last_sec)
      {
        /* This is the very first time we are called: Set the counters
           to an not so easy predictable value to avoid always
           starting at 0.  Not really needed but it doesn't harm.  */
        counter1 = (u32)getpid ();
#ifndef HAVE_W32_SYSTEM
        counter0 = (u32)getppid ();
#endif
      }


    if (gettimeofday (&tv, NULL))
      log_fatal ("gettimeofday() failed: %s\n", strerror (errno));

    /* The microseconds part is always less than 1 millon (0x0f4240).
       Thus we don't care about the MSB and in addition shift it to
       the left by 4 bits.  */
    usec = tv.tv_usec;
    usec <<= 4;
    /* If we got the same time as by the last invocation, bump up
       counter2 and save the time for the next invocation.  */
    if (tv.tv_sec == last_sec && usec == last_usec)
      {
        counter2++;
        counter2 &= 0x0fff;
      }
    else
      {
        counter2 = 0;
        last_sec = tv.tv_sec;
        last_usec = usec;
      }
    /* Fill the buffer with the timestamp.  */
    buffer[0] = ((tv.tv_sec >> 24) & 0xff);
    buffer[1] = ((tv.tv_sec >> 16) & 0xff);
    buffer[2] = ((tv.tv_sec >> 8) & 0xff);
    buffer[3] = (tv.tv_sec & 0xff);
    buffer[4] = ((usec >> 16) & 0xff);
    buffer[5] = ((usec >> 8) & 0xff);
    buffer[6] = ((usec & 0xf0) | ((counter2 >> 8) & 0x0f));
    buffer[7] = (counter2 & 0xff);
    /* Add the free running counter.  */
    buffer[8]  = ((counter1 >> 24) & 0xff);
    buffer[9]  = ((counter1 >> 16) & 0xff);
    buffer[10] = ((counter1 >> 8) & 0xff); 
    buffer[11] = ((counter1) & 0xff);
    buffer[12] = ((counter0 >> 24) & 0xff);
    buffer[13] = ((counter0 >> 16) & 0xff);
    buffer[14] = ((counter0 >> 8) & 0xff); 
    buffer[15] = ((counter0) & 0xff);
    /* Bump up that counter.  */
    if (!++counter0)
      ++counter1;
  }
#else
  log_fatal ("gettimeofday() not available on this system\n");
#endif

  /* log_printhex ("x931_get_dt: ", buffer, 16); */
}


/* XOR the buffers A and B which are each of LENGTH bytes and store
   the result at R.  R needs to be provided by the caller with a size
   of at least LENGTH bytes.  */
static void
xor_buffer (unsigned char *r, 
            const unsigned char *a, const unsigned char *b, size_t length)
{
  for ( ; length; length--, a++, b++, r++)
    *r = (*a ^ *b);
}


/* Encrypt LENGTH bytes of INPUT to OUTPUT using KEY.  LENGTH
   needs to be 16. */
static void
encrypt_aes (gcry_cipher_hd_t key, 
             unsigned char *output, const unsigned char *input, size_t length)
{
  gpg_error_t err;

  gcry_assert (length == 16);

  err = gcry_cipher_encrypt (key, output, length, input, length);
  if (err)
    log_fatal ("AES encryption in RNG failed: %s\n", gcry_strerror (err));
}


/* The core ANSI X9.31, Appendix A.2.4 function using AES.  The caller
   needs to pass a 16 byte buffer for the result, the 16 byte
   datetime_DT value and the 16 byte seed value V.  The caller also
   needs to pass an appropriate KEY and make sure to pass a valid
   seed_V.  The caller also needs to provide two 16 bytes buffer for
   intermediate results, they may be reused by the caller later.

   On return the result is stored at RESULT_R and the SEED_V is
   updated.  May only be used while holding the lock.  */
static void
x931_aes (unsigned char result_R[16], 
          unsigned char datetime_DT[16], unsigned char seed_V[16],
          gcry_cipher_hd_t key,
          unsigned char intermediate_I[16], unsigned char temp_xor[16])
{
  /* Let ede*X(Y) represent the AES encryption of Y under the key *X.

     Let V be a 128-bit seed value which is also kept secret, and XOR
     be the exclusive-or operator. Let DT be a date/time vector which
     is updated on each iteration. I is a intermediate value. 

     I = ede*K(DT)  */
  encrypt_aes (key, intermediate_I, datetime_DT, 16);

  /* R = ede*K(I XOR V) */
  xor_buffer (temp_xor, intermediate_I, seed_V, 16);
  encrypt_aes (key, result_R, temp_xor, 16);

  /* V = ede*K(R XOR I).  */
  xor_buffer (temp_xor, result_R, intermediate_I, 16);
  encrypt_aes (key, seed_V, temp_xor, 16);

  /* Zero out temporary values.  */
  wipememory (intermediate_I, 16);
  wipememory (temp_xor, 16);
}


/* The high level driver to x931_aes.  This one does the required
   tests and calls the core function until the entire buffer has been
   filled.  OUTPUT is a caller provided buffer of LENGTH bytes to
   receive the random, RNG_CTX is the context of the RNG.  The context
   must be properly initialized.  Returns 0 on success. */
static int
x931_aes_driver (unsigned char *output, size_t length, rng_context_t rng_ctx)
{
  unsigned char datetime_DT[16];
  unsigned char *intermediate_I, *temp_buffer, *result_buffer;
  size_t nbytes;

  gcry_assert (fips_rng_is_locked);
  gcry_assert (rng_ctx->cipher_hd);
  gcry_assert (rng_ctx->is_seeded);

  gcry_assert (tempvalue_for_x931_aes_driver);
  gcry_assert (TEMPVALUE_FOR_X931_AES_DRIVER_SIZE == 48);
  intermediate_I = tempvalue_for_x931_aes_driver;
  temp_buffer    = tempvalue_for_x931_aes_driver + 16;
  result_buffer  = tempvalue_for_x931_aes_driver + 32;

  while (length)
    {
      /* Unless we are running with a test context, we require a new
         seed after some time.  */
      if (!rng_ctx->test_dt_ptr && rng_ctx->use_counter > SEED_TTL)
        {
          x931_reseed (rng_ctx);
          rng_ctx->use_counter = 0;
        }

      /* Due to the design of the RNG, we always receive 16 bytes (128
         bit) of random even if we require less.  The extra bytes
         returned are not used.  Intheory we could save them for the
         next invocation, but that would make the control flow harder
         to read.  */
      nbytes = length < 16? length : 16;

      x931_get_dt (datetime_DT, 16, rng_ctx);
      x931_aes (result_buffer,
                datetime_DT, rng_ctx->seed_V, rng_ctx->cipher_hd,
                intermediate_I, temp_buffer);
      rng_ctx->use_counter++;

      if (rng_ctx->test_no_dup_check
          && rng_ctx->test_dt_ptr
          && rng_ctx != nonce_context
          && rng_ctx != std_rng_context
          && rng_ctx != strong_rng_context)
        {
          /* This is a test context which does not want the duplicate
             block check. */
        }
      else
        {
          /* Do a basic check on the output to avoid a stuck generator.  */
          if (!rng_ctx->compare_value_valid)
            {
              /* First time used, only save the result.  */
              memcpy (rng_ctx->compare_value, result_buffer, 16);
              rng_ctx->compare_value_valid = 1;
              continue;
            }
          if (!memcmp (rng_ctx->compare_value, result_buffer, 16))
            {
              /* Ooops, we received the same 128 bit block - that should
                 in theory never happen.  The FIPS requirement says that
                 we need to put ourself into the error state in such
                 case.  */
              fips_signal_error ("duplicate 128 bit block returned by RNG");
              return -1;
            }
          memcpy (rng_ctx->compare_value, result_buffer, 16);
        }
      
      /* Append to outbut.  */
      memcpy (output, result_buffer, nbytes);
      wipememory (result_buffer, 16);
      output += nbytes;
      length -= nbytes;
    }

  return 0;
}


/* Callback for x931_generate_key. Note that this callback uses the
   global ENTROPY_COLLECT_BUFFER which has been setup by get_entropy.
   ORIGIN is not used but required due to the design of entropy
   gathering module. */
static void
entropy_collect_cb (const void *buffer, size_t length,
                    enum random_origins origin)
{
  const unsigned char *p = buffer;

  (void)origin;

  gcry_assert (fips_rng_is_locked);
  gcry_assert (entropy_collect_buffer);

  /* Note that we need to protect against gatherers returning more
     than the requested bytes (e.g. rndw32).  */
  while (length-- && entropy_collect_buffer_len < entropy_collect_buffer_size)
    {
      entropy_collect_buffer[entropy_collect_buffer_len++] ^= *p++;
    }
}


/* Get NBYTES of entropy from the kernel device.  The callers needs to
   free the returned buffer.  The function either succeeds or
   terminates the process in case of a fatal error. */
static void *
get_entropy (size_t nbytes)
{
  void *result;
  int rc;

  gcry_assert (!entropy_collect_buffer);
  entropy_collect_buffer = gcry_xmalloc_secure (nbytes);
  entropy_collect_buffer_size = nbytes;
  entropy_collect_buffer_len = 0;

#if USE_RNDLINUX
  rc = _gcry_rndlinux_gather_random (entropy_collect_cb, 0,
                                     X931_AES_KEYLEN,
                                     GCRY_VERY_STRONG_RANDOM);
#elif USE_RNDW32
  do 
    {
      rc = _gcry_rndw32_gather_random (entropy_collect_cb, 0,
                                       X931_AES_KEYLEN,
                                       GCRY_VERY_STRONG_RANDOM);
    }
  while (rc >= 0 && entropy_collect_buffer_len < entropy_collect_buffer_size);
#else
  rc = -1;
#endif

  if (rc < 0 || entropy_collect_buffer_len != entropy_collect_buffer_size)
    {
      gcry_free (entropy_collect_buffer);
      entropy_collect_buffer = NULL;
      log_fatal ("error getting entropy data\n");
    }
  result = entropy_collect_buffer;
  entropy_collect_buffer = NULL;
  return result;
}


/* Generate a key for use with x931_aes.  The function returns a
   handle to the cipher context readily prepared for ECB encryption.
   If FOR_NONCE is true, the key is retrieved by readong random from
   the standard generator.  On error NULL is returned.  */
static gcry_cipher_hd_t
x931_generate_key (int for_nonce)
{
  gcry_cipher_hd_t hd;
  gpg_error_t err;
  void *buffer;

  gcry_assert (fips_rng_is_locked);

  /* Allocate a cipher context.  */
  err = gcry_cipher_open (&hd, GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_ECB,
                          GCRY_CIPHER_SECURE);
  if (err)
    {
      log_error ("error creating cipher context for RNG: %s\n",
                 gcry_strerror (err));
      return NULL;
    }

  /* Get a key from the standard RNG or from the entropy source.  */
  if (for_nonce)
    {
      buffer = gcry_xmalloc (X931_AES_KEYLEN);
      get_random (buffer, X931_AES_KEYLEN, std_rng_context);
    }
  else
    {
      buffer = get_entropy (X931_AES_KEYLEN);
    }

  /* Set the key and delete the buffer because the key is now part of
     the cipher context.  */
  err = gcry_cipher_setkey (hd, buffer, X931_AES_KEYLEN);
  wipememory (buffer, X931_AES_KEYLEN);
  gcry_free (buffer);
  if (err)
    {
      log_error ("error creating key for RNG: %s\n", gcry_strerror (err));
      gcry_cipher_close (hd);
      return NULL;
    }

  return hd;
}


/* Generate a key for use with x931_aes.  The function copies a seed
   of LENGTH bytes into SEED_BUFFER. LENGTH needs to by given as 16.  */
static void
x931_generate_seed (unsigned char *seed_buffer, size_t length)
{
  void *buffer;

  gcry_assert (fips_rng_is_locked);
  gcry_assert (length == 16);

  buffer = get_entropy (X931_AES_KEYLEN);

  memcpy (seed_buffer, buffer, X931_AES_KEYLEN);
  wipememory (buffer, X931_AES_KEYLEN);
  gcry_free (buffer);
}



/* Reseed a generator.  This is also used for the initial seeding. */
static void
x931_reseed (rng_context_t rng_ctx)
{
  gcry_assert (fips_rng_is_locked);

  if (rng_ctx == nonce_context)
    {
      /* The nonce context is special.  It will be seeded using the
         standard random generator.  */
      get_random (rng_ctx->seed_V, 16, std_rng_context);
      rng_ctx->is_seeded = 1;
      rng_ctx->seed_init_pid = getpid ();
    }
  else
    {
      /* The other two generators are seeded from /dev/random.  */
      x931_generate_seed (rng_ctx->seed_V, 16);
      rng_ctx->is_seeded = 1;
      rng_ctx->seed_init_pid = getpid ();
    }
}


/* Core random function.  This is used for both nonce and random
   generator.  The actual RNG to be used depends on the random context
   RNG_CTX passed.  Note that this function is called with the RNG not
   yet locked.  */
static void
get_random (void *buffer, size_t length, rng_context_t rng_ctx)
{
  gcry_assert (buffer);
  gcry_assert (rng_ctx);

  check_guards (rng_ctx);

  /* Initialize the cipher handle and thus setup the key if needed.  */
  if (!rng_ctx->cipher_hd)
    {
      if (rng_ctx == nonce_context)
        rng_ctx->cipher_hd = x931_generate_key (1);
      else
        rng_ctx->cipher_hd = x931_generate_key (0);
      if (!rng_ctx->cipher_hd)
        goto bailout;
      rng_ctx->key_init_pid = getpid ();
    }

  /* Initialize the seed value if needed.  */
  if (!rng_ctx->is_seeded)
    x931_reseed (rng_ctx);

  if (rng_ctx->key_init_pid != getpid ()
      || rng_ctx->seed_init_pid != getpid ())
    {
      /* We are in a child of us.  Because we have no way yet to do
         proper re-initialization (including self-checks etc), the
         only chance we have is to bail out.  Obviusly a fork/exec
         won't harm because the exec overwrites the old image. */
      fips_signal_error ("fork without proper re-initialization "
                         "detected in RNG");
      goto bailout;
    }

  if (x931_aes_driver (buffer, length, rng_ctx))
    goto bailout;

  check_guards (rng_ctx);
  return;

 bailout:
  log_fatal ("severe error getting random\n");
  /*NOTREACHED*/
}



/* --- Public Functions --- */

/* Initialize this random subsystem.  If FULL is false, this function
   merely calls the basic initialization of the module and does not do
   anything more.  Doing this is not really required but when running
   in a threaded environment we might get a race condition
   otherwise. */
void
_gcry_rngfips_initialize (int full)
{
  basic_initialization ();
  if (!full)
    return;

  /* Allocate temporary buffers.  If that buffer already exists we
     know that we are already initialized.  */
  lock_rng ();
  if (!tempvalue_for_x931_aes_driver)
    {
      tempvalue_for_x931_aes_driver
        = gcry_xmalloc_secure (TEMPVALUE_FOR_X931_AES_DRIVER_SIZE);

      /* Allocate the random contexts.  Note that we do not need to use
         secure memory for the nonce context.  */
      nonce_context = gcry_xcalloc (1, sizeof *nonce_context);
      setup_guards (nonce_context);

      std_rng_context = gcry_xcalloc_secure (1, sizeof *std_rng_context);
      setup_guards (std_rng_context);
      
      strong_rng_context = gcry_xcalloc_secure (1, sizeof *strong_rng_context);
      setup_guards (strong_rng_context);
    }
  else
    {
      /* Already initialized. Do some sanity checks.  */
      gcry_assert (!nonce_context->test_dt_ptr);
      gcry_assert (!std_rng_context->test_dt_ptr);
      gcry_assert (!strong_rng_context->test_dt_ptr);
      check_guards (nonce_context);
      check_guards (std_rng_context);
      check_guards (strong_rng_context);
    }
  unlock_rng ();
}


/* Print some statistics about the RNG.  */
void
_gcry_rngfips_dump_stats (void)
{
  /* Not yet implemented.  */
}


/* This function returns true if no real RNG is available or the
   quality of the RNG has been degraded for test purposes.  */
int
_gcry_rngfips_is_faked (void)
{
  return 0;  /* Faked random is not allowed.  */
}


/* Add BUFLEN bytes from BUF to the internal random pool.  QUALITY
   should be in the range of 0..100 to indicate the goodness of the
   entropy added, or -1 for goodness not known. */
gcry_error_t
_gcry_rngfips_add_bytes (const void *buf, size_t buflen, int quality)
{
  (void)buf;
  (void)buflen;
  (void)quality;
  return 0;  /* Not implemented. */
}   

    
/* Public function to fill the buffer with LENGTH bytes of
   cryptographically strong random bytes.  Level GCRY_WEAK_RANDOM is
   here mapped to GCRY_STRONG_RANDOM, GCRY_STRONG_RANDOM is strong
   enough for most usage, GCRY_VERY_STRONG_RANDOM is good for key
   generation stuff but may be very slow.  */
void
_gcry_rngfips_randomize (void *buffer, size_t length,
                         enum gcry_random_level level)
{
  _gcry_rngfips_initialize (1);  /* Auto-initialize if needed.  */
  
  lock_rng ();
  if (level == GCRY_VERY_STRONG_RANDOM)
    get_random (buffer, length, strong_rng_context);
  else
    get_random (buffer, length, std_rng_context);
  unlock_rng ();
}


/* Create an unpredicable nonce of LENGTH bytes in BUFFER. */
void
_gcry_rngfips_create_nonce (void *buffer, size_t length)
{
  _gcry_rngfips_initialize (1);  /* Auto-initialize if needed.  */

  lock_rng ();
  get_random (buffer, length, nonce_context);
  unlock_rng ();
}


/* Run a Know-Answer-Test using a dedicated test context.  Note that
   we can't use the samples from the NISR RNGVS document because they
   don't take the requirement to throw away the first block and use
   that for duplicate check in account.  Thus we made up our own test
   vectors. */
static gcry_err_code_t
selftest_kat (selftest_report_func_t report)
{
  static struct 
  {
    const unsigned char key[16];
    const unsigned char dt[16];
    const unsigned char v[16];
    const unsigned char r[3][16];
  } tv[] =
    {
      { { 0xb9, 0xca, 0x7f, 0xd6, 0xa0, 0xf5, 0xd3, 0x42,
          0x19, 0x6d, 0x84, 0x91, 0x76, 0x1c, 0x3b, 0xbe },
        { 0x48, 0xb2, 0x82, 0x98, 0x68, 0xc2, 0x80, 0x00,
          0x00, 0x00, 0x28, 0x18, 0x00, 0x00, 0x25, 0x00 },
        { 0x52, 0x17, 0x8d, 0x29, 0xa2, 0xd5, 0x84, 0x12,
          0x9d, 0x89, 0x9a, 0x45, 0x82, 0x02, 0xf7, 0x77 },
        { { 0x42, 0x9c, 0x08, 0x3d, 0x82, 0xf4, 0x8a, 0x40,
            0x66, 0xb5, 0x49, 0x27, 0xab, 0x42, 0xc7, 0xc3 },
          { 0x0e, 0xb7, 0x61, 0x3c, 0xfe, 0xb0, 0xbe, 0x73,
            0xf7, 0x6e, 0x6d, 0x6f, 0x1d, 0xa3, 0x14, 0xfa },
          { 0xbb, 0x4b, 0xc1, 0x0e, 0xc5, 0xfb, 0xcd, 0x46,
            0xbe, 0x28, 0x61, 0xe7, 0x03, 0x2b, 0x37, 0x7d } } },
      { { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
        { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
        { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
        { { 0xf7, 0x95, 0xbd, 0x4a, 0x52, 0xe2, 0x9e, 0xd7,
            0x13, 0xd3, 0x13, 0xfa, 0x20, 0xe9, 0x8d, 0xbc },
          { 0xc8, 0xd1, 0xe5, 0x11, 0x59, 0x52, 0xf7, 0xfa,
            0x37, 0x38, 0xb4, 0xc5, 0xce, 0xb2, 0xb0, 0x9a },
          { 0x0d, 0x9c, 0xc5, 0x0d, 0x16, 0xe1, 0xbc, 0xed, 
            0xcf, 0x60, 0x62, 0x09, 0x9d, 0x20, 0x83, 0x7e } } },
      { { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
          0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f },
        { 0x80, 0x00, 0x81, 0x01, 0x82, 0x02, 0x83, 0x03,
          0xa0, 0x20, 0xa1, 0x21, 0xa2, 0x22, 0xa3, 0x23 },
        { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
          0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff },
        { { 0x96, 0xed, 0xcc, 0xc3, 0xdd, 0x04, 0x7f, 0x75,
            0x63, 0x19, 0x37, 0x6f, 0x15, 0x22, 0x57, 0x56 },
          { 0x7a, 0x14, 0x76, 0x77, 0x95, 0x17, 0x7e, 0xc8,
            0x92, 0xe8, 0xdd, 0x15, 0xcb, 0x1f, 0xbc, 0xb1 },
          { 0x25, 0x3e, 0x2e, 0xa2, 0x41, 0x1b, 0xdd, 0xf5, 
            0x21, 0x48, 0x41, 0x71, 0xb3, 0x8d, 0x2f, 0x4c } } }
    };
  int tvidx, ridx;
  rng_context_t test_ctx;
  gpg_error_t err;
  const char *errtxt = NULL;
  unsigned char result[16];

  gcry_assert (tempvalue_for_x931_aes_driver);

  test_ctx = gcry_xcalloc (1, sizeof *test_ctx);
  setup_guards (test_ctx);
  
  lock_rng ();

  for (tvidx=0; tvidx < DIM (tv); tvidx++)
    {
      /* Setup the key.  */
      err = gcry_cipher_open (&test_ctx->cipher_hd,
                              GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_ECB,
                              GCRY_CIPHER_SECURE);
      if (err)
        {
          errtxt = "error creating cipher context for RNG";
          goto leave;
        }

      err = gcry_cipher_setkey (test_ctx->cipher_hd, tv[tvidx].key, 16);
      if (err)
        {
          errtxt = "error setting key for RNG";
          goto leave;
        }
      test_ctx->key_init_pid = getpid ();
      
      /* Setup the seed.  */
      memcpy (test_ctx->seed_V, tv[tvidx].v, 16);
      test_ctx->is_seeded = 1;
      test_ctx->seed_init_pid = getpid ();
      
      /* Setup a DT value.  */
      test_ctx->test_dt_ptr = tv[tvidx].dt;
      test_ctx->test_dt_counter = ( (tv[tvidx].dt[12] << 24) 
                                   |(tv[tvidx].dt[13] << 16)
                                   |(tv[tvidx].dt[14] << 8)
                                   |(tv[tvidx].dt[15]) );

      /* Get and compare the first three results.  */
      for (ridx=0; ridx < 3; ridx++)
        {
          /* Compute the next value.  */
          if (x931_aes_driver (result, 16, test_ctx))
            {
              errtxt = "X9.31 RNG core function failed";
              goto leave;
            }
          
          /* Compare it to the known value.  */
          if (memcmp (result, tv[tvidx].r[ridx], 16))
            {
              /* log_printhex ("x931_aes got: ", result, 16); */
              /* log_printhex ("x931_aes exp: ", tv[tvidx].r[ridx], 16); */
              errtxt = "RNG output does not match known value";
              goto leave;
            }
        }

      /* This test is actual pretty pointless because we use a local test
         context.  */
      if (test_ctx->key_init_pid != getpid ()
          || test_ctx->seed_init_pid != getpid ())
        {
          errtxt = "fork detection failed";
          goto leave;
        }

      gcry_cipher_close (test_ctx->cipher_hd);
      test_ctx->cipher_hd = NULL;
      test_ctx->is_seeded = 0;
      check_guards (test_ctx);
    }

 leave:
  unlock_rng ();
  gcry_cipher_close (test_ctx->cipher_hd);
  check_guards (test_ctx);
  gcry_free (test_ctx);
  if (report && errtxt)
    report ("random", 0, "KAT", errtxt);
  return errtxt? GPG_ERR_SELFTEST_FAILED : 0;
}


/* Run the self-tests.  */
gcry_error_t
_gcry_rngfips_selftest (selftest_report_func_t report)
{
  gcry_err_code_t ec;

#if defined(USE_RNDLINUX) || defined(USE_RNDW32)
  {
    char buffer[8];

    /* Do a simple test using the public interface.  This will also
       enforce full intialization of the RNG.  We need to be fully
       initialized due to the global requirement of the
       tempvalue_for_x931_aes_driver stuff. */
    gcry_randomize (buffer, sizeof buffer, GCRY_STRONG_RANDOM);
  }

  ec = selftest_kat (report);

#else /*!(USE_RNDLINUX||USE_RNDW32)*/
  report ("random", 0, "setup", "no entropy gathering module");
  ec = GPG_ERR_SELFTEST_FAILED;
#endif
  return gpg_error (ec);
}


/* Create a new test context for an external RNG test driver.  On
   success the test context is stored at R_CONTEXT; on failure NULL is
   stored at R_CONTEXT and an error code is returned.  */
gcry_err_code_t
_gcry_rngfips_init_external_test (void **r_context, unsigned int flags,
                                  const void *key, size_t keylen,
                                  const void *seed, size_t seedlen,
                                  const void *dt, size_t dtlen)
{
  gpg_error_t err;
  rng_context_t test_ctx;

  _gcry_rngfips_initialize (1);  /* Auto-initialize if needed.  */
  
  if (!r_context
      || !key  || keylen  != 16 
      || !seed || seedlen != 16
      || !dt   || dtlen   != 16 )
    return GPG_ERR_INV_ARG;

  test_ctx = gcry_calloc (1, sizeof *test_ctx + dtlen);
  if (!test_ctx)
    return gpg_err_code_from_syserror ();
  setup_guards (test_ctx);
  
  /* Setup the key.  */
  err = gcry_cipher_open (&test_ctx->cipher_hd,
                          GCRY_CIPHER_AES128, GCRY_CIPHER_MODE_ECB,
                          GCRY_CIPHER_SECURE);
  if (err)
    goto leave;

  err = gcry_cipher_setkey (test_ctx->cipher_hd, key, keylen);
  if (err)
    goto leave;

  test_ctx->key_init_pid = getpid ();
      
  /* Setup the seed.  */
  memcpy (test_ctx->seed_V, seed, seedlen);
  test_ctx->is_seeded = 1;
  test_ctx->seed_init_pid = getpid ();
  
  /* Setup a DT value.  Because our context structure only stores a
     pointer we copy the DT value to the extra space we allocated in
     the test_ctx and set the pointer to that address.  */
  memcpy ((unsigned char*)test_ctx + sizeof *test_ctx, dt, dtlen);
  test_ctx->test_dt_ptr = (unsigned char*)test_ctx + sizeof *test_ctx; 
  test_ctx->test_dt_counter = ( (test_ctx->test_dt_ptr[12] << 24) 
                               |(test_ctx->test_dt_ptr[13] << 16)
                               |(test_ctx->test_dt_ptr[14] << 8)
                               |(test_ctx->test_dt_ptr[15]) );

  if ( (flags & 1) )
    test_ctx->test_no_dup_check = 1;

  check_guards (test_ctx);
  /* All fine.  */
  err = 0;

 leave:
  if (err)
    {
      gcry_cipher_close (test_ctx->cipher_hd);
      gcry_free (test_ctx);
      *r_context = NULL;
    }
  else
    *r_context = test_ctx;
  return gcry_err_code (err);
}


/* Get BUFLEN bytes from the RNG using the test CONTEXT and store them
   at BUFFER.  Return 0 on success or an error code.  */
gcry_err_code_t
_gcry_rngfips_run_external_test (void *context, char *buffer, size_t buflen)
{
  rng_context_t test_ctx = context;

  if (!test_ctx || !buffer || buflen != 16)
    return GPG_ERR_INV_ARG;

  lock_rng ();
  get_random (buffer, buflen, test_ctx);
  unlock_rng ();
  return 0;
}

/* Release the test CONTEXT.  */
void
_gcry_rngfips_deinit_external_test (void *context)
{
  rng_context_t test_ctx = context;

  if (test_ctx)
    {
      gcry_cipher_close (test_ctx->cipher_hd);
      gcry_free (test_ctx);
    }
}


