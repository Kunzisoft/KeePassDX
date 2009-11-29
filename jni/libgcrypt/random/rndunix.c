/****************************************************************************
 *									    *
 *									    *
 *   Unix Randomness-Gathering Code					    *
 *									    *
 *   Copyright Peter Gutmann, Paul Kendall, and Chris Wedgwood 1996-1999.   *
 *   Heavily modified for GnuPG by Werner Koch				    *
 *									    *
 *									    *
 ****************************************************************************/

/* This module is part of the cryptlib continuously seeded pseudorandom
   number generator.  For usage conditions, see lib_rand.c

   [Here is the notice from lib_rand.c:]

   This module and the misc/rnd*.c modules represent the cryptlib
   continuously seeded pseudorandom number generator (CSPRNG) as described in
   my 1998 Usenix Security Symposium paper "The generation of random numbers
   for cryptographic purposes".

   The CSPRNG code is copyright Peter Gutmann (and various others) 1996,
   1997, 1998, 1999, all rights reserved.  Redistribution of the CSPRNG
   modules and use in source and binary forms, with or without modification,
   are permitted provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice
      and this permission notice in its entirety.

   2. Redistributions in binary form must reproduce the copyright notice in
      the documentation and/or other materials provided with the distribution.

   3. A copy of any bugfixes or enhancements made must be provided to the
      author, <pgut001@cs.auckland.ac.nz> to allow them to be added to the
      baseline version of the code.

  ALTERNATIVELY, the code may be distributed under the terms of the
  GNU Lesser General Public License, version 2.1 or any later version
  published by the Free Software Foundation, in which case the
  provisions of the GNU LGPL are required INSTEAD OF the above
  restrictions.

  Although not required under the terms of the LGPL, it would still be
  nice if you could make any changes available to the author to allow
  a consistent code base to be maintained.  */
/*************************************************************************
 The above alternative was changed from GPL to LGPL on 2007-08-22 with
 permission from Peter Gutmann:
 ==========
 From: pgut001 <pgut001@cs.auckland.ac.nz>
 Subject: Re: LGPL for the windows entropy gatherer
 To: wk@gnupg.org
 Date: Wed, 22 Aug 2007 03:05:42 +1200
 
 Hi,
 
 >As of now libgcrypt is GPL under Windows due to that module and some people
 >would really like to see it under LGPL too.  Can you do such a license change
 >to LGPL version 2?  Note that LGPL give the user the option to relicense it
 >under GPL, so the change would be pretty easy and backwar compatible.
 
 Sure.  I assumed that since GPG was GPLd, you'd prefer the GPL for the entropy
 code as well, but Ian asked for LGPL as an option so as of the next release
 I'll have LGPL in there.  You can consider it to be retroactive, so your
 current version will be LGPLd as well.
 
 Peter.
 ==========
 From: pgut001 <pgut001@cs.auckland.ac.nz>
 Subject: Re: LGPL for the windows entropy gatherer
 To: wk@gnupg.org
 Date: Wed, 22 Aug 2007 20:50:08 +1200
 
 >Would you mind to extend this also to the Unix entropy gatherer which is
 >still used on systems without /dev/random and when EGD is not installed? That
 >would be the last GPLed piece in Libgcrypt.
 
 Sure, it covers the entire entropy-gathering subsystem.
 
 Peter.
 =========
*/

/* General includes */

#include <config.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

/* OS-specific includes */

#ifdef __osf__
  /* Somewhere in the morass of system-specific cruft which OSF/1 pulls in
   * via the following includes are various endianness defines, so we
   * undefine the cryptlib ones, which aren't really needed for this module
   * anyway */
#undef BIG_ENDIAN
#undef LITTLE_ENDIAN
#endif				/* __osf__ */

#include <unistd.h>
#include <fcntl.h>
#include <pwd.h>
#ifndef __QNX__
#include <sys/errno.h>
#include <sys/ipc.h>
#endif				/* __QNX__ */
#include <sys/time.h>		/* SCO and SunOS need this before resource.h */
#ifndef __QNX__
#include <sys/resource.h>
#endif				/* __QNX__ */
#if defined( _AIX ) || defined( __QNX__ )
#include <sys/select.h>
#endif				/* _AIX */
#ifndef __QNX__
#include <sys/shm.h>
#include <signal.h>
#include <sys/signal.h>
#endif				/* __QNX__ */
#include <sys/stat.h>
#include <sys/types.h>		/* Verschiedene komische Typen */
#if defined( __hpux ) && ( OS_VERSION == 9 )
#include <vfork.h>
#endif				/* __hpux 9.x, after that it's in unistd.h */
#include <sys/wait.h>
/* #include <kitchensink.h> */
#ifdef __QNX__
#include <signal.h>
#include <process.h>
#endif		      /* __QNX__ */
#include <errno.h>

