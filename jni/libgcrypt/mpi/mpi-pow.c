/* mpi-pow.c  -  MPI functions for exponentiation
 * Copyright (C) 1994, 1996, 1998, 2000, 2002
 *               2003  Free Software Foundation, Inc.
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
#include <string.h>

#include "mpi-internal.h"
#include "longlong.h"


/****************
 * RES = BASE ^ EXPO mod MOD
 */
void
gcry_mpi_powm (gcry_mpi_t res, 
               gcry_mpi_t base, gcry_mpi_t expo, gcry_mpi_t mod)
{
  /* Pointer to the limbs of the arguments, their size and signs. */
  mpi_ptr_t  rp, ep, mp, bp;
  mpi_size_t esize, msize, bsize, rsize;
  int               msign, bsign, rsign;
  /* Flags telling the secure allocation status of the arguments.  */
  int        esec,  msec,  bsec,  rsec;  
  /* Size of the result including space for temporary values.  */
  mpi_size_t size;
  /* Helper.  */
  int mod_shift_cnt;
  int negative_result;
  mpi_ptr_t mp_marker = NULL;
  mpi_ptr_t bp_marker = NULL;
  mpi_ptr_t ep_marker = NULL;
  mpi_ptr_t xp_marker = NULL;
  unsigned int mp_nlimbs = 0;
  unsigned int bp_nlimbs = 0;
  unsigned int ep_nlimbs = 0;
  unsigned int xp_nlimbs = 0;
  mpi_ptr_t tspace = NULL;
  mpi_size_t tsize = 0; 


  esize = expo->nlimbs;
  msize = mod->nlimbs;
  size = 2 * msize;
  msign = mod->sign;
  
  esec = mpi_is_secure(expo);
  msec = mpi_is_secure(mod);
  bsec = mpi_is_secure(base);
  rsec = mpi_is_secure(res);

  rp = res->d;
  ep = expo->d;

  if (!msize)
    msize = 1 / msize;	    /* Provoke a signal.  */

  if (!esize) 
    {
      /* Exponent is zero, result is 1 mod MOD, i.e., 1 or 0 depending
        on if MOD equals 1.  */
      rp[0] = 1;
      res->nlimbs = (msize == 1 && mod->d[0] == 1) ? 0 : 1;
      res->sign = 0;
      goto leave;
    }

  /* Normalize MOD (i.e. make its most significant bit set) as
     required by mpn_divrem.  This will make the intermediate values
     in the calculation slightly larger, but the correct result is
     obtained after a final reduction using the original MOD value. */
  mp_nlimbs = msec? msize:0;
  mp = mp_marker = mpi_alloc_limb_space(msize, msec);
  count_leading_zeros (mod_shift_cnt, mod->d[msize-1]);
  if (mod_shift_cnt)
    _gcry_mpih_lshift (mp, mod->d, msize, mod_shift_cnt);
  else
    MPN_COPY( mp, mod->d, msize );

  bsize = base->nlimbs;
  bsign = base->sign;
  if (bsize > msize) 
    {
      /* The base is larger than the module.  Reduce it.

         Allocate (BSIZE + 1) with space for remainder and quotient.
         (The quotient is (bsize - msize + 1) limbs.)  */
      bp_nlimbs = bsec ? (bsize + 1):0;
      bp = bp_marker = mpi_alloc_limb_space( bsize + 1, bsec );
      MPN_COPY ( bp, base->d, bsize );
      /* We don't care about the quotient, store it above the
       * remainder, at BP + MSIZE.  */
      _gcry_mpih_divrem( bp + msize, 0, bp, bsize, mp, msize );
      bsize = msize;
      /* Canonicalize the base, since we are going to multiply with it
	 quite a few times.  */
      MPN_NORMALIZE( bp, bsize );
    }
  else
    bp = base->d;

  if (!bsize)
    {
      res->nlimbs = 0;
      res->sign = 0;
      goto leave;
    }


  /* Make BASE, EXPO and MOD not overlap with RES.  */
  if ( rp == bp )
    {
      /* RES and BASE are identical.  Allocate temp. space for BASE.  */
      gcry_assert (!bp_marker);
      bp_nlimbs = bsec? bsize:0;
      bp = bp_marker = mpi_alloc_limb_space( bsize, bsec );
      MPN_COPY(bp, rp, bsize);
    }
  if ( rp == ep )
    {
      /* RES and EXPO are identical.  Allocate temp. space for EXPO.  */
      ep_nlimbs = esec? esize:0;
      ep = ep_marker = mpi_alloc_limb_space( esize, esec );
      MPN_COPY(ep, rp, esize);
    }
  if ( rp == mp ) 
    {
      /* RES and MOD are identical.  Allocate temporary space for MOD.*/
      gcry_assert (!mp_marker);
      mp_nlimbs = msec?msize:0;
      mp = mp_marker = mpi_alloc_limb_space( msize, msec );
      MPN_COPY(mp, rp, msize);
    }

  /* Copy base to the result.  */
  if (res->alloced < size)
    {
      mpi_resize (res, size);
      rp = res->d;
    }
  MPN_COPY ( rp, bp, bsize );
  rsize = bsize;
  rsign = bsign;
  
  /* Main processing.  */
  {
    mpi_size_t i;
    mpi_ptr_t xp;
    int c;
    mpi_limb_t e;
    mpi_limb_t carry_limb;
    struct karatsuba_ctx karactx;
    
    xp_nlimbs = msec? (2 * (msize + 1)):0;
    xp = xp_marker = mpi_alloc_limb_space( 2 * (msize + 1), msec );
    
    memset( &karactx, 0, sizeof karactx );
    negative_result = (ep[0] & 1) && base->sign;
    
    i = esize - 1;
    e = ep[i];
    count_leading_zeros (c, e);
    e = (e << c) << 1;     /* Shift the expo bits to the left, lose msb.  */
    c = BITS_PER_MPI_LIMB - 1 - c;

    /* Main loop.
     
       Make the result be pointed to alternately by XP and RP.  This
       helps us avoid block copying, which would otherwise be
       necessary with the overlap restrictions of
       _gcry_mpih_divmod. With 50% probability the result after this
       loop will be in the area originally pointed by RP (==RES->d),
       and with 50% probability in the area originally pointed to by XP. */
    for (;;)
      {
        while (c) 
          {
            mpi_ptr_t tp;
            mpi_size_t xsize;
            
            /*mpih_mul_n(xp, rp, rp, rsize);*/
            if ( rsize < KARATSUBA_THRESHOLD )
              _gcry_mpih_sqr_n_basecase( xp, rp, rsize );
            else 
              {
                if ( !tspace ) 
                  {
                    tsize = 2 * rsize;
                    tspace = mpi_alloc_limb_space( tsize, 0 );
                  }
                else if ( tsize < (2*rsize) )
                  {
                    _gcry_mpi_free_limb_space (tspace, 0);
                    tsize = 2 * rsize;
                    tspace = mpi_alloc_limb_space (tsize, 0 );
                  }
                _gcry_mpih_sqr_n (xp, rp, rsize, tspace);
              }

            xsize = 2 * rsize;
            if ( xsize > msize )
              {
                _gcry_mpih_divrem(xp + msize, 0, xp, xsize, mp, msize);
                xsize = msize;
              }

            tp = rp; rp = xp; xp = tp;
            rsize = xsize;

            if ( (mpi_limb_signed_t)e < 0 )
              {
                /*mpih_mul( xp, rp, rsize, bp, bsize );*/
                if( bsize < KARATSUBA_THRESHOLD ) 
                  _gcry_mpih_mul ( xp, rp, rsize, bp, bsize );
                else 
                  _gcry_mpih_mul_karatsuba_case (xp, rp, rsize, bp, bsize,
                                                 &karactx);
                
                xsize = rsize + bsize;
                if ( xsize > msize ) 
                  {
                    _gcry_mpih_divrem(xp + msize, 0, xp, xsize, mp, msize);
                    xsize = msize;
                  }
                
                tp = rp; rp = xp; xp = tp;
                rsize = xsize;
              }
            e <<= 1;
            c--;
          }

        i--;
        if ( i < 0 )
          break;
        e = ep[i];
        c = BITS_PER_MPI_LIMB;
      }

    /* We shifted MOD, the modulo reduction argument, left
       MOD_SHIFT_CNT steps.  Adjust the result by reducing it with the
       original MOD.

       Also make sure the result is put in RES->d (where it already
       might be, see above).  */
    if ( mod_shift_cnt ) 
      {
        carry_limb = _gcry_mpih_lshift( res->d, rp, rsize, mod_shift_cnt);
        rp = res->d;
        if ( carry_limb )
          {
            rp[rsize] = carry_limb;
            rsize++;
          }
      }
    else if (res->d != rp)
      {
        MPN_COPY (res->d, rp, rsize);
        rp = res->d;
      }

    if ( rsize >= msize ) 
      {
        _gcry_mpih_divrem(rp + msize, 0, rp, rsize, mp, msize);
        rsize = msize;
      }

    /* Remove any leading zero words from the result.  */
    if ( mod_shift_cnt )
      _gcry_mpih_rshift( rp, rp, rsize, mod_shift_cnt);
    MPN_NORMALIZE (rp, rsize);
    
    _gcry_mpih_release_karatsuba_ctx (&karactx );
  }

  /* Fixup for negative results.  */
  if ( negative_result && rsize )
    {
      if ( mod_shift_cnt )
        _gcry_mpih_rshift( mp, mp, msize, mod_shift_cnt);
      _gcry_mpih_sub( rp, mp, msize, rp, rsize);
      rsize = msize;
      rsign = msign;
      MPN_NORMALIZE(rp, rsize);
    }
  gcry_assert (res->d == rp);
  res->nlimbs = rsize;
  res->sign = rsign;
  
 leave:
  if (mp_marker)
    _gcry_mpi_free_limb_space( mp_marker, mp_nlimbs );
  if (bp_marker)
    _gcry_mpi_free_limb_space( bp_marker, bp_nlimbs );
  if (ep_marker)
    _gcry_mpi_free_limb_space( ep_marker, ep_nlimbs );
  if (xp_marker)
    _gcry_mpi_free_limb_space( xp_marker, xp_nlimbs );
  if (tspace)
    _gcry_mpi_free_limb_space( tspace, 0 );
}

