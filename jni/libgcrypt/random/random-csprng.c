/* random-csprng.c - CSPRNG style random number generator (libgcrypt classic)
 * Copyright (C) 1998, 2000, 2001, 2002, 2003, 2004, 2005, 2006,
 *               2007, 2008  Free Software Foundation, Inc.
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
   This random number generator is modelled after the one described in
   Peter Gutmann's 1998 Usenix Security Symposium paper: "Software
   Generation of Practically Strong Random Numbers".  See also chapter
   6 in his book "Cryptographic Security Architecture", New York,
   2004, ISBN 0-387-95387-6.

   Note that the acronym CSPRNG stands for "Continuously Seeded
   PseudoRandom Number Generator" as used in Peter's implementation of
   the paper and not only for "Cryptographically Secure PseudoRandom
   Number Generator".
 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#ifdef	HAVE_GETHRTIME
#include <sys/times.h>
#endif
#ifdef HAVE_GETTIMEOFDAY
#include <sys/time.h>
#endif
#ifdef HAVE_GETRUSAGE
#include <sys/resource.h>
#endif
#ifdef __MINGW32__
#include <process.h>
#endif
#include "g10lib.h"
#include "../cipher/rmd.h"
#include "random.h"
#include "rand-internal.h"
#include "cipher.h" /* Required for the rmd160_hash_buffer() prototype.  */
#include "ath.h"

#ifndef RAND_MAX   /* For SunOS. */
#define RAND_MAX 32767
#endif

/* Check whether we can lock the seed file read write. */
#if defined(HAVE_FCNTL) && defined(HAVE_FTRUNCATE) && !defined(HAVE_W32_SYSTEM)
#define LOCK_SEED_FILE 1
#else
#define LOCK_SEED_FILE 0
#endif

/* Define the constant we use for transforming the pool at read-out. */
#if SIZEOF_UNSIGNED_LONG == 8
#define ADD_VALUE 0xa5a5a5a5a5a5a5a5
#elif SIZEOF_UNSIGNED_LONG == 4
#define ADD_VALUE 0xa5a5a5a5
#else
#error weird size for an unsigned long
#endif

/* Contstants pertaining to the hash pool. */
#define BLOCKLEN  64   /* Hash this amount of bytes... */
#define DIGESTLEN 20   /* ... into a digest of this length (rmd160). */
/* POOLBLOCKS is the number of digests which make up the pool.  */
#define POOLBLOCKS 30
/* POOLSIZE must be a multiple of the digest length to make the AND
   operations faster, the size should also be a multiple of unsigned
   long.  */
#define POOLSIZE (POOLBLOCKS*DIGESTLEN)
#if (POOLSIZE % SIZEOF_UNSIGNED_LONG)
#error Please make sure that poolsize is a multiple of unsigned long
#endif
#define POOLWORDS (POOLSIZE / SIZEOF_UNSIGNED_LONG)


/* RNDPOOL is the pool we use to collect the entropy and to stir it
   up.  Its allocated size is POOLSIZE+BLOCKLEN.  Note that this is
   also an indication on whether the module has been fully
   initialized. */
static unsigned char *rndpool;	

/* KEYPOOL is used as a scratch copy to read out random from RNDPOOL.
   Its allocated size is also POOLSIZE+BLOCKLEN.  */
static unsigned char *keypool;	

/* This is the offset into RNDPOOL where the next random bytes are to
   be mixed in.  */
static size_t pool_writepos;

/* When reading data out of KEYPOOL, we start the read at different
   positions.  This variable keeps track on where to read next.  */
static size_t pool_readpos;

/* This flag is set to true as soon as the pool has been completely
   filled the first time.  This may happen either by rereading a seed
   file or by adding enough entropy.  */
static int pool_filled;

/* This counter is used to track whether the initial seeding has been
   done with enough bytes from a reliable entropy source.  */
static size_t pool_filled_counter;

/* If random of level GCRY_VERY_STRONG_RANDOM has been requested we
   have stricter requirements on what kind of entropy is in the pool.
   In particular POOL_FILLED is not sufficient.  Thus we add some
   extra seeding and set this flag to true if the extra seeding has
   been done.  */
static int did_initial_extra_seeding;

/* This variable is used to estimated the amount of fresh entropy
   available in RNDPOOL.  */
static int pool_balance;

/* After a mixing operation this variable will be set to true and
   cleared if new entropy has been added or a remix is required for
   otehr reasons.  */
static int just_mixed;

/* The name of the seed file or NULL if no seed file has been defined.
   The seed file needs to be regsitered at initialiation time.  We
   keep a malloced copy here.  */
static char *seed_file_name;

/* If a seed file has been registered and maybe updated on exit this
   flag set. */
static int allow_seed_file_update;

/* Option flag set at initialiation time to force allocation of the
   pool in secure memory.  */
static int secure_alloc;

/* This function pointer is set to the actual entropy gathering
   function during initailization.  After initialization it is
   guaranteed to point to function.  (On systems without a random
   gatherer module a dummy function is used).*/
static int (*slow_gather_fnc)(void (*)(const void*, size_t,
                                       enum random_origins),
                              enum random_origins, size_t, int);

/* This function is set to the actual fast entropy gathering fucntion
   during initialization.  If it is NULL, no such function is
   available. */
static void (*fast_gather_fnc)(void (*)(const void*, size_t,
                                        enum random_origins),
                               enum random_origins);


