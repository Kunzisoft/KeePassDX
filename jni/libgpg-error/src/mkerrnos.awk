# mkerrnos.awk
# Copyright (C) 2003, 2004 g10 Code GmbH
# 
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of
# the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
#
# As a special exception, g10 Code GmbH gives unlimited permission to
# copy, distribute and modify the C source files that are the output
# of mkerrnos.awk.  You need not follow the terms of the GNU General
# Public License when using or distributing such scripts, even though
# portions of the text of mkerrnos.awk appear in them.  The GNU
# General Public License (GPL) does govern all other use of the material
# that constitutes the mkerrnos.awk program.
#
# Certain portions of the mkerrnos.awk source text are designed to be
# copied (in certain cases, depending on the input) into the output of
# mkerrnos.awk.  We call these the "data" portions.  The rest of the
# mkerrnos.awk source text consists of comments plus executable code
# that decides which of the data portions to output in any given case.
# We call these comments and executable code the "non-data" portions.
# mkerrnos.h never copies any of the non-data portions into its output.
#
# This special exception to the GPL applies to versions of mkerrnos.awk
# released by g10 Code GmbH.  When you make and distribute a modified version
# of mkerrnos.awk, you may extend this special exception to the GPL to
# apply to your modified version as well, *unless* your modified version
# has the potential to copy into its output some of the text that was the
# non-data portion of the version that you started with.  (In other words,
# unless your change moves or copies text from the non-data portions to the
# data portions.)  If your modification has such potential, you must delete
# any notice of this special exception to the GPL from your modified version.

# This script outputs a source file that does define the following
# symbols:
#
# static const int err_code_to_errno[];
# A mapping of gpg_err_code_t numbers to system errno.  The index of an
# error code in the table can be obtained after removing the system error
# code indication bit.
#
# The input file is a list of possible system errors in the column errnoidx
# (defaults to 2).
#
# Comments (starting with # and ending at the end of the line) are removed,
# as is trailing whitespace.

BEGIN {
  FS="[\t]+";
  header = 1;
  if (errnoidx == 0)
    errnoidx = 2;

  print "/* Output of mkerrnos.awk.  DO NOT EDIT.  */";
  print "";
}

/^#/ { next; }

header {
  if ($1 ~ /^[0-9]/)
    {
      print "#include <errno.h>";
      print "";
      print "static const int err_code_to_errno [] = {";
      header = 0;
    }
  else
    print;
}

!header {
  sub (/\#.+/, "");
  sub (/[ 	]+$/, ""); # Strip trailing space and tab characters.

  if (/^$/)
    next;

    print "#ifdef " $errnoidx;
    print "  " $errnoidx ",";
    print "#else";
    print "  0,";
    print "#endif";
}
END {
  print "};";
}
