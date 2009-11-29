# Configure.ac script for Libgcrypt
# Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2006,
#               2007, 2008, 2009 Free Software Foundation, Inc.
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

# (Process this file with autoconf to produce a configure script.)
AC_REVISION($Revision: 1382 $)
AC_PREREQ(2.60)
min_automake_version="1.10"


# Remember to change the version number immediately *after* a release.
# Set my_issvn to "yes" for non-released code.  Remember to run an
# "svn up" and "autogen.sh" right before creating a distribution.
m4_define([my_version], [1.4.4])
m4_define([my_issvn], [no])

m4_define([svn_revision], m4_esyscmd([printf "%d" $(svn info 2>/dev/null \
          | sed -n '/^Revision:/ s/[^0-9]//gp'|head -1)]))
AC_INIT([libgcrypt], 
        [my_version[]m4_if(my_issvn,[yes],[-svn[]svn_revision])],
        [bug-libgcrypt@gnupg.org])
# LT Version numbers, remember to change them just *before* a release.
#   (Interfaces removed:    CURRENT++, AGE=0, REVISION=0)
#   (Interfaces added:      CURRENT++, AGE++, REVISION=0)
#   (No interfaces changed:                   REVISION++)
LIBGCRYPT_LT_CURRENT=16
LIBGCRYPT_LT_AGE=5
LIBGCRYPT_LT_REVISION=2


# If the API is changed in an incompatible way: increment the next counter.
LIBGCRYPT_CONFIG_API_VERSION=1

NEED_GPG_ERROR_VERSION=1.4

is_development_version=my_issvn
BUILD_REVISION=svn_revision
PACKAGE=$PACKAGE_NAME
VERSION=$PACKAGE_VERSION

AC_CONFIG_SRCDIR([src/libgcrypt.vers])
AM_INIT_AUTOMAKE($PACKAGE, $VERSION)
AM_CONFIG_HEADER(config.h)
AC_CANONICAL_HOST
AM_MAINTAINER_MODE

AH_TOP([
#ifndef _GCRYPT_CONFIG_H_INCLUDED
#define _GCRYPT_CONFIG_H_INCLUDED
/* need this, because some autoconf tests rely on this (e.g. stpcpy)
 * and it should be used for new programs  */
#define _GNU_SOURCE  1
])

AH_BOTTOM([
#define _GCRYPT_IN_LIBGCRYPT 1

/* If the configure check for endianness has been disabled, get it from
   OS macros.  This is intended for making fat binary builds on OS X.  */
#ifdef DISABLED_ENDIAN_CHECK
# if defined(__BIG_ENDIAN__)
#  define WORDS_BIGENDIAN 1
# elif defined(__LITTLE_ENDIAN__)
#  undef WORDS_BIGENDIAN
# else
#  error "No endianness found"
# endif
#endif /*DISABLED_ENDIAN_CHECK*/

/* We basically use the original Camellia source.  Make sure the symbols 
   properly prefixed.  */
#define CAMELLIA_EXT_SYM_PREFIX _gcry_

/* This error code is only available with gpg-error 1.7.  Thus
   we define it here with the usual gcry prefix.  */
#define GCRY_GPG_ERR_NOT_OPERATIONAL  176


#endif /*_GCRYPT_CONFIG_H_INCLUDED*/
])

AH_VERBATIM([_REENTRANT],
[/* To allow the use of Libgcrypt in multithreaded programs we have to use
    special features from the library. */
#ifndef _REENTRANT
# define _REENTRANT 1
#endif
])


AC_SUBST(LIBGCRYPT_LT_CURRENT)
AC_SUBST(LIBGCRYPT_LT_AGE)
AC_SUBST(LIBGCRYPT_LT_REVISION)
AC_SUBST(PACKAGE)
AC_SUBST(VERSION)
AC_DEFINE_UNQUOTED(PACKAGE, "$PACKAGE", [Name of this package])
AC_DEFINE_UNQUOTED(VERSION, "$VERSION", [Version of this package])


######################
##  Basic checks.  ### (we need some results later on (e.g. $GCC)
######################

AC_PROG_MAKE_SET
missing_dir=`cd $ac_aux_dir && pwd`
AM_MISSING_PROG(ACLOCAL, aclocal, $missing_dir)
AM_MISSING_PROG(AUTOCONF, autoconf, $missing_dir)
AM_MISSING_PROG(AUTOMAKE, automake, $missing_dir)
AM_MISSING_PROG(AUTOHEADER, autoheader, $missing_dir)
# AM_MISSING_PROG(MAKEINFO, makeinfo, $missing_dir)
AC_PROG_CC
AC_PROG_CPP
AM_PROG_CC_C_O
AM_PROG_AS
AC_ISC_POSIX
AC_PROG_INSTALL
AC_PROG_AWK

AC_LIBTOOL_WIN32_DLL
AC_LIBTOOL_RC
AM_PROG_LIBTOOL


##########################
## General definitions. ##
##########################

# Used by libgcrypt-config
LIBGCRYPT_CONFIG_LIBS="-lgcrypt"
LIBGCRYPT_CONFIG_CFLAGS=""

# Definitions for symmetric ciphers.
available_ciphers="arcfour blowfish cast5 des aes twofish serpent rfc2268 seed"
available_ciphers="$available_ciphers camellia"
enabled_ciphers=""

# Definitions for public-key ciphers.
available_pubkey_ciphers="dsa elgamal rsa ecc"
enabled_pubkey_ciphers=""

# Definitions for message digests.
available_digests="crc md4 md5 rmd160 sha1 sha256"
available_digests_64="sha512 tiger whirlpool"
enabled_digests=""

# Definitions for random modules.
available_random_modules="linux egd unix"
auto_random_modules="$available_random_modules"

# Supported thread backends.
LIBGCRYPT_THREAD_MODULES=""

