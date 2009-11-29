/* hash-common.h - Declarations of common code for hash algorithms.
 * Copyright (C) 2008 Free Software Foundation, Inc.
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

#ifndef GCRY_HASH_COMMON_H
#define GCRY_HASH_COMMON_H


const char * _gcry_hash_selftest_check_one 
/**/         (int algo, 
              int datamode, const void *data, size_t datalen,
              const void *expect, size_t expectlen);
     




#endif /*GCRY_HASH_COMMON_H*/
