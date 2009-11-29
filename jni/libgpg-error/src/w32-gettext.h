/* w32-gettext.h - A simple gettext implementation for Windows targets.
   Copyright (C) 2005 g10 Code GmbH

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

#if ENABLE_NLS

#include <locale.h>
#if !defined LC_MESSAGES && !(defined __LOCALE_H || (defined _LOCALE_H && defined __sun))
# define LC_MESSAGES 1729
#endif

/* If we build on w32, we will use our own simple gettext
   implementation.  For now, this is not a drop in replacement, so we
   must cheat a bit and redirect all calls to the external gettext to
   an internal implementation.  We try to be as little invasive as
   possible, so that the refactorization of the code occurs at logical
   interfaces.

   Note that this function intimately knows the various definitions in
   the target libintl.h and the local gettext.h file, from which it is
   included.  */

#define bindtextdomain		_gpg_err_bindtextdomain
#define textdomain		_gpg_err_textdomain
#define dgettext		_gpg_err_dgettext
#define gettext			_gpg_err_gettext

/* Specify that the DOMAINNAME message catalog will be found
   in DIRNAME rather than in the system locale data base.  */
char *bindtextdomain (const char *domainname, const char *dirname);

const char *gettext (const char *msgid);

char *textdomain (const char *domainname);

char *dgettext (const char *domainname, const char *msgid);

#endif	/* ENABLE_NLS */