# Other definitions.
print_egd_notice=no
have_w32_system=no

# Setup some stuff depending on host.
case "${host}" in
    *-*-mingw32*)
      available_random_modules="w32"
      ac_cv_have_dev_random=no
      have_w32_system=yes
      AC_DEFINE(USE_ONLY_8DOT3,1,
                [set this to limit filenames to the 8.3 format])
      AC_DEFINE(HAVE_DRIVE_LETTERS,1,
                [defined if we must run on a stupid file system])
      AC_DEFINE(HAVE_DOSISH_SYSTEM,1,
                [defined if we run on some of the PCDOS like systems 
                 (DOS, Windoze. OS/2) with special properties like
                  no file modes])
      ;;

    i?86-emx-os2 | i?86-*-os2*emx)
        # OS/2 with the EMX environment
        ac_cv_have_dev_random=no
        AC_DEFINE(HAVE_DRIVE_LETTERS)
        AC_DEFINE(HAVE_DOSISH_SYSTEM)
        ;;

    i?86-*-msdosdjgpp*)
        # DOS with the DJGPP environment
        ac_cv_have_dev_random=no
        AC_DEFINE(HAVE_DRIVE_LETTERS)
        AC_DEFINE(HAVE_DOSISH_SYSTEM)
        ;;

    *-*-freebsd*)
       # FreeBSD
       CPPFLAGS="$CPPFLAGS -I/usr/local/include"
       LDFLAGS="$LDFLAGS -L/usr/local/lib"
       ;;

    *-*-hpux*)
        if test -z "$GCC" ; then
            CFLAGS="$CFLAGS -Ae -D_HPUX_SOURCE"
        fi
        ;;
    *-dec-osf4*)
        if test -z "$GCC" ; then
            # Suppress all warnings
            # to get rid of the unsigned/signed char mismatch warnings.
            CFLAGS="$CFLAGS -w"
        fi
        ;;
    m68k-atari-mint)
        ;;
    *)
      ;;
esac

if test "$have_w32_system" = yes; then
   AC_DEFINE(HAVE_W32_SYSTEM,1, [Defined if we run on a W32 API based system])
fi
AM_CONDITIONAL(HAVE_W32_SYSTEM, test "$have_w32_system" = yes)



# A printable OS Name is sometimes useful.
case "${host}" in
    *-*-mingw32*)
        PRINTABLE_OS_NAME="MingW32"
        ;;

    i?86-emx-os2 | i?86-*-os2*emx )
        PRINTABLE_OS_NAME="OS/2"
        ;;

    i?86-*-msdosdjgpp*)
        PRINTABLE_OS_NAME="MSDOS/DJGPP"
        ;;

    *-linux*)
        PRINTABLE_OS_NAME="GNU/Linux"
        ;;

    *)
        PRINTABLE_OS_NAME=`uname -s || echo "Unknown"`
        ;;
esac

#
# Figure out the name of the random device
#
case "${host}" in
    *-openbsd*)
        NAME_OF_DEV_RANDOM="/dev/srandom"
        NAME_OF_DEV_URANDOM="/dev/urandom"
        ;;

    *)
        NAME_OF_DEV_RANDOM="/dev/random"
        NAME_OF_DEV_URANDOM="/dev/urandom"
        ;;
esac


AC_ARG_ENABLE(endian-check,
              AC_HELP_STRING([--disable-endian-check],
	      [disable the endian check and trust the OS provided macros]),
	      endiancheck=$enableval,endiancheck=yes)
if test x"$endiancheck" = xyes ; then
  AC_C_BIGENDIAN
else
  AC_DEFINE(DISABLED_ENDIAN_CHECK,1,[configure did not test for endianess])
fi

AC_CHECK_SIZEOF(unsigned short, 2)
AC_CHECK_SIZEOF(unsigned int, 4)
AC_CHECK_SIZEOF(unsigned long, 4)
AC_CHECK_SIZEOF(unsigned long long, 0)

AC_TYPE_UINTPTR_T

if test "$ac_cv_sizeof_unsigned_short" = "0" \
   || test "$ac_cv_sizeof_unsigned_int" = "0" \
   || test "$ac_cv_sizeof_unsigned_long" = "0"; then
    AC_MSG_WARN([Hmmm, something is wrong with the sizes - using defaults]);
fi

# Do we have any 64-bit data types?
if test "$ac_cv_sizeof_unsigned_int" != "8" \
   && test "$ac_cv_sizeof_unsigned_long" != "8" \
   && test "$ac_cv_sizeof_unsigned_long_long" != "8" \
   && test "$ac_cv_sizeof_uint64_t" != "8"; then
    AC_MSG_WARN([No 64-bit types.  Disabling TIGER/192, SHA-384, and SHA-512])
else
  available_digests="$available_digests $available_digests_64"
fi

# If not specified otherwise, all available algorithms will be
# included.  
default_ciphers="$available_ciphers"
default_pubkey_ciphers="$available_pubkey_ciphers"
default_digests="$available_digests"

############################
## Command line switches. ##
############################

# Implementation of the --enable-ciphers switch.
AC_ARG_ENABLE(ciphers,
	      AC_HELP_STRING([--enable-ciphers=ciphers],
			     [select the symmetric ciphers to include]),
	      [enabled_ciphers=`echo $enableval | tr ',:' '  ' | tr '[A-Z]' '[a-z]'`],
	      [enabled_ciphers=""])
if test "x$enabled_ciphers" = "x" \
   -o "$enabled_ciphers" = "yes"  \
   -o "$enabled_ciphers" = "no"; then
   enabled_ciphers=$default_ciphers
fi
AC_MSG_CHECKING([which symmetric ciphers to include])
for cipher in $enabled_ciphers; do
    LIST_MEMBER($cipher, $available_ciphers)
    if test "$found" = "0"; then
       AC_MSG_ERROR([unsupported cipher "$cipher" specified])
    fi