/* Option flag useful for debugging and the test suite.  If set
   requests for very strong random are degraded to strong random.  Not
   used by regular applications.  */
static int quick_test;

/* On systems without entropy gathering modules, this flag is set to
   indicate that the random generator is not working properly.  A
   warning message is issued as well.  This is useful only for
   debugging and during development.  */
static int faked_rng;

/* This is the lock we use to protect all pool operations.  */
static ath_mutex_t pool_lock = ATH_MUTEX_INITIALIZER;

/* This is a helper for assert calls.  These calls are used to assert
   that functions are called in a locked state.  It is not meant to be
   thread-safe but as a method to get aware of missing locks in the
   test suite.  */
static int pool_is_locked;

/* This is the lock we use to protect the buffer used by the nonce
   generation.  */
static ath_mutex_t nonce_buffer_lock = ATH_MUTEX_INITIALIZER;


/* We keep some counters in this structure for the sake of the
   _gcry_random_dump_stats () function.  */
static struct
{
  unsigned long mixrnd;
  unsigned long mixkey;
  unsigned long slowpolls;
  unsigned long fastpolls;
  unsigned long getbytes1;
  unsigned long ngetbytes1;
  unsigned long getbytes2;
  unsigned long ngetbytes2;
  unsigned long addbytes;
  unsigned long naddbytes;
} rndstats;



/* --- Stuff pertaining to the random daemon support. --- */
#ifdef USE_RANDOM_DAEMON

/* If ALLOW_DAEMON is true, the module will try to use the random
   daemon first.  If the daemon has failed, this variable is set to
   back to false and the code continues as normal.  Note, we don't
   test this flag in a locked state because a wrong value does not
   harm and the trhead will find out itself that the daemon does not
   work and set it (again) to false.  */
static int allow_daemon;       

/* During initialization, the user may set a non-default socket name
   for accessing the random daemon.  If this value is NULL, the
   default name will be used. */
static char *daemon_socket_name;

#endif /*USE_RANDOM_DAEMON*/



/* ---  Prototypes  --- */
static void read_pool (byte *buffer, size_t length, int level );
static void add_randomness (const void *buffer, size_t length, 
                            enum random_origins origin);
static void random_poll (void);
static void do_fast_random_poll (void);
static int (*getfnc_gather_random (void))(void (*)(const void*, size_t, 
                                                   enum random_origins), 
                                          enum random_origins, size_t, int);
static void (*getfnc_fast_random_poll (void))(void (*)(const void*, size_t,
                                                       enum random_origins),
                                              enum random_origins);
static void read_random_source (enum random_origins origin,
                                size_t length, int level);
static int gather_faked (void (*add)(const void*, size_t, enum random_origins),
                         enum random_origins, size_t length, int level );



/* ---  Functions  --- */


/* Basic initialization which is required to initialize mutexes and
   such.  It does not run a full initialization so that the filling of
   the random pool can be delayed until it is actually needed.  We
   assume that this function is used before any concurrent access
   happens. */
static void
initialize_basics(void)
{
  static int initialized;
  int err;

  if (!initialized)
    {
      initialized = 1;
      err = ath_mutex_init (&pool_lock);
      if (err)
        log_fatal ("failed to create the pool lock: %s\n", strerror (err) );
      
      err = ath_mutex_init (&nonce_buffer_lock);
      if (err)
        log_fatal ("failed to create the nonce buffer lock: %s\n",
                   strerror (err) );

#ifdef USE_RANDOM_DAEMON
      _gcry_daemon_initialize_basics ();
#endif /*USE_RANDOM_DAEMON*/

      /* Make sure that we are still using the values we have
         traditionally used for the random levels.  */
      gcry_assert (GCRY_WEAK_RANDOM == 0 
                   && GCRY_STRONG_RANDOM == 1
                   && GCRY_VERY_STRONG_RANDOM == 2);
    }
}

/* Take the pool lock. */
static void
lock_pool (void)
{
  int err; 

  err = ath_mutex_lock (&pool_lock);
  if (err)
    log_fatal ("failed to acquire the pool lock: %s\n", strerror (err));
  pool_is_locked = 1;
}

/* Release the pool lock. */
static void
unlock_pool (void)
{
  int err; 

  pool_is_locked = 0;
  err = ath_mutex_unlock (&pool_lock);
  if (err)
    log_fatal ("failed to release the pool lock: %s\n", strerror (err));
}


/* Full initialization of this module. */
static void
initialize(void)
{
  /* Although the basic initialization should have happened already,
     we call it here to make sure that all prerequisites are met.  */
  initialize_basics ();

  /* Now we can look the pool and complete the initialization if
     necessary.  */
  lock_pool ();
  if (!rndpool)
    {
      /* The data buffer is allocated somewhat larger, so that we can
         use this extra space (which is allocated in secure memory) as
         a temporary hash buffer */
      rndpool = (secure_alloc
                 ? gcry_xcalloc_secure (1, POOLSIZE + BLOCKLEN)
                 : gcry_xcalloc (1, POOLSIZE + BLOCKLEN));
      keypool = (secure_alloc
                 ? gcry_xcalloc_secure (1, POOLSIZE + BLOCKLEN)
                 : gcry_xcalloc (1, POOLSIZE + BLOCKLEN));

      /* Setup the slow entropy gathering function.  The code requires
         that this function exists. */
      slow_gather_fnc = getfnc_gather_random ();
      if (!slow_gather_fnc)
        {
          faked_rng = 1;
          slow_gather_fnc = gather_faked;
	}
      
      /* Setup the fast entropy gathering function.  */
      fast_gather_fnc = getfnc_fast_random_poll ();

    }
  unlock_pool ();
}




