/* primegen.c - prime number generator
 * Copyright (C) 1998, 2000, 2001, 2002, 2003
 *               2004, 2008 Free Software Foundation, Inc.
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
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */

#include <config.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "g10lib.h"
#include "mpi.h"
#include "cipher.h"
#include "ath.h"

static gcry_mpi_t gen_prime (unsigned int nbits, int secret, int randomlevel, 
                             int (*extra_check)(void *, gcry_mpi_t),
                             void *extra_check_arg);
static int check_prime( gcry_mpi_t prime, gcry_mpi_t val_2, int rm_rounds,
                        gcry_prime_check_func_t cb_func, void *cb_arg );
static int is_prime (gcry_mpi_t n, int steps, unsigned int *count);
static void m_out_of_n( char *array, int m, int n );

static void (*progress_cb) (void *,const char*,int,int, int );
static void *progress_cb_data;

/* Note: 2 is not included because it can be tested more easily by
   looking at bit 0. The last entry in this list is marked by a zero */
static ushort small_prime_numbers[] = {
    3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43,
    47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101,
    103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
    157, 163, 167, 173, 179, 181, 191, 193, 197, 199,
    211, 223, 227, 229, 233, 239, 241, 251, 257, 263,
    269, 271, 277, 281, 283, 293, 307, 311, 313, 317,
    331, 337, 347, 349, 353, 359, 367, 373, 379, 383,
    389, 397, 401, 409, 419, 421, 431, 433, 439, 443,
    449, 457, 461, 463, 467, 479, 487, 491, 499, 503,
    509, 521, 523, 541, 547, 557, 563, 569, 571, 577,
    587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
    643, 647, 653, 659, 661, 673, 677, 683, 691, 701,
    709, 719, 727, 733, 739, 743, 751, 757, 761, 769,
    773, 787, 797, 809, 811, 821, 823, 827, 829, 839,
    853, 857, 859, 863, 877, 881, 883, 887, 907, 911,
    919, 929, 937, 941, 947, 953, 967, 971, 977, 983,
    991, 997, 1009, 1013, 1019, 1021, 1031, 1033,
    1039, 1049, 1051, 1061, 1063, 1069, 1087, 1091,
    1093, 1097, 1103, 1109, 1117, 1123, 1129, 1151,
    1153, 1163, 1171, 1181, 1187, 1193, 1201, 1213,
    1217, 1223, 1229, 1231, 1237, 1249, 1259, 1277,
    1279, 1283, 1289, 1291, 1297, 1301, 1303, 1307,
    1319, 1321, 1327, 1361, 1367, 1373, 1381, 1399,
    1409, 1423, 1427, 1429, 1433, 1439, 1447, 1451,
    1453, 1459, 1471, 1481, 1483, 1487, 1489, 1493,
    1499, 1511, 1523, 1531, 1543, 1549, 1553, 1559,
    1567, 1571, 1579, 1583, 1597, 1601, 1607, 1609,
    1613, 1619, 1621, 1627, 1637, 1657, 1663, 1667,
    1669, 1693, 1697, 1699, 1709, 1721, 1723, 1733,
    1741, 1747, 1753, 1759, 1777, 1783, 1787, 1789,
    1801, 1811, 1823, 1831, 1847, 1861, 1867, 1871,
    1873, 1877, 1879, 1889, 1901, 1907, 1913, 1931,
    1933, 1949, 1951, 1973, 1979, 1987, 1993, 1997,
    1999, 2003, 2011, 2017, 2027, 2029, 2039, 2053,
    2063, 2069, 2081, 2083, 2087, 2089, 2099, 2111,
    2113, 2129, 2131, 2137, 2141, 2143, 2153, 2161,
    2179, 2203, 2207, 2213, 2221, 2237, 2239, 2243,
    2251, 2267, 2269, 2273, 2281, 2287, 2293, 2297,
    2309, 2311, 2333, 2339, 2341, 2347, 2351, 2357,
    2371, 2377, 2381, 2383, 2389, 2393, 2399, 2411,
    2417, 2423, 2437, 2441, 2447, 2459, 2467, 2473,
    2477, 2503, 2521, 2531, 2539, 2543, 2549, 2551,
    2557, 2579, 2591, 2593, 2609, 2617, 2621, 2633,
    2647, 2657, 2659, 2663, 2671, 2677, 2683, 2687,
    2689, 2693, 2699, 2707, 2711, 2713, 2719, 2729,
    2731, 2741, 2749, 2753, 2767, 2777, 2789, 2791,
    2797, 2801, 2803, 2819, 2833, 2837, 2843, 2851,
    2857, 2861, 2879, 2887, 2897, 2903, 2909, 2917,
    2927, 2939, 2953, 2957, 2963, 2969, 2971, 2999,
    3001, 3011, 3019, 3023, 3037, 3041, 3049, 3061,
    3067, 3079, 3083, 3089, 3109, 3119, 3121, 3137,
    3163, 3167, 3169, 3181, 3187, 3191, 3203, 3209,
    3217, 3221, 3229, 3251, 3253, 3257, 3259, 3271,
    3299, 3301, 3307, 3313, 3319, 3323, 3329, 3331,
    3343, 3347, 3359, 3361, 3371, 3373, 3389, 3391,
    3407, 3413, 3433, 3449, 3457, 3461, 3463, 3467,
    3469, 3491, 3499, 3511, 3517, 3527, 3529, 3533,
    3539, 3541, 3547, 3557, 3559, 3571, 3581, 3583,
    3593, 3607, 3613, 3617, 3623, 3631, 3637, 3643,
    3659, 3671, 3673, 3677, 3691, 3697, 3701, 3709,
    3719, 3727, 3733, 3739, 3761, 3767, 3769, 3779,
    3793, 3797, 3803, 3821, 3823, 3833, 3847, 3851,
    3853, 3863, 3877, 3881, 3889, 3907, 3911, 3917,
    3919, 3923, 3929, 3931, 3943, 3947, 3967, 3989,
    4001, 4003, 4007, 4013, 4019, 4021, 4027, 4049,
    4051, 4057, 4073, 4079, 4091, 4093, 4099, 4111,
    4127, 4129, 4133, 4139, 4153, 4157, 4159, 4177,
    4201, 4211, 4217, 4219, 4229, 4231, 4241, 4243,
    4253, 4259, 4261, 4271, 4273, 4283, 4289, 4297,
    4327, 4337, 4339, 4349, 4357, 4363, 4373, 4391,
    4397, 4409, 4421, 4423, 4441, 4447, 4451, 4457,
    4463, 4481, 4483, 4493, 4507, 4513, 4517, 4519,
    4523, 4547, 4549, 4561, 4567, 4583, 4591, 4597,
    4603, 4621, 4637, 4639, 4643, 4649, 4651, 4657,
    4663, 4673, 4679, 4691, 4703, 4721, 4723, 4729,
    4733, 4751, 4759, 4783, 4787, 4789, 4793, 4799,
    4801, 4813, 4817, 4831, 4861, 4871, 4877, 4889,
    4903, 4909, 4919, 4931, 4933, 4937, 4943, 4951,
    4957, 4967, 4969, 4973, 4987, 4993, 4999,
    0
};
static int no_of_small_prime_numbers = DIM (small_prime_numbers) - 1;



/* An object and a list to build up a global pool of primes.  See
   save_pool_prime and get_pool_prime. */
struct primepool_s 
{
  struct primepool_s *next;
  gcry_mpi_t prime;      /* If this is NULL the entry is not used. */
  unsigned int nbits;
  gcry_random_level_t randomlevel;
};
struct primepool_s *primepool;
/* Mutex used to protect access to the primepool.  */
static ath_mutex_t primepool_lock = ATH_MUTEX_INITIALIZER;



/* Save PRIME which has been generated at RANDOMLEVEL for later
   use. Needs to be called while primepool_lock is being hold.  Note
   that PRIME should be considered released after calling this
   function. */
