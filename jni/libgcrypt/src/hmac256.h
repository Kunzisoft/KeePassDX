/* hmac256.h - Declarations for _gcry_hmac256
 *	Copyright (C) 2008 Free Software Foundation, Inc.
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

#ifndef HMAC256_H
#define HMAC256_H


struct hmac256_context;
typedef struct hmac256_context *hmac256_context_t;

hmac256_context_t _gcry_hmac256_new (const void *key, size_t keylen);
void _gcry_hmac256_update (hmac256_context_t hd, const void *buf, size_t len);
const void *_gcry_hmac256_finalize (hmac256_context_t hd, size_t *r_dlen);
void _gcry_hmac256_release (hmac256_context_t hd);

int _gcry_hmac256_file (void *result, size_t resultsize, const char *filename,
                        const void *key, size_t keylen);


#endif /*HMAC256_H*/
