/* global.c  -	global control functions
 * Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003
 *               2004, 2005, 2006, 2008  Free Software Foundation, Inc.
 *
 * This file is part of Libgcrypt.
 *
 * Libgcrypt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
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
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include <limits.h>
#include <errno.h>
#include <unistd.h>
#ifdef HAVE_SYSLOG
# include <syslog.h>
#endif /*HAVE_SYSLOG*/

#include "g10lib.h"
#include "cipher.h"
#include "stdmem.h" /* our own memory allocator */
#include "secmem.h" /* our own secmem allocator */
#include "ath.h"



/****************
 * flag bits: 0 : general cipher debug
 *	      1 : general MPI debug
 */
static unsigned int debug_flags;

/* gcry_control (GCRYCTL_SET_FIPS_MODE), sets this flag so that the
   intialization code swicthed fips mode on.  */
static int force_fips_mode;

/* Controlled by global_init().  */
static int any_init_done;



/* Memory management. */

static gcry_handler_alloc_t alloc_func;
static gcry_handler_alloc_t alloc_secure_func;
static gcry_handler_secure_check_t is_secure_func;
static gcry_handler_realloc_t realloc_func;
static gcry_handler_free_t free_func;
static gcry_handler_no_mem_t outofcore_handler;
static void *outofcore_handler_value;
static int no_secure_memory;





/* This is our handmade constructor.  It gets called by any function
   likely to be called at startup.  The suggested way for an
   application to make sure that this has been called is by using
   gcry_check_version. */
static void
global_init (void)
{
  gcry_error_t err = 0;

  if (any_init_done)
    return;
  any_init_done = 1;

  /* Initialize our portable thread/mutex wrapper.  */
  err = ath_init ();
  if (err)
    goto fail;
  
  /* See whether the system is in FIPS mode.  This needs to come as
     early as possible put after the ATH has been initialized.  */
  _gcry_initialize_fips_mode (force_fips_mode);

  /* Before we do any other initialization we need to test available
     hardware features.  */
  _gcry_detect_hw_features ();

  err = _gcry_cipher_init ();
  if (err)
    goto fail;
  err = _gcry_md_init ();
  if (err)
    goto fail;
  err = _gcry_pk_init ();
  if (err)
    goto fail;
#if 0
  /* Hmmm, as of now ac_init does nothing. */
  if ( !fips_mode () )
    {
      err = _gcry_ac_init ();
      if (err)
        goto fail;
    }
#endif

  return;

 fail:
  BUG ();
}


/* This function is called by the macro fips_is_operational and makes
   sure that the minimal initialization has been done.  This is far
   from a perfect solution and hides problems with an improper
   initialization but at least in single-threaded mode it should work
   reliable. 

   The reason we need this is that a lot of applications don't use
   Libgcrypt properly by not running any initialization code at all.
   They just call a Libgcrypt function and that is all what they want.
   Now with the FIPS mode, that has the side effect of entering FIPS
   mode (for security reasons, FIPS mode is the default if no
   initialization has been done) and bailing out immediately because
   the FSM is in the wrong state.  If we always run the init code,
   Libgcrypt can test for FIPS mode and at least if not in FIPS mode,
   it will behave as before.  Note that this on-the-fly initialization
   is only done for the cryptographic functions subject to FIPS mode
   and thus not all API calls will do such an initialization.  */
int
_gcry_global_is_operational (void)
{
  if (!any_init_done)
    {
#ifdef HAVE_SYSLOG
      syslog (LOG_USER|LOG_WARNING, "Libgcrypt warning: "
              "missing initialization - please fix the application");
#endif /*HAVE_SYSLOG*/
      global_init ();
    }
  return _gcry_fips_is_operational ();
}




/* Version number parsing.  */

/* This function parses the first portion of the version number S and
   stores it in *NUMBER.  On sucess, this function returns a pointer
   into S starting with the first character, which is not part of the
   initial number portion; on failure, NULL is returned.  */
static const char*
parse_version_number( const char *s, int *number )
{
    int val = 0;

    if( *s == '0' && isdigit(s[1]) )
	return NULL; /* leading zeros are not allowed */
    for ( ; isdigit(*s); s++ ) {
	val *= 10;
	val += *s - '0';
    }
    *number = val;
    return val < 0? NULL : s;
}

