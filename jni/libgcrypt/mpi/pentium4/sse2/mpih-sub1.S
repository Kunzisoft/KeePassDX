/* Intel Pentium-4 mpn_sub_n -- mpn subtraction.
 *
 * Copyright 2001, 2002 Free Software Foundation, Inc.
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
 *  mpi_limb_t
 *  _gcry_mpih_sub_n( mpi_ptr_t res_ptr,	(sp + 4)
 *		   mpi_ptr_t s1_ptr,	(sp + 8)
 *		   mpi_ptr_t s2_ptr,	(sp + 12)
 *		   mpi_size_t size)	(sp + 16)
 *
 * P4 Willamette, Northwood: 4.0 cycles/limb if dst!=src1 and dst!=src2
 *			     6.0 cycles/limb if dst==src1 or dst==src2
 * P4 Prescott:		     >= 5 cycles/limb
 *
 * The main loop code is 2x unrolled so that the carry bit can alternate
 * between mm0 and mm1.
 */


.text
	ALIGN (3)
	.globl C_SYMBOL_NAME(_gcry_mpih_sub_n)
C_SYMBOL_NAME(_gcry_mpih_sub_n:)

	pxor	%mm0, %mm0
.Lstart_nc:
	movl	8(%esp), %eax
	movl	%ebx, 8(%esp)
	movl	12(%esp), %ebx
	movl	4(%esp), %edx
	movl	16(%esp), %ecx

	leal	(%eax,%ecx,4), %eax	
	leal	(%ebx,%ecx,4), %ebx	
	leal	(%edx,%ecx,4), %edx	
	negl	%ecx			

.Ltop:
/*
	C eax	src1 end
	C ebx	src2 end
	C ecx	counter, limbs, negative
	C edx	dst end
	C mm0	carry bit
*/

	movd	(%eax,%ecx,4), %mm1
	movd	(%ebx,%ecx,4), %mm2
	psubq	%mm2, %mm1

	psubq	%mm0, %mm1
	movd	%mm1, (%edx,%ecx,4)

	psrlq	$63, %mm1

	addl	$1, %ecx
	jz	.Ldone_mm1

	movd	(%eax,%ecx,4), %mm0
	movd	(%ebx,%ecx,4), %mm2
	psubq	%mm2, %mm0

	psubq	%mm1, %mm0
	movd	%mm0, (%edx,%ecx,4)

	psrlq	$63, %mm0

	addl	$1, %ecx
	jnz	.Ltop


	movd	%mm0, %eax
	movl	8(%esp), %ebx
	emms
	ret



.Ldone_mm1:
	movd	%mm1, %eax
	movl	8(%esp), %ebx
	emms
	ret
