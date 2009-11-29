/* gcryptrnd.c - Libgcrypt Random Number Daemon
 * Copyright (C) 2006 Free Software Foundation, Inc.
 *
 * Gcryptend is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Gcryptrnd is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

/* We require vsyslog pth
   We need to test for:  setrlimit

   We should also prioritize requests.  This is best done by putting
   the requests into queues and have a main thread processing these
   queues.

 */

#include <config.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <assert.h>
#include <time.h>
#include <sys/times.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <syslog.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>
#include <pth.h>
#include <gcrypt.h>

#define PGM "gcryptrnd"
#define MYVERSION_LINE PGM " (Libgcrypt) " VERSION
#define BUGREPORT_LINE "\nReport bugs to <bug-libgcrypt@gnupg.org>.\n"

/* Pth wrapper function definitions. */
GCRY_THREAD_OPTION_PTH_IMPL;


/* Flag set to true if we have been daemonized. */
static int running_detached;
/* Flag indicating that a shutdown has been requested.  */
static int shutdown_pending;
/* Counter for active connections.  */
static int active_connections;



/* Local prototypes.  */
static void serve (int listen_fd);





/* To avoid that a compiler optimizes certain memset calls away, these
   macros may be used instead. */
#define wipememory2(_ptr,_set,_len) do { \
              volatile char *_vptr=(volatile char *)(_ptr); \
              size_t _vlen=(_len); \
              while(_vlen) { *_vptr=(_set); _vptr++; _vlen--; } \
                  } while(0)
#define wipememory(_ptr,_len) wipememory2(_ptr,0,_len)




/* Error printing utility.  PRIORITY should be one of syslog's
   priority levels.  This fucntions prints to the stderro or syslog
   depending on whether we are already daemonized. */
static void
logit (int priority, const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format) ;
  if (running_detached)
    {
      vsyslog (priority, format, arg_ptr);
    }
  else
    {
      fputs (PGM ": ", stderr);
      vfprintf (stderr, format, arg_ptr);
      putc ('\n', stderr);
    }
  va_end (arg_ptr);
}

/* Callback used by libgcrypt for logging. */
static void
my_gcry_logger (void *dummy, int level, const char *format, va_list arg_ptr)
{
  (void)dummy;

  /* Map the log levels. */
  switch (level)
    {
    case GCRY_LOG_CONT: level = LOG_INFO /* FIXME */; break;
    case GCRY_LOG_INFO: level = LOG_INFO; break;
    case GCRY_LOG_WARN: level = LOG_WARNING; break;
    case GCRY_LOG_ERROR:level = LOG_ERR; break;
    case GCRY_LOG_FATAL:level = LOG_CRIT; break;
    case GCRY_LOG_BUG:  level = LOG_CRIT; break;
    case GCRY_LOG_DEBUG:level = LOG_DEBUG; break;
    default:            level = LOG_ERR; break;  
    }
  if (running_detached)
    {
      vsyslog (level, format, arg_ptr);
    }
  else
    {
      fputs (PGM ": ", stderr);
      vfprintf (stderr, format, arg_ptr);
      if (!*format || format[strlen (format)-1] != '\n')
        putc ('\n', stderr);
    }
}


/* The cleanup handler - used to wipe out the secure memory. */
static void
cleanup (void)
{
  gcry_control (GCRYCTL_TERM_SECMEM );
}


/* Make us a daemon and open the syslog. */
static void
daemonize (void)
{
  int i;
  pid_t pid;

  fflush (NULL);

  pid = fork ();
  if (pid == (pid_t)-1) 
    {
      logit (LOG_CRIT, "fork failed: %s", strerror (errno));
      exit (1);
    }
  if (pid)
    exit (0); 

  if (setsid() == -1)
    {
      logit (LOG_CRIT, "setsid() failed: %s", strerror(errno));
      exit (1);
    }
  
  signal (SIGHUP, SIG_IGN);

  pid = fork ();
  if (pid == (pid_t)-1) 
    {
      logit (LOG_CRIT, PGM ": second fork failed: %s", strerror (errno));
      exit (1);
    }
  if (pid)
    exit (0); /* First child exits. */

  running_detached = 1;

  if (chdir("/"))
    {
      logit (LOG_CRIT, "chdir(\"/\") failed: %s", strerror (errno));
      exit (1);
    }
  umask (0);

  for (i=0; i <= 2; i++)
    close (i);

  openlog (PGM, LOG_PID, LOG_DAEMON);
}


