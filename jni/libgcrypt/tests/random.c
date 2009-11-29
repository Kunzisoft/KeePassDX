/* random.c - part of the Libgcrypt test suite.
   Copyright (C) 2005 Free Software Foundation, Inc.

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
   USA.  */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <sys/wait.h>

#include "../src/gcrypt.h"

static int verbose;

static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}


static void
print_hex (const char *text, const void *buf, size_t n)
{
  const unsigned char *p = buf;

  fputs (text, stdout);
  for (; n; n--, p++)
    printf ("%02X", *p);
  putchar ('\n');
}


static int
writen (int fd, const void *buf, size_t nbytes)
{
  size_t nleft = nbytes;
  int nwritten;
  
  while (nleft > 0)
    {
      nwritten = write (fd, buf, nleft);
      if (nwritten < 0)
        {
          if (errno == EINTR)
            nwritten = 0;
          else 
            return -1;
        }
      nleft -= nwritten;
      buf = (const char*)buf + nwritten;
    }
    
  return 0;
}

static int
readn (int fd, void *buf, size_t buflen, size_t *ret_nread)
{
  size_t nleft = buflen;
  int nread;
  char *p;
  
  p = buf;
  while ( nleft > 0 )
    {
      nread = read ( fd, buf, nleft );
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



/* Check that forking won't return the same random. */
static void
check_forking (void)
{
  pid_t pid;
  int rp[2];
  int i, status;
  size_t nread;
  char tmp1[16], tmp1c[16], tmp1p[16];
  
  /* We better make sure that the RNG has been initialzied. */
  gcry_randomize (tmp1, sizeof tmp1, GCRY_STRONG_RANDOM);
  if (verbose)
    print_hex ("initial random: ", tmp1, sizeof tmp1);

  if (pipe (rp) == -1)
    die ("pipe failed: %s\n", strerror (errno));

  pid = fork ();
  if (pid == (pid_t)(-1))
    die ("fork failed: %s\n", strerror (errno));
  if (!pid)
    {
      gcry_randomize (tmp1c, sizeof tmp1c, GCRY_STRONG_RANDOM);
      if (writen (rp[1], tmp1c, sizeof tmp1c))
        die ("write failed: %s\n", strerror (errno));
      if (verbose)
        {
          print_hex ("  child random: ", tmp1c, sizeof tmp1c);
          fflush (stdout);
        }
      _exit (0);
    }
  gcry_randomize (tmp1p, sizeof tmp1p, GCRY_STRONG_RANDOM);
  if (verbose)
    print_hex (" parent random: ", tmp1p, sizeof tmp1p);

  close (rp[1]);
  if (readn (rp[0], tmp1c, sizeof tmp1c, &nread))
    die ("read failed: %s\n", strerror (errno)); 
  if (nread != sizeof tmp1c)
    die ("read too short\n");

  while ( (i=waitpid (pid, &status, 0)) == -1 && errno == EINTR)
    ;
  if (i != (pid_t)(-1)
      && WIFEXITED (status) && !WEXITSTATUS (status))
    ;
  else
    die ("child failed\n");

  if (!memcmp (tmp1p, tmp1c, sizeof tmp1c))
    die ("parent and child got the same random number\n");
}



/* Check that forking won't return the same nonce. */
static void
check_nonce_forking (void)
{
  pid_t pid;
  int rp[2];
  int i, status;
  size_t nread;
  char nonce1[10], nonce1c[10], nonce1p[10];
  
  /* We won't get the same nonce back if we never initialized the
     nonce subsystem, thus we get one nonce here and forget about
     it. */
  gcry_create_nonce (nonce1, sizeof nonce1);
  if (verbose)
    print_hex ("initial nonce: ", nonce1, sizeof nonce1);

  if (pipe (rp) == -1)
    die ("pipe failed: %s\n", strerror (errno));

  pid = fork ();
  if (pid == (pid_t)(-1))
    die ("fork failed: %s\n", strerror (errno));
  if (!pid)
    {
      gcry_create_nonce (nonce1c, sizeof nonce1c);
      if (writen (rp[1], nonce1c, sizeof nonce1c))
        die ("write failed: %s\n", strerror (errno));
      if (verbose)
        {
          print_hex ("  child nonce: ", nonce1c, sizeof nonce1c);
          fflush (stdout);
        }
      _exit (0);
    }
  gcry_create_nonce (nonce1p, sizeof nonce1p);
  if (verbose)
    print_hex (" parent nonce: ", nonce1p, sizeof nonce1p);

  close (rp[1]);
  if (readn (rp[0], nonce1c, sizeof nonce1c, &nread))
    die ("read failed: %s\n", strerror (errno)); 
  if (nread != sizeof nonce1c)
    die ("read too short\n");

  while ( (i=waitpid (pid, &status, 0)) == -1 && errno == EINTR)
    ;
  if (i != (pid_t)(-1)
      && WIFEXITED (status) && !WEXITSTATUS (status))
    ;
  else
    die ("child failed\n");

  if (!memcmp (nonce1p, nonce1c, sizeof nonce1c))
    die ("parent and child got the same nonce\n");
}






int
main (int argc, char **argv)
{
  int debug = 0;

  if ((argc > 1) && (! strcmp (argv[1], "--verbose")))
    verbose = 1;
  else if ((argc > 1) && (! strcmp (argv[1], "--debug")))
    verbose = debug = 1;

  signal (SIGPIPE, SIG_IGN);

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (!gcry_check_version (GCRYPT_VERSION))
    die ("version mismatch\n");

  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u, 0);

  check_forking ();
  check_nonce_forking ();

  return 0;
}
