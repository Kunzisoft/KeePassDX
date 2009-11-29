/* gpg-error.c - Determining gpg-error error codes.
   Copyright (C) 2004 g10 Code GmbH

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

#if HAVE_CONFIG_H
#include <config.h>
#endif

#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>

#ifdef HAVE_LOCALE_H
# include <locale.h>	
#endif
#ifdef ENABLE_NLS
#ifdef HAVE_W32_SYSTEM
# include "gettext.h"
#else
# include <libintl.h>
#endif
# define _(a) gettext (a)
# ifdef gettext_noop
#  define N_(a) gettext_noop (a)
# else
#  define N_(a) (a)
# endif
#else
# define _(a) (a)
# define N_(a) (a)
#endif

#include <gpg-error.h>


#if HAVE_W32_SYSTEM
/* The implementation follows below.  */
static char *get_locale_dir (void);
static void drop_locale_dir (char *locale_dir);
#else
#define get_locale_dir() LOCALEDIR
#define drop_locale_dir(dir)
#endif

static void
i18n_init (void)
{
#ifdef ENABLE_NLS
  char *locale_dir;

# ifdef HAVE_LC_MESSAGES
  setlocale (LC_TIME, "");
  setlocale (LC_MESSAGES, "");
# else
  setlocale (LC_ALL, "" );
# endif

  locale_dir = get_locale_dir ();
  if (locale_dir)
    {
      bindtextdomain (PACKAGE, locale_dir);
      drop_locale_dir (locale_dir);
    }
  textdomain (PACKAGE);
#endif
}


#ifdef HAVE_W32_SYSTEM

#include <windows.h>

static HKEY
get_root_key(const char *root)
{
  HKEY root_key;

  if( !root )
    root_key = HKEY_CURRENT_USER;
  else if( !strcmp( root, "HKEY_CLASSES_ROOT" ) )
    root_key = HKEY_CLASSES_ROOT;
  else if( !strcmp( root, "HKEY_CURRENT_USER" ) )
    root_key = HKEY_CURRENT_USER;
  else if( !strcmp( root, "HKEY_LOCAL_MACHINE" ) )
    root_key = HKEY_LOCAL_MACHINE;
  else if( !strcmp( root, "HKEY_USERS" ) )
    root_key = HKEY_USERS;
  else if( !strcmp( root, "HKEY_PERFORMANCE_DATA" ) )
    root_key = HKEY_PERFORMANCE_DATA;
  else if( !strcmp( root, "HKEY_CURRENT_CONFIG" ) )
    root_key = HKEY_CURRENT_CONFIG;
  else
    return NULL;
  return root_key;
}

/****************
 * Return a string from the Win32 Registry or NULL in case of
 * error.  Caller must release the return value.   A NULL for root
 * is an alias for HKEY_CURRENT_USER, HKEY_LOCAL_MACHINE in turn.
 * NOTE: The value is allocated with a plain malloc() - use free() and not
 * the usual xfree()!!!
 */
static char *
read_w32_registry_string( const char *root, const char *dir, const char *name )
{
  HKEY root_key, key_handle;
  DWORD n1, nbytes, type;
  char *result = NULL;

  if ( !(root_key = get_root_key(root) ) )
    return NULL;

  if( RegOpenKeyEx( root_key, dir, 0, KEY_READ, &key_handle ) )
    {
      if (root)
	return NULL; /* no need for a RegClose, so return direct */
      /* It seems to be common practise to fall back to HKLM. */
      if (RegOpenKeyEx (HKEY_LOCAL_MACHINE, dir, 0, KEY_READ, &key_handle) )
	return NULL; /* still no need for a RegClose, so return direct */
    }

  nbytes = 1;
  if( RegQueryValueEx( key_handle, name, 0, NULL, NULL, &nbytes ) ) {
    if (root)
      goto leave;
    /* Try to fallback to HKLM also vor a missing value.  */
    RegCloseKey (key_handle);
    if (RegOpenKeyEx (HKEY_LOCAL_MACHINE, dir, 0, KEY_READ, &key_handle) )
      return NULL; /* Nope.  */
    if (RegQueryValueEx( key_handle, name, 0, NULL, NULL, &nbytes))
      goto leave;
  }
  result = malloc( (n1=nbytes+1) );
  if( !result )
    goto leave;
  if( RegQueryValueEx( key_handle, name, 0, &type, result, &n1 ) ) {
    free(result); result = NULL;
    goto leave;
  }
  result[nbytes] = 0; /* make sure it is really a string  */
  if (type == REG_EXPAND_SZ && strchr (result, '%')) {
    char *tmp;

    n1 += 1000;
    tmp = malloc (n1+1);
    if (!tmp)
      goto leave;
    nbytes = ExpandEnvironmentStrings (result, tmp, n1);
    if (nbytes && nbytes > n1) {
      free (tmp);
      n1 = nbytes;
      tmp = malloc (n1 + 1);
      if (!tmp)
	goto leave;
      nbytes = ExpandEnvironmentStrings (result, tmp, n1);
      if (nbytes && nbytes > n1) {
	free (tmp); /* oops - truncated, better don't expand at all */
	goto leave;
      }
      tmp[nbytes] = 0;
      free (result);
      result = tmp;
    }
    else if (nbytes) { /* okay, reduce the length */
      tmp[nbytes] = 0;
      free (result);
      result = malloc (strlen (tmp)+1);
      if (!result)
	result = tmp;
      else {
	strcpy (result, tmp);
	free (tmp);
      }
    }
    else {  /* error - don't expand */
      free (tmp);
    }
  }

 leave:
  RegCloseKey( key_handle );
  return result;
}


