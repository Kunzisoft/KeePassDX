/* alpha - left shift
 *
 *      Copyright (C) 1994, 1995, 1998, 2001,
 *                    2002  Free Software Foundation, Inc.
 *
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



/*******************
 * mpi_limb_t
 * _gcry_mpih_lshift( mpi_ptr_t wp,	(r16)
 *		   mpi_ptr_t up,	(r17)
 *		   mpi_size_t usize,	(r18)
 *		   unsigned cnt)	(r19)
 *
 * This code runs at 4.8 cycles/limb on the 21064.  With infinite unrolling,
 * it would take 4 cycles/limb.  It should be possible to get down to 3
 * cycles/limb since both ldq and stq can be paired with the other used
 * instructions.  But there are many restrictions in the 21064 pipeline that
 * makes it hard, if not impossible, to get down to 3 cycles/limb:
 *
 * 1. ldq has a 3 cycle delay, srl and sll have a 2 cycle delay.
 * 2. Only aligned instruction pairs can be paired.
 * 3. The store buffer or silo might not be able to deal with the bandwidth.
 */

	.set	noreorder
	.set	noat
.text
	.align	3
	.globl	_gcry_mpih_lshift
	.ent	_gcry_mpih_lshift
_gcry_mpih_lshift:
	.frame	$30,0,$26,0

	s8addq	$18,$17,$17	# make r17 point at end of s1
	ldq	$4,-8($17)	# load first limb
	subq	$17,8,$17
	subq	$31,$19,$7
	s8addq	$18,$16,$16	# make r16 point at end of RES
	subq	$18,1,$18
	and	$18,4-1,$20	# number of limbs in first loop
	srl	$4,$7,$0	# compute function result

	beq	$20,.L0
	subq	$18,$20,$18

	.align	3
.Loop0:
	ldq	$3,-8($17)
	subq	$16,8,$16
	subq	$17,8,$17
	subq	$20,1,$20
	sll	$4,$19,$5
	srl	$3,$7,$6
	bis	$3,$3,$4
	bis	$5,$6,$8
	stq	$8,0($16)
	bne	$20,.Loop0

.L0:	beq	$18,.Lend

	.align	3
.Loop:	ldq	$3,-8($17)
	subq	$16,32,$16
	subq	$18,4,$18
	sll	$4,$19,$5
	srl	$3,$7,$6

	ldq	$4,-16($17)
	sll	$3,$19,$1
	bis	$5,$6,$8
	stq	$8,24($16)
	srl	$4,$7,$2

	ldq	$3,-24($17)
	sll	$4,$19,$5
	bis	$1,$2,$8
	stq	$8,16($16)
	srl	$3,$7,$6

	ldq	$4,-32($17)
	sll	$3,$19,$1
	bis	$5,$6,$8
	stq	$8,8($16)
	srl	$4,$7,$2

	subq	$17,32,$17
	bis	$1,$2,$8
	stq	$8,0($16)

	bgt	$18,.Loop

.Lend:	sll	$4,$19,$8
	stq	$8,-8($16)
	ret	$31,($26),1
	.end	_gcry_mpih_lshift


