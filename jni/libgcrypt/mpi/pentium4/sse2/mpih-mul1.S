/* Intel Pentium-4 mpn_mul_1 -- Multiply a limb vector with a limb and store
 * the result in a second limb vector.
 *
 * Copyright 2001, 2002, 2003, 2005 Free Software Foundation, Inc.
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


#include "sysdep.h"
#include "asm-syntax.h"


/*******************
 * mpi_limb_t
 * _gcry_mpih_mul_1( mpi_ptr_t res_ptr,	(sp + 4)
 *		  mpi_ptr_t s1_ptr,	(sp + 8)
 *		  mpi_size_t s1_size,	(sp + 12)
 *		  mpi_limb_t s2_limb)	(sp + 16)
 *
 *                           src != dst      src == dst
 * P6 model 9  (Banias)          ?.?
 * P6 model 13 (Dothan)          4.75            4.75
 * P4 model 0  (Willamette)      4.0             6.0
 * P4 model 1  (?)               4.0             6.0
 * P4 model 2  (Northwood)       4.0             6.0
 * P4 model 3  (Prescott)        ?.?             ?.?
 * P4 model 4  (Nocona)          ?.?             ?.?
 * Unfortunately when src==dst the write-combining described in
 * pentium4/README takes us up to 6 c/l.
 *
 */

	TEXT
	ALIGN (3)
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_mul_1)
C_SYMBOL_NAME(_gcry_mpih_mul_1:); 

	pxor	%mm0, %mm0

.Lstart_1c:
	movl	8(%esp), %eax
	movd	16(%esp), %mm7
	movl	4(%esp), %edx
	movl	12(%esp), %ecx

.Ltop:

/*
	C eax	src, incrementing
	C ebx
	C ecx	counter, size iterations
	C edx	dst, incrementing
	C
	C mm0	carry limb
	C mm7	multiplier
*/
	
	movd	(%eax), %mm1
	addl	$4, %eax
	pmuludq	%mm7, %mm1

	paddq	%mm1, %mm0
	movd	%mm0, (%edx)
	addl	$4, %edx

	psrlq	$32, %mm0

	subl	$1, %ecx
	jnz	.Ltop


	movd	%mm0, %eax
	emms
	ret

