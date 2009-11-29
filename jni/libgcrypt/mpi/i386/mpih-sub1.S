/* i80386 sub_n -- Sub two limb vectors of the same length > 0 and store
 *		   sum in a third limb vector.
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
 */


.text
	ALIGN (3)
	.globl C_SYMBOL_NAME(_gcry_mpih_sub_n)
C_SYMBOL_NAME(_gcry_mpih_sub_n:)
	pushl %edi
	pushl %esi

	movl 12(%esp),%edi		/* res_ptr */
	movl 16(%esp),%esi		/* s1_ptr */
	movl 20(%esp),%edx		/* s2_ptr */
	movl 24(%esp),%ecx		/* size */

	movl	%ecx,%eax
	shrl	$3,%ecx 		/* compute count for unrolled loop */
	negl	%eax
	andl	$7,%eax 		/* get index where to start loop */
	jz	Loop			/* necessary special case for 0 */
	incl	%ecx			/* adjust loop count */
	shll	$2,%eax 		/* adjustment for pointers... */
	subl	%eax,%edi		/* ... since they are offset ... */
	subl	%eax,%esi		/* ... by a constant when we ... */
	subl	%eax,%edx		/* ... enter the loop */
	shrl	$2,%eax 		/* restore previous value */
#ifdef PIC
/* Calculate start address in loop for PIC.  Due to limitations in some
   assemblers, Loop-L0-3 cannot be put into the leal */
	call	L0
L0:	leal	(%eax,%eax,8),%eax
	addl	(%esp),%eax
	addl	$(Loop-L0-3),%eax
	addl	$4,%esp
#else
/* Calculate start address in loop for non-PIC.  */
	leal	(Loop - 3)(%eax,%eax,8),%eax
#endif
	jmp	*%eax			/* jump into loop */
	ALIGN (3)
Loop:	movl	(%esi),%eax
	sbbl	(%edx),%eax
	movl	%eax,(%edi)
	movl	4(%esi),%eax
	sbbl	4(%edx),%eax
	movl	%eax,4(%edi)
	movl	8(%esi),%eax
	sbbl	8(%edx),%eax
	movl	%eax,8(%edi)
	movl	12(%esi),%eax
	sbbl	12(%edx),%eax
	movl	%eax,12(%edi)
	movl	16(%esi),%eax
	sbbl	16(%edx),%eax
	movl	%eax,16(%edi)
	movl	20(%esi),%eax
	sbbl	20(%edx),%eax
	movl	%eax,20(%edi)
	movl	24(%esi),%eax
	sbbl	24(%edx),%eax
	movl	%eax,24(%edi)
	movl	28(%esi),%eax
	sbbl	28(%edx),%eax
	movl	%eax,28(%edi)
	leal	32(%edi),%edi
	leal	32(%esi),%esi
	leal	32(%edx),%edx
	decl	%ecx
	jnz	Loop

	sbbl	%eax,%eax
	negl	%eax

	popl %esi
	popl %edi
	ret