done
AC_MSG_RESULT([$enabled_ciphers])

# Implementation of the --enable-pubkey-ciphers switch.
AC_ARG_ENABLE(pubkey-ciphers,
	      AC_HELP_STRING([--enable-pubkey-ciphers=ciphers],
			     [select the public-key ciphers to include]),
	      [enabled_pubkey_ciphers=`echo $enableval | tr ',:' '  ' | tr '[A-Z]' '[a-z]'`],
	      [enabled_pubkey_ciphers=""])
if test "x$enabled_pubkey_ciphers" = "x" \
   -o "$enabled_pubkey_ciphers" = "yes"  \
   -o "$enabled_pubkey_ciphers" = "no"; then
   enabled_pubkey_ciphers=$default_pubkey_ciphers
fi
AC_MSG_CHECKING([which public-key ciphers to include])
for cipher in $enabled_pubkey_ciphers; do
    LIST_MEMBER($cipher, $available_pubkey_ciphers)
    if test "$found" = "0"; then
       AC_MSG_ERROR([unsupported public-key cipher specified])
    fi
done
AC_MSG_RESULT([$enabled_pubkey_ciphers])

# Implementation of the --enable-digests switch.
AC_ARG_ENABLE(digests,
	      AC_HELP_STRING([--enable-digests=digests],
			     [select the message digests to include]),
	      [enabled_digests=`echo $enableval | tr ',:' '  ' | tr '[A-Z]' '[a-z]'`],
	      [enabled_digests=""])
if test "x$enabled_digests" = "x" \
   -o "$enabled_digests" = "yes"  \
   -o "$enabled_digests" = "no"; then
   enabled_digests=$default_digests
fi
AC_MSG_CHECKING([which message digests to include])
for digest in $enabled_digests; do
    LIST_MEMBER($digest, $available_digests)
    if test "$found" = "0"; then
       AC_MSG_ERROR([unsupported message digest specified])
    fi
done
AC_MSG_RESULT([$enabled_digests])

# Implementation of the --enable-random switch.
AC_ARG_ENABLE(random,
	      AC_HELP_STRING([--enable-random=name],
	                     [select which random number generator to use]),
	      [random=`echo $enableval | tr '[A-Z]' '[a-z]'`],
	      [])
if test "x$random" = "x" -o "$random" = "yes" -o "$random" = "no"; then
    random=default
fi
AC_MSG_CHECKING([which random module to use])
if test "$random" != "default" -a "$random" != "auto"; then
    LIST_MEMBER($random, $available_random_modules)
    if test "$found" = "0"; then
       AC_MSG_ERROR([unsupported random module specified])
    fi
fi
AC_MSG_RESULT($random)

# Implementation of the --disable-dev-random switch.
AC_MSG_CHECKING([whether use of /dev/random is requested])
AC_ARG_ENABLE(dev-random,
[  --disable-dev-random    disable the use of dev random],
    try_dev_random=$enableval, try_dev_random=yes)
AC_MSG_RESULT($try_dev_random)

# Implementation of the --with-egd-socket switch.
AC_ARG_WITH(egd-socket,
    [  --with-egd-socket=NAME  Use NAME for the EGD socket)],
            egd_socket_name="$withval", egd_socket_name="" )
AC_DEFINE_UNQUOTED(EGD_SOCKET_NAME, "$egd_socket_name",
                   [Define if you don't want the default EGD socket name.
                    For details see cipher/rndegd.c])

# Implementation of the --enable-random-daemon
AC_MSG_CHECKING([whether the experimental random daemon is requested])
AC_ARG_ENABLE([random-daemon],
              AC_HELP_STRING([--enable-random-daemon],
                             [Build and support the experimental gcryptrnd]),
              [use_random_daemon=$enableval],
              [use_random_daemon=no])
AC_MSG_RESULT($use_random_daemon)
if test x$use_random_daemon = xyes ; then
    AC_DEFINE(USE_RANDOM_DAEMON,1,
              [Define to support the experimental random daemon])
fi
AM_CONDITIONAL(USE_RANDOM_DAEMON, test x$use_random_daemon = xyes)


# Implementation of --disable-asm.
AC_MSG_CHECKING([whether MPI assembler modules are requested])
AC_ARG_ENABLE([asm],
              AC_HELP_STRING([--disable-asm],
	                     [Disable MPI assembler modules]),
              [try_asm_modules=$enableval],
              [try_asm_modules=yes])
AC_MSG_RESULT($try_asm_modules)

# Implementation of the --enable-m-guard switch.
AC_MSG_CHECKING([whether memory guard is requested])
AC_ARG_ENABLE(m-guard,
              AC_HELP_STRING([--enable-m-guard],
                             [Enable memory guard facility]),
              [use_m_guard=$enableval], [use_m_guard=no])
AC_MSG_RESULT($use_m_guard)
if test "$use_m_guard" = yes ; then
    AC_DEFINE(M_GUARD,1,[Define to use the (obsolete) malloc guarding feature])
fi

# Implementation of the --with-capabilities switch.
# Check whether we want to use Linux capabilities
AC_MSG_CHECKING([whether use of capabilities is requested])
AC_ARG_WITH(capabilities,
            AC_HELP_STRING([--with-capabilities],
                           [Use linux capabilities [default=no]]),
            [use_capabilities="$withval"],[use_capabilities=no])
AC_MSG_RESULT($use_capabilities)

# Implementation of the --enable-hmac-binary-check.
AC_MSG_CHECKING([whether a HMAC binary check is requested])
AC_ARG_ENABLE(hmac-binary-check,
              AC_HELP_STRING([--enable-hmac-binary-check],
                             [Enable library integrity check]),
              [use_hmac_binary_check=$enableval],
              [use_hmac_binary_check=no])