/* This function breaks up the complete string-representation of the
   version number S, which is of the following struture: <major
   number>.<minor number>.<micro number><patch level>.  The major,
   minor and micro number components will be stored in *MAJOR, *MINOR
   and *MICRO.

   On success, the last component, the patch level, will be returned;
   in failure, NULL will be returned.  */

static const char *
parse_version_string( const char *s, int *major, int *minor, int *micro )
{
    s = parse_version_number( s, major );
    if( !s || *s != '.' )
	return NULL;
    s++;
    s = parse_version_number( s, minor );
    if( !s || *s != '.' )
	return NULL;
    s++;
    s = parse_version_number( s, micro );
    if( !s )
	return NULL;
    return s; /* patchlevel */
}

/* If REQ_VERSION is non-NULL, check that the version of the library
   is at minimum the requested one.  Returns the string representation
   of the library version if the condition is satisfied; return NULL
   if the requested version is newer than that of the library.

   If a NULL is passed to this function, no check is done, but the
   string representation of the library is simply returned.  */
const char *
gcry_check_version( const char *req_version )
{
    const char *ver = VERSION;
    int my_major, my_minor, my_micro;
    int rq_major, rq_minor, rq_micro;
    const char *my_plvl, *rq_plvl;

    /* Initialize library.  */
    global_init ();

    if ( !req_version )
        /* Caller wants our version number.  */
	return ver;

    /* Parse own version number.  */
    my_plvl = parse_version_string( ver, &my_major, &my_minor, &my_micro );
    if ( !my_plvl )
        /* very strange our own version is bogus.  Shouldn't we use
	   assert() here and bail out in case this happens?  -mo.  */
	return NULL;

  /* Parse requested version number.  */
    rq_plvl = parse_version_string( req_version, &rq_major, &rq_minor,
								&rq_micro );
    if ( !rq_plvl )
        /* req version string is invalid, this can happen.  */
	return NULL;

    /* Compare version numbers.  */
    if ( my_major > rq_major
	|| (my_major == rq_major && my_minor > rq_minor)
	|| (my_major == rq_major && my_minor == rq_minor
				 && my_micro > rq_micro)
	|| (my_major == rq_major && my_minor == rq_minor
				 && my_micro == rq_micro
				 && strcmp( my_plvl, rq_plvl ) >= 0) ) {
	return ver;
    }

    return NULL;
}


static void
print_config ( int (*fnc)(FILE *fp, const char *format, ...), FILE *fp)
{
  unsigned int hwf;
  struct {
    unsigned int flag;
    const char *desc;
  } hwflist[] = {
    { HWF_PADLOCK_RNG, "padlock-rng" },
    { HWF_PADLOCK_AES, "padlock-aes" },
    { HWF_PADLOCK_SHA, "padlock-sha" },
    { 0, NULL} 
  };
  int i;

  fnc (fp, "version:%s:\n", VERSION);
  fnc (fp, "ciphers:%s:\n", LIBGCRYPT_CIPHERS);
  fnc (fp, "pubkeys:%s:\n", LIBGCRYPT_PUBKEY_CIPHERS);
  fnc (fp, "digests:%s:\n", LIBGCRYPT_DIGESTS);
  fnc (fp, "rnd-mod:"
#if USE_RNDEGD
                "egd:"
#endif
#if USE_RNDLINUX
                "linux:"
#endif
#if USE_RNDUNIX
                "unix:"
#endif
#if USE_RNDW32
                "w32:"
#endif
       "\n");
  fnc (fp, "mpi-asm:%s:\n", _gcry_mpi_get_hw_config ());
  hwf = _gcry_get_hw_features ();
  fnc (fp, "hwflist:");
  for (i=0; hwflist[i].desc; i++)
  if ( (hwf & hwflist[i].flag) )
    fnc (fp, "%s:", hwflist[i].desc);
  fnc (fp, "\n");
  /* We use y/n instead of 1/0 for the simple reason that Emacsen's
     compile error parser would accidently flag that line when printed
     during "make check" as an error.  */
  fnc (fp, "fips-mode:%c:%c:\n", 
       fips_mode ()? 'y':'n',
       _gcry_enforced_fips_mode ()? 'y':'n' );
}




