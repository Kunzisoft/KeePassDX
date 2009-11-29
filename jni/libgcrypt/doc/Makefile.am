## Process this file with automake to create Makefile.in
# Copyright (C) 2002 Free Software Foundation, Inc.
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

EXTRA_DIST = README.apichanges HACKING \
	     libgcrypt-modules.eps fips-fsm.eps \
	     libgcrypt-modules.png fips-fsm.png \
             libgcrypt-modules.pdf fips-fsm.pdf 

DISTCLEANFILES = gcrypt.cps

BUILT_SOURCES = libgcrypt-modules.eps fips-fsm.eps \
                libgcrypt-modules.png fips-fsm.png \
                libgcrypt-modules.pdf fips-fsm.pdf

info_TEXINFOS = gcrypt.texi
gcrypt_TEXINFOS = lgpl.texi gpl.texi libgcrypt-modules.fig fips-fsm.fig


.fig.png:
	fig2dev -L png `test -f '$<' || echo '$(srcdir)/'`$< $@

.fig.jpg:
	fig2dev -L jpg `test -f '$<' || echo '$(srcdir)/'`$< $@

.fig.eps:
	fig2dev -L eps `test -f '$<' || echo '$(srcdir)/'`$< $@

.fig.pdf:
	fig2dev -L pdf `test -f '$<' || echo '$(srcdir)/'`$< $@


# Make sure that gcrypt.texi is touched if any other source file has
# been modified.  This is required so that the version.texi magic
# updates the release date.
gnupg.texi : $(gcrypt_TEXINFOS)
	touch $(srcdir)/gcrypt.texi

online: gcrypt.html gcrypt.pdf gcrypt.info
	set -e; \
	echo "Uploading current manuals to www.gnupg.org ..."; \
	cp libgcrypt-modules.png gcrypt.html/; \
	cp fips-fsm.png gcrypt.html/; \
        user=werner ; dashdevel="" ; \
        if echo "@PACKAGE_VERSION@" | grep -- "-svn" >/dev/null; then \
	  dashdevel="-devel" ; \
	  cp gcrypt.pdf gcrypt.html/; \
	  cp gcrypt.info gcrypt.html/; \
	else \
          rsync -v gcrypt.pdf gcrypt.info \
               $${user}@trithemius.gnupg.org:webspace/manuals/ ; \
        fi ; \
	cd gcrypt.html ; \
        rsync -vr --exclude='.svn' .  \
	  $${user}@trithemius.gnupg.org:webspace/manuals/gcrypt$${dashdevel}/ 

