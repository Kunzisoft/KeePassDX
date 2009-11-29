/* hppa   sub_n -- Sub two limb vectors of the same length > 0 and store
 *		   sum in a third limb vector.
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
 *  _gcry_mpih_sub_n( mpi_ptr_t res_ptr,	(gr26)
 *		   mpi_ptr_t s1_ptr,	(gr25)
 *		   mpi_ptr_t s2_ptr,	(gr24)
 *		   mpi_size_t size)	(gr23)
 *
 * One might want to unroll this as for other processors, but it turns
 * out that the data cache contention after a store makes such
 * unrolling useless.  We can't come under 5 cycles/limb anyway.
 */


	.code
	.export 	_gcry_mpih_sub_n
	.label		_gcry_mpih_sub_n
	.proc
	.callinfo	frame=0,no_calls
	.entry

	ldws,ma 	4(0,%r25),%r20
	ldws,ma 	4(0,%r24),%r19

	addib,= 	-1,%r23,L$end	; check for (SIZE == 1)
	 sub		%r20,%r19,%r28	; subtract first limbs ignoring cy

	.label	L$loop
	ldws,ma 	4(0,%r25),%r20
	ldws,ma 	4(0,%r24),%r19
	stws,ma 	%r28,4(0,%r26)
	addib,<>	-1,%r23,L$loop
	 subb		%r20,%r19,%r28

	.label	L$end
	stws		%r28,0(0,%r26)
	addc		%r0,%r0,%r28
	bv		0(%r2)
	 subi		1,%r28,%r28

	.exit
	.procend