/* Command dispatcher function, acting as general control
   function.  */
gcry_error_t
_gcry_vcontrol (enum gcry_ctl_cmds cmd, va_list arg_ptr)
{
  static int init_finished = 0;
  gcry_err_code_t err = 0;
  
  switch (cmd)
    {
    case GCRYCTL_ENABLE_M_GUARD:
      _gcry_private_enable_m_guard ();
      break;

    case GCRYCTL_ENABLE_QUICK_RANDOM:
      _gcry_enable_quick_random_gen ();
      break;

    case GCRYCTL_FAKED_RANDOM_P:
      /* Return an error if the RNG is faked one (e.g. enabled by
         ENABLE_QUICK_RANDOM. */
      if (_gcry_random_is_faked ())
        err = GPG_ERR_GENERAL;  /* Use as TRUE value.  */
      break;

    case GCRYCTL_DUMP_RANDOM_STATS:
      _gcry_random_dump_stats ();
      break;

    case GCRYCTL_DUMP_MEMORY_STATS:
      /*m_print_stats("[fixme: prefix]");*/
      break;

    case GCRYCTL_DUMP_SECMEM_STATS:
      _gcry_secmem_dump_stats ();
      break;

    case GCRYCTL_DROP_PRIVS:
      global_init ();
      _gcry_secmem_init (0);
      break;

    case GCRYCTL_DISABLE_SECMEM:
      global_init ();
      no_secure_memory = 1;
      break;    

    case GCRYCTL_INIT_SECMEM:
      global_init ();
      _gcry_secmem_init (va_arg (arg_ptr, unsigned int));
      if ((_gcry_secmem_get_flags () & GCRY_SECMEM_FLAG_NOT_LOCKED))
        err = GPG_ERR_GENERAL;
      break;

    case GCRYCTL_TERM_SECMEM:
      global_init ();
      _gcry_secmem_term ();
      break;

    case GCRYCTL_DISABLE_SECMEM_WARN:
      _gcry_secmem_set_flags ((_gcry_secmem_get_flags ()
			       | GCRY_SECMEM_FLAG_NO_WARNING));
      break;

    case GCRYCTL_SUSPEND_SECMEM_WARN:
      _gcry_secmem_set_flags ((_gcry_secmem_get_flags ()
			       | GCRY_SECMEM_FLAG_SUSPEND_WARNING));
      break;

    case GCRYCTL_RESUME_SECMEM_WARN:
      _gcry_secmem_set_flags ((_gcry_secmem_get_flags ()
			       & ~GCRY_SECMEM_FLAG_SUSPEND_WARNING));
      break;

    case GCRYCTL_USE_SECURE_RNDPOOL:
      global_init ();
      _gcry_secure_random_alloc (); /* Put random number into secure memory. */
      break;

    case GCRYCTL_SET_RANDOM_SEED_FILE:
      _gcry_set_random_seed_file (va_arg (arg_ptr, const char *));
      break;

    case GCRYCTL_UPDATE_RANDOM_SEED_FILE:
      if ( fips_is_operational () )
        _gcry_update_random_seed_file ();
      break;

    case GCRYCTL_SET_VERBOSITY:
      _gcry_set_log_verbosity (va_arg (arg_ptr, int));
      break;

    case GCRYCTL_SET_DEBUG_FLAGS:
      debug_flags |= va_arg (arg_ptr, unsigned int);
      break;

    case GCRYCTL_CLEAR_DEBUG_FLAGS:
      debug_flags &= ~va_arg (arg_ptr, unsigned int);
      break;

    case GCRYCTL_DISABLE_INTERNAL_LOCKING:
      /* Not used anymore.  */
      global_init ();
      break;

    case GCRYCTL_ANY_INITIALIZATION_P:
      if (any_init_done)
	err = GPG_ERR_GENERAL;
      break;

    case GCRYCTL_INITIALIZATION_FINISHED_P:
      if (init_finished)
	err = GPG_ERR_GENERAL; /* Yes.  */
      break;

    case GCRYCTL_INITIALIZATION_FINISHED:
      /* This is a hook which should be used by an application after
	 all initialization has been done and right before any threads
	 are started.  It is not really needed but the only way to be
	 really sure that all initialization for thread-safety has
	 been done. */
      if (! init_finished)
        {
          global_init ();
          /* Do only a basic random initialization, i.e. init the
             mutexes. */
          _gcry_random_initialize (0);
          init_finished = 1;
          /* Force us into operational state if in FIPS mode.  */
          (void)fips_is_operational ();
        }
      break;

    case GCRYCTL_SET_THREAD_CBS:
      err = ath_install (va_arg (arg_ptr, void *), any_init_done);
      if (! err)
	global_init ();
      break;

    case GCRYCTL_FAST_POLL:
      /* We need to do make sure that the random pool is really
         initialized so that the poll function is not a NOP. */
      _gcry_random_initialize (1);

      if ( fips_is_operational () )
        _gcry_fast_random_poll (); 
      break;

    case GCRYCTL_SET_RNDEGD_SOCKET:
#if USE_RNDEGD
      err = _gcry_rndegd_set_socket_name (va_arg (arg_ptr, const char *));
#else
      err = gpg_error (GPG_ERR_NOT_SUPPORTED);
#endif
      break;

    case GCRYCTL_SET_RANDOM_DAEMON_SOCKET:
      _gcry_set_random_daemon_socket (va_arg (arg_ptr, const char *));
      break;

    case GCRYCTL_USE_RANDOM_DAEMON:
      /* We need to do make sure that the random pool is really
         initialized so that the poll function is not a NOP. */
      _gcry_random_initialize (1);
      _gcry_use_random_daemon (!! va_arg (arg_ptr, int));
      break;
      
      /* This command dumps information pertaining to the
         configuration of libgcrypt to the given stream.  It may be
         used before the intialization has been finished but not
         before a gcry_version_check. */
    case GCRYCTL_PRINT_CONFIG:
      {
        FILE *fp = va_arg (arg_ptr, FILE *);
        print_config (fp?fprintf:_gcry_log_info_with_dummy_fp, fp);
      }
      break;

    case GCRYCTL_OPERATIONAL_P:
      /* Returns true if the library is in an operational state.  This
         is always true for non-fips mode.  */
      if (_gcry_fips_test_operational ())
        err = GPG_ERR_GENERAL; /* Used as TRUE value */
      break;

    case GCRYCTL_FIPS_MODE_P:
      if (fips_mode () 
          && !_gcry_is_fips_mode_inactive () 
          && !no_secure_memory)
	err = GPG_ERR_GENERAL; /* Used as TRUE value */
      break;

    case GCRYCTL_FORCE_FIPS_MODE:
      /* Performing this command puts the library into fips mode.  If
         the library has already been initialized into fips mode, a
         selftest is triggered.  it is not possible to put the libraty
         into fips mode after having passed the initialization. */
      if (!any_init_done)
        {
          /* Not yet intialized at all.  Set a flag so that we are put
             into fips mode during initialization.  */
          force_fips_mode = 1;
        }
      else 
        {
          /* Already initialized.  If we are already operational we
             run a selftest.  If not we use the is_operational call to
             force us into operational state if possible.  */
          if (_gcry_fips_test_error_or_operational ())
            _gcry_fips_run_selftests (1);
          if (_gcry_fips_is_operational ())
            err = GPG_ERR_GENERAL; /* Used as TRUE value */
      }
      break;

    case GCRYCTL_SELFTEST:
      /* Run a selftest.  This works in fips mode as well as in
         standard mode.  In contrast to the power-up tests, we use an
         extended version of the selftests. Returns 0 on success or an
         error code. */
      global_init ();
      err = _gcry_fips_run_selftests (1);
      break;

    case 58:  /* Init external random test.  */
      {
        void **rctx        = va_arg (arg_ptr, void **);
        unsigned int flags = va_arg (arg_ptr, unsigned int);
        const void *key    = va_arg (arg_ptr, const void *);
        size_t keylen      = va_arg (arg_ptr, size_t);
        const void *seed   = va_arg (arg_ptr, const void *);
        size_t seedlen     = va_arg (arg_ptr, size_t);
        const void *dt     = va_arg (arg_ptr, const void *);
        size_t dtlen       = va_arg (arg_ptr, size_t);
        if (!fips_is_operational ())
          err = fips_not_operational ();
        else
          err = _gcry_random_init_external_test (rctx, flags, key, keylen,
                                                 seed, seedlen, dt, dtlen);
      }
      break;
    case 59:  /* Run external random test.  */
      {
        void *ctx     = va_arg (arg_ptr, void *);
        void *buffer  = va_arg (arg_ptr, void *);
        size_t buflen = va_arg (arg_ptr, size_t);
        if (!fips_is_operational ())
          err = fips_not_operational ();
        else
          err = _gcry_random_run_external_test (ctx, buffer, buflen);
      }
      break;
    case 60:  /* Deinit external random test.  */
      {
        void *ctx = va_arg (arg_ptr, void *);
        _gcry_random_deinit_external_test (ctx);
      }
      break;


    default:
      err = GPG_ERR_INV_OP;
    }

  return gcry_error (err);
}