static void
save_pool_prime (gcry_mpi_t prime, gcry_random_level_t randomlevel)
{
  struct primepool_s *item, *item2;
  size_t n;

  for (n=0, item = primepool; item; item = item->next, n++)
    if (!item->prime)
      break;
  if (!item && n > 100)
    {
      /* Remove some of the entries.  Our strategy is removing
         the last third from the list. */
      int i;
      
      for (i=0, item2 = primepool; item2; item2 = item2->next)
        {
          if (i >= n/3*2)
            {
              gcry_mpi_release (item2->prime);
              item2->prime = NULL;
              if (!item)
                item = item2;
            }
        }
    }
  if (!item)
    {
      item = gcry_calloc (1, sizeof *item);
      if (!item)
        {
          /* Out of memory.  Silently giving up. */
          gcry_mpi_release (prime);
          return; 
        }
      item->next = primepool;
      primepool = item;
    }
  item->prime = prime;
  item->nbits = mpi_get_nbits (prime);
  item->randomlevel = randomlevel;
}


/* Return a prime for the prime pool or NULL if none has been found.
   The prime needs to match NBITS and randomlevel. This function needs
   to be called why the primepool_look is being hold. */
static gcry_mpi_t
get_pool_prime (unsigned int nbits, gcry_random_level_t randomlevel)
{
  struct primepool_s *item;

  for (item = primepool; item; item = item->next)
    if (item->prime
        && item->nbits == nbits && item->randomlevel == randomlevel)
      {
        gcry_mpi_t prime = item->prime;
        item->prime = NULL;
        gcry_assert (nbits == mpi_get_nbits (prime));
        return prime;
      }
  return NULL;
}






void
_gcry_register_primegen_progress ( void (*cb)(void *,const char*,int,int,int),
                                   void *cb_data )
{
  progress_cb = cb;
  progress_cb_data = cb_data;
}


static void
progress( int c )
{
  if ( progress_cb )
    progress_cb ( progress_cb_data, "primegen", c, 0, 0 );
}


/****************
 * Generate a prime number (stored in secure memory)
 */
gcry_mpi_t
_gcry_generate_secret_prime (unsigned int nbits,
                             gcry_random_level_t random_level,
                             int (*extra_check)(void*, gcry_mpi_t),
                             void *extra_check_arg)
{
  gcry_mpi_t prime;

  prime = gen_prime (nbits, 1, random_level, extra_check, extra_check_arg);
  progress('\n');
  return prime;
}


/* Generate a prime number which may be public, i.e. not allocated in
   secure memory.  */
gcry_mpi_t
_gcry_generate_public_prime (unsigned int nbits,
                             gcry_random_level_t random_level,
                             int (*extra_check)(void*, gcry_mpi_t),
                             void *extra_check_arg)
{
  gcry_mpi_t prime;

  prime = gen_prime (nbits, 0, random_level, extra_check, extra_check_arg);
  progress('\n');
  return prime;
}


/* Core prime generation function.  The algorithm used to generate
   practically save primes is due to Lim and Lee as described in the
   CRYPTO '97 proceedings (ISBN3540633847) page 260.

   NEED_Q_FACTOR: If true make sure that at least one factor is of
                  size qbits.  This is for example required for DSA.
   PRIME_GENERATED: Adresss of a variable where the resulting prime
                    number will be stored.
   PBITS: Requested size of the prime number.  At least 48.
   QBITS: One factor of the prime needs to be of this size.  Maybe 0
          if this is not required.  See also MODE.
   G: If not NULL an MPI which will receive a generator for the prime
      for use with Elgamal.
   RET_FACTORS: if not NULL, an array with all factors are stored at
                that address.
   ALL_FACTORS: If set to true all factors of prime-1 are returned.
   RANDOMLEVEL:  How strong should the random numers be.
   FLAGS: Prime generation bit flags. Currently supported:
          GCRY_PRIME_FLAG_SECRET - The prime needs to be kept secret.
   CB_FUNC, CB_ARG:  Callback to be used for extra checks.

 */
