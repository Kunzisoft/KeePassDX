# Makefile.am for libgpg-error.
# Copyright (C) 2003, 2006, 2007 g10 Code GmbH
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

ACLOCAL_AMFLAGS = -I m4
AUTOMAKE_OPTIONS = dist-bzip2

EXTRA_DIST = autogen.sh config.rpath libgpg-error.spec.in 

if LANGUAGES_SOME
lang_subdirs = lang
else
lang_subdirs =
endif

SUBDIRS = m4 src tests po $(lang_subdirs)


dist-hook:
	sed -e 's/@pkg_version@/$(VERSION)/g' \
	    $(top_srcdir)/libgpg-error.spec.in  > $(distdir)/libgpg-error.spec
	@set -e; echo "$(VERSION)" > $(distdir)/VERSION

stowinstall: 
	$(MAKE) $(AM_MAKEFLAGS) install prefix=/usr/local/stow/libgpg-error

