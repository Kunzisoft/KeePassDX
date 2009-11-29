/* Intel Pentium-4 mpn_rshift -- right shift.
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
 * _gcry_mpih_rshift( mpi_ptr_t wp,	(sp + 4)
 *		   mpi_ptr_t up,	(sp + 8)
 *		   mpi_size_t usize,	(sp + 12)
 *		   unsigned cnt)	(sp + 16)
 *
 * P4 Willamette, Northwood: 1.75 cycles/limb
 * P4 Prescott:		     2.0 cycles/limb
 */

.text
	ALIGN (3)
	.globl C_SYMBOL_NAME(_gcry_mpih_rshift)
C_SYMBOL_NAME(_gcry_mpih_rshift:)
	pushl	%ebx
	pushl	%edi


	movl	20(%esp), %eax
	movl	12(%esp), %edx

	movl	16(%esp), %ebx
	movl	24(%esp), %ecx

	cmp	$5, %eax
	jae	.Lunroll

	decl	%eax
	movl	(%ebx), %edi		

	jnz	.Lsimple

	shrdl	%cl, %edi, %eax	

	shrl	%cl, %edi

	movl	%edi, (%edx)		
	popl	%edi			

	popl	%ebx

	ret





	.align	8, 0x90
.Lsimple:
	
	
	
	
	
	
	


	movd	(%ebx), %mm5		
	leal	(%ebx,%eax,4), %ebx	

	movd	%ecx, %mm6		
	leal	-4(%edx,%eax,4), %edx	

	psllq	$32, %mm5
	negl	%eax







.Lsimple_top:
	
	
	
	
	
	
	
	

	movq	(%ebx,%eax,4), %mm0
	incl	%eax

	psrlq	%mm6, %mm0

	movd	%mm0, (%edx,%eax,4)
	jnz	.Lsimple_top


	movd	(%ebx), %mm0
	psrlq	%mm6, %mm5		

	psrlq	%mm6, %mm0
	popl	%edi

	movd	%mm5, %eax
	popl	%ebx

	movd	%mm0, 4(%edx)

	emms

	ret





	.align	8, 0x90
.Lunroll:
	
	
	
	
	
	
	


	movd	(%ebx), %mm5		
	movl	$4, %edi

	movd	%ecx, %mm6		
	testl	%edi, %ebx

	psllq	$32, %mm5
	jz	.Lstart_src_aligned


	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	(%ebx), %mm0		

	psrlq	%mm6, %mm0
	addl	$4, %ebx

	decl	%eax

	movd	%mm0, (%edx)
	addl	$4, %edx
.Lstart_src_aligned:


	movq	(%ebx), %mm1
	testl	%edi, %edx

	psrlq	%mm6, %mm5		
	jz	.Lstart_dst_aligned

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	%mm1, %mm0
	addl	$32, %ecx		

	psrlq	%mm6, %mm0

	movd	%ecx, %mm6

	movd	%mm0, (%edx)
	addl	$4, %edx
.Lstart_dst_aligned:


	movq	8(%ebx), %mm3
	negl	%ecx

	movq	%mm3, %mm2		
	addl	$64, %ecx

	movd	%ecx, %mm7
	psrlq	%mm6, %mm1

	leal	-12(%ebx,%eax,4), %ebx
	leal	-20(%edx,%eax,4), %edx

	psllq	%mm7, %mm3
	subl	$7, %eax		

	por	%mm1, %mm3		
	negl	%eax			

	jns	.Lfinish


	
	
	
	
	
	
	
	
	
	
	
	

	.align	8, 0x90
.Lunroll_loop:
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	(%ebx,%eax,4), %mm0
	psrlq	%mm6, %mm2

	movq	%mm0, %mm1
	psllq	%mm7, %mm0

	movq	%mm3, -8(%edx,%eax,4)	
	por	%mm2, %mm0

	movq	8(%ebx,%eax,4), %mm3	
	psrlq	%mm6, %mm1		

	movq	%mm0, (%edx,%eax,4)
	movq	%mm3, %mm2		

	psllq	%mm7, %mm3		
	addl	$4, %eax

	por	%mm1, %mm3		
	js	.Lunroll_loop


.Lfinish:
	

	testb	$2, %al

	jnz	.Lfinish_no_two

	movq	(%ebx,%eax,4), %mm0
	psrlq	%mm6, %mm2

	movq	%mm0, %mm1
	psllq	%mm7, %mm0

	movq	%mm3, -8(%edx,%eax,4)	
	por	%mm2, %mm0

	movq	%mm1, %mm2
	movq	%mm0, %mm3

	addl	$2, %eax
.Lfinish_no_two:


	
	
	
	

	testb	$1, %al
	popl	%edi

	movd	%mm5, %eax	
	jnz	.Lfinish_zero


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	


	movd	8(%ebx), %mm0
	psrlq	%mm6, %mm2

	movq	%mm0, %mm1
	psllq	%mm7, %mm0

	movq	%mm3, (%edx)
	por	%mm2, %mm0

	psrlq	%mm6, %mm1
	andl	$32, %ecx

	popl	%ebx
	jz	.Lfinish_one_unaligned

	
	movd	%mm1, 16(%edx)
.Lfinish_one_unaligned:

	movq	%mm0, 8(%edx)

	emms

	ret




.Lfinish_zero:

	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	


	movq	%mm3, 4(%edx)
	psrlq	%mm6, %mm2

	movd	%mm2, 12(%edx)
	andl	$32, %ecx

	popl	%ebx
	jz	.Lfinish_zero_unaligned

	movq	%mm2, 12(%edx)
.Lfinish_zero_unaligned:

	emms

	ret