/* Command dispatcher function, acting as general control
   function.  */
gcry_error_t
gcry_control (enum gcry_ctl_cmds cmd, ...)
{
  gcry_error_t err;
  va_list arg_ptr;
  
  va_start (arg_ptr, cmd);
  err = _gcry_vcontrol (cmd, arg_ptr);
  va_end(arg_ptr);
  return err;
}



/* Return a pointer to a string containing a description of the error
   code in the error value ERR.  */
const char *
gcry_strerror (gcry_error_t err)
{
  return gpg_strerror (err);
}

/* Return a pointer to a string containing a description of the error
   source in the error value ERR.  */
const char *
gcry_strsource (gcry_error_t err)
{
  return gpg_strsource (err);
}

/* Retrieve the error code for the system error ERR.  This returns
   GPG_ERR_UNKNOWN_ERRNO if the system error is not mapped (report
   this).  */
gcry_err_code_t
gcry_err_code_from_errno (int err)
{
  return gpg_err_code_from_errno (err);
}


/* Retrieve the system error for the error code CODE.  This returns 0
   if CODE is not a system error code.  */
int
gcry_err_code_to_errno (gcry_err_code_t code)
{
  return gpg_err_code_from_errno (code);
}

  
/* Return an error value with the error source SOURCE and the system
   error ERR.  */
