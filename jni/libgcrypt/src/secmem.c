/* secmem.c  -	memory allocation from a secure heap
 * Copyright (C) 1998, 1999, 2000, 2001, 2002,
 *               2003, 2007 Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
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

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <unistd.h>
#include <stddef.h>

#if defined(HAVE_MLOCK) || defined(HAVE_MMAP)
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>
#ifdef USE_CAPABILITIES
#include <sys/capability.h>
#endif
#endif

#include "ath.h"
#include "g10lib.h"
#include "secmem.h"

#if defined (MAP_ANON) && ! defined (MAP_ANONYMOUS)
#define MAP_ANONYMOUS MAP_ANON
#endif

#define MINIMUM_POOL_SIZE 16384
#define STANDARD_POOL_SIZE 32768
#define DEFAULT_PAGE_SIZE 4096

typedef struct memblock
{
  unsigned size;		/* Size of the memory available to the
				   user.  */
  int flags;			/* See below.  */
  PROPERLY_ALIGNED_TYPE aligned;
} memblock_t;

/* This flag specifies that the memory block is in use.  */
#define MB_FLAG_ACTIVE (1 << 0)

/* The pool of secure memory.  */
static void *pool;

/* Size of POOL in bytes.  */
static size_t pool_size;

/* True, if the memory pool is ready for use.  May be checked in an
   atexit function.  */
static volatile int pool_okay;

/* True, if the memory pool is mmapped.  */
static volatile int pool_is_mmapped;

/* FIXME?  */
static int disable_secmem;
static int show_warning;
static int not_locked;
static int no_warning;
static int suspend_warning;

/* Stats.  */
static unsigned int cur_alloced, cur_blocks;

/* Lock protecting accesses to the memory pool.  */
static ath_mutex_t secmem_lock;

/* Convenient macros.  */
#define SECMEM_LOCK   ath_mutex_lock   (&secmem_lock)
#define SECMEM_UNLOCK ath_mutex_unlock (&secmem_lock)

/* The size of the memblock structure; this does not include the
   memory that is available to the user.  */
#define BLOCK_HEAD_SIZE \
  offsetof (memblock_t, aligned)

/* Convert an address into the according memory block structure.  */
#define ADDR_TO_BLOCK(addr) \
  (memblock_t *) ((char *) addr - BLOCK_HEAD_SIZE)

/* Check whether P points into the pool.  */
static int
ptr_into_pool_p (const void *p)
{
  /* We need to convert pointers to addresses.  This is required by
     C-99 6.5.8 to avoid undefined behaviour.  Using size_t is at
     least only implementation defined.  See also
     http://lists.gnupg.org/pipermail/gcrypt-devel/2007-February/001102.html
  */
  size_t p_addr = (size_t)p;
  size_t pool_addr = (size_t)pool;

  return p_addr >= pool_addr && p_addr <  pool_addr+pool_size;
}

/* Update the stats.  */
static void
stats_update (size_t add, size_t sub)
{
  if (add)
    {
      cur_alloced += add;
      cur_blocks++;
    }
  if (sub)
    {
      cur_alloced -= sub;
      cur_blocks--;
    }
}

/* Return the block following MB or NULL, if MB is the last block.  */
static memblock_t *
mb_get_next (memblock_t *mb)
{
  memblock_t *mb_next;

  mb_next = (memblock_t *) ((char *) mb + BLOCK_HEAD_SIZE + mb->size);
  
  if (! ptr_into_pool_p (mb_next))
    mb_next = NULL;

  return mb_next;
}

/* Return the block preceeding MB or NULL, if MB is the first
   block.  */
static memblock_t *
mb_get_prev (memblock_t *mb)
{
  memblock_t *mb_prev, *mb_next;

  if (mb == pool)
    mb_prev = NULL;
  else
    {
      mb_prev = (memblock_t *) pool;
      while (1)
	{
	  mb_next = mb_get_next (mb_prev);
	  if (mb_next == mb)
	    break;
	  else
	    mb_prev = mb_next;
	}
    }

  return mb_prev;
}

/* If the preceeding block of MB and/or the following block of MB
   exist and are not active, merge them to form a bigger block.  */
static void
mb_merge (memblock_t *mb)
{
  memblock_t *mb_prev, *mb_next;

  mb_prev = mb_get_prev (mb);
  mb_next = mb_get_next (mb);

  if (mb_prev && (! (mb_prev->flags & MB_FLAG_ACTIVE)))
    {
      mb_prev->size += BLOCK_HEAD_SIZE + mb->size;
      mb = mb_prev;
    }
  if (mb_next && (! (mb_next->flags & MB_FLAG_ACTIVE)))
    mb->size += BLOCK_HEAD_SIZE + mb_next->size;
}

