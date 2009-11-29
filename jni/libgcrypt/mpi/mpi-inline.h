/* mpi-inline.h  -  Internal to the Multi Precision Integers
 * Copyright (C) 1994, 1996, 1998, 1999,
 *               2001, 2002 Free Software Foundation, Inc.
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

#ifndef G10_MPI_INLINE_H
#define G10_MPI_INLINE_H

#ifndef G10_MPI_INLINE_DECL
#define G10_MPI_INLINE_DECL  extern __inline__
#endif

G10_MPI_INLINE_DECL  mpi_limb_t
_gcry_mpih_add_1( mpi_ptr_t res_ptr, mpi_ptr_t s1_ptr,
	       mpi_size_t s1_size, mpi_limb_t s2_limb)
{
    mpi_limb_t x;

    x = *s1_ptr++;
    s2_limb += x;
    *res_ptr++ = s2_limb;
    if( s2_limb < x ) { /* sum is less than the left operand: handle carry */
	while( --s1_size ) {
	    x = *s1_ptr++ + 1;	/* add carry */
	    *res_ptr++ = x;	/* and store */
	    if( x )		/* not 0 (no overflow): we can stop */
		goto leave;
	}
	return 1; /* return carry (size of s1 to small) */
    }

  leave:
    if( res_ptr != s1_ptr ) { /* not the same variable */
	mpi_size_t i;	       /* copy the rest */
	for( i=0; i < s1_size-1; i++ )
	    res_ptr[i] = s1_ptr[i];
    }
    return 0; /* no carry */
}



G10_MPI_INLINE_DECL mpi_limb_t
_gcry_mpih_add(mpi_ptr_t res_ptr, mpi_ptr_t s1_ptr, mpi_size_t s1_size,
			       mpi_ptr_t s2_ptr, mpi_size_t s2_size)
{
    mpi_limb_t cy = 0;

    if( s2_size )
	cy = _gcry_mpih_add_n( res_ptr, s1_ptr, s2_ptr, s2_size );

    if( s1_size - s2_size )
	cy = _gcry_mpih_add_1( res_ptr + s2_size, s1_ptr + s2_size,
			    s1_size - s2_size, cy);
    return cy;
}


G10_MPI_INLINE_DECL mpi_limb_t
_gcry_mpih_sub_1(mpi_ptr_t res_ptr,  mpi_ptr_t s1_ptr,
	      mpi_size_t s1_size, mpi_limb_t s2_limb )
{
    mpi_limb_t x;

    x = *s1_ptr++;
    s2_limb = x - s2_limb;
    *res_ptr++ = s2_limb;
    if( s2_limb > x ) {
	while( --s1_size ) {
	    x = *s1_ptr++;
	    *res_ptr++ = x - 1;
	    if( x )
		goto leave;
	}
	return 1;
    }

  leave:
    if( res_ptr != s1_ptr ) {
	mpi_size_t i;
	for( i=0; i < s1_size-1; i++ )
	    res_ptr[i] = s1_ptr[i];
    }
    return 0;
}



G10_MPI_INLINE_DECL   mpi_limb_t
_gcry_mpih_sub( mpi_ptr_t res_ptr, mpi_ptr_t s1_ptr, mpi_size_t s1_size,
				mpi_ptr_t s2_ptr, mpi_size_t s2_size)
{
    mpi_limb_t cy = 0;

    if( s2_size )
	cy = _gcry_mpih_sub_n(res_ptr, s1_ptr, s2_ptr, s2_size);

    if( s1_size - s2_size )
	cy = _gcry_mpih_sub_1(res_ptr + s2_size, s1_ptr + s2_size,
				      s1_size - s2_size, cy);
    return cy;
}

/****************
 * Compare OP1_PTR/OP1_SIZE with OP2_PTR/OP2_SIZE.
 * There are no restrictions on the relative sizes of
 * the two arguments.
 * Return 1 if OP1 > OP2, 0 if they are equal, and -1 if OP1 < OP2.
 */
G10_MPI_INLINE_DECL int
_gcry_mpih_cmp( mpi_ptr_t op1_ptr, mpi_ptr_t op2_ptr, mpi_size_t size )
{
    mpi_size_t i;
    mpi_limb_t op1_word, op2_word;

    for( i = size - 1; i >= 0 ; i--) {
	op1_word = op1_ptr[i];
	op2_word = op2_ptr[i];
	if( op1_word != op2_word )
	    goto diff;
    }
    return 0;

  diff:
    /* This can *not* be simplified to
     *	 op2_word - op2_word
     * since that expression might give signed overflow.  */
    return (op1_word > op2_word) ? 1 : -1;
}


#endif /*G10_MPI_INLINE_H*/
