/* PowerPC-32 submul_1 -- Multiply a limb vector with a limb and subtract
 *			  the result from a second limb vector.
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
 * _gcry_mpih_submul_1( mpi_ptr_t res_ptr,      (r3)
 *		     mpi_ptr_t s1_ptr,	     (r4)
 *		     mpi_size_t s1_size,     (r5)
 *		     mpi_limb_t s2_limb)     (r6)
 *
 * This is a fairly straightforward implementation.  The timing of the PC601
 * is hard to understand, so I will wait to optimize this until I have some
 * hardware to play with.
 *
 * The code trivially generalizes to 64 bit limbs for the PC620.
 */

	.toc
	.csect ._gcry_mpih_submul_1[PR]
	.align 2
	.globl _gcry_mpih_submul_1
	.globl ._gcry_mpih_submul_1
	.csect _gcry_mpih_submul_1[DS]
_gcry_mpih_submul_1:
	.long ._gcry_mpih_submul_1[PR], TOC[tc0], 0
	.csect ._gcry_mpih_submul_1[PR]
._gcry_mpih_submul_1:
	mtctr	5

	lwz	0,0(4)
	mullw	7,0,6
	mulhwu	10,0,6
	lwz	9,0(3)
	subfc	8,7,9
	addc	7,7,8		# invert cy (r7 is junk)
	addi	3,3,-4
	bdz	Lend

Loop:	lwzu	0,4(4)
	stwu	8,4(3)
	mullw	8,0,6
	adde	7,8,10
	mulhwu	10,0,6
	lwz	9,4(3)
	addze	10,10
	subfc	8,7,9
	addc	7,7,8		# invert cy (r7 is junk)
	bdnz	Loop

Lend:	stw	8,4(3)
	addze	3,10
	blr

#else
/* Multiply a limb vector by a single limb, for PowerPC.
   Copyright (C) 1993, 1994, 1995, 1997 Free Software Foundation, Inc.
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

/* mp_limb_t mpn_submul_1 (mp_ptr res_ptr, mp_srcptr s1_ptr,
			   mp_size_t s1_size, mp_limb_t s2_limb)
   Calculate res-s1*s2 and put result back in res; return carry.  */

ENTRY(_gcry_mpih_submul_1)
       mtctr   %r5

       lwz     %r0,0(%r4)
       mullw   %r7,%r0,%r6
       mulhwu  %r10,%r0,%r6
       lwz     %r9,0(%r3)
       subf    %r8,%r7,%r9
       addc    %r7,%r7,%r8	       # invert cy (r7 is junk)
       addi    %r3,%r3,-4	       # adjust res_ptr
       bdz     1f

0:     lwzu    %r0,4(%r4)
       stwu    %r8,4(%r3)
       mullw   %r8,%r0,%r6
       adde    %r7,%r8,%r10
       mulhwu  %r10,%r0,%r6
       lwz     %r9,4(%r3)
       addze   %r10,%r10
       subf    %r8,%r7,%r9
       addc    %r7,%r7,%r8	       # invert cy (r7 is junk)
       bdnz    0b

1:     stw     %r8,4(%r3)
       addze   %r3,%r10
       blr
END(_gcry_mpih_submul_1)
#endif
