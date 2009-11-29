dnl macros to configure Libgcrypt
dnl Copyright (C) 1998, 1999, 2000, 2001, 2002,
dnl               2003 Free Software Foundation, Inc.
dnl
dnl This file is part of Libgcrypt.
dnl
dnl Libgcrypt is free software; you can redistribute it and/or modify
dnl it under the terms of the GNU Lesser General Public License as
dnl published by the Free Software Foundation; either version 2.1 of
dnl the License, or (at your option) any later version.
dnl
dnl Libgcrypt is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
dnl GNU Lesser General Public License for more details.
dnl
dnl You should have received a copy of the GNU Lesser General Public
dnl License along with this program; if not, write to the Free Software
dnl Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA

dnl GNUPG_MSG_PRINT(STRING)
dnl print a message
dnl
define([GNUPG_MSG_PRINT],
  [ echo $ac_n "$1"" $ac_c" 1>&AS_MESSAGE_FD([])
  ])

dnl GNUPG_CHECK_TYPEDEF(TYPE, HAVE_NAME)
dnl Check whether a typedef exists and create a #define $2 if it exists
dnl
AC_DEFUN([GNUPG_CHECK_TYPEDEF],
  [ AC_MSG_CHECKING(for $1 typedef)
    AC_CACHE_VAL(gnupg_cv_typedef_$1,
    [AC_TRY_COMPILE([#define _GNU_SOURCE 1
    #include <stdlib.h>
    #include <sys/types.h>], [
    #undef $1
    int a = sizeof($1);
    ], gnupg_cv_typedef_$1=yes, gnupg_cv_typedef_$1=no )])
    AC_MSG_RESULT($gnupg_cv_typedef_$1)
    if test "$gnupg_cv_typedef_$1" = yes; then
        AC_DEFINE($2,1,[Defined if a `]$1[' is typedef'd])
    fi
  ])


dnl GNUPG_CHECK_GNUMAKE
dnl
AC_DEFUN([GNUPG_CHECK_GNUMAKE],
  [
    if ${MAKE-make} --version 2>/dev/null | grep '^GNU ' >/dev/null 2>&1; then
        :
    else
        AC_MSG_WARN([[
***
*** It seems that you are not using GNU make.  Some make tools have serious
*** flaws and you may not be able to build this software at all. Before you
*** complain, please try GNU make:  GNU make is easy to build and available
*** at all GNU archives.  It is always available from ftp.gnu.org:/gnu/make.
***]])
    fi
  ])


#
# GNUPG_SYS_SYMBOL_UNDERSCORE
# Does the compiler prefix global symbols with an underscore?
#
# Taken from GnuPG 1.2 and modified to use the libtool macros.
AC_DEFUN([GNUPG_SYS_SYMBOL_UNDERSCORE],
[tmp_do_check="no"
case "${host}" in
    *-mingw32msvc*)
        ac_cv_sys_symbol_underscore=yes
        ;;
    i386-emx-os2 | i[3456]86-pc-os2*emx | i386-pc-msdosdjgpp)
        ac_cv_sys_symbol_underscore=yes
        ;;
    *)
      if test "$cross_compiling" = yes; then
        if test "x$ac_cv_sys_symbol_underscore" = x ; then
           ac_cv_sys_symbol_underscore=yes
        fi
      else
         tmp_do_check="yes"
      fi
       ;;
esac
if test "$tmp_do_check" = "yes"; then
  AC_REQUIRE([AC_LIBTOOL_SYS_GLOBAL_SYMBOL_PIPE])
  AC_MSG_CHECKING([for _ prefix in compiled symbols])
  AC_CACHE_VAL(ac_cv_sys_symbol_underscore,
  [ac_cv_sys_symbol_underscore=no
   cat > conftest.$ac_ext <<EOF
      void nm_test_func(){}
      int main(){nm_test_func;return 0;}
EOF
  if AC_TRY_EVAL(ac_compile); then
    # Now try to grab the symbols.
    ac_nlist=conftest.nm
    if AC_TRY_EVAL(NM conftest.$ac_objext \| $lt_cv_sys_global_symbol_pipe \| cut -d \' \' -f 2 \> $ac_nlist) && test -s "$ac_nlist"; then
      # See whether the symbols have a leading underscore.
      if egrep '^_nm_test_func' "$ac_nlist" >/dev/null; then
        ac_cv_sys_symbol_underscore=yes
      else
        if egrep '^nm_test_func ' "$ac_nlist" >/dev/null; then
          :
        else
          echo "configure: cannot find nm_test_func in $ac_nlist" >&AC_FD_CC
        fi
      fi
    else
      echo "configure: cannot run $lt_cv_sys_global_symbol_pipe" >&AC_FD_CC
    fi
  else
    echo "configure: failed program was:" >&AC_FD_CC
    cat conftest.c >&AC_FD_CC
  fi
  rm -rf conftest*
  ])
  else
  AC_MSG_CHECKING([for _ prefix in compiled symbols])
  fi
AC_MSG_RESULT($ac_cv_sys_symbol_underscore)
if test x$ac_cv_sys_symbol_underscore = xyes; then
  AC_DEFINE(WITH_SYMBOL_UNDERSCORE,1,
            [Defined if compiled symbols have a leading underscore])
fi
])


######################################################################
# Check whether mlock is broken (hpux 10.20 raises a SIGBUS if mlock
# is not called from uid 0 (not tested whether uid 0 works)
# For DECs Tru64 we have also to check whether mlock is in librt
# mlock is there a macro using memlk()
######################################################################
dnl GNUPG_CHECK_MLOCK
dnl
define(GNUPG_CHECK_MLOCK,
  [ AC_CHECK_FUNCS(mlock)
    if test "$ac_cv_func_mlock" = "no"; then
        AC_CHECK_HEADERS(sys/mman.h)
        if test "$ac_cv_header_sys_mman_h" = "yes"; then
            # Add librt to LIBS:
            AC_CHECK_LIB(rt, memlk)
            AC_CACHE_CHECK([whether mlock is in sys/mman.h],
                            gnupg_cv_mlock_is_in_sys_mman,
                [AC_TRY_LINK([
                    #include <assert.h>
                    #ifdef HAVE_SYS_MMAN_H
                    #include <sys/mman.h>
                    #endif
                ], [
int i;

/* glibc defines this for functions which it implements
 * to always fail with ENOSYS.  Some functions are actually
 * named something starting with __ and the normal name
 * is an alias.  */
#if defined (__stub_mlock) || defined (__stub___mlock)
choke me
#else
mlock(&i, 4);
#endif
; return 0;
                ],
                gnupg_cv_mlock_is_in_sys_mman=yes,
                gnupg_cv_mlock_is_in_sys_mman=no)])
            if test "$gnupg_cv_mlock_is_in_sys_mman" = "yes"; then
                AC_DEFINE(HAVE_MLOCK,1,
                          [Defined if the system supports an mlock() call])
            fi
        fi
    fi
    if test "$ac_cv_func_mlock" = "yes"; then
        AC_CHECK_FUNCS(sysconf getpagesize)
        AC_MSG_CHECKING(whether mlock is broken)
          AC_CACHE_VAL(gnupg_cv_have_broken_mlock,
             AC_TRY_RUN([
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>

int main()
{
    char *pool;
    int err;
    long int pgsize;

#if defined(HAVE_SYSCONF) && defined(_SC_PAGESIZE)
    pgsize = sysconf (_SC_PAGESIZE);
#elif defined (HAVE_GETPAGESIZE)
    pgsize = getpagesize();       
#else
    pgsize = -1;
#endif

    if (pgsize == -1)
      pgsize = 4096;

    pool = malloc( 4096 + pgsize );
    if( !pool )
        return 2;
    pool += (pgsize - ((long int)pool % pgsize));

    err = mlock( pool, 4096 );
    if( !err || errno == EPERM )
        return 0; /* okay */

    return 1;  /* hmmm */
}

            ],
            gnupg_cv_have_broken_mlock="no",
            gnupg_cv_have_broken_mlock="yes",
            gnupg_cv_have_broken_mlock="assume-no"
           )
         )
         if test "$gnupg_cv_have_broken_mlock" = "yes"; then
             AC_DEFINE(HAVE_BROKEN_MLOCK,1,
                       [Defined if the mlock() call does not work])
             AC_MSG_RESULT(yes)
         else
            if test "$gnupg_cv_have_broken_mlock" = "no"; then
                AC_MSG_RESULT(no)
            else
                AC_MSG_RESULT(assuming no)
            fi
         fi
    fi
  ])

# GNUPG_SYS_LIBTOOL_CYGWIN32 - find tools needed on cygwin32
AC_DEFUN([GNUPG_SYS_LIBTOOL_CYGWIN32],
[AC_CHECK_TOOL(DLLTOOL, dlltool, false)
AC_CHECK_TOOL(AS, as, false)
])

dnl LIST_MEMBER()
dnl Check wether an element ist contained in a list.  Set `found' to
dnl `1' if the element is found in the list, to `0' otherwise.
AC_DEFUN([LIST_MEMBER],
[
name=$1
list=$2
found=0

for n in $list; do
  if test "x$name" = "x$n"; then
    found=1
  fi
done
])

dnl AM_PATH_GPG_ERROR([MINIMUM-VERSION,
dnl                   [ACTION-IF-FOUND [, ACTION-IF-NOT-FOUND ]]])
dnl Test for libgpg-error and define GPG_ERROR_CFLAGS and GPG_ERROR_LIBS
dnl
AC_DEFUN([AM_PATH_GPG_ERROR],
[ AC_ARG_WITH(gpg-error-prefix,
            AC_HELP_STRING([--with-gpg-error-prefix=PFX],
                           [prefix where GPG Error is installed (optional)]),
     gpg_error_config_prefix="$withval", gpg_error_config_prefix="")
  if test x$gpg_error_config_prefix != x ; then
     if test x${GPG_ERROR_CONFIG+set} != xset ; then
        GPG_ERROR_CONFIG=$gpg_error_config_prefix/bin/gpg-error-config
     fi
  fi

  AC_PATH_PROG(GPG_ERROR_CONFIG, gpg-error-config, no)
  min_gpg_error_version=ifelse([$1], ,0.0,$1)
  AC_MSG_CHECKING(for GPG Error - version >= $min_gpg_error_version)
  ok=no
  if test "$GPG_ERROR_CONFIG" != "no" ; then
    req_major=`echo $min_gpg_error_version | \
               sed 's/\([[0-9]]*\)\.\([[0-9]]*\)/\1/'`
    req_minor=`echo $min_gpg_error_version | \
               sed 's/\([[0-9]]*\)\.\([[0-9]]*\)/\2/'`
    gpg_error_config_version=`$GPG_ERROR_CONFIG $gpg_error_config_args --version`
    if test "$gpg_error_config_version"; then
      major=`echo $gpg_error_config_version | \
                 sed 's/\([[0-9]]*\)\.\([[0-9]]*\).*/\1/'`
      minor=`echo $gpg_error_config_version | \
                 sed 's/\([[0-9]]*\)\.\([[0-9]]*\).*/\2/'`
      if test "$major" -gt "$req_major"; then
          ok=yes
      else 
          if test "$major" -eq "$req_major"; then
              if test "$minor" -ge "$req_minor"; then
                 ok=yes
              fi
          fi
      fi
    fi
  fi
  if test $ok = yes; then
    GPG_ERROR_CFLAGS=`$GPG_ERROR_CONFIG $gpg_error_config_args --cflags`
    GPG_ERROR_LIBS=`$GPG_ERROR_CONFIG $gpg_error_config_args --libs`
    AC_MSG_RESULT(yes)
    ifelse([$2], , :, [$2])
  else
    GPG_ERROR_CFLAGS=""
    GPG_ERROR_LIBS=""
    AC_MSG_RESULT(no)
    ifelse([$3], , :, [$3])
  fi
  AC_SUBST(GPG_ERROR_CFLAGS)
  AC_SUBST(GPG_ERROR_LIBS)
])


dnl Check for socklen_t: historically on BSD it is an int, and in
dnl POSIX 1g it is a type of its own, but some platforms use different
dnl types for the argument to getsockopt, getpeername, etc.  So we
dnl have to test to find something that will work.
AC_DEFUN([TYPE_SOCKLEN_T],
[
   AC_CHECK_TYPE([socklen_t], ,[
      AC_MSG_CHECKING([for socklen_t equivalent])
      AC_CACHE_VAL([socklen_t_equiv],
      [
         # Systems have either "struct sockaddr *" or
         # "void *" as the second argument to getpeername
         socklen_t_equiv=
         for arg2 in "struct sockaddr" void; do
            for t in int size_t unsigned long "unsigned long"; do
               AC_TRY_COMPILE([
#include <sys/types.h>
#include <sys/socket.h>

int getpeername (int, $arg2 *, $t *);
               ],[
                  $t len;
                  getpeername(0,0,&len);
               ],[
                  socklen_t_equiv="$t"
                  break
               ])
            done
         done

         if test "x$socklen_t_equiv" = x; then
            AC_MSG_ERROR([Cannot find a type to use in place of socklen_t])
         fi
      ])
      AC_MSG_RESULT($socklen_t_equiv)
      AC_DEFINE_UNQUOTED(socklen_t, $socklen_t_equiv,
			[type to use in place of socklen_t if not defined])],
      [#include <sys/types.h>
#include <sys/socket.h>])
])


# GNUPG_PTH_VERSION_CHECK(REQUIRED)
# 
# If the version is sufficient, HAVE_PTH will be set to yes.
#
# Taken form the m4 macros which come with Pth
AC_DEFUN([GNUPG_PTH_VERSION_CHECK],
  [
    _pth_version=`$PTH_CONFIG --version | awk 'NR==1 {print [$]3}'`
    _req_version="ifelse([$1],,1.2.0,$1)"

    AC_MSG_CHECKING(for PTH - version >= $_req_version)
    for _var in _pth_version _req_version; do
        eval "_val=\"\$${_var}\""
        _major=`echo $_val | sed 's/\([[0-9]]*\)\.\([[0-9]]*\)\([[ab.]]\)\([[0-9]]*\)/\1/'`
        _minor=`echo $_val | sed 's/\([[0-9]]*\)\.\([[0-9]]*\)\([[ab.]]\)\([[0-9]]*\)/\2/'`
        _rtype=`echo $_val | sed 's/\([[0-9]]*\)\.\([[0-9]]*\)\([[ab.]]\)\([[0-9]]*\)/\3/'`
        _micro=`echo $_val | sed 's/\([[0-9]]*\)\.\([[0-9]]*\)\([[ab.]]\)\([[0-9]]*\)/\4/'`
        case $_rtype in
            "a" ) _rtype=0 ;;
            "b" ) _rtype=1 ;;
            "." ) _rtype=2 ;;
        esac
        _hex=`echo dummy | awk '{ printf("%d%02d%1d%02d", major, minor, rtype, micro); }' \
              "major=$_major" "minor=$_minor" "rtype=$_rtype" "micro=$_micro"`
        eval "${_var}_hex=\"\$_hex\""
    done
    have_pth=no
    if test ".$_pth_version_hex" != .; then
        if test ".$_req_version_hex" != .; then
            if test $_pth_version_hex -ge $_req_version_hex; then
                have_pth=yes
            fi
        fi
    fi
    if test $have_pth = yes; then
       AC_MSG_RESULT(yes)
       AC_MSG_CHECKING([whether PTH installation is sane])
       AC_CACHE_VAL(gnupg_cv_pth_is_sane,[
         _gnupg_pth_save_cflags=$CFLAGS
         _gnupg_pth_save_ldflags=$LDFLAGS
         _gnupg_pth_save_libs=$LIBS
         CFLAGS="$CFLAGS `$PTH_CONFIG --cflags`"
         LDFLAGS="$LDFLAGS `$PTH_CONFIG --ldflags`"
         LIBS="$LIBS `$PTH_CONFIG --libs`"
         AC_LINK_IFELSE([AC_LANG_PROGRAM([#include <pth.h>
                                         ],
                                         [[ pth_init ();]])],
                        gnupg_cv_pth_is_sane=yes,
                        gnupg_cv_pth_is_sane=no)
         CFLAGS=$_gnupg_pth_save_cflags
         LDFLAGS=$_gnupg_pth_save_ldflags
         LIBS=$_gnupg_pth_save_libs
       ])
       if test $gnupg_cv_pth_is_sane != yes; then
          have_pth=no
       fi
       AC_MSG_RESULT($gnupg_cv_pth_is_sane)
    else
       AC_MSG_RESULT(no)
    fi    
  ])


