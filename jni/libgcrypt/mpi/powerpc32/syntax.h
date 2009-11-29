/* gmp2-2.0.2-ppc/mpn/powerpc-linux/syntax.h   Tue Oct	6 19:27:01 1998 */
/* From glibc's sysdeps/unix/sysv/linux/powerpc/sysdep.h */

/* Copyright (C) 1992, 1997, 1998 Free Software Foundation, Inc.
   This file is part of the GNU C Library.

   The GNU C Library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Library General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.

   The GNU C Library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Library General Public License for more details.

   You should have received a copy of the GNU Library General Public
   License along with the GNU C Library; see the file COPYING.LIB.  If not,
   write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
   Boston, MA 02111-1307, USA.	*/


#define USE_PPC_PATCHES 1

/* This seems to always be the case on PPC.  */
#define ALIGNARG(log2) log2
/* For ELF we need the `.type' directive to make shared libs work right.  */
#define ASM_TYPE_DIRECTIVE(name,typearg) .type name,typearg;
#define ASM_SIZE_DIRECTIVE(name) .size name,.-name
#define ASM_GLOBAL_DIRECTIVE   .globl

#ifdef __STDC__
#define C_LABEL(name) C_SYMBOL_NAME(name)##:
#else
#define C_LABEL(name) C_SYMBOL_NAME(name)/**/:
#endif

#ifdef __STDC__
#define L(body) .L##body
#else
#define L(body) .L/**/body
#endif

/* No profiling of gmp's assembly for now... */
#define CALL_MCOUNT /* no profiling */

#define        ENTRY(name)				    \
  ASM_GLOBAL_DIRECTIVE C_SYMBOL_NAME(name);		    \
  ASM_TYPE_DIRECTIVE (C_SYMBOL_NAME(name),@function)	    \
  .align ALIGNARG(2);					    \
  C_LABEL(name) 					    \
  CALL_MCOUNT

#define EALIGN_W_0  /* No words to insert.  */
#define EALIGN_W_1  nop
#define EALIGN_W_2  nop;nop
#define EALIGN_W_3  nop;nop;nop
#define EALIGN_W_4  EALIGN_W_3;nop
#define EALIGN_W_5  EALIGN_W_4;nop
#define EALIGN_W_6  EALIGN_W_5;nop
#define EALIGN_W_7  EALIGN_W_6;nop

/* EALIGN is like ENTRY, but does alignment to 'words'*4 bytes
   past a 2^align boundary.  */
#define EALIGN(name, alignt, words)			\
  ASM_GLOBAL_DIRECTIVE C_SYMBOL_NAME(name);		\
  ASM_TYPE_DIRECTIVE (C_SYMBOL_NAME(name),@function)	\
  .align ALIGNARG(alignt);				\
  EALIGN_W_##words;					\
  C_LABEL(name)

#undef END
#define END(name)		     \
  ASM_SIZE_DIRECTIVE(name)

