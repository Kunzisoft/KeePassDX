/* stdmem.c  -	private memory allocator
 * Copyright (C) 1998, 2000, 2002, 2005, 2008 Free Software Foundation, Inc.
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

/*
 * Description of the layered memory management in Libgcrypt:
 *
 *                                  [User]
 *                                    |
 *                                    |
 *                                   \ /
 *                   global.c: [MM entrance points]   -----> [user callbacks]
 *                               |          |    
 *                               |          |    
 *                              \ /        \ /
 *
 *      stdmem.c: [non-secure handlers] [secure handlers]
 *
 *                               |         |
 *                               |         |
 *                              \ /       \ /
 *
 *                  stdmem.c: [ memory guard ]
 *
 *                               |         |
 *                               |         |
 *                              \ /       \ /
 *
 *           libc: [ MM functions ]     secmem.c: [ secure MM functions]
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include "g10lib.h"
#include "stdmem.h"
#include "secmem.h"



#define MAGIC_NOR_BYTE 0x55
#define MAGIC_SEC_BYTE 0xcc
#define MAGIC_END_BYTE 0xaa

#if SIZEOF_UNSIGNED_LONG == 8
#define EXTRA_ALIGN 4
#else
#define EXTRA_ALIGN 0
#endif


static int use_m_guard = 0;

/****************
 * Warning: Never use this function after any of the functions
 * here have been used.
 */
void
_gcry_private_enable_m_guard (void)
{
  use_m_guard = 1;
}


/*
 * Allocate memory of size n.
 * Return NULL if we are out of memory.
 */
void *
_gcry_private_malloc (size_t n)
{
  if (!n) 
    return NULL; /* Allocating 0 bytes is undefined - we better return
                    an error to detect such coding errors.  */
  if (use_m_guard) 
    {
      char *p;
      
      if ( !(p = malloc (n + EXTRA_ALIGN+5)) )
        return NULL;
      ((byte*)p)[EXTRA_ALIGN+0] = n;
      ((byte*)p)[EXTRA_ALIGN+1] = n >> 8 ;
      ((byte*)p)[EXTRA_ALIGN+2] = n >> 16 ;
      ((byte*)p)[EXTRA_ALIGN+3] = MAGIC_NOR_BYTE;
      p[4+EXTRA_ALIGN+n] = MAGIC_END_BYTE;
      return p+EXTRA_ALIGN+4;
    }
  else 
    {
      return malloc( n );
    }
}


/*
 * Allocate memory of size N from the secure memory pool.  Return NULL
 * if we are out of memory.
 */
void *
_gcry_private_malloc_secure (size_t n)
{
  if (!n) 
    return NULL; /* Allocating 0 bytes is undefined - better return an
                    error to detect such coding errors.  */
  if (use_m_guard) 
    {
      char *p;
      
      if ( !(p = _gcry_secmem_malloc (n +EXTRA_ALIGN+ 5)) )
        return NULL;
      ((byte*)p)[EXTRA_ALIGN+0] = n;
      ((byte*)p)[EXTRA_ALIGN+1] = n >> 8 ;
      ((byte*)p)[EXTRA_ALIGN+2] = n >> 16 ;
      ((byte*)p)[EXTRA_ALIGN+3] = MAGIC_SEC_BYTE;
      p[4+EXTRA_ALIGN+n] = MAGIC_END_BYTE;
      return p+EXTRA_ALIGN+4;
    }
  else
    {
      return _gcry_secmem_malloc( n );
    }
}


/*
 * Realloc and clear the old space
 * Return NULL if there is not enough memory.
 */
void *
_gcry_private_realloc ( void *a, size_t n )
{
  if (use_m_guard)
    {
      unsigned char *p = a;
      char *b;
      size_t len;
      
      if (!a)
        return _gcry_private_malloc(n);
        
      _gcry_private_check_heap(p);
      len  = p[-4];
      len |= p[-3] << 8;
      len |= p[-2] << 16;
      if( len >= n ) /* We don't shrink for now. */
        return a;
      if (p[-1] == MAGIC_SEC_BYTE)
        b = _gcry_private_malloc_secure(n);
      else
        b = _gcry_private_malloc(n);
      if (!b)
        return NULL;
      memcpy (b, a, len);
      memset (b+len, 0, n-len);
      _gcry_private_free (p);
      return b;
    }
  else if ( _gcry_private_is_secure(a) )
    {
      return _gcry_secmem_realloc( a, n );
    }
  else
    {
      return realloc( a, n );
    }
}


void
_gcry_private_check_heap (const void *a)
{
  if (use_m_guard)
    {
      const byte *p = a;
      size_t len;
      
      if (!p)
        return;
      
      if ( !(p[-1] == MAGIC_NOR_BYTE || p[-1] == MAGIC_SEC_BYTE) )
        _gcry_log_fatal ("memory at %p corrupted (underflow=%02x)\n", p, p[-1]);
      len  = p[-4];
      len |= p[-3] << 8;
      len |= p[-2] << 16;
      if ( p[len] != MAGIC_END_BYTE )
        _gcry_log_fatal ("memory at %p corrupted (overflow=%02x)\n", p, p[-1]);
    }
}


/*
 * Free a memory block allocated by this or the secmem module
 */
void
_gcry_private_free (void *a)
{
  unsigned char *p = a;

  if (!p)
    return;
  if (use_m_guard )
    {
      _gcry_private_check_heap(p);
      if ( _gcry_private_is_secure(a) )
        _gcry_secmem_free(p-EXTRA_ALIGN-4);
      else
        {
          free(p-EXTRA_ALIGN-4);
	}
    }
  else if ( _gcry_private_is_secure(a) )
    _gcry_secmem_free(p);
  else
    free(p);
}