static void
disable_core_dumps (void)
{
#ifdef HAVE_SETRLIMIT
  struct rlimit limit;

  if (getrlimit (RLIMIT_CORE, &limit))
    limit.rlim_max = 0;
  limit.rlim_cur = 0;
  if( !setrlimit (RLIMIT_CORE, &limit) )
    return 0;
  if (errno != EINVAL && errno != ENOSYS)
    logit (LOG_ERR, "can't disable core dumps: %s\n", strerror (errno));
#endif /* HAVE_SETRLIMIT */
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
           "Usage: " PGM " [OPTIONS] [SOCKETNAME]\n"
           "Start Libgcrypt's random number daemon listening"
           " on socket SOCKETNAME\n"
           "SOCKETNAME defaults to XXX\n"
           "\n"
           "  --no-detach   do not deatach from the console\n"        
           "  --version     print version of the program and exit\n"
           "  --help        display this help and exit\n"
           BUGREPORT_LINE, stdout );
  
  exit (0);
}

static int
print_usage (void)
{
  fputs ("usage: " PGM " [OPTIONS] [SOCKETNAME]\n", stderr);
  fputs ("       (use --help to display options)\n", stderr);
  exit (1);
}


int 
main (int argc, char **argv)
{
  int no_detach = 0;
  gpg_error_t err;
  struct sockaddr_un *srvr_addr;
  socklen_t addrlen;
  int fd;
  int rc;
  const char *socketname = "/var/run/libgcrypt/S.gcryptrnd";

 
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
      else if (!strcmp (*argv, "--no-detach"))
        {
          no_detach = 1;
          argc--; argv++;
        }
      else
        print_usage ();
    }          
 
  if (argc == 1)
    socketname = argv[0];
  else if (argc > 1)
    print_usage ();

  if (!no_detach)
    daemonize ();

  signal (SIGPIPE, SIG_IGN);

  logit (LOG_NOTICE, "started version " VERSION );

  /* Libgcrypt requires us to register the threading model before we
     do anything else with it. Note that this also calls pth_init.  We
     do the initialization while already running as a daemon to avoid
     overhead with double initialization of Libgcrypt. */
  err = gcry_control (GCRYCTL_SET_THREAD_CBS, &gcry_threads_pth);
  if (err)
    {
      logit (LOG_CRIT, "can't register GNU Pth with Libgcrypt: %s",
             gpg_strerror (err));
      exit (1);
    }

  /* Check that the libgcrypt version is sufficient.  */
  if (!gcry_check_version (VERSION) )
    {
      logit (LOG_CRIT, "libgcrypt is too old (need %s, have %s)",
             VERSION, gcry_check_version (NULL) );
      exit (1);
    }

  /* Register the logging callback and tell Libcgrypt to put the
     random pool into secure memory. */
  gcry_set_log_handler (my_gcry_logger, NULL);
  gcry_control (GCRYCTL_USE_SECURE_RNDPOOL);

  /* Obviously we don't want to allow any core dumps. */
  disable_core_dumps ();

  /* Initialize the secure memory stuff which will also drop any extra
     privileges we have. */
  gcry_control (GCRYCTL_INIT_SECMEM, 16384, 0);

  /* Register a cleanup handler. */
  atexit (cleanup);

  /* Create and listen on the socket. */
  fd = socket (AF_UNIX, SOCK_STREAM, 0);
  if (fd == -1)
    {
      logit (LOG_CRIT, "can't create socket: %s", strerror (errno));
      exit (1);
    }
  srvr_addr = gcry_xmalloc (sizeof *srvr_addr); 
  memset (srvr_addr, 0, sizeof *srvr_addr);
  srvr_addr->sun_family = AF_UNIX;
  if (strlen (socketname) + 1 >= sizeof (srvr_addr->sun_path))
    {
      logit (LOG_CRIT, "socket name `%s' too long", socketname);
      exit (1);
    }
  strcpy (srvr_addr->sun_path, socketname);
  addrlen = (offsetof (struct sockaddr_un, sun_path)
             + strlen (srvr_addr->sun_path) + 1);
  rc = bind (fd, (struct sockaddr*) srvr_addr, addrlen);
  if (rc == -1 && errno == EADDRINUSE)
    {
      remove (socketname);
      rc = bind (fd, (struct sockaddr*) srvr_addr, addrlen);
    }
  if (rc == -1)
    {
      logit (LOG_CRIT, "error binding socket to `%s': %s",
             srvr_addr->sun_path, strerror (errno));
      close (fd);
      exit (1);
    }

  if (listen (fd, 5 ) == -1)
    {
      logit (LOG_CRIT, "listen() failed: %s", strerror (errno));
      close (fd);
      exit (1);
    }
  
  logit (LOG_INFO, "listening on socket `%s', fd=%d",
         srvr_addr->sun_path, fd);

  serve (fd);
  close (fd);

  logit (LOG_NOTICE, "stopped version " VERSION );
  return 0;
}