#define REGKEY "Software\\GNU\\GnuPG"

static char *
get_locale_dir (void)
{
  char *instdir;
  char *p;
  char *dname;

  instdir = read_w32_registry_string ("HKEY_LOCAL_MACHINE", REGKEY,
				      "Install Directory");
  if (!instdir)
    return NULL;
  
  /* Build the key: "<instdir>/share/locale".  */
#define SLDIR "\\share\\locale"
  dname = malloc (strlen (instdir) + strlen (SLDIR) + 1);
  if (!dname)
    {
      free (instdir);
      return NULL;
    }
  p = dname;
  strcpy (p, instdir);
  p += strlen (instdir);
  strcpy (p, SLDIR);
  
  free (instdir);
  
  return dname;
}


static void
drop_locale_dir (char *locale_dir)
{
  free (locale_dir);
}

#endif	/* HAVE_W32_SYSTEM */


const char *gpg_strerror_sym (gpg_error_t err);
const char *gpg_strsource_sym (gpg_error_t err);


static int
get_err_from_number (char *str, gpg_error_t *err)
{
  unsigned long nr;
  char *tail;

  errno = 0;
  nr = strtoul (str, &tail, 0);
  if (errno)
    return 0;

  if (nr > UINT_MAX)
    return 0;

  if (*tail)
    {
      unsigned long cnr = strtoul (tail + 1, &tail, 0);
      if (errno || *tail)
	return 0;

      if (nr >= GPG_ERR_SOURCE_DIM || cnr >= GPG_ERR_CODE_DIM)
	return 0;

      nr = gpg_err_make (nr, cnr);
    }

  *err = (unsigned int) nr;
  return 1;
}


static int
get_err_from_symbol_one (char *str, gpg_error_t *err,
			 int *have_source, int *have_code)
{
  static const char src_prefix[] = "GPG_ERR_SOURCE_";
  static const char code_prefix[] = "GPG_ERR_";

  if (!strncasecmp (src_prefix, str, sizeof (src_prefix) - 1))
    {
      gpg_err_source_t src;

      if (*have_source)
	return 0;
      *have_source = 1;
      str += sizeof (src_prefix) - 1;

      for (src = 0; src < GPG_ERR_SOURCE_DIM; src++)
	{
	  const char *src_sym;

	  src_sym = gpg_strsource_sym (src << GPG_ERR_SOURCE_SHIFT);
	  if (src_sym && !strcasecmp (str, src_sym + sizeof (src_prefix) - 1))
	    {
	      *err |= src << GPG_ERR_SOURCE_SHIFT;
	      return 1;
	    }
	}
    }
  else if (!strncasecmp (code_prefix, str, sizeof (code_prefix) - 1))
    {
      gpg_err_code_t code;

      if (*have_code)
	return 0;
      *have_code = 1;
      str += sizeof (code_prefix) - 1;

      for (code = 0; code < GPG_ERR_CODE_DIM; code++)
	{
	  const char *code_sym = gpg_strerror_sym (code);
	  if (code_sym
	      && !strcasecmp (str, code_sym + sizeof (code_prefix) - 1))
	    {
	      *err |= code;
	      return 1;
	    }
	}
    }
  return 0;
}