#include "types.h"  /* for byte and u32 typedefs */
#include "g10lib.h"
#include "rand-internal.h"

#ifndef EAGAIN
#define EAGAIN	EWOULDBLOCK
#endif
#ifndef STDIN_FILENO
#define STDIN_FILENO 0
#endif
#ifndef STDOUT_FILENO
#define STDOUT_FILENO 1
#endif

#define GATHER_BUFSIZE		49152	/* Usually about 25K are filled */

/* The structure containing information on random-data sources.  Each
 * record contains the source and a relative estimate of its usefulness
 * (weighting) which is used to scale the number of kB of output from the
 * source (total = data_bytes / usefulness).  Usually the weighting is in the
 * range 1-3 (or 0 for especially useless sources), resulting in a usefulness
 * rating of 1...3 for each kB of source output (or 0 for the useless
 * sources).
 *
 * If the source is constantly changing (certain types of network statistics
 * have this characteristic) but the amount of output is small, the weighting
 * is given as a negative value to indicate that the output should be treated
 * as if a minimum of 1K of output had been obtained.  If the source produces
 * a lot of output then the scale factor is fractional, resulting in a
 * usefulness rating of < 1 for each kB of source output.
 *
 * In order to provide enough randomness to satisfy the requirements for a
 * slow poll, we need to accumulate at least 20 points of usefulness (a
 * typical system should get about 30 points).
 *
 * Some potential options are missed out because of special considerations.
 * pstat -i and pstat -f can produce amazing amounts of output (the record
 * is 600K on an Oracle server) which floods the buffer and doesn't yield
 * anything useful (apart from perhaps increasing the entropy of the vmstat
 * output a bit), so we don't bother with this.  pstat in general produces
 * quite a bit of output, but it doesn't change much over time, so it gets
 * very low weightings.  netstat -s produces constantly-changing output but
 * also produces quite a bit of it, so it only gets a weighting of 2 rather
 * than 3.  The same holds for netstat -in, which gets 1 rather than 2.
 *
 * Some binaries are stored in different locations on different systems so
 * alternative paths are given for them.  The code sorts out which one to
 * run by itself, once it finds an exectable somewhere it moves on to the
 * next source.  The sources are arranged roughly in their order of
 * usefulness, occasionally sources which provide a tiny amount of
 * relatively useless data are placed ahead of ones which provide a large
 * amount of possibly useful data because another 100 bytes can't hurt, and
 * it means the buffer won't be swamped by one or two high-output sources.
 * All the high-output sources are clustered towards the end of the list
 * for this reason.  Some binaries are checked for in a certain order, for
 * example under Slowaris /usr/ucb/ps understands aux as an arg, but the
 * others don't.  Some systems have conditional defines enabling alternatives
 * to commands which don't understand the usual options but will provide
 * enough output (in the form of error messages) to look like they're the
 * real thing, causing alternative options to be skipped (we can't check the
 * return either because some commands return peculiar, non-zero status even
 * when they're working correctly).
 *
 * In order to maximise use of the buffer, the code performs a form of run-
 * length compression on its input where a repeated sequence of bytes is
 * replaced by the occurrence count mod 256.  Some commands output an awful
 * lot of whitespace, this measure greatly increases the amount of data we
 * can fit in the buffer.
 *
 * When we scale the weighting using the SC() macro, some preprocessors may
 * give a division by zero warning for the most obvious expression
 * 'weight ? 1024 / weight : 0' (and gcc 2.7.2.2 dies with a division by zero
 * trap), so we define a value SC_0 which evaluates to zero when fed to
 * '1024 / SC_0' */

#define SC( weight )	( 1024 / weight )	/* Scale factor */
#define SC_0			16384	/* SC( SC_0 ) evalutes to 0 */