/* Initialize this random subsystem.  If FULL is false, this function
   merely calls the initialize and does not do anything more.  Doing
   this is not really required but when running in a threaded
   environment we might get a race condition otherwise. */
void
_gcry_rngcsprng_initialize (int full)
{
  if (!full)
    initialize_basics ();
  else
    initialize ();
}


void
_gcry_rngcsprng_dump_stats (void)
{
  /* In theory we would need to lock the stats here.  However this
     function is usually called during cleanup and then we _might_ run
     into problems.  */

  log_info ("random usage: poolsize=%d mixed=%lu polls=%lu/%lu added=%lu/%lu\n"
	    "              outmix=%lu getlvl1=%lu/%lu getlvl2=%lu/%lu%s\n",
            POOLSIZE, rndstats.mixrnd, rndstats.slowpolls, rndstats.fastpolls,
            rndstats.naddbytes, rndstats.addbytes,
            rndstats.mixkey, rndstats.ngetbytes1, rndstats.getbytes1,
            rndstats.ngetbytes2, rndstats.getbytes2,
            _gcry_rndhw_failed_p()? " (hwrng failed)":"");
}


/* This function should be called during initialization and before
   intialization of this module to place the random pools into secure
   memory.  */
void
_gcry_rngcsprng_secure_alloc (void)
{
  secure_alloc = 1;
}


/* This may be called before full initialization to degrade the
   quality of the RNG for the sake of a faster running test suite.  */
void
_gcry_rngcsprng_enable_quick_gen (void)
{
  quick_test = 1;
}


void
_gcry_rngcsprng_set_daemon_socket (const char *socketname)
{
#ifdef USE_RANDOM_DAEMON
  if (daemon_socket_name)
    BUG ();

  daemon_socket_name = gcry_xstrdup (socketname);
#else /*!USE_RANDOM_DAEMON*/
  (void)socketname;
#endif /*!USE_RANDOM_DAEMON*/
}

/* With ONOFF set to 1, enable the use of the daemon.  With ONOFF set
   to 0, disable the use of the daemon.  With ONOF set to -1, return
   whether the daemon has been enabled. */
int
_gcry_rngcsprng_use_daemon (int onoff)
{
#ifdef USE_RANDOM_DAEMON
  int last;
  
  /* This is not really thread safe.  However it is expected that this
     function is being called during initialization and at that point
     we are for other reasons not really thread safe.  We do not want
     to lock it because we might eventually decide that this function
     may even be called prior to gcry_check_version.  */
  last = allow_daemon;
  if (onoff != -1)
    allow_daemon = onoff;

  return last;
#else /*!USE_RANDOM_DAEMON*/
  (void)onoff;
  return 0;
#endif /*!USE_RANDOM_DAEMON*/
}


/* This function returns true if no real RNG is available or the
   quality of the RNG has been degraded for test purposes.  */
int
_gcry_rngcsprng_is_faked (void)
{
  /* We need to initialize due to the runtime determination of
     available entropy gather modules.  */
  initialize();
  return (faked_rng || quick_test);
}


/* Add BUFLEN bytes from BUF to the internal random pool.  QUALITY
   should be in the range of 0..100 to indicate the goodness of the
   entropy added, or -1 for goodness not known.  */
gcry_error_t
_gcry_rngcsprng_add_bytes (const void *buf, size_t buflen, int quality)
{
  size_t nbytes;
  const char *bufptr;

  if (quality == -1)
    quality = 35;
  else if (quality > 100)
    quality = 100;
  else if (quality < 0)
    quality = 0;
      
  if (!buf)
    return gpg_error (GPG_ERR_INV_ARG);

  if (!buflen || quality < 10)
    return 0; /* Take a shortcut. */

  /* Because we don't increment the entropy estimation with FASTPOLL,
     we don't need to take lock that estimation while adding from an
     external source.  This limited entropy estimation also means that
     we can't take QUALITY into account.  */
  initialize_basics ();
  bufptr = buf;
  while (buflen)
    {
      nbytes = buflen > POOLSIZE? POOLSIZE : buflen;
      lock_pool ();
      if (rndpool)
        add_randomness (bufptr, nbytes, RANDOM_ORIGIN_EXTERNAL);
      unlock_pool ();
      bufptr += nbytes;
      buflen -= nbytes;
    }
  return 0;
}   

    
/* Public function to fill the buffer with LENGTH bytes of
   cryptographically strong random bytes.  Level GCRY_WEAK_RANDOM is
   not very strong, GCRY_STRONG_RANDOM is strong enough for most
   usage, GCRY_VERY_STRONG_RANDOM is good for key generation stuff but
   may be very slow.  */
void
_gcry_rngcsprng_randomize (void *buffer, size_t length,
                           enum gcry_random_level level)
{
  unsigned char *p;

  /* Make sure we are initialized. */
  initialize ();

  /* Handle our hack used for regression tests of Libgcrypt. */
  if ( quick_test && level > GCRY_STRONG_RANDOM )
    level = GCRY_STRONG_RANDOM;

  /* Make sure the level is okay. */
  level &= 3;

#ifdef USE_RANDOM_DAEMON
  if (allow_daemon
      && !_gcry_daemon_randomize (daemon_socket_name, buffer, length, level))
    return; /* The daemon succeeded. */
  allow_daemon = 0; /* Daemon failed - switch off. */
#endif /*USE_RANDOM_DAEMON*/

