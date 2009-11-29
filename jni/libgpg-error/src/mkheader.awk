# mkheader.awk
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
# of mkheader.awk.  You need not follow the terms of the GNU General
# Public License when using or distributing such scripts, even though
# portions of the text of mkheader.awk appear in them.  The GNU
# General Public License (GPL) does govern all other use of the material
# that constitutes the mkheader.awk program.
#
# Certain portions of the mkheader.awk source text are designed to be
# copied (in certain cases, depending on the input) into the output of
# mkheader.awk.  We call these the "data" portions.  The rest of the
# mkheader.awk source text consists of comments plus executable code
# that decides which of the data portions to output in any given case.
# We call these comments and executable code the "non-data" portions.
# mkheader.h never copies any of the non-data portions into its output.
#
# This special exception to the GPL applies to versions of mkheader.awk
# released by g10 Code GmbH.  When you make and distribute a modified version
# of mkheader.awk, you may extend this special exception to the GPL to
# apply to your modified version as well, *unless* your modified version
# has the potential to copy into its output some of the text that was the
# non-data portion of the version that you started with.  (In other words,
# unless your change moves or copies text from the non-data portions to the
# data portions.)  If your modification has such potential, you must delete
# any notice of this special exception to the GPL from your modified version.

# This script processes gpg-error.h.in in an awful way.
# Its input is, one after another, the content of the err-sources.h.in file,
# the err-codes.h.in file, the errnos.in file, and then gpg-error.h.in.
# There is nothing fancy about this.
#
# An alternative would be to use getline to get the content of the first three files,
# but then we need to pre-process gpg-error.h.in with configure to get
# at the full path of the files in @srcdir@.

BEGIN {
  FS = "[\t]+";
# sources_nr holds the number of error sources.
  sources_nr = 0;
# codes_nr holds the number of error codes.
  codes_nr = 0;
# errnos_nr holds the number of system errors.
  errnos_nr = 0;

# These variables walk us through our input.
  sources_header = 1;
  sources_body = 0;
  between_sources_and_codes = 0;
  codes_body = 0;
  between_codes_and_errnos = 0;
  errnos_body = 0;
  gpg_error_h = 0;

  print "/* Output of mkheader.awk.  DO NOT EDIT.  */";
  print "";

}

sources_header {
  if ($1 ~ /^[0123456789]+$/)
    {
      sources_header = 0;
      sources_body = 1;
    }      
}

sources_body {
  sub (/\#.+/, "");
  sub (/[ 	]+$/, ""); # Strip trailing space and tab characters.

  if (/^$/)
    next;

  if ($1 == "")
    {
      sources_body = 0;
      between_sources_and_codes = 1;
    }
  else
    {
# Remember the error source number and symbol of each error source.
      sources_idx[sources_nr] = $1;
      sources_sym[sources_nr] = $2;
      sources_nr++;
    }
}

between_sources_and_codes {
  if ($1 ~ /^[0123456789]+$/)
    {
      between_sources_and_codes = 0;
      codes_body = 1;
    }      
}

codes_body {
  sub (/\#.+/, "");
  sub (/[ 	]+$/, ""); # Strip trailing space and tab characters.

  if (/^$/)
    next;

  if ($1 == "")
    {
      codes_body = 0;
      between_codes_and_errnos = 1;
    }
  else
    {
# Remember the error code number and symbol of each error source.
      codes_idx[codes_nr] = $1;
      codes_sym[codes_nr] = $2;
      codes_nr++;
    }
}

between_codes_and_errnos {
  if ($1 ~ /^[0-9]/)
    {
      between_codes_and_errnos = 0;
      errnos_body = 1;
    }
}

errnos_body {
  sub (/\#.+/, "");
  sub (/[ 	]+$/, ""); # Strip trailing space and tab characters.

  if (/^$/)
    next;

  if ($1 !~ /^[0-9]/)
    {
# Note that this assumes that gpg-error.h.in doesn't start with a digit.
      errnos_body = 0;
      gpg_error_h = 1;
    }
  else
    {
      errnos_idx[errnos_nr] = "GPG_ERR_SYSTEM_ERROR | " $1;
      errnos_sym[errnos_nr] = "GPG_ERR_" $2;
      errnos_nr++;
    }
}

gpg_error_h {
  if ($0 ~ /^@include err-sources/)
    {
      for (i = 0; i < sources_nr; i++)
	{
	  print "    " sources_sym[i] " = " sources_idx[i] ",";
#	  print "#define " sources_sym[i] " (" sources_idx[i] ")";
	}
    }
  else if ($0 ~ /^@include err-codes/)
    {
      for (i = 0; i < codes_nr; i++)
	{
	  print "    " codes_sym[i] " = " codes_idx[i] ",";
#	  print "#define " codes_sym[i] " (" codes_idx[i] ")";
	}
    }
  else if ($0 ~ /^@include errnos/)
    {
      for (i = 0; i < errnos_nr; i++)
	{
	  print "    " errnos_sym[i] " = " errnos_idx[i] ",";
#	  print "#define " errnos_sym[i] " (" errnos_idx[i] ")";
	}
    }
  else
    print;
}