static gcry_err_code_t
prime_generate_internal (int need_q_factor,
			 gcry_mpi_t *prime_generated, unsigned int pbits,
			 unsigned int qbits, gcry_mpi_t g,
			 gcry_mpi_t **ret_factors,
			 gcry_random_level_t randomlevel, unsigned int flags,
                         int all_factors,
                         gcry_prime_check_func_t cb_func, void *cb_arg)
{
  gcry_err_code_t err = 0;
  gcry_mpi_t *factors_new = NULL; /* Factors to return to the
				     caller.  */
  gcry_mpi_t *factors = NULL;	/* Current factors.  */
  gcry_random_level_t poolrandomlevel; /* Random level used for pool primes. */
  gcry_mpi_t *pool = NULL;	/* Pool of primes.  */
  int *pool_in_use = NULL;      /* Array with currently used POOL elements. */
  unsigned char *perms = NULL;	/* Permutations of POOL.  */
  gcry_mpi_t q_factor = NULL;	/* Used if QBITS is non-zero.  */
  unsigned int fbits = 0;	/* Length of prime factors.  */
  unsigned int n = 0;		/* Number of factors.  */
  unsigned int m = 0;		/* Number of primes in pool.  */
  gcry_mpi_t q = NULL;		/* First prime factor.  */
  gcry_mpi_t prime = NULL;	/* Prime candidate.  */
  unsigned int nprime = 0;	/* Bits of PRIME.  */
  unsigned int req_qbits;       /* The original QBITS value.  */
  gcry_mpi_t val_2;             /* For check_prime().  */
  int is_locked = 0;            /* Flag to help unlocking the primepool. */
  unsigned int is_secret = (flags & GCRY_PRIME_FLAG_SECRET);
  unsigned int count1 = 0, count2 = 0;
  unsigned int i = 0, j = 0;

  if (pbits < 48)
    return GPG_ERR_INV_ARG;

  /* We won't use a too strong random elvel for the pooled subprimes. */
  poolrandomlevel = (randomlevel > GCRY_STRONG_RANDOM?
                     GCRY_STRONG_RANDOM : randomlevel);


  /* If QBITS is not given, assume a reasonable value. */
  if (!qbits)
    qbits = pbits / 3;

  req_qbits = qbits;

  /* Find number of needed prime factors N.  */
  for (n = 1; (pbits - qbits - 1) / n  >= qbits; n++)
    ;
  n--;

  val_2 = mpi_alloc_set_ui (2);

  if ((! n) || ((need_q_factor) && (n < 2)))
    {
      err = GPG_ERR_INV_ARG;
      goto leave;
    }

  if (need_q_factor)
    {
      n--;  /* Need one factor less because we want a specific Q-FACTOR. */
      fbits = (pbits - 2 * req_qbits -1) / n;
      qbits =  pbits - req_qbits - n * fbits;
    }
  else
    {
      fbits = (pbits - req_qbits -1) / n;
      qbits = pbits - n * fbits;
    }
  
  if (DBG_CIPHER)
    log_debug ("gen prime: pbits=%u qbits=%u fbits=%u/%u n=%d\n",
               pbits, req_qbits, qbits, fbits, n);

  /* Allocate an integer to old the new prime. */
  prime = gcry_mpi_new (pbits);

  /* Generate first prime factor.  */
  q = gen_prime (qbits, is_secret, randomlevel, NULL, NULL);

  /* Generate a specific Q-Factor if requested. */
  if (need_q_factor)
    q_factor = gen_prime (req_qbits, is_secret, randomlevel, NULL, NULL);
  
  /* Allocate an array to hold all factors + 2 for later usage.  */
  factors = gcry_calloc (n + 2, sizeof (*factors));
  if (!factors)
    {
      err = gpg_err_code_from_errno (errno);
      goto leave;
    }

  /* Allocate an array to track pool usage. */
  pool_in_use = gcry_malloc (n * sizeof *pool_in_use);
  if (!pool_in_use)
    {
      err = gpg_err_code_from_errno (errno);
      goto leave;
    }
  for (i=0; i < n; i++)
    pool_in_use[i] = -1;
      
  /* Make a pool of 3n+5 primes (this is an arbitrary value).  We
     require at least 30 primes for are useful selection process. 
     
     Fixme: We need to research the best formula for sizing the pool.
  */
  m = n * 3 + 5;
  if (need_q_factor) /* Need some more in this case. */
    m += 5;
  if (m < 30)
    m = 30;
  pool = gcry_calloc (m , sizeof (*pool));
  if (! pool)
    {
      err = gpg_err_code_from_errno (errno);
      goto leave;
    }

  /* Permutate over the pool of primes until we find a prime of the
     requested length.  */
  do
    {
    next_try:
      for (i=0; i < n; i++)
        pool_in_use[i] = -1;

      if (!perms)
        {
          /* Allocate new primes.  This is done right at the beginning
             of the loop and if we have later run out of primes. */
          for (i = 0; i < m; i++)
            {
              mpi_free (pool[i]);
              pool[i] = NULL;
            }

          /* Init m_out_of_n().  */
          perms = gcry_calloc (1, m);
          if (!perms)
            {
              err = gpg_err_code_from_errno (errno);
              goto leave;
            }

          if (ath_mutex_lock (&primepool_lock))
            {
              err = GPG_ERR_INTERNAL;
              goto leave;
            }
          is_locked = 1;
          for (i = 0; i < n; i++)
            {
              perms[i] = 1; 
              /* At a maximum we use strong random for the factors.
                 This saves us a lot of entropy. Given that Q and
                 possible Q-factor are also used in the final prime
                 this should be acceptable.  We also don't allocate in
                 secure memory to save on that scare resource too.  If
                 Q has been allocated in secure memory, the final
                 prime will be saved there anyway.  This is because
                 our MPI routines take care of that.  GnuPG has worked
                 this way ever since.  */
              pool[i] = NULL;
              if (is_locked)
                {
                  pool[i] = get_pool_prime (fbits, poolrandomlevel);
                  if (!pool[i])
                    {
                      if (ath_mutex_unlock (&primepool_lock))
                        {
                          err = GPG_ERR_INTERNAL;
                          goto leave;
                        }
                      is_locked = 0;
                    }
                }
              if (!pool[i])
                pool[i] = gen_prime (fbits, 0, poolrandomlevel, NULL, NULL);
              pool_in_use[i] = i;
              factors[i] = pool[i];
            }
          if (is_locked && ath_mutex_unlock (&primepool_lock))
            {
              err = GPG_ERR_INTERNAL;
              goto leave;
            }
          is_locked = 0;
        }
      else
        {
          /* Get next permutation. */
          m_out_of_n ( (char*)perms, n, m);
          if (ath_mutex_lock (&primepool_lock))
            {
              err = GPG_ERR_INTERNAL;
              goto leave;
            }
          is_locked = 1;
          for (i = j = 0; (i < m) && (j < n); i++)
            if (perms[i])
              {
                /* If the subprime has not yet beed generated do it now. */
                if (!pool[i] && is_locked)
                  {
                    pool[i] = get_pool_prime (fbits, poolrandomlevel);
                    if (!pool[i])
                      {
                        if (ath_mutex_unlock (&primepool_lock))
                          {
                            err = GPG_ERR_INTERNAL;
                            goto leave;
                          }
                        is_locked = 0;
                      }
                  }
                if (!pool[i])
                  pool[i] = gen_prime (fbits, 0, poolrandomlevel, NULL, NULL);
                pool_in_use[j] = i;
                factors[j++] = pool[i];
              }
          if (is_locked && ath_mutex_unlock (&primepool_lock))
            {
              err = GPG_ERR_INTERNAL;
              goto leave;
            }
          is_locked = 0;
          if (i == n)
            {
              /* Ran out of permutations: Allocate new primes.  */
              gcry_free (perms);
              perms = NULL;
              progress ('!');
              goto next_try;	
            }
        }

	/* Generate next prime candidate:
	   p = 2 * q [ * q_factor] * factor_0 * factor_1 * ... * factor_n + 1. 
         */
	mpi_set (prime, q);
	mpi_mul_ui (prime, prime, 2);
	if (need_q_factor)
	  mpi_mul (prime, prime, q_factor);
	for(i = 0; i < n; i++)
	  mpi_mul (prime, prime, factors[i]);
	mpi_add_ui (prime, prime, 1);
	nprime = mpi_get_nbits (prime);

	if (nprime < pbits)
	  {
	    if (++count1 > 20)
	      {
		count1 = 0;
		qbits++;
		progress('>');
		mpi_free (q);
		q = gen_prime (qbits, is_secret, randomlevel, NULL, NULL);
		goto next_try;
	      }
	  }
	else
	  count1 = 0;
        
	if (nprime > pbits)
	  {
	    if (++count2 > 20)
	      {
		count2 = 0;
		qbits--;
		progress('<');
		mpi_free (q);
		q = gen_prime (qbits, is_secret, randomlevel, NULL, NULL);
		goto next_try;
	      }
	  }
	else
	  count2 = 0;
    }
  while (! ((nprime == pbits) && check_prime (prime, val_2, 5,
                                              cb_func, cb_arg)));

  if (DBG_CIPHER)
    {
      progress ('\n');
      log_mpidump ("prime    : ", prime);
      log_mpidump ("factor  q: ", q);
      if (need_q_factor)
        log_mpidump ("factor q0: ", q_factor);
      for (i = 0; i < n; i++)
        log_mpidump ("factor pi: ", factors[i]);
      log_debug ("bit sizes: prime=%u, q=%u",
                 mpi_get_nbits (prime), mpi_get_nbits (q));
      if (need_q_factor)
        log_debug (", q0=%u", mpi_get_nbits (q_factor));
      for (i = 0; i < n; i++)
        log_debug (", p%d=%u", i, mpi_get_nbits (factors[i]));
      progress('\n');
    }

  if (ret_factors)
    {
      /* Caller wants the factors.  */
      factors_new = gcry_calloc (n + 4, sizeof (*factors_new));
      if (! factors_new)
        {
          err = gpg_err_code_from_errno (errno);
          goto leave;
        }

      if (all_factors)
        {
          i = 0;
          factors_new[i++] = gcry_mpi_set_ui (NULL, 2);
          factors_new[i++] = mpi_copy (q);
          if (need_q_factor)
            factors_new[i++] = mpi_copy (q_factor);
          for(j=0; j < n; j++)
            factors_new[i++] = mpi_copy (factors[j]);
        }
      else
        {
          i = 0;
          if (need_q_factor)
            {
              factors_new[i++] = mpi_copy (q_factor);
              for (; i <= n; i++)
                factors_new[i] = mpi_copy (factors[i]);
            }
          else
            for (; i < n; i++ )
              factors_new[i] = mpi_copy (factors[i]);
        }
    }
  
  if (g)
    {
      /* Create a generator (start with 3).  */
      gcry_mpi_t tmp = mpi_alloc (mpi_get_nlimbs (prime));
      gcry_mpi_t b = mpi_alloc (mpi_get_nlimbs (prime));
      gcry_mpi_t pmin1 = mpi_alloc (mpi_get_nlimbs (prime));
      
      if (need_q_factor)
        err = GPG_ERR_NOT_IMPLEMENTED;
      else
        {
          factors[n] = q;
          factors[n + 1] = mpi_alloc_set_ui (2);
          mpi_sub_ui (pmin1, prime, 1);
          mpi_set_ui (g, 2);
          do
            {
              mpi_add_ui (g, g, 1);
              if (DBG_CIPHER)
                {
                  log_debug ("checking g:");
                  gcry_mpi_dump (g);
                  log_printf ("\n");
                }
              else
                progress('^');
              for (i = 0; i < n + 2; i++)
                {
                  mpi_fdiv_q (tmp, pmin1, factors[i]);
                  /* No mpi_pow(), but it is okay to use this with mod
                     prime.  */
                  gcry_mpi_powm (b, g, tmp, prime);
                  if (! mpi_cmp_ui (b, 1))
                    break;
                }
              if (DBG_CIPHER)
                progress('\n');
            } 
          while (i < n + 2);

          mpi_free (factors[n+1]);
          mpi_free (tmp);
          mpi_free (b);
          mpi_free (pmin1);
        }
    }
  
  if (! DBG_CIPHER)
    progress ('\n');


 leave:
  if (pool)
    {
      is_locked = !ath_mutex_lock (&primepool_lock);
      for(i = 0; i < m; i++)
        {
          if (pool[i])
            {
              for (j=0; j < n; j++)
                if (pool_in_use[j] == i)
                  break;
              if (j == n && is_locked)
                {
                  /* This pooled subprime has not been used. */
                  save_pool_prime (pool[i], poolrandomlevel);
                }
              else
                mpi_free (pool[i]);
            }
        }
      if (is_locked && ath_mutex_unlock (&primepool_lock))
        err = GPG_ERR_INTERNAL;
      is_locked = 0;
      gcry_free (pool);
    }
  gcry_free (pool_in_use);
  if (factors)
    gcry_free (factors);  /* Factors are shallow copies.  */
  if (perms)
    gcry_free (perms);

  mpi_free (val_2);
  mpi_free (q);
  mpi_free (q_factor);

  if (! err)
    {
      *prime_generated = prime;
      if (ret_factors)
	*ret_factors = factors_new;
    }
  else
    {
      if (factors_new)
	{
	  for (i = 0; factors_new[i]; i++)
	    mpi_free (factors_new[i]);
	  gcry_free (factors_new);
	}
      mpi_free (prime);
    }

  return err;
}