  /* Acquire the pool lock. */
  lock_pool ();

  /* Update the statistics. */
  if (level >= GCRY_VERY_STRONG_RANDOM)
    {
      rndstats.getbytes2 += length;
      rndstats.ngetbytes2++;
    }
  else
    {
      rndstats.getbytes1 += length;
      rndstats.ngetbytes1++;
    }

  /* Read the random into the provided buffer. */
  for (p = buffer; length > 0;)
    {
      size_t n;

      n = length > POOLSIZE? POOLSIZE : length;
      read_pool (p, n, level);
      length -= n;
      p += n;
    }

  /* Release the pool lock. */
  unlock_pool ();
}




/*
   Mix the pool:

   |........blocks*20byte........|20byte|..44byte..|
   <..44byte..>           <20byte> 
        |                    |
        |                    +------+
        +---------------------------|----------+
                                    v          v
   |........blocks*20byte........|20byte|..44byte..|
                                 <.....64bytes.....>   
                                         |
      +----------------------------------+
     Hash
      v
   |.............................|20byte|..44byte..|
   <20byte><20byte><..44byte..>
      |                |
      |                +---------------------+
      +-----------------------------+        |
                                    v        v
   |.............................|20byte|..44byte..|
                                 <.....64byte......>
                                        |
              +-------------------------+
             Hash
              v
   |.............................|20byte|..44byte..|
   <20byte><20byte><..44byte..>

   and so on until we did this for all blocks. 

   To better protect against implementation errors in this code, we
   xor a digest of the entire pool into the pool before mixing.

   Note: this function must only be called with a locked pool.
 */
static void
mix_pool(unsigned char *pool)
{
  static unsigned char failsafe_digest[DIGESTLEN];
  static int failsafe_digest_valid;

  unsigned char *hashbuf = pool + POOLSIZE;
  unsigned char *p, *pend;
  int i, n;
  RMD160_CONTEXT md;

#if DIGESTLEN != 20
#error must have a digest length of 20 for ripe-md-160
#endif

  gcry_assert (pool_is_locked);
  _gcry_rmd160_init( &md );

  /* Loop over the pool.  */
  pend = pool + POOLSIZE;
  memcpy(hashbuf, pend - DIGESTLEN, DIGESTLEN );
  memcpy(hashbuf+DIGESTLEN, pool, BLOCKLEN-DIGESTLEN);
  _gcry_rmd160_mixblock( &md, hashbuf);
  memcpy(pool, hashbuf, 20 );

  if (failsafe_digest_valid && pool == rndpool)
    {
      for (i=0; i < 20; i++)
        pool[i] ^= failsafe_digest[i];
    }
  
  p = pool;
  for (n=1; n < POOLBLOCKS; n++)
    {
      memcpy (hashbuf, p, DIGESTLEN);

      p += DIGESTLEN;
      if (p+DIGESTLEN+BLOCKLEN < pend)
        memcpy (hashbuf+DIGESTLEN, p+DIGESTLEN, BLOCKLEN-DIGESTLEN);
      else 
        {
          unsigned char *pp = p + DIGESTLEN;
          
          for (i=DIGESTLEN; i < BLOCKLEN; i++ )
            {
              if ( pp >= pend )
                pp = pool;
              hashbuf[i] = *pp++;
	    }
	}
      
      _gcry_rmd160_mixblock ( &md, hashbuf);
      memcpy(p, hashbuf, 20 );
    }

    /* Our hash implementation does only leave small parts (64 bytes)
       of the pool on the stack, so it is okay not to require secure
       memory here.  Before we use this pool, it will be copied to the
       help buffer anyway. */
    if ( pool == rndpool)
      {
        _gcry_rmd160_hash_buffer (failsafe_digest, pool, POOLSIZE);
        failsafe_digest_valid = 1;
      }

    _gcry_burn_stack (384); /* for the rmd160_mixblock(), rmd160_hash_buffer */
}


void
_gcry_rngcsprng_set_seed_file (const char *name)
{
  if (seed_file_name)
    BUG ();
  seed_file_name = gcry_xstrdup (name);
}


/* Lock an open file identified by file descriptor FD and wait a
   reasonable time to succeed.  With FOR_WRITE set to true a write
   lock will be taken.  FNAME is used only for diagnostics. Returns 0
   on success or -1 on error. */
static int
lock_seed_file (int fd, const char *fname, int for_write)
{
#if LOCK_SEED_FILE
  struct flock lck;
  struct timeval tv;
  int backoff=0;

  /* We take a lock on the entire file. */
  memset (&lck, 0, sizeof lck);
  lck.l_type = for_write? F_WRLCK : F_RDLCK;
  lck.l_whence = SEEK_SET;

  while (fcntl (fd, F_SETLK, &lck) == -1)
    {
      if (errno != EAGAIN && errno != EACCES)
        {
          log_info (_("can't lock `%s': %s\n"), fname, strerror (errno));
          return -1;
        }

      if (backoff > 2) /* Show the first message after ~2.25 seconds. */
        log_info( _("waiting for lock on `%s'...\n"), fname);
      
      tv.tv_sec = backoff;
      tv.tv_usec = 250000;
      select (0, NULL, NULL, NULL, &tv);
      if (backoff < 10)
        backoff++ ;
    }
#endif /*LOCK_SEED_FILE*/
  return 0;
}


