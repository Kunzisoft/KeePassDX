/* mpi-mod.c -  Modular reduction
   Copyright (C) 1998, 1999, 2001, 2002, 2003,
                 2007  Free Software Foundation, Inc.

   This file is part of Libgcrypt.
  
   Libgcrypt is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
  
   Libgcrypt is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.
  
   You should have received a copy of the GNU Lesser General Public
   License along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
   USA.  */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>

#include "mpi-internal.h"
#include "longlong.h"
#include "g10lib.h"


/* Context used with Barrett reduction.  */
struct barrett_ctx_s
{
  gcry_mpi_t m;   /* The modulus - may not be modified. */
  int m_copied;   /* If true, M needs to be released.  */
  int k;
  gcry_mpi_t y; 
  gcry_mpi_t r1;  /* Helper MPI. */
  gcry_mpi_t r2;  /* Helper MPI. */
  gcry_mpi_t r3;  /* Helper MPI allocated on demand. */
};



void
_gcry_mpi_mod (gcry_mpi_t rem, gcry_mpi_t dividend, gcry_mpi_t divisor)
{
  _gcry_mpi_fdiv_r (rem, dividend, divisor);
  rem->sign = 0;
}


/* This function returns a new context for Barrett based operations on
   the modulus M.  This context needs to be released using
   _gcry_mpi_barrett_free.  If COPY is true M will be transferred to
   the context and the user may change M.  If COPY is false, M may not
   be changed until gcry_mpi_barrett_free has been called. */
mpi_barrett_t 
_gcry_mpi_barrett_init (gcry_mpi_t m, int copy)
{
  mpi_barrett_t ctx;
  gcry_mpi_t tmp;

  mpi_normalize (m);
  ctx = gcry_xcalloc (1, sizeof *ctx);

  if (copy)
    {
      ctx->m = mpi_copy (m);
      ctx->m_copied = 1;
    }
  else
    ctx->m = m;

  ctx->k = mpi_get_nlimbs (m);
  tmp = mpi_alloc (ctx->k + 1);

  /* Barrett precalculation: y = floor(b^(2k) / m). */
  mpi_set_ui (tmp, 1);
  mpi_lshift_limbs (tmp, 2 * ctx->k);
  mpi_fdiv_q (tmp, tmp, m);

  ctx->y  = tmp;
  ctx->r1 = mpi_alloc ( 2 * ctx->k + 1 );
  ctx->r2 = mpi_alloc ( 2 * ctx->k + 1 );

  return ctx;
}

void
_gcry_mpi_barrett_free (mpi_barrett_t ctx)
{
  if (ctx)
    {
      mpi_free (ctx->y);
      mpi_free (ctx->r1);
      mpi_free (ctx->r2);
      if (ctx->r3)
        mpi_free (ctx->r3);
      if (ctx->m_copied)
        mpi_free (ctx->m);
      gcry_free (ctx);
    }
}


/* R = X mod M

   Using Barrett reduction.  Before using this function
   _gcry_mpi_barrett_init must have been called to do the
   precalculations.  CTX is the context created by this precalculation
   and also conveys M.  If the Barret reduction could no be done a
   starightforward reduction method is used.

   We assume that these conditions are met:
   Input:  x =(x_2k-1 ...x_0)_b
 	   m =(m_k-1 ....m_0)_b	  with m_k-1 != 0
   Output: r = x mod m
 */
void
_gcry_mpi_mod_barrett (gcry_mpi_t r, gcry_mpi_t x, mpi_barrett_t ctx)
{
  gcry_mpi_t m = ctx->m;
  int k = ctx->k;
  gcry_mpi_t y = ctx->y;
  gcry_mpi_t r1 = ctx->r1;
  gcry_mpi_t r2 = ctx->r2;

  mpi_normalize (x);
  if (mpi_get_nlimbs (x) > 2*k )
    {
      mpi_mod (r, x, m);
      return;
    }

  /* 1. q1 = floor( x / b^k-1)
   *    q2 = q1 * y
   *    q3 = floor( q2 / b^k+1 )
   * Actually, we don't need qx, we can work direct on r2 
   */
  mpi_set ( r2, x );
  mpi_rshift_limbs ( r2, k-1 );
  mpi_mul ( r2, r2, y );
  mpi_rshift_limbs ( r2, k+1 );

  /* 2. r1 = x mod b^k+1
   *	r2 = q3 * m mod b^k+1
   *	r  = r1 - r2
   * 3. if r < 0 then  r = r + b^k+1
   */
  mpi_set ( r1, x );
  if ( r1->nlimbs > k+1 ) /* Quick modulo operation.  */
    r1->nlimbs = k+1;
  mpi_mul ( r2, r2, m );
  if ( r2->nlimbs > k+1 ) /* Quick modulo operation. */
    r2->nlimbs = k+1;
  mpi_sub ( r, r1, r2 );

  if ( mpi_is_neg( r ) )
    {
      if (!ctx->r3)
        {
          ctx->r3 = mpi_alloc ( k + 2 );
          mpi_set_ui (ctx->r3, 1);
          mpi_lshift_limbs (ctx->r3, k + 1 );
        }
      mpi_add ( r, r, ctx->r3 );
    }
  
  /* 4. while r >= m do r = r - m */
  while ( mpi_cmp( r, m ) >= 0 )
    mpi_sub ( r, r, m );

}


void
_gcry_mpi_mul_barrett (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v,
                       mpi_barrett_t ctx)
{
  gcry_mpi_mul (w, u, v);
  mpi_mod_barrett (w, w, ctx);
}

