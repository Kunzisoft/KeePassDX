/* Intel Pentium-4 mpn_addmul_1 -- Multiply a limb vector with a limb and add
 * the result to a second limb vector.
 *
 * Copyright 2001, 2002, 2004, 2005 Free Software Foundation, Inc.
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
 * _gcry_mpih_addmul_1( mpi_ptr_t res_ptr,      (sp + 4)
 *		     mpi_ptr_t s1_ptr,	     (sp + 8)
 *		     mpi_size_t s1_size,     (sp + 12)
 *		     mpi_limb_t s2_limb)     (sp + 16)
 *
 * P3 model 9  (Banias)          ?.?
 * P3 model 13 (Dothan)          5.8
 * P4 model 0  (Willamette)      5.5
 * P4 model 1  (?)               5.5
 * P4 model 2  (Northwood)       5.5
 * P4 model 3  (Prescott)        6.0
 * P4 model 4  (Nocona)
 *
 * Only the carry limb propagation is on the dependent chain, but some other
 * Pentium4 pipeline magic brings down performance to 6 cycles/l from the
 * ideal 4 cycles/l.
 */


	TEXT
	ALIGN (4)
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_addmul_1)
C_SYMBOL_NAME(_gcry_mpih_addmul_1:)

	pxor	%mm4, %mm4
.Lstart_1c:
	movl	8(%esp), %eax
	movl	12(%esp), %ecx
	movl	4(%esp), %edx
	movd	16(%esp), %mm7

/*
	C eax	src, incrementing ; 5B
	C ecx	loop counter, decrementing
	C edx	dst, incrementing
	C
	C mm4	carry, low 32-bits
	C mm7	multiplier
*/

	movd		(%eax), %mm2	
	pmuludq		%mm7, %mm2

	shrl	$1, %ecx
	jnc	.Leven

	leal		4(%eax), %eax
	movd		(%edx), %mm1
	paddq		%mm2, %mm1
	paddq		%mm1, %mm4
	movd		%mm4, (%edx)
	psrlq		$32, %mm4

	testl	%ecx, %ecx
	jz	.Lrtn
	leal	4(%edx), %edx

	movd		(%eax), %mm2	
	pmuludq		%mm7, %mm2
.Leven:
	movd		4(%eax), %mm0	
	movd		(%edx), %mm1	
	pmuludq		%mm7, %mm0

	subl	$1, %ecx
	jz	.Lend
.Lloop:
	paddq		%mm2, %mm1	
	movd		8(%eax), %mm2	
	paddq		%mm1, %mm4	
	movd		4(%edx), %mm3	
	pmuludq		%mm7, %mm2
	movd		%mm4, (%edx)
	psrlq		$32, %mm4

	paddq		%mm0, %mm3	
	movd		12(%eax), %mm0	
	paddq		%mm3, %mm4	
	movd		8(%edx), %mm1	
	pmuludq		%mm7, %mm0
	movd		%mm4, 4(%edx)
	psrlq		$32, %mm4

	leal	8(%eax), %eax
	leal	8(%edx), %edx
	subl	$1, %ecx
	jnz	.Lloop
.Lend:
	paddq		%mm2, %mm1	
	paddq		%mm1, %mm4	
	movd		4(%edx), %mm3	
	movd		%mm4, (%edx)
	psrlq		$32, %mm4
	paddq		%mm0, %mm3	
	paddq		%mm3, %mm4	
	movd		%mm4, 4(%edx)
	psrlq		$32, %mm4
.Lrtn:
	movd	%mm4, %eax
	emms
	ret
