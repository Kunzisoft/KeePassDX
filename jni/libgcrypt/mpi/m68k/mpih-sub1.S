/* mc68020 __mpn_sub_n -- Subtract two limb vectors of the same length > 0 and
 *                        store difference in a third limb vector.
 *
 *      Copyright (C) 1992, 1994, 1996, 1998, 
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
 *		   mpi_ptr_t s2_ptr,	(sp + 16)
 *		   mpi_size_t size)	(sp + 12)
 */


	TEXT
	ALIGN
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_sub_n)

C_SYMBOL_NAME(_gcry_mpih_sub_n:)
PROLOG(_gcry_mpih_sub_n)
/* Save used registers on the stack.  */
	movel	R(d2),MEM_PREDEC(sp)
	movel	R(a2),MEM_PREDEC(sp)

/* Copy the arguments to registers.  Better use movem?	*/
	movel	MEM_DISP(sp,12),R(a2)
	movel	MEM_DISP(sp,16),R(a0)
	movel	MEM_DISP(sp,20),R(a1)
	movel	MEM_DISP(sp,24),R(d2)

	eorw	#1,R(d2)
	lsrl	#1,R(d2)
	bcc	L(L1)
	subql	#1,R(d2)	/* clears cy as side effect */

L(Loop:)
	movel	MEM_POSTINC(a0),R(d0)
	movel	MEM_POSTINC(a1),R(d1)
	subxl	R(d1),R(d0)
	movel	R(d0),MEM_POSTINC(a2)
L(L1:)	movel	MEM_POSTINC(a0),R(d0)
	movel	MEM_POSTINC(a1),R(d1)
	subxl	R(d1),R(d0)
	movel	R(d0),MEM_POSTINC(a2)

	dbf	R(d2),L(Loop)		/* loop until 16 lsb of %4 == -1 */
	subxl	R(d0),R(d0)	/* d0 <= -cy; save cy as 0 or -1 in d0 */
	subl	#0x10000,R(d2)
	bcs	L(L2)
	addl	R(d0),R(d0)	/* restore cy */
	bra	L(Loop)

L(L2:)
	negl	R(d0)

/* Restore used registers from stack frame.  */
	movel	MEM_POSTINC(sp),R(a2)
	movel	MEM_POSTINC(sp),R(d2)

	rts
EPILOG(_gcry_mpih_sub_n)


