/* rand-internal.h - header to glue the random functions
 *	Copyright (C) 1998, 2002 Free Software Foundation, Inc.
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

#ifndef G10_RAND_INTERNAL_H
#define G10_RAND_INTERNAL_H

#include "../src/cipher-proto.h"

/* Constants used to define the origin of random added to the pool.
   The code is sensitive to the order of the values.  */
enum random_origins 
  {
    RANDOM_ORIGIN_INIT = 0,      /* Used only for initialization. */
    RANDOM_ORIGIN_EXTERNAL = 1,  /* Added from an external source.  */
    RANDOM_ORIGIN_FASTPOLL = 2,  /* Fast random poll function.  */
    RANDOM_ORIGIN_SLOWPOLL = 3,  /* Slow poll function.  */
    RANDOM_ORIGIN_EXTRAPOLL = 4  /* Used to mark an extra pool seed
                                    due to a GCRY_VERY_STRONG_RANDOM
                                    random request.  */
  };



/*-- random.c --*/
void _gcry_random_progress (const char *what, int printchar,
                            int current, int total);


/*-- random-csprng.c --*/
void _gcry_rngcsprng_initialize (int full);
void _gcry_rngcsprng_dump_stats (void);
void _gcry_rngcsprng_secure_alloc (void);
void _gcry_rngcsprng_enable_quick_gen (void);
void _gcry_rngcsprng_set_daemon_socket (const char *socketname);
int  _gcry_rngcsprng_use_daemon (int onoff);
int  _gcry_rngcsprng_is_faked (void);
gcry_error_t _gcry_rngcsprng_add_bytes (const void *buf, size_t buflen,
                                        int quality);
void *_gcry_rngcsprng_get_bytes (size_t nbytes,
                                 enum gcry_random_level level);
void *_gcry_rngcsprng_get_bytes_secure (size_t nbytes, 
                                        enum gcry_random_level level);
void _gcry_rngcsprng_randomize (void *buffer, size_t length,
                                enum gcry_random_level level);
void _gcry_rngcsprng_set_seed_file (const char *name);
void _gcry_rngcsprng_update_seed_file (void);
void _gcry_rngcsprng_fast_poll (void);
void _gcry_rngcsprng_create_nonce (void *buffer, size_t length);

/*-- random-rngcsprng.c --*/
void _gcry_rngfips_initialize (int full);
void _gcry_rngfips_dump_stats (void);
int  _gcry_rngfips_is_faked (void);
gcry_error_t _gcry_rngfips_add_bytes (const void *buf, size_t buflen,
                                        int quality);
void *_gcry_rngfips_get_bytes (size_t nbytes,
                               enum gcry_random_level level);
void *_gcry_rngfips_get_bytes_secure (size_t nbytes, 
                                      enum gcry_random_level level);
void _gcry_rngfips_randomize (void *buffer, size_t length,
                                enum gcry_random_level level);
void _gcry_rngfips_create_nonce (void *buffer, size_t length);

gcry_error_t _gcry_rngfips_selftest (selftest_report_func_t report);

gcry_err_code_t _gcry_rngfips_init_external_test (void **r_context,
                                                  unsigned int flags,
                                                  const void *key,
                                                  size_t keylen,
                                                  const void *seed,
                                                  size_t seedlen,
                                                  const void *dt,
                                                  size_t dtlen);
gcry_err_code_t _gcry_rngfips_run_external_test (void *context,
                                                 char *buffer, size_t buflen);
void _gcry_rngfips_deinit_external_test (void *context);





/*-- rndlinux.c --*/
int _gcry_rndlinux_gather_random (void (*add) (const void *, size_t,
                                               enum random_origins),
                                   enum random_origins origin,
                                  size_t length, int level);

/*-- rndunix.c --*/
int _gcry_rndunix_gather_random (void (*add) (const void *, size_t,
                                              enum random_origins),
                                 enum random_origins origin,
                                 size_t length, int level);

/*-- rndelg.c --*/
int _gcry_rndegd_gather_random (void (*add) (const void *, size_t,
                                             enum random_origins),
                                enum random_origins origin,
                                size_t length, int level);
int _gcry_rndegd_connect_socket (int nofail);

/*-- rndw32.c --*/
int _gcry_rndw32_gather_random (void (*add) (const void *, size_t,
                                             enum random_origins),
                                enum random_origins origin,
                                size_t length, int level);
void _gcry_rndw32_gather_random_fast (void (*add)(const void*, size_t, 
                                                  enum random_origins),
                                      enum random_origins origin );

/*-- rndhw.c --*/
int _gcry_rndhw_failed_p (void);
void _gcry_rndhw_poll_fast (void (*add)(const void*, size_t,
                                        enum random_origins),
                            enum random_origins origin);
size_t _gcry_rndhw_poll_slow (void (*add)(const void*, size_t,
                                          enum random_origins),
                              enum random_origins origin);



#endif /*G10_RAND_INTERNAL_H*/
