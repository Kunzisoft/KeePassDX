/* ath.c - Thread-safeness library.
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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <assert.h>  /* Right: We need to use assert and not gcry_assert.  */
#include <unistd.h>
#ifdef HAVE_SYS_SELECT_H
# include <sys/select.h>
#else
# include <sys/time.h>
#endif
#include <sys/types.h>
#ifndef _WIN32
#include <sys/wait.h>
#endif
#include <errno.h>

#include "ath.h"



/* The interface table.  */
static struct ath_ops ops;

/* True if we should use the external callbacks.  */
static int ops_set;


/* For the dummy interface.  */
#define MUTEX_UNLOCKED	((ath_mutex_t) 0)
#define MUTEX_LOCKED	((ath_mutex_t) 1)
#define MUTEX_DESTROYED	((ath_mutex_t) 2)


/* Return the thread type from the option field. */
#define GET_OPTION(a)    ((a) & 0xff)
/* Return the version number from the option field.  */
#define GET_VERSION(a)   (((a) >> 8)& 0xff)



/* The lock we take while checking for lazy lock initialization.  */
static ath_mutex_t check_init_lock = ATH_MUTEX_INITIALIZER;

int
ath_init (void)
{
  int err = 0;

  if (ops_set)
    {
      if (ops.init)
	err = (*ops.init) ();
      if (err)
	return err;
      err = (*ops.mutex_init) (&check_init_lock);
    }
  return err;
}


/* Initialize the locking library.  Returns 0 if the operation was
   successful, EINVAL if the operation table was invalid and EBUSY if
   we already were initialized.  */
gpg_err_code_t
ath_install (struct ath_ops *ath_ops, int check_only)
{
  if (check_only)
    {
      unsigned int option = 0;
      
      /* Check if the requested thread option is compatible to the
	 thread option we are already committed to.  */
      if (ath_ops)
	option = ath_ops->option;

      if (!ops_set && GET_OPTION (option))
	return GPG_ERR_NOT_SUPPORTED;

      if (GET_OPTION (ops.option) == ATH_THREAD_OPTION_USER
	  || GET_OPTION (option) == ATH_THREAD_OPTION_USER
	  || GET_OPTION (ops.option) != GET_OPTION (option)
          || GET_VERSION (ops.option) != GET_VERSION (option))
	return GPG_ERR_NOT_SUPPORTED;

      return 0;
    }
    
  if (ath_ops)
    {
      /* It is convenient to not require DESTROY.  */
      if (!ath_ops->mutex_init || !ath_ops->mutex_lock
	  || !ath_ops->mutex_unlock)
	return GPG_ERR_INV_ARG;

      ops = *ath_ops;
      ops_set = 1;
    }
  else
    ops_set = 0;

  return 0;
}


static int
mutex_init (ath_mutex_t *lock, int just_check)
{
  int err = 0;

  if (just_check)
    (*ops.mutex_lock) (&check_init_lock);
  if (*lock == ATH_MUTEX_INITIALIZER || !just_check)
    err = (*ops.mutex_init) (lock);
  if (just_check)
    (*ops.mutex_unlock) (&check_init_lock);
  return err;
}


int
ath_mutex_init (ath_mutex_t *lock)
{
  if (ops_set)
    return mutex_init (lock, 0);

#ifndef NDEBUG
  *lock = MUTEX_UNLOCKED;
#endif
  return 0;
}


int
ath_mutex_destroy (ath_mutex_t *lock)
{
  if (ops_set)
    {
      if (!ops.mutex_destroy)
	return 0;

      (*ops.mutex_lock) (&check_init_lock);
      if (*lock == ATH_MUTEX_INITIALIZER)
	{
	  (*ops.mutex_unlock) (&check_init_lock);
	  return 0;
	}
      (*ops.mutex_unlock) (&check_init_lock);
      return (*ops.mutex_destroy) (lock);
    }

#ifndef NDEBUG
  assert (*lock == MUTEX_UNLOCKED);

  *lock = MUTEX_DESTROYED;
#endif
  return 0;
}


int
ath_mutex_lock (ath_mutex_t *lock)
{
  if (ops_set)
    {
      int ret = mutex_init (lock, 1);
      if (ret)
	return ret;
      return (*ops.mutex_lock) (lock);
    }

#ifndef NDEBUG
  assert (*lock == MUTEX_UNLOCKED);

  *lock = MUTEX_LOCKED;
#endif
  return 0;
}


int
ath_mutex_unlock (ath_mutex_t *lock)
{
  if (ops_set)
    {
      int ret = mutex_init (lock, 1);
      if (ret)
	return ret;
      return (*ops.mutex_unlock) (lock);
    }

#ifndef NDEBUG
  assert (*lock == MUTEX_LOCKED);

  *lock = MUTEX_UNLOCKED;
#endif
  return 0;
}


ssize_t
ath_read (int fd, void *buf, size_t nbytes)
{
  if (ops_set && ops.read)
    return (*ops.read) (fd, buf, nbytes);
  else
    return read (fd, buf, nbytes);
}


ssize_t
ath_write (int fd, const void *buf, size_t nbytes)
{
  if (ops_set && ops.write)
    return (*ops.write) (fd, buf, nbytes);
  else
    return write (fd, buf, nbytes);
}


ssize_t
#ifdef _WIN32
ath_select (int nfd, void *rset, void *wset, void *eset,
	    struct timeval *timeout)
#else
ath_select (int nfd, fd_set *rset, fd_set *wset, fd_set *eset,
	    struct timeval *timeout)
#endif
{
  if (ops_set && ops.select)
    return (*ops.select) (nfd, rset, wset, eset, timeout);
  else
#ifdef _WIN32
    return -1;
#else
    return select (nfd, rset, wset, eset, timeout);
#endif
}

 
ssize_t
ath_waitpid (pid_t pid, int *status, int options)
{
  if (ops_set && ops.waitpid)
    return (*ops.waitpid) (pid, status, options);
  else
#ifdef _WIN32
    return -1;
#else
    return waitpid (pid, status, options);
#endif
}


int
#ifdef _WIN32
ath_accept (int s, void *addr, int *length_ptr)
#else
ath_accept (int s, struct sockaddr *addr, socklen_t *length_ptr)
#endif
{
  if (ops_set && ops.accept)
    return (*ops.accept) (s, addr, length_ptr);
  else
#ifdef _WIN32
    return -1;
#else
    return accept (s, addr, length_ptr);
#endif
}


int
#ifdef _WIN32
ath_connect (int s, void *addr, int length)
#else
ath_connect (int s, struct sockaddr *addr, socklen_t length)
#endif
{
  if (ops_set && ops.connect)
    return (*ops.connect) (s, addr, length);
  else
#ifdef _WIN32
    return -1;
#else
    return connect (s, addr, length);
#endif
}


int
#ifdef _WIN32
ath_sendmsg (int s, const void *msg, int flags)
#else
ath_sendmsg (int s, const struct msghdr *msg, int flags)
#endif
{
  if (ops_set && ops.sendmsg)
    return (*ops.sendmsg) (s, msg, flags);
  else
#ifdef _WIN32
    return -1;
#else
    return sendmsg (s, msg, flags);
#endif
}


int
#ifdef _WIN32
ath_recvmsg (int s, void *msg, int flags)
#else
ath_recvmsg (int s, struct msghdr *msg, int flags)
#endif
{
  if (ops_set && ops.recvmsg)
    return (*ops.recvmsg) (s, msg, flags);
  else
#ifdef _WIN32
    return -1;
#else
    return recvmsg (s, msg, flags);
#endif
}

