/* Intel Pentium-4 mpn_submul_1 -- Multiply a limb vector with a limb and
 * subtract the result from a second limb vector.
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
 * mpi_limb_t
 * _gcry_mpih_submul_1( mpi_ptr_t res_ptr,      (sp + 4)
 *		     mpi_ptr_t s1_ptr,	     (sp + 8)
 *		     mpi_size_t s1_size,     (sp + 12)
 *		     mpi_limb_t s2_limb)     (sp + 16)
 *
 * P4: 7 cycles/limb, unstable timing, at least on early Pentium4 silicon
 *    (stepping 10).
 *
 * This code is not particularly good at 7 c/l.  The dependent chain is only
 * 4 c/l and there's only 4 MMX unit instructions, so it's not clear why that
 * speed isn't achieved.
 *
 * The arrangements made here to get a two instruction dependent chain are
 * slightly subtle.  In the loop the carry (or borrow rather) is a negative
 * so that a paddq can be used to give a low limb ready to store, and a high
 * limb ready to become the new carry after a psrlq.
 *
 * If the carry was a simple twos complement negative then the psrlq shift
 * would need to bring in 0 bits or 1 bits according to whether the high was
 * zero or non-zero, since a non-zero value would represent a negative
 * needing sign extension.  That wouldn't be particularly easy to arrange and
 * certainly would add an instruction to the dependent chain, so instead an
 * offset is applied so that the high limb will be 0xFFFFFFFF+c.  With c in
 * the range -0xFFFFFFFF to 0, the value 0xFFFFFFFF+c is in the range 0 to
 * 0xFFFFFFFF and is therefore always positive and can always have 0 bits
 * shifted in, which is what psrlq does.
 *
 * The extra 0xFFFFFFFF must be subtracted before c is used, but that can be
 * done off the dependent chain.  The total adjustment then is to add
 * 0xFFFFFFFF00000000 to offset the new carry, and subtract
 * 0x00000000FFFFFFFF to remove the offset from the current carry, for a net
 * add of 0xFFFFFFFE00000001.  In the code this is applied to the destination
 * limb when fetched.
 *
 * It's also possible to view the 0xFFFFFFFF adjustment as a ones-complement
 * negative, which is how it's undone for the return value, but that doesn't
 * seem as clear.
*/

	TEXT
	ALIGN (4)
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_submul_1)
C_SYMBOL_NAME(_gcry_mpih_submul_1:)

	pxor	%mm1, %mm1		

.Lstart_1c:
	movl	8(%esp), %eax
	pcmpeqd	%mm0, %mm0

	movd	16(%esp), %mm7
	pcmpeqd	%mm6, %mm6

	movl	4(%esp), %edx
	psrlq	$32, %mm0		

	movl	12(%esp), %ecx
	psllq	$32, %mm6		

	psubq	%mm0, %mm6		

	psubq	%mm1, %mm0		

/*
	C eax	src, incrementing
	C ebx
	C ecx	loop counter, decrementing
	C edx	dst, incrementing
	C
	C mm0	0xFFFFFFFF - borrow
	C mm6	0xFFFFFFFE00000001
	C mm7	multiplier
*/
	
.Lloop:
	movd	(%eax), %mm1		
	leal	4(%eax), %eax
	movd	(%edx), %mm2		
	paddq	%mm6, %mm2		
	pmuludq	%mm7, %mm1
	psubq	%mm1, %mm2		
	paddq	%mm2, %mm0		
	subl	$1, %ecx
	movd	%mm0, (%edx)		
	psrlq	$32, %mm0
	leal	4(%edx), %edx
	jnz	.Lloop

	movd	%mm0, %eax
	notl	%eax
	emms
	ret