/* Generate a prime used for discrete logarithm algorithms; i.e. this
   prime will be public and no strong random is required.  */
gcry_mpi_t
_gcry_generate_elg_prime (int mode, unsigned pbits, unsigned qbits,
			  gcry_mpi_t g, gcry_mpi_t **ret_factors)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  gcry_mpi_t prime = NULL;
  
  err = prime_generate_internal ((mode == 1), &prime, pbits, qbits, g,
				 ret_factors, GCRY_WEAK_RANDOM, 0, 0,
                                 NULL, NULL);

  return prime;
}


static gcry_mpi_t
gen_prime (unsigned int nbits, int secret, int randomlevel, 
           int (*extra_check)(void *, gcry_mpi_t), void *extra_check_arg)
{
  gcry_mpi_t prime, ptest, pminus1, val_2, val_3, result;
  int i;
  unsigned int x, step;
  unsigned int count1, count2;
  int *mods;
  
/*   if (  DBG_CIPHER ) */
/*     log_debug ("generate a prime of %u bits ", nbits ); */

  if (nbits < 16)
    log_fatal ("can't generate a prime with less than %d bits\n", 16);

  mods = gcry_xmalloc( no_of_small_prime_numbers * sizeof *mods );
  /* Make nbits fit into gcry_mpi_t implementation. */
  val_2  = mpi_alloc_set_ui( 2 );
  val_3 = mpi_alloc_set_ui( 3);
  prime  = secret? gcry_mpi_snew ( nbits ): gcry_mpi_new ( nbits );
  result = mpi_alloc_like( prime );
  pminus1= mpi_alloc_like( prime );
  ptest  = mpi_alloc_like( prime );
  count1 = count2 = 0;
  for (;;)
    {  /* try forvever */
      int dotcount=0;
      
      /* generate a random number */
      gcry_mpi_randomize( prime, nbits, randomlevel );
      
      /* Set high order bit to 1, set low order bit to 1.  If we are
         generating a secret prime we are most probably doing that
         for RSA, to make sure that the modulus does have the
         requested key size we set the 2 high order bits. */
      mpi_set_highbit (prime, nbits-1);
      if (secret)
        mpi_set_bit (prime, nbits-2);
      mpi_set_bit(prime, 0);
      
      /* Calculate all remainders. */
      for (i=0; (x = small_prime_numbers[i]); i++ )
        mods[i] = mpi_fdiv_r_ui(NULL, prime, x);
      
      /* Now try some primes starting with prime. */
      for(step=0; step < 20000; step += 2 ) 
        {
          /* Check against all the small primes we have in mods. */
          count1++;
          for (i=0; (x = small_prime_numbers[i]); i++ ) 
            {
              while ( mods[i] + step >= x )
                mods[i] -= x;
              if ( !(mods[i] + step) )
                break;
	    }
          if ( x )
            continue;   /* Found a multiple of an already known prime. */
          
          mpi_add_ui( ptest, prime, step );

          /* Do a fast Fermat test now. */
          count2++;
          mpi_sub_ui( pminus1, ptest, 1);
          gcry_mpi_powm( result, val_2, pminus1, ptest );
          if ( !mpi_cmp_ui( result, 1 ) )
            { 
              /* Not composite, perform stronger tests */
              if (is_prime(ptest, 5, &count2 ))
                {
                  if (!mpi_test_bit( ptest, nbits-1-secret ))
                    {
                      progress('\n');
                      log_debug ("overflow in prime generation\n");
                      break; /* Stop loop, continue with a new prime. */
                    }

                  if (extra_check && extra_check (extra_check_arg, ptest))
                    { 
                      /* The extra check told us that this prime is
                         not of the caller's taste. */
                      progress ('/');
                    }
                  else
                    { 
                      /* Got it. */
                      mpi_free(val_2);
                      mpi_free(val_3);
                      mpi_free(result);
                      mpi_free(pminus1);
                      mpi_free(prime);
                      gcry_free(mods);
                      return ptest; 
                    }
                }
	    }
          if (++dotcount == 10 )
            {
              progress('.');
              dotcount = 0;
	    }
	}
      progress(':'); /* restart with a new random value */
    }
}

/****************
 * Returns: true if this may be a prime
 * RM_ROUNDS gives the number of Rabin-Miller tests to run.
 */
static int
check_prime( gcry_mpi_t prime, gcry_mpi_t val_2, int rm_rounds,
             gcry_prime_check_func_t cb_func, void *cb_arg)
{
  int i;
  unsigned int x;
  unsigned int count=0;

  /* Check against small primes. */
  for (i=0; (x = small_prime_numbers[i]); i++ )
    {
      if ( mpi_divisible_ui( prime, x ) )
        return 0;
    }

  /* A quick Fermat test. */
  {
    gcry_mpi_t result = mpi_alloc_like( prime );
    gcry_mpi_t pminus1 = mpi_alloc_like( prime );
    mpi_sub_ui( pminus1, prime, 1);
    gcry_mpi_powm( result, val_2, pminus1, prime );
    mpi_free( pminus1 );
    if ( mpi_cmp_ui( result, 1 ) )
      { 
        /* Is composite. */
        mpi_free( result );
        progress('.');
        return 0;
      }
    mpi_free( result );
  }

  if (!cb_func || cb_func (cb_arg, GCRY_PRIME_CHECK_AT_MAYBE_PRIME, prime))
    {
      /* Perform stronger tests. */
      if ( is_prime( prime, rm_rounds, &count ) )
        {
          if (!cb_func
              || cb_func (cb_arg, GCRY_PRIME_CHECK_AT_GOT_PRIME, prime))
            return 1; /* Probably a prime. */
        }
    }
  progress('.');
  return 0;
}


/*
 * Return true if n is probably a prime
 */
