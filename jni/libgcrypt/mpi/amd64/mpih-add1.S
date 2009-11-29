/* AMD64 (x86_64) add_n -- Add two limb vectors of the same length > 0 and store
 *		   sum in a third limb vector.
 *
 *      Copyright (C) 1992, 1994, 1995, 1998, 
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
 *  mpi_limb_t
 *  _gcry_mpih_add_n( mpi_ptr_t res_ptr,	rdi
 *		   mpi_ptr_t s1_ptr,		rsi
 *		   mpi_ptr_t s2_ptr,		rdx
 *		   mpi_size_t size)		rcx
 */

.text
	.globl C_SYMBOL_NAME(_gcry_mpih_add_n)
C_SYMBOL_NAME(_gcry_mpih_add_n:)
	leaq	(%rsi,%rcx,8), %rsi
	leaq	(%rdi,%rcx,8), %rdi
	leaq	(%rdx,%rcx,8), %rdx
	negq	%rcx
	xorl	%eax, %eax		/* clear cy */

	ALIGN(4)			/* minimal alignment for claimed speed */
.Loop:	movq	(%rsi,%rcx,8), %rax
	movq	(%rdx,%rcx,8), %r10
	adcq	%r10, %rax
	movq	%rax, (%rdi,%rcx,8)
	incq	%rcx
	jne	.Loop

	movq	%rcx, %rax		/* zero %rax */
	adcq	%rax, %rax
	ret
	