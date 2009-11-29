/* rndhw.c  - Access to the external random daemon
 * Copyright (C) 2007  Free Software Foundation, Inc.
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

#include <config.h>
#include <stdio.h>
#include <stdlib.h>

#include "types.h"
#include "g10lib.h"
#include "rand-internal.h"

#undef USE_PADLOCK
#ifdef ENABLE_PADLOCK_SUPPORT
# if defined (__i386__) && SIZEOF_UNSIGNED_LONG == 4 && defined (__GNUC__)
# define USE_PADLOCK
# endif
#endif /*ENABLE_PADLOCK_SUPPORT*/

/* Keep track on whether the RNG has problems.  */
static volatile int rng_failed;


#ifdef USE_PADLOCK
static size_t
poll_padlock (void (*add)(const void*, size_t, enum random_origins),
              enum random_origins origin, int fast)
{
  char buffer[64+8] __attribute__ ((aligned (8)));
  char *p;
  unsigned int nbytes, status;
  
  /* Peter Gutmann's cryptlib tests again whether the RNG is enabled
     but we don't do so.  We would have to do this also for our AES
     implementaion and that is definitely too time consuming.  There
     would be a race condition anyway.  Thus we assume that the OS
     does not change the Padlock initialization while a user process
     is running.  */
  p = buffer;
  nbytes = 0;
  while (nbytes < 64)
    {
      asm volatile 
        ("movl %1, %%edi\n\t"         /* Set buffer.  */
         "xorl %%edx, %%edx\n\t"      /* Request up to 8 bytes.  */
         ".byte 0x0f, 0xa7, 0xc0\n\t" /* XSTORE RNG. */
         "movl %%eax, %0\n"           /* Return the status.  */
         : "=g" (status)
         : "g" (p)
         : "%edx", "%edi", "cc"
         );
      if ((status & (1<<6))         /* RNG still enabled.  */
          && !(status & (1<<13))    /* von Neumann corrector is enabled.  */
          && !(status & (1<<14))    /* String filter is disabled.  */
          && !(status & 0x1c00)     /* BIAS voltage at default.  */
          && (!(status & 0x1f) || (status & 0x1f) == 8) /* Sanity check.  */
          )
        {
          nbytes += (status & 0x1f);
          if (fast)
            break; /* Don't get into the loop with the fast flag set.  */
          p += (status & 0x1f);
        }
      else 
        {
          /* If there was an error we need to break the loop and
             record that there is something wrong with the padlock
             RNG.  */
          rng_failed = 1;
          break; 
        }
    }

  if (nbytes)
    {
      (*add) (buffer, nbytes, origin);
      wipememory (buffer, nbytes);
    }
  return nbytes;
}
#endif /*USE_PADLOCK*/


int
_gcry_rndhw_failed_p (void)
{
  return rng_failed;
}


/* Try to read random from a hardware RNG if a fast one is
   available.  */
void
_gcry_rndhw_poll_fast (void (*add)(const void*, size_t, enum random_origins),
                       enum random_origins origin)
{
  (void)add;
  (void)origin;

#ifdef USE_PADLOCK
  if ((_gcry_get_hw_features () & HWF_PADLOCK_RNG))
    poll_padlock (add, origin, 1);
#endif  
}


/* Read 64 bytes from a hardware RNG and return the number of bytes
   actually read.  */
size_t
_gcry_rndhw_poll_slow (void (*add)(const void*, size_t, enum random_origins),
                       enum random_origins origin)
{
  size_t nbytes = 0;

  (void)add;
  (void)origin;

#ifdef USE_PADLOCK
  if ((_gcry_get_hw_features () & HWF_PADLOCK_RNG))
    nbytes += poll_padlock (add, origin, 0);
#endif  

  return nbytes;
}