gcry_error_t
gcry_err_make_from_errno (gpg_err_source_t source, int err)
{
  return gpg_err_make_from_errno (source, err);
}


/* Return an error value with the system error ERR.  */
gcry_err_code_t
gcry_error_from_errno (int err)
{
  return gcry_error (gpg_err_code_from_errno (err));
}


/* Set custom allocation handlers.  This is in general not useful
 * because the libgcrypt allocation functions are guaranteed to
 * provide proper allocation handlers which zeroize memory if needed.
 * NOTE: All 5 functions should be set.  */
void
gcry_set_allocation_handler (gcry_handler_alloc_t new_alloc_func,
			     gcry_handler_alloc_t new_alloc_secure_func,
			     gcry_handler_secure_check_t new_is_secure_func,
			     gcry_handler_realloc_t new_realloc_func,
			     gcry_handler_free_t new_free_func)
{
  global_init ();

  if (fips_mode ())
    {
      /* We do not want to enforce the fips mode, but merely set a
         flag so that the application may check whether it is still in
         fips mode.  */
      _gcry_inactivate_fips_mode ("custom allocation handler");
    }

  alloc_func = new_alloc_func;
  alloc_secure_func = new_alloc_secure_func;
  is_secure_func = new_is_secure_func;
  realloc_func = new_realloc_func;
  free_func = new_free_func;
}



/****************
 * Set an optional handler which is called in case the xmalloc functions
 * ran out of memory.  This handler may do one of these things:
 *   o free some memory and return true, so that the xmalloc function
 *     tries again.
 *   o Do whatever it like and return false, so that the xmalloc functions
 *     use the default fatal error handler.
 *   o Terminate the program and don't return.
 *
 * The handler function is called with 3 arguments:  The opaque value set with
 * this function, the requested memory size, and a flag with these bits
 * currently defined:
 *	bit 0 set = secure memory has been requested.
 */