static int
is_prime (gcry_mpi_t n, int steps, unsigned int *count)
{
  gcry_mpi_t x = mpi_alloc( mpi_get_nlimbs( n ) );
  gcry_mpi_t y = mpi_alloc( mpi_get_nlimbs( n ) );
  gcry_mpi_t z = mpi_alloc( mpi_get_nlimbs( n ) );
  gcry_mpi_t nminus1 = mpi_alloc( mpi_get_nlimbs( n ) );
  gcry_mpi_t a2 = mpi_alloc_set_ui( 2 );
  gcry_mpi_t q;
  unsigned i, j, k;
  int rc = 0;
  unsigned nbits = mpi_get_nbits( n );

  if (steps < 5) /* Make sure that we do at least 5 rounds. */
    steps = 5; 

  mpi_sub_ui( nminus1, n, 1 );

  /* Find q and k, so that n = 1 + 2^k * q . */
  q = mpi_copy ( nminus1 );
  k = mpi_trailing_zeros ( q );
  mpi_tdiv_q_2exp (q, q, k);

  for (i=0 ; i < steps; i++ )
    {
      ++*count;
      if( !i )
        {
          mpi_set_ui( x, 2 );
        }
      else
        {
          gcry_mpi_randomize( x, nbits, GCRY_WEAK_RANDOM );

          /* Make sure that the number is smaller than the prime and
             keep the randomness of the high bit. */
          if ( mpi_test_bit ( x, nbits-2) )
            {
              mpi_set_highbit ( x, nbits-2); /* Clear all higher bits. */
            }
          else
            {
              mpi_set_highbit( x, nbits-2 );
              mpi_clear_bit( x, nbits-2 );
            }
          gcry_assert (mpi_cmp (x, nminus1) < 0 && mpi_cmp_ui (x, 1) > 0);
	}
      gcry_mpi_powm ( y, x, q, n);
      if ( mpi_cmp_ui(y, 1) && mpi_cmp( y, nminus1 ) )
        {
          for ( j=1; j < k && mpi_cmp( y, nminus1 ); j++ )
            {
              gcry_mpi_powm(y, y, a2, n);
              if( !mpi_cmp_ui( y, 1 ) )
                goto leave; /* Not a prime. */
            }
          if (mpi_cmp( y, nminus1 ) )
            goto leave; /* Not a prime. */
	}
      progress('+');
    }
  rc = 1; /* May be a prime. */

 leave:
  mpi_free( x );
  mpi_free( y );
  mpi_free( z );
  mpi_free( nminus1 );
  mpi_free( q );
  mpi_free( a2 );

  return rc;
}


/* Given ARRAY of size N with M elements set to true produce a
   modified array with the next permutation of M elements.  Note, that
   ARRAY is used in a one-bit-per-byte approach.  To detected the last
   permutation it is useful to intialize the array with the first M
   element set to true and use this test:
       m_out_of_n (array, m, n);
       for (i = j = 0; i < n && j < m; i++)
         if (array[i])
           j++;
       if (j == m)
         goto ready;
     
   This code is based on the algorithm 452 from the "Collected
   Algorithms From ACM, Volume II" by C. N. Liu and D. T. Tang.
*/
static void
m_out_of_n ( char *array, int m, int n )
{
  int i=0, i1=0, j=0, jp=0,  j1=0, k1=0, k2=0;

  if( !m || m >= n )
    return;

  /* Need to handle this simple case separately. */
  if( m == 1 )
    { 
      for (i=0; i < n; i++ )
        {
          if ( array[i] )
            {
              array[i++] = 0;
              if( i >= n )
                i = 0;
              array[i] = 1;
              return;
            }
        }
      BUG();
    }


  for (j=1; j < n; j++ )
    {
      if ( array[n-1] == array[n-j-1])
        continue;
      j1 = j;
      break;
    }

  if ( (m & 1) )
    {
      /* M is odd. */
      if( array[n-1] )
        {
          if( j1 & 1 )
            {
              k1 = n - j1;
              k2 = k1+2;
              if( k2 > n )
                k2 = n;
              goto leave;
            }
          goto scan;
        }
      k2 = n - j1 - 1;
      if( k2 == 0 )
        {
          k1 = i;
          k2 = n - j1;
        }
      else if( array[k2] && array[k2-1] )
        k1 = n;
      else
        k1 = k2 + 1;
    }
  else 
    {
      /* M is even. */
      if( !array[n-1] )
        {
          k1 = n - j1;
          k2 = k1 + 1;
          goto leave;
        }
        
      if( !(j1 & 1) )
        {
          k1 = n - j1;
          k2 = k1+2;
          if( k2 > n )
            k2 = n;
          goto leave;
        }
    scan:
      jp = n - j1 - 1;
      for (i=1; i <= jp; i++ ) 
        {
          i1 = jp + 2 - i;
          if( array[i1-1]  )
            {
              if( array[i1-2] )
                {
                  k1 = i1 - 1;
                  k2 = n - j1;
		}
              else
                {
                  k1 = i1 - 1;
                  k2 = n + 1 - j1;
                }
              goto leave;
            }
        }
      k1 = 1;
      k2 = n + 1 - m;
    }
 leave:
  /* Now complement the two selected bits. */
  array[k1-1] = !array[k1-1];
  array[k2-1] = !array[k2-1];
}


/* Generate a new prime number of PRIME_BITS bits and store it in
   PRIME.  If FACTOR_BITS is non-zero, one of the prime factors of
   (prime - 1) / 2 must be FACTOR_BITS bits long.  If FACTORS is
   non-zero, allocate a new, NULL-terminated array holding the prime
   factors and store it in FACTORS.  FLAGS might be used to influence
   the prime number generation process.  */
gcry_error_t
gcry_prime_generate (gcry_mpi_t *prime, unsigned int prime_bits,
		     unsigned int factor_bits, gcry_mpi_t **factors,
		     gcry_prime_check_func_t cb_func, void *cb_arg,
		     gcry_random_level_t random_level,
		     unsigned int flags)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  gcry_mpi_t *factors_generated = NULL;
  gcry_mpi_t prime_generated = NULL;
  unsigned int mode = 0;

  if (!prime)
    return gpg_error (GPG_ERR_INV_ARG);
  *prime = NULL; 

  if (flags & GCRY_PRIME_FLAG_SPECIAL_FACTOR)
    mode = 1;

  /* Generate.  */
  err = prime_generate_internal ((mode==1), &prime_generated, prime_bits,
				 factor_bits, NULL,
                                 factors? &factors_generated : NULL,
				 random_level, flags, 1,
                                 cb_func, cb_arg);

  if (! err)
    if (cb_func)
      {
	/* Additional check. */
	if ( !cb_func (cb_arg, GCRY_PRIME_CHECK_AT_FINISH, prime_generated))
	  {
	    /* Failed, deallocate resources.  */
	    unsigned int i;

	    mpi_free (prime_generated);
            if (factors)
              {
                for (i = 0; factors_generated[i]; i++)
                  mpi_free (factors_generated[i]);
                gcry_free (factors_generated);
              }
	    err = GPG_ERR_GENERAL; 
	  }
      }

  if (! err)
    {
      if (factors)
        *factors = factors_generated;
      *prime = prime_generated;
    }

  return gcry_error (err);
}

/* Check wether the number X is prime.  */
gcry_error_t
gcry_prime_check (gcry_mpi_t x, unsigned int flags)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  gcry_mpi_t val_2 = mpi_alloc_set_ui (2); /* Used by the Fermat test. */

  (void)flags;

  /* We use 64 rounds because the prime we are going to test is not
     guaranteed to be a random one. */
  if (! check_prime (x, val_2, 64, NULL, NULL))
    err = GPG_ERR_NO_PRIME;

  mpi_free (val_2);

  return gcry_error (err);
}

/* Find a generator for PRIME where the factorization of (prime-1) is
   in the NULL terminated array FACTORS. Return the generator as a
   newly allocated MPI in R_G.  If START_G is not NULL, use this as s
   atart for the search. Returns 0 on success.*/
gcry_error_t
gcry_prime_group_generator (gcry_mpi_t *r_g,
                            gcry_mpi_t prime, gcry_mpi_t *factors,
                            gcry_mpi_t start_g)
{
  gcry_mpi_t tmp = gcry_mpi_new (0);
  gcry_mpi_t b = gcry_mpi_new (0);
  gcry_mpi_t pmin1 = gcry_mpi_new (0);
  gcry_mpi_t g = start_g? gcry_mpi_copy (start_g) : gcry_mpi_set_ui (NULL, 3);
  int first = 1;
  int i, n;

  if (!factors || !r_g || !prime)
    return gpg_error (GPG_ERR_INV_ARG);
  *r_g = NULL; 

  for (n=0; factors[n]; n++)
    ;
  if (n < 2)
    return gpg_error (GPG_ERR_INV_ARG);

  /* Extra sanity check - usually disabled. */  
/*   mpi_set (tmp, factors[0]); */
/*   for(i = 1; i < n; i++) */
/*     mpi_mul (tmp, tmp, factors[i]); */
/*   mpi_add_ui (tmp, tmp, 1); */
/*   if (mpi_cmp (prime, tmp)) */
/*     return gpg_error (GPG_ERR_INV_ARG); */
  
  gcry_mpi_sub_ui (pmin1, prime, 1);      
  do         
    {
      if (first)
        first = 0;
      else
        gcry_mpi_add_ui (g, g, 1);
      
      if (DBG_CIPHER)
        {
          log_debug ("checking g:");
          gcry_mpi_dump (g);
          log_debug ("\n");
        }
      else
        progress('^');
      
      for (i = 0; i < n; i++)
        {
          mpi_fdiv_q (tmp, pmin1, factors[i]);
          gcry_mpi_powm (b, g, tmp, prime);
          if (! mpi_cmp_ui (b, 1))
            break;
        }
      if (DBG_CIPHER)
        progress('\n');
    }
  while (i < n);
  
  gcry_mpi_release (tmp);
  gcry_mpi_release (b); 
  gcry_mpi_release (pmin1); 
  *r_g = g; 

  return 0; 
}

