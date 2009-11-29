/* Intel Pentium-4 mpn_lshift -- left shift.
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
 * _gcry_mpih_lshift( mpi_ptr_t wp,	(sp + 4)
 *		   mpi_ptr_t up,	(sp + 8)
 *		   mpi_size_t usize,	(sp + 12)
 *		   unsigned cnt)	(sp + 16)
 *
 * P4 Willamette, Northwood: 1.75 cycles/limb
 * P4 Prescott:		     2.0 cycles/limb
 */

.text
	ALIGN (3)
	.globl C_SYMBOL_NAME(_gcry_mpih_lshift)
C_SYMBOL_NAME(_gcry_mpih_lshift:)

	
	pushl	%ebx
	pushl	%edi


	movl	20(%esp), %eax
	movl	12(%esp), %edx

	movl	16(%esp), %ebx
	movl	24(%esp), %ecx

	cmp	$5, %eax
	jae	.Lunroll

	movl	-4(%ebx,%eax,4), %edi	
	decl	%eax

	jnz	.Lsimple

	shldl	%cl, %edi, %eax	

	shll	%cl, %edi

	movl	%edi, (%edx)		
	popl	%edi			

	popl	%ebx

	ret





.Lsimple:
	
	
	
	
	
	
	


	movd	(%ebx,%eax,4), %mm5	

	movd	%ecx, %mm6		
	negl	%ecx

	psllq	%mm6, %mm5
	addl	$32, %ecx

	movd	%ecx, %mm7
	psrlq	$32, %mm5		


.Lsimple_top:
	
	
	
	
	
	
	
	
	
	
	

	movq	-4(%ebx,%eax,4), %mm0
	decl	%eax

	psrlq	%mm7, %mm0

	

	movd	%mm0, 4(%edx,%eax,4)
	jnz	.Lsimple_top


	movd	(%ebx), %mm0

	movd	%mm5, %eax
	psllq	%mm6, %mm0

	popl	%edi
	popl	%ebx

	movd	%mm0, (%edx)

	emms

	ret





	.align	8, 0x90
.Lunroll:
	
	
	
	
	
	
	


	movd	-4(%ebx,%eax,4), %mm5	
	leal	(%ebx,%eax,4), %edi

	movd	%ecx, %mm6		
	andl	$4, %edi

	psllq	%mm6, %mm5
	jz	.Lstart_src_aligned


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	-8(%ebx,%eax,4), %mm0	

	psllq	%mm6, %mm0
	decl	%eax

	psrlq	$32, %mm0

	

	movd	%mm0, (%edx,%eax,4)
.Lstart_src_aligned:

	movq	-8(%ebx,%eax,4), %mm1	
	leal	(%edx,%eax,4), %edi

	andl	$4, %edi
	psrlq	$32, %mm5		

	movq	-16(%ebx,%eax,4), %mm3	
	jz	.Lstart_dst_aligned

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	%mm1, %mm0
	addl	$32, %ecx		

	psllq	%mm6, %mm0

	movd	%ecx, %mm6
	psrlq	$32, %mm0

	

	movd	%mm0, -4(%edx,%eax,4)
	subl	$4, %edx
.Lstart_dst_aligned:


	psllq	%mm6, %mm1
	negl	%ecx			

	addl	$64, %ecx		
	movq	%mm3, %mm2

	movd	%ecx, %mm7
	subl	$8, %eax		

	psrlq	%mm7, %mm3

	por	%mm1, %mm3		
	jc	.Lfinish


	

	.align	8, 0x90
.Lunroll_loop:
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	movq	8(%ebx,%eax,4), %mm0
	psllq	%mm6, %mm2

	movq	%mm0, %mm1
	psrlq	%mm7, %mm0

	movq	%mm3, 24(%edx,%eax,4)	
	por	%mm2, %mm0

	movq	(%ebx,%eax,4), %mm3	
	psllq	%mm6, %mm1		

	movq	%mm0, 16(%edx,%eax,4)
	movq	%mm3, %mm2		

	psrlq	%mm7, %mm3		
	subl	$4, %eax

	por	%mm1, %mm3		
	jnc	.Lunroll_loop



.Lfinish:
	

	testb	$2, %al

	jz	.Lfinish_no_two

	movq	8(%ebx,%eax,4), %mm0
	psllq	%mm6, %mm2

	movq	%mm0, %mm1
	psrlq	%mm7, %mm0

	movq	%mm3, 24(%edx,%eax,4)	
	por	%mm2, %mm0

	movq	%mm1, %mm2
	movq	%mm0, %mm3

	subl	$2, %eax
.Lfinish_no_two:


	
	
	
	

	testb	$1, %al
	movd	%mm5, %eax	

	popl	%edi
	jz	.Lfinish_zero


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	
	


	movd	(%ebx), %mm0
	psllq	%mm6, %mm2

	movq	%mm3, 12(%edx)
	psllq	$32, %mm0

	movq	%mm0, %mm1
	psrlq	%mm7, %mm0

	por	%mm2, %mm0
	psllq	%mm6, %mm1

	movq	%mm0, 4(%edx)
	psrlq	$32, %mm1

	andl	$32, %ecx
	popl	%ebx

	jz	.Lfinish_one_unaligned

	movd	%mm1, (%edx)
.Lfinish_one_unaligned:

	emms

	ret




.Lfinish_zero:

	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	
	
	
	
	
	
	
	
	
	
	
	
	


	
	


	movq	%mm3, 8(%edx)
	andl	$32, %ecx

	psllq	%mm6, %mm2
	jz	.Lfinish_zero_unaligned

	movq	%mm2, (%edx)
.Lfinish_zero_unaligned:

	psrlq	$32, %mm2
	popl	%ebx

	movd	%mm5, %eax	

	movd	%mm2, 4(%edx)

	emms

	ret