static int
get_err_from_symbol (char *str, gpg_error_t *err)
{
  char *str2 = str;
  int have_source = 0;
  int have_code = 0;
  int ret;
  char *saved_pos = NULL;
  char saved_char;

  *err = 0;
  while (*str2 && ((*str2 >= 'A' && *str2 <= 'Z')
		   || (*str2 >= '0' && *str2 <= '9')
		   || *str2 == '_'))
    str2++;
  if (*str2)
    {
      saved_pos = str2;
      saved_char = *str2;
      *str2 = '\0';
      str2++;
    }
  else
    str2 = NULL;

  ret = get_err_from_symbol_one (str, err, &have_source, &have_code);
  if (ret && str2)
    ret = get_err_from_symbol_one (str2, err, &have_source, &have_code);

  if (saved_pos)
    *saved_pos = saved_char;
  return ret;
}


static int
get_err_from_str_one (char *str, gpg_error_t *err,
		      int *have_source, int *have_code)
{
  gpg_err_source_t src;
  gpg_err_code_t code;

  for (src = 0; src < GPG_ERR_SOURCE_DIM; src++)
    {
      const char *src_str = gpg_strsource (src << GPG_ERR_SOURCE_SHIFT);
      if (src_str && !strcasecmp (str, src_str))
	{
	  if (*have_source)
	    return 0;

	  *have_source = 1;
	  *err |= src << GPG_ERR_SOURCE_SHIFT;
	  return 1;
	}
    }

  for (code = 0; code < GPG_ERR_CODE_DIM; code++)
    {
      const char *code_str = gpg_strerror (code);
      if (code_str && !strcasecmp (str, code_str))
	{
	  if (*have_code)
	    return 0;
	  
	  *have_code = 1;
	  *err |= code;
	  return 1;
	}
    }

  return 0;
}


static int
get_err_from_str (char *str, gpg_error_t *err)
{
  char *str2 = str;
  int have_source = 0;
  int have_code = 0;
  int ret;
  char *saved_pos = NULL;
  char saved_char;

  *err = 0;
  ret = get_err_from_str_one (str, err, &have_source, &have_code);
  if (ret)
    return ret;

  while (*str2 && ((*str2 >= 'A' && *str2 <= 'Z')
		   || (*str2 >= 'a' && *str2 <= 'z')
		   || (*str2 >= '0' && *str2 <= '9')
		   || *str2 == '_'))
    str2++;
  if (*str2)
    {
      saved_pos = str2;
      saved_char = *str2;
      *((char *) str2) = '\0';
      str2++;
      while (*str2 && !((*str2 >= 'A' && *str2 <= 'Z')
			|| (*str2 >= 'a' && *str2 <= 'z')
			|| (*str2 >= '0' && *str2 <= '9')
			|| *str2 == '_'))
	str2++;
    }
  else
    str2 = NULL;

  ret = get_err_from_str_one (str, err, &have_source, &have_code);
  if (ret && str2)
    ret = get_err_from_str_one (str2, err, &have_source, &have_code);

  if (saved_pos)
    *saved_pos = saved_char;
  return ret;
}



int
main (int argc, char *argv[])
{
  int i = 1;

#ifndef GPG_ERR_INITIALIZED
  gpg_err_init ();
#endif

  i18n_init ();


  if (argc == 1)
    {
      fprintf (stderr, _("Usage: %s GPG-ERROR [...]\n"), 
               strrchr (argv[0],'/')? (strrchr (argv[0], '/')+1): argv[0]);
      exit (1);
    }
  else if (argc == 2 && !strcmp (argv[1], "--version"))
    {
      fputs ("gpg-error (" PACKAGE_NAME ") " PACKAGE_VERSION "\n", stdout);
      exit (0);
    }


  while (i < argc)
    {
      gpg_error_t err;

      if (get_err_from_number (argv[i], &err)
	  || get_err_from_symbol (argv[i], &err)
	  || get_err_from_str (argv[i], &err))
	{
	  const char *source_sym = gpg_strsource_sym (err);
	  const char *error_sym = gpg_strerror_sym (err);
	  
	  printf ("%u = (%u, %u) = (%s, %s) = (%s, %s)\n",
		  err, gpg_err_source (err), gpg_err_code (err),
		  source_sym ? source_sym : "-", error_sym ? error_sym : "-",
		  gpg_strsource (err), gpg_strerror (err));
	}
      else
	fprintf (stderr, _("%s: warning: could not recognize %s\n"),
		 argv[0], argv[i]);
      i++;
    }

  exit (0);
}