/* Convenience function to release the factors array. */
void
gcry_prime_release_factors (gcry_mpi_t *factors)
{
  if (factors)
    {
      int i;
      
      for (i=0; factors[i]; i++)
        mpi_free (factors[i]);
      gcry_free (factors);
    }
}



/* Helper for _gcry_derive_x931_prime.  */
static gcry_mpi_t
find_x931_prime (const gcry_mpi_t pfirst)
{
  gcry_mpi_t val_2 = mpi_alloc_set_ui (2); 
  gcry_mpi_t prime;
  
  prime = gcry_mpi_copy (pfirst);
  /* If P is even add 1.  */ 
  mpi_set_bit (prime, 0);

  /* We use 64 Rabin-Miller rounds which is better and thus
     sufficient.  We do not have a Lucas test implementaion thus we
     can't do it in the X9.31 preferred way of running a few
     Rabin-Miller followed by one Lucas test.  */
  while ( !check_prime (prime, val_2, 64, NULL, NULL) )
    mpi_add_ui (prime, prime, 2);

  mpi_free (val_2);

  return prime;
}


/* Generate a prime using the algorithm from X9.31 appendix B.4. 

   This function requires that the provided public exponent E is odd.
   XP, XP1 and XP2 are the seed values.  All values are mandatory.

   On success the prime is returned.  If R_P1 or R_P2 are given the
   internal values P1 and P2 are saved at these addresses.  On error
   NULL is returned.  */
gcry_mpi_t
_gcry_derive_x931_prime (const gcry_mpi_t xp, 
                         const gcry_mpi_t xp1, const gcry_mpi_t xp2,
                         const gcry_mpi_t e,
                         gcry_mpi_t *r_p1, gcry_mpi_t *r_p2)
{
  gcry_mpi_t p1, p2, p1p2, yp0;

  if (!xp || !xp1 || !xp2)
    return NULL;
  if (!e || !mpi_test_bit (e, 0))
    return NULL;  /* We support only odd values for E.  */

  p1 = find_x931_prime (xp1);
  p2 = find_x931_prime (xp2);
  p1p2 = mpi_alloc_like (xp);
  mpi_mul (p1p2, p1, p2);

  {
    gcry_mpi_t r1, tmp;
  
    /* r1 = (p2^{-1} mod p1)p2 - (p1^{-1} mod p2) */
    tmp = mpi_alloc_like (p1);
    mpi_invm (tmp, p2, p1);
    mpi_mul (tmp, tmp, p2);
    r1 = tmp;
    
    tmp = mpi_alloc_like (p2);
    mpi_invm (tmp, p1, p2);
    mpi_mul (tmp, tmp, p1);
    mpi_sub (r1, r1, tmp);

    /* Fixup a negative value.  */
    if (mpi_is_neg (r1)) 
      mpi_add (r1, r1, p1p2);

    /* yp0 = xp + (r1 - xp mod p1*p2)  */
    yp0 = tmp; tmp = NULL;
    mpi_subm (yp0, r1, xp, p1p2);
    mpi_add (yp0, yp0, xp);
    mpi_free (r1);

    /* Fixup a negative value.  */
    if (mpi_cmp (yp0, xp) < 0 ) 
      mpi_add (yp0, yp0, p1p2);
  }

  /* yp0 is now the first integer greater than xp with p1 being a
     large prime factor of yp0-1 and p2 a large prime factor of yp0+1.  */

  /* Note that the first example from X9.31 (D.1.1) which uses
       (Xq1 #1A5CF72EE770DE50CB09ACCEA9#)
       (Xq2 #134E4CAA16D2350A21D775C404#)
       (Xq  #CC1092495D867E64065DEE3E7955F2EBC7D47A2D
             7C9953388F97DDDC3E1CA19C35CA659EDC2FC325
             6D29C2627479C086A699A49C4C9CEE7EF7BD1B34
             321DE34A#))))
     returns an yp0 of
            #CC1092495D867E64065DEE3E7955F2EBC7D47A2D
             7C9953388F97DDDC3E1CA19C35CA659EDC2FC4E3
             BF20CB896EE37E098A906313271422162CB6C642
             75C1201F#
     and not
            #CC1092495D867E64065DEE3E7955F2EBC7D47A2D
             7C9953388F97DDDC3E1CA19C35CA659EDC2FC2E6
             C88FE299D52D78BE405A97E01FD71DD7819ECB91
             FA85A076#
     as stated in the standard.  This seems to be a bug in X9.31.
   */

  {
    gcry_mpi_t val_2 = mpi_alloc_set_ui (2); 
    gcry_mpi_t gcdtmp = mpi_alloc_like (yp0);
    int gcdres;
  
    mpi_sub_ui (p1p2, p1p2, 1); /* Adjust for loop body.  */
    mpi_sub_ui (yp0, yp0, 1);   /* Ditto.  */
    for (;;)
      {
        gcdres = gcry_mpi_gcd (gcdtmp, e, yp0);
        mpi_add_ui (yp0, yp0, 1);
        if (!gcdres)
          progress ('/');  /* gcd (e, yp0-1) != 1  */
        else if (check_prime (yp0, val_2, 64, NULL, NULL))
          break; /* Found.  */
        /* We add p1p2-1 because yp0 is incremented after the gcd test.  */
        mpi_add (yp0, yp0, p1p2);
      }
    mpi_free (gcdtmp);
    mpi_free (val_2);
  }

  mpi_free (p1p2);

  progress('\n');
  if (r_p1)
    *r_p1 = p1;
  else
    mpi_free (p1);
  if (r_p2)
    *r_p2 = p2;
  else
    mpi_free (p2);
  return yp0;
}



/* Generate the two prime used for DSA using the algorithm specified
   in FIPS 186-2.  PBITS is the desired length of the prime P and a
   QBITS the length of the prime Q.  If SEED is not supplied and
   SEEDLEN is 0 the function generates an appropriate SEED.  On
   success the generated primes are stored at R_Q and R_P, the counter
   value is stored at R_COUNTER and the seed actually used for
   generation is stored at R_SEED and R_SEEDVALUE.  */
