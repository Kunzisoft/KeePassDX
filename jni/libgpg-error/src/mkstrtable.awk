# mkstrtable.awk
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
# of mkstrtable.awk.  You need not follow the terms of the GNU General
# Public License when using or distributing such scripts, even though
# portions of the text of mkstrtable.awk appear in them.  The GNU
# General Public License (GPL) does govern all other use of the material
# that constitutes the mkstrtable.awk program.
#
# Certain portions of the mkstrtable.awk source text are designed to be
# copied (in certain cases, depending on the input) into the output of
# mkstrtable.awk.  We call these the "data" portions.  The rest of the
# mkstrtable.awk source text consists of comments plus executable code
# that decides which of the data portions to output in any given case.
# We call these comments and executable code the "non-data" portions.
# mkstrtable.h never copies any of the non-data portions into its output.
#
# This special exception to the GPL applies to versions of mkstrtable.awk
# released by g10 Code GmbH.  When you make and distribute a modified version
# of mkstrtable.awk, you may extend this special exception to the GPL to
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
# CODE1	...	MESSAGE1	(code nr, <tab>, something, <tab>, msg)
# CODE2	...	MESSAGE2	(code nr, <tab>, something, <tab>, msg)
# ...
# CODEn	...	MESSAGEn	(code nr, <tab>, something, <tab>, msg)
# 	...	DEFAULT-MESSAGE	(<tab>, something, <tab>, fall-back msg)
#
# Comments (starting with # and ending at the end of the line) are removed,
# as is trailing whitespace.  The last line is optional; if no DEFAULT
# MESSAGE is given, msgidxof will return the number -1 for unknown
# index numbers.
#
# The field to be used is specified with the variable "textidx" on
# the command line.  It defaults to 2.
#
# The variable nogettext can be set to 1 to suppress gettext markers.
#
# The variable prefix can be used to prepend a string to each message.
#
# The variable namespace can be used to prepend a string to each
# variable and macro name.

BEGIN {
  FS = "[\t]+";
# cpos holds the current position in the message string.
  cpos = 0;
# msg holds the number of messages.
  msg = 0;
  print "/* Output of mkstrtable.awk.  DO NOT EDIT.  */";
  print "";
  header = 1;
  if (textidx == 0)
    textidx = 2;
# nogettext can be set to 1 to suppress gettext noop markers.
}

/^#/ { next; }

header {
  if ($1 ~ /^[0123456789]+$/)
    {
      print "/* The purpose of this complex string table is to produce";
      print "   optimal code with a minimum of relocations.  */";
      print "";
      print "static const char " namespace "msgstr[] = ";
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
  if (last_msgstr)
    {
      if (nogettext)
	print "  \"" last_msgstr "\" \"\\0\"";
      else
	print "  gettext_noop (\"" last_msgstr "\") \"\\0\"";
    }
  last_msgstr = prefix $textidx;

# Remember the error code and msgidx of each error message.
  code[msg] = $1;
  pos[msg] = cpos;
  cpos += length (last_msgstr) + 1;
  msg++;

  if ($1 == "")
    {
      has_default = 1;
      exit;
    }
}
END {
  if (has_default)
    coded_msgs = msg - 1;
  else
    coded_msgs = msg;

  if (nogettext)
    print "  \"" prefix last_msgstr "\";";
  else
    print "  gettext_noop (\"" prefix last_msgstr "\");";
  print "";
  print "static const int " namespace "msgidx[] =";
  print "  {";
  for (i = 0; i < coded_msgs; i++)
    print "    " pos[i] ",";
  print "    " pos[coded_msgs];
  print "  };";
  print "";
  print "#define " namespace "msgidxof(code) (0 ? -1 \\";

# Gather the ranges.
  skip = code[0];
  start = code[0];
  stop = code[0];
  for (i = 1; i < coded_msgs; i++)
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
  if (has_default)
    print "  : " stop + 1 " - " skip ")";
  else
    print "  : -1)";
    
 }
