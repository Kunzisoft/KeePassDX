# Makefile.am for libgpg-error.
# Copyright (C) 2003, 2006 g10 Code GmbH
# 
# This file is part of libgpg-error.
# 
# libgpg-error is free software; you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of the
# License, or (at your option) any later version.
# 
# libgpg-error is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public
# License along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA

distributed_clfiles = gpg-error.asd \
	gpg-error-package.lisp gpg-error.lisp

generated_clfiles = gpg-error-codes.lisp

clfiles = $(distributed_clfiles) $(generated_clfiles)

# FIXME: Should be configurable.
clfilesdir = $(datadir)/common-lisp/source/gpg-error
dist_clfiles_DATA = $(distributed_clfiles)
nodist_clfiles_DATA = $(generated_clfiles)

EXTRA_DIST = README mkerrcodes.awk
CLEANFILES = gpg-error-codes.lisp

codes_file = $(top_srcdir)/src/err-codes.h.in
errno_file = $(top_srcdir)/src/errnos.in

gpg-error-codes.lisp: Makefile mkerrcodes.awk $(codes_file) $(errno_file)
	echo '@errnos@' | cat $(codes_file) - $(errno_file) \
	| $(AWK) -f $(srcdir)/mkerrcodes.awk >$@
