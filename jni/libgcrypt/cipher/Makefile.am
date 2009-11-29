# Makefile for cipher modules
# Copyright (C) 1998, 1999, 2000, 2001, 2002,
#               2003 Free Software Foundation, Inc.
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
# License along with this program; if not, see <http://www.gnu.org/licenses/>.

# Process this file with automake to produce Makefile.in

EXTRA_DIST = Manifest

# Need to include ../src in addition to top_srcdir because gcrypt.h is
# a built header.
AM_CPPFLAGS = -I../src -I$(top_srcdir)/src 
AM_CFLAGS = $(GPG_ERROR_CFLAGS)


noinst_LTLIBRARIES = libcipher.la

GCRYPT_MODULES = @GCRYPT_CIPHERS@ @GCRYPT_PUBKEY_CIPHERS@ @GCRYPT_DIGESTS@

libcipher_la_DEPENDENCIES = $(GCRYPT_MODULES)
libcipher_la_LIBADD = $(GCRYPT_MODULES)

libcipher_la_SOURCES = \
cipher.c pubkey.c ac.c md.c \
hmac-tests.c \
bithelp.h  \
primegen.c  \
hash-common.c hash-common.h \
rmd.h

EXTRA_libcipher_la_SOURCES = \
arcfour.c \
blowfish.c \
cast5.c \
crc.c \
des.c \
dsa.c \
elgamal.c \
ecc.c \
md4.c \
md5.c \
rijndael.c rijndael-tables.h \
rmd160.c \
rsa.c \
seed.c \
serpent.c \
sha1.c \
sha256.c \
sha512.c \
tiger.c \
whirlpool.c \
twofish.c \
rfc2268.c \
camellia.c camellia.h camellia-glue.c


# We need to lower the optimization for this module.
tiger.o: $(srcdir)/tiger.c
	`echo $(COMPILE) -c $(srcdir)/tiger.c | sed -e 's/-O[2-9s]*/-O1/g' `

tiger.lo: $(srcdir)/tiger.c
	`echo $(LTCOMPILE) -c $(srcdir)/tiger.c | sed -e 's/-O[2-9s]*/-O1/g' `
