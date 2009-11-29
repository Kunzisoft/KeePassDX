/* HP-PA  __udiv_qrnnd division support, used from longlong.h.
 *	  This version runs fast on PA 7000 and later.
 *
 *      Copyright (C) 1993, 1994, 1998, 
 *                    2001, 2002, 2004 Free Software Foundation, Inc.
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



/* INPUT PARAMETERS
 *   rem_ptr	   gr26
 *   n1 	   gr25
 *   n0 	   gr24
 *   d		   gr23
 */

	.level		1.1

        .data
	.align		8
	.label L$0000
	.word		0x43f00000
	.word		0x0
        .code
	.export 	__udiv_qrnnd
	.label		__udiv_qrnnd
	.proc
	.callinfo	frame=64,no_calls
	.entry
	ldo		64(%r30),%r30

	stws		%r25,-16(0,%r30)	; n_hi
	stws		%r24,-12(0,%r30)	; n_lo
        stw             %r19,-32(%r30)
        addil           LT%L$0000,%r19
        ldw             RT%L$0000(%r1),%r1
	fldds		-16(0,%r30),%fr5
	stws		%r23,-12(0,%r30)
	comib,<=	0,%r25,L$1
	fcnvxf,dbl,dbl	%fr5,%fr5
        fldds           0(0,%r1),%fr4
	fadd,dbl	%fr4,%fr5,%fr5
	.label	L$1
	fcpy,sgl	%fr0,%fr6L
	fldws		-12(0,%r30),%fr6R
	fcnvxf,dbl,dbl	%fr6,%fr4

	fdiv,dbl	%fr5,%fr4,%fr5

	fcnvfx,dbl,dbl	%fr5,%fr4
	fstws		%fr4R,-16(%r30)
	xmpyu		%fr4R,%fr6R,%fr6
	ldws		-16(%r30),%r28
	fstds		%fr6,-16(0,%r30)
	ldws		-12(0,%r30),%r21
	ldws		-16(0,%r30),%r20
	sub		%r24,%r21,%r22
        subb            %r25,%r20,%r1
        comib,=         0,%r1,L$2
	ldo		-64(%r30),%r30

	add		%r22,%r23,%r22
	ldo		-1(%r28),%r28
	.label	L$2
	bv		0(%r2)
	stws		%r22,0(0,%r26)

	.exit
	.procend

