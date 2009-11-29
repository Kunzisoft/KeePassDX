# mkerrcodes.awk
# Copyright (C) 2004, 2005, 2006 g10 Code GmbH
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
# copy, distribute and modify the lisp source files that are the output
# of mkerrcodes.awk.  You need not follow the terms of the GNU General
# Public License when using or distributing such scripts, even though
# portions of the text of mkerrcodes.awk appear in them.  The GNU
# General Public License (GPL) does govern all other use of the material
# that constitutes the mkerrcodes.awk program.
#
# Certain portions of the mkerrcodes.awk source text are designed to be
# copied (in certain cases, depending on the input) into the output of
# mkerrcodes.awk.  We call these the "data" portions.  The rest of the
# mkerrcodes.awk source text consists of comments plus executable code
# that decides which of the data portions to output in any given case.
# We call these comments and executable code the "non-data" portions.
# mkerrcodes.awk never copies any of the non-data portions into its output.
#
# This special exception to the GPL applies to versions of mkerrcodes.awk
# released by g10 Code GmbH.  When you make and distribute a modified version
# of mkerrcodes.awk, you may extend this special exception to the GPL to
# apply to your modified version as well, *unless* your modified version
# has the potential to copy into its output some of the text that was the
# non-data portion of the version that you started with.  (In other words,
# unless your change moves or copies text from the non-data portions to the
# data portions.)  If your modification has such potential, you must delete
# any notice of this special exception to the GPL from your modified version.

# The input file is in the following format:
# [CODE SYMBOL...]
# @errnos@
# [CODE SYMBOL...]
#
# The difference between the sections is how symbol is transformed.
# The second section gets GPG_ERR_ prepended before processing.
#
# Comments (starting with # and ending at the end of the line) are removed,
# as is trailing whitespace.

BEGIN {
  FS="[ \t]+";
  print ";;;; Output of mkerrcodes.awk.  DO NOT EDIT.";
  print "";
  print ";;; Copyright (C) 2006 g10 Code GmbH";
  print ";;;";
  print ";;; This file is part of libgpg-error.";
  print ";;;";
  print ";;; libgpg-error is free software; you can redistribute it and/or";
  print ";;; modify it under the terms of the GNU Lesser General Public License";
  print ";;; as published by the Free Software Foundation; either version 2.1 of";
  print ";;; the License, or (at your option) any later version.";
  print ";;;";
  print ";;; libgpg-error is distributed in the hope that it will be useful, but";
  print ";;; WITHOUT ANY WARRANTY; without even the implied warranty of";
  print ";;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU";
  print ";;; Lesser General Public License for more details.";
  print ";;;";
  print ";;; You should have received a copy of the GNU Lesser General Public";
  print ";;; License along with libgpg-error; if not, write to the Free";
  print ";;; Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA";
  print ";;; 02111-1307, USA.";
  print "";

  header = 1;
  errnos = 0;
}

/^#/ { next; }

header {
  if (errnos)
    {
      if ($1 ~ /^[0123456789]+$/)
	{
	  header = 0;

	  print "";
	  print "  ;; The following error codes map system errors.";
	}
    }
  else
    {
      if ($1 ~ /^[0123456789]+$/)
	{
	  header = 0;
	  
	  print "(in-package :gpg-error)";
	  print "";
	  print ";;; The error code type gpg-err-code-t.";
	  print "";
	  print ";;; This is used for system error codes.";
	  print "(defconstant +gpg-err-system-error+ (ash 1 15))";
	  print "";
	  print ";;; This is one more than the largest allowed entry.";
	  print "(defconstant +gpg-err-code-dim+ 65536)";
	  print "";
	  print ";;; A helper macro to have the keyword values evaluated.";
	  print "(defmacro defcenum-eval (type doc &rest vals)";
	  print "  `(defcenum ,type ,doc";
	  print "    ,@(loop for v in vals";
	  print "            collect `(,(first v) ,(eval (second v))))))";
	  print "";
	  print "(defcenum-eval gpg-err-code-t";
	  print "    \"The GPG error code type.\"";
	}
    }
}

!header {
  sub (/\#.+/, "");
  sub (/[ 	]+$/, ""); # Strip trailing space and tab characters.

  if (/^$/)
    next;

  # The following can happen for GPG_ERR_CODE_DIM.
  if ($1 == "")
    next;

  if (/^@errnos@$/)
    {
      header = 1;
      errnos = 1;
      next;
    }

  $2 = tolower($2);
  gsub ("_", "-", $2);

  if (errnos)
    print "  (:gpg-err-" $2 " (logior +gpg-err-system-error+ " $1 "))";
  else
    print "  (:" $2 " " $1 ")";
}

END {
  # I am very sorry to break lisp coding style here.
  print ")";
}
