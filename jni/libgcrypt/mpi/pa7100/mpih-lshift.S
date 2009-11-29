/* hppa   lshift
 *	  optimized for the PA7100, where it runs at 3.25 cycles/limb
 * 
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
 */



/*******************
 * mpi_limb_t
 * _gcry_mpih_lshift( mpi_ptr_t wp,	(gr26)
 *		   mpi_ptr_t up,	(gr25)
 *		   mpi_size_t usize,	(gr24)
 *		   unsigned cnt)	(gr23)
 */

	.code
	.export 	_gcry_mpih_lshift
	.label		_gcry_mpih_lshift
	.proc
	.callinfo	frame=64,no_calls
	.entry

	sh2add		%r24,%r25,%r25
	sh2add		%r24,%r26,%r26
	ldws,mb 	-4(0,%r25),%r22
	subi		32,%r23,%r1
	mtsar		%r1
	addib,= 	-1,%r24,L$0004
	vshd		%r0,%r22,%r28		; compute carry out limb
	ldws,mb 	-4(0,%r25),%r29
	addib,<=	-5,%r24,L$rest
	vshd		%r22,%r29,%r20

	.label	L$loop
	ldws,mb 	-4(0,%r25),%r22
	stws,mb 	%r20,-4(0,%r26)
	vshd		%r29,%r22,%r20
	ldws,mb 	-4(0,%r25),%r29
	stws,mb 	%r20,-4(0,%r26)
	vshd		%r22,%r29,%r20
	ldws,mb 	-4(0,%r25),%r22
	stws,mb 	%r20,-4(0,%r26)
	vshd		%r29,%r22,%r20
	ldws,mb 	-4(0,%r25),%r29
	stws,mb 	%r20,-4(0,%r26)
	addib,> 	-4,%r24,L$loop
	vshd		%r22,%r29,%r20

	.label	L$rest
	addib,= 	4,%r24,L$end1
	nop
	.label	L$eloop
	ldws,mb 	-4(0,%r25),%r22
	stws,mb 	%r20,-4(0,%r26)
	addib,<=	-1,%r24,L$end2
	vshd		%r29,%r22,%r20
	ldws,mb 	-4(0,%r25),%r29
	stws,mb 	%r20,-4(0,%r26)
	addib,> 	-1,%r24,L$eloop
	vshd		%r22,%r29,%r20

	.label	L$end1
	stws,mb 	%r20,-4(0,%r26)
	vshd		%r29,%r0,%r20
	bv		0(%r2)
	stw		%r20,-4(0,%r26)
	.label	L$end2
	stws,mb 	%r20,-4(0,%r26)
	.label	L$0004
	vshd		%r22,%r0,%r20
	bv		0(%r2)
	stw		%r20,-4(0,%r26)

	.exit
	.procend