void
gcry_set_outofcore_handler( int (*f)( void*, size_t, unsigned int ),
							void *value )
{
  global_init ();

  if (fips_mode () )
    {
      log_info ("out of core handler ignored in FIPS mode\n");
      return;
    }
  
  outofcore_handler = f;
  outofcore_handler_value = value;
}

/* Return the no_secure_memory flag.  */
static int
get_no_secure_memory (void)
{
  if (!no_secure_memory)
    return 0;
  if (_gcry_enforced_fips_mode ())
    {
      no_secure_memory = 0;
      return 0;
    }
  return no_secure_memory;
}


static gcry_err_code_t
do_malloc (size_t n, unsigned int flags, void **mem)
{
  gcry_err_code_t err = 0;
  void *m;

  if ((flags & GCRY_ALLOC_FLAG_SECURE) && !get_no_secure_memory ())
    {
      if (alloc_secure_func)
	m = (*alloc_secure_func) (n);
      else
	m = _gcry_private_malloc_secure (n);
    }
  else
    {
      if (alloc_func)
	m = (*alloc_func) (n);
      else
	m = _gcry_private_malloc (n);
    }

  if (!m)
    {
      /* Make sure that ERRNO has been set in case a user supplied
         memory handler didn't it correctly. */
      if (!errno)
        errno = ENOMEM;
      err = gpg_err_code_from_errno (errno);
    }
  else
    *mem = m;

  return err;
}
  
void *
gcry_malloc (size_t n)
{
  void *mem = NULL;

  do_malloc (n, 0, &mem);

  return mem;
}

void *
gcry_malloc_secure (size_t n)
{
  void *mem = NULL;

  do_malloc (n, GCRY_ALLOC_FLAG_SECURE, &mem);

  return mem;
}

int
gcry_is_secure (const void *a)
{
  if (get_no_secure_memory ())
    return 0;
  if (is_secure_func)
    return is_secure_func (a) ;
  return _gcry_private_is_secure (a);
}

void
_gcry_check_heap( const void *a )
{
  (void)a;
  
    /* FIXME: implement this*/
#if 0
    if( some_handler )
	some_handler(a)
    else
	_gcry_private_check_heap(a)
#endif
}

void *
gcry_realloc (void *a, size_t n)
{
  void *p;

  if (realloc_func)
    p = realloc_func (a, n);
  else
    p =  _gcry_private_realloc (a, n);
  if (!p && !errno)
    errno = ENOMEM;
  return p;
}

void
gcry_free (void *p)
{
  int save_errno;

  if (!p)
    return;

  /* In case ERRNO is set we better save it so that the free machinery
     may not accidently change ERRNO.  We restore it only if it was
     already set to comply with the usual C semantic for ERRNO.  */
  save_errno = errno;
  if (free_func)
    free_func (p);
  else
    _gcry_private_free (p);

  if (save_errno)
    errno = save_errno;
}

void *
gcry_calloc (size_t n, size_t m)
{
  size_t bytes;
  void *p;

  bytes = n * m; /* size_t is unsigned so the behavior on overflow is
                    defined. */
  if (m && bytes / m != n) 
    {
      errno = ENOMEM;
      return NULL;
    }

  p = gcry_malloc (bytes);
  if (p)
    memset (p, 0, bytes);
  return p;
}

void *
gcry_calloc_secure (size_t n, size_t m)
{
  size_t bytes;
  void *p;

  bytes = n * m; /* size_t is unsigned so the behavior on overflow is
                    defined. */
  if (m && bytes / m != n) 
    {
      errno = ENOMEM;
      return NULL;
    }
  
  p = gcry_malloc_secure (bytes);
  if (p)
    memset (p, 0, bytes);
  return p;
}


/* Create and return a copy of the null-terminated string STRING.  If
   it is contained in secure memory, the copy will be contained in
   secure memory as well.  In an out-of-memory condition, NULL is
   returned.  */
char *
gcry_strdup (const char *string)
{
  char *string_cp = NULL;
  size_t string_n = 0;

  string_n = strlen (string);

  if (gcry_is_secure (string))
    string_cp = gcry_malloc_secure (string_n + 1);
  else
    string_cp = gcry_malloc (string_n + 1);
  
  if (string_cp)
    strcpy (string_cp, string);

  return string_cp;
}


