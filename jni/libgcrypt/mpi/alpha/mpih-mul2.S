/* Alpha 21064 addmul_1 -- Multiply a limb vector with a limb and add
 *			   the result to a second limb vector.
 *
 *      Copyright (C) 1992, 1994, 1995, 1998,
 *                    2001, 2002 Free Software Foundation, Inc.
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


/*******************
 * mpi_limb_t
 * _gcry_mpih_addmul_1( mpi_ptr_t res_ptr,      (r16)
 *		     mpi_ptr_t s1_ptr,	     (r17)
 *		     mpi_size_t s1_size,     (r18)
 *		     mpi_limb_t s2_limb)     (r19)
 *
 * This code runs at 42 cycles/limb on EV4 and 18 cycles/limb on EV5.
 */


	.set	noreorder
	.set	noat
.text
	.align	3
	.globl	_gcry_mpih_addmul_1
	.ent	_gcry_mpih_addmul_1 2
_gcry_mpih_addmul_1:
	.frame	$30,0,$26

	ldq	$2,0($17)	# $2 = s1_limb
	addq	$17,8,$17	# s1_ptr++
	subq	$18,1,$18	# size--
	mulq	$2,$19,$3	# $3 = prod_low
	ldq	$5,0($16)	# $5 = *res_ptr
	umulh	$2,$19,$0	# $0 = prod_high
	beq	$18,.Lend1	# jump if size was == 1
	ldq	$2,0($17)	# $2 = s1_limb
	addq	$17,8,$17	# s1_ptr++
	subq	$18,1,$18	# size--
	addq	$5,$3,$3
	cmpult	$3,$5,$4
	stq	$3,0($16)
	addq	$16,8,$16	# res_ptr++
	beq	$18,.Lend2	# jump if size was == 2

	.align	3
.Loop:	mulq	$2,$19,$3	# $3 = prod_low
	ldq	$5,0($16)	# $5 = *res_ptr
	addq	$4,$0,$0	# cy_limb = cy_limb + 'cy'
	subq	$18,1,$18	# size--
	umulh	$2,$19,$4	# $4 = cy_limb
	ldq	$2,0($17)	# $2 = s1_limb
	addq	$17,8,$17	# s1_ptr++
	addq	$3,$0,$3	# $3 = cy_limb + prod_low
	cmpult	$3,$0,$0	# $0 = carry from (cy_limb + prod_low)
	addq	$5,$3,$3
	cmpult	$3,$5,$5
	stq	$3,0($16)
	addq	$16,8,$16	# res_ptr++
	addq	$5,$0,$0	# combine carries
	bne	$18,.Loop

.Lend2: mulq	$2,$19,$3	# $3 = prod_low
	ldq	$5,0($16)	# $5 = *res_ptr
	addq	$4,$0,$0	# cy_limb = cy_limb + 'cy'
	umulh	$2,$19,$4	# $4 = cy_limb
	addq	$3,$0,$3	# $3 = cy_limb + prod_low
	cmpult	$3,$0,$0	# $0 = carry from (cy_limb + prod_low)
	addq	$5,$3,$3
	cmpult	$3,$5,$5
	stq	$3,0($16)
	addq	$5,$0,$0	# combine carries
	addq	$4,$0,$0	# cy_limb = prod_high + cy
	ret	$31,($26),1
.Lend1: addq	$5,$3,$3
	cmpult	$3,$5,$5
	stq	$3,0($16)
	addq	$0,$5,$0
	ret	$31,($26),1

	.end	_gcry_mpih_addmul_1

