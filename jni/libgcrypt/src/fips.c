/* fips.c - FIPS mode management
 * Copyright (C) 2008  Free Software Foundation, Inc.
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
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#ifdef ENABLE_HMAC_BINARY_CHECK
# include <dlfcn.h> 
#endif
#ifdef HAVE_SYSLOG
# include <syslog.h>
#endif /*HAVE_SYSLOG*/

#include "g10lib.h"
#include "ath.h"
#include "cipher-proto.h"
#include "hmac256.h"


/* The name of the file used to foce libgcrypt into fips mode. */
#define FIPS_FORCE_FILE "/etc/gcrypt/fips_enabled"


/* The states of the finite state machine used in fips mode.  */
enum module_states 
  {
    /* POWEROFF cannot be represented.  */
    STATE_POWERON  = 0,
    STATE_INIT,
    STATE_SELFTEST,
    STATE_OPERATIONAL,
    STATE_ERROR,
    STATE_FATALERROR,
    STATE_SHUTDOWN
  };


/* Flag telling whether we are in fips mode.  It uses inverse logic so
   that fips mode is the default unless changed by the intialization
   code. To check whether fips mode is enabled, use the function
   fips_mode()! */
static int no_fips_mode_required;

/* Flag to indicate that we are in the enforced FIPS mode.  */
static int enforced_fips_mode;

/* If this flag is set, the application may no longer assume that the
   process is running in FIPS mode.  This flag is protected by the
   FSM_LOCK.  */
static int inactive_fips_mode;

/* This is the lock we use to protect the FSM.  */
static ath_mutex_t fsm_lock = ATH_MUTEX_INITIALIZER;

/* The current state of the FSM.  The whole state machinery is only
   used while in fips mode. Change this only while holding fsm_lock. */
static enum module_states current_state;





static void fips_new_state (enum module_states new_state);



/* Convert lowercase hex digits; assumes valid hex digits. */
#define loxtoi_1(p)   (*(p) <= '9'? (*(p)- '0'): (*(p)-'a'+10))
#define loxtoi_2(p)   ((loxtoi_1(p) * 16) + loxtoi_1((p)+1))

/* Returns true if P points to a lowercase hex digit. */
#define loxdigit_p(p) !!strchr ("01234567890abcdef", *(p))



/* Check whether the OS is in FIPS mode and record that in a module
   local variable.  If FORCE is passed as true, fips mode will be
   enabled anyway. Note: This function is not thread-safe and should
   be called before any threads are created.  This function may only
   be called once.  */
void
_gcry_initialize_fips_mode (int force)
{
  static int done;
  gpg_error_t err;
      
  /* Make sure we are not accidently called twice.  */
  if (done)
    {
      if ( fips_mode () )
        {
          fips_new_state (STATE_FATALERROR);
          fips_noreturn ();
        }
      /* If not in fips mode an assert is sufficient.  */
      gcry_assert (!done);
    }
  done = 1;

  /* If the calling application explicitly requested fipsmode, do so.  */
  if (force)
    {
      gcry_assert (!no_fips_mode_required);
      goto leave;
    }

  /* For testing the system it is useful to override the system
     provided detection of the FIPS mode and force FIPS mode using a
     file.  The filename is hardwired so that there won't be any
     confusion on whether /etc/gcrypt/ or /usr/local/etc/gcrypt/ is
     actually used.  The file itself may be empty.  */
  if ( !access (FIPS_FORCE_FILE, F_OK) )
    {
      gcry_assert (!no_fips_mode_required);
      goto leave;
    }

  /* Checking based on /proc file properties.  */
  {
    static const char procfname[] = "/proc/sys/crypto/fips_enabled";
    FILE *fp;
    int saved_errno;

    fp = fopen (procfname, "r");
    if (fp)
      {
        char line[256];
        
        if (fgets (line, sizeof line, fp) && atoi (line))
          {
            /* System is in fips mode.  */
            fclose (fp);
            gcry_assert (!no_fips_mode_required);
            goto leave;
          }
        fclose (fp);
      }
    else if ((saved_errno = errno) != ENOENT
             && !access ("/proc/version", F_OK) )
      {
        /* Problem reading the fips file despite that we have the proc
           file system.  We better stop right away. */
        log_info ("FATAL: error reading `%s' in libgcrypt: %s\n",
                  procfname, strerror (saved_errno));
#ifdef HAVE_SYSLOG
        syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
                "reading `%s' failed: %s - abort",
                procfname, strerror (saved_errno));
#endif /*HAVE_SYSLOG*/
        abort ();
      }
  }
  
  /* Fips not not requested, set flag.  */
  no_fips_mode_required = 1;

 leave:
  if (!no_fips_mode_required)
    {
      /* Yes, we are in FIPS mode.  */
      FILE *fp;

      /* Intitialize the lock to protect the FSM.  */
      err = ath_mutex_init (&fsm_lock);
      if (err)
        {
          /* If that fails we can't do anything but abort the
             process. We need to use log_info so that the FSM won't
             get involved.  */
          log_info ("FATAL: failed to create the FSM lock in libgcrypt: %s\n",
                     strerror (err));
#ifdef HAVE_SYSLOG
          syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
                  "creating FSM lock failed: %s - abort",
                  strerror (err));
#endif /*HAVE_SYSLOG*/
          abort ();
        }

      
      /* If the FIPS force files exists, is readable and has a number
         != 0 on its first line, we enable the enforced fips mode.  */
      fp = fopen (FIPS_FORCE_FILE, "r");
      if (fp)
        {
          char line[256];
          
          if (fgets (line, sizeof line, fp) && atoi (line))
            enforced_fips_mode = 1;
          fclose (fp);
        }

      /* Now get us into the INIT state.  */
      fips_new_state (STATE_INIT);
      
    }
  return;
}

