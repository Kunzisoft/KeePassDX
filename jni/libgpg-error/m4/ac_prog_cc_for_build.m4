dnl Available from the GNU Autoconf Macro Archive at:
dnl http://www.gnu.org/software/ac-archive/htmldoc/ac_prog_cc_for_build.html
dnl
dnl All content of this archive is free software;
dnl you can redistribute it and/or modify it under the terms of the GNU
dnl General Public License as published by the Free Software Foundation;
dnl either version 2, or (at your option) any later version.
dnl
dnl As a special exception, the respective Autoconf Macro's copyright
dnl owner gives unlimited permission to copy, distribute and modify the
dnl configure scripts that are the output of Autoconf when processing the
dnl Macro. You need not follow the terms of the GNU General Public License
dnl when using or distributing such scripts, even though portions of the
dnl text of the Macro appear in them. The GNU General Public License (GPL)
dnl does govern all other use of the material that constitutes the
dnl Autoconf Macro.
dnl 
dnl This special exception to the GPL applies to versions of the Autoconf
dnl Macro released by the GNU Autoconf Macro Archive. When you make and
dnl distribute a modified version of the Autoconf Macro, you may extend
dnl this special exception to the GPL to apply to your modified version as
dnl well.

dnl Synopsis AC_PROG_CC_FOR_BUILD
dnl
dnl This macro searches for a C compiler that generates native
dnl executables, that is a C compiler that surely is not a cross-compiler.
dnl This can be useful if you have to generate source code at compile-time
dnl like for example GCC does.
dnl
dnl The macro sets the CC_FOR_BUILD and CPP_FOR_BUILD macros to anything
dnl needed to compile or link (CC_FOR_BUILD) and preprocess
dnl (CPP_FOR_BUILD).  The value of these variables can be overridden by
dnl the user by specifying a compiler with an environment variable (like
dnl you do for standard CC).
dnl
dnl It also sets BUILD_EXEEXT and BUILD_OBJEXT to the executable and
dnl object file extensions for the build platform, and GCC_FOR_BUILD
dnl to `yes' if the compiler we found is GCC.  All these variables but
dnl GCC_FOR_BUILD are substituted in the Makefile.
dnl
dnl Author Paolo Bonzini <bonzini@gnu.org>
dnl
AC_DEFUN([AC_PROG_CC_FOR_BUILD], [dnl
AC_REQUIRE([AC_PROG_CC])dnl
AC_REQUIRE([AC_PROG_CPP])dnl
AC_REQUIRE([AC_EXEEXT])dnl
AC_REQUIRE([AC_CANONICAL_SYSTEM])dnl
dnl
pushdef([AC_TRY_COMPILER], [
cat > conftest.$ac_ext << EOF
#line __oline__ "configure"
#include "confdefs.h"
[$1]
EOF
# If we can't run a trivial program, we are probably using a cross
compiler.
# Fail miserably.
if AC_TRY_EVAL(ac_link) && test -s conftest${ac_exeext} && (./conftest;
exit) 2>/dev/null; then
  [$2]=yes
else
  echo "configure: failed program was:" >&AC_FD_CC
  cat conftest.$ac_ext >&AC_FD_CC
  [$2]=no
fi
[$3]=no
rm -fr conftest*])dnl

dnl Use the standard macros, but make them use other variable names
dnl
pushdef([cross_compiling], [#])dnl
pushdef([ac_cv_prog_CPP], ac_cv_build_prog_CPP)dnl
pushdef([ac_cv_prog_gcc], ac_cv_build_prog_gcc)dnl
pushdef([ac_cv_prog_cc_works], ac_cv_build_prog_cc_works)dnl
pushdef([ac_cv_prog_cc_cross], ac_cv_build_prog_cc_cross)dnl
pushdef([ac_cv_prog_cc_g], ac_cv_build_prog_cc_g)dnl
pushdef([ac_cv_exeext], ac_cv_build_exeext)dnl
pushdef([ac_cv_objext], ac_cv_build_objext)dnl
pushdef([ac_exeext], ac_build_exeext)dnl
pushdef([ac_objext], ac_build_objext)dnl
pushdef([CC], CC_FOR_BUILD)dnl
pushdef([CPP], CPP_FOR_BUILD)dnl
pushdef([CFLAGS], CFLAGS_FOR_BUILD)dnl
pushdef([CPPFLAGS], CPPFLAGS_FOR_BUILD)dnl
pushdef([host], build)dnl
pushdef([host_alias], build_alias)dnl
pushdef([host_cpu], build_cpu)dnl
pushdef([host_vendor], build_vendor)dnl
pushdef([host_os], build_os)dnl
pushdef([ac_cv_host], ac_cv_build)dnl
pushdef([ac_cv_host_alias], ac_cv_build_alias)dnl
pushdef([ac_cv_host_cpu], ac_cv_build_cpu)dnl
pushdef([ac_cv_host_vendor], ac_cv_build_vendor)dnl
pushdef([ac_cv_host_os], ac_cv_build_os)dnl
pushdef([ac_cpp], ac_build_cpp)dnl
pushdef([ac_compile], ac_build_compile)dnl
pushdef([ac_link], ac_build_link)dnl

dnl Defeat the anti-duplication mechanism
dnl
dnl undefine([AC_PROVIDE_AC_PROG_CPP])dnl
dnl undefine([AC_PROVIDE_AC_PROG_C])dnl
dnl undefine([AC_PROVIDE_AC_EXEEXT])dnl

AC_PROG_CC
AC_PROG_CPP
AC_EXEEXT

dnl Restore the old definitions
dnl
popdef([AC_TRY_COMPILER])dnl
popdef([ac_link])dnl
popdef([ac_compile])dnl
popdef([ac_cpp])dnl
popdef([ac_cv_host_os])dnl
popdef([ac_cv_host_vendor])dnl
popdef([ac_cv_host_cpu])dnl
popdef([ac_cv_host_alias])dnl
popdef([ac_cv_host])dnl
popdef([host_os])dnl
popdef([host_vendor])dnl
popdef([host_cpu])dnl
popdef([host_alias])dnl
popdef([host])dnl
popdef([CPPFLAGS])dnl
popdef([CFLAGS])dnl
popdef([CPP])dnl
popdef([CC])dnl
popdef([ac_objext])dnl
popdef([ac_exeext])dnl
popdef([ac_cv_objext])dnl
popdef([ac_cv_exeext])dnl
popdef([ac_cv_prog_cc_g])dnl
popdef([ac_cv_prog_cc_works])dnl
popdef([ac_cv_prog_cc_cross])dnl
popdef([ac_cv_prog_gcc])dnl
popdef([ac_cv_prog_CPP])dnl
popdef([cross_compiling])dnl

dnl Finally, set Makefile variables
dnl
BUILD_EXEEXT=$ac_build_exeext
BUILD_OBJEXT=$ac_build_objext
AC_SUBST(BUILD_EXEEXT)dnl
AC_SUBST(BUILD_OBJEXT)dnl
AC_SUBST([CFLAGS_FOR_BUILD])dnl
AC_SUBST([CPPFLAGS_FOR_BUILD])dnl
])
