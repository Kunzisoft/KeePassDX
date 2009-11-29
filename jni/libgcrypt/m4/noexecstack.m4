# noexecstack.m4
dnl Copyright (C) 1995-2006 Free Software Foundation, Inc.
dnl
dnl This library is free software; you can redistribute it and/or
dnl modify it under the terms of the GNU Lesser General Public
dnl License as published by the Free Software Foundation; either
dnl version 2.1 of the License, or (at your option) any later version.
dnl
dnl This library is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
dnl Lesser General Public License for more details.
dnl
dnl You should have received a copy of the GNU Lesser General Public
dnl License along with this library; if not, write to the Free Software
dnl Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA

dnl Checks whether the stack can be marked nonexecutable by passing an
dnl option to the C-compiler when acting on .s files.  Returns that
dnl option in NOEXECSTACK_FLAGS.
dnl This macro is adapted from one found in GLIBC-2.3.5.
AC_DEFUN([CL_AS_NOEXECSTACK],[
AC_REQUIRE([AC_PROG_CC])
AC_REQUIRE([AM_PROG_AS])

AC_MSG_CHECKING([whether non excutable stack support is requested])
AC_ARG_ENABLE(noexecstack,
              AC_HELP_STRING([--disable-noexecstack],
                             [disable non executable stack support]),
              noexecstack_support=$enableval, noexecstack_support=yes)
AC_MSG_RESULT($noexecstack_support)

AC_CACHE_CHECK([whether assembler supports --noexecstack option],
cl_cv_as_noexecstack, [dnl
  cat > conftest.c <<EOF
void foo() {}
EOF
  if AC_TRY_COMMAND([${CC} $CFLAGS $CPPFLAGS
                     -S -o conftest.s conftest.c >/dev/null]) \
     && grep .note.GNU-stack conftest.s >/dev/null \
     && AC_TRY_COMMAND([${CCAS} $CCASFLAGS $CPPFLAGS -Wa,--noexecstack
                       -c -o conftest.o conftest.s >/dev/null])
  then
    cl_cv_as_noexecstack=yes
  else
    cl_cv_as_noexecstack=no
  fi
  rm -f conftest*])
  if test "$noexecstack_support" = yes -a "$cl_cv_as_noexecstack" = yes; then
	NOEXECSTACK_FLAGS="-Wa,--noexecstack"
  else
        NOEXECSTACK_FLAGS=
  fi
  AC_SUBST(NOEXECSTACK_FLAGS)
])