static void
lock_fsm (void)
{
  gpg_error_t err;

  err = ath_mutex_lock (&fsm_lock);
  if (err)
    {
      log_info ("FATAL: failed to acquire the FSM lock in libgrypt: %s\n", 
                strerror (err));
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
              "acquiring FSM lock failed: %s - abort",
              strerror (err));
#endif /*HAVE_SYSLOG*/
      abort ();
    }
}

static void
unlock_fsm (void)
{
  gpg_error_t err;

  err = ath_mutex_unlock (&fsm_lock);
  if (err)
    {
      log_info ("FATAL: failed to release the FSM lock in libgrypt: %s\n",
                strerror (err));
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
              "releasing FSM lock failed: %s - abort",
              strerror (err));
#endif /*HAVE_SYSLOG*/
      abort ();
    }
}


/* This function returns true if fips mode is enabled.  This is
   independent of the fips required finite state machine and only used
   to enable fips specific code.  Please use the fips_mode macro
   instead of calling this function directly. */
int
_gcry_fips_mode (void)
{
  /* No locking is required because we have the requirement that this
     variable is only initialized once with no other threads
     existing.  */
  return !no_fips_mode_required;
}


/* Return a flag telling whether we are in the enforced fips mode.  */
int 
_gcry_enforced_fips_mode (void)
{
  return enforced_fips_mode;
}


/* If we do not want to enforce the fips mode, we can set a flag so
   that the application may check whether it is still in fips mode.
   TEXT will be printed as part of a syslog message.  This function
   may only be be called if in fips mode. */
void
_gcry_inactivate_fips_mode (const char *text)
{
  gcry_assert (_gcry_fips_mode ());

  if (_gcry_enforced_fips_mode () )
    {
      /* Get us into the error state. */
      fips_signal_error (text);
      return;
    }

  lock_fsm ();
  if (!inactive_fips_mode)
    {
      inactive_fips_mode = 1;
      unlock_fsm ();
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_WARNING, "Libgcrypt warning: "
              "%s - FIPS mode inactivated", text);
#endif /*HAVE_SYSLOG*/
    }
  else
    unlock_fsm ();
}


/* Return the FIPS mode inactive flag.  If it is true the FIPS mode is
   not anymore active.  */
int
_gcry_is_fips_mode_inactive (void)
{
  int flag;

  if (!_gcry_fips_mode ())
    return 0;
  lock_fsm ();
  flag = inactive_fips_mode;
  unlock_fsm ();
  return flag;
}



static const char *
state2str (enum module_states state)
{
  const char *s;

  switch (state)
    {
    case STATE_POWERON:     s = "Power-On"; break;
    case STATE_INIT:        s = "Init"; break;
    case STATE_SELFTEST:    s = "Self-Test"; break;
    case STATE_OPERATIONAL: s = "Operational"; break;
    case STATE_ERROR:       s = "Error"; break;
    case STATE_FATALERROR:  s = "Fatal-Error"; break;
    case STATE_SHUTDOWN:    s = "Shutdown"; break;
    default:                s = "?"; break;
    }
  return s;
}


