/* AMD64 submul_1 -- Multiply a limb vector with a limb and add
 *		      the result to a second limb vector.
 *
 *      Copyright (C) 1992, 1994, 1998, 
 *                    2001, 2002, 2006 Free Software Foundation, Inc.
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
 * _gcry_mpih_submul_1( mpi_ptr_t res_ptr,   (rdi)
 *		     mpi_ptr_t s1_ptr,	     (rsi)
 *		     mpi_size_t s1_size,     (rdx)
 *		     mpi_limb_t s2_limb)     (rcx)
 */
	TEXT
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_submul_1)
C_SYMBOL_NAME(_gcry_mpih_submul_1:)

	movq	%rdx, %r11
	leaq	(%rsi,%r11,8), %rsi
	leaq	(%rdi,%r11,8), %rdi
	negq	%r11
	xorl	%r8d, %r8d

	ALIGN(3)			/* minimal alignment for claimed speed */
.Loop:	movq	(%rsi,%r11,8), %rax
	movq	(%rdi,%r11,8), %r10
	mulq	%rcx
	subq	%r8, %r10
	movl	$0, %r8d
	adcl	%r8d, %r8d
	subq	%rax, %r10
	adcq	%rdx, %r8
	movq	%r10, (%rdi,%r11,8)
	incq	%r11
	jne	.Loop

	movq	%r8, %rax
	ret