static struct RI {
    const char *path;		/* Path to check for existence of source */
    const char *arg;		/* Args for source */
    const int usefulness;	/* Usefulness of source */
    FILE *pipe; 		/* Pipe to source as FILE * */
    int pipeFD; 		/* Pipe to source as FD */
    pid_t pid;			/* pid of child for waitpid() */
    int length; 		/* Quantity of output produced */
    const int hasAlternative;	    /* Whether source has alt.location */
} dataSources[] = {

    {	"/bin/vmstat", "-s", SC(-3), NULL, 0, 0, 0, 1    },
    {	"/usr/bin/vmstat", "-s", SC(-3), NULL, 0, 0, 0, 0},
    {	"/bin/vmstat", "-c", SC(-3), NULL, 0, 0, 0, 1     },
    {	"/usr/bin/vmstat", "-c", SC(-3), NULL, 0, 0, 0, 0},
    {	"/usr/bin/pfstat", NULL, SC(-2), NULL, 0, 0, 0, 0},
    {	"/bin/vmstat", "-i", SC(-2), NULL, 0, 0, 0, 1     },
    {	"/usr/bin/vmstat", "-i", SC(-2), NULL, 0, 0, 0, 0},
    {	"/usr/ucb/netstat", "-s", SC(2), NULL, 0, 0, 0, 1 },
    {	"/usr/bin/netstat", "-s", SC(2), NULL, 0, 0, 0, 1 },
    {	"/usr/sbin/netstat", "-s", SC(2), NULL, 0, 0, 0, 1},
    {	"/usr/etc/netstat", "-s", SC(2), NULL, 0, 0, 0, 0},
    {	"/usr/bin/nfsstat", NULL, SC(2), NULL, 0, 0, 0, 0},
    {	"/usr/ucb/netstat", "-m", SC(-1), NULL, 0, 0, 0, 1  },
    {	"/usr/bin/netstat", "-m", SC(-1), NULL, 0, 0, 0, 1  },
    {	"/usr/sbin/netstat", "-m", SC(-1), NULL, 0, 0, 0, 1 },
    {	"/usr/etc/netstat", "-m", SC(-1), NULL, 0, 0, 0, 0 },
    {	"/bin/netstat",     "-in", SC(-1), NULL, 0, 0, 0, 1 },
    {	"/usr/ucb/netstat", "-in", SC(-1), NULL, 0, 0, 0, 1 },
    {	"/usr/bin/netstat", "-in", SC(-1), NULL, 0, 0, 0, 1 },
    {	"/usr/sbin/netstat", "-in", SC(-1), NULL, 0, 0, 0, 1},
    {	"/usr/etc/netstat", "-in", SC(-1), NULL, 0, 0, 0, 0},
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.7.1.0",
				    SC(-1), NULL, 0, 0, 0, 0 }, /* UDP in */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.7.4.0",
				    SC(-1), NULL, 0, 0, 0, 0 },  /* UDP out */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.4.3.0",
				    SC(-1), NULL, 0, 0, 0, 0 }, /* IP ? */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.6.10.0",
				    SC(-1), NULL, 0, 0, 0, 0 }, /* TCP ? */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.6.11.0",
				    SC(-1), NULL, 0, 0, 0, 0 }, /* TCP ? */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.6.13.0",
				    SC(-1), NULL, 0, 0, 0, 0 }, /* TCP ? */
    {	"/usr/bin/mpstat", NULL, SC(1), NULL, 0, 0, 0, 0     },
    {	"/usr/bin/w", NULL, SC(1), NULL, 0, 0, 0, 1           },
    {	"/usr/bsd/w", NULL, SC(1), NULL, 0, 0, 0, 0          },
    {	"/usr/bin/df", NULL, SC(1), NULL, 0, 0, 0, 1          },
    {	"/bin/df", NULL, SC(1), NULL, 0, 0, 0, 0             },
    {	"/usr/sbin/portstat", NULL, SC(1), NULL, 0, 0, 0, 0  },
    {	"/usr/bin/iostat", NULL, SC(SC_0), NULL, 0, 0, 0, 0  },
    {	"/usr/bin/uptime", NULL, SC(SC_0), NULL, 0, 0, 0, 1   },
    {	"/usr/bsd/uptime", NULL, SC(SC_0), NULL, 0, 0, 0, 0  },
    {	"/bin/vmstat", "-f", SC(SC_0), NULL, 0, 0, 0, 1       },
    {	"/usr/bin/vmstat", "-f", SC(SC_0), NULL, 0, 0, 0, 0  },
    {	"/bin/vmstat", NULL, SC(SC_0), NULL, 0, 0, 0, 1       },
    {	"/usr/bin/vmstat", NULL, SC(SC_0), NULL, 0, 0, 0, 0  },
    {	"/usr/ucb/netstat", "-n", SC(0.5), NULL, 0, 0, 0, 1   },
    {	"/usr/bin/netstat", "-n", SC(0.5), NULL, 0, 0, 0, 1   },
    {	"/usr/sbin/netstat", "-n", SC(0.5), NULL, 0, 0, 0, 1  },
    {	"/usr/etc/netstat", "-n", SC(0.5), NULL, 0, 0, 0, 0  },
