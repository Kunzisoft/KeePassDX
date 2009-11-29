# Makefile for cipher modules
# Copyright (C) 2008 Free Software Foundation, Inc.
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

# Need to include ../src in addition to top_srcdir because gcrypt.h is
# a built header.
AM_CPPFLAGS = -I../src -I$(top_srcdir)/src 
AM_CFLAGS = $(GPG_ERROR_CFLAGS)

noinst_LTLIBRARIES = librandom.la

GCRYPT_MODULES = @GCRYPT_RANDOM@

librandom_la_DEPENDENCIES = $(GCRYPT_MODULES)
librandom_la_LIBADD = $(GCRYPT_MODULES)

librandom_la_SOURCES = \
random.c random.h \
rand-internal.h \
random-csprng.c \
random-fips.c \
rndhw.c

if USE_RANDOM_DAEMON
librandom_la_SOURCES += random-daemon.c 
endif USE_RANDOM_DAEMON


EXTRA_librandom_la_SOURCES = \
rndlinux.c \
rndegd.c \
rndunix.c \
rndw32.c 
