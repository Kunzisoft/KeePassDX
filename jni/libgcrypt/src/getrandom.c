/* getrandom.c - Libgcrypt Random Number client
 * Copyright (C) 2006 Free Software Foundation, Inc.
 *
 * Getrandom is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Getrandom is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

#include <config.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>

#define PGM "getrandom"
#define MYVERSION_LINE PGM " (Libgcrypt) " VERSION
#define BUGREPORT_LINE "\nReport bugs to <bug-libgcrypt@gnupg.org>.\n"


static void
logit (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format) ;
  fputs (PGM ": ", stderr);
  vfprintf (stderr, format, arg_ptr);
  putc ('\n', stderr);
  va_end (arg_ptr);
}


/* Send LENGTH bytes of BUFFER to file descriptor FD.  Returns 0 on
   success or another value on write error. */
static int
writen (int fd, const void *buffer, size_t length)
{
  while (length)
    {
      ssize_t n;
      
      do
        n = write (fd, buffer, length);
      while (n < 0 && errno == EINTR);
      if (n < 0)
         {
           logit ("write error: %s", strerror (errno));
           return -1; /* write error */
         }
      length -= n;
      buffer = (const char *)buffer + n;
    }
  return 0;  /* Okay */
}




static void
print_version (int with_help)
{
  fputs (MYVERSION_LINE "\n"
         "Copyright (C) 2006 Free Software Foundation, Inc.\n"
         "License GPLv2+: GNU GPL version 2 or later "
         "<http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>\n"
         "This is free software: you are free to change and redistribute it.\n"
         "There is NO WARRANTY, to the extent permitted by law.\n",
         stdout);
        
  if (with_help)
    fputs ("\n"
           "Usage: " PGM " [OPTIONS] NBYTES\n"
           "Connect to libgcrypt's random number daemon and "
           "return random numbers"
           "\n"
           "  --nonce       Return weak random suitable for a nonce\n"
           "  --very-strong Return very strong random\n"
           "  --ping        Send a ping\n"
           "  --socket NAME Name of sockket to connect to\n"        
           "  --hex         Return result as a hex dump\n"
           "  --verbose     Show what we are doing\n"
           "  --version     Print version of the program and exit\n"
           "  --help        Display this help and exit\n"
           BUGREPORT_LINE, stdout );
  
  exit (0);
}

static int
print_usage (void)
{
  fputs ("usage: " PGM " [OPTIONS] NBYTES\n", stderr);
  fputs ("       (use --help to display options)\n", stderr);
  exit (1);
}