#if defined( __sgi ) || defined( __hpux )
    {	"/bin/ps", "-el", SC(0.3), NULL, 0, 0, 0, 1           },
#endif				/* __sgi || __hpux */
    {	"/usr/ucb/ps", "aux", SC(0.3), NULL, 0, 0, 0, 1       },
    {	"/usr/bin/ps", "aux", SC(0.3), NULL, 0, 0, 0, 1       },
    {	"/bin/ps", "aux", SC(0.3), NULL, 0, 0, 0, 0          },
    {   "/bin/ps", "-A", SC(0.3), NULL, 0, 0, 0, 0           }, /*QNX*/
    {	"/usr/bin/ipcs", "-a", SC(0.5), NULL, 0, 0, 0, 1      },
    {	"/bin/ipcs", "-a", SC(0.5), NULL, 0, 0, 0, 0         },
    /* Unreliable source, depends on system usage */
    {	"/etc/pstat", "-p", SC(0.5), NULL, 0, 0, 0, 1         },
    {	"/bin/pstat", "-p", SC(0.5), NULL, 0, 0, 0, 0        },
    {	"/etc/pstat", "-S", SC(0.2), NULL, 0, 0, 0, 1         },
    {	"/bin/pstat", "-S", SC(0.2), NULL, 0, 0, 0, 0        },
    {	"/etc/pstat", "-v", SC(0.2), NULL, 0, 0, 0, 1         },
    {	"/bin/pstat", "-v", SC(0.2), NULL, 0, 0, 0, 0        },
    {	"/etc/pstat", "-x", SC(0.2), NULL, 0, 0, 0, 1         },
    {	"/bin/pstat", "-x", SC(0.2), NULL, 0, 0, 0, 0        },
    {	"/etc/pstat", "-t", SC(0.1), NULL, 0, 0, 0, 1         },
    {	"/bin/pstat", "-t", SC(0.1), NULL, 0, 0, 0, 0        },
    /* pstat is your friend */
    {	"/usr/bin/last", "-n 50", SC(0.3), NULL, 0, 0, 0, 1   },
#ifdef __sgi
    {	"/usr/bsd/last", "-50", SC(0.3), NULL, 0, 0, 0, 0    },
#endif				/* __sgi */
#ifdef __hpux
    {	"/etc/last", "-50", SC(0.3), NULL, 0, 0, 0, 0        },
#endif				/* __hpux */
    {	"/usr/bsd/last", "-n 50", SC(0.3), NULL, 0, 0, 0, 0  },
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.5.1.0",
				SC(0.1), NULL, 0, 0, 0, 0 }, /* ICMP ? */
    {	"/usr/sbin/snmp_request", "localhost public get 1.3.6.1.2.1.5.3.0",
				SC(0.1), NULL, 0, 0, 0, 0 }, /* ICMP ? */
    {	"/etc/arp", "-a", SC(0.1), NULL, 0, 0, 0, 1  },
    {	"/usr/etc/arp", "-a", SC(0.1), NULL, 0, 0, 0, 1  },
    {	"/usr/bin/arp", "-a", SC(0.1), NULL, 0, 0, 0, 1  },
    {	"/usr/sbin/arp", "-a", SC(0.1), NULL, 0, 0, 0, 0 },
    {	"/usr/sbin/ripquery", "-nw 1 127.0.0.1",
				SC(0.1), NULL, 0, 0, 0, 0 },
    {	"/bin/lpstat", "-t", SC(0.1), NULL, 0, 0, 0, 1     },
    {	"/usr/bin/lpstat", "-t", SC(0.1), NULL, 0, 0, 0, 1 },
    {	"/usr/ucb/lpstat", "-t", SC(0.1), NULL, 0, 0, 0, 0 },
    {	"/usr/bin/tcpdump", "-c 5 -efvvx", SC(1), NULL, 0, 0, 0, 0 },
    /* This is very environment-dependant.  If network traffic is low, it'll
     * probably time out before delivering 5 packets, which is OK because
     * it'll probably be fixed stuff like ARP anyway */
    {	"/usr/sbin/advfsstat", "-b usr_domain",
				SC(SC_0), NULL, 0, 0, 0, 0},
    {	"/usr/sbin/advfsstat", "-l 2 usr_domain",
				SC(0.5), NULL, 0, 0, 0, 0},
    {	"/usr/sbin/advfsstat", "-p usr_domain",
				SC(SC_0), NULL, 0, 0, 0, 0},
    /* This is a complex and screwball program.  Some systems have things
     * like rX_dmn, x = integer, for RAID systems, but the statistics are
     * pretty dodgy */
