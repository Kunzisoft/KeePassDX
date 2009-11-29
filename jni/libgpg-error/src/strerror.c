/* strerror.c - Describing an error code.
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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <gpg-error.h>

#include "gettext.h"
#include "err-codes.h"

/* Return a pointer to a string containing a description of the error
   code in the error value ERR.  This function is not thread-safe.  */
const char *
gpg_strerror (gpg_error_t err)
{
  gpg_err_code_t code = gpg_err_code (err);

  if (code & GPG_ERR_SYSTEM_ERROR)
    {
      int no = gpg_err_code_to_errno (code);
      if (no)
	return strerror (no);
      else
	code = GPG_ERR_UNKNOWN_ERRNO;
    }
  return dgettext (PACKAGE, msgstr + msgidx[msgidxof (code)]);
}


#ifdef HAVE_STRERROR_R
#ifdef STRERROR_R_CHAR_P
/* The GNU C library and probably some other systems have this weird
   variant of strerror_r.  */

/* Return a dynamically allocated string in *STR describing the system
   error NO.  If this call succeeds, return 1.  If this call fails due
   to a resource shortage, set *STR to NULL and return 1.  If this
   call fails because the error number is not valid, don't set *STR
   and return 0.  */
static int
system_strerror_r (int no, char *buf, size_t buflen)
{
  char *errstr;

  errstr = strerror_r (no, buf, buflen);
  if (errstr != buf)
    {
      size_t errstr_len = strlen (errstr) + 1;
      size_t cpy_len = errstr_len < buflen ? errstr_len : buflen;
      memcpy (buf, errstr, cpy_len);

      return cpy_len == errstr_len ? 0 : ERANGE;
    }
  else
    {
      /* We can not tell if the buffer was large enough, but we can
	 try to make a guess.  */
      if (strlen (buf) + 1 >= buflen)
	return ERANGE;

      return 0;
    }
}

#else	/* STRERROR_R_CHAR_P */
/* Now the POSIX version.  */

static int
system_strerror_r (int no, char *buf, size_t buflen)
{
  return strerror_r (no, buf, buflen);
}

#endif	/* STRERROR_R_CHAR_P */

#else	/* HAVE_STRERROR_H */
/* Without strerror_r(), we can still provide a non-thread-safe
   version.  Maybe we are even lucky and the system's strerror() is
   already thread-safe.  */

static int
system_strerror_r (int no, char *buf, size_t buflen)
{
  char *errstr = strerror (no);

  if (!errstr)
    {
      int saved_errno = errno;

      if (saved_errno != EINVAL)
	snprintf (buf, buflen, "strerror failed: %i\n", errno);
      return saved_errno;
    }
  else
    {
      size_t errstr_len = strlen (errstr) + 1;
      size_t cpy_len = errstr_len < buflen ? errstr_len : buflen;
      memcpy (buf, errstr, cpy_len);
      return cpy_len == errstr_len ? 0 : ERANGE;
    }
}
#endif


/* Return the error string for ERR in the user-supplied buffer BUF of
   size BUFLEN.  This function is, in contrast to gpg_strerror,
   thread-safe if a thread-safe strerror_r() function is provided by
   the system.  If the function succeeds, 0 is returned and BUF
   contains the string describing the error.  If the buffer was not
   large enough, ERANGE is returned and BUF contains as much of the
   beginning of the error string as fits into the buffer.  */
int
gpg_strerror_r (gpg_error_t err, char *buf, size_t buflen)
{
  gpg_err_code_t code = gpg_err_code (err);
  const char *errstr;
  size_t errstr_len;
  size_t cpy_len;

  if (code & GPG_ERR_SYSTEM_ERROR)
    {
      int no = gpg_err_code_to_errno (code);
      if (no)
	{
	  int system_err = system_strerror_r (no, buf, buflen);

	  if (system_err != EINVAL)
	    {
	      if (buflen)
		buf[buflen - 1] = '\0';
	      return system_err;
	    }
	}
      code = GPG_ERR_UNKNOWN_ERRNO;
    }

  errstr = dgettext (PACKAGE, msgstr + msgidx[msgidxof (code)]);
  errstr_len = strlen (errstr) + 1;
  cpy_len = errstr_len < buflen ? errstr_len : buflen;
  memcpy (buf, errstr, cpy_len);
  if (buflen)
    buf[buflen - 1] = '\0';

  return cpy_len == errstr_len ? 0 : ERANGE;
}
