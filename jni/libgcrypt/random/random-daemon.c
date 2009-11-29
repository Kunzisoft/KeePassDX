/* random-daemon.c  - Access to the external random daemon
 * Copyright (C) 2006  Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Libgcrypt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */

/*
   The functions here are used by random.c to divert calls to an
   external random number daemon.  The actual daemon we use is
   gcryptrnd.  Such a daemon is useful to keep a persistent pool in
   memory over invocations of a single application and to allow
   prioritizing access to the actual entropy sources.  The drawback is
   that we need to use IPC (i.e. unix domain socket) to convey
   sensitive data.
 */


#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <errno.h>
#include <unistd.h>

#include "g10lib.h"
#include "random.h"
#include "ath.h"



/* This is default socket name we use in case the provided socket name
   is NULL.  */
#define RANDOM_DAEMON_SOCKET "/var/run/libgcrypt/S.gcryptrnd"

/* The lock serializing access to the daemon.  */
static ath_mutex_t daemon_lock = ATH_MUTEX_INITIALIZER;

/* The socket connected to the daemon.  */
static int daemon_socket = -1; 

/* Creates a socket connected to the daemon.  On success, store the
   socket fd in *SOCK.  Returns error code.  */
static gcry_error_t
connect_to_socket (const char *socketname, int *sock)
{
  struct sockaddr_un *srvr_addr;
  socklen_t addrlen;
  gcry_error_t err;
  int fd;
  int rc;

  srvr_addr = NULL;

  /* Create a socket. */
  fd = socket (AF_UNIX, SOCK_STREAM, 0);
  if (fd == -1)
    {
      log_error ("can't create socket: %s\n", strerror (errno));
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Set up address.  */
  srvr_addr = gcry_malloc (sizeof *srvr_addr); 
  if (! srvr_addr)
    {
      log_error ("malloc failed: %s\n", strerror (errno));
      err = gcry_error_from_errno (errno);
      goto out;
    }
  memset (srvr_addr, 0, sizeof *srvr_addr);
  srvr_addr->sun_family = AF_UNIX;
  if (strlen (socketname) + 1 >= sizeof (srvr_addr->sun_path))
    {
      log_error ("socket name `%s' too long\n", socketname);
      err = gcry_error (GPG_ERR_ENAMETOOLONG);
      goto out;
    }
  strcpy (srvr_addr->sun_path, socketname);
  addrlen = (offsetof (struct sockaddr_un, sun_path)
             + strlen (srvr_addr->sun_path) + 1);

  /* Connect socket.  */
  rc = connect (fd, (struct sockaddr *) srvr_addr, addrlen);
  if (rc == -1)
    {
      log_error ("error connecting socket `%s': %s\n",
		 srvr_addr->sun_path, strerror (errno));
      err = gcry_error_from_errno (errno);
      goto out;
    }

  err = 0;

 out:

  gcry_free (srvr_addr);
  if (err)
    {
      close (fd);
      fd = -1;
    }
  *sock = fd;

  return err;
}


/* Initialize basics of this module. This should be viewed as a
   constructor to prepare locking. */
void
_gcry_daemon_initialize_basics (void)
{
  static int initialized;
  int err;

  if (!initialized)
    {
      initialized = 1;
      err = ath_mutex_init (&daemon_lock);
      if (err)
        log_fatal ("failed to create the daemon lock: %s\n", strerror (err) );
    }
}



/* Send LENGTH bytes of BUFFER to file descriptor FD.  Returns 0 on
   success or another value on write error. */
static int
writen (int fd, const void *buffer, size_t length)
{
  ssize_t n;
      
  while (length)
    {
      do
        n = ath_write (fd, buffer, length);
      while (n < 0 && errno == EINTR);
      if (n < 0)
         {
           log_error ("write error: %s\n", strerror (errno));
           return -1; /* write error */
         }
      length -= n;
      buffer = (const char*)buffer + n;
    }
  return 0;  /* Okay */
}

static int
readn (int fd, void *buf, size_t buflen, size_t *ret_nread)
{
  size_t nleft = buflen;
  int nread;
  char *p;
  
  p = buf;
  while (nleft > 0)
    {
      nread = ath_read (fd, buf, nleft);
      if (nread < 0)
        {
          if (nread == EINTR)
            nread = 0;
          else 
            return -1;
        }
      else if (!nread)
        break; /* EOF */
      nleft -= nread;
      buf = (char*)buf + nread;
    }
  if (ret_nread)
    *ret_nread = buflen - nleft;
  return 0;
}

/* This functions requests REQ_NBYTES from the daemon.  If NONCE is
   true, the data should be suited for a nonce.  If NONCE is FALSE,
   data of random level LEVEL will be generated.  The retrieved random
   data will be stored in BUFFER.  Returns error code.  */
static gcry_error_t
call_daemon (const char *socketname,
             void *buffer, size_t req_nbytes, int nonce,
	     enum gcry_random_level level)
{
  static int initialized;
  unsigned char buf[255];
  gcry_error_t err = 0;
  size_t nbytes;
  size_t nread;
  int rc;

  if (!req_nbytes)
    return 0;

  ath_mutex_lock (&daemon_lock);

  /* Open the socket if that has not been done. */
  if (!initialized)
    {
      initialized = 1;
      err = connect_to_socket (socketname ? socketname : RANDOM_DAEMON_SOCKET,
			       &daemon_socket);
      if (err)
        {
          daemon_socket = -1;
          log_info ("not using random daemon\n");
          ath_mutex_unlock (&daemon_lock);
          return err;
        }
    }

  /* Check that we have a valid socket descriptor. */
  if ( daemon_socket == -1 )
    {
      ath_mutex_unlock (&daemon_lock);
      return gcry_error (GPG_ERR_INTERNAL);
    }


  /* Do the real work.  */

  do
    {
      /* Process in chunks.  */
      nbytes = req_nbytes > sizeof (buf) ? sizeof (buf) : req_nbytes;
      req_nbytes -= nbytes;

      /* Construct request.  */
      buf[0] = 3;
      if (nonce)
	buf[1] = 10;
      else if (level == GCRY_VERY_STRONG_RANDOM)
	buf[1] = 12;
      else if (level == GCRY_STRONG_RANDOM)
	buf[1] = 11;
      buf[2] = nbytes;

      /* Send request.  */
      rc = writen (daemon_socket, buf, 3);
      if (rc == -1)
	{
	  err = gcry_error_from_errno (errno);
	  break;
	}
	
      /* Retrieve response.  */

      rc = readn (daemon_socket, buf, 2, &nread);
      if (rc == -1)
	{
	  err = gcry_error_from_errno (errno);
	  log_error ("read error: %s\n", gcry_strerror (err));
	  break;
	}
      if (nread && buf[0])
	{
	  log_error ("random daemon returned error code %d\n", buf[0]);
	  err = gcry_error (GPG_ERR_INTERNAL); /* ? */
	  break;
	}
      if (nread != 2)
	{
	  log_error ("response too small\n");
	  err = gcry_error (GPG_ERR_PROTOCOL_VIOLATION); /* ? */
	  break;
	}

      /*      if (1)*/			/* Do this in verbose mode? */
      /*	log_info ("received response with %d bytes of data\n", buf[1]);*/

      if (buf[1] < nbytes)
	{
	  log_error ("error: server returned less bytes than requested\n");
	  err = gcry_error (GPG_ERR_PROTOCOL_VIOLATION); /* ? */
	  break;
	}
      else if (buf[1] > nbytes)
	{
	  log_error ("warning: server returned more bytes than requested\n");
	  err = gcry_error (GPG_ERR_PROTOCOL_VIOLATION); /* ? */
	  break;
	}

      assert (nbytes <= sizeof (buf));

      rc = readn (daemon_socket, buf, nbytes, &nread);
      if (rc == -1)
	{
	  err = gcry_error_from_errno (errno);
	  log_error ("read error: %s\n", gcry_strerror (err));
	  break;
	}
      
      if (nread != nbytes)
	{
	  log_error ("too little random data read\n");
	  err = gcry_error (GPG_ERR_INTERNAL);
	  break;
	}

      /* Successfuly read another chunk of data.  */
      memcpy (buffer, buf, nbytes);
      buffer = ((char *) buffer) + nbytes;
    }
  while (req_nbytes);

  ath_mutex_unlock (&daemon_lock);

  return err;
}

/* Internal function to fill BUFFER with LENGTH bytes of random.  We
   support GCRY_STRONG_RANDOM and GCRY_VERY_STRONG_RANDOM here.
   Return 0 on success. */
int
_gcry_daemon_randomize (const char *socketname, 
                        void *buffer, size_t length,
                        enum gcry_random_level level)
{
  gcry_error_t err;

  err = call_daemon (socketname, buffer, length, 0, level);

  return err ? -1 : 0;
}


/* Internal function to fill BUFFER with NBYTES of data usable for a
   nonce.  Returns 0 on success. */
int
_gcry_daemon_create_nonce (const char *socketname, void *buffer, size_t length)
{
  gcry_error_t err;

  err = call_daemon (socketname, buffer, length, 1, 0);

  return err ? -1 : 0;
}

/* END */
