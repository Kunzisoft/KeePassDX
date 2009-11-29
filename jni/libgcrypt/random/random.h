/* random.h - random functions
 *	Copyright (C) 1998, 2002, 2006 Free Software Foundation, Inc.
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
#ifndef G10_RANDOM_H
#define G10_RANDOM_H

#include "types.h"

/*-- random.c --*/
void _gcry_register_random_progress (void (*cb)(void *,const char*,int,int,int),
                                     void *cb_data );

void _gcry_random_initialize (int full);
void _gcry_random_dump_stats(void);
void _gcry_secure_random_alloc(void);
void _gcry_enable_quick_random_gen (void);
int  _gcry_random_is_faked(void);
void _gcry_set_random_daemon_socket (const char *socketname);
int  _gcry_use_random_daemon (int onoff);
void _gcry_set_random_seed_file (const char *name);
void _gcry_update_random_seed_file (void);

byte *_gcry_get_random_bits( size_t nbits, int level, int secure );
void _gcry_fast_random_poll( void );

gcry_err_code_t _gcry_random_init_external_test (void **r_context, 
                                                 unsigned int flags,
                                                 const void *key,
                                                 size_t keylen,
                                                 const void *seed,
                                                 size_t seedlen,
                                                 const void *dt, 
                                                 size_t dtlen);
gcry_err_code_t _gcry_random_run_external_test (void *context,
                                                char *buffer, size_t buflen);
void            _gcry_random_deinit_external_test (void *context);


/*-- rndegd.c --*/
gpg_error_t _gcry_rndegd_set_socket_name (const char *name);

/*-- random-daemon.c (only used from random.c) --*/
#ifdef USE_RANDOM_DAEMON
void _gcry_daemon_initialize_basics (void);
int _gcry_daemon_randomize (const char *socketname,
                            void *buffer, size_t length,
                            enum gcry_random_level level);
int _gcry_daemon_create_nonce (const char *socketname,
                               void *buffer, size_t length);
#endif /*USE_RANDOM_DAEMON*/

#endif /*G10_RANDOM_H*/




