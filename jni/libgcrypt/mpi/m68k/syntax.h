/* asm.h -- Definitions for 68k syntax variations.
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


#undef ALIGN

#ifdef MIT_SYNTAX
#define PROLOG(name)
#define EPILOG(name)
#define R(r)r
#define MEM(base)base@
#define MEM_DISP(base,displacement)base@(displacement)
#define MEM_INDX(base,idx,size_suffix)base@(idx:size_suffix)
#define MEM_INDX1(base,idx,size_suffix,scale)base@(idx:size_suffix:scale)
#define MEM_PREDEC(memory_base)memory_base@-
#define MEM_POSTINC(memory_base)memory_base@+
#define L(label) label
#define TEXT .text
#define ALIGN .even
#define GLOBL .globl
#define moveql moveq
/* Use variable sized opcodes.  */
#define bcc jcc
#define bcs jcs
#define bls jls
#define beq jeq
#define bne jne
#define bra jra
#endif

#ifdef SONY_SYNTAX
#define PROLOG(name)
#define EPILOG(name)
#define R(r)r
#define MEM(base)(base)
#define MEM_DISP(base,displacement)(displacement,base)
#define MEM_INDX(base,idx,size_suffix)(base,idx.size_suffix)
#define MEM_INDX1(base,idx,size_suffix,scale)(base,idx.size_suffix*scale)
#define MEM_PREDEC(memory_base)-(memory_base)
#define MEM_POSTINC(memory_base)(memory_base)+
#define L(label) label
#define TEXT .text
#define ALIGN .even
#define GLOBL .globl
#endif

#ifdef MOTOROLA_SYNTAX
#define PROLOG(name)
#define EPILOG(name)
#define R(r)r
#define MEM(base)(base)
#define MEM_DISP(base,displacement)(displacement,base)
#define MEM_INDX(base,idx,size_suffix)(base,idx.size_suffix)
#define MEM_INDX1(base,idx,size_suffix,scale)(base,idx.size_suffix*scale)
#define MEM_PREDEC(memory_base)-(memory_base)
#define MEM_POSTINC(memory_base)(memory_base)+
#define L(label) label
#define TEXT
#define ALIGN
#define GLOBL XDEF
#define lea LEA
#define movel MOVE.L
#define moveml MOVEM.L
#define moveql MOVEQ.L
#define cmpl CMP.L
#define orl OR.L
#define clrl CLR.L
#define eorw EOR.W
#define lsrl LSR.L
#define lsll LSL.L
#define roxrl ROXR.L
#define roxll ROXL.L
#define addl ADD.L
#define addxl ADDX.L
#define addql ADDQ.L
#define subl SUB.L
#define subxl SUBX.L
#define subql SUBQ.L
#define negl NEG.L
#define mulul MULU.L
#define bcc BCC
#define bcs BCS
#define bls BLS
#define beq BEQ
#define bne BNE
#define bra BRA
#define dbf DBF
#define rts RTS
#define d0 D0
#define d1 D1
#define d2 D2
#define d3 D3
#define d4 D4
#define d5 D5
#define d6 D6
#define d7 D7
#define a0 A0
#define a1 A1
#define a2 A2
#define a3 A3
#define a4 A4
#define a5 A5
#define a6 A6
#define a7 A7
#define sp SP
#endif

#ifdef ELF_SYNTAX
#define PROLOG(name) .type name,@function
#define EPILOG(name) .size name,.-name
#define MEM(base)(R(base))
#define MEM_DISP(base,displacement)(displacement,R(base))
#define MEM_PREDEC(memory_base)-(R(memory_base))
#define MEM_POSTINC(memory_base)(R(memory_base))+
#ifdef __STDC__
#define R_(r)%##r
#define R(r)R_(r)
#define MEM_INDX_(base,idx,size_suffix)(R(base),R(idx##.##size_suffix))
#define MEM_INDX(base,idx,size_suffix)MEM_INDX_(base,idx,size_suffix)
#define MEM_INDX1_(base,idx,size_suffix,scale)(R(base),R(idx##.##size_suffix*scale))
#define MEM_INDX1(base,idx,size_suffix,scale)MEM_INDX1_(base,idx,size_suffix,scale)
#define L(label) .##label
#else
#define R(r)%/**/r
#define MEM_INDX(base,idx,size_suffix)(R(base),R(idx).size_suffix)
#define MEM_INDX1(base,idx,size_suffix,scale)(R(base),R(idx).size_suffix*scale)
#define L(label) ./**/label
#endif
#define TEXT .text
#define ALIGN .align 2
#define GLOBL .globl
#define bcc jbcc
#define bcs jbcs
#define bls jbls
#define beq jbeq
#define bne jbne
#define bra jbra
#endif

#if defined (SONY_SYNTAX) || defined (ELF_SYNTAX)
#define movel move.l
#define moveml movem.l
#define moveql moveq.l
#define cmpl cmp.l
#define orl or.l
#define clrl clr.l
#define eorw eor.w
#define lsrl lsr.l
#define lsll lsl.l
#define roxrl roxr.l
#define roxll roxl.l
#define addl add.l
#define addxl addx.l
#define addql addq.l
#define subl sub.l
#define subxl subx.l
#define subql subq.l
#define negl neg.l
#define mulul mulu.l
#endif