/* Return a new block, which can hold SIZE bytes.  */
static memblock_t *
mb_get_new (memblock_t *block, size_t size)
{
  memblock_t *mb, *mb_split;
  
  for (mb = block; ptr_into_pool_p (mb); mb = mb_get_next (mb))
    if (! (mb->flags & MB_FLAG_ACTIVE) && mb->size >= size)
      {
	/* Found a free block.  */
	mb->flags |= MB_FLAG_ACTIVE;

	if (mb->size - size > BLOCK_HEAD_SIZE)
	  {
	    /* Split block.  */
	  
	    mb_split = (memblock_t *) (((char *) mb) + BLOCK_HEAD_SIZE + size);
	    mb_split->size = mb->size - size - BLOCK_HEAD_SIZE;
	    mb_split->flags = 0;

	    mb->size = size;

	    mb_merge (mb_split);

	  }

	break;
      }

  if (! ptr_into_pool_p (mb))
    mb = NULL;

  return mb;
}

/* Print a warning message.  */
static void
print_warn (void)
{
  if (!no_warning)
    log_info (_("Warning: using insecure memory!\n"));
}

/* Lock the memory pages into core and drop privileges.  */
static void
lock_pool (void *p, size_t n)
{
#if defined(USE_CAPABILITIES) && defined(HAVE_MLOCK)
  int err;

  cap_set_proc (cap_from_text ("cap_ipc_lock+ep"));
  err = mlock (p, n);
  if (err && errno)
    err = errno;
  cap_set_proc (cap_from_text ("cap_ipc_lock+p"));

  if (err)
    {
      if (errno != EPERM
#ifdef EAGAIN	/* OpenBSD returns this */
	  && errno != EAGAIN
#endif
#ifdef ENOSYS	/* Some SCOs return this (function not implemented) */
	  && errno != ENOSYS
#endif
#ifdef ENOMEM  /* Linux might return this. */
            && errno != ENOMEM
#endif
	  )
	log_error ("can't lock memory: %s\n", strerror (err));
      show_warning = 1;
      not_locked = 1;
    }

#elif defined(HAVE_MLOCK)
  uid_t uid;
  int err;

  uid = getuid ();

#ifdef HAVE_BROKEN_MLOCK
  /* Under HP/UX mlock segfaults if called by non-root.  Note, we have
     noch checked whether mlock does really work under AIX where we
     also detected a broken nlock.  Note further, that using plock ()
     is not a good idea under AIX. */ 
  if (uid)
    {
      errno = EPERM;
      err = errno;
    }
  else
    {
      err = mlock (p, n);
      if (err && errno)
	err = errno;
    }
#else /* !HAVE_BROKEN_MLOCK */
  err = mlock (p, n);
  if (err && errno)
    err = errno;
#endif /* !HAVE_BROKEN_MLOCK */

  if (uid && ! geteuid ())
    {
      /* check that we really dropped the privs.
       * Note: setuid(0) should always fail */
      if (setuid (uid) || getuid () != geteuid () || !setuid (0))
	log_fatal ("failed to reset uid: %s\n", strerror (errno));
    }

  if (err)
    {
      if (errno != EPERM
#ifdef EAGAIN	/* OpenBSD returns this. */
	  && errno != EAGAIN
#endif
#ifdef ENOSYS	/* Some SCOs return this (function not implemented). */
	  && errno != ENOSYS
#endif
#ifdef ENOMEM  /* Linux might return this. */
            && errno != ENOMEM
#endif
	  )
	log_error ("can't lock memory: %s\n", strerror (err));
      show_warning = 1;
      not_locked = 1;
    }

#elif defined ( __QNX__ )
  /* QNX does not page at all, so the whole secure memory stuff does
   * not make much sense.  However it is still of use because it
   * wipes out the memory on a free().
   * Therefore it is sufficient to suppress the warning
   */
#elif defined (HAVE_DOSISH_SYSTEM) || defined (__CYGWIN__)
    /* It does not make sense to print such a warning, given the fact that 
     * this whole Windows !@#$% and their user base are inherently insecure
     */
#elif defined (__riscos__)
    /* no virtual memory on RISC OS, so no pages are swapped to disc,
     * besides we don't have mmap, so we don't use it! ;-)
     * But don't complain, as explained above.
     */
#else
  log_info ("Please note that you don't have secure memory on this system\n");
#endif
}

