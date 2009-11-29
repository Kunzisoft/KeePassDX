/* i80386   lshift
 *      Copyright (C) 1992, 1994, 1998,
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
 * mpi_limb_t
 * _gcry_mpih_lshift( mpi_ptr_t wp,	(sp + 4)
 *		   mpi_ptr_t up,	(sp + 8)
 *		   mpi_size_t usize,	(sp + 12)
 *		   unsigned cnt)	(sp + 16)
 */

.text
	ALIGN (3)
	.globl C_SYMBOL_NAME(_gcry_mpih_lshift)
C_SYMBOL_NAME(_gcry_mpih_lshift:)
	pushl	%edi
	pushl	%esi
	pushl	%ebx

	movl	16(%esp),%edi		/* res_ptr */
	movl	20(%esp),%esi		/* s_ptr */
	movl	24(%esp),%edx		/* size */
	movl	28(%esp),%ecx		/* cnt */

	subl	$4,%esi 		/* adjust s_ptr */

	movl	(%esi,%edx,4),%ebx	/* read most significant limb */
	xorl	%eax,%eax
	shldl	%cl,%ebx,%eax		/* compute carry limb */
	decl	%edx
	jz	Lend
	pushl	%eax			/* push carry limb onto stack */
	testb	$1,%dl
	jnz	L1			/* enter loop in the middle */
	movl	%ebx,%eax

	ALIGN (3)
Loop:	movl	(%esi,%edx,4),%ebx	/* load next lower limb */
	shldl	%cl,%ebx,%eax		/* compute result limb */
	movl	%eax,(%edi,%edx,4)	/* store it */
	decl	%edx
L1:	movl	(%esi,%edx,4),%eax
	shldl	%cl,%eax,%ebx
	movl	%ebx,(%edi,%edx,4)
	decl	%edx
	jnz	Loop

	shll	%cl,%eax		/* compute least significant limb */
	movl	%eax,(%edi)		/* store it */

	popl	%eax			/* pop carry limb */

	popl	%ebx
	popl	%esi
	popl	%edi
	ret

Lend:	shll	%cl,%ebx		/* compute least significant limb */
	movl	%ebx,(%edi)		/* store it */

	popl	%ebx
	popl	%esi
	popl	%edi
	ret

