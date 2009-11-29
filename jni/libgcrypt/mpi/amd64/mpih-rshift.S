/* AMD64 (x86_64) rshift -- Right shift a limb vector and store
 *		  result in a second limb vector.
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
 * mpi_limb_t
 * _gcry_mpih_rshift( mpi_ptr_t wp,	rdi
 *		   mpi_ptr_t up,	rsi
 *		   mpi_size_t usize,	rdx
 *		   unsigned cnt)	rcx
 */

.text
	.globl C_SYMBOL_NAME(_gcry_mpih_rshift)
C_SYMBOL_NAME(_gcry_mpih_rshift:)
	movq	(%rsi), %mm7
	movd	%ecx, %mm1
	movl	$64, %eax
	subl	%ecx, %eax
	movd	%eax, %mm0
	movq	%mm7, %mm3
	psllq	%mm0, %mm7
	movd	%mm7, %rax
	leaq	(%rsi,%rdx,8), %rsi
	leaq	(%rdi,%rdx,8), %rdi
	negq	%rdx
	addq	$2, %rdx
	jg	.Lendo

	ALIGN(8)			/* minimal alignment for claimed speed */
.Loop:	movq	-8(%rsi,%rdx,8), %mm6
	movq	%mm6, %mm2
	psllq	%mm0, %mm6
	psrlq	%mm1, %mm3
	por	%mm6, %mm3
	movq	%mm3, -16(%rdi,%rdx,8)
	je	.Lende
	movq	(%rsi,%rdx,8), %mm7
	movq	%mm7, %mm3
	psllq	%mm0, %mm7
	psrlq	%mm1, %mm2
	por	%mm7, %mm2
	movq	%mm2, -8(%rdi,%rdx,8)
	addq	$2, %rdx
	jle	.Loop

.Lendo:	movq	%mm3, %mm2
.Lende:	psrlq	%mm1, %mm2
	movq	%mm2, -8(%rdi)
	emms
	ret
