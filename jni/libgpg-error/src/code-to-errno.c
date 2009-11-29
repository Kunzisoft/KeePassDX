/* code-to-errno.c - Mapping error codes to errnos.
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
   License along with libgpg-error; if not, write to the Free
   Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */

#if HAVE_CONFIG_H
#include <config.h>
#endif

#include <gpg-error.h>

#include "code-to-errno.h"

/* Retrieve the system error for the error code CODE.  This returns 0
   if CODE is not a system error code.  */
int
gpg_err_code_to_errno (gpg_err_code_t code)
{
  if (!(code & GPG_ERR_SYSTEM_ERROR))
    return 0;
  code &= ~GPG_ERR_SYSTEM_ERROR;

  if (code < sizeof (err_code_to_errno) / sizeof (err_code_to_errno[0]))
    return err_code_to_errno[code];
  else
    return 0;
}
