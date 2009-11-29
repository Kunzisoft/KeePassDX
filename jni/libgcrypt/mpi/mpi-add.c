/* mpi-add.c  -  MPI functions
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


/****************
 * Add the unsigned integer V to the mpi-integer U and store the
 * result in W. U and V may be the same.
 */
void
gcry_mpi_add_ui(gcry_mpi_t w, gcry_mpi_t u, unsigned long v )
{
    mpi_ptr_t wp, up;
    mpi_size_t usize, wsize;
    int usign, wsign;

    usize = u->nlimbs;
    usign = u->sign;
    wsign = 0;

    /* If not space for W (and possible carry), increase space.  */
    wsize = usize + 1;
    if( w->alloced < wsize )
	mpi_resize(w, wsize);

    /* These must be after realloc (U may be the same as W).  */
    up = u->d;
    wp = w->d;

    if( !usize ) {  /* simple */
	wp[0] = v;
	wsize = v? 1:0;
    }
    else if( !usign ) {  /* mpi is not negative */
	mpi_limb_t cy;
	cy = _gcry_mpih_add_1(wp, up, usize, v);
	wp[usize] = cy;
	wsize = usize + cy;
    }
    else {  /* The signs are different.  Need exact comparison to determine
	     * which operand to subtract from which.  */
	if( usize == 1 && up[0] < v ) {
	    wp[0] = v - up[0];
	    wsize = 1;
	}
	else {
	    _gcry_mpih_sub_1(wp, up, usize, v);
	    /* Size can decrease with at most one limb. */
	    wsize = usize - (wp[usize-1]==0);
	    wsign = 1;
	}
    }

    w->nlimbs = wsize;
    w->sign   = wsign;
}


void
gcry_mpi_add(gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
    mpi_ptr_t wp, up, vp;
    mpi_size_t usize, vsize, wsize;
    int usign, vsign, wsign;

    if( u->nlimbs < v->nlimbs ) { /* Swap U and V. */
	usize = v->nlimbs;
	usign = v->sign;
	vsize = u->nlimbs;
	vsign = u->sign;
	wsize = usize + 1;
	RESIZE_IF_NEEDED(w, wsize);
	/* These must be after realloc (u or v may be the same as w).  */
	up    = v->d;
	vp    = u->d;
    }
    else {
	usize = u->nlimbs;
	usign = u->sign;
	vsize = v->nlimbs;
	vsign = v->sign;
	wsize = usize + 1;
	RESIZE_IF_NEEDED(w, wsize);
	/* These must be after realloc (u or v may be the same as w).  */
	up    = u->d;
	vp    = v->d;
    }
    wp = w->d;
    wsign = 0;

    if( !vsize ) {  /* simple */
	MPN_COPY(wp, up, usize );
	wsize = usize;
	wsign = usign;
    }
    else if( usign != vsign ) { /* different sign */
	/* This test is right since USIZE >= VSIZE */
	if( usize != vsize ) {
	    _gcry_mpih_sub(wp, up, usize, vp, vsize);
	    wsize = usize;
	    MPN_NORMALIZE(wp, wsize);
	    wsign = usign;
	}
	else if( _gcry_mpih_cmp(up, vp, usize) < 0 ) {
	    _gcry_mpih_sub_n(wp, vp, up, usize);
	    wsize = usize;
	    MPN_NORMALIZE(wp, wsize);
	    if( !usign )
		wsign = 1;
	}
	else {
	    _gcry_mpih_sub_n(wp, up, vp, usize);
	    wsize = usize;
	    MPN_NORMALIZE(wp, wsize);
	    if( usign )
		wsign = 1;
	}
    }
    else { /* U and V have same sign. Add them. */
	mpi_limb_t cy = _gcry_mpih_add(wp, up, usize, vp, vsize);
	wp[usize] = cy;
	wsize = usize + cy;
	if( usign )
	    wsign = 1;
    }

    w->nlimbs = wsize;
    w->sign = wsign;
}


/****************
 * Subtract the unsigned integer V from the mpi-integer U and store the
 * result in W.
 */
void
gcry_mpi_sub_ui(gcry_mpi_t w, gcry_mpi_t u, unsigned long v )
{
    mpi_ptr_t wp, up;
    mpi_size_t usize, wsize;
    int usign, wsign;

    usize = u->nlimbs;
    usign = u->sign;
    wsign = 0;

    /* If not space for W (and possible carry), increase space.  */
    wsize = usize + 1;
    if( w->alloced < wsize )
	mpi_resize(w, wsize);

    /* These must be after realloc (U may be the same as W).  */
    up = u->d;
    wp = w->d;

    if( !usize ) {  /* simple */
	wp[0] = v;
	wsize = v? 1:0;
	wsign = 1;
    }
    else if( usign ) {	/* mpi and v are negative */
	mpi_limb_t cy;
	cy = _gcry_mpih_add_1(wp, up, usize, v);
	wp[usize] = cy;
	wsize = usize + cy;
    }
    else {  /* The signs are different.  Need exact comparison to determine
	     * which operand to subtract from which.  */
	if( usize == 1 && up[0] < v ) {
	    wp[0] = v - up[0];
	    wsize = 1;
	    wsign = 1;
	}
	else {
	    _gcry_mpih_sub_1(wp, up, usize, v);
	    /* Size can decrease with at most one limb. */
	    wsize = usize - (wp[usize-1]==0);
	}
    }

    w->nlimbs = wsize;
    w->sign   = wsign;
}

void
gcry_mpi_sub(gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
  gcry_mpi_t vv = mpi_copy (v);
  vv->sign = ! vv->sign;
  gcry_mpi_add (w, u, vv);
  mpi_free (vv);
}


void
gcry_mpi_addm( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
    gcry_mpi_add(w, u, v);
    _gcry_mpi_fdiv_r( w, w, m );
}

void
gcry_mpi_subm( gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
    gcry_mpi_sub(w, u, v);
    _gcry_mpi_fdiv_r( w, w, m );
}