#ifdef __QNXNTO__                                                             
    { "/bin/pidin", "-F%A%B%c%d%E%I%J%K%m%M%n%N%p%P%S%s%T", SC(0.3),
             NULL, 0, 0, 0, 0       },
#endif     
#if 0
    /* The following aren't enabled since they're somewhat slow and not very
     * unpredictable, however they give an indication of the sort of sources
     * you can use (for example the finger might be more useful on a
     * firewalled internal network) */
    {	"/usr/bin/finger", "@ml.media.mit.edu", SC(0.9), NULL, 0, 0, 0, 0 },
    {	"/usr/local/bin/wget", "-O - http://lavarand.sgi.com/block.html",
				SC(0.9), NULL, 0, 0, 0, 0 },
    {	"/bin/cat", "/usr/spool/mqueue/syslog", SC(0.9), NULL, 0, 0, 0, 0 },
#endif				/* 0 */
    {	NULL, NULL, 0, NULL, 0, 0, 0, 0 }
};

static byte *gather_buffer;	    /* buffer for gathering random noise */
static int gather_buffer_size;	    /* size of the memory buffer */
static uid_t gatherer_uid;

/* The message structure used to communicate with the parent */
typedef struct {
    int  usefulness;	/* usefulness of data */
    int  ndata; 	/* valid bytes in data */
    char data[500];	/* gathered data */
} GATHER_MSG;

#ifndef HAVE_WAITPID
static pid_t
waitpid(pid_t pid, int *statptr, int options)
{
#ifdef HAVE_WAIT4
	return wait4(pid, statptr, options, NULL);
#else
	/* If wait4 is also not available, try wait3 for SVR3 variants */
	/* Less ideal because can't actually request a specific pid */
	/* For that reason, first check to see if pid is for an */
	/*   existing process. */
	int tmp_pid, dummystat;;
	if (kill(pid, 0) == -1) {
		errno = ECHILD;
		return -1;
	}
	if (statptr == NULL)
		statptr = &dummystat;
	while (((tmp_pid = wait3(statptr, options, 0)) != pid) &&
		    (tmp_pid != -1) && (tmp_pid != 0) && (pid != -1))
	    ;
	return tmp_pid;
#endif
}
#endif

/* Under SunOS popen() doesn't record the pid of the child process.  When
 * pclose() is called, instead of calling waitpid() for the correct child, it
 * calls wait() repeatedly until the right child is reaped.  The problem is
 * that this reaps any other children that happen to have died at that
 * moment, and when their pclose() comes along, the process hangs forever.
 * The fix is to use a wrapper for popen()/pclose() which saves the pid in
 * the dataSources structure (code adapted from GNU-libc's popen() call).
 *
 * Aut viam inveniam aut faciam */

static FILE *
my_popen(struct RI *entry)
{
    int pipedes[2];
    FILE *stream;

    /* Create the pipe */
    if (pipe(pipedes) < 0)
	return (NULL);

    /* Fork off the child ("vfork() is like an OS orgasm.  All OS's want to
     * do it, but most just end up faking it" - Chris Wedgwood).  If your OS
     * supports it, you should try to use vfork() here because it's somewhat
     * more efficient */
#if defined( sun ) || defined( __ultrix__ ) || defined( __osf__ ) || \
	defined(__hpux)
    entry->pid = vfork();
#else				/*  */
    entry->pid = fork();
#endif				/* Unixen which have vfork() */
    if (entry->pid == (pid_t) - 1) {
	/* The fork failed */
	close(pipedes[0]);
	close(pipedes[1]);
	return (NULL);
    }

    if (entry->pid == (pid_t) 0) {
	struct passwd *passwd;

	/* We are the child.  Make the read side of the pipe be stdout */
	if (dup2(pipedes[STDOUT_FILENO], STDOUT_FILENO) < 0)
	    exit(127);

	/* Now that everything is set up, give up our permissions to make
	 * sure we don't read anything sensitive.  If the getpwnam() fails,
	 * we default to -1, which is usually nobody */
	if (gatherer_uid == (uid_t)-1 && \
	    (passwd = getpwnam("nobody")) != NULL)
	    gatherer_uid = passwd->pw_uid;

	setuid(gatherer_uid);

	/* Close the pipe descriptors */
	close(pipedes[STDIN_FILENO]);
	close(pipedes[STDOUT_FILENO]);

	/* Try and exec the program */
	execl(entry->path, entry->path, entry->arg, NULL);

	/* Die if the exec failed */
	exit(127);
    }

    /* We are the parent.  Close the irrelevant side of the pipe and open
     * the relevant side as a new stream.  Mark our side of the pipe to
     * close on exec, so new children won't see it */
    close(pipedes[STDOUT_FILENO]);

#ifdef FD_CLOEXEC
    fcntl(pipedes[STDIN_FILENO], F_SETFD, FD_CLOEXEC);
#endif

    stream = fdopen(pipedes[STDIN_FILENO], "r");

    if (stream == NULL) {
	int savedErrno = errno;

	/* The stream couldn't be opened or the child structure couldn't be
	 * allocated.  Kill the child and close the other side of the pipe */
	kill(entry->pid, SIGKILL);
	if (stream == NULL)
	    close(pipedes[STDOUT_FILENO]);
	else
	    fclose(stream);

	waitpid(entry->pid, NULL, 0);

	entry->pid = 0;
	errno = savedErrno;
	return (NULL);
    }

    return (stream);
}

