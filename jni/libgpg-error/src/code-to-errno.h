/* Output of mkerrnos.awk.  DO NOT EDIT.  */

/* errnos.h - List of system error values.
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



#include <errno.h>

static const int err_code_to_errno [] = {
#ifdef E2BIG
  E2BIG,
#else
  0,
#endif
#ifdef EACCES
  EACCES,
#else
  0,
#endif
#ifdef EADDRINUSE
  EADDRINUSE,
#else
  0,
#endif
#ifdef EADDRNOTAVAIL
  EADDRNOTAVAIL,
#else
  0,
#endif
#ifdef EADV
  EADV,
#else
  0,
#endif
#ifdef EAFNOSUPPORT
  EAFNOSUPPORT,
#else
  0,
#endif
#ifdef EAGAIN
  EAGAIN,
#else
  0,
#endif
#ifdef EALREADY
  EALREADY,
#else
  0,
#endif
#ifdef EAUTH
  EAUTH,
#else
  0,
#endif
#ifdef EBACKGROUND
  EBACKGROUND,
#else
  0,
#endif
#ifdef EBADE
  EBADE,
#else
  0,
#endif
#ifdef EBADF
  EBADF,
#else
  0,
#endif
#ifdef EBADFD
  EBADFD,
#else
  0,
#endif
#ifdef EBADMSG
  EBADMSG,
#else
  0,
#endif
#ifdef EBADR
  EBADR,
#else
  0,
#endif
#ifdef EBADRPC
  EBADRPC,
#else
  0,
#endif
#ifdef EBADRQC
  EBADRQC,
#else
  0,
#endif
#ifdef EBADSLT
  EBADSLT,
#else
  0,
#endif
#ifdef EBFONT
  EBFONT,
#else
  0,
#endif
#ifdef EBUSY
  EBUSY,
#else
  0,
#endif
#ifdef ECANCELED
  ECANCELED,
#else
  0,
#endif
#ifdef ECHILD
  ECHILD,
#else
  0,
#endif
#ifdef ECHRNG
  ECHRNG,
#else
  0,
#endif
#ifdef ECOMM
  ECOMM,
#else
  0,
#endif
#ifdef ECONNABORTED
  ECONNABORTED,
#else
  0,
#endif
#ifdef ECONNREFUSED
  ECONNREFUSED,
#else
  0,
#endif
#ifdef ECONNRESET
  ECONNRESET,
#else
  0,
#endif
#ifdef ED
  ED,
#else
  0,
#endif
#ifdef EDEADLK
  EDEADLK,
#else
  0,
#endif
#ifdef EDEADLOCK
  EDEADLOCK,
#else
  0,
#endif
#ifdef EDESTADDRREQ
  EDESTADDRREQ,
#else
  0,
#endif
#ifdef EDIED
  EDIED,
#else
  0,
#endif
#ifdef EDOM
  EDOM,
#else
  0,
#endif
#ifdef EDOTDOT
  EDOTDOT,
#else
  0,
#endif
#ifdef EDQUOT
  EDQUOT,
#else
  0,
#endif
#ifdef EEXIST
  EEXIST,
#else
  0,
#endif
#ifdef EFAULT
  EFAULT,
#else
  0,
#endif
#ifdef EFBIG
  EFBIG,
#else
  0,
#endif
#ifdef EFTYPE
  EFTYPE,
#else
  0,
#endif
#ifdef EGRATUITOUS
  EGRATUITOUS,
#else
  0,
#endif
#ifdef EGREGIOUS
  EGREGIOUS,
#else
  0,
#endif
#ifdef EHOSTDOWN
  EHOSTDOWN,
#else
  0,
#endif
#ifdef EHOSTUNREACH
  EHOSTUNREACH,
#else
  0,
#endif
#ifdef EIDRM
  EIDRM,
#else
  0,
#endif
#ifdef EIEIO
  EIEIO,
#else
  0,
#endif
#ifdef EILSEQ
  EILSEQ,
#else
  0,
#endif
#ifdef EINPROGRESS
  EINPROGRESS,
#else
  0,
#endif
#ifdef EINTR
  EINTR,
#else
  0,
#endif
#ifdef EINVAL
  EINVAL,
#else
  0,
#endif
#ifdef EIO
  EIO,
#else
  0,
#endif
#ifdef EISCONN
  EISCONN,
#else
  0,
#endif
#ifdef EISDIR
  EISDIR,
#else
  0,
#endif
#ifdef EISNAM
  EISNAM,
#else
  0,
#endif
#ifdef EL2HLT
  EL2HLT,
#else
  0,
#endif
#ifdef EL2NSYNC
  EL2NSYNC,
#else
  0,
#endif
#ifdef EL3HLT
  EL3HLT,
#else
  0,
#endif
#ifdef EL3RST
  EL3RST,
#else
  0,
#endif
#ifdef ELIBACC
  ELIBACC,
#else
  0,
#endif
#ifdef ELIBBAD
  ELIBBAD,
#else
  0,
#endif
#ifdef ELIBEXEC
  ELIBEXEC,
#else
  0,
#endif
#ifdef ELIBMAX
  ELIBMAX,
#else
  0,
#endif
#ifdef ELIBSCN
  ELIBSCN,
#else
  0,
#endif
#ifdef ELNRNG
  ELNRNG,
#else
  0,
#endif
#ifdef ELOOP
  ELOOP,
#else
  0,
#endif
#ifdef EMEDIUMTYPE
  EMEDIUMTYPE,
#else
  0,
#endif
#ifdef EMFILE
  EMFILE,
#else
  0,
#endif
#ifdef EMLINK
  EMLINK,
#else
  0,
#endif
#ifdef EMSGSIZE
  EMSGSIZE,
#else
  0,
#endif
#ifdef EMULTIHOP
  EMULTIHOP,
#else
  0,
#endif
#ifdef ENAMETOOLONG
  ENAMETOOLONG,
#else
  0,
#endif
#ifdef ENAVAIL
  ENAVAIL,
#else
  0,
#endif
#ifdef ENEEDAUTH
  ENEEDAUTH,
#else
  0,
#endif
#ifdef ENETDOWN
  ENETDOWN,
#else
  0,
#endif
#ifdef ENETRESET
  ENETRESET,
#else
  0,
#endif
#ifdef ENETUNREACH
  ENETUNREACH,
#else
  0,
#endif
#ifdef ENFILE
  ENFILE,
#else
  0,
#endif
#ifdef ENOANO
  ENOANO,
#else
  0,
#endif
#ifdef ENOBUFS
  ENOBUFS,
#else
  0,
#endif
#ifdef ENOCSI
  ENOCSI,
#else
  0,
#endif
#ifdef ENODATA
  ENODATA,
#else
  0,
#endif
#ifdef ENODEV
  ENODEV,
#else
  0,
#endif
#ifdef ENOENT
  ENOENT,
#else
  0,
#endif
#ifdef ENOEXEC
  ENOEXEC,
#else
  0,
#endif
#ifdef ENOLCK
  ENOLCK,
#else
  0,
#endif
#ifdef ENOLINK
  ENOLINK,
#else
  0,
#endif
#ifdef ENOMEDIUM
  ENOMEDIUM,
#else
  0,
#endif
#ifdef ENOMEM
  ENOMEM,
#else
  0,
#endif
#ifdef ENOMSG
  ENOMSG,
#else
  0,
#endif
#ifdef ENONET
  ENONET,
#else
  0,
#endif
#ifdef ENOPKG
  ENOPKG,
#else
  0,
#endif
#ifdef ENOPROTOOPT
  ENOPROTOOPT,
#else
  0,
#endif
#ifdef ENOSPC
  ENOSPC,
#else
  0,
#endif
#ifdef ENOSR
  ENOSR,
#else
  0,
#endif
#ifdef ENOSTR
  ENOSTR,
#else
  0,
#endif
#ifdef ENOSYS
  ENOSYS,
#else
  0,
#endif
#ifdef ENOTBLK
  ENOTBLK,
#else
  0,
#endif
#ifdef ENOTCONN
  ENOTCONN,
#else
  0,
#endif
#ifdef ENOTDIR
  ENOTDIR,
#else
  0,
#endif
#ifdef ENOTEMPTY
  ENOTEMPTY,
#else
  0,
#endif
#ifdef ENOTNAM
  ENOTNAM,
#else
  0,
#endif
#ifdef ENOTSOCK
  ENOTSOCK,
#else
  0,
#endif
#ifdef ENOTSUP
  ENOTSUP,
#else
  0,
#endif
#ifdef ENOTTY
  ENOTTY,
#else
  0,
#endif
#ifdef ENOTUNIQ
  ENOTUNIQ,
#else
  0,
#endif
#ifdef ENXIO
  ENXIO,
#else
  0,
#endif
#ifdef EOPNOTSUPP
  EOPNOTSUPP,
#else
  0,
#endif
#ifdef EOVERFLOW
  EOVERFLOW,
#else
  0,
#endif
#ifdef EPERM
  EPERM,
#else
  0,
#endif
#ifdef EPFNOSUPPORT
  EPFNOSUPPORT,
#else
  0,
#endif
#ifdef EPIPE
  EPIPE,
#else
  0,
#endif
#ifdef EPROCLIM
  EPROCLIM,
#else
  0,
#endif
#ifdef EPROCUNAVAIL
  EPROCUNAVAIL,
#else
  0,
#endif
#ifdef EPROGMISMATCH
  EPROGMISMATCH,
#else
  0,
#endif
#ifdef EPROGUNAVAIL
  EPROGUNAVAIL,
#else
  0,
#endif
#ifdef EPROTO
  EPROTO,
#else
  0,
#endif
#ifdef EPROTONOSUPPORT
  EPROTONOSUPPORT,
#else
  0,
#endif
#ifdef EPROTOTYPE
  EPROTOTYPE,
#else
  0,
#endif
#ifdef ERANGE
  ERANGE,
#else
  0,
#endif
#ifdef EREMCHG
  EREMCHG,
#else
  0,
#endif
#ifdef EREMOTE
  EREMOTE,
#else
  0,
#endif
#ifdef EREMOTEIO
  EREMOTEIO,
#else
  0,
#endif
#ifdef ERESTART
  ERESTART,
#else
  0,
#endif
#ifdef EROFS
  EROFS,
#else
  0,
#endif
#ifdef ERPCMISMATCH
  ERPCMISMATCH,
#else
  0,
#endif
#ifdef ESHUTDOWN
  ESHUTDOWN,
#else
  0,
#endif
#ifdef ESOCKTNOSUPPORT
  ESOCKTNOSUPPORT,
#else
  0,
#endif
#ifdef ESPIPE
  ESPIPE,
#else
  0,
#endif
#ifdef ESRCH
  ESRCH,
#else
  0,
#endif
#ifdef ESRMNT
  ESRMNT,
#else
  0,
#endif
#ifdef ESTALE
  ESTALE,
#else
  0,
#endif
#ifdef ESTRPIPE
  ESTRPIPE,
#else
  0,
#endif
#ifdef ETIME
  ETIME,
#else
  0,
#endif
#ifdef ETIMEDOUT
  ETIMEDOUT,
#else
  0,
#endif
#ifdef ETOOMANYREFS
  ETOOMANYREFS,
#else
  0,
#endif
#ifdef ETXTBSY
  ETXTBSY,
#else
  0,
#endif
#ifdef EUCLEAN
  EUCLEAN,
#else
  0,
#endif
#ifdef EUNATCH
  EUNATCH,
#else
  0,
#endif
#ifdef EUSERS
  EUSERS,
#else
  0,
#endif
#ifdef EWOULDBLOCK
  EWOULDBLOCK,
#else
  0,
#endif
#ifdef EXDEV
  EXDEV,
#else
  0,
#endif
#ifdef EXFULL
  EXFULL,
#else
  0,
#endif
};