AC_MSG_RESULT($use_hmac_binary_check)
if test "$use_hmac_binary_check" = yes ; then
    AC_DEFINE(ENABLE_HMAC_BINARY_CHECK,1,
              [Define to support an HMAC based integrity check])
fi


# Implementation of the --disable-padlock-support switch.
AC_MSG_CHECKING([whether padlock support is requested])
AC_ARG_ENABLE(padlock-support,
              AC_HELP_STRING([--disable-padlock-support],
       	         [Disable support for the PadLock Engine of VIA processors]),
	      padlocksupport=$enableval,padlocksupport=yes)
AC_MSG_RESULT($padlocksupport)
if test x"$padlocksupport" = xyes ; then
  AC_DEFINE(ENABLE_PADLOCK_SUPPORT, 1,
            [Enable support for the PadLock engine.])
fi



AC_DEFINE_UNQUOTED(PRINTABLE_OS_NAME, "$PRINTABLE_OS_NAME",
                   [A human readable text with the name of the OS])

# For some systems we know that we have ld_version scripts.  
# Use it then as default.
have_ld_version_script=no
case "${host}" in
    *-*-linux*)
	have_ld_version_script=yes
        ;;
    *-*-gnu*)
	have_ld_version_script=yes
        ;;
esac
AC_ARG_ENABLE([ld-version-script],
              AC_HELP_STRING([--enable-ld-version-script],
                             [enable/disable use of linker version script.
                              (default is system dependent)]),
              [have_ld_version_script=$enableval],
              [ : ] )
AM_CONDITIONAL(HAVE_LD_VERSION_SCRIPT, test "$have_ld_version_script" = "yes")

AC_DEFINE_UNQUOTED(NAME_OF_DEV_RANDOM, "$NAME_OF_DEV_RANDOM",
                   [defined to the name of the strong random device])
AC_DEFINE_UNQUOTED(NAME_OF_DEV_URANDOM, "$NAME_OF_DEV_URANDOM",
                   [defined to the name of the weaker random device])

###############################
#### Checks for libraries. ####
###############################

#
# gpg-error is required.
#
AM_PATH_GPG_ERROR("$NEED_GPG_ERROR_VERSION")
if test "x$GPG_ERROR_LIBS" = "x"; then
  AC_MSG_ERROR([libgpg-error is needed.
                See ftp://ftp.gnupg.org/gcrypt/libgpg-error/ .])
fi

AC_DEFINE(GPG_ERR_SOURCE_DEFAULT, GPG_ERR_SOURCE_GCRYPT,
          [The default error source for libgcrypt.])

#
# Check whether the GNU Pth library is available.  We require this 
# to build the optional gcryptrnd program.
#
AC_ARG_WITH(pth-prefix,
            AC_HELP_STRING([--with-pth-prefix=PFX],
                           [prefix where GNU Pth is installed (optional)]),
     pth_config_prefix="$withval", pth_config_prefix="")
if test x$pth_config_prefix != x ; then
   PTH_CONFIG="$pth_config_prefix/bin/pth-config"
fi
if test "$use_random_daemon" = "yes"; then
  AC_PATH_PROG(PTH_CONFIG, pth-config, no)
  if test "$PTH_CONFIG" = "no"; then
    AC_MSG_WARN([[
***
*** To build the Libgcrypt's random number daemon
*** we need the support of the GNU Portable Threads Library.
*** Download it from ftp://ftp.gnu.org/gnu/pth/
*** On a Debian GNU/Linux system you might want to try 
***   apt-get install libpth-dev
***]])
  else
    GNUPG_PTH_VERSION_CHECK([1.3.7])
    if test $have_pth = yes; then      
       PTH_CFLAGS=`$PTH_CONFIG --cflags`
       PTH_LIBS=`$PTH_CONFIG --ldflags`
       PTH_LIBS="$PTH_LIBS `$PTH_CONFIG --libs --all`"
       AC_DEFINE(USE_GNU_PTH, 1,
                [Defined if the GNU Portable Thread Library should be used])
       AC_DEFINE(HAVE_PTH, 1,
                [Defined if the GNU Pth is available])
    fi
  fi
fi
AC_SUBST(PTH_CFLAGS)
AC_SUBST(PTH_LIBS)


# Solaris needs -lsocket and -lnsl. Unisys system includes
# gethostbyname in libsocket but needs libnsl for socket.
AC_SEARCH_LIBS(setsockopt, [socket], ,
	[AC_SEARCH_LIBS(setsockopt, [socket], , , [-lnsl])])
AC_SEARCH_LIBS(setsockopt, [nsl])

##################################
#### Checks for header files. ####
##################################

AC_HEADER_STDC
AC_CHECK_HEADERS(unistd.h sys/select.h)

##########################################
#### Checks for typedefs, structures, ####
####  and compiler characteristics.   ####
##########################################

AC_C_CONST
AC_C_INLINE
AC_TYPE_SIZE_T
AC_TYPE_SIGNAL
AC_DECL_SYS_SIGLIST

GNUPG_CHECK_TYPEDEF(byte, HAVE_BYTE_TYPEDEF)
GNUPG_CHECK_TYPEDEF(ushort, HAVE_USHORT_TYPEDEF)
GNUPG_CHECK_TYPEDEF(ulong, HAVE_ULONG_TYPEDEF)
GNUPG_CHECK_TYPEDEF(u16, HAVE_U16_TYPEDEF)
GNUPG_CHECK_TYPEDEF(u32, HAVE_U32_TYPEDEF)

gl_TYPE_SOCKLEN_T
case "${host}" in
  *-*-mingw32*)
    # socklen_t may or may not be defined depending on what headers
    # are included.  To be safe we use int as this is the actual type.
    FALLBACK_SOCKLEN_T="typedef int gcry_socklen_t;"
    ;;
  *)
    if test ".$gl_cv_socklen_t_equiv" = "."; then
      FALLBACK_SOCKLEN_T="typedef socklen_t gcry_socklen_t;"
    else
      FALLBACK_SOCKLEN_T="typedef ${gl_cv_socklen_t_equiv} gcry_socklen_t;"
    fi