static int
my_pclose(struct RI *entry)
{
    int status = 0;

    if (fclose(entry->pipe))
	return (-1);

    /* We ignore the return value from the process because some
       programs return funny values which would result in the input
       being discarded even if they executed successfully.  This isn't
       a problem because the result data size threshold will filter
       out any programs which exit with a usage message without
       producing useful output.  */
    if (waitpid(entry->pid, NULL, 0) != entry->pid)
	status = -1;

    entry->pipe = NULL;
    entry->pid = 0;
    return (status);
}


/* Unix slow poll (without special support for Linux)
 *
 * If a few of the randomness sources create a large amount of output then
 * the slowPoll() stops once the buffer has been filled (but before all the
 * randomness sources have been sucked dry) so that the 'usefulness' factor
 * remains below the threshold.  For this reason the gatherer buffer has to
 * be fairly sizeable on moderately loaded systems.  This is something of a
 * bug since the usefulness should be influenced by the amount of output as
 * well as the source type */


static int
slow_poll(FILE *dbgfp, int dbgall, size_t *nbytes )
{
    int moreSources;
    struct timeval tv;
    fd_set fds;
#if defined( __hpux )
    size_t maxFD = 0;
#else
    int maxFD = 0;
#endif /* OS-specific brokenness */
    int bufPos, i, usefulness = 0;


    /* Fire up each randomness source */
    FD_ZERO(&fds);
    for (i = 0; dataSources[i].path != NULL; i++) {
	/* Since popen() is a fairly heavy function, we check to see whether
	 * the executable exists before we try to run it */
	if (access(dataSources[i].path, X_OK)) {
	    if( dbgfp && dbgall )
		fprintf(dbgfp, "%s not present%s\n", dataSources[i].path,
			       dataSources[i].hasAlternative ?
					", has alternatives" : "");
	    dataSources[i].pipe = NULL;
	}
	else
	    dataSources[i].pipe = my_popen(&dataSources[i]);

	if (dataSources[i].pipe != NULL) {
	    dataSources[i].pipeFD = fileno(dataSources[i].pipe);
	    if (dataSources[i].pipeFD > maxFD)
		maxFD = dataSources[i].pipeFD;

#ifdef O_NONBLOCK /* Ohhh what a hack (used for Atari) */
	    fcntl(dataSources[i].pipeFD, F_SETFL, O_NONBLOCK);
#else
#error O_NONBLOCK is missing
#endif

	    FD_SET(dataSources[i].pipeFD, &fds);
	    dataSources[i].length = 0;

	    /* If there are alternatives for this command, don't try and
	     * execute them */
	    while (dataSources[i].hasAlternative) {
		if( dbgfp && dbgall )
		    fprintf(dbgfp, "Skipping %s\n", dataSources[i + 1].path);
		i++;
	    }
	}
    }


    /* Suck all the data we can get from each of the sources */
    bufPos = 0;
    moreSources = 1;
    while (moreSources && bufPos <= gather_buffer_size) {
	/* Wait for data to become available from any of the sources, with a
	 * timeout of 10 seconds.  This adds even more randomness since data
	 * becomes available in a nondeterministic fashion.  Kudos to HP's QA
	 * department for managing to ship a select() which breaks its own
	 * prototype */
	tv.tv_sec = 10;
	tv.tv_usec = 0;

#if defined( __hpux ) && ( OS_VERSION == 9 )
	if (select(maxFD + 1, (int *)&fds, NULL, NULL, &tv) == -1)
#else  /*  */
	if (select(maxFD + 1, &fds, NULL, NULL, &tv) == -1)
#endif /* __hpux */
	    break;

	/* One of the sources has data available, read it into the buffer */
	for (i = 0; dataSources[i].path != NULL; i++) {
	    if( dataSources[i].pipe && FD_ISSET(dataSources[i].pipeFD, &fds)) {
		size_t noBytes;

		if ((noBytes = fread(gather_buffer + bufPos, 1,
				     gather_buffer_size - bufPos,
				     dataSources[i].pipe)) == 0) {
		    if (my_pclose(&dataSources[i]) == 0) {
			int total = 0;

			/* Try and estimate how much entropy we're getting
			 * from a data source */
			if (dataSources[i].usefulness) {
			    if (dataSources[i].usefulness < 0)
				total = (dataSources[i].length + 999)
					/ -dataSources[i].usefulness;
			    else
				total = dataSources[i].length
					/ dataSources[i].usefulness;
			}
			if( dbgfp )
			    fprintf(dbgfp,
			       "%s %s contributed %d bytes, "
			       "usefulness = %d\n", dataSources[i].path,
			       (dataSources[i].arg != NULL) ?
				       dataSources[i].arg : "",
				      dataSources[i].length, total);
			if( dataSources[i].length )
			    usefulness += total;
		    }
		    dataSources[i].pipe = NULL;
		}
		else {
		    int currPos = bufPos;
		    int endPos = bufPos + noBytes;

		    /* Run-length compress the input byte sequence */
		    while (currPos < endPos) {
			int ch = gather_buffer[currPos];

			/* If it's a single byte, just copy it over */
			if (ch != gather_buffer[currPos + 1]) {
			    gather_buffer[bufPos++] = ch;
			    currPos++;
			}
			else {
			    int count = 0;

			    /* It's a run of repeated bytes, replace them
			     * with the byte count mod 256 */
			    while ((ch == gather_buffer[currPos])
				    && currPos < endPos) {
				count++;
				currPos++;
			    }
			    gather_buffer[bufPos++] = count;
			    noBytes -= count - 1;
			}
		    }

		    /* Remember the number of (compressed) bytes of input we
		     * obtained */
		    dataSources[i].length += noBytes;
		}
	    }
	}

	/* Check if there is more input available on any of the sources */
	moreSources = 0;
	FD_ZERO(&fds);
	for (i = 0; dataSources[i].path != NULL; i++) {
	    if (dataSources[i].pipe != NULL) {
		FD_SET(dataSources[i].pipeFD, &fds);
		moreSources = 1;
	    }
	}
    }

    if( dbgfp ) {
	fprintf(dbgfp, "Got %d bytes, usefulness = %d\n", bufPos, usefulness);
	fflush(dbgfp);
    }
    *nbytes = bufPos;
    return usefulness;
}

