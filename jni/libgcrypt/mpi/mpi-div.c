/* mpi-div.c  -  MPI functions
 * Copyright (C) 1994, 1996, 1998, 2001, 2002,
 *               2003 Free Software Foundation, Inc.
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
#include "longlong.h"
#include "g10lib.h"


void
_gcry_mpi_fdiv_r( gcry_mpi_t rem, gcry_mpi_t dividend, gcry_mpi_t divisor )
{
    int divisor_sign = divisor->sign;
    gcry_mpi_t temp_divisor = NULL;

    /* We need the original value of the divisor after the remainder has been
     * preliminary calculated.	We have to copy it to temporary space if it's
     * the same variable as REM.  */
    if( rem == divisor ) {
	temp_divisor = mpi_copy( divisor );
	divisor = temp_divisor;
    }

    _gcry_mpi_tdiv_r( rem, dividend, divisor );

    if( ((divisor_sign?1:0) ^ (dividend->sign?1:0)) && rem->nlimbs )
	gcry_mpi_add( rem, rem, divisor);

    if( temp_divisor )
	mpi_free(temp_divisor);
}



/****************
 * Division rounding the quotient towards -infinity.
 * The remainder gets the same sign as the denominator.
 * rem is optional
 */

ulong
_gcry_mpi_fdiv_r_ui( gcry_mpi_t rem, gcry_mpi_t dividend, ulong divisor )
{
    mpi_limb_t rlimb;

    rlimb = _gcry_mpih_mod_1( dividend->d, dividend->nlimbs, divisor );
    if( rlimb && dividend->sign )
	rlimb = divisor - rlimb;

    if( rem ) {
	rem->d[0] = rlimb;
	rem->nlimbs = rlimb? 1:0;
    }
    return rlimb;
}


void
_gcry_mpi_fdiv_q( gcry_mpi_t quot, gcry_mpi_t dividend, gcry_mpi_t divisor )
{
    gcry_mpi_t tmp = mpi_alloc( mpi_get_nlimbs(quot) );
    _gcry_mpi_fdiv_qr( quot, tmp, dividend, divisor);
    mpi_free(tmp);
}

void
_gcry_mpi_fdiv_qr( gcry_mpi_t quot, gcry_mpi_t rem, gcry_mpi_t dividend, gcry_mpi_t divisor )
{
    int divisor_sign = divisor->sign;
    gcry_mpi_t temp_divisor = NULL;

    if( quot == divisor || rem == divisor ) {
	temp_divisor = mpi_copy( divisor );
	divisor = temp_divisor;
    }

    _gcry_mpi_tdiv_qr( quot, rem, dividend, divisor );

    if( (divisor_sign ^ dividend->sign) && rem->nlimbs ) {
	gcry_mpi_sub_ui( quot, quot, 1 );
	gcry_mpi_add( rem, rem, divisor);
    }

    if( temp_divisor )
	mpi_free(temp_divisor);
}


/* If den == quot, den needs temporary storage.
 * If den == rem, den needs temporary storage.
 * If num == quot, num needs temporary storage.
 * If den has temporary storage, it can be normalized while being copied,
 *   i.e no extra storage should be allocated.
 */

void
_gcry_mpi_tdiv_r( gcry_mpi_t rem, gcry_mpi_t num, gcry_mpi_t den)
{
    _gcry_mpi_tdiv_qr(NULL, rem, num, den );
}