gpg_err_code_t
_gcry_generate_fips186_2_prime (unsigned int pbits, unsigned int qbits,
                                const void *seed, size_t seedlen,
                                gcry_mpi_t *r_q, gcry_mpi_t *r_p,
                                int *r_counter,
                                void **r_seed, size_t *r_seedlen)
{
  gpg_err_code_t ec;
  unsigned char seed_help_buffer[160/8];  /* Used to hold a generated SEED. */
  unsigned char *seed_plus;     /* Malloced buffer to hold SEED+x.  */
  unsigned char digest[160/8];  /* Helper buffer for SHA-1 digest.  */
  gcry_mpi_t val_2 = NULL;      /* Helper for the prime test.  */
  gcry_mpi_t tmpval = NULL;     /* Helper variable.  */
  int i;

  unsigned char value_u[160/8];
  int value_n, value_b, value_k;
  int counter;
  gcry_mpi_t value_w = NULL;
  gcry_mpi_t value_x = NULL;
  gcry_mpi_t prime_q = NULL;
  gcry_mpi_t prime_p = NULL;

  /* FIPS 186-2 allows only for 1024/160 bit.  */
  if (pbits != 1024 || qbits != 160)
    return GPG_ERR_INV_KEYLEN;

  if (!seed && !seedlen)
    ; /* No seed value given:  We are asked to generate it.  */
  else if (!seed || seedlen < qbits/8)
    return GPG_ERR_INV_ARG;
  
  /* Allocate a buffer to later compute SEED+some_increment. */
  seed_plus = gcry_malloc (seedlen < 20? 20:seedlen);
  if (!seed_plus)
    {
      ec = gpg_err_code_from_syserror ();
      goto leave;
    }

  val_2   = mpi_alloc_set_ui (2);
  value_n = (pbits - 1) / qbits;
  value_b = (pbits - 1) - value_n * qbits;
  value_w = gcry_mpi_new (pbits);
  value_x = gcry_mpi_new (pbits);

 restart:  
  /* Generate Q.  */
  for (;;)
    {
      /* Step 1: Generate a (new) seed unless one has been supplied.  */
      if (!seed)
        {
          seedlen = sizeof seed_help_buffer;
          gcry_create_nonce (seed_help_buffer, seedlen);
          seed = seed_help_buffer;
        }
      
      /* Step 2: U = sha1(seed) ^ sha1((seed+1) mod 2^{qbits})  */
      memcpy (seed_plus, seed, seedlen);
      for (i=seedlen-1; i >= 0; i--)
        {
          seed_plus[i]++;
          if (seed_plus[i])
            break;
        }
      gcry_md_hash_buffer (GCRY_MD_SHA1, value_u, seed, seedlen);
      gcry_md_hash_buffer (GCRY_MD_SHA1, digest, seed_plus, seedlen);
      for (i=0; i < sizeof value_u; i++)
        value_u[i] ^= digest[i];
  
      /* Step 3:  Form q from U  */
      gcry_mpi_release (prime_q); prime_q = NULL;
      ec = gpg_err_code (gcry_mpi_scan (&prime_q, GCRYMPI_FMT_USG, 
                                        value_u, sizeof value_u, NULL));
      if (ec)
        goto leave;
      mpi_set_highbit (prime_q, qbits-1 );
      mpi_set_bit (prime_q, 0);
      
      /* Step 4:  Test whether Q is prime using 64 round of Rabin-Miller.  */
      if (check_prime (prime_q, val_2, 64, NULL, NULL))
        break; /* Yes, Q is prime.  */

      /* Step 5.  */
      seed = NULL;  /* Force a new seed at Step 1.  */
    }
  
  /* Step 6.  Note that we do no use an explicit offset but increment
     SEED_PLUS accordingly.  SEED_PLUS is currently SEED+1.  */
  counter = 0;

  /* Generate P. */
  prime_p = gcry_mpi_new (pbits);
  for (;;)
    {
      /* Step 7: For k = 0,...n let 
                   V_k = sha1(seed+offset+k) mod 2^{qbits}  
         Step 8: W = V_0 + V_1*2^160 + 
                         ... 
                         + V_{n-1}*2^{(n-1)*160}
                         + (V_{n} mod 2^b)*2^{n*160}                
       */
      mpi_set_ui (value_w, 0);
      for (value_k=0; value_k <= value_n; value_k++)
        {
          /* There is no need to have an explicit offset variable:  In
             the first round we shall have an offset of 2, this is
             achieved by using SEED_PLUS which is already at SEED+1,
             thus we just need to increment it once again.  The
             requirement for the next round is to update offset by N,
             which we implictly did at the end of this loop, and then
             to add one; this one is the same as in the first round.  */
          for (i=seedlen-1; i >= 0; i--)
            {
              seed_plus[i]++;
              if (seed_plus[i])
                break;
            }
          gcry_md_hash_buffer (GCRY_MD_SHA1, digest, seed_plus, seedlen);
          
          gcry_mpi_release (tmpval); tmpval = NULL;
          ec = gpg_err_code (gcry_mpi_scan (&tmpval, GCRYMPI_FMT_USG,
                                            digest, sizeof digest, NULL));
          if (ec)
            goto leave;
          if (value_k == value_n)
            mpi_clear_highbit (tmpval, value_b); /* (V_n mod 2^b) */
          mpi_lshift (tmpval, tmpval, value_k*qbits);
          mpi_add (value_w, value_w, tmpval);
        }

      /* Step 8 continued: X = W + 2^{L-1}  */
      mpi_set_ui (value_x, 0);
      mpi_set_highbit (value_x, pbits-1);
      mpi_add (value_x, value_x, value_w);

      /* Step 9:  c = X mod 2q,  p = X - (c - 1)  */
      mpi_mul_2exp (tmpval, prime_q, 1);
      mpi_mod (tmpval, value_x, tmpval);
      mpi_sub_ui (tmpval, tmpval, 1);
      mpi_sub (prime_p, value_x, tmpval);

      /* Step 10: If  p < 2^{L-1}  skip the primality test.  */
      /* Step 11 and 12: Primality test.  */
      if (mpi_get_nbits (prime_p) >= pbits-1
          && check_prime (prime_p, val_2, 64, NULL, NULL) )
        break; /* Yes, P is prime, continue with Step 15.  */

      /* Step 13: counter = counter + 1, offset = offset + n + 1. */
      counter++;

      /* Step 14: If counter >= 2^12  goto Step 1.  */
      if (counter >= 4096)
        goto restart;
    }

  /* Step 15:  Save p, q, counter and seed.  */
/*   log_debug ("fips186-2 pbits p=%u q=%u counter=%d\n", */
/*              mpi_get_nbits (prime_p), mpi_get_nbits (prime_q), counter); */
/*   log_printhex("fips186-2 seed:", seed, seedlen); */
/*   log_mpidump ("fips186-2 prime p", prime_p); */
/*   log_mpidump ("fips186-2 prime q", prime_q); */
  if (r_q)
    {
      *r_q = prime_q;
      prime_q = NULL;
    }
  if (r_p)
    {
      *r_p = prime_p;
      prime_p = NULL;
    }
  if (r_counter)
    *r_counter = counter;
  if (r_seed && r_seedlen)
    {
      memcpy (seed_plus, seed, seedlen);
      *r_seed = seed_plus;
      seed_plus = NULL;
      *r_seedlen = seedlen;
    }


 leave:
  gcry_mpi_release (tmpval);
  gcry_mpi_release (value_x);
  gcry_mpi_release (value_w);
  gcry_mpi_release (prime_p);
  gcry_mpi_release (prime_q);
  gcry_free (seed_plus);
  gcry_mpi_release (val_2);
  return ec;
}