void *
gcry_xmalloc( size_t n )
{
  void *p;
  
  while ( !(p = gcry_malloc( n )) ) 
    {
      if ( fips_mode () 
           || !outofcore_handler
           || !outofcore_handler (outofcore_handler_value, n, 0) )
        {
          _gcry_fatal_error (gpg_err_code_from_errno (errno), NULL);
        }
    }
    return p;
}

void *
gcry_xrealloc( void *a, size_t n )
{
  void *p;
  
  while ( !(p = gcry_realloc( a, n )) )
    {
      if ( fips_mode ()
           || !outofcore_handler
           || !outofcore_handler (outofcore_handler_value, n,
                                   gcry_is_secure(a)? 3:2 ) )
        {
          _gcry_fatal_error (gpg_err_code_from_errno (errno), NULL );
	}
    }
    return p;
}

void *
gcry_xmalloc_secure( size_t n )
{
  void *p;
  
  while ( !(p = gcry_malloc_secure( n )) ) 
    {
      if ( fips_mode ()
           || !outofcore_handler
           || !outofcore_handler (outofcore_handler_value, n, 1) )
        {
          _gcry_fatal_error (gpg_err_code_from_errno (errno),
                             _("out of core in secure memory"));
	}
    }
  return p;
}


void *
gcry_xcalloc( size_t n, size_t m )
{
  size_t nbytes;
  void *p;

  nbytes = n * m; 
  if (m && nbytes / m != n) 
    {
      errno = ENOMEM;
      _gcry_fatal_error(gpg_err_code_from_errno (errno), NULL );
    }

  p = gcry_xmalloc ( nbytes );
  memset ( p, 0, nbytes );
  return p;
}

void *
gcry_xcalloc_secure( size_t n, size_t m )
{
  size_t nbytes;
  void *p;

  nbytes = n * m; 
  if (m && nbytes / m != n) 
    {
      errno = ENOMEM;
      _gcry_fatal_error(gpg_err_code_from_errno (errno), NULL );
    }

  p = gcry_xmalloc_secure ( nbytes );
  memset ( p, 0, nbytes );
  return p;
}

char *
gcry_xstrdup (const char *string)
{
  char *p;
  
  while ( !(p = gcry_strdup (string)) ) 
    {
      size_t n = strlen (string);
      int is_sec = !!gcry_is_secure (string);
      
      if (fips_mode ()
          || !outofcore_handler
          || !outofcore_handler (outofcore_handler_value, n, is_sec) ) 
        {
          _gcry_fatal_error (gpg_err_code_from_errno (errno),
                             is_sec? _("out of core in secure memory"):NULL);
	}
    }

  return p;
}


int
_gcry_get_debug_flag (unsigned int mask)
{
  if ( fips_mode () )
    return 0;
  return (debug_flags & mask);
}



/* It is often useful to get some feedback of long running operations.
   This function may be used to register a handler for this. 
   The callback function CB is used as:

   void cb (void *opaque, const char *what, int printchar,
           int current, int total);

   Where WHAT is a string identifying the the type of the progress
   output, PRINTCHAR the character usually printed, CURRENT the amount
   of progress currently done and TOTAL the expected amount of
   progress.  A value of 0 for TOTAL indicates that there is no
   estimation available.

   Defined values for WHAT:

   "need_entropy"  X    0  number-of-bytes-required
            When running low on entropy
   "primegen"      '\n'  0 0
           Prime generated
                   '!'
           Need to refresh the prime pool
                   '<','>'
           Number of bits adjusted
                   '^'
           Looking for a generator
                   '.'
           Fermat tests on 10 candidates failed
                  ':'
           Restart with a new random value
                  '+'
           Rabin Miller test passed          
   "pk_elg"        '+','-','.','\n'   0  0
            Only used in debugging mode.
   "pk_dsa"       
            Only used in debugging mode.
*/
void
gcry_set_progress_handler (void (*cb)(void *,const char*,int, int, int),
                           void *cb_data)
{
#if USE_DSA
  _gcry_register_pk_dsa_progress (cb, cb_data);
#endif
#if USE_ELGAMAL
  _gcry_register_pk_elg_progress (cb, cb_data);
#endif
  _gcry_register_primegen_progress (cb, cb_data);
  _gcry_register_random_progress (cb, cb_data);
}
