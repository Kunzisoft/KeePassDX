/* Output of mkstrtable.awk.  DO NOT EDIT.  */

/* err-sources.h - List of error sources and their description.
   Copyright (C) 2003, 2004 g10 Code GmbH

   This file is part of libgpg-error.

   libgpg-error is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
 
   libgpg-error is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.
 
   You should have received a copy of the GNU Lesser General Public
   License along with libgpg-error; if not, write to the Free
   Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */


/* The purpose of this complex string table is to produce
   optimal code with a minimum of relocations.  */

static const char msgstr[] = 
  gettext_noop ("Unspecified source") "\0"
  gettext_noop ("gcrypt") "\0"
  gettext_noop ("GnuPG") "\0"
  gettext_noop ("GpgSM") "\0"
  gettext_noop ("GPG Agent") "\0"
  gettext_noop ("Pinentry") "\0"
  gettext_noop ("SCD") "\0"
  gettext_noop ("GPGME") "\0"
  gettext_noop ("Keybox") "\0"
  gettext_noop ("KSBA") "\0"
  gettext_noop ("Dirmngr") "\0"
  gettext_noop ("GSTI") "\0"
  gettext_noop ("GPA") "\0"
  gettext_noop ("Kleopatra") "\0"
  gettext_noop ("Any source") "\0"
  gettext_noop ("User defined source 1") "\0"
  gettext_noop ("User defined source 2") "\0"
  gettext_noop ("User defined source 3") "\0"
  gettext_noop ("User defined source 4") "\0"
  gettext_noop ("Unknown source");

static const int msgidx[] =
  {
    0,
    19,
    26,
    32,
    38,
    48,
    57,
    61,
    67,
    74,
    79,
    87,
    92,
    96,
    106,
    117,
    139,
    161,
    183,
    205
  };

#define msgidxof(code) (0 ? -1 \
  : ((code >= 0) && (code <= 13)) ? (code - 0) \
  : ((code >= 31) && (code <= 35)) ? (code - 17) \
  : 36 - 17)