/* Return true if the library is in the operational state.  */
int 
_gcry_fips_is_operational (void)
{
  int result;

  if (!fips_mode ())
    result = 1;
  else
    {
      lock_fsm ();
      if (current_state == STATE_INIT)
        {
          /* If we are still in the INIT state, we need to run the
             selftests so that the FSM can eventually get into
             operational state.  Given that we would need a 2-phase
             initialization of libgcrypt, but that has traditionally
             not been enforced, we use this on demand self-test
             checking.  Note that Proper applications would do the
             application specific libgcrypt initialization between a
             gcry_check_version() and gcry_control
             (GCRYCTL_INITIALIZATION_FINISHED) where the latter will
             run the selftests.  The drawback of these on-demand
             self-tests are a small chance that self-tests are
             performed by severeal threads; that is no problem because
             our FSM make sure that we won't oversee any error. */
          unlock_fsm ();
          _gcry_fips_run_selftests (0);
          lock_fsm ();
        }

      result = (current_state == STATE_OPERATIONAL);
      unlock_fsm ();
    }
  return result;
}


/* This is test on wether the library is in the operational state.  In
   contrast to _gcry_fips_is_operational this function won't do a
   state transition on the fly.  */
int
_gcry_fips_test_operational (void)
{
  int result;

  if (!fips_mode ())
    result = 1;
  else
    {
      lock_fsm ();
      result = (current_state == STATE_OPERATIONAL);
      unlock_fsm ();
    }
  return result;
}


/* This is a test on whether the library is in the error or
   operational state. */
int
_gcry_fips_test_error_or_operational (void)
{
  int result;

  if (!fips_mode ())
    result = 1;
  else
    {
      lock_fsm ();
      result = (current_state == STATE_OPERATIONAL
                || current_state == STATE_ERROR);
      unlock_fsm ();
    }
  return result;
}


static void
reporter (const char *domain, int algo, const char *what, const char *errtxt)
{
  if (!errtxt && !_gcry_log_verbosity (2))
    return;

  log_info ("libgcrypt selftest: %s %s%s (%d): %s%s%s%s\n",
            !strcmp (domain, "hmac")? "digest":domain,
            !strcmp (domain, "hmac")? "HMAC-":"",
            !strcmp (domain, "cipher")? _gcry_cipher_algo_name (algo) :
            !strcmp (domain, "digest")? _gcry_md_algo_name (algo) :
            !strcmp (domain, "hmac")?   _gcry_md_algo_name (algo) :
            !strcmp (domain, "pubkey")? _gcry_pk_algo_name (algo) : "",
            algo, errtxt? errtxt:"Okay", 
            what?" (":"", what? what:"", what?")":""); 
}

/* Run self-tests for all required cipher algorithms.  Return 0 on
   success. */
static int
run_cipher_selftests (int extended)
{
  static int algos[] = 
    {
      GCRY_CIPHER_3DES,
      GCRY_CIPHER_AES128,
      GCRY_CIPHER_AES192,
      GCRY_CIPHER_AES256,
      0
    };
  int idx;
  gpg_error_t err;
  int anyerr = 0;

  for (idx=0; algos[idx]; idx++)
    {
      err = _gcry_cipher_selftest (algos[idx], extended, reporter);
      reporter ("cipher", algos[idx], NULL,
                err? gpg_strerror (err):NULL);
      if (err)
        anyerr = 1;
    }
  return anyerr;
}


/* Run self-tests for all required hash algorithms.  Return 0 on
   success. */
static int
run_digest_selftests (int extended)
{
  static int algos[] = 
    {
      GCRY_MD_SHA1,
      GCRY_MD_SHA224,
      GCRY_MD_SHA256,
      GCRY_MD_SHA384,
      GCRY_MD_SHA512,
      0
    };
  int idx;
  gpg_error_t err;
  int anyerr = 0;

  for (idx=0; algos[idx]; idx++)
    {
      err = _gcry_md_selftest (algos[idx], extended, reporter);
      reporter ("digest", algos[idx], NULL,
                err? gpg_strerror (err):NULL);
      if (err)
        anyerr = 1;
    }
  return anyerr;
}


/* Run self-tests for all HMAC algorithms.  Return 0 on success. */
static int
run_hmac_selftests (int extended)
{
  static int algos[] = 
    {
      GCRY_MD_SHA1,
      GCRY_MD_SHA224,
      GCRY_MD_SHA256,
      GCRY_MD_SHA384,
      GCRY_MD_SHA512,
      0
    };
  int idx;
  gpg_error_t err;
  int anyerr = 0;

  for (idx=0; algos[idx]; idx++)
    {
      err = _gcry_hmac_selftest (algos[idx], extended, reporter);
      reporter ("hmac", algos[idx], NULL,
                err? gpg_strerror (err):NULL);
      if (err)
        anyerr = 1;
    }
  return anyerr;
}


