/* mc68020 lshift -- Shift left a low-level natural-number integer.
 *
 *      Copyright (C) 1996, 1998, 2001, 2002 Free Software Foundation, Inc.
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

#define res_ptr a1
#define s_ptr a0
#define s_size d6
#define cnt d4

	TEXT
	ALIGN
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_lshift)

C_SYMBOL_NAME(_gcry_mpih_lshift:)
PROLOG(_gcry_mpih_lshift)

	/* Save used registers on the stack.  */
	moveml	R(d2)-R(d6)/R(a2),MEM_PREDEC(sp)

	/* Copy the arguments to registers.  */
	movel	MEM_DISP(sp,28),R(res_ptr)
	movel	MEM_DISP(sp,32),R(s_ptr)
	movel	MEM_DISP(sp,36),R(s_size)
	movel	MEM_DISP(sp,40),R(cnt)

	moveql	#1,R(d5)
	cmpl	R(d5),R(cnt)
	bne	L(Lnormal)
	cmpl	R(s_ptr),R(res_ptr)
	bls	L(Lspecial)		/* jump if s_ptr >= res_ptr */
#if (defined (__mc68020__) || defined (__NeXT__) || defined(mc68020))
	lea	MEM_INDX1(s_ptr,s_size,l,4),R(a2)
#else /* not mc68020 */
	movel	R(s_size),R(d0)
	asll	#2,R(d0)
	lea	MEM_INDX(s_ptr,d0,l),R(a2)
#endif
	cmpl	R(res_ptr),R(a2)
	bls	L(Lspecial)		/* jump if res_ptr >= s_ptr + s_size */

L(Lnormal:)
	moveql	#32,R(d5)
	subl	R(cnt),R(d5)

#if (defined (__mc68020__) || defined (__NeXT__) || defined(mc68020))
	lea	MEM_INDX1(s_ptr,s_size,l,4),R(s_ptr)
	lea	MEM_INDX1(res_ptr,s_size,l,4),R(res_ptr)
#else /* not mc68000 */
	movel	R(s_size),R(d0)
	asll	#2,R(d0)
	addl	R(s_size),R(s_ptr)
	addl	R(s_size),R(res_ptr)
#endif
	movel	MEM_PREDEC(s_ptr),R(d2)
	movel	R(d2),R(d0)
	lsrl	R(d5),R(d0)		/* compute carry limb */

	lsll	R(cnt),R(d2)
	movel	R(d2),R(d1)
	subql	#1,R(s_size)
	beq	L(Lend)
	lsrl	#1,R(s_size)
	bcs	L(L1)
	subql	#1,R(s_size)

L(Loop:)
	movel	MEM_PREDEC(s_ptr),R(d2)
	movel	R(d2),R(d3)
	lsrl	R(d5),R(d3)
	orl	R(d3),R(d1)
	movel	R(d1),MEM_PREDEC(res_ptr)
	lsll	R(cnt),R(d2)
L(L1:)
	movel	MEM_PREDEC(s_ptr),R(d1)
	movel	R(d1),R(d3)
	lsrl	R(d5),R(d3)
	orl	R(d3),R(d2)
	movel	R(d2),MEM_PREDEC(res_ptr)
	lsll	R(cnt),R(d1)

	dbf	R(s_size),L(Loop)
	subl	#0x10000,R(s_size)
	bcc	L(Loop)

L(Lend:)
	movel	R(d1),MEM_PREDEC(res_ptr) /* store least significant limb */

/* Restore used registers from stack frame.  */
	moveml	MEM_POSTINC(sp),R(d2)-R(d6)/R(a2)
	rts

/* We loop from least significant end of the arrays, which is only
   permissable if the source and destination don't overlap, since the
   function is documented to work for overlapping source and destination.  */

L(Lspecial:)
	clrl	R(d0)			/* initialize carry */
	eorw	#1,R(s_size)
	lsrl	#1,R(s_size)
	bcc	L(LL1)
	subql	#1,R(s_size)

L(LLoop:)
	movel	MEM_POSTINC(s_ptr),R(d2)
	addxl	R(d2),R(d2)
	movel	R(d2),MEM_POSTINC(res_ptr)
L(LL1:)
	movel	MEM_POSTINC(s_ptr),R(d2)
	addxl	R(d2),R(d2)
	movel	R(d2),MEM_POSTINC(res_ptr)

	dbf	R(s_size),L(LLoop)
	addxl	R(d0),R(d0)		/* save cy in lsb */
	subl	#0x10000,R(s_size)
	bcs	L(LLend)
	lsrl	#1,R(d0)		/* restore cy */
	bra	L(LLoop)

L(LLend:)
/* Restore used registers from stack frame.  */
	moveml	MEM_POSTINC(sp),R(d2)-R(d6)/R(a2)
	rts
EPILOG(_gcry_mpih_lshift)