esac
AC_SUBST(FALLBACK_SOCKLEN_T)
            
#            
# Check for ELF visibility support.
#
AC_CACHE_CHECK(whether the visibility attribute is supported,
       gcry_cv_visibility_attribute,
       [gcry_cv_visibility_attribute=no
        AC_LANG_CONFTEST([AC_LANG_SOURCE(
          [[int foo __attribute__ ((visibility ("hidden"))) = 1;
            int bar __attribute__ ((visibility ("protected"))) = 1;
          ]])])
        
        if ${CC-cc} -Werror -S conftest.c -o conftest.s \
                  1>&AS_MESSAGE_LOG_FD 2>&AS_MESSAGE_LOG_FD ; then
            if grep '\.hidden.*foo' conftest.s >/dev/null 2>&1 ; then
                if grep '\.protected.*bar' conftest.s >/dev/null 2>&1; then
                    gcry_cv_visibility_attribute=yes
                fi
            fi  
        fi  
       ])
if test "$gcry_cv_visibility_attribute" = "yes"; then
    AC_CACHE_CHECK(for broken visibility attribute,
       gcry_cv_broken_visibility_attribute,
       [gcry_cv_broken_visibility_attribute=yes
        AC_LANG_CONFTEST([AC_LANG_SOURCE(
          [[int foo (int x);
            int bar (int x) __asm__ ("foo")
                            __attribute__ ((visibility ("hidden")));
            int bar (int x) { return x; }
          ]])])
                  
        if ${CC-cc} -Werror -S conftest.c -o conftest.s \
                  1>&AS_MESSAGE_LOG_FD 2>&AS_MESSAGE_LOG_FD ; then
           if grep '\.hidden@<:@ 	_@:>@foo' conftest.s >/dev/null 2>&1;
            then
               gcry_cv_broken_visibility_attribute=no
           fi
        fi
       ])
fi
if test "$gcry_cv_visibility_attribute" = "yes"; then
    AC_CACHE_CHECK(for broken alias attribute,
       gcry_cv_broken_alias_attribute,
       [gcry_cv_broken_alias_attribute=yes
        AC_LANG_CONFTEST([AC_LANG_SOURCE(
          [[extern int foo (int x) __asm ("xyzzy");
            int bar (int x) { return x; }
            extern __typeof (bar) foo __attribute ((weak, alias ("bar")));
            extern int dfoo;
            extern __typeof (dfoo) dfoo __asm ("abccb");
            int dfoo = 1;
          ]])])

        if ${CC-cc} -Werror -S conftest.c -o conftest.s \
                  1>&AS_MESSAGE_LOG_FD 2>&AS_MESSAGE_LOG_FD ; then
           if grep 'xyzzy' conftest.s >/dev/null 2>&1 && \
              grep 'abccb' conftest.s >/dev/null 2>&1; then
              gcry_cv_broken_alias_attribute=no
           fi
        fi
        ])
fi
if test "$gcry_cv_visibility_attribute" = "yes"; then
    AC_CACHE_CHECK(if gcc supports -fvisibility=hidden,
       gcry_cv_gcc_has_f_visibility,
       [gcry_cv_gcc_has_f_visibility=no
        _gcc_cflags_save=$CFLAGS
        CFLAGS="-fvisibility=hidden"
        AC_COMPILE_IFELSE(AC_LANG_PROGRAM([]),
                          gcry_cv_gcc_has_f_visibility=yes)
        CFLAGS=$_gcc_cflags_save;
       ])
fi
if test "$gcry_cv_visibility_attribute" = "yes" \
   && test "$gcry_cv_broken_visibility_attribute" != "yes" \
   && test "$gcry_cv_broken_alias_attribute" != "yes" \
   && test "$gcry_cv_gcc_has_f_visibility" = "yes"  
 then
   AC_DEFINE(GCRY_USE_VISIBILITY, 1,
               [Define to use the GNU C visibility attribute.])
   CFLAGS="$CFLAGS -fvisibility=hidden"
fi


#######################################
#### Checks for library functions. ####
#######################################

AC_FUNC_VPRINTF
# We have replacements for these in src/missing-string.c
AC_CHECK_FUNCS(stpcpy strcasecmp)
# We have replacements for these in src/g10lib.h
AC_CHECK_FUNCS(strtoul memmove stricmp atexit raise)
# Other checks
AC_CHECK_FUNCS(strerror rand mmap getpagesize sysconf waitpid wait4)
AC_CHECK_FUNCS(gettimeofday getrusage gethrtime clock_gettime syslog)
AC_CHECK_FUNCS(fcntl ftruncate)

GNUPG_CHECK_MLOCK


#
# Check wether it is necessary to link against libdl.
#
DL_LIBS=""
if test "$use_hmac_binary_check" = yes ; then
  _gcry_save_libs="$LIBS"
  LIBS=""
  AC_SEARCH_LIBS(dlopen, c dl,,,)
  DL_LIBS=$LIBS
  LIBS="$_gcry_save_libs"
  LIBGCRYPT_CONFIG_LIBS="${LIBGCRYPT_CONFIG_LIBS} ${DL_LIBS}"
fi
AC_SUBST(DL_LIBS)


#
# Check whether we can use Linux capabilities as requested.
#
if test "$use_capabilities" = "yes" ; then
use_capabilities=no
AC_CHECK_HEADERS(sys/capability.h)
if test "$ac_cv_header_sys_capability_h" = "yes" ; then
  AC_CHECK_LIB(cap, cap_init, ac_need_libcap=1)
  if test "$ac_cv_lib_cap_cap_init" = "yes"; then
     AC_DEFINE(USE_CAPABILITIES,1,
               [define if capabilities should be used])
     LIBS="$LIBS -lcap"
     use_capabilities=yes
  fi