/****************
 * Start the gatherer process which writes messages of
 * type GATHERER_MSG to pipedes
 */
static void
start_gatherer( int pipefd )
{
    FILE *dbgfp = NULL;
    int dbgall;

    {
	const char *s = getenv("GNUPG_RNDUNIX_DBG");
	if( s ) {
	    dbgfp = (*s=='-' && !s[1])? stdout : fopen(s, "a");
	    if( !dbgfp )
		log_info("can't open debug file `%s': %s\n",
			     s, strerror(errno) );
	    else
		fprintf(dbgfp,"\nSTART RNDUNIX DEBUG pid=%d\n", (int)getpid());
	}
	dbgall = !!getenv("GNUPG_RNDUNIX_DBGALL");
    }
    /* close all files but the ones we need */
    {	int nmax, n1, n2, i;
#ifdef _SC_OPEN_MAX
	if( (nmax=sysconf( _SC_OPEN_MAX )) < 0 ) {
#ifdef _POSIX_OPEN_MAX
	    nmax = _POSIX_OPEN_MAX;
#else
	    nmax = 20; /* assume a reasonable value */
#endif
	}
#else /*!_SC_OPEN_MAX*/
	nmax = 20; /* assume a reasonable value */
#endif /*!_SC_OPEN_MAX*/
	n1 = fileno( stderr );
	n2 = dbgfp? fileno( dbgfp ) : -1;
	for(i=0; i < nmax; i++ ) {
	    if( i != n1 && i != n2 && i != pipefd )
		close(i);
	}
	errno = 0;
    }


    /* Set up the buffer.  Not ethat we use a plain standard malloc here. */
    gather_buffer_size = GATHER_BUFSIZE;
    gather_buffer = malloc( gather_buffer_size );
    if( !gather_buffer ) {
	log_error("out of core while allocating the gatherer buffer\n");
	exit(2);
    }

    /* Reset the SIGC(H)LD handler to the system default.  This is necessary
     * because if the program which cryptlib is a part of installs its own
     * SIGC(H)LD handler, it will end up reaping the cryptlib children before
     * cryptlib can.  As a result, my_pclose() will call waitpid() on a
     * process which has already been reaped by the installed handler and
     * return an error, so the read data won't be added to the randomness
     * pool.  There are two types of SIGC(H)LD naming, the SysV SIGCLD and
     * the BSD/Posix SIGCHLD, so we need to handle either possibility */
#ifdef SIGCLD
    signal(SIGCLD, SIG_DFL);
#else
    signal(SIGCHLD, SIG_DFL);
#endif

    fclose(stderr);		/* Arrghh!!  It's Stuart code!! */

    for(;;) {
	GATHER_MSG msg;
	size_t nbytes;
	const char *p;

	msg.usefulness = slow_poll( dbgfp, dbgall, &nbytes );
	p = gather_buffer;
	while( nbytes ) {
	    msg.ndata = nbytes > sizeof(msg.data)? sizeof(msg.data) : nbytes;
	    memcpy( msg.data, p, msg.ndata );
	    nbytes -= msg.ndata;
	    p += msg.ndata;

	    while( write( pipefd, &msg, sizeof(msg) ) != sizeof(msg) ) {
		if( errno == EINTR )
		    continue;
		if( errno == EAGAIN ) {
		    struct timeval tv;
		    tv.tv_sec = 0;
		    tv.tv_usec = 50000;
		    select(0, NULL, NULL, NULL, &tv);
		    continue;
		}
		if( errno == EPIPE ) /* parent has exited, so give up */
		   exit(0);

		/* we can't do very much here because stderr is closed */
		if( dbgfp )
		    fprintf(dbgfp, "gatherer can't write to pipe: %s\n",
				    strerror(errno) );
		/* we start a new poll to give the system some time */
		nbytes = 0;
		break;
	    }
	}
    }
    /* we are killed when the parent dies */
}