/* Read in a seed from the random_seed file and return true if this
   was successful.

   Note: Multiple instances of applications sharing the same random
   seed file can be started in parallel, in which case they will read
   out the same pool and then race for updating it (the last update
   overwrites earlier updates).  They will differentiate only by the
   weak entropy that is added in read_seed_file based on the PID and
   clock, and up to 16 bytes of weak random non-blockingly.  The
   consequence is that the output of these different instances is
   correlated to some extent.  In the perfect scenario, the attacker
   can control (or at least guess) the PID and clock of the
   application, and drain the system's entropy pool to reduce the "up
   to 16 bytes" above to 0.  Then the dependencies of the inital
   states of the pools are completely known.  */
static int
read_seed_file (void)
{
  int fd;
  struct stat sb;
  unsigned char buffer[POOLSIZE];
  int n;

  gcry_assert (pool_is_locked);

  if (!seed_file_name)
    return 0;
  
#ifdef HAVE_DOSISH_SYSTEM
  fd = open( seed_file_name, O_RDONLY | O_BINARY );
#else
  fd = open( seed_file_name, O_RDONLY );
#endif
  if( fd == -1 && errno == ENOENT)
    {
      allow_seed_file_update = 1;
      return 0;
    }

  if (fd == -1 )
    {
      log_info(_("can't open `%s': %s\n"), seed_file_name, strerror(errno) );
      return 0;
    }
  if (lock_seed_file (fd, seed_file_name, 0))
    {
      close (fd);
      return 0;
    }
  if (fstat( fd, &sb ) )
    {
      log_info(_("can't stat `%s': %s\n"), seed_file_name, strerror(errno) );
      close(fd);
      return 0;
    }
  if (!S_ISREG(sb.st_mode) )
    {
      log_info(_("`%s' is not a regular file - ignored\n"), seed_file_name );
      close(fd);
      return 0;
    }
  if (!sb.st_size )
    {
      log_info(_("note: random_seed file is empty\n") );
      close(fd);
      allow_seed_file_update = 1;
      return 0;
    }
  if (sb.st_size != POOLSIZE ) 
    {
      log_info(_("warning: invalid size of random_seed file - not used\n") );
      close(fd);
      return 0;
    }

  do
    {
      n = read( fd, buffer, POOLSIZE );
    } 
  while (n == -1 && errno == EINTR );

  if (n != POOLSIZE)
    {
      log_fatal(_("can't read `%s': %s\n"), seed_file_name,strerror(errno) );
      close(fd);/*NOTREACHED*/
      return 0;
    }
  
  close(fd);

  add_randomness( buffer, POOLSIZE, RANDOM_ORIGIN_INIT );
  /* add some minor entropy to the pool now (this will also force a mixing) */
  {	
    pid_t x = getpid();
    add_randomness( &x, sizeof(x), RANDOM_ORIGIN_INIT );
  }
  {
    time_t x = time(NULL);
    add_randomness( &x, sizeof(x), RANDOM_ORIGIN_INIT );
  }
  {	
    clock_t x = clock();
    add_randomness( &x, sizeof(x), RANDOM_ORIGIN_INIT );
  }

  /* And read a few bytes from our entropy source.  By using a level
   * of 0 this will not block and might not return anything with some
   * entropy drivers, however the rndlinux driver will use
   * /dev/urandom and return some stuff - Do not read too much as we
   * want to be friendly to the scare system entropy resource. */
  read_random_source ( RANDOM_ORIGIN_INIT, 16, GCRY_WEAK_RANDOM );

  allow_seed_file_update = 1;
  return 1;
}


void
_gcry_rngcsprng_update_seed_file (void)
{
  unsigned long *sp, *dp;
  int fd, i;

  /* We do only a basic initialization so that we can lock the pool.
     This is required to cope with the case that this function is
     called by some cleanup code at a point where the RNG has never
     been initialized.  */
  initialize_basics ();
  lock_pool ();

  if ( !seed_file_name || !rndpool || !pool_filled )
    {
      unlock_pool ();
      return;
    }
  if ( !allow_seed_file_update )
    {
      unlock_pool ();
      log_info(_("note: random_seed file not updated\n"));
      return;
    }

  /* At this point we know that there is something in the pool and
     thus we can conclude that the pool has been fully initialized.  */


  /* Copy the entropy pool to a scratch pool and mix both of them. */
  for (i=0,dp=(unsigned long*)keypool, sp=(unsigned long*)rndpool;
       i < POOLWORDS; i++, dp++, sp++ ) 
    {
      *dp = *sp + ADD_VALUE;
    }
  mix_pool(rndpool); rndstats.mixrnd++;
  mix_pool(keypool); rndstats.mixkey++;

#if defined(HAVE_DOSISH_SYSTEM) || defined(__CYGWIN__)
  fd = open (seed_file_name, O_WRONLY|O_CREAT|O_TRUNC|O_BINARY,
             S_IRUSR|S_IWUSR );
#else
# if LOCK_SEED_FILE
    fd = open (seed_file_name, O_WRONLY|O_CREAT, S_IRUSR|S_IWUSR );
# else
    fd = open (seed_file_name, O_WRONLY|O_CREAT|O_TRUNC, S_IRUSR|S_IWUSR );
# endif
#endif

  if (fd == -1 )
    log_info (_("can't create `%s': %s\n"), seed_file_name, strerror(errno) );
  else if (lock_seed_file (fd, seed_file_name, 1))
    {
      close (fd);
    }
#if LOCK_SEED_FILE
  else if (ftruncate (fd, 0))
    {
      log_info(_("can't write `%s': %s\n"), seed_file_name, strerror(errno));
      close (fd);
    }
#endif /*LOCK_SEED_FILE*/
  else 
    {
      do
        {
          i = write (fd, keypool, POOLSIZE );
        } 
      while (i == -1 && errno == EINTR);
      if (i != POOLSIZE) 
        log_info (_("can't write `%s': %s\n"),seed_file_name, strerror(errno));
      if (close(fd))
        log_info (_("can't close `%s': %s\n"),seed_file_name, strerror(errno));
    }
  
  unlock_pool ();
}