fi
if test "$use_capabilities" = "no" ; then
    AC_MSG_WARN([[
***
*** The use of capabilities on this system is not possible.
*** You need a recent Linux kernel and some patches:
***   fcaps-2.2.9-990610.patch      (kernel patch for 2.2.9)
***   fcap-module-990613.tar.gz     (kernel module)
***   libcap-1.92.tar.gz            (user mode library and utilities)
*** And you have to configure the kernel with CONFIG_VFS_CAP_PLUGIN
*** set (filesystems menu). Be warned: This code is *really* ALPHA.
***]])
fi
fi

# Check whether a random device is available.
if test "$try_dev_random" = yes ; then
    AC_CACHE_CHECK(for random device, ac_cv_have_dev_random,
    [if test -r "$NAME_OF_DEV_RANDOM" && test -r "$NAME_OF_DEV_URANDOM" ; then
      ac_cv_have_dev_random=yes; else ac_cv_have_dev_random=no; fi])
    if test "$ac_cv_have_dev_random" = yes; then
        AC_DEFINE(HAVE_DEV_RANDOM,1,
                 [defined if the system supports a random device] )
    fi
else
    AC_MSG_CHECKING(for random device)
    ac_cv_have_dev_random=no
    AC_MSG_RESULT(has been disabled)
fi

# Figure out the random modules for this configuration.
if test "$random" = "default"; then

    # Select default value.
    if test "$ac_cv_have_dev_random" = yes; then
        # Try Linuxish random device.
        random_modules="linux"
    else
        case "${host}" in
        *-*-mingw32*|*-*-cygwin*)
          # Windows random device.
          random_modules="w32"
          ;;
        *)
          # Build everything, allow to select at runtime.
          random_modules="$auto_random_modules"
          ;;
        esac    
    fi
else
    if test "$random" = "auto"; then
        # Build everything, allow to select at runtime.
        random_modules="$auto_random_modules"
    else
        random_modules="$random"
    fi
fi


#
# Setup assembler stuff.
#
GNUPG_SYS_SYMBOL_UNDERSCORE()
AC_ARG_ENABLE(mpi-path,
              AC_HELP_STRING([--enable-mpi-path=EXTRA_PATH],
	      [prepend EXTRA_PATH to list of CPU specific optimizations]),
	      mpi_extra_path="$enableval",mpi_extra_path="")
AC_MSG_CHECKING(for mpi assembler functions)
if test -f $srcdir/mpi/config.links ; then
    . $srcdir/mpi/config.links
    AC_CONFIG_LINKS("$mpi_ln_list")
    ac_cv_mpi_sflags="$mpi_sflags"
    AC_MSG_RESULT(done)
else
    AC_MSG_RESULT(failed)
    AC_MSG_ERROR([mpi/config.links missing!])
fi
MPI_SFLAGS="$ac_cv_mpi_sflags"
AC_SUBST(MPI_SFLAGS)

AM_CONDITIONAL(MPI_MOD_ASM_MPIH_ADD1, test "$mpi_mod_asm_mpih_add1" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_SUB1, test "$mpi_mod_asm_mpih_sub1" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_MUL1, test "$mpi_mod_asm_mpih_mul1" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_MUL2, test "$mpi_mod_asm_mpih_mul2" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_MUL3, test "$mpi_mod_asm_mpih_mul3" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_LSHIFT, test "$mpi_mod_asm_mpih_lshift" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_MPIH_RSHIFT, test "$mpi_mod_asm_mpih_rshift" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_UDIV, test "$mpi_mod_asm_udiv" = yes)
AM_CONDITIONAL(MPI_MOD_ASM_UDIV_QRNND, test "$mpi_mod_asm_udiv_qrnnd" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_ADD1, test "$mpi_mod_c_mpih_add1" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_SUB1, test "$mpi_mod_c_mpih_sub1" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_MUL1, test "$mpi_mod_c_mpih_mul1" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_MUL2, test "$mpi_mod_c_mpih_mul2" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_MUL3, test "$mpi_mod_c_mpih_mul3" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_LSHIFT, test "$mpi_mod_c_mpih_lshift" = yes)
AM_CONDITIONAL(MPI_MOD_C_MPIH_RSHIFT, test "$mpi_mod_c_mpih_rshift" = yes)
AM_CONDITIONAL(MPI_MOD_C_UDIV, test "$mpi_mod_c_udiv" = yes)
AM_CONDITIONAL(MPI_MOD_C_UDIV_QRNND, test "$mpi_mod_c_udiv_qrnnd" = yes)

if test "$is_development_version" = "yes"; then
    AC_DEFINE(IS_DEVELOPMENT_VERSION,1,
              [Defined if this is not a regular release])
fi


AM_CONDITIONAL(CROSS_COMPILING, test x$cross_compiling = xyes)


# This is handy for debugging so the compiler doesn't rearrange
# things and eliminate variables.
AC_ARG_ENABLE(optimization,
       AC_HELP_STRING([--disable-optimization],
		      [disable compiler optimization]),
                      [if test $enableval = no ; then
                         CFLAGS=`echo $CFLAGS | sed 's/-O[[0-9]]//'`
                       fi])