int 
main (int argc, char **argv)
{
  struct sockaddr_un *srvr_addr;
  socklen_t addrlen;
  int fd;
  int rc;
  unsigned char buffer[300];
  int nleft, nread;
  const char *socketname = "/var/run/libgcrypt/S.gcryptrnd";
  int do_ping = 0;
  int get_nonce = 0;
  int get_very_strong = 0;
  int req_nbytes, nbytes, n;
  int verbose = 0;
  int fail = 0;
  int do_hex = 0;
 
  if (argc)
    {
      argc--; argv++;
    }
  while (argc && **argv == '-' && (*argv)[1] == '-')
    {
      if (!(*argv)[2])
        {
          argc--; argv++;
          break;
        }
      else if (!strcmp (*argv, "--version"))
        print_version (0);
      else if (!strcmp (*argv, "--help"))
        print_version (1);
      else if (!strcmp (*argv, "--socket") && argc > 1 )
        {
          argc--; argv++;
          socketname = *argv;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--nonce"))
        {
          argc--; argv++;
          get_nonce = 1;
        }
      else if (!strcmp (*argv, "--very-strong"))
        {
          argc--; argv++;
          get_very_strong = 1;
        }
      else if (!strcmp (*argv, "--ping"))
        {
          argc--; argv++;
          do_ping = 1;
        }
      else if (!strcmp (*argv, "--hex"))
        {
          argc--; argv++;
          do_hex = 1;
        }
      else if (!strcmp (*argv, "--verbose"))
        {
          argc--; argv++;
          verbose = 1;
        }
      else
        print_usage ();
    }          

 
  if (!argc && do_ping)
    ; /* This is allowed. */
  else if (argc != 1)
    print_usage ();
  req_nbytes = argc? atoi (*argv) : 0;
  
  if (req_nbytes < 0)
    print_usage ();

  /* Create a socket. */
  fd = socket (AF_UNIX, SOCK_STREAM, 0);
  if (fd == -1)
    {
      logit ("can't create socket: %s", strerror (errno));
      exit (1);
    }
  srvr_addr = malloc (sizeof *srvr_addr); 
  if (!srvr_addr)
    {
      logit ("malloc failed: %s", strerror (errno));
      exit (1);
    }
  memset (srvr_addr, 0, sizeof *srvr_addr);
  srvr_addr->sun_family = AF_UNIX;
  if (strlen (socketname) + 1 >= sizeof (srvr_addr->sun_path))
    {
      logit ("socket name `%s' too long", socketname);
      exit (1);
    }
  strcpy (srvr_addr->sun_path, socketname);
  addrlen = (offsetof (struct sockaddr_un, sun_path)
             + strlen (srvr_addr->sun_path) + 1);
  rc = connect (fd, (struct sockaddr*) srvr_addr, addrlen);
  if (rc == -1)
    {
      logit ("error connecting socket `%s': %s",
             srvr_addr->sun_path, strerror (errno));
      close (fd);
      exit (1);
    }

  do
    {
      nbytes = req_nbytes > 255? 255 : req_nbytes;
      req_nbytes -= nbytes;

      buffer[0] = 3;
      if (do_ping)
        buffer[1] = 0;
      else if (get_nonce)
        buffer[1] = 10;
      else if (get_very_strong)
        buffer[1] = 12;
      else
        buffer[1] = 11;
      buffer[2] = nbytes;
      if (writen (fd, buffer, 3))
        fail = 1;
      else
        {
          for (nleft=2, nread=0; nleft > 0; )
            {
              do 
                n = read (fd, buffer+nread, nleft);
              while (n < 0 && errno == EINTR);
              if (n < 0)
                {
                  logit ("read error: %s", strerror (errno));
                  exit (1);
                }
              nleft -= n;
              nread += n;
              if (nread && buffer[0])
                {
                  logit ("server returned error code %d", buffer[0]);
                  exit (1);
                }
            }
          if (verbose)
            logit ("received response with %d bytes of data", buffer[1]);
          if (buffer[1] < nbytes)
            {
              logit ("warning: server returned less bytes than requested");
              fail = 1;
            }
          else if (buffer[1] > nbytes && !do_ping)
            {
              logit ("warning: server returned more bytes than requested");
              fail = 1;
            }
          nbytes = buffer[1];
          if (nbytes > sizeof buffer)
            {
              logit ("buffer too short to receive data");
              exit (1);
            }

          for (nleft=nbytes, nread=0; nleft > 0; )
            {
              do 
                n = read (fd, buffer+nread, nleft);
              while (n < 0 && errno == EINTR);
              if (n < 0)
                {
                  logit ("read error: %s", strerror (errno));
                  exit (1);
                }
              nleft -= n;
              nread += n;
            }

          if (do_hex)
            {
              for (n=0; n < nbytes; n++)
                {
                  if (!n)
                    ;
                  else if (!(n % 16))
                    putchar ('\n');
                  else
                    putchar (' ');
                  printf ("%02X", buffer[n]);
                }
              if (nbytes)
                putchar ('\n');
            }
          else
            {
              if (fwrite (buffer, nbytes, 1, stdout) != 1)
                {
                  logit ("error writing to stdout: %s", strerror (errno));
                  fail = 1;
                }
            }
        }
    }
  while (!fail && req_nbytes);

  close (fd);
  free (srvr_addr);
  return fail? 1 : 0;
}