/* Run self-tests for all required public key algorithms.  Return 0 on
   success. */
static int
run_pubkey_selftests (int extended)
{
  static int algos[] = 
    {
      GCRY_PK_RSA,
      GCRY_PK_DSA,
      /* GCRY_PK_ECDSA is not enabled in fips mode.  */
      0
    };
  int idx;
  gpg_error_t err;
  int anyerr = 0;

  for (idx=0; algos[idx]; idx++)
    {
      err = _gcry_pk_selftest (algos[idx], extended, reporter);
      reporter ("pubkey", algos[idx], NULL,
                err? gpg_strerror (err):NULL);
      if (err)
        anyerr = 1;
    }
  return anyerr;
}


/* Run self-tests for the random number generator.  Returns 0 on
   success. */
static int
run_random_selftests (void)
{
  gpg_error_t err;

  err = _gcry_random_selftest (reporter);
  reporter ("random", 0, NULL, err? gpg_strerror (err):NULL);
  
  return !!err;
}

/* Run an integrity check on the binary.  Returns 0 on success.  */
static int
check_binary_integrity (void)
{
#ifdef ENABLE_HMAC_BINARY_CHECK
  gpg_error_t err;
  Dl_info info;
  unsigned char digest[32];
  int dlen;
  char *fname = NULL;
  const char key[] = "What am I, a doctor or a moonshuttle conductor?";
  
  if (!dladdr ("gcry_check_version", &info))
    err = gpg_error_from_syserror ();
  else
    {
      dlen = _gcry_hmac256_file (digest, sizeof digest, info.dli_fname,
                                 key, strlen (key));
      if (dlen < 0)
        err = gpg_error_from_syserror ();
      else if (dlen != 32)
        err = gpg_error (GPG_ERR_INTERNAL);
      else
        {
          fname = gcry_malloc (strlen (info.dli_fname) + 1 + 5 + 1 );
          if (!fname)
            err = gpg_error_from_syserror ();
          else
            {
              FILE *fp;
              char *p;

              /* Prefix the basename with a dot.  */
              strcpy (fname, info.dli_fname);
              p = strrchr (fname, '/');
              if (p)
                p++;
              else
                p = fname;
              memmove (p+1, p, strlen (p)+1);
              *p = '.';
              strcat (fname, ".hmac");

              /* Open the file.  */
              fp = fopen (fname, "r");
              if (!fp)
                err = gpg_error_from_syserror ();
              else
                {
                  /* A buffer of 64 bytes plus one for a LF and one to
                     detect garbage.  */
                  unsigned char buffer[64+1+1]; 
                  const unsigned char *s;
                  int n;

                  /* The HMAC files consists of lowercase hex digits
                     only with an optional trailing linefeed.  Fail if
                     there is any garbage.  */
                  err = gpg_error (GPG_ERR_SELFTEST_FAILED);
                  n = fread (buffer, 1, sizeof buffer, fp);
                  if (n == 64 || (n == 65 && buffer[64] == '\n'))
                    {
                      buffer[64] = 0;
                      for (n=0, s= buffer;
                           n < 32 && loxdigit_p (s) && loxdigit_p (s+1);
                           n++, s += 2)
                        buffer[n] = loxtoi_2 (s);
                      if ( n == 32 && !memcmp (digest, buffer, 32) )
                        err = 0;
                    }
                  fclose (fp);
                }
            }
        }
    }
  reporter ("binary", 0, fname, err? gpg_strerror (err):NULL);
#ifdef HAVE_SYSLOG
  if (err)
    syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
            "integrity check using `%s' failed: %s",
            fname? fname:"[?]", gpg_strerror (err));
#endif /*HAVE_SYSLOG*/
  gcry_free (fname);
  return !!err;
#else
  return 0;
#endif
}


/* Run the self-tests.  If EXTENDED is true, extended versions of the
   selftest are run, that is more tests than required by FIPS.  */
gpg_err_code_t
_gcry_fips_run_selftests (int extended)
{
  enum module_states result = STATE_ERROR;
  gcry_err_code_t ec = GPG_ERR_SELFTEST_FAILED;
  
  if (fips_mode ())
    fips_new_state (STATE_SELFTEST);

  if (run_cipher_selftests (extended))
    goto leave;

  if (run_digest_selftests (extended))
    goto leave;

  if (run_hmac_selftests (extended))
    goto leave;

  /* Run random tests before the pubkey tests because the latter
     require random.  */
  if (run_random_selftests ())
    goto leave;

  if (run_pubkey_selftests (extended))
    goto leave;

  /* Now check the integrity of the binary.  We do this this after
     having checked the HMAC code.  */
  if (check_binary_integrity ())
    goto leave;

  /* All selftests passed.  */
  result = STATE_OPERATIONAL;
  ec = 0;

 leave:
  if (fips_mode ())
    fips_new_state (result);

  return ec;
}


