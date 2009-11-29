/* mpi-mpow.c  -  MPI functions
 *	Copyright (C) 1998, 1999, 2001, 2002, 2003 Free Software Foundation, Inc.
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

#include <config.h>
#include <stdio.h>
#include <stdlib.h>

#include "mpi-internal.h"
#include "longlong.h"
#include "g10lib.h"


/* Barrett is slower than the classical way.  It can be tweaked by
 * using partial multiplications
 */
/*#define USE_BARRETT*/



#ifdef USE_BARRETT
static void barrett_mulm( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m, gcry_mpi_t y, int k, gcry_mpi_t r1, gcry_mpi_t r2 );
static gcry_mpi_t init_barrett( gcry_mpi_t m, int *k, gcry_mpi_t *r1, gcry_mpi_t *r2 );
static int calc_barrett( gcry_mpi_t r, gcry_mpi_t x, gcry_mpi_t m, gcry_mpi_t y, int k, gcry_mpi_t r1, gcry_mpi_t r2  );
#else
#define barrett_mulm( w, u, v, m, y, k, r1, r2 ) gcry_mpi_mulm( (w), (u), (v), (m) )
#endif


static int
build_index( gcry_mpi_t *exparray, int k, int i, int t )
{
    int j, bitno;
    int idx = 0;

    bitno = t-i;
    for(j=k-1; j >= 0; j-- ) {
	idx <<= 1;
	if( mpi_test_bit( exparray[j], bitno ) )
	    idx |= 1;
    }
    /*log_debug("t=%d i=%d idx=%d\n", t, i, idx );*/
    return idx;
}

/****************
 * RES = (BASE[0] ^ EXP[0]) *  (BASE[1] ^ EXP[1]) * ... * mod M
 */
void
_gcry_mpi_mulpowm( gcry_mpi_t res, gcry_mpi_t *basearray, gcry_mpi_t *exparray, gcry_mpi_t m)
{
    int k;	/* number of elements */
    int t;	/* bit size of largest exponent */
    int i, j, idx;
    gcry_mpi_t *G;	/* table with precomputed values of size 2^k */
    gcry_mpi_t tmp;
#ifdef USE_BARRETT
    gcry_mpi_t barrett_y, barrett_r1, barrett_r2;
    int barrett_k;
#endif

    for(k=0; basearray[k]; k++ )
	;
    gcry_assert(k);
    for(t=0, i=0; (tmp=exparray[i]); i++ ) {
	/*log_mpidump("exp: ", tmp );*/
	j = mpi_get_nbits(tmp);
	if( j > t )
	    t = j;
    }
    /*log_mpidump("mod: ", m );*/
    gcry_assert (i==k);
    gcry_assert (t);
    gcry_assert (k < 10);

    G = gcry_xcalloc( (1<<k) , sizeof *G );
#ifdef USE_BARRETT
    barrett_y = init_barrett( m, &barrett_k, &barrett_r1, &barrett_r2 );
#endif
    /* and calculate */
    tmp =  mpi_alloc( mpi_get_nlimbs(m)+1 );
    mpi_set_ui( res, 1 );
    for(i = 1; i <= t; i++ ) {
	barrett_mulm(tmp, res, res, m, barrett_y, barrett_k,
				       barrett_r1, barrett_r2 );
	idx = build_index( exparray, k, i, t );
	gcry_assert (idx >= 0 && idx < (1<<k));
	if( !G[idx] ) {
	    if( !idx )
		 G[0] = mpi_alloc_set_ui( 1 );
	    else {
		for(j=0; j < k; j++ ) {
		    if( (idx & (1<<j) ) ) {
			if( !G[idx] )
			    G[idx] = mpi_copy( basearray[j] );
			else
			    barrett_mulm( G[idx], G[idx], basearray[j],
					       m, barrett_y, barrett_k, barrett_r1, barrett_r2	);
		    }
		}
		if( !G[idx] )
		    G[idx] = mpi_alloc(0);
	    }
	}
	barrett_mulm(res, tmp, G[idx], m, barrett_y, barrett_k, barrett_r1, barrett_r2	);
    }

    /* cleanup */
    mpi_free(tmp);
#ifdef USE_BARRETT
    mpi_free(barrett_y);
    mpi_free(barrett_r1);
    mpi_free(barrett_r2);
#endif
    for(i=0; i < (1<<k); i++ )
	mpi_free(G[i]);
    gcry_free(G);
}



#ifdef USE_BARRETT
static void
barrett_mulm( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m, gcry_mpi_t y, int k, gcry_mpi_t r1, gcry_mpi_t r2	)
{
    mpi_mul(w, u, v);
    if( calc_barrett( w, w, m, y, k, r1, r2 ) )
	mpi_fdiv_r( w, w, m );
}

/****************
 * Barrett precalculation: y = floor(b^(2k) / m)
 */
static gcry_mpi_t
init_barrett( gcry_mpi_t m, int *k, gcry_mpi_t *r1, gcry_mpi_t *r2 )
{
    gcry_mpi_t tmp;

    mpi_normalize( m );
    *k = mpi_get_nlimbs( m );
    tmp = mpi_alloc( *k + 1 );
    mpi_set_ui( tmp, 1 );
    mpi_lshift_limbs( tmp, 2 * *k );
    mpi_fdiv_q( tmp, tmp, m );
    *r1 = mpi_alloc( 2* *k + 1 );
    *r2 = mpi_alloc( 2* *k + 1 );
    return tmp;
}

/****************
 * Barrett reduction: We assume that these conditions are met:
 * Given x =(x_2k-1 ...x_0)_b
 *	 m =(m_k-1 ....m_0)_b	  with m_k-1 != 0
 * Output r = x mod m
 * Before using this function init_barret must be used to calucalte y and k.
 * Returns: false = no error
 *	    true = can't perform barret reduction
 */
static int
calc_barrett( gcry_mpi_t r, gcry_mpi_t x, gcry_mpi_t m, gcry_mpi_t y, int k, gcry_mpi_t r1, gcry_mpi_t r2 )
{
    int xx = k > 3 ? k-3:0;

    mpi_normalize( x );
    if( mpi_get_nlimbs(x) > 2*k )
	return 1; /* can't do it */

    /* 1. q1 = floor( x / b^k-1)
     *	  q2 = q1 * y
     *	  q3 = floor( q2 / b^k+1 )
     * Actually, we don't need qx, we can work direct on r2
     */
    mpi_set( r2, x );
    mpi_rshift_limbs( r2, k-1 );
    mpi_mul( r2, r2, y );
    mpi_rshift_limbs( r2, k+1 );

    /* 2. r1 = x mod b^k+1
     *	  r2 = q3 * m mod b^k+1
     *	  r  = r1 - r2
     * 3. if r < 0 then  r = r + b^k+1
     */
    mpi_set( r1, x );
    if( r1->nlimbs > k+1 ) /* quick modulo operation */
	r1->nlimbs = k+1;
    mpi_mul( r2, r2, m );
    if( r2->nlimbs > k+1 ) /* quick modulo operation */
	r2->nlimbs = k+1;
    mpi_sub( r, r1, r2 );

    if( mpi_is_neg( r ) ) {
	gcry_mpi_t tmp;

	tmp = mpi_alloc( k + 2 );
	mpi_set_ui( tmp, 1 );
	mpi_lshift_limbs( tmp, k+1 );
	mpi_add( r, r, tmp );
	mpi_free(tmp);
    }

    /* 4. while r >= m do r = r - m */
    while( mpi_cmp( r, m ) >= 0 )
	mpi_sub( r, r, m );

    return 0;
}
#endif /* USE_BARRETT */