/* Read random out of the pool.  This function is the core of the
   public random functions.  Note that Level GCRY_WEAK_RANDOM is not
   anymore handled special and in fact is an alias in the API for
   level GCRY_STRONG_RANDOM.  Must be called with the pool already
   locked.  */
static void
read_pool (byte *buffer, size_t length, int level)
{
  int i;
  unsigned long *sp, *dp;
  /* The volatile is there to make sure the compiler does not optimize
     the code away in case the getpid function is badly attributed.
     Note that we keep a pid in a static variable as well as in a
     stack based one; the latter is to detect ill behaving thread
     libraries, ignoring the pool mutexes. */
  static volatile pid_t my_pid = (pid_t)(-1); 
  volatile pid_t my_pid2;

  gcry_assert (pool_is_locked);

 retry:
  /* Get our own pid, so that we can detect a fork. */
  my_pid2 = getpid ();
  if (my_pid == (pid_t)(-1))                                
    my_pid = my_pid2;
  if ( my_pid != my_pid2 )
    {
      /* We detected a plain fork; i.e. we are now the child.  Update
         the static pid and add some randomness. */
      pid_t x;

      my_pid = my_pid2;
      x = my_pid;
      add_randomness (&x, sizeof(x), RANDOM_ORIGIN_INIT);
      just_mixed = 0; /* Make sure it will get mixed. */
    }

  gcry_assert (pool_is_locked);

  /* Our code does not allow to extract more than POOLSIZE.  Better
     check it here. */
  if (length > POOLSIZE)
    {
      log_bug("too many random bits requested\n");
    }

  if (!pool_filled)
    {
      if (read_seed_file() )
        pool_filled = 1;
    }

  /* For level 2 quality (key generation) we always make sure that the
     pool has been seeded enough initially. */
  if (level == GCRY_VERY_STRONG_RANDOM && !did_initial_extra_seeding)
    {
      size_t needed;

      pool_balance = 0;
      needed = length - pool_balance;
      if (needed < POOLSIZE/2)
        needed = POOLSIZE/2;
      else if( needed > POOLSIZE )
        BUG ();
      read_random_source (RANDOM_ORIGIN_EXTRAPOLL, needed,
                          GCRY_VERY_STRONG_RANDOM);
      pool_balance += needed;
      did_initial_extra_seeding = 1;
    }

  /* For level 2 make sure that there is enough random in the pool. */
  if (level == GCRY_VERY_STRONG_RANDOM && pool_balance < length)
    {
      size_t needed;
      
      if (pool_balance < 0)
        pool_balance = 0;
      needed = length - pool_balance;
      if (needed > POOLSIZE)
        BUG ();
      read_random_source (RANDOM_ORIGIN_EXTRAPOLL, needed,
                          GCRY_VERY_STRONG_RANDOM);
      pool_balance += needed;
    }

  /* Make sure the pool is filled. */
  while (!pool_filled)
    random_poll();

  /* Always do a fast random poll (we have to use the unlocked version). */
  do_fast_random_poll();
  
  /* Mix the pid in so that we for sure won't deliver the same random
     after a fork. */
  {
    pid_t apid = my_pid;
    add_randomness (&apid, sizeof (apid), RANDOM_ORIGIN_INIT);
  }

  /* Mix the pool (if add_randomness() didn't it). */
  if (!just_mixed)
    {
      mix_pool(rndpool);
      rndstats.mixrnd++;
    }

  /* Create a new pool. */
  for(i=0,dp=(unsigned long*)keypool, sp=(unsigned long*)rndpool;
      i < POOLWORDS; i++, dp++, sp++ )
    *dp = *sp + ADD_VALUE;

  /* Mix both pools. */
  mix_pool(rndpool); rndstats.mixrnd++;
  mix_pool(keypool); rndstats.mixkey++;

  /* Read the requested data.  We use a read pointer to read from a
     different position each time.  */
  while (length--)
    {
      *buffer++ = keypool[pool_readpos++];
      if (pool_readpos >= POOLSIZE)
        pool_readpos = 0;
      pool_balance--;
    }
 
  if (pool_balance < 0)
    pool_balance = 0;

  /* Clear the keypool. */
  memset (keypool, 0, POOLSIZE);

  /* We need to detect whether a fork has happened.  A fork might have
     an identical pool and thus the child and the parent could emit
     the very same random number.  This test here is to detect forks
     in a multi-threaded process.  It does not work with all thread
     implementations in particular not with pthreads.  However it is
     good enough for GNU Pth. */
  if ( getpid () != my_pid2 )
    {
      pid_t x = getpid();
      add_randomness (&x, sizeof(x), RANDOM_ORIGIN_INIT);
      just_mixed = 0; /* Make sure it will get mixed. */
      my_pid = x;     /* Also update the static pid. */
      goto retry;
    }
}