/* This function is used to tell the FSM about errors in the library.
   The FSM will be put into an error state.  This function should not
   be called directly but by one of the macros

     fips_signal_error (description)
     fips_signal_fatal_error (description)

   where DESCRIPTION is a string describing the error. */
void
_gcry_fips_signal_error (const char *srcfile, int srcline, const char *srcfunc,
                         int is_fatal, const char *description)
{
  if (!fips_mode ())
    return;  /* Not required.  */

  /* Set new state before printing an error.  */
  fips_new_state (is_fatal? STATE_FATALERROR : STATE_ERROR);

  /* Print error.  */
  log_info ("%serror in libgcrypt, file %s, line %d%s%s: %s\n",
            is_fatal? "fatal ":"",
            srcfile, srcline, 
            srcfunc? ", function ":"", srcfunc? srcfunc:"",
            description? description : "no description available");
#ifdef HAVE_SYSLOG
  syslog (LOG_USER|LOG_ERR, "Libgcrypt error: "
          "%serror in file %s, line %d%s%s: %s",
          is_fatal? "fatal ":"",
          srcfile, srcline, 
          srcfunc? ", function ":"", srcfunc? srcfunc:"",
          description? description : "no description available");
#endif /*HAVE_SYSLOG*/
}


/* Perform a state transition to NEW_STATE.  If this is an invalid
   transition, the module will go into a fatal error state. */
static void
fips_new_state (enum module_states new_state)
{
  int ok = 0;
  enum module_states last_state;

  lock_fsm ();

  last_state = current_state;
  switch (current_state)
    {
    case STATE_POWERON:
      if (new_state == STATE_INIT
          || new_state == STATE_ERROR
          || new_state == STATE_FATALERROR)
        ok = 1;
      break;

    case STATE_INIT:
      if (new_state == STATE_SELFTEST
          || new_state == STATE_ERROR
          || new_state == STATE_FATALERROR)
        ok = 1;
      break;
      
    case STATE_SELFTEST:
      if (new_state == STATE_OPERATIONAL
          || new_state == STATE_ERROR
          || new_state == STATE_FATALERROR)
        ok = 1;
      break;
      
    case STATE_OPERATIONAL:
      if (new_state == STATE_SHUTDOWN 
          || new_state == STATE_SELFTEST
          || new_state == STATE_ERROR
          || new_state == STATE_FATALERROR)
        ok = 1;
      break;
      
    case STATE_ERROR:
      if (new_state == STATE_SHUTDOWN
          || new_state == STATE_ERROR
          || new_state == STATE_FATALERROR
          || new_state == STATE_SELFTEST)
        ok = 1;
      break;
      
    case STATE_FATALERROR:
      if (new_state == STATE_SHUTDOWN )
        ok = 1;
      break;
      
    case STATE_SHUTDOWN:
      /* We won't see any transition *from* Shutdown because the only
         allowed new state is Power-Off and that one can't be
         represented.  */
      break;
      
    }

  if (ok)
    {
      current_state = new_state;
    }

  unlock_fsm ();

  if (!ok || _gcry_log_verbosity (2))
    log_info ("libgcrypt state transition %s => %s %s\n",
              state2str (last_state), state2str (new_state),
              ok? "granted":"denied");
  
  if (!ok)
    {
      /* Invalid state transition.  Halting library. */
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_ERR, 
              "Libgcrypt error: invalid state transition %s => %s",
              state2str (last_state), state2str (new_state));
#endif /*HAVE_SYSLOG*/
      fips_noreturn ();
    }
  else if (new_state == STATE_ERROR || new_state == STATE_FATALERROR)
    {
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_WARNING, 
              "Libgcrypt notice: state transition %s => %s",
              state2str (last_state), state2str (new_state));
#endif /*HAVE_SYSLOG*/
    }
}




/* This function should be called to ensure that the execution shall
   not continue. */
void
_gcry_fips_noreturn (void)
{
#ifdef HAVE_SYSLOG
  syslog (LOG_USER|LOG_ERR, "Libgcrypt terminated the application");
#endif /*HAVE_SYSLOG*/
  fflush (NULL);
  abort ();
  /*NOTREACHED*/
}