static int
read_a_msg( int fd, GATHER_MSG *msg )
{
    char *buffer = (char*)msg;
    size_t length = sizeof( *msg );
    int n;

    do {
	do {
	    n = read(fd, buffer, length );
	} while( n == -1 && errno == EINTR );
	if( n == -1 )
	    return -1;
	buffer += n;
	length -= n;
    } while( length );
    return 0;
}


/****************
 * Using a level of 0 should never block and better add nothing
 * to the pool.  So this is just a dummy for this gatherer.
 */
int
_gcry_rndunix_gather_random (void (*add)(const void*, size_t,
                                         enum random_origins),
                             enum random_origins origin,
                             size_t length, int level )
{
    static pid_t gatherer_pid = 0;
    static int pipedes[2];
    GATHER_MSG msg;
    size_t n;

    if( !level )
	return 0;

    if( !gatherer_pid ) {
	/* Make sure we are not setuid. */
	if ( getuid() != geteuid() )
	    BUG();
	/* time to start the gatherer process */
	if( pipe( pipedes ) ) {
	    log_error("pipe() failed: %s\n", strerror(errno));
	    return -1;
	}
	gatherer_pid = fork();
	if( gatherer_pid == -1 ) {
	    log_error("can't for gatherer process: %s\n", strerror(errno));
	    return -1;
	}
	if( !gatherer_pid ) {
	    start_gatherer( pipedes[1] );
	    /* oops, can't happen */
	    return -1;
	}
    }

    /* now read from the gatherer */
    while( length ) {
	int goodness;
	ulong subtract;

	if( read_a_msg( pipedes[0], &msg ) ) {
	    log_error("reading from gatherer pipe failed: %s\n",
							    strerror(errno));
	    return -1;
	}


	if( level > 1 ) {
	    if( msg.usefulness > 30 )
		goodness = 100;
	    else if ( msg.usefulness )
		goodness = msg.usefulness * 100 / 30;
	    else
		goodness = 0;
	}
	else if( level ) {
	    if( msg.usefulness > 15 )
		goodness = 100;
	    else if ( msg.usefulness )
		goodness = msg.usefulness * 100 / 15;
	    else
		goodness = 0;
	}
	else
	    goodness = 100; /* goodness of level 0 is always 100 % */

	n = msg.ndata;
	if( n > length )
	    n = length;
	(*add)( msg.data, n, origin );

	/* this is the trick how we cope with the goodness */
	subtract = (ulong)n * goodness / 100;
	/* subtract at least 1 byte to avoid infinite loops */
	length -= subtract ? subtract : 1;
    }

    return 0;
}