/* Send LENGTH bytes of BUFFER to file descriptor FD.  Returns 0 on
   success or another value on write error. */
static int
writen (int fd, const void *buffer, size_t length)
{
  while (length)
    {
      ssize_t n = pth_write (fd, buffer, length);
      if (n < 0)
         {
           logit (LOG_ERR, "connection %d: write error: %s",
                  fd, strerror (errno));
           return -1; /* write error */
         }
      length -= n;
      buffer = (const char*)buffer + n;
    }
  return 0;  /* Okay */
}


/* Send an error response back.  Returns 0 on success. */
static int
send_error (int fd, int errcode)
{
  unsigned char buf[2];

  buf[0] = errcode;
  buf[1] = 0;
  return writen (fd, buf, 2 );
}

/* Send a pong response back.  Returns 0 on success or another value
   on write error.  */
static int
send_pong (int fd)
{
  return writen (fd, "\x00\x04pong", 6);
}

/* Send a nonce of size LENGTH back. Return 0 on success. */
static int
send_nonce (int fd, int length)
{
  unsigned char buf[2+255];
  int rc;

  assert (length >= 0 && length <= 255);
  buf[0] = 0;
  buf[1] = length;
  gcry_create_nonce (buf+2, length);
  rc = writen (fd, buf, 2+length );
  wipememory (buf+2, length);
  return rc;
}

/* Send a random of size LENGTH with quality LEVEL back. Return 0 on
   success. */
static int
send_random (int fd, int length, int level)
{
  unsigned char buf[2+255];
  int rc;

  assert (length >= 0 && length <= 255);
  assert (level == GCRY_STRONG_RANDOM || level == GCRY_VERY_STRONG_RANDOM);
  buf[0] = 0;
  buf[1] = length;
  /* Note that we don't bother putting the random stuff into secure
     memory because this daemon is anyway intended to be run under
     root and it is questionable whether the kernel buffers etc. are
     equally well protected. */
  gcry_randomize (buf+2, length, level);
  rc = writen (fd, buf, 2+length );
  wipememory (buf+2, length);
  return rc;
}

/* Main processing loop for a connection.

   A request is made up of:

    1 byte  Total length of request; must be 3
    1 byte  Command
            0   = Ping
            10  = GetNonce
            11  = GetStrongRandom
            12  = GetVeryStrongRandom
            (all other values are reserved)
    1 byte  Number of requested bytes.
            This is ignored for command Ping.

   A response is made up of:

    1 byte  Error Code
            0    = Everything is fine
            1    = Bad Command
            0xff = Other error.
            (For a bad request the connection will simply be closed)
    1 byte  Length of data
    n byte  data

   The requests are read as long as the connection is open.


 */
static void
connection_loop (int fd)
{
  unsigned char request[3];
  unsigned char *p;
  int nleft, n;
  int rc;

  for (;;)
    {
      for (nleft=3, p=request; nleft > 0; )
        {
          n = pth_read (fd, p, nleft);
          if (!n && p == request)
            return; /* Client terminated connection. */
          if (n <= 0)
            {
              logit (LOG_ERR, "connection %d: read error: %s",
                     fd, n? strerror (errno) : "Unexpected EOF");
              return;
            }
          p += n;
          nleft -= n;
        }
      if (request[0] != 3)
        {
          logit (LOG_ERR, "connection %d: invalid length (%d) of request",
                 fd, request[0]);
          return;
        }

      switch (request[1])
        {
        case 0: /* Ping */
          rc = send_pong (fd);
          break;
        case 10: /* GetNonce */
          rc = send_nonce (fd, request[2]);
          break;
        case 11: /* GetStrongRandom */
          rc = send_random (fd, request[2], GCRY_STRONG_RANDOM);
          break;
        case 12: /* GetVeryStrongRandom */
          rc = send_random (fd, request[2], GCRY_VERY_STRONG_RANDOM);
          break;

        default: /* Invalid command */
          rc = send_error (fd, 1);
          break;
        }
      if (rc)
        break; /* A write error occured while sending the response. */
    }
}



