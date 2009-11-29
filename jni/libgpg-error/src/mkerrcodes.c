/* mkerrcodes.c - Generate list of system error values.
   Copyright (C) 2004 g10 Code GmbH

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

/* This file must not include config.h, as that is for the host
   system, while this file will be run on the build system.  */

#include <stdio.h>

#include "mkerrcodes.h"

static const char header[] =
"/* errnos.h - List of system error values.\n"
"   Copyright (C) 2004 g10 Code GmbH\n"
"   This file is part of libgpg-error.\n"
"\n"
"   libgpg-error is free software; you can redistribute it and/or\n"
"   modify it under the terms of the GNU Lesser General Public License\n"
"   as published by the Free Software Foundation; either version 2.1 of\n"
"   the License, or (at your option) any later version.\n"
"\n"
"   libgpg-error is distributed in the hope that it will be useful, but\n"
"   WITHOUT ANY WARRANTY; without even the implied warranty of\n"
"   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU\n"
"   Lesser General Public License for more details.\n"
"\n"
"   You should have received a copy of the GNU Lesser General Public\n"
"   License along with libgpg-error; if not, write to the Free\n"
"   Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA\n"
"   02111-1307, USA.  */\n"
"\n";

int
main (int argc, char **argv)
{
  int sorted;
  int i;

  printf ("%s", header);
  do
    {
      sorted = 1;
      for (i = 0; i < sizeof (err_table) / sizeof (err_table[0]) - 1; i++)
	if (err_table[i].err > err_table[i + 1].err)
	  {
	    int err = err_table[i].err;
	    const char *err_sym = err_table[i].err_sym;

	    err_table[i].err = err_table[i + 1].err;
	    err_table[i].err_sym = err_table[i + 1].err_sym;
	    err_table[i + 1].err = err;
	    err_table[i + 1].err_sym = err_sym;
	    sorted = 0;
	  }
    }
  while (!sorted);
      
  for (i = 0; i < sizeof (err_table) / sizeof (err_table[0]); i++)
    printf ("%i\t%s\n", err_table[i].err, err_table[i].err_sym);

  return 0;
}
