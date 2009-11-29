/* t-strerror.c - Regression test.
   Copyright (C) 2003 g10 Code GmbH

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
   License along with libgpgme-error; if not, write to the Free
   Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */

#if HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#if HAVE_STDLIB_H
#include <stdlib.h>
#endif

#include <gpg-error.h>

int
main (int argc, char *argv[])
{
  if (argc > 1)
    {
      int i = 1;
      while (i + 1 < argc)
	{
	  gpg_error_t err = gpg_err_make (atoi (argv[i]), atoi (argv[i + 1]));
	  printf ("%s: %s\n", gpg_strsource (err), gpg_strerror (err));
	  i += 2;
	}
    }
  else
    {
      struct
      {
	gpg_err_source_t src;
	gpg_err_code_t code;
      } list[] = { { 0, 0 }, { 1, 201 }, { 2, 2 }, { 3, 102 },
		   { 4, 100 }, { 5, 99 }, { 6, 110 }, { 7, 7 }, { 8, 888 } };
      int i = 0;

      while (i < sizeof (list) / sizeof (list[0]))
	{
	  gpg_error_t err = gpg_err_make (list[i].src, list[i].code);
	  printf ("%s: %s\n", gpg_strsource (err), gpg_strerror (err));
	  i++;
	}
    }
  return 0;
}
