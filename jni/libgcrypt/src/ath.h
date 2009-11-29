/* ath.h - Thread-safeness library.
   Copyright (C) 2002, 2003, 2004 Free Software Foundation, Inc.

   This file is part of Libgcrypt.
 
   Libgcrypt is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
 
   Libgcrypt is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
 
   You should have received a copy of the GNU Lesser General Public
   License along with Libgcrypt; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */

#ifndef ATH_H
#define ATH_H

#ifdef _WIN32
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#endif
#include <gpg-error.h>

#include <config.h>


/* Define _ATH_EXT_SYM_PREFIX if you want to give all external symbols
   a prefix.  */
#define _ATH_EXT_SYM_PREFIX _gcry_

#ifdef _ATH_EXT_SYM_PREFIX
#define _ATH_PREFIX1(x,y) x ## y
#define _ATH_PREFIX2(x,y) _ATH_PREFIX1(x,y)
#define _ATH_PREFIX(x) _ATH_PREFIX2(_ATH_EXT_SYM_PREFIX,x)
#define ath_install _ATH_PREFIX(ath_install)
#define ath_init _ATH_PREFIX(ath_init)
#define ath_mutex_init _ATH_PREFIX(ath_mutex_init)
#define ath_mutex_destroy _ATH_PREFIX(ath_mutex_destroy)
#define ath_mutex_lock _ATH_PREFIX(ath_mutex_lock)
#define ath_mutex_unlock _ATH_PREFIX(ath_mutex_unlock)
#define ath_read _ATH_PREFIX(ath_read)
#define ath_write _ATH_PREFIX(ath_write)
#define ath_select _ATH_PREFIX(ath_select)
#define ath_waitpid _ATH_PREFIX(ath_waitpid)
#define ath_connect _ATH_PREFIX(ath_connect)
#define ath_accept _ATH_PREFIX(ath_accept)
#define ath_sendmsg _ATH_PREFIX(ath_sendmsg)
#define ath_recvmsg _ATH_PREFIX(ath_recvmsg)
#endif


enum ath_thread_option
  {
    ATH_THREAD_OPTION_DEFAULT = 0,
    ATH_THREAD_OPTION_USER = 1,
    ATH_THREAD_OPTION_PTH = 2,
    ATH_THREAD_OPTION_PTHREAD = 3
  };

struct ath_ops
{
  /* The OPTION field encodes the thread model and the version number
     of this structure.   
       Bits  7 - 0  are used for the thread model
       Bits 15 - 8  are used for the version number.
  */
  unsigned int option;

  int (*init) (void);
  int (*mutex_init) (void **priv);
  int (*mutex_destroy) (void *priv);
  int (*mutex_lock) (void *priv);
  int (*mutex_unlock) (void *priv);
  ssize_t (*read) (int fd, void *buf, size_t nbytes);
  ssize_t (*write) (int fd, const void *buf, size_t nbytes);
#ifdef _WIN32
  ssize_t (*select) (int nfd, void *rset, void *wset, void *eset,
		     struct timeval *timeout);
  ssize_t (*waitpid) (pid_t pid, int *status, int options);
  int (*accept) (int s, void  *addr, int *length_ptr);
  int (*connect) (int s, void *addr, int length);
  int (*sendmsg) (int s, const void *msg, int flags);
  int (*recvmsg) (int s, void *msg, int flags);
#else
  ssize_t (*select) (int nfd, fd_set *rset, fd_set *wset, fd_set *eset,
		     struct timeval *timeout);
  ssize_t (*waitpid) (pid_t pid, int *status, int options);
  int (*accept) (int s, struct sockaddr *addr, socklen_t *length_ptr);
  int (*connect) (int s, struct sockaddr *addr, socklen_t length);
  int (*sendmsg) (int s, const struct msghdr *msg, int flags);
  int (*recvmsg) (int s, struct msghdr *msg, int flags);
#endif
};

gpg_err_code_t ath_install (struct ath_ops *ath_ops, int check_only);
int ath_init (void);


/* Functions for mutual exclusion.  */
typedef void *ath_mutex_t;
#define ATH_MUTEX_INITIALIZER 0

int ath_mutex_init (ath_mutex_t *mutex);
int ath_mutex_destroy (ath_mutex_t *mutex);
int ath_mutex_lock (ath_mutex_t *mutex);
int ath_mutex_unlock (ath_mutex_t *mutex);

/* Replacement for the POSIX functions, which can be used to allow
   other (user-level) threads to run.  */
ssize_t ath_read (int fd, void *buf, size_t nbytes);
ssize_t ath_write (int fd, const void *buf, size_t nbytes);
#ifdef _WIN32
ssize_t ath_select (int nfd, void *rset, void *wset, void *eset,
		    struct timeval *timeout);
ssize_t ath_waitpid (pid_t pid, int *status, int options);
int ath_accept (int s, void *addr, int *length_ptr);
int ath_connect (int s, void *addr, int length);
int ath_sendmsg (int s, const void *msg, int flags);
int ath_recvmsg (int s, void *msg, int flags);
#else
ssize_t ath_select (int nfd, fd_set *rset, fd_set *wset, fd_set *eset,
		    struct timeval *timeout);
ssize_t ath_waitpid (pid_t pid, int *status, int options);
int ath_accept (int s, struct sockaddr *addr, socklen_t *length_ptr);
int ath_connect (int s, struct sockaddr *addr, socklen_t length);
int ath_sendmsg (int s, const struct msghdr *msg, int flags);
int ath_recvmsg (int s, struct msghdr *msg, int flags);
#endif 

#endif	/* ATH_H */