void
_gcry_mpi_tdiv_qr( gcry_mpi_t quot, gcry_mpi_t rem, gcry_mpi_t num, gcry_mpi_t den)
{
    mpi_ptr_t np, dp;
    mpi_ptr_t qp, rp;
    mpi_size_t nsize = num->nlimbs;
    mpi_size_t dsize = den->nlimbs;
    mpi_size_t qsize, rsize;
    mpi_size_t sign_remainder = num->sign;
    mpi_size_t sign_quotient = num->sign ^ den->sign;
    unsigned normalization_steps;
    mpi_limb_t q_limb;
    mpi_ptr_t marker[5];
    unsigned int marker_nlimbs[5];
    int markidx=0;

    /* Ensure space is enough for quotient and remainder.
     * We need space for an extra limb in the remainder, because it's
     * up-shifted (normalized) below.  */
    rsize = nsize + 1;
    mpi_resize( rem, rsize);

    qsize = rsize - dsize;	  /* qsize cannot be bigger than this.	*/
    if( qsize <= 0 ) {
	if( num != rem ) {
	    rem->nlimbs = num->nlimbs;
	    rem->sign = num->sign;
	    MPN_COPY(rem->d, num->d, nsize);
	}
	if( quot ) {
	    /* This needs to follow the assignment to rem, in case the
	     * numerator and quotient are the same.  */
	    quot->nlimbs = 0;
	    quot->sign = 0;
	}
	return;
    }

    if( quot )
	mpi_resize( quot, qsize);

    /* Read pointers here, when reallocation is finished.  */
    np = num->d;
    dp = den->d;
    rp = rem->d;

    /* Optimize division by a single-limb divisor.  */
    if( dsize == 1 ) {
	mpi_limb_t rlimb;
	if( quot ) {
	    qp = quot->d;
	    rlimb = _gcry_mpih_divmod_1( qp, np, nsize, dp[0] );
	    qsize -= qp[qsize - 1] == 0;
	    quot->nlimbs = qsize;
	    quot->sign = sign_quotient;
	}
	else
	    rlimb = _gcry_mpih_mod_1( np, nsize, dp[0] );
	rp[0] = rlimb;
	rsize = rlimb != 0?1:0;
	rem->nlimbs = rsize;
	rem->sign = sign_remainder;
	return;
    }


    if( quot ) {
	qp = quot->d;
	/* Make sure QP and NP point to different objects.  Otherwise the
	 * numerator would be gradually overwritten by the quotient limbs.  */
	if(qp == np) { /* Copy NP object to temporary space.  */
            marker_nlimbs[markidx] = nsize;
	    np = marker[markidx++] = mpi_alloc_limb_space(nsize,
							  mpi_is_secure(quot));
	    MPN_COPY(np, qp, nsize);
	}
    }
    else /* Put quotient at top of remainder. */
	qp = rp + dsize;

    count_leading_zeros( normalization_steps, dp[dsize - 1] );

    /* Normalize the denominator, i.e. make its most significant bit set by
     * shifting it NORMALIZATION_STEPS bits to the left.  Also shift the
     * numerator the same number of steps (to keep the quotient the same!).
     */
    if( normalization_steps ) {
	mpi_ptr_t tp;
	mpi_limb_t nlimb;

	/* Shift up the denominator setting the most significant bit of
	 * the most significant word.  Use temporary storage not to clobber
	 * the original contents of the denominator.  */
        marker_nlimbs[markidx] = dsize;
	tp = marker[markidx++] = mpi_alloc_limb_space(dsize,mpi_is_secure(den));
	_gcry_mpih_lshift( tp, dp, dsize, normalization_steps );
	dp = tp;

	/* Shift up the numerator, possibly introducing a new most
	 * significant word.  Move the shifted numerator in the remainder
	 * meanwhile.  */
	nlimb = _gcry_mpih_lshift(rp, np, nsize, normalization_steps);
	if( nlimb ) {
	    rp[nsize] = nlimb;
	    rsize = nsize + 1;
	}
	else
	    rsize = nsize;
    }
    else {
	/* The denominator is already normalized, as required.	Copy it to
	 * temporary space if it overlaps with the quotient or remainder.  */
	if( dp == rp || (quot && (dp == qp))) {
	    mpi_ptr_t tp;

            marker_nlimbs[markidx] = dsize;
	    tp = marker[markidx++] = mpi_alloc_limb_space(dsize,
                                                          mpi_is_secure(den));
	    MPN_COPY( tp, dp, dsize );
	    dp = tp;
	}

	/* Move the numerator to the remainder.  */
	if( rp != np )
	    MPN_COPY(rp, np, nsize);

	rsize = nsize;
    }

    q_limb = _gcry_mpih_divrem( qp, 0, rp, rsize, dp, dsize );

    if( quot ) {
	qsize = rsize - dsize;
	if(q_limb) {
	    qp[qsize] = q_limb;
	    qsize += 1;
	}

	quot->nlimbs = qsize;
	quot->sign = sign_quotient;
    }

    rsize = dsize;
    MPN_NORMALIZE (rp, rsize);

    if( normalization_steps && rsize ) {
	_gcry_mpih_rshift(rp, rp, rsize, normalization_steps);
	rsize -= rp[rsize - 1] == 0?1:0;
    }

    rem->nlimbs = rsize;
    rem->sign	= sign_remainder;
    while( markidx )
      {
        markidx--;
	_gcry_mpi_free_limb_space (marker[markidx], marker_nlimbs[markidx]);
      }
}

void
_gcry_mpi_tdiv_q_2exp( gcry_mpi_t w, gcry_mpi_t u, unsigned int count )
{
    mpi_size_t usize, wsize;
    mpi_size_t limb_cnt;

    usize = u->nlimbs;
    limb_cnt = count / BITS_PER_MPI_LIMB;
    wsize = usize - limb_cnt;
    if( limb_cnt >= usize )
	w->nlimbs = 0;
    else {
	mpi_ptr_t wp;
	mpi_ptr_t up;

	RESIZE_IF_NEEDED( w, wsize );
	wp = w->d;
	up = u->d;

	count %= BITS_PER_MPI_LIMB;
	if( count ) {
	    _gcry_mpih_rshift( wp, up + limb_cnt, wsize, count );
	    wsize -= !wp[wsize - 1];
	}
	else {
	    MPN_COPY_INCR( wp, up + limb_cnt, wsize);
	}

	w->nlimbs = wsize;
    }
}

/****************
 * Check whether dividend is divisible by divisor
 * (note: divisor must fit into a limb)
 */
int
_gcry_mpi_divisible_ui(gcry_mpi_t dividend, ulong divisor )
{
    return !_gcry_mpih_mod_1( dividend->d, dividend->nlimbs, divisor );
}


void
gcry_mpi_div (gcry_mpi_t quot, gcry_mpi_t rem, gcry_mpi_t dividend, gcry_mpi_t divisor, int round)
{
  if (!round)
    {
      if (!rem)
        {
          gcry_mpi_t tmp = mpi_alloc (mpi_get_nlimbs(quot));
          _gcry_mpi_tdiv_qr (quot, tmp, dividend, divisor);
          mpi_free (tmp);
        }
      else
        _gcry_mpi_tdiv_qr (quot, rem, dividend, divisor);
    }
  else if (round < 0)
    {
      if (!rem)
        _gcry_mpi_fdiv_q (quot, dividend, divisor);
      else if (!quot)
        _gcry_mpi_fdiv_r (rem, dividend, divisor);
      else
        _gcry_mpi_fdiv_qr (quot, rem, dividend, divisor);
    }
  else
    log_bug ("mpi rounding to ceiling not yet implemented\n");
}



