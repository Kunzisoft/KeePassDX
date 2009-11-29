/* mc68020 __mpn_mul_1 -- Multiply a limb vector with a limb and store
 *                        the result in a second limb vector.
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
 * mpi_limb_t
 * _gcry_mpih_mul_1( mpi_ptr_t res_ptr,	(sp + 4)
 *		  mpi_ptr_t s1_ptr,	(sp + 8)
 *		  mpi_size_t s1_size,	(sp + 12)
 *		  mpi_limb_t s2_limb)	(sp + 16)
 */


	TEXT
	ALIGN
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_mul_1)

C_SYMBOL_NAME(_gcry_mpih_mul_1:)
PROLOG(_gcry_mpih_mul_1)

#define res_ptr a0
#define s1_ptr a1
#define s1_size d2
#define s2_limb d4

/* Save used registers on the stack.  */
	moveml	R(d2)-R(d4),MEM_PREDEC(sp)
#if 0
	movel	R(d2),MEM_PREDEC(sp)
	movel	R(d3),MEM_PREDEC(sp)
	movel	R(d4),MEM_PREDEC(sp)
#endif

/* Copy the arguments to registers.  Better use movem?	*/
	movel	MEM_DISP(sp,16),R(res_ptr)
	movel	MEM_DISP(sp,20),R(s1_ptr)
	movel	MEM_DISP(sp,24),R(s1_size)
	movel	MEM_DISP(sp,28),R(s2_limb)

	eorw	#1,R(s1_size)
	clrl	R(d1)
	lsrl	#1,R(s1_size)
	bcc	L(L1)
	subql	#1,R(s1_size)
	subl	R(d0),R(d0)	/* (d0,cy) <= (0,0) */

L(Loop:)
	movel	MEM_POSTINC(s1_ptr),R(d3)
	mulul	R(s2_limb),R(d1):R(d3)
	addxl	R(d0),R(d3)
	movel	R(d3),MEM_POSTINC(res_ptr)
L(L1:)	movel	MEM_POSTINC(s1_ptr),R(d3)
	mulul	R(s2_limb),R(d0):R(d3)
	addxl	R(d1),R(d3)
	movel	R(d3),MEM_POSTINC(res_ptr)

	dbf	R(s1_size),L(Loop)
	clrl	R(d3)
	addxl	R(d3),R(d0)
	subl	#0x10000,R(s1_size)
	bcc	L(Loop)

/* Restore used registers from stack frame.  */
	moveml	MEM_POSTINC(sp),R(d2)-R(d4)
#if 0
	movel	MEM_POSTINC(sp),R(d4)
	movel	MEM_POSTINC(sp),R(d3)
	movel	MEM_POSTINC(sp),R(d2)
#endif
	rts
EPILOG(_gcry_mpih_mul_1)


