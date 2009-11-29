/* code-from-errno.c - Mapping errnos to error codes.
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

#include <errno.h> 
#ifdef HAVE_W32_SYSTEM
#include <winsock2.h>
#endif

#include <gpg-error.h>

#include "code-from-errno.h"

#ifdef HAVE_W32_SYSTEM
/* Under Windows socket related error codes are defined in a different
   file and prefixed with "WSA".  As their ranges don't overlap, we map
   some of them to our usual error codes. */
gpg_err_code_t
w32_special_errnos (int err)
{
  switch (err)
    {
    case WSAEADDRINUSE:    return GPG_ERR_EADDRINUSE;
    case WSAEADDRNOTAVAIL: return GPG_ERR_EADDRNOTAVAIL;  
    case WSAECONNABORTED:  return GPG_ERR_ECONNABORTED;
    case WSAECONNREFUSED:  return GPG_ERR_ECONNREFUSED;
    case WSAECONNRESET:    return GPG_ERR_ECONNRESET;
    case WSAENAMETOOLONG:  return GPG_ERR_ENAMETOOLONG;
    case WSAEHOSTDOWN:     return GPG_ERR_EHOSTDOWN;
    case WSAEHOSTUNREACH:  return GPG_ERR_EHOSTUNREACH;
    default: 
      return GPG_ERR_UNKNOWN_ERRNO;
    }
}
#endif /*HAVE_W32_SYSTEM*/

/* Retrieve the error code for the system error ERR.  This returns
   GPG_ERR_UNKNOWN_ERRNO if the system error is not mapped (report
   this).  */
gpg_err_code_t
gpg_err_code_from_errno (int err)
{
  int idx;

  if (!err)
    return GPG_ERR_NO_ERROR;

  idx = errno_to_idx (err);

  if (idx < 0)
    {
#ifdef HAVE_W32_SYSTEM
      return w32_special_errnos (err);
#else
      return GPG_ERR_UNKNOWN_ERRNO;
#endif
    }

  return GPG_ERR_SYSTEM_ERROR | err_code_from_index[idx];
}


/* Retrieve the error code directly from the ERRNO variable.  This
   returns GPG_ERR_UNKNOWN_ERRNO if the system error is not mapped
   (report this) and GPG_ERR_MISSING_ERRNO if ERRNO has the value 0. */
gpg_err_code_t
gpg_err_code_from_syserror (void)
{
  int err = errno;
  int idx;

  if (!err)
    return GPG_ERR_MISSING_ERRNO;

  idx = errno_to_idx (err);

  if (idx < 0)
    {
#ifdef HAVE_W32_SYSTEM
      return w32_special_errnos (err);
#else
      return GPG_ERR_UNKNOWN_ERRNO;
#endif
    }

  return GPG_ERR_SYSTEM_ERROR | err_code_from_index[idx];
}
