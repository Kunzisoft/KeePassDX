#! /bin/sh
# Run this to generate all the initial makefiles, etc. 
#
# Copyright (C) 2003 g10 Code GmbH
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

configure_ac="configure.ac"

cvtver () {
  awk 'NR==1 {split($NF,A,".");X=1000000*A[1]+1000*A[2]+A[3];print X;exit 0}'
}

check_version () {
    if [ `("$1" --version || echo "0") | cvtver` -ge "$2" ]; then
       return 0
    fi
    echo "**Error**: "\`$1\'" not installed or too old." >&2
    echo '           Version '$3' or newer is required.' >&2
    [ -n "$4" ] && echo '           Note that this is part of '\`$4\''.' >&2
    DIE="yes"
    return 1
}


DIE=no
FORCE=
if test x"$1" = x"--force"; then
  FORCE=" --force"
  shift
fi

# ***** W32 build script *******
# Used to cross-compile for Windows.
if test "$1" = "--build-w32"; then
    tmp=`dirname $0`
    tsdir=`cd "$tmp"; pwd`
    shift
    if [ ! -f $tsdir/config.guess ]; then
        echo "$tsdir/config.guess not found" >&2
        exit 1
    fi
    build=`$tsdir/config.guess`

    [ -z "$w32root" ] && w32root="$HOME/w32root"
    echo "Using $w32root as standard install directory" >&2
    
    # Locate the cross compiler
    crossbindir=
    for host in i586-mingw32msvc i386-mingw32msvc; do
        if ${host}-gcc --version >/dev/null 2>&1 ; then
            crossbindir=/usr/${host}/bin
            conf_CC="CC=${host}-gcc"
            break;
        fi
    done
    if [ -z "$crossbindir" ]; then
        echo "Cross compiler kit not installed" >&2
        echo "Under Debian GNU/Linux, you may install it using" >&2
        echo "  apt-get install mingw32 mingw32-runtime mingw32-binutils" >&2 
        echo "Stop." >&2
        exit 1
    fi
   
    if [ -f "$tsdir/config.log" ]; then
        if ! head $tsdir/config.log | grep "$host" >/dev/null; then
            echo "Pease run a 'make distclean' first" >&2
            exit 1
        fi
    fi

    ./configure --enable-maintainer-mode  --prefix=${w32root}  \
            --host=${host} --build=${build} \
            --with-gpg-error-prefix=${w32root}
    exit $?
fi
# ***** end W32 build script *******


# ***** AMD64 cross build script *******
# Used to cross-compile for AMD64 (for testing)
if test "$1" = "--build-amd64"; then
    tmp=`dirname $0`
    tsdir=`cd "$tmp"; pwd`
    shift
    if [ ! -f $tsdir/config.guess ]; then
        echo "$tsdir/config.guess not found" >&2
        exit 1
    fi
    build=`$tsdir/config.guess`

    [ -z "$amd64root" ] && amd64root="$HOME/amd64root"
    echo "Using $amd64root as standard install directory" >&2
    
    # Locate the cross compiler
    crossbindir=
    for host in x86_64-linux-gnu amd64-linux-gnu; do
        if ${host}-gcc --version >/dev/null 2>&1 ; then
            crossbindir=/usr/${host}/bin
            conf_CC="CC=${host}-gcc"
            break;
        fi
    done
    if [ -z "$crossbindir" ]; then
        echo "Cross compiler kit not installed" >&2
        echo "Stop." >&2
        exit 1
    fi
   
    if [ -f "$tsdir/config.log" ]; then
        if ! head $tsdir/config.log | grep "$host" >/dev/null; then
            echo "Please run a 'make distclean' first" >&2
            exit 1
        fi
    fi

    $tsdir/configure --enable-maintainer-mode --prefix=${amd64root}  \
             --host=${host} --build=${build} \
             --with-gpg-error-prefix=${amd64root} 
 
    rc=$?
    exit $rc
fi
# ***** end AMD64 cross build script *******

# Grep the required versions from configure.ac
autoconf_vers=`sed -n '/^AC_PREREQ(/ { 
s/^.*(\(.*\))/\1/p
q
}' ${configure_ac}`
autoconf_vers_num=`echo "$autoconf_vers" | cvtver`

automake_vers=`sed -n '/^min_automake_version=/ { 
s/^.*="\(.*\)"/\1/p
q
}' ${configure_ac}`
automake_vers_num=`echo "$automake_vers" | cvtver`

#gettext_vers=`sed -n '/^AM_GNU_GETTEXT_VERSION(/ { 
#s/^.*(\(.*\))/\1/p
#q
#}' ${configure_ac}`
#gettext_vers_num=`echo "$gettext_vers" | cvtver`


if [ -z "$autoconf_vers" -o -z "$automake_vers" ]
then
  echo "**Error**: version information not found in "\`${configure_ac}\'"." >&2
  exit 1
fi

# Allow to override the default tool names
AUTOCONF=${AUTOCONF_PREFIX}${AUTOCONF:-autoconf}${AUTOCONF_SUFFIX}
AUTOHEADER=${AUTOCONF_PREFIX}${AUTOHEADER:-autoheader}${AUTOCONF_SUFFIX}

AUTOMAKE=${AUTOMAKE_PREFIX}${AUTOMAKE:-automake}${AUTOMAKE_SUFFIX}
ACLOCAL=${AUTOMAKE_PREFIX}${ACLOCAL:-aclocal}${AUTOMAKE_SUFFIX}

#GETTEXT=${GETTEXT_PREFIX}${GETTEXT:-gettext}${GETTEXT_SUFFIX}
#MSGMERGE=${GETTEXT_PREFIX}${MSGMERGE:-msgmerge}${GETTEXT_SUFFIX}


if check_version $AUTOCONF $autoconf_vers_num $autoconf_vers ; then
    check_version $AUTOHEADER $autoconf_vers_num $autoconf_vers autoconf
fi
if check_version $AUTOMAKE $automake_vers_num $automake_vers; then
  check_version $ACLOCAL $automake_vers_num $autoconf_vers automake
fi
#if check_version $GETTEXT $gettext_vers_num $gettext_vers; then
#  check_version $MSGMERGE $gettext_vers_num $gettext_vers gettext
#fi

if test "$DIE" = "yes"; then
    cat <<EOF

Note that you may use alternative versions of the tools by setting 
the corresponding environment variables; see README.SVN for details.
                   
EOF
    exit 1
fi

echo "Running aclocal -I m4 ${ACLOCAL_FLAGS:+$ACLOCAL_FLAGS }..."
$ACLOCAL -I m4 $ACLOCAL_FLAGS
echo "Running autoheader..."
$AUTOHEADER
echo "Running automake --gnu ..."
$AUTOMAKE --gnu;
echo "Running autoconf${FORCE} ..."
$AUTOCONF${FORCE}

echo "You may now run: 
  ./configure --enable-maintainer-mode && make
"
