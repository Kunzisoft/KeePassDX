## Process this file with automake to produce Makefile.in
# Copyright (C) 1992, 1999, 2000, 2002 Free Software Foundation, Inc.
#
# This file is part of Libgcrypt.
#
# Libgcrypt is free software; you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# Libgcrypt is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA

# 1.5 leads to a combinatorial explosion due to all the conditionals
# I was not able to build it with 64Megs - 1.6 fixes this.
# not anymore required: AUTOMAKE_OPTIONS = 1.6

# Need to include ../src in addition to top_srcdir because gcrypt.h is
# a built header.
AM_CPPFLAGS = -I../src -I$(top_srcdir)/src
AM_CFLAGS = $(GPG_ERROR_CFLAGS)

AM_ASFLAGS = $(MPI_SFLAGS)
AM_CCASFLAGS = $(NOEXECSTACK_FLAGS)

EXTRA_DIST = Manifest config.links
DISTCLEANFILES = mpi-asm-defs.h \
                 mpih-add1-asm.S mpih-mul1-asm.S mpih-mul2-asm.S mpih-mul3-asm.S  \
		 mpih-lshift-asm.S mpih-rshift-asm.S mpih-sub1-asm.S asm-syntax.h \
                 mpih-add1.c mpih-mul1.c mpih-mul2.c mpih-mul3.c  \
		 mpih-lshift.c mpih-rshift.c mpih-sub1.c \
	         sysdep.h mod-source-info.h

# Beware: The following list is not a comment but grepped by
#         config.links to get the list of symlinked modules
#         Optional modules are marked with an O in the second column.
#BEGIN_ASM_LIST
# mpih-add1    C
# mpih-sub1    C
# mpih-mul1    C
# mpih-mul2    C
# mpih-mul3    C
# mpih-lshift  C
# mpih-rshift  C
# udiv         O
# udiv-qrnnd   O
#END_ASM_LIST

# Note: This function has not yet been implemented.  There is only a dummy in
# generic/
# udiv-w-sdiv  O

# And we need to have conditionals for all modules because
# we don't know whether they are .c or .S.  Very ugly; I know.
# Remember to define them all in configure.ac
if MPI_MOD_ASM_MPIH_ADD1
mpih_add1 = mpih-add1-asm.S
else
if MPI_MOD_C_MPIH_ADD1
mpih_add1 = mpih-add1.c
else
mpih_add1 = 
endif
endif

if MPI_MOD_ASM_MPIH_SUB1
mpih_sub1 = mpih-sub1-asm.S
else
if MPI_MOD_C_MPIH_SUB1
mpih_sub1 = mpih-sub1.c
else
mpih_sub1 = 
endif
endif

if MPI_MOD_ASM_MPIH_MUL1
mpih_mul1 = mpih-mul1-asm.S
else
if MPI_MOD_C_MPIH_MUL1
mpih_mul1 = mpih-mul1.c
else
mpih_mul1 = 
endif
endif

if MPI_MOD_ASM_MPIH_MUL2
mpih_mul2 = mpih-mul2-asm.S
else
if MPI_MOD_C_MPIH_MUL2
mpih_mul2 = mpih-mul2.c
else
mpih_mul2 = 
endif
endif

if MPI_MOD_ASM_MPIH_MUL3
mpih_mul3 = mpih-mul3-asm.S
else
if MPI_MOD_C_MPIH_MUL3
mpih_mul3 = mpih-mul3.c
else
mpih_mul3 = 
endif
endif

if MPI_MOD_ASM_MPIH_LSHIFT
mpih_lshift = mpih-lshift-asm.S
else
if MPI_MOD_C_MPIH_LSHIFT
mpih_lshift = mpih-lshift.c
else
mpih_lshift = 
endif
endif

if MPI_MOD_ASM_MPIH_RSHIFT
mpih_rshift = mpih-rshift-asm.S
else
if MPI_MOD_C_MPIH_RSHIFT
mpih_rshift = mpih-rshift.c
else
mpih_rshift = 
endif
endif

if MPI_MOD_ASM_UDIV
udiv = udiv-asm.S
else
if MPI_MOD_C_UDIV
udiv = udiv.c
else
udiv = 
endif
endif

if MPI_MOD_ASM_UDIV_QRNND
udiv_qrnnd = udiv-qrnnd-asm.S
else
if MPI_MOD_C_UDIV_QRNND
udiv_qrnnd = udiv-qrnnd.c
else
udiv_qrnnd = 
endif
endif

noinst_LTLIBRARIES = libmpi.la

libmpi_la_LDFLAGS =
nodist_libmpi_la_SOURCES = $(mpih_add1) $(mpih_sub1) $(mpih_mul1) \
	$(mpih_mul2) $(mpih_mul3) $(mpih_lshift) $(mpih_rshift) \
	$(udiv) $(udiv_qrnnd)
libmpi_la_SOURCES = longlong.h	   \
	      mpi-add.c      \
	      mpi-bit.c      \
	      mpi-cmp.c      \
	      mpi-div.c      \
	      mpi-gcd.c      \
	      mpi-internal.h \
	      mpi-inline.h   \
	      mpi-inline.c   \
	      mpi-inv.c      \
	      mpi-mul.c      \
	      mpi-mod.c      \
	      mpi-pow.c      \
	      mpi-mpow.c     \
	      mpi-scan.c     \
	      mpicoder.c     \
	      mpih-div.c     \
	      mpih-mul.c     \
	      mpiutil.c      \
              ec.c