/* Entry point for a connection's thread. */
static void *
connection_thread (void *arg)
{
  int fd = (int)arg;

  active_connections++;
  logit (LOG_INFO, "connection handler for fd %d started", fd);

  connection_loop (fd);

  close (fd);
  logit (LOG_INFO, "connection handler for fd %d terminated", fd);
  active_connections--;
  
  return NULL;
}


/* This signal handler is called from the main loop between acepting
   connections.  It is called on the regular stack, thus no special
   caution needs to be taken.  It returns true to indicate that the
   process should terminate. */
static int
handle_signal (int signo)
{
  switch (signo)
    {
    case SIGHUP:
      logit (LOG_NOTICE, "SIGHUP received - re-reading configuration");
      break;
      
    case SIGUSR1:
      logit (LOG_NOTICE, "SIGUSR1 received - no action defined");
      break;
      
    case SIGUSR2:
      logit (LOG_NOTICE, "SIGUSR2 received - no action defined");
      break;

    case SIGTERM:
      if (!shutdown_pending)
        logit (LOG_NOTICE, "SIGTERM received - shutting down ...");
      else
        logit (LOG_NOTICE, "SIGTERM received - still %d active connections",
               active_connections);
      shutdown_pending++;
      if (shutdown_pending > 2)
        {
          logit (LOG_NOTICE, "shutdown forced");
          return 1;
	}
      break;
        
    case SIGINT:
      logit (LOG_NOTICE, "SIGINT received - immediate shutdown");
      return 1;

    default:
      logit (LOG_NOTICE, "signal %d received - no action defined\n", signo);
    }
  return 0;
}



/* Main server loop.  This is called with the FD of the listening
   socket. */
static void
serve (int listen_fd)
{
  pth_attr_t tattr;
  pth_event_t ev;
  sigset_t sigs;
  int signo;
  struct sockaddr_un paddr;
  socklen_t plen = sizeof (paddr);
  int fd;

  tattr = pth_attr_new();
  pth_attr_set (tattr, PTH_ATTR_JOINABLE, 0);
  pth_attr_set (tattr, PTH_ATTR_STACK_SIZE, 256*1024);
  pth_attr_set (tattr, PTH_ATTR_NAME, "connection");

  sigemptyset (&sigs);
  sigaddset (&sigs, SIGHUP);
  sigaddset (&sigs, SIGUSR1);
  sigaddset (&sigs, SIGUSR2);
  sigaddset (&sigs, SIGINT);
  sigaddset (&sigs, SIGTERM);
  ev = pth_event (PTH_EVENT_SIGS, &sigs, &signo);

  for (;;)
    {
      if (shutdown_pending)
        {
          if (!active_connections)
            break; /* Ready. */

          /* Do not accept anymore connections but wait for existing
             connections to terminate.  */
          signo = 0;
          pth_wait (ev);
          if (pth_event_occurred (ev) && signo)
            if (handle_signal (signo))
              break; /* Stop the loop. */
          continue;
	}

      gcry_fast_random_poll ();
      fd = pth_accept_ev (listen_fd, (struct sockaddr *)&paddr, &plen, ev);
      if (fd == -1)
        {
          if (pth_event_occurred (ev))
            {
              if (handle_signal (signo))
                break; /* Stop the loop. */
              continue;
	    }
          logit (LOG_WARNING, "accept failed: %s - waiting 1s\n",
                 strerror (errno));
          gcry_fast_random_poll ();
          pth_sleep (1);
          continue;
	}

      if (!pth_spawn (tattr, connection_thread, (void*)fd))
        {
          logit (LOG_ERR, "error spawning connection handler: %s\n",
                 strerror (errno) );
          close (fd);
	}
    }
  
  pth_event_free (ev, PTH_FREE_ALL);
}