# CFLAGS mangling when using gcc.
if test "$GCC" = yes; then
    CFLAGS="$CFLAGS -Wall"
    if test "$USE_MAINTAINER_MODE" = "yes"; then
        CFLAGS="$CFLAGS -Wcast-align -Wshadow -Wstrict-prototypes"
        CFLAGS="$CFLAGS -Wformat -Wno-format-y2k -Wformat-security"

        # If -Wno-missing-field-initializers is supported we can enable a 
        # a bunch of really useful warnings.
        AC_MSG_CHECKING([if gcc supports -Wno-missing-field-initializers])
        _gcc_cflags_save=$CFLAGS
        CFLAGS="-Wno-missing-field-initializers"
        AC_COMPILE_IFELSE(AC_LANG_PROGRAM([]),_gcc_wopt=yes,_gcc_wopt=no)
        AC_MSG_RESULT($_gcc_wopt)
        CFLAGS=$_gcc_cflags_save;
        if test x"$_gcc_wopt" = xyes ; then
          CFLAGS="$CFLAGS -W -Wextra -Wbad-function-cast"
          CFLAGS="$CFLAGS -Wwrite-strings"
          CFLAGS="$CFLAGS -Wdeclaration-after-statement"
          CFLAGS="$CFLAGS -Wno-missing-field-initializers"
          CFLAGS="$CFLAGS -Wno-sign-compare"
        fi

        AC_MSG_CHECKING([if gcc supports -Wpointer-arith])
        _gcc_cflags_save=$CFLAGS
        CFLAGS="-Wpointer-arith"
        AC_COMPILE_IFELSE(AC_LANG_PROGRAM([]),_gcc_wopt=yes,_gcc_wopt=no)
        AC_MSG_RESULT($_gcc_wopt)
        CFLAGS=$_gcc_cflags_save;
        if test x"$_gcc_wopt" = xyes ; then
          CFLAGS="$CFLAGS -Wpointer-arith"
        fi
    fi

fi

# Check whether as(1) supports a noeexecstack feature.  This test
# includes an override option.
CL_AS_NOEXECSTACK


AC_SUBST(LIBGCRYPT_CONFIG_API_VERSION)
AC_SUBST(LIBGCRYPT_CONFIG_LIBS)
AC_SUBST(LIBGCRYPT_CONFIG_CFLAGS)
AC_SUBST(LIBGCRYPT_THREAD_MODULES)

AC_CONFIG_COMMANDS([gcrypt-conf],[[
chmod +x src/libgcrypt-config
]],[[
prefix=$prefix
exec_prefix=$exec_prefix
libdir=$libdir
datadir=$datadir
DATADIRNAME=$DATADIRNAME
]])

#####################
#### Conclusion. ####
#####################

# Define conditional sources and config.h symbols depending on the
# selected ciphers, pubkey-ciphers, digests and random modules.

LIST_MEMBER(arcfour, $enabled_ciphers)
if test "$found" = "1"; then 
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS arcfour.lo"
   AC_DEFINE(USE_ARCFOUR, 1, [Defined if this module should be included])
fi

