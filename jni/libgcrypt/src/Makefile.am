# Makefile.am - for gcrypt/src
# Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005,
#               2006, 2007  Free Software Foundation, Inc.
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

## Process this file with automake to produce Makefile.in

EXTRA_DIST = Manifest libgcrypt-config.in libgcrypt.m4 libgcrypt.vers \
             gcrypt.h.in libgcrypt.def
bin_SCRIPTS = libgcrypt-config
m4datadir = $(datadir)/aclocal
m4data_DATA = libgcrypt.m4
include_HEADERS = gcrypt.h gcrypt-module.h

lib_LTLIBRARIES = libgcrypt.la
bin_PROGRAMS = dumpsexp hmac256
if USE_RANDOM_DAEMON
sbin_PROGRAMS = gcryptrnd
bin_PROGRAMS += getrandom
endif USE_RANDOM_DAEMON


if HAVE_LD_VERSION_SCRIPT
  libgcrypt_version_script_cmd = -Wl,--version-script=$(srcdir)/libgcrypt.vers
else
  libgcrypt_version_script_cmd = 
endif

libgcrypt_la_CFLAGS = @GPG_ERROR_CFLAGS@
libgcrypt_la_SOURCES = g10lib.h visibility.c visibility.h types.h \
	cipher.h cipher-proto.h \
	misc.c global.c sexp.c hwfeatures.c \
	stdmem.c stdmem.h secmem.c secmem.h \
	mpi.h missing-string.c module.c fips.c \
	hmac256.c hmac256.h \
	ath.h ath.c

if HAVE_W32_SYSTEM

LTRCCOMPILE = $(LIBTOOL) --mode=compile $(RC) \
     `echo $(DEFS) $(DEFAULT_INCLUDES) $(INCLUDES) | \
     sed -e 's/-I/--include-dir /g;s/-D/--define /g'`

.rc.lo:
	$(LTRCCOMPILE) -i $< -o $@

gcrypt_res = versioninfo.lo
gcrypt_res_ldflag = -Wl,.libs/versioninfo.o

no_undefined = -no-undefined
export_symbols = -export-symbols $(srcdir)/libgcrypt.def

install-def-file:
	$(INSTALL) $(srcdir)/libgcrypt.def $(DESTDIR)$(libdir)/libgcrypt.def

uninstall-def-file:
	-rm $(DESTDIR)$(libdir)/libgcrypt.def

gcrypt_deps = $(gcrypt_res) libgcrypt.def

else !HAVE_W32_SYSTEM

gcrypt_res =
gcrypt_res_ldflag =
no_undefined =
export_symbols =
install-def-file:
uninstall-def-file:

gcrypt_deps =

endif !HAVE_W32_SYSTEM


libgcrypt_la_LDFLAGS = $(gcrypt_res_ldflag) $(no_undefined) $(export_symbols) \
	$(libgcrypt_version_script_cmd) -version-info \
	@LIBGCRYPT_LT_CURRENT@:@LIBGCRYPT_LT_REVISION@:@LIBGCRYPT_LT_AGE@
libgcrypt_la_DEPENDENCIES = \
	../cipher/libcipher.la \
	../random/librandom.la \
	../mpi/libmpi.la \
	$(srcdir)/libgcrypt.vers $(gcrypt_deps)
libgcrypt_la_LIBADD = \
	../cipher/libcipher.la \
	../random/librandom.la \
	../mpi/libmpi.la \
	@LTLIBOBJS@ @GPG_ERROR_LIBS@


dumpsexp_SOURCES = dumpsexp.c
dumpsexp_LDADD = 

hmac256_SOURCES = hmac256.c
hmac256_CFLAGS = -DSTANDALONE
hmac256_LDADD = 

if USE_RANDOM_DAEMON
gcryptrnd_SOURCES = gcryptrnd.c
gcryptrnd_CFLAGS = $(GPG_ERROR_CFLAGS) $(PTH_CFLAGS)
gcryptrnd_LDADD = libgcrypt.la $(PTH_LIBS)

getrandom_SOURCES = getrandom.c
endif USE_RANDOM_DAEMON


install-data-local: install-def-file

uninstall-local: uninstall-def-file

# FIXME: We need to figure out how to get the actual name (parsing
# libgcrypt.la?) and how to create the hmac file already at link time
# so that it can be used without installing libgcrypt first.
#install-exec-hook:
#	./hmac256 "What am I, a doctor or a moonshuttle conductor?" \
#          < $(DESTDIR)$(libdir)/libgcrypt.so.11.5.0 \
#          > $(DESTDIR)$(libdir)/.libgcrypt.so.11.5.0.hmac

