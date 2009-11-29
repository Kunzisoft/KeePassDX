# configure.ac for libgpg-error
# Copyright (C) 2003, 2004, 2006 g10 Code GmbH
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

# (Process this file with autoconf to produce a configure script.)
# The following lines are used by ./autogen.sh.
AC_PREREQ(2.59)
min_automake_version="1.9.6"

# Remember to change the version number immediately *after* a release.
# Set my_issvn to "yes" for non-released code.  Remember to run an
# "svn up" and "autogen.sh" right before creating a distribution.
m4_define([my_version], [1.6])
m4_define([my_issvn], [no])

m4_define([svn_revision], m4_esyscmd([echo -n $( (svn info 2>/dev/null \
            || echo 'Revision: 0')|sed -n '/^Revision:/ {s/[^0-9]//gp;q;}')]))
AC_INIT([libgpg-error], my_version[]m4_if(my_issvn,[yes],[-svn[]svn_revision]),
        [bug-gnupg@gnupg.org])
# LT Version numbers, remember to change them just *before* a release.
#   (Code changed:			REVISION++)
#   (Interfaces added/removed/changed:	CURRENT++, REVISION=0)
#   (Interfaces added:			AGE++)
#   (Interfaces removed:		AGE=0)
# Note that added error codes don't constitute an interface change.
LIBGPG_ERROR_LT_CURRENT=4
LIBGPG_ERROR_LT_AGE=4
LIBGPG_ERROR_LT_REVISION=0
AC_SUBST(LIBGPG_ERROR_LT_CURRENT)
AC_SUBST(LIBGPG_ERROR_LT_AGE)
AC_SUBST(LIBGPG_ERROR_LT_REVISION)

AM_INIT_AUTOMAKE
AM_MAINTAINER_MODE
AC_CONFIG_SRCDIR([src/err-sources.h.in])
AC_CONFIG_HEADER([config.h])

# We need to know about the host architecture to avoid spurious
# warnings.
AC_CANONICAL_HOST
AB_INIT

# Checks for programs.
AC_PROG_CC
AM_PROG_CC_C_O
AC_PROG_CPP
AC_PROG_AWK
AC_CHECK_TOOL(AR, ar, :)
AC_LIBTOOL_WIN32_DLL
AC_LIBTOOL_RC
AC_GNU_SOURCE

AC_PROG_LIBTOOL

# We need to compile and run a program on the build machine.
dnl The AC_PROG_CC_FOR_BUILD macro in the AC archive is broken for
dnl autoconf 2.57.
dnl AC_PROG_CC_FOR_BUILD
AC_MSG_CHECKING(for cc for build)
if test "$cross_compiling" = "yes"; then
  CC_FOR_BUILD="${CC_FOR_BUILD-cc}"
else
  CC_FOR_BUILD="${CC_FOR_BUILD-$CC}"
fi
AC_MSG_RESULT($CC_FOR_BUILD)
AC_ARG_VAR(CC_FOR_BUILD,[build system C compiler])


# Set some internal variables depending on the platform for later use.
have_w32_system=no
case "${host}" in
    *-mingw32*)
        have_w32_system=yes
        ;;
    *)
       ;;
esac


AH_BOTTOM([
/* Force using of NLS for W32 even if no libintl has been found.  This is 
   okay because we have our own gettext implementation for W32.  */
#if defined(HAVE_W32_SYSTEM) && !defined(ENABLE_NLS)
#define ENABLE_NLS 1
#endif
])


# Note, that autogen.sh greps for the next line.
AM_GNU_GETTEXT_VERSION([0.15])
AM_GNU_GETTEXT([external])

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS([stdlib.h locale.h])
AC_FUNC_STRERROR_R
case "${host_os}" in
     solaris*)
     # All versions of Solaris from 2.4 have a thread-safe strerror().
     # Since Solaris 10, in addition strerror_r() exists.
     ;;
     *)
     AC_CHECK_FUNC([strerror_r], [],
AC_MSG_WARN([[Without strerror_r, gpg_strerror_r might not be thread-safe]]))
     ;;
esac

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST

# Substitution used for gpg-error-config
GPG_ERROR_CONFIG_LIBS="-lgpg-error"
GPG_ERROR_CONFIG_CFLAGS=""
AC_SUBST(GPG_ERROR_CONFIG_LIBS)
AC_SUBST(GPG_ERROR_CONFIG_CFLAGS)
AC_CONFIG_FILES([src/gpg-error-config], [chmod +x src/gpg-error-config])


# Special defines for certain platforms
if test "$have_w32_system" = yes; then
    AC_DEFINE(HAVE_W32_SYSTEM,1,[Defined if we run on a W32 API based system])
    BUILD_TIMESTAMP=`date --iso-8601=minutes`
    AC_SUBST(BUILD_TIMESTAMP)
    changequote(,)dnl 
    BUILD_FILEVERSION=`echo "$VERSION" | sed 's/\([0-9.]*\).*/\1./;s/\./,/g'`
    changequote([,])dnl
    case "$VERSION" in
      *-svn*) BUILD_FILEVERSION="${BUILD_FILEVERSION}0,0" ;;
      *-cvs)  BUILD_FILEVERSION="${BUILD_FILEVERSION}0,0" ;;
      *-rc*)  BUILD_FILEVERSION="${BUILD_FILEVERSION}0,1" ;;
      *)      BUILD_FILEVERSION="${BUILD_FILEVERSION}0,2" ;;
    esac
fi
AC_SUBST(BUILD_TIMESTAMP)
AC_SUBST(BUILD_FILEVERSION)
AM_CONDITIONAL(HAVE_W32_SYSTEM, test "$have_w32_system" = yes)


AC_ARG_ENABLE(languages,
[  --disable-languages     do not build support for other languages than C])
AM_CONDITIONAL([LANGUAGES_SOME], [test "x$enable_languages" != xno])



# Substitution
AC_CONFIG_FILES([Makefile])
AC_CONFIG_FILES([po/Makefile.in m4/Makefile])
AC_CONFIG_FILES([src/Makefile tests/Makefile])
AC_CONFIG_FILES([lang/Makefile lang/cl/Makefile lang/cl/gpg-error.asd])
AC_CONFIG_FILES([src/versioninfo.rc])

AC_OUTPUT

echo "
        Libgpg-error v${VERSION} has been configured as follows:
        
        Platform:  $host

"
