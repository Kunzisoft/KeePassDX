/* mpi-mul.c  -  MPI functions
 * Copyright (C) 1994, 1996, 1998, 2001, 2002, 2003 Free Software Foundation, Inc.
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
 *
 * Note: This code is heavily based on the GNU MP Library.
 *	 Actually it's the same code with only minor changes in the
 *	 way the data is stored; this is to support the abstraction
 *	 of an optional secure memory allocation which may be used
 *	 to avoid revealing of sensitive data due to paging etc.
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include "mpi-internal.h"


void
gcry_mpi_mul_ui( gcry_mpi_t prod, gcry_mpi_t mult, unsigned long small_mult )
{
    mpi_size_t size, prod_size;
    mpi_ptr_t  prod_ptr;
    mpi_limb_t cy;
    int sign;

    size = mult->nlimbs;
    sign = mult->sign;

    if( !size || !small_mult ) {
	prod->nlimbs = 0;
	prod->sign = 0;
	return;
    }

    prod_size = size + 1;
    if( prod->alloced < prod_size )
	mpi_resize( prod, prod_size );
    prod_ptr = prod->d;

    cy = _gcry_mpih_mul_1( prod_ptr, mult->d, size, (mpi_limb_t)small_mult );
    if( cy )
	prod_ptr[size++] = cy;
    prod->nlimbs = size;
    prod->sign = sign;
}


void
gcry_mpi_mul_2exp( gcry_mpi_t w, gcry_mpi_t u, unsigned long cnt)
{
    mpi_size_t usize, wsize, limb_cnt;
    mpi_ptr_t wp;
    mpi_limb_t wlimb;
    int usign, wsign;

    usize = u->nlimbs;
    usign = u->sign;

    if( !usize ) {
	w->nlimbs = 0;
	w->sign = 0;
	return;
    }

    limb_cnt = cnt / BITS_PER_MPI_LIMB;
    wsize = usize + limb_cnt + 1;
    if( w->alloced < wsize )
	mpi_resize(w, wsize );
    wp = w->d;
    wsize = usize + limb_cnt;
    wsign = usign;

    cnt %= BITS_PER_MPI_LIMB;
    if( cnt ) {
	wlimb = _gcry_mpih_lshift( wp + limb_cnt, u->d, usize, cnt );
	if( wlimb ) {
	    wp[wsize] = wlimb;
	    wsize++;
	}
    }
    else {
	MPN_COPY_DECR( wp + limb_cnt, u->d, usize );
    }

    /* Zero all whole limbs at low end.  Do it here and not before calling
     * mpn_lshift, not to lose for U == W.  */
    MPN_ZERO( wp, limb_cnt );

    w->nlimbs = wsize;
    w->sign = wsign;
}


void
gcry_mpi_mul( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
    mpi_size_t usize, vsize, wsize;
    mpi_ptr_t up, vp, wp;
    mpi_limb_t cy;
    int usign, vsign, usecure, vsecure, sign_product;
    int assign_wp=0;
    mpi_ptr_t tmp_limb=NULL;
    unsigned int tmp_limb_nlimbs = 0;

    if( u->nlimbs < v->nlimbs ) { /* Swap U and V. */
	usize = v->nlimbs;
	usign = v->sign;
	usecure = mpi_is_secure(v);
	up    = v->d;
	vsize = u->nlimbs;
	vsign = u->sign;
	vsecure = mpi_is_secure(u);
	vp    = u->d;
    }
    else {
	usize = u->nlimbs;
	usign = u->sign;
	usecure = mpi_is_secure(u);
	up    = u->d;
	vsize = v->nlimbs;
	vsign = v->sign;
	vsecure = mpi_is_secure(v);
	vp    = v->d;
    }
    sign_product = usign ^ vsign;
    wp = w->d;

    /* Ensure W has space enough to store the result.  */
    wsize = usize + vsize;
    if ( !mpi_is_secure (w) && (mpi_is_secure (u) || mpi_is_secure (v)) ) {
        /* w is not allocated in secure space but u or v is.  To make sure
         * that no temporray results are stored in w, we temporary use 
         * a newly allocated limb space for w */
        wp = mpi_alloc_limb_space( wsize, 1 );
        assign_wp = 2; /* mark it as 2 so that we can later copy it back to
                        * mormal memory */
    }
    else if( w->alloced < wsize ) {
	if( wp == up || wp == vp ) {
	    wp = mpi_alloc_limb_space( wsize, mpi_is_secure(w) );
	    assign_wp = 1;
	}
	else {
	    mpi_resize(w, wsize );
	    wp = w->d;
	}
    }
    else { /* Make U and V not overlap with W.	*/
	if( wp == up ) {
	    /* W and U are identical.  Allocate temporary space for U.	*/
            tmp_limb_nlimbs = usize;
	    up = tmp_limb = mpi_alloc_limb_space( usize, usecure  );
	    /* Is V identical too?  Keep it identical with U.  */
	    if( wp == vp )
		vp = up;
	    /* Copy to the temporary space.  */
	    MPN_COPY( up, wp, usize );
	}
	else if( wp == vp ) {
	    /* W and V are identical.  Allocate temporary space for V.	*/
            tmp_limb_nlimbs = vsize;
	    vp = tmp_limb = mpi_alloc_limb_space( vsize, vsecure );
	    /* Copy to the temporary space.  */
	    MPN_COPY( vp, wp, vsize );
	}
    }

    if( !vsize )
	wsize = 0;
    else {
	cy = _gcry_mpih_mul( wp, up, usize, vp, vsize );
	wsize -= cy? 0:1;
    }

    if( assign_wp ) {
        if (assign_wp == 2) {
            /* copy the temp wp from secure memory back to normal memory */
	    mpi_ptr_t tmp_wp = mpi_alloc_limb_space (wsize, 0);
	    MPN_COPY (tmp_wp, wp, wsize);
            _gcry_mpi_free_limb_space (wp, 0);
            wp = tmp_wp;
        }
	_gcry_mpi_assign_limb_space( w, wp, wsize );
    }
    w->nlimbs = wsize;
    w->sign = sign_product;
    if( tmp_limb )
	_gcry_mpi_free_limb_space (tmp_limb, tmp_limb_nlimbs);
}


void
gcry_mpi_mulm( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
    gcry_mpi_mul(w, u, v);
    _gcry_mpi_fdiv_r( w, w, m );
}