LIST_MEMBER(blowfish, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS blowfish.lo"
   AC_DEFINE(USE_BLOWFISH, 1, [Defined if this module should be included])
fi

LIST_MEMBER(cast5, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS cast5.lo"
   AC_DEFINE(USE_CAST5, 1, [Defined if this module should be included])
fi

LIST_MEMBER(des, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS des.lo"
   AC_DEFINE(USE_DES, 1, [Defined if this module should be included])
fi

LIST_MEMBER(aes, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS rijndael.lo"
   AC_DEFINE(USE_AES, 1, [Defined if this module should be included])
fi

LIST_MEMBER(twofish, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS twofish.lo"
   AC_DEFINE(USE_TWOFISH, 1, [Defined if this module should be included])
fi

LIST_MEMBER(serpent, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS serpent.lo"
   AC_DEFINE(USE_SERPENT, 1, [Defined if this module should be included])
fi

LIST_MEMBER(rfc2268, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS rfc2268.lo"
   AC_DEFINE(USE_RFC2268, 1, [Defined if this module should be included])
fi

LIST_MEMBER(seed, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS seed.lo"
   AC_DEFINE(USE_SEED, 1, [Defined if this module should be included])
fi

LIST_MEMBER(camellia, $enabled_ciphers)
if test "$found" = "1" ; then
   GCRYPT_CIPHERS="$GCRYPT_CIPHERS camellia.lo camellia-glue.lo"
   AC_DEFINE(USE_CAMELLIA, 1, [Defined if this module should be included])
fi

LIST_MEMBER(dsa, $enabled_pubkey_ciphers)
if test "$found" = "1" ; then
   GCRYPT_PUBKEY_CIPHERS="$GCRYPT_PUBKEY_CIPHERS dsa.lo"
   AC_DEFINE(USE_DSA, 1, [Defined if this module should be included])
fi

LIST_MEMBER(rsa, $enabled_pubkey_ciphers)
if test "$found" = "1" ; then
   GCRYPT_PUBKEY_CIPHERS="$GCRYPT_PUBKEY_CIPHERS rsa.lo"
   AC_DEFINE(USE_RSA, 1, [Defined if this module should be included])
fi

LIST_MEMBER(elgamal, $enabled_pubkey_ciphers)
if test "$found" = "1" ; then
   GCRYPT_PUBKEY_CIPHERS="$GCRYPT_PUBKEY_CIPHERS elgamal.lo"
   AC_DEFINE(USE_ELGAMAL, 1, [Defined if this module should be included])
fi

LIST_MEMBER(ecc, $enabled_pubkey_ciphers)
if test "$found" = "1" ; then
   GCRYPT_PUBKEY_CIPHERS="$GCRYPT_PUBKEY_CIPHERS ecc.lo"
   AC_DEFINE(USE_ECC, 1, [Defined if this module should be included])
fi

LIST_MEMBER(crc, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS crc.lo"
   AC_DEFINE(USE_CRC, 1, [Defined if this module should be included])
fi

LIST_MEMBER(md4, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS md4.lo"
   AC_DEFINE(USE_MD4, 1, [Defined if this module should be included])
fi

LIST_MEMBER(md5, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS md5.lo"
   AC_DEFINE(USE_MD5, 1, [Defined if this module should be included])
fi

LIST_MEMBER(sha256, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS sha256.lo"
   AC_DEFINE(USE_SHA256, 1, [Defined if this module should be included])
fi

LIST_MEMBER(sha512, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS sha512.lo"
   AC_DEFINE(USE_SHA512, 1, [Defined if this module should be included])
fi

LIST_MEMBER(tiger, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS tiger.lo"
   AC_DEFINE(USE_TIGER, 1, [Defined if this module should be included])
fi

LIST_MEMBER(whirlpool, $enabled_digests)
if test "$found" = "1" ; then
   GCRYPT_DIGESTS="$GCRYPT_DIGESTS whirlpool.lo"
   AC_DEFINE(USE_WHIRLPOOL, 1, [Defined if this module should be included])
fi

# rmd160 and sha1 should be included always.
GCRYPT_DIGESTS="$GCRYPT_DIGESTS rmd160.lo sha1.lo"
AC_DEFINE(USE_RMD160, 1, [Defined if this module should be included])
AC_DEFINE(USE_SHA1, 1,   [Defined if this module should be included])

LIST_MEMBER(linux, $random_modules)
if test "$found" = "1" ; then
   GCRYPT_RANDOM="$GCRYPT_RANDOM rndlinux.lo"
   AC_DEFINE(USE_RNDLINUX, 1, [Defined if the /dev/random RNG should be used.])
fi

LIST_MEMBER(unix, $random_modules)
if test "$found" = "1" ; then
   GCRYPT_RANDOM="$GCRYPT_RANDOM rndunix.lo"
   AC_DEFINE(USE_RNDUNIX, 1, [Defined if the default Unix RNG should be used.])
   print_egd_notice=yes
fi

LIST_MEMBER(egd, $random_modules)
if test "$found" = "1" ; then
   GCRYPT_RANDOM="$GCRYPT_RANDOM rndegd.lo"
   AC_DEFINE(USE_RNDEGD, 1, [Defined if the EGD based RNG should be used.])
fi

LIST_MEMBER(w32, $random_modules)
if test "$found" = "1" ; then
   GCRYPT_RANDOM="$GCRYPT_RANDOM rndw32.lo"
   AC_DEFINE(USE_RNDW32, 1, 
             [Defined if the Windows specific RNG should be used.])
fi

AC_SUBST([GCRYPT_CIPHERS])
AC_SUBST([GCRYPT_PUBKEY_CIPHERS])
AC_SUBST([GCRYPT_DIGESTS])
AC_SUBST([GCRYPT_RANDOM])

AC_SUBST(LIBGCRYPT_CIPHERS, $enabled_ciphers)
AC_SUBST(LIBGCRYPT_PUBKEY_CIPHERS, $enabled_pubkey_ciphers)
AC_SUBST(LIBGCRYPT_DIGESTS, $enabled_digests)

# For printing the configuration we need a colon separated list of
# algorithm names.
tmp=`echo "$enabled_ciphers" | tr ' ' : `
AC_DEFINE_UNQUOTED(LIBGCRYPT_CIPHERS, "$tmp", 
                   [List of available cipher algorithms])
tmp=`echo "$enabled_pubkey_ciphers" | tr ' ' : `
AC_DEFINE_UNQUOTED(LIBGCRYPT_PUBKEY_CIPHERS, "$tmp", 
                   [List of available public key cipher algorithms])
tmp=`echo "$enabled_digests" | tr ' ' : `
AC_DEFINE_UNQUOTED(LIBGCRYPT_DIGESTS, "$tmp", 
                   [List of available digest algorithms])



# Generate extended version information for W32.
if test "$have_w32_system" = yes; then
   BUILD_TIMESTAMP=`date --iso-8601=minutes`
   changequote(,)dnl 
   BUILD_FILEVERSION=`echo "$VERSION" | sed 's/\([0-9.]*\).*/\1./;s/\./,/g'`
   changequote([,])dnl
   BUILD_FILEVERSION="${BUILD_FILEVERSION}${BUILD_REVISION}"
fi
AC_SUBST(BUILD_REVISION)
AC_SUBST(BUILD_TIMESTAMP)
AC_SUBST(BUILD_FILEVERSION)
AC_DEFINE_UNQUOTED(BUILD_REVISION, "$BUILD_REVISION", 
                   [Subversion revision used to build this package])



# And create the files.
AC_CONFIG_FILES([
Makefile
m4/Makefile
mpi/Makefile
cipher/Makefile
random/Makefile
doc/Makefile
src/Makefile
src/gcrypt.h
src/libgcrypt-config
src/versioninfo.rc
tests/Makefile
])
AC_OUTPUT

# Give some feedback
echo "
        Libgcrypt v${VERSION} has been configured as follows:
        
        Platform:  $PRINTABLE_OS_NAME ($host)
"


if test "$print_egd_notice" = "yes"; then
cat <<G10EOF
  
   The performance of the Unix random gatherer module (rndunix) is not
   very good and it does not keep the entropy pool over multiple
   invocations of Libgcrypt base applications.  The suggested way to
   overcome this problem is to use the
  
                 Entropy Gathering Daemon (EGD)
  
   which provides a entropy source for the whole system.  It is written
   in Perl and available at the GnuPG FTP servers.  To enable EGD you
   should rerun configure with the option "--enable-static-rnd=egd".
   For more information consult the GnuPG webpages:
  
             http://www.gnupg.org/download.html#egd

G10EOF
fi

if test -n "$gpl"; then
  echo "Please note that you are building a version of Libgcrypt with"
  echo "  $gpl"
  echo "included.  These parts are licensed under the GPL and thus the"
  echo "use of this library has to comply with the conditions of the GPL."
fi

