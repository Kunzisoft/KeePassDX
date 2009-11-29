# mkstrtable.awk
# Copyright (C) 2003 g10 Code GmbH
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
# of mkerrcodes2.awk.  You need not follow the terms of the GNU General
# Public License when using or distributing such scripts, even though
# portions of the text of mkerrcodes2.awk appear in them.  The GNU
# General Public License (GPL) does govern all other use of the material
# that constitutes the mkerrcodes2.awk program.
#
# Certain portions of the mkerrcodes2.awk source text are designed to be
# copied (in certain cases, depending on the input) into the output of
# mkerrcodes2.awk.  We call these the "data" portions.  The rest of the
# mkerrcodes2.awk source text consists of comments plus executable code
# that decides which of the data portions to output in any given case.
# We call these comments and executable code the "non-data" portions.
# mkstrtable.h never copies any of the non-data portions into its output.
#
# This special exception to the GPL applies to versions of mkerrcodes2.awk
# released by g10 Code GmbH.  When you make and distribute a modified version
# of mkerrcodes2.awk, you may extend this special exception to the GPL to
# apply to your modified version as well, *unless* your modified version
# has the potential to copy into its output some of the text that was the
# non-data portion of the version that you started with.  (In other words,
# unless your change moves or copies text from the non-data portions to the
# data portions.)  If your modification has such potential, you must delete
# any notice of this special exception to the GPL from your modified version.

# This script outputs a source file that does define the following
# symbols:
#
# static const char msgstr[];
# A string containing all messages in the list.
#
# static const int msgidx[];
# A list of index numbers, one for each message, that points to the
# beginning of the string in msgstr.
#
# msgidxof (code);
# A macro that maps code numbers to idx numbers.  If a DEFAULT MESSAGE
# is provided (see below), its index will be returned for unknown codes.
# Otherwise -1 is returned for codes that do not appear in the list.
# You can lookup the message with code CODE with:
# msgstr + msgidx[msgidxof (code)].
#
# The input file has the following format:
# CODE1	MESSAGE1		(Code number, <tab>, message string)
# CODE2	MESSAGE2		(Code number, <tab>, message string)
# ...
# CODEn	MESSAGEn		(Code number, <tab>, message string)
# 	DEFAULT MESSAGE		(<tab>, fall-back message string)
#
# Comments (starting with # and ending at the end of the line) are removed,
# as is trailing whitespace.  The last line is optional; if no DEFAULT
# MESSAGE is given, msgidxof will return the number -1 for unknown
# index numbers.

BEGIN {
# msg holds the number of messages.
  msg = 0;
  print "/* Output of mkerrcodes2.awk.  DO NOT EDIT.  */";
  print "";
  header = 1;
}

/^#/ { next; }

header {
  if ($1 ~ /^[0123456789]+$/)
    {
      print "static const int err_code_from_index[] = {";
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

# Print the string msgstr line by line.  We delay output by one line to be able
# to treat the last line differently (see END).
  print "  " $2 ",";

# Remember the error value and index of each error code.
  code[msg] = $1;
  pos[msg] = $2;
  msg++;
}
END {
  print "};";
  print "";
  print "#define errno_to_idx(code) (0 ? -1 \\";

# Gather the ranges.
  skip = code[0];
  start = code[0];
  stop = code[0];
  for (i = 1; i < msg; i++)
    {
      if (code[i] == stop + 1)
	stop++;
      else
	{
	  print "  : ((code >= " start ") && (code <= " stop ")) ? (code - " \
            skip ") \\";
	  skip += code[i] - stop - 1;
	  start = code[i];
	  stop = code[i];
	}
    }
  print "  : ((code >= " start ") && (code <= " stop ")) ? (code - " \
    skip ") \\";
  print "  : -1)";
}