/* Add LENGTH bytes of randomness from buffer to the pool.  ORIGIN is
   used to specify the randomness origin.  This is one of the
   RANDOM_ORIGIN_* values. */
static void
add_randomness (const void *buffer, size_t length, enum random_origins origin)
{
  const unsigned char *p = buffer;
  size_t count = 0;

  gcry_assert (pool_is_locked);

  rndstats.addbytes += length;
  rndstats.naddbytes++;
  while (length-- )
    {
      rndpool[pool_writepos++] ^= *p++;
      count++;
      if (pool_writepos >= POOLSIZE )
        {
          /* It is possible that we are invoked before the pool is
             filled using an unreliable origin of entropy, for example
             the fast random poll.  To avoid flagging the pool as
             filled in this case, we track the initial filling state
             separately.  See also the remarks about the seed file. */
          if (origin >= RANDOM_ORIGIN_SLOWPOLL && !pool_filled)
            {
              pool_filled_counter += count;
              count = 0;
              if (pool_filled_counter >= POOLSIZE)
                pool_filled = 1;
            }
          pool_writepos = 0;
          mix_pool(rndpool); rndstats.mixrnd++;
          just_mixed = !length;
	}
    }
}



static void
random_poll()
{
  rndstats.slowpolls++;
  read_random_source (RANDOM_ORIGIN_SLOWPOLL, POOLSIZE/5, GCRY_STRONG_RANDOM);
}


/* Runtime determination of the slow entropy gathering module.  */
static int (*
getfnc_gather_random (void))(void (*)(const void*, size_t, 
                                      enum random_origins), 
                             enum random_origins, size_t, int)
{
  int (*fnc)(void (*)(const void*, size_t, enum random_origins), 
             enum random_origins, size_t, int);
  
#if USE_RNDLINUX
  if ( !access (NAME_OF_DEV_RANDOM, R_OK)
       && !access (NAME_OF_DEV_URANDOM, R_OK))
    {
      fnc = _gcry_rndlinux_gather_random;
      return fnc;
    }
#endif

#if USE_RNDEGD
  if ( _gcry_rndegd_connect_socket (1) != -1 )
    {
      fnc = _gcry_rndegd_gather_random;
      return fnc;
    }
#endif

#if USE_RNDUNIX
  fnc = _gcry_rndunix_gather_random;
  return fnc;
#endif

#if USE_RNDW32
  fnc = _gcry_rndw32_gather_random;
  return fnc;
#endif

  log_fatal (_("no entropy gathering module detected\n"));

  return NULL; /*NOTREACHED*/
}

/* Runtime determination of the fast entropy gathering function.
   (Currently a compile time method is used.)  */
static void (*
getfnc_fast_random_poll (void))( void (*)(const void*, size_t,
                                          enum random_origins),
                                 enum random_origins)
{
#if USE_RNDW32
  return _gcry_rndw32_gather_random_fast;
#endif
  return NULL;
}



static void
do_fast_random_poll (void)
{
  gcry_assert (pool_is_locked);

  rndstats.fastpolls++;

  if (fast_gather_fnc)
    fast_gather_fnc (add_randomness, RANDOM_ORIGIN_FASTPOLL);

  /* Continue with the generic functions. */
#if HAVE_GETHRTIME
  {	
    hrtime_t tv;
    tv = gethrtime();
    add_randomness( &tv, sizeof(tv), RANDOM_ORIGIN_FASTPOLL );
  }
#elif HAVE_GETTIMEOFDAY
  {	
    struct timeval tv;
    if( gettimeofday( &tv, NULL ) )
      BUG();
    add_randomness( &tv.tv_sec, sizeof(tv.tv_sec), RANDOM_ORIGIN_FASTPOLL );
    add_randomness( &tv.tv_usec, sizeof(tv.tv_usec), RANDOM_ORIGIN_FASTPOLL );
  }
#elif HAVE_CLOCK_GETTIME
  {	struct timespec tv;
  if( clock_gettime( CLOCK_REALTIME, &tv ) == -1 )
    BUG();
  add_randomness( &tv.tv_sec, sizeof(tv.tv_sec), RANDOM_ORIGIN_FASTPOLL );
  add_randomness( &tv.tv_nsec, sizeof(tv.tv_nsec), RANDOM_ORIGIN_FASTPOLL );
  }
#else /* use times */
# ifndef HAVE_DOSISH_SYSTEM
  {	struct tms buf;
  times( &buf );
  add_randomness( &buf, sizeof buf, RANDOM_ORIGIN_FASTPOLL );
  }
# endif
#endif

#ifdef HAVE_GETRUSAGE
# ifdef RUSAGE_SELF
  {	
    struct rusage buf;
    /* QNX/Neutrino does return ENOSYS - so we just ignore it and add
       whatever is in buf.  In a chroot environment it might not work
       at all (i.e. because /proc/ is not accessible), so we better
       ignore all error codes and hope for the best. */
    getrusage (RUSAGE_SELF, &buf );
    add_randomness( &buf, sizeof buf, RANDOM_ORIGIN_FASTPOLL );
    memset( &buf, 0, sizeof buf );
  }
# else /*!RUSAGE_SELF*/
#  ifdef __GCC__
#   warning There is no RUSAGE_SELF on this system
#  endif
# endif /*!RUSAGE_SELF*/
#endif /*HAVE_GETRUSAGE*/

  /* Time and clock are availabe on all systems - so we better do it
     just in case one of the above functions didn't work.  */
  {
    time_t x = time(NULL);
    add_randomness( &x, sizeof(x), RANDOM_ORIGIN_FASTPOLL );
  }
  {	
    clock_t x = clock();
    add_randomness( &x, sizeof(x), RANDOM_ORIGIN_FASTPOLL );
  }

  /* If the system features a fast hardware RNG, read some bytes from
     there.  */
  _gcry_rndhw_poll_fast (add_randomness, RANDOM_ORIGIN_FASTPOLL);
}