/* Initialize POOL.  */
static void
init_pool (size_t n)
{
  size_t pgsize;
  long int pgsize_val;
  memblock_t *mb;

  pool_size = n;

  if (disable_secmem)
    log_bug ("secure memory is disabled");

#if defined(HAVE_SYSCONF) && defined(_SC_PAGESIZE)
  pgsize_val = sysconf (_SC_PAGESIZE);
#elif defined(HAVE_GETPAGESIZE)
  pgsize_val = getpagesize ();
#else
  pgsize_val = -1;
#endif
  pgsize = (pgsize_val != -1 && pgsize_val > 0)? pgsize_val:DEFAULT_PAGE_SIZE;


#if HAVE_MMAP
  pool_size = (pool_size + pgsize - 1) & ~(pgsize - 1);
#ifdef MAP_ANONYMOUS
  pool = mmap (0, pool_size, PROT_READ | PROT_WRITE,
	       MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
#else /* map /dev/zero instead */
  {
    int fd;

    fd = open ("/dev/zero", O_RDWR);
    if (fd == -1)
      {
	log_error ("can't open /dev/zero: %s\n", strerror (errno));
	pool = (void *) -1;
      }
    else
      {
	pool = mmap (0, pool_size, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0);
        close (fd);
      }
  }
#endif
  if (pool == (void *) -1)
    log_info ("can't mmap pool of %u bytes: %s - using malloc\n",
	      (unsigned) pool_size, strerror (errno));
  else
    {
      pool_is_mmapped = 1;
      pool_okay = 1;
    }

#endif
  if (!pool_okay)
    {
      pool = malloc (pool_size);
      if (!pool)
	log_fatal ("can't allocate memory pool of %u bytes\n",
		   (unsigned) pool_size);
      else
	pool_okay = 1;
    }

  /* Initialize first memory block.  */
  mb = (memblock_t *) pool;
  mb->size = pool_size;
  mb->flags = 0;
}

void
_gcry_secmem_set_flags (unsigned flags)
{
  int was_susp;

  SECMEM_LOCK;

  was_susp = suspend_warning;
  no_warning = flags & GCRY_SECMEM_FLAG_NO_WARNING;
  suspend_warning = flags & GCRY_SECMEM_FLAG_SUSPEND_WARNING;

  /* and now issue the warning if it is not longer suspended */
  if (was_susp && !suspend_warning && show_warning)
    {
      show_warning = 0;
      print_warn ();
    }

  SECMEM_UNLOCK;
}

unsigned int
_gcry_secmem_get_flags (void)
{
  unsigned flags;

  SECMEM_LOCK;

  flags = no_warning ? GCRY_SECMEM_FLAG_NO_WARNING : 0;
  flags |= suspend_warning ? GCRY_SECMEM_FLAG_SUSPEND_WARNING : 0;
  flags |= not_locked ? GCRY_SECMEM_FLAG_NOT_LOCKED : 0;

  SECMEM_UNLOCK;

  return flags;
}


/* See _gcry_secmem_init.  This function is expected to be called with
   the secmem lock held. */
static void
secmem_init (size_t n)
{
  if (!n)
    {
#ifdef USE_CAPABILITIES
      /* drop all capabilities */
      cap_set_proc (cap_from_text ("all-eip"));

#elif !defined(HAVE_DOSISH_SYSTEM)
      uid_t uid;

      disable_secmem = 1;
      uid = getuid ();
      if (uid != geteuid ())
	{
	  if (setuid (uid) || getuid () != geteuid () || !setuid (0))
	    log_fatal ("failed to drop setuid\n");
	}
#endif
    }
  else
    {
      if (n < MINIMUM_POOL_SIZE)
	n = MINIMUM_POOL_SIZE;
      if (! pool_okay)
	{
	  init_pool (n);
	  lock_pool (pool, n);
	}
      else
	log_error ("Oops, secure memory pool already initialized\n");
    }
}



/* Initialize the secure memory system.  If running with the necessary
   privileges, the secure memory pool will be locked into the core in
   order to prevent page-outs of the data.  Furthermore allocated
   secure memory will be wiped out when released.  */
void
_gcry_secmem_init (size_t n)
{
  SECMEM_LOCK;

  secmem_init (n);

  SECMEM_UNLOCK;
}


static void *
_gcry_secmem_malloc_internal (size_t size)
{
  memblock_t *mb;

  if (!pool_okay)
    {
      /* Try to initialize the pool if the user forgot about it.  */
      secmem_init (STANDARD_POOL_SIZE);
      if (!pool_okay)
        {
          log_info (_("operation is not possible without "
                      "initialized secure memory\n"));
          return NULL;
        }
    }
  if (not_locked && fips_mode ())
    {
      log_info (_("secure memory pool is not locked while in FIPS mode\n"));
      return NULL;
    }
  if (show_warning && !suspend_warning)
    {
      show_warning = 0;
      print_warn ();
    }

  /* Blocks are always a multiple of 32. */
  size = ((size + 31) / 32) * 32;

  mb = mb_get_new ((memblock_t *) pool, size);
  if (mb)
    stats_update (size, 0);

  return mb ? &mb->aligned.c : NULL;
}

void *
_gcry_secmem_malloc (size_t size)
{
  void *p;

  SECMEM_LOCK;
  p = _gcry_secmem_malloc_internal (size);
  SECMEM_UNLOCK;
  
  return p;
}

static void
_gcry_secmem_free_internal (void *a)
{
  memblock_t *mb;
  int size;

  if (!a)
    return;

  mb = ADDR_TO_BLOCK (a);
  size = mb->size;

  /* This does not make much sense: probably this memory is held in the
   * cache. We do it anyway: */
#define MB_WIPE_OUT(byte) \
  wipememory2 ((memblock_t *) ((char *) mb + BLOCK_HEAD_SIZE), (byte), size);

  MB_WIPE_OUT (0xff);
  MB_WIPE_OUT (0xaa);
  MB_WIPE_OUT (0x55);
  MB_WIPE_OUT (0x00);

  stats_update (0, size);

  mb->flags &= ~MB_FLAG_ACTIVE;

  /* Update stats.  */

  mb_merge (mb);
}

/* Wipe out and release memory.  */
void
_gcry_secmem_free (void *a)
{
  SECMEM_LOCK;
  _gcry_secmem_free_internal (a);
  SECMEM_UNLOCK;
}

/* Realloc memory.  */
void *
_gcry_secmem_realloc (void *p, size_t newsize)
{
  memblock_t *mb;
  size_t size;
  void *a;

  SECMEM_LOCK;

  mb = (memblock_t *) ((char *) p - ((size_t) &((memblock_t *) 0)->aligned.c));
  size = mb->size;
  if (newsize < size)
    {
      /* It is easier to not shrink the memory.  */
      a = p;
    }
  else
    {
      a = _gcry_secmem_malloc_internal (newsize);
      if (a)
	{
	  memcpy (a, p, size);
	  memset ((char *) a + size, 0, newsize - size);
	  _gcry_secmem_free_internal (p);
	}
    }

  SECMEM_UNLOCK;

  return a;
}


/* Return true if P points into the secure memory area.  */
int
_gcry_private_is_secure (const void *p)
{
  return pool_okay && ptr_into_pool_p (p);
}


/****************
 * Warning:  This code might be called by an interrupt handler
 *	     and frankly, there should really be such a handler,
 *	     to make sure that the memory is wiped out.
 *	     We hope that the OS wipes out mlocked memory after
 *	     receiving a SIGKILL - it really should do so, otherwise
 *	     there is no chance to get the secure memory cleaned.
 */
void
_gcry_secmem_term ()
{
  if (!pool_okay)
    return;

  wipememory2 (pool, 0xff, pool_size);
  wipememory2 (pool, 0xaa, pool_size);
  wipememory2 (pool, 0x55, pool_size);
  wipememory2 (pool, 0x00, pool_size);
#if HAVE_MMAP
  if (pool_is_mmapped)
    munmap (pool, pool_size);
#endif
  pool = NULL;
  pool_okay = 0;
  pool_size = 0;
  not_locked = 0;
}


void
_gcry_secmem_dump_stats ()
{
#if 1 
  SECMEM_LOCK;

 if (pool_okay)
    log_info ("secmem usage: %u/%lu bytes in %u blocks\n",
	      cur_alloced, (unsigned long)pool_size, cur_blocks);
  SECMEM_UNLOCK;
#else
  memblock_t *mb;
  int i;

  SECMEM_LOCK;

  for (i = 0, mb = (memblock_t *) pool;
       ptr_into_pool_p (mb);
       mb = mb_get_next (mb), i++)
    log_info ("SECMEM: [%s] block: %i; size: %i\n",
	      (mb->flags & MB_FLAG_ACTIVE) ? "used" : "free",
	      i,
	      mb->size);
  SECMEM_UNLOCK;
#endif
}
