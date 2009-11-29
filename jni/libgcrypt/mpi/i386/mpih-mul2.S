/* i80386 addmul_1 -- Multiply a limb vector with a limb and add
 *		      the result to a second limb vector.
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
 * mpi_limb_t
 * _gcry_mpih_addmul_1( mpi_ptr_t res_ptr,      (sp + 4)
 *		     mpi_ptr_t s1_ptr,	     (sp + 8)
 *		     mpi_size_t s1_size,     (sp + 12)
 *		     mpi_limb_t s2_limb)     (sp + 16)
 */

#define res_ptr edi
#define s1_ptr	esi
#define size	ecx
#define s2_limb ebp

	TEXT
	ALIGN (3)
	GLOBL	C_SYMBOL_NAME(_gcry_mpih_addmul_1)
C_SYMBOL_NAME(_gcry_mpih_addmul_1:)

	INSN1(push,l	,R(edi))
	INSN1(push,l	,R(esi))
	INSN1(push,l	,R(ebx))
	INSN1(push,l	,R(ebp))

	INSN2(mov,l	,R(res_ptr),MEM_DISP(esp,20))
	INSN2(mov,l	,R(s1_ptr),MEM_DISP(esp,24))
	INSN2(mov,l	,R(size),MEM_DISP(esp,28))
	INSN2(mov,l	,R(s2_limb),MEM_DISP(esp,32))

	INSN2(lea,l	,R(res_ptr),MEM_INDEX(res_ptr,size,4))
	INSN2(lea,l	,R(s1_ptr),MEM_INDEX(s1_ptr,size,4))
	INSN1(neg,l	,R(size))
	INSN2(xor,l	,R(ebx),R(ebx))
	ALIGN (3)
Loop:
	INSN2(mov,l	,R(eax),MEM_INDEX(s1_ptr,size,4))
	INSN1(mul,l	,R(s2_limb))
	INSN2(add,l	,R(eax),R(ebx))
	INSN2(adc,l	,R(edx),$0)
	INSN2(add,l	,MEM_INDEX(res_ptr,size,4),R(eax))
	INSN2(adc,l	,R(edx),$0)
	INSN2(mov,l	,R(ebx),R(edx))

	INSN1(inc,l	,R(size))
	INSN1(jnz,	,Loop)
	INSN2(mov,l	,R(eax),R(ebx))

	INSN1(pop,l	,R(ebp))
	INSN1(pop,l	,R(ebx))
	INSN1(pop,l	,R(esi))
	INSN1(pop,l	,R(edi))
	ret

