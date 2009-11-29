/* t-syserror.c - System error specific regression test.
   Copyright (C) 2006 g10 Code GmbH

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
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
   02110-1301, USA.  */


#if HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#if HAVE_STDLIB_H
#include <stdlib.h>
#endif
#include <errno.h>

#include <gpg-error.h>

int
main (int argc, char *argv[])
{
  FILE *fp;
  int save_errno;
  gpg_err_code_t ec;

  fp = fopen ("/does-not-exist/110761/nowhere.foo", "r");
  if (fp)
    {
      fclose (fp);
      fp = fopen ("  no this file does not exists foo 4711", "r");
    }
  if (fp)
    {
      fprintf (stderr, "unable to run test\n");
      return 1;
    }
  save_errno = errno;

  ec = gpg_err_code_from_syserror ();
  if (ec != GPG_ERR_ENOENT)
    {
      fprintf (stderr, "fopen failed with bad code: %d\n", save_errno);
      return 1;
    }

  if (ec != gpg_err_code_from_errno (save_errno))
    {
      fprintf (stderr, "oops at %d\n",__LINE__);
      return 1;
    }

  errno = 0;

  ec = gpg_err_code_from_syserror ();
  if (ec != GPG_ERR_MISSING_ERRNO)
    {
      fprintf (stderr, "oops at %d\n",__LINE__);
      return 1;
    }

  if ( gpg_err_code_from_errno (0) )
    {
      fprintf (stderr, "oops at %d\n",__LINE__);
      return 1;
    }


  return 0;
}
