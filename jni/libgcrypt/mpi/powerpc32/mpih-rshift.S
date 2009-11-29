/* PowerPC-32 rshift
 *
 *      Copyright (C) 1995, 1998, 2002 Free Software Foundation, Inc.
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


#include "sysdep.h"
#include "asm-syntax.h"


#ifndef USE_PPC_PATCHES

/*******************
 * mpi_limb_t
 * _gcry_mpih_rshift( mpi_ptr_t wp,	(r3)
 *		   mpi_ptr_t up,	(r4)
 *		   mpi_size_t usize,	(r5)
 *		   unsigned cnt)	(r6)
 */

	.toc
.csect	.text[PR]
	.align	2
	.globl	_gcry_mpih_rshift
	.globl	._gcry_mpih_rshift
	.csect	_gcry_mpih_rshift[DS]
_gcry_mpih_rshift:
	.long	._gcry_mpih_rshift,  TOC[tc0],	  0
	.csect	.text[PR]
._gcry_mpih_rshift:
	mtctr	5		# copy size into CTR
	addi	7,3,-4		# move adjusted res_ptr to free return reg
	subfic	8,6,32
	lwz	11,0(4) 	# load first s1 limb
	slw	3,11,8		# compute function return value
	bdz	Lend1

Loop:	lwzu	10,4(4)
	srw	9,11,6
	slw	12,10,8
	or	9,9,12
	stwu	9,4(7)
	bdz	Lend2
	lwzu	11,4(4)
	srw	9,10,6
	slw	12,11,8
	or	9,9,12
	stwu	9,4(7)
	bdnz	Loop

Lend1:	srw	0,11,6
	stw	0,4(7)
	blr

Lend2:	srw	0,10,6
	stw	0,4(7)
	blr

#else
/* Shift a limb right, low level routine.
   Copyright (C) 1995, 1997 Free Software Foundation, Inc.
   This file is part of the GNU C Library.

   The GNU C Library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Library General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.

   The GNU C Library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Library General Public License for more details.

   You should have received a copy of the GNU Library General Public
   License along with the GNU C Library; see the file COPYING.LIB.  If not,
   write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
   Boston, MA 02111-1307, USA.	*/


/* INPUT PARAMETERS
   res_ptr     r3
   s1_ptr      r4
   size 	       r5
   cnt	       r6  */

ENTRY(_gcry_mpih_rshift)
       mtctr   5	       # copy size into CTR
       addi    7,3,-4	       # move adjusted res_ptr to free return reg
       subfic  8,6,32
       lwz     11,0(4)	       # load first s1 limb
       slw     3,11,8	       # compute function return value
       bdz     1f

0:     lwzu    10,4(4)
       srw     9,11,6
       slw     12,10,8
       or      9,9,12
       stwu    9,4(7)
       bdz     2f
       lwzu    11,4(4)
       srw     9,10,6
       slw     12,11,8
       or      9,9,12
       stwu    9,4(7)
       bdnz    0b

1:     srw     0,11,6
       stw     0,4(7)
       blr

2:     srw     0,10,6
       stw     0,4(7)
       blr
END(_gcry_mpih_rshift)
#endif