/* The fast random pool function as called at some places in
   libgcrypt.  This is merely a wrapper to make sure that this module
   is initalized and to look the pool.  Note, that this function is a
   NOP unless a random function has been used or _gcry_initialize (1)
   has been used.  We use this hack so that the internal use of this
   function in cipher_open and md_open won't start filling up the
   random pool, even if no random will be required by the process. */
void
_gcry_rngcsprng_fast_poll (void)
{
  initialize_basics ();

  lock_pool ();
  if (rndpool)
    {
      /* Yes, we are fully initialized. */
      do_fast_random_poll ();
    }
  unlock_pool ();
}



static void
read_random_source (enum random_origins orgin, size_t length, int level )
{
  if ( !slow_gather_fnc )
    log_fatal ("Slow entropy gathering module not yet initialized\n");

  if ( slow_gather_fnc (add_randomness, orgin, length, level) < 0)
    log_fatal ("No way to gather entropy for the RNG\n");
}


static int
gather_faked (void (*add)(const void*, size_t, enum random_origins),
              enum random_origins origin, size_t length, int level )
{
  static int initialized=0;
  size_t n;
  char *buffer, *p;
  
  (void)add;
  (void)level;
  
  if ( !initialized )
    {
      log_info(_("WARNING: using insecure random number generator!!\n"));
      initialized=1;
#ifdef HAVE_RAND
      srand( time(NULL)*getpid());
#else
      srandom( time(NULL)*getpid());
#endif
    }

  p = buffer = gcry_xmalloc( length );
  n = length;
#ifdef HAVE_RAND
  while ( n-- )
    *p++ = ((unsigned)(1 + (int) (256.0*rand()/(RAND_MAX+1.0)))-1);
#else
  while ( n-- )
    *p++ = ((unsigned)(1 + (int) (256.0*random()/(RAND_MAX+1.0)))-1);
#endif
  add_randomness ( buffer, length, origin );
  gcry_free (buffer);
  return 0; /* okay */
}


/* Create an unpredicable nonce of LENGTH bytes in BUFFER. */
void
_gcry_rngcsprng_create_nonce (void *buffer, size_t length)
{
  static unsigned char nonce_buffer[20+8];
  static int nonce_buffer_initialized = 0;
  static volatile pid_t my_pid; /* The volatile is there to make sure the
                                   compiler does not optimize the code away
                                   in case the getpid function is badly
                                   attributed. */
  volatile pid_t apid;
  unsigned char *p;
  size_t n;
  int err;

  /* Make sure we are initialized. */
  initialize ();

#ifdef USE_RANDOM_DAEMON
  if (allow_daemon
      && !_gcry_daemon_create_nonce (daemon_socket_name, buffer, length))
    return; /* The daemon succeeded. */
  allow_daemon = 0; /* Daemon failed - switch off. */
#endif /*USE_RANDOM_DAEMON*/

  /* Acquire the nonce buffer lock. */
  err = ath_mutex_lock (&nonce_buffer_lock);
  if (err)
    log_fatal ("failed to acquire the nonce buffer lock: %s\n",
               strerror (err));

  apid = getpid ();
  /* The first time intialize our buffer. */
  if (!nonce_buffer_initialized)
    {
      time_t atime = time (NULL);
      pid_t xpid = apid;

      my_pid = apid;

      if ((sizeof apid + sizeof atime) > sizeof nonce_buffer)
        BUG ();

      /* Initialize the first 20 bytes with a reasonable value so that
         a failure of gcry_randomize won't affect us too much.  Don't
         care about the uninitialized remaining bytes. */
      p = nonce_buffer;
      memcpy (p, &xpid, sizeof xpid);
      p += sizeof xpid;
      memcpy (p, &atime, sizeof atime); 

      /* Initialize the never changing private part of 64 bits. */
      gcry_randomize (nonce_buffer+20, 8, GCRY_WEAK_RANDOM);

      nonce_buffer_initialized = 1;
    }
  else if ( my_pid != apid )
    {
      /* We forked. Need to reseed the buffer - doing this for the
         private part should be sufficient. */
      gcry_randomize (nonce_buffer+20, 8, GCRY_WEAK_RANDOM);
      /* Update the pid so that we won't run into here again and
         again. */
      my_pid = apid;
    }

  /* Create the nonce by hashing the entire buffer, returning the hash
     and updating the first 20 bytes of the buffer with this hash. */
  for (p = buffer; length > 0; length -= n, p += n)
    {
      _gcry_sha1_hash_buffer (nonce_buffer,
                              nonce_buffer, sizeof nonce_buffer);
      n = length > 20? 20 : length;
      memcpy (p, nonce_buffer, n);
    }


  /* Release the nonce buffer lock. */
  err = ath_mutex_unlock (&nonce_buffer_lock);
  if (err)
    log_fatal ("failed to release the nonce buffer lock: %s\n",
               strerror (err));

}