/* WARNING: The code below has not yet been tested!  However, it is
   not yet used.  We need to wait for FIPS 186-3 final and for test
   vectors.

   Generate the two prime used for DSA using the algorithm specified
   in FIPS 186-3, A.1.1.2.  PBITS is the desired length of the prime P
   and a QBITS the length of the prime Q.  If SEED is not supplied and
   SEEDLEN is 0 the function generates an appropriate SEED.  On
   success the generated primes are stored at R_Q and R_P, the counter
   value is stored at R_COUNTER and the seed actually used for
   generation is stored at R_SEED and R_SEEDVALUE.  The hash algorithm
   used is stored at R_HASHALGO.
   
   Note that this function is very similar to the fips186_2 code.  Due
   to the minor differences, other buffer sizes and for documentarion,
   we use a separate function.
*/
gpg_err_code_t
_gcry_generate_fips186_3_prime (unsigned int pbits, unsigned int qbits,
                                const void *seed, size_t seedlen,
                                gcry_mpi_t *r_q, gcry_mpi_t *r_p,
                                int *r_counter,
                                void **r_seed, size_t *r_seedlen,
                                int *r_hashalgo)
{
  gpg_err_code_t ec;
  unsigned char seed_help_buffer[256/8];  /* Used to hold a generated SEED. */
  unsigned char *seed_plus;     /* Malloced buffer to hold SEED+x.  */
  unsigned char digest[256/8];  /* Helper buffer for SHA-1 digest.  */
  gcry_mpi_t val_2 = NULL;      /* Helper for the prime test.  */
  gcry_mpi_t tmpval = NULL;     /* Helper variable.  */
  int hashalgo;                 /* The id of the Approved Hash Function.  */
  int i;
  
  unsigned char value_u[256/8];
  int value_n, value_b, value_j;
  int counter;
  gcry_mpi_t value_w = NULL;
  gcry_mpi_t value_x = NULL;
  gcry_mpi_t prime_q = NULL;
  gcry_mpi_t prime_p = NULL;

  gcry_assert (sizeof seed_help_buffer == sizeof digest
               && sizeof seed_help_buffer == sizeof value_u);

  /* Step 1:  Check the requested prime lengths.  */
  /* Note that due to the size of our buffers QBITS is limited to 256.  */
  if (pbits == 1024 && qbits == 160)
    hashalgo = GCRY_MD_SHA1;
  else if (pbits == 2048 && qbits == 224)
    hashalgo = GCRY_MD_SHA224;
  else if (pbits == 2048 && qbits == 256)
    hashalgo = GCRY_MD_SHA256;
  else if (pbits == 3072 && qbits == 256)
    hashalgo = GCRY_MD_SHA256;
  else
    return GPG_ERR_INV_KEYLEN;

  /* Also check that the hash algorithm is available.  */
  ec = gpg_err_code (gcry_md_test_algo (hashalgo));
  if (ec)
    return ec;
  gcry_assert (qbits/8 <= sizeof digest);
  gcry_assert (gcry_md_get_algo_dlen (hashalgo) == qbits/8);


  /* Step 2:  Check seedlen.  */
  if (!seed && !seedlen)
    ; /* No seed value given:  We are asked to generate it.  */
  else if (!seed || seedlen < qbits/8)
    return GPG_ERR_INV_ARG;
  
  /* Allocate a buffer to later compute SEED+some_increment and a few
     helper variables.  */
  seed_plus = gcry_malloc (seedlen < sizeof seed_help_buffer? 
                           sizeof seed_help_buffer : seedlen);
  if (!seed_plus)
    {
      ec = gpg_err_code_from_syserror ();
      goto leave;
    }
  val_2   = mpi_alloc_set_ui (2);
  value_w = gcry_mpi_new (pbits);
  value_x = gcry_mpi_new (pbits);

  /* Step 3: n = \lceil L / outlen \rceil - 1  */
  value_n = (pbits + qbits - 1) / qbits - 1;
  /* Step 4: b = L - 1 - (n * outlen)  */
  value_b = pbits - 1 - (value_n * qbits);

 restart:  
  /* Generate Q.  */
  for (;;)
    {
      /* Step 5:  Generate a (new) seed unless one has been supplied.  */
      if (!seed)
        {
          seedlen = qbits/8;
          gcry_assert (seedlen <= sizeof seed_help_buffer);
          gcry_create_nonce (seed_help_buffer, seedlen);
          seed = seed_help_buffer;
        }
      
      /* Step 6:  U = hash(seed)  */
      gcry_md_hash_buffer (hashalgo, value_u, seed, seedlen);

      /* Step 7:  q = 2^{N-1} + U + 1 - (U mod 2)  */
      if ( !(value_u[qbits/8-1] & 0x01) )
        {
          for (i=qbits/8-1; i >= 0; i--)
            {
              value_u[i]++;
              if (value_u[i])
                break;
            }
        }
      gcry_mpi_release (prime_q); prime_q = NULL;
      ec = gpg_err_code (gcry_mpi_scan (&prime_q, GCRYMPI_FMT_USG, 
                                        value_u, sizeof value_u, NULL));
      if (ec)
        goto leave;
      mpi_set_highbit (prime_q, qbits-1 );
      
      /* Step 8:  Test whether Q is prime using 64 round of Rabin-Miller.
                  According to table C.1 this is sufficient for all
                  supported prime sizes (i.e. up 3072/256).  */
      if (check_prime (prime_q, val_2, 64, NULL, NULL))
        break; /* Yes, Q is prime.  */

      /* Step 8.  */
      seed = NULL;  /* Force a new seed at Step 5.  */
    }
  
  /* Step 11.  Note that we do no use an explicit offset but increment
     SEED_PLUS accordingly.  */
  memcpy (seed_plus, seed, seedlen);
  counter = 0;

  /* Generate P. */
  prime_p = gcry_mpi_new (pbits);
  for (;;)
    {
      /* Step 11.1: For j = 0,...n let 
                      V_j = hash(seed+offset+j)  
         Step 11.2: W = V_0 + V_1*2^outlen + 
                            ... 
                            + V_{n-1}*2^{(n-1)*outlen}
                            + (V_{n} mod 2^b)*2^{n*outlen}                
       */
      mpi_set_ui (value_w, 0);
      for (value_j=0; value_j <= value_n; value_j++)
        {
          /* There is no need to have an explicit offset variable: In
             the first round we shall have an offset of 1 and a j of
             0.  This is achieved by incrementing SEED_PLUS here.  For
             the next round offset is implicitly updated by using
             SEED_PLUS again.  */
          for (i=seedlen-1; i >= 0; i--)
            {
              seed_plus[i]++;
              if (seed_plus[i])
                break;
            }
          gcry_md_hash_buffer (GCRY_MD_SHA1, digest, seed_plus, seedlen);
          
          gcry_mpi_release (tmpval); tmpval = NULL;
          ec = gpg_err_code (gcry_mpi_scan (&tmpval, GCRYMPI_FMT_USG,
                                            digest, sizeof digest, NULL));
          if (ec)
            goto leave;
          if (value_j == value_n)
            mpi_clear_highbit (tmpval, value_b); /* (V_n mod 2^b) */
          mpi_lshift (tmpval, tmpval, value_j*qbits);
          mpi_add (value_w, value_w, tmpval);
        }

      /* Step 11.3: X = W + 2^{L-1}  */
      mpi_set_ui (value_x, 0);
      mpi_set_highbit (value_x, pbits-1);
      mpi_add (value_x, value_x, value_w);

      /* Step 11.4:  c = X mod 2q  */
      mpi_mul_2exp (tmpval, prime_q, 1);
      mpi_mod (tmpval, value_x, tmpval);

      /* Step 11.5:  p = X - (c - 1)  */
      mpi_sub_ui (tmpval, tmpval, 1);
      mpi_sub (prime_p, value_x, tmpval);

      /* Step 11.6: If  p < 2^{L-1}  skip the primality test.  */
      /* Step 11.7 and 11.8: Primality test.  */
      if (mpi_get_nbits (prime_p) >= pbits-1
          && check_prime (prime_p, val_2, 64, NULL, NULL) )
        break; /* Yes, P is prime, continue with Step 15.  */
      
      /* Step 11.9: counter = counter + 1, offset = offset + n + 1.
                    If counter >= 4L  goto Step 5.  */
      counter++;
      if (counter >= 4*pbits)
        goto restart;
    }

  /* Step 12:  Save p, q, counter and seed.  */
  log_debug ("fips186-3 pbits p=%u q=%u counter=%d\n",
             mpi_get_nbits (prime_p), mpi_get_nbits (prime_q), counter);
  log_printhex("fips186-3 seed:", seed, seedlen);
  log_mpidump ("fips186-3 prime p", prime_p);
  log_mpidump ("fips186-3 prime q", prime_q);
  if (r_q)
    {
      *r_q = prime_q;
      prime_q = NULL;
    }
  if (r_p)
    {
      *r_p = prime_p;
      prime_p = NULL;
    }
  if (r_counter)
    *r_counter = counter;
  if (r_seed && r_seedlen)
    {
      memcpy (seed_plus, seed, seedlen);
      *r_seed = seed_plus;
      seed_plus = NULL;
      *r_seedlen = seedlen;
    }
  if (r_hashalgo)
    *r_hashalgo = hashalgo;

 leave:
  gcry_mpi_release (tmpval);
  gcry_mpi_release (value_x);
  gcry_mpi_release (value_w);
  gcry_mpi_release (prime_p);
  gcry_mpi_release (prime_q);
  gcry_free (seed_plus);
  gcry_mpi_release (val_2);
  return ec;
}

