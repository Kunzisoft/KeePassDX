/* IBM POWER sub_n -- Subtract two limb vectors of equal, non-zero length.
 *
 *      Copyright (C) 1992, 1994, 1995, 1996, 1999,
 *                    2002 Free Software Foundation, Inc.
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

/*
# INPUT PARAMETERS
# res_ptr	r3
# s1_ptr	r4
# s2_ptr	r5
# size		r6
 */

	.toc
	.extern _gcry_mpih_sub_n[DS]
	.extern ._gcry_mpih_sub_n
.csect [PR]
	.align 2
	.globl _gcry_mpih_sub_n
	.globl ._gcry_mpih_sub_n
	.csect _gcry_mpih_sub_n[DS]
_gcry_mpih_sub_n:
	.long ._gcry_mpih_sub_n, TOC[tc0], 0
	.csect [PR]
._gcry_mpih_sub_n:
	andil.	10,6,1		# odd or even number of limbs?
	l	8,0(4)		# load least significant s1 limb
	l	0,0(5)		# load least significant s2 limb
	cal	3,-4(3) 	# offset res_ptr, it's updated before it's used
	sri	10,6,1		# count for unrolled loop
	sf	7,0,8		# subtract least significant limbs, set cy
	mtctr	10		# copy count into CTR
	beq	0,Leven 	# branch if even # of limbs (# of limbs >= 2)

# We have an odd # of limbs.  Add the first limbs separately.
	cmpi	1,10,0		# is count for unrolled loop zero?
	bne	1,L1		# branch if not
	st	7,4(3)
	sfe	3,0,0		# load !cy into ...
	sfi	3,3,0		# ... return value register
	br			# return

# We added least significant limbs.  Now reload the next limbs to enter loop.
L1:	lu	8,4(4)		# load s1 limb and update s1_ptr
	lu	0,4(5)		# load s2 limb and update s2_ptr
	stu	7,4(3)
	sfe	7,0,8		# subtract limbs, set cy
Leven:	lu	9,4(4)		# load s1 limb and update s1_ptr
	lu	10,4(5) 	# load s2 limb and update s2_ptr
	bdz	Lend		# If done, skip loop

Loop:	lu	8,4(4)		# load s1 limb and update s1_ptr
	lu	0,4(5)		# load s2 limb and update s2_ptr
	sfe	11,10,9 	# subtract previous limbs with cy, set cy
	stu	7,4(3)		#
	lu	9,4(4)		# load s1 limb and update s1_ptr
	lu	10,4(5) 	# load s2 limb and update s2_ptr
	sfe	7,0,8		# subtract previous limbs with cy, set cy
	stu	11,4(3) 	#
	bdn	Loop		# decrement CTR and loop back

Lend:	sfe	11,10,9 	# subtract limbs with cy, set cy
	st	7,4(3)		#
	st	11,8(3) 	#
	sfe	3,0,0		# load !cy into ...
	sfi	3,3,0		# ... return value register
	br

