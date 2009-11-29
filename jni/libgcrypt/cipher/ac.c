/* ac.c - Alternative interface for asymmetric cryptography.
   Copyright (C) 2003, 2004, 2005, 2006
                 2007, 2008  Free Software Foundation, Inc.
 
   This file is part of Libgcrypt.
  
   Libgcrypt is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser general Public License as
   published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
  
   Libgcrypt is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.
  
   You should have received a copy of the GNU Lesser General Public
   License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#include <config.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stddef.h>

#include "g10lib.h"
#include "cipher.h"
#include "mpi.h"



/* At the moment the ac interface is a wrapper around the pk
   interface, but this might change somewhen in the future, depending
   on how many people prefer the ac interface.  */

/* Mapping of flag numbers to the according strings as it is expected
   for S-expressions.  */
static struct number_string
{
  int number;
  const char *string;
} ac_flags[] =
  {
    { GCRY_AC_FLAG_NO_BLINDING, "no-blinding" },
  };

/* The positions in this list correspond to the values contained in
   the gcry_ac_key_type_t enumeration list.  */
static const char *ac_key_identifiers[] =
  {
    "private-key",
    "public-key"
  };

/* These specifications are needed for key-pair generation; the caller
   is allowed to pass additional, algorithm-specific `specs' to
   gcry_ac_key_pair_generate.  This list is used for decoding the
   provided values according to the selected algorithm.  */
struct gcry_ac_key_generate_spec
{
  int algorithm;		/* Algorithm for which this flag is
				   relevant.  */
  const char *name;		/* Name of this flag.  */
  size_t offset;		/* Offset in the cipher-specific spec
				   structure at which the MPI value
				   associated with this flag is to be
				   found.  */
} ac_key_generate_specs[] =
  {
    { GCRY_AC_RSA, "rsa-use-e", offsetof (gcry_ac_key_spec_rsa_t, e) },
    { 0 }
  };

/* Handle structure.  */
struct gcry_ac_handle
{
  int algorithm;		/* Algorithm ID associated with this
				   handle.  */
  const char *algorithm_name;	/* Name of the algorithm.  */
  unsigned int flags;		/* Flags, not used yet.  */
  gcry_module_t module;	        /* Reference to the algorithm
				   module.  */
};

/* A named MPI value.  */
typedef struct gcry_ac_mpi
{
  char *name;			/* Self-maintained copy of name.  */
  gcry_mpi_t mpi;		/* MPI value.         */
  unsigned int flags;		/* Flags.             */
} gcry_ac_mpi_t;

/* A data set, that is simply a list of named MPI values.  */
struct gcry_ac_data
{
  gcry_ac_mpi_t *data;		/* List of named values.      */
  unsigned int data_n;		/* Number of values in DATA.  */
};

/* A single key.  */
struct gcry_ac_key
{
  gcry_ac_data_t data;		/* Data in native ac structure.  */
  gcry_ac_key_type_t type;	/* Type of the key.              */
};

/* A key pair.  */
struct gcry_ac_key_pair
{
  gcry_ac_key_t public;
  gcry_ac_key_t secret;
};



/* 
 * Functions for working with data sets.
 */

/* Creates a new, empty data set and store it in DATA.  */
gcry_error_t
_gcry_ac_data_new (gcry_ac_data_t *data)
{
  gcry_ac_data_t data_new;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_new = gcry_malloc (sizeof (*data_new));
  if (! data_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  data_new->data = NULL;
  data_new->data_n = 0;
  *data = data_new;
  err = 0;

 out:

  return err;
}

/* Destroys all the entries in DATA, but not DATA itself.  */
static void
ac_data_values_destroy (gcry_ac_data_t data)
{
  unsigned int i;
  
  for (i = 0; i < data->data_n; i++)
    if (data->data[i].flags & GCRY_AC_FLAG_DEALLOC)
      {
	gcry_mpi_release (data->data[i].mpi);
	gcry_free (data->data[i].name);
      }
}

/* Destroys the data set DATA.  */
void
_gcry_ac_data_destroy (gcry_ac_data_t data)
{
  if (data)
    {
      ac_data_values_destroy (data);
      gcry_free (data->data);
      gcry_free (data);
    }
}

/* This function creates a copy of the array of named MPIs DATA_MPIS,
   which is of length DATA_MPIS_N; the copy is stored in
   DATA_MPIS_CP.  */
static gcry_error_t
ac_data_mpi_copy (gcry_ac_mpi_t *data_mpis, unsigned int data_mpis_n,
		  gcry_ac_mpi_t **data_mpis_cp)
{
  gcry_ac_mpi_t *data_mpis_new;
  gcry_error_t err;
  unsigned int i;
  gcry_mpi_t mpi;
  char *label;

  data_mpis_new = gcry_malloc (sizeof (*data_mpis_new) * data_mpis_n);
  if (! data_mpis_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
  memset (data_mpis_new, 0, sizeof (*data_mpis_new) * data_mpis_n);

  err = 0;
  for (i = 0; i < data_mpis_n; i++)
    {
      /* Copy values.  */

      label = gcry_strdup (data_mpis[i].name);
      mpi = gcry_mpi_copy (data_mpis[i].mpi);
      if (! (label && mpi))
	{
	  err = gcry_error_from_errno (errno);
	  gcry_mpi_release (mpi);
	  gcry_free (label);
	  break;
	}

      data_mpis_new[i].flags = GCRY_AC_FLAG_DEALLOC;
      data_mpis_new[i].name = label;
      data_mpis_new[i].mpi = mpi;
    }
  if (err)
    goto out;

  *data_mpis_cp = data_mpis_new;
  err = 0;

 out:

  if (err)
    if (data_mpis_new)
      {
	for (i = 0; i < data_mpis_n; i++)
	  {
	    gcry_mpi_release (data_mpis_new[i].mpi);
	    gcry_free (data_mpis_new[i].name);
	  }
	gcry_free (data_mpis_new);
      }

  return err;
}

/* Create a copy of the data set DATA and store it in DATA_CP.  */
gcry_error_t
_gcry_ac_data_copy (gcry_ac_data_t *data_cp, gcry_ac_data_t data)
{
  gcry_ac_mpi_t *data_mpis = NULL;
  gcry_ac_data_t data_new;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  /* Allocate data set.  */
  data_new = gcry_malloc (sizeof (*data_new));
  if (! data_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  err = ac_data_mpi_copy (data->data, data->data_n, &data_mpis);
  if (err)
    goto out;
  
  data_new->data_n = data->data_n;
  data_new->data = data_mpis;
  *data_cp = data_new;

 out:

  if (err)
    gcry_free (data_new);

  return err;
}

/* Returns the number of named MPI values inside of the data set
   DATA.  */
unsigned int
_gcry_ac_data_length (gcry_ac_data_t data)
{
  return data->data_n;
}


/* Add the value MPI to DATA with the label NAME.  If FLAGS contains
   GCRY_AC_FLAG_COPY, the data set will contain copies of NAME
   and MPI.  If FLAGS contains GCRY_AC_FLAG_DEALLOC or
   GCRY_AC_FLAG_COPY, the values contained in the data set will
   be deallocated when they are to be removed from the data set.  */
gcry_error_t
_gcry_ac_data_set (gcry_ac_data_t data, unsigned int flags,
		   const char *name, gcry_mpi_t mpi)
{
  gcry_error_t err;
  gcry_mpi_t mpi_cp;
  char *name_cp;
  unsigned int i;

  name_cp = NULL;
  mpi_cp = NULL;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  if (flags & ~(GCRY_AC_FLAG_DEALLOC | GCRY_AC_FLAG_COPY))
    {
      err = gcry_error (GPG_ERR_INV_ARG);
      goto out;
    }

  if (flags & GCRY_AC_FLAG_COPY)
    {
      /* Create copies.  */

      flags |= GCRY_AC_FLAG_DEALLOC;
      name_cp = gcry_strdup (name);
      mpi_cp = gcry_mpi_copy (mpi);
      if (! (name_cp && mpi_cp))
	{
	  err = gcry_error_from_errno (errno);
	  goto out;
	}
    }

  /* Search for existing entry.  */
  for (i = 0; i < data->data_n; i++)
    if (! strcmp (name, data->data[i].name))
      break;
  if (i < data->data_n)
    {
      /* An entry for NAME does already exist.  */
      if (data->data[i].flags & GCRY_AC_FLAG_DEALLOC)
	{
	  gcry_mpi_release (data->data[i].mpi);
	  gcry_free (data->data[i].name);
	}
    }
  else
    {
      /* Create a new entry.  */

      gcry_ac_mpi_t *ac_mpis;

      ac_mpis = gcry_realloc (data->data,
			      sizeof (*data->data) * (data->data_n + 1));
      if (! ac_mpis)
	{
	  err = gcry_error_from_errno (errno);
	  goto out;
	}

      if (data->data != ac_mpis)
	data->data = ac_mpis;
      data->data_n++;
    }

  data->data[i].name = name_cp ? name_cp : ((char *) name);
  data->data[i].mpi = mpi_cp ? mpi_cp : mpi;
  data->data[i].flags = flags;
  err = 0;

 out:

  if (err)
    {
      gcry_mpi_release (mpi_cp);
      gcry_free (name_cp);
    }

  return err;
}

/* Stores the value labelled with NAME found in the data set DATA in
   MPI.  The returned MPI value will be released in case
   gcry_ac_data_set is used to associate the label NAME with a
   different MPI value.  */
gcry_error_t
_gcry_ac_data_get_name (gcry_ac_data_t data, unsigned int flags,
			const char *name, gcry_mpi_t *mpi)
{
  gcry_mpi_t mpi_return;
  gcry_error_t err;
  unsigned int i;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  if (flags & ~(GCRY_AC_FLAG_COPY))
    {
      err = gcry_error (GPG_ERR_INV_ARG);
      goto out;
    }

  for (i = 0; i < data->data_n; i++)
    if (! strcmp (name, data->data[i].name))
      break;
  if (i == data->data_n)
    {
      err = gcry_error (GPG_ERR_NOT_FOUND);
      goto out;
    }

  if (flags & GCRY_AC_FLAG_COPY)
    {
      mpi_return = gcry_mpi_copy (data->data[i].mpi);
      if (! mpi_return)
	{
	  err = gcry_error_from_errno (errno); /* FIXME? */
	  goto out;
	}
    }
  else
    mpi_return = data->data[i].mpi;

  *mpi = mpi_return;
  err = 0;

 out:

  return err;
}

/* Stores in NAME and MPI the named MPI value contained in the data
   set DATA with the index IDX.  NAME or MPI may be NULL.  The
   returned MPI value will be released in case gcry_ac_data_set is
   used to associate the label NAME with a different MPI value.  */
gcry_error_t
_gcry_ac_data_get_index (gcry_ac_data_t data, unsigned int flags,
			 unsigned int idx,
			 const char **name, gcry_mpi_t *mpi)
{
  gcry_error_t err;
  gcry_mpi_t mpi_cp;
  char *name_cp;

  name_cp = NULL;
  mpi_cp = NULL;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  if (flags & ~(GCRY_AC_FLAG_COPY))
    {
      err = gcry_error (GPG_ERR_INV_ARG);
      goto out;
    }

  if (idx >= data->data_n)
    {
      err = gcry_error (GPG_ERR_INV_ARG);
      goto out;
    }

  if (flags & GCRY_AC_FLAG_COPY)
    {
      /* Return copies to the user.  */
      if (name)
	{
	  name_cp = gcry_strdup (data->data[idx].name);
	  if (! name_cp)
	    {
	      err = gcry_error_from_errno (errno);
	      goto out;
	    }
	}
      if (mpi)
	{
	  mpi_cp = gcry_mpi_copy (data->data[idx].mpi);
	  if (! mpi_cp)
	    {
	      err = gcry_error_from_errno (errno);
	      goto out;
	    }
	}
    }

  if (name)
    *name = name_cp ? name_cp : data->data[idx].name;
  if (mpi)
    *mpi = mpi_cp ? mpi_cp : data->data[idx].mpi;
  err = 0;

 out:

  if (err)
    {
      gcry_mpi_release (mpi_cp);
      gcry_free (name_cp);
    }

  return err;
}

/* Convert the data set DATA into a new S-Expression, which is to be
   stored in SEXP, according to the identifiers contained in
   IDENTIFIERS.  */
gcry_error_t
_gcry_ac_data_to_sexp (gcry_ac_data_t data, gcry_sexp_t *sexp,
		       const char **identifiers)
{
  gcry_sexp_t sexp_new;
  gcry_error_t err;
  char *sexp_buffer;
  size_t sexp_buffer_n;
  size_t identifiers_n;
  const char *label;
  gcry_mpi_t mpi;
  void **arg_list;
  size_t data_n;
  unsigned int i;

  sexp_buffer_n = 1;
  sexp_buffer = NULL;
  arg_list = NULL;
  err = 0;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  /* Calculate size of S-expression representation.  */

  i = 0;
  if (identifiers)
    while (identifiers[i])
      {
	/* For each identifier, we add "(<IDENTIFIER>)".  */
	sexp_buffer_n += 1 + strlen (identifiers[i]) + 1;
	i++;
      }
  identifiers_n = i;
  
  if (! identifiers_n)
    /* If there are NO identifiers, we still add surrounding braces so
       that we have a list of named MPI value lists.  Otherwise it
       wouldn't be too much fun to process these lists.  */
    sexp_buffer_n += 2;
  
  data_n = _gcry_ac_data_length (data);
  for (i = 0; i < data_n; i++)
    {
      err = gcry_ac_data_get_index (data, 0, i, &label, NULL);
      if (err)
	break;
      /* For each MPI we add "(<LABEL> %m)".  */
      sexp_buffer_n += 1 + strlen (label) + 4;
    }
  if (err)
    goto out;

  /* Allocate buffer.  */

  sexp_buffer = gcry_malloc (sexp_buffer_n);
  if (! sexp_buffer)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Fill buffer.  */

  *sexp_buffer = 0;
  sexp_buffer_n = 0;

  /* Add identifiers: (<IDENTIFIER0>(<IDENTIFIER1>...)).  */
  if (identifiers_n)
    {
      /* Add nested identifier lists as usual.  */
      for (i = 0; i < identifiers_n; i++)
	sexp_buffer_n += sprintf (sexp_buffer + sexp_buffer_n, "(%s",
				  identifiers[i]);
    }
  else
    {
      /* Add special list.  */
      sexp_buffer_n += sprintf (sexp_buffer + sexp_buffer_n, "(");
    }

  /* Add MPI list.  */
  arg_list = gcry_malloc (sizeof (*arg_list) * (data_n + 1));
  if (! arg_list)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
  for (i = 0; i < data_n; i++)
    {
      err = gcry_ac_data_get_index (data, 0, i, &label, &mpi);
      if (err)
	break;
      sexp_buffer_n += sprintf (sexp_buffer + sexp_buffer_n,
				"(%s %%m)", label);
      arg_list[i] = &data->data[i].mpi;
    }
  if (err)
    goto out;

  if (identifiers_n)
    {
      /* Add closing braces for identifier lists as usual.  */
      for (i = 0; i < identifiers_n; i++)
	sexp_buffer_n += sprintf (sexp_buffer + sexp_buffer_n, ")");
    }
  else
    {
      /* Add closing braces for special list.  */
      sexp_buffer_n += sprintf (sexp_buffer + sexp_buffer_n, ")");
    }

  /* Construct.  */
  err = gcry_sexp_build_array (&sexp_new, NULL, sexp_buffer, arg_list);
  if (err)
    goto out;

  *sexp = sexp_new;

 out:

  gcry_free (sexp_buffer);
  gcry_free (arg_list);

  return err;
}

/* Create a new data set, which is to be stored in DATA_SET, from the
   S-Expression SEXP, according to the identifiers contained in
   IDENTIFIERS.  */
gcry_error_t
_gcry_ac_data_from_sexp (gcry_ac_data_t *data_set, gcry_sexp_t sexp,
			 const char **identifiers)
{
  gcry_ac_data_t data_set_new;
  gcry_error_t err;
  gcry_sexp_t sexp_cur;
  gcry_sexp_t sexp_tmp;
  gcry_mpi_t mpi;
  char *string;
  const char *data;
  size_t data_n;
  size_t sexp_n;
  unsigned int i;
  int skip_name;

  data_set_new = NULL;
  sexp_cur = sexp;
  sexp_tmp = NULL;
  string = NULL;
  mpi = NULL;
  err = 0;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  /* Process S-expression/identifiers.  */

  if (identifiers)
    {
      for (i = 0; identifiers[i]; i++)
	{
	  /* Next identifier.  Extract first data item from
	     SEXP_CUR.  */
	  data = gcry_sexp_nth_data (sexp_cur, 0, &data_n);

	  if (! ((data_n == strlen (identifiers[i]))
		 && (! strncmp (data, identifiers[i], data_n))))
	    {
	      /* Identifier mismatch -> error.  */
	      err = gcry_error (GPG_ERR_INV_SEXP);
	      break;
	    }

	  /* Identifier matches.  Now we have to distinguish two
	     cases:
	     
	     (i)  we are at the last identifier:
	     leave loop

	     (ii) we are not at the last identifier:
	     extract next element, which is supposed to be a
	     sublist.  */

	  if (! identifiers[i + 1])
	    /* Last identifier.  */
	    break;
	  else
	    {
	      /* Not the last identifier, extract next sublist.  */

	      sexp_tmp = gcry_sexp_nth (sexp_cur, 1);
	      if (! sexp_tmp)
		{
		  /* Missing sublist.  */
		  err = gcry_error (GPG_ERR_INV_SEXP);
		  break;
		}

	      /* Release old SEXP_CUR, in case it is not equal to the
		 original SEXP.  */
	      if (sexp_cur != sexp)
		gcry_sexp_release (sexp_cur);

	      /* Make SEXP_CUR point to the new current sublist.  */
	      sexp_cur = sexp_tmp;
              sexp_tmp = NULL;
	    }
	}
      if (err)
	goto out;

      if (i)
        {
          /* We have at least one identifier in the list, this means
             the the list of named MPI values is prefixed, this means
             that we need to skip the first item (the list name), when
             processing the MPI values.  */
          skip_name = 1;
        }
      else
        {
          /* Since there is no identifiers list, the list of named MPI
             values is not prefixed with a list name, therefore the
             offset to use is zero.  */
          skip_name = 0;
        }
    }
  else
    /* Since there is no identifiers list, the list of named MPI
       values is not prefixed with a list name, therefore the offset
       to use is zero.  */
    skip_name = 0;

  /* Create data set from S-expression data.  */
  
  err = gcry_ac_data_new (&data_set_new);
  if (err)
    goto out;

  /* Figure out amount of named MPIs in SEXP_CUR.  */
  if (sexp_cur)
    sexp_n = gcry_sexp_length (sexp_cur) - skip_name;
  else
    sexp_n = 0;

  /* Extracte the named MPIs sequentially.  */
  for (i = 0; i < sexp_n; i++)
    {
      /* Store next S-Expression pair, which is supposed to consist of
	 a name and an MPI value, in SEXP_TMP.  */

      sexp_tmp = gcry_sexp_nth (sexp_cur, i + skip_name);
      if (! sexp_tmp)
	{
	  err = gcry_error (GPG_ERR_INV_SEXP);
	  break;
	}

      /* Extract name from current S-Expression pair.  */
      data = gcry_sexp_nth_data (sexp_tmp, 0, &data_n);
      string = gcry_malloc (data_n + 1);
      if (! string)
	{
	  err = gcry_error_from_errno (errno);
	  break;
	}
      memcpy (string, data, data_n);
      string[data_n] = 0;

      /* Extract MPI value.  */
      mpi = gcry_sexp_nth_mpi (sexp_tmp, 1, 0);
      if (! mpi)
	{
	  err = gcry_error (GPG_ERR_INV_SEXP); /* FIXME? */
	  break;
	}

      /* Store named MPI in data_set_new.  */
      err = gcry_ac_data_set (data_set_new, GCRY_AC_FLAG_DEALLOC, string, mpi);
      if (err)
	break;

/*       gcry_free (string); */
      string = NULL;
/*       gcry_mpi_release (mpi); */
      mpi = NULL;

      gcry_sexp_release (sexp_tmp);
      sexp_tmp = NULL;
    }
  if (err)
    goto out;

  *data_set = data_set_new;

 out:

  if (sexp_cur != sexp)
    gcry_sexp_release (sexp_cur);
  gcry_sexp_release (sexp_tmp);
  gcry_mpi_release (mpi);
  gcry_free (string);
  
  if (err)
    gcry_ac_data_destroy (data_set_new);

  return err;
}


static void
_gcry_ac_data_dump (const char *prefix, gcry_ac_data_t data)
{
  unsigned char *mpi_buffer;
  size_t mpi_buffer_n;
  unsigned int data_n;
  gcry_error_t err;
  const char *name;
  gcry_mpi_t mpi;
  unsigned int i;

  if (! data)
    return;

  if (fips_mode ())
    return;

  mpi_buffer = NULL;

  data_n = _gcry_ac_data_length (data);
  for (i = 0; i < data_n; i++)
    {
      err = gcry_ac_data_get_index (data, 0, i, &name, &mpi);
      if (err)
	{
	  log_error ("failed to dump data set");
	  break;
	}

      err = gcry_mpi_aprint (GCRYMPI_FMT_HEX, &mpi_buffer, &mpi_buffer_n, mpi);
      if (err)
	{
	  log_error ("failed to dump data set");
	  break;
	}

      log_printf ("%s%s%s: %s\n",
		  prefix ? prefix : "",
		  prefix ? ": " : ""
		  , name, mpi_buffer);

      gcry_free (mpi_buffer);
      mpi_buffer = NULL;
    }

  gcry_free (mpi_buffer);
}

/* Dump the named MPI values contained in the data set DATA to
   Libgcrypt's logging stream.  */
void
gcry_ac_data_dump (const char *prefix, gcry_ac_data_t data)
{
  _gcry_ac_data_dump (prefix, data);
}

/* Destroys any values contained in the data set DATA.  */
void
_gcry_ac_data_clear (gcry_ac_data_t data)
{
  ac_data_values_destroy (data);
  gcry_free (data->data);
  data->data = NULL;
  data->data_n = 0;
}



/*
 * Implementation of `ac io' objects.
 */

/* Initialize AC_IO according to MODE, TYPE and the variable list of
   arguments AP.  The list of variable arguments to specify depends on
   the given TYPE.  */
void
_gcry_ac_io_init_va (gcry_ac_io_t *ac_io,
		     gcry_ac_io_mode_t mode, gcry_ac_io_type_t type, va_list ap)
{
  memset (ac_io, 0, sizeof (*ac_io));

  if (fips_mode ())
    return;

  gcry_assert ((mode == GCRY_AC_IO_READABLE) || (mode == GCRY_AC_IO_WRITABLE));
  gcry_assert ((type == GCRY_AC_IO_STRING) || (type == GCRY_AC_IO_STRING));

  ac_io->mode = mode;
  ac_io->type = type;

  switch (mode)
    {
    case GCRY_AC_IO_READABLE:
      switch (type)
	{
	case GCRY_AC_IO_STRING:
	  ac_io->io.readable.string.data = va_arg (ap, unsigned char *);
	  ac_io->io.readable.string.data_n = va_arg (ap, size_t);
	  break;

	case GCRY_AC_IO_CALLBACK:
	  ac_io->io.readable.callback.cb = va_arg (ap, gcry_ac_data_read_cb_t);
	  ac_io->io.readable.callback.opaque = va_arg (ap, void *);
	  break;
	}
      break;
    case GCRY_AC_IO_WRITABLE:
      switch (type)
	{
	case GCRY_AC_IO_STRING:
	  ac_io->io.writable.string.data = va_arg (ap, unsigned char **);
	  ac_io->io.writable.string.data_n = va_arg (ap, size_t *);
	  break;

	case GCRY_AC_IO_CALLBACK:
	  ac_io->io.writable.callback.cb = va_arg (ap, gcry_ac_data_write_cb_t);
	  ac_io->io.writable.callback.opaque = va_arg (ap, void *);
	  break;
	}
      break;
    }
}

/* Initialize AC_IO according to MODE, TYPE and the variable list of
   arguments.  The list of variable arguments to specify depends on
   the given TYPE. */
void
_gcry_ac_io_init (gcry_ac_io_t *ac_io,
		  gcry_ac_io_mode_t mode, gcry_ac_io_type_t type, ...)
{
  va_list ap;

  va_start (ap, type);
  _gcry_ac_io_init_va (ac_io, mode, type, ap);
  va_end (ap);
}


/* Write to the IO object AC_IO BUFFER_N bytes from BUFFER.  Return
   zero on success or error code.  */
static gcry_error_t
_gcry_ac_io_write (gcry_ac_io_t *ac_io, unsigned char *buffer, size_t buffer_n)
{
  gcry_error_t err;

  gcry_assert (ac_io->mode == GCRY_AC_IO_WRITABLE);
  err = 0;

  switch (ac_io->type)
    {
    case GCRY_AC_IO_STRING:
      {
	unsigned char *p;

	if (*ac_io->io.writable.string.data)
	  {
	    p = gcry_realloc (*ac_io->io.writable.string.data,
			      *ac_io->io.writable.string.data_n + buffer_n);
	    if (! p)
	      err = gcry_error_from_errno (errno);
	    else
	      {
		if (*ac_io->io.writable.string.data != p)
		  *ac_io->io.writable.string.data = p;
		memcpy (p + *ac_io->io.writable.string.data_n, buffer, buffer_n);
		*ac_io->io.writable.string.data_n += buffer_n;
	      }
	  }
	else
	  {
	    if (gcry_is_secure (buffer))
	      p = gcry_malloc_secure (buffer_n);
	    else
	      p = gcry_malloc (buffer_n);
	    if (! p)
	      err = gcry_error_from_errno (errno);
	    else
	      {
		memcpy (p, buffer, buffer_n);
		*ac_io->io.writable.string.data = p;
		*ac_io->io.writable.string.data_n = buffer_n;
	      }
	  }
      }
      break;

    case GCRY_AC_IO_CALLBACK:
      err = (*ac_io->io.writable.callback.cb) (ac_io->io.writable.callback.opaque,
					       buffer, buffer_n);
      break;
    }

  return err;
}

/* Read *BUFFER_N bytes from the IO object AC_IO into BUFFER; NREAD
   bytes have already been read from the object; on success, store the
   amount of bytes read in *BUFFER_N; zero bytes read means EOF.
   Return zero on success or error code.  */
static gcry_error_t
_gcry_ac_io_read (gcry_ac_io_t *ac_io,
		  unsigned int nread, unsigned char *buffer, size_t *buffer_n)
{
  gcry_error_t err;
  
  gcry_assert (ac_io->mode == GCRY_AC_IO_READABLE);
  err = 0;

  switch (ac_io->type)
    {
    case GCRY_AC_IO_STRING:
      {
	size_t bytes_available;
	size_t bytes_to_read;
	size_t bytes_wanted;

	bytes_available = ac_io->io.readable.string.data_n - nread;
	bytes_wanted = *buffer_n;

	if (bytes_wanted > bytes_available)
	  bytes_to_read = bytes_available;
	else
	  bytes_to_read = bytes_wanted;

	memcpy (buffer, ac_io->io.readable.string.data + nread, bytes_to_read);
	*buffer_n = bytes_to_read;
	err = 0;
	break;
      }

    case GCRY_AC_IO_CALLBACK:
      err = (*ac_io->io.readable.callback.cb)
	(ac_io->io.readable.callback.opaque, buffer, buffer_n);
      break;
    }

  return err;
}

/* Read all data available from the IO object AC_IO into newly
   allocated memory, storing an appropriate pointer in *BUFFER and the
   amount of bytes read in *BUFFER_N.  Return zero on success or error
   code.  */
static gcry_error_t
_gcry_ac_io_read_all (gcry_ac_io_t *ac_io, unsigned char **buffer, size_t *buffer_n)
{
  unsigned char *buffer_new;
  size_t buffer_new_n;
  unsigned char buf[BUFSIZ];
  size_t buf_n;
  unsigned char *p;
  gcry_error_t err;

  buffer_new = NULL;
  buffer_new_n = 0;

  while (1)
    {
      buf_n = sizeof (buf);
      err = _gcry_ac_io_read (ac_io, buffer_new_n, buf, &buf_n);
      if (err)
	break;

      if (buf_n)
	{
	  p = gcry_realloc (buffer_new, buffer_new_n + buf_n);
	  if (! p)
	    {
	      err = gcry_error_from_errno (errno);
	      break;
	    }
	  
	  if (buffer_new != p)
	    buffer_new = p;

	  memcpy (buffer_new + buffer_new_n, buf, buf_n);
	  buffer_new_n += buf_n;
	}
      else
	break;
    }
  if (err)
    goto out;

  *buffer_n = buffer_new_n;
  *buffer = buffer_new;

 out:

  if (err)
    gcry_free (buffer_new);

  return err;
}

/* Read data chunks from the IO object AC_IO until EOF, feeding them
   to the callback function CB.  Return zero on success or error
   code.  */
static gcry_error_t
_gcry_ac_io_process (gcry_ac_io_t *ac_io,
		     gcry_ac_data_write_cb_t cb, void *opaque)
{
  unsigned char buffer[BUFSIZ];
  unsigned int nread;
  size_t buffer_n;
  gcry_error_t err;

  nread = 0;

  while (1)
    {
      buffer_n = sizeof (buffer);
      err = _gcry_ac_io_read (ac_io, nread, buffer, &buffer_n);
      if (err)
	break;
      if (buffer_n)
	{
	  err = (*cb) (opaque, buffer, buffer_n);
	  if (err)
	    break;
	  nread += buffer_n;
	}
      else
	break;
    }

  return err;
}



/* 
 * Functions for converting data between the native ac and the
 * S-expression structure used by the pk interface.
 */

/* Extract the S-Expression DATA_SEXP into DATA under the control of
   TYPE and NAME.  This function assumes that S-Expressions are of the
   following structure:

   (IDENTIFIER [...]
   (ALGORITHM <list of named MPI values>)) */
static gcry_error_t
ac_data_extract (const char *identifier, const char *algorithm,
		 gcry_sexp_t sexp, gcry_ac_data_t *data)
{
  gcry_error_t err;
  gcry_sexp_t value_sexp;
  gcry_sexp_t data_sexp;
  size_t data_sexp_n;
  gcry_mpi_t value_mpi;
  char *value_name;
  const char *data_raw;
  size_t data_raw_n;
  gcry_ac_data_t data_new;
  unsigned int i;

  value_sexp = NULL;
  data_sexp = NULL;
  value_name = NULL;
  value_mpi = NULL;
  data_new = NULL;

  /* Verify that the S-expression contains the correct identifier.  */
  data_raw = gcry_sexp_nth_data (sexp, 0, &data_raw_n);
  if ((! data_raw) || strncmp (identifier, data_raw, data_raw_n))
    {
      err = gcry_error (GPG_ERR_INV_SEXP);
      goto out;
    }

  /* Extract inner S-expression.  */
  data_sexp = gcry_sexp_find_token (sexp, algorithm, 0);
  if (! data_sexp)
    {
      err = gcry_error (GPG_ERR_INV_SEXP);
      goto out;
    }

  /* Count data elements.  */
  data_sexp_n = gcry_sexp_length (data_sexp);
  data_sexp_n--;

  /* Allocate new data set.  */
  err = _gcry_ac_data_new (&data_new);
  if (err)
    goto out;

  /* Iterate through list of data elements and add them to the data
     set.  */
  for (i = 0; i < data_sexp_n; i++)
    {
      /* Get the S-expression of the named MPI, that contains the name
	 and the MPI value.  */
      value_sexp = gcry_sexp_nth (data_sexp, i + 1);
      if (! value_sexp)
	{
	  err = gcry_error (GPG_ERR_INV_SEXP);
	  break;
	}

      /* Extract the name.  */
      data_raw = gcry_sexp_nth_data (value_sexp, 0, &data_raw_n);
      if (! data_raw)
	{
	  err = gcry_error (GPG_ERR_INV_SEXP);
	  break;
	}

      /* Extract the MPI value.  */
      value_mpi = gcry_sexp_nth_mpi (value_sexp, 1, GCRYMPI_FMT_USG);
      if (! value_mpi)
	{
	  err = gcry_error (GPG_ERR_INTERNAL); /* FIXME? */
	  break;
	}

      /* Duplicate the name.  */
      value_name = gcry_malloc (data_raw_n + 1);
      if (! value_name)
	{
	  err = gcry_error_from_errno (errno);
	  break;
	}
      strncpy (value_name, data_raw, data_raw_n);
      value_name[data_raw_n] = 0;

      err = _gcry_ac_data_set (data_new, GCRY_AC_FLAG_DEALLOC, value_name, value_mpi);
      if (err)
	break;

      gcry_sexp_release (value_sexp);
      value_sexp = NULL;
      value_name = NULL;
      value_mpi = NULL;
    }
  if (err)
    goto out;

  /* Copy out.  */
  *data = data_new;

 out:

  /* Deallocate resources.  */
  if (err)
    {
      _gcry_ac_data_destroy (data_new);
      gcry_mpi_release (value_mpi);
      gcry_free (value_name);
      gcry_sexp_release (value_sexp);
    }
  gcry_sexp_release (data_sexp);

  return err;
}

/* Construct an S-expression from the DATA and store it in
   DATA_SEXP. The S-expression will be of the following structure:

   (IDENTIFIER [(flags [...])]
   (ALGORITHM <list of named MPI values>))  */
static gcry_error_t
ac_data_construct (const char *identifier, int include_flags,
		   unsigned int flags, const char *algorithm,
		   gcry_ac_data_t data, gcry_sexp_t *sexp)
{
  unsigned int data_length;
  gcry_sexp_t sexp_new;
  gcry_error_t err;
  size_t sexp_format_n;
  char *sexp_format;
  void **arg_list;
  unsigned int i;

  arg_list = NULL;
  sexp_new = NULL;
  sexp_format = NULL;

  /* We build a list of arguments to pass to
     gcry_sexp_build_array().  */
  data_length = _gcry_ac_data_length (data);
  arg_list = gcry_malloc (sizeof (*arg_list) * (data_length * 2));
  if (! arg_list)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Fill list with MPIs.  */
  for (i = 0; i < data_length; i++)
    {
      char **nameaddr  = &data->data[i].name;

      arg_list[(i * 2) + 0] = nameaddr;
      arg_list[(i * 2) + 1] = &data->data[i].mpi;
    }

  /* Calculate size of format string.  */
  sexp_format_n = (3
		   + (include_flags ? 7 : 0)
		   + (algorithm ? (2 + strlen (algorithm)) : 0)
		   + strlen (identifier));

  for (i = 0; i < data_length; i++)
    /* Per-element sizes.  */
    sexp_format_n += 6;

  if (include_flags)
    /* Add flags.  */
    for (i = 0; i < DIM (ac_flags); i++)
      if (flags & ac_flags[i].number)
	sexp_format_n += strlen (ac_flags[i].string) + 1;

  /* Done.  */
  sexp_format = gcry_malloc (sexp_format_n);
  if (! sexp_format)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Construct the format string.  */

  *sexp_format = 0;
  strcat (sexp_format, "(");
  strcat (sexp_format, identifier);
  if (include_flags)
    {
      strcat (sexp_format, "(flags");
      for (i = 0; i < DIM (ac_flags); i++)
	if (flags & ac_flags[i].number)
	  {
	    strcat (sexp_format, " ");
	    strcat (sexp_format, ac_flags[i].string);
	  }
      strcat (sexp_format, ")");
    }
  if (algorithm)
    {
      strcat (sexp_format, "(");
      strcat (sexp_format, algorithm);
    }
  for (i = 0; i < data_length; i++)
    strcat (sexp_format, "(%s%m)");
  if (algorithm)
    strcat (sexp_format, ")");
  strcat (sexp_format, ")");

  /* Create final S-expression.  */
  err = gcry_sexp_build_array (&sexp_new, NULL, sexp_format, arg_list);
  if (err)
    goto out;

  *sexp = sexp_new;

 out:

  /* Deallocate resources.  */
  gcry_free (sexp_format);
  gcry_free (arg_list);
  if (err)
    gcry_sexp_release (sexp_new);

  return err;
}



/*
 * Handle management.
 */

/* Creates a new handle for the algorithm ALGORITHM and stores it in
   HANDLE.  FLAGS is not used yet.  */
gcry_error_t
_gcry_ac_open (gcry_ac_handle_t *handle,
	       gcry_ac_id_t algorithm, unsigned int flags)
{
  gcry_ac_handle_t handle_new;
  const char *algorithm_name;
  gcry_module_t module;
  gcry_error_t err;

  *handle = NULL;
  module = NULL;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  /* Get name.  */
  algorithm_name = _gcry_pk_aliased_algo_name (algorithm);
  if (! algorithm_name)
    {
      err = gcry_error (GPG_ERR_PUBKEY_ALGO);
      goto out;
    }

  /* Acquire reference to the pubkey module.  */
  err = _gcry_pk_module_lookup (algorithm, &module);
  if (err)
    goto out;
  
  /* Allocate.  */
  handle_new = gcry_malloc (sizeof (*handle_new));
  if (! handle_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Done.  */
  handle_new->algorithm = algorithm;
  handle_new->algorithm_name = algorithm_name;
  handle_new->flags = flags;
  handle_new->module = module;
  *handle = handle_new;

 out:
  
  /* Deallocate resources.  */
  if (err)
    _gcry_pk_module_release (module);

  return err;
}


/* Destroys the handle HANDLE.  */
void
_gcry_ac_close (gcry_ac_handle_t handle)
{
  /* Release reference to pubkey module.  */
  if (handle)
    {
      _gcry_pk_module_release (handle->module);
      gcry_free (handle);
    }
}



/* 
 * Key management.
 */

/* Initialize a key from a given data set.  */
/* FIXME/Damn: the argument HANDLE is not only unnecessary, it is
   completely WRONG here.  */
gcry_error_t
_gcry_ac_key_init (gcry_ac_key_t *key, gcry_ac_handle_t handle,
		   gcry_ac_key_type_t type, gcry_ac_data_t data)
{
  gcry_ac_data_t data_new;
  gcry_ac_key_t key_new;
  gcry_error_t err;

  (void)handle;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  /* Allocate.  */
  key_new = gcry_malloc (sizeof (*key_new));
  if (! key_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Copy data set.  */
  err = _gcry_ac_data_copy (&data_new, data);
  if (err)
    goto out;

  /* Done.  */
  key_new->data = data_new;
  key_new->type = type;
  *key = key_new;

 out:

  if (err)
    /* Deallocate resources.  */
    gcry_free (key_new);

  return err;
}


/* Generates a new key pair via the handle HANDLE of NBITS bits and
   stores it in KEY_PAIR.  In case non-standard settings are wanted, a
   pointer to a structure of type gcry_ac_key_spec_<algorithm>_t,
   matching the selected algorithm, can be given as KEY_SPEC.
   MISC_DATA is not used yet.  */
gcry_error_t
_gcry_ac_key_pair_generate (gcry_ac_handle_t handle, unsigned int nbits,
			    void *key_spec,
			    gcry_ac_key_pair_t *key_pair,
			    gcry_mpi_t **misc_data)
{
  gcry_sexp_t genkey_sexp_request;
  gcry_sexp_t genkey_sexp_reply;
  gcry_ac_data_t key_data_secret;
  gcry_ac_data_t key_data_public;
  gcry_ac_key_pair_t key_pair_new;
  gcry_ac_key_t key_secret;
  gcry_ac_key_t key_public;
  gcry_sexp_t key_sexp;
  gcry_error_t err;
  char *genkey_format;
  size_t genkey_format_n;
  void **arg_list;
  size_t arg_list_n;
  unsigned int i;
  unsigned int j;

  (void)misc_data;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  key_data_secret = NULL;
  key_data_public = NULL;
  key_secret = NULL;
  key_public = NULL;
  genkey_format = NULL;
  arg_list = NULL;
  genkey_sexp_request = NULL;
  genkey_sexp_reply = NULL;
  key_sexp = NULL;

  /* Allocate key pair.  */
  key_pair_new = gcry_malloc (sizeof (struct gcry_ac_key_pair));
  if (! key_pair_new)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Allocate keys.  */
  key_secret = gcry_malloc (sizeof (*key_secret));
  if (! key_secret)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
  key_public = gcry_malloc (sizeof (*key_public));
  if (! key_public)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Calculate size of the format string, that is used for creating
     the request S-expression.  */
  genkey_format_n = 22;

  /* Respect any relevant algorithm specific commands.  */
  if (key_spec)
    for (i = 0; i < DIM (ac_key_generate_specs); i++)
      if (handle->algorithm == ac_key_generate_specs[i].algorithm)
	genkey_format_n += 6;

  /* Create format string.  */
  genkey_format = gcry_malloc (genkey_format_n);
  if (! genkey_format)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Fill format string.  */
  *genkey_format = 0;
  strcat (genkey_format, "(genkey(%s(nbits%d)");
  if (key_spec)
    for (i = 0; i < DIM (ac_key_generate_specs); i++)
      if (handle->algorithm == ac_key_generate_specs[i].algorithm)
	strcat (genkey_format, "(%s%m)");
  strcat (genkey_format, "))");

  /* Build list of argument pointers, the algorithm name and the nbits
     are always needed.  */
  arg_list_n = 2;

  /* Now the algorithm specific arguments.  */
  if (key_spec)
    for (i = 0; i < DIM (ac_key_generate_specs); i++)
      if (handle->algorithm == ac_key_generate_specs[i].algorithm)
	arg_list_n += 2;

  /* Allocate list.  */
  arg_list = gcry_malloc (sizeof (*arg_list) * arg_list_n);
  if (! arg_list)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  arg_list[0] = (void *) &handle->algorithm_name;
  arg_list[1] = (void *) &nbits;
  if (key_spec)
    for (j = 2, i = 0; i < DIM (ac_key_generate_specs); i++)
      if (handle->algorithm == ac_key_generate_specs[i].algorithm)
	{
	  /* Add name of this specification flag and the
	     according member of the spec strucuture.  */
	  arg_list[j++] = (void *)(&ac_key_generate_specs[i].name);
	  arg_list[j++] = (void *)
	    (((char *) key_spec)
	     + ac_key_generate_specs[i].offset);
	  /* FIXME: above seems to suck.  */
	}

  /* Construct final request S-expression.  */
  err = gcry_sexp_build_array (&genkey_sexp_request,
			       NULL, genkey_format, arg_list);
  if (err)
    goto out;

  /* Perform genkey operation.  */
  err = gcry_pk_genkey (&genkey_sexp_reply, genkey_sexp_request);
  if (err)
    goto out;

  key_sexp = gcry_sexp_find_token (genkey_sexp_reply, "private-key", 0);
  if (! key_sexp)
    {
      err = gcry_error (GPG_ERR_INTERNAL);
      goto out;
    }
  err = ac_data_extract ("private-key", handle->algorithm_name,
			 key_sexp, &key_data_secret);
  if (err)
    goto out;

  gcry_sexp_release (key_sexp);
  key_sexp = gcry_sexp_find_token (genkey_sexp_reply, "public-key", 0);
  if (! key_sexp)
    {
      err = gcry_error (GPG_ERR_INTERNAL);
      goto out;
    }
  err = ac_data_extract ("public-key", handle->algorithm_name,
			 key_sexp, &key_data_public);
  if (err)
    goto out;

  /* Done.  */

  key_secret->type = GCRY_AC_KEY_SECRET;
  key_secret->data = key_data_secret;
  key_public->type = GCRY_AC_KEY_PUBLIC;
  key_public->data = key_data_public;
  key_pair_new->secret = key_secret;
  key_pair_new->public = key_public;
  *key_pair = key_pair_new;

 out:

  /* Deallocate resources.  */
  
  gcry_free (genkey_format);
  gcry_free (arg_list);
  gcry_sexp_release (genkey_sexp_request);
  gcry_sexp_release (genkey_sexp_reply);
  gcry_sexp_release (key_sexp);
  if (err)
    {
      _gcry_ac_data_destroy (key_data_secret);
      _gcry_ac_data_destroy (key_data_public);
      gcry_free (key_secret);
      gcry_free (key_public);
      gcry_free (key_pair_new);
    }

  return err;
}

/* Returns the key of type WHICH out of the key pair KEY_PAIR.  */
gcry_ac_key_t
_gcry_ac_key_pair_extract (gcry_ac_key_pair_t key_pair, 
                           gcry_ac_key_type_t which)
{
  gcry_ac_key_t key;

  if (fips_mode ())
    return NULL;

  switch (which)
    {
    case GCRY_AC_KEY_SECRET:
      key = key_pair->secret;
      break;

    case GCRY_AC_KEY_PUBLIC:
      key = key_pair->public;
      break;

    default:
      key = NULL;
      break;
    }

  return key;
}

/* Destroys the key KEY.  */
void
_gcry_ac_key_destroy (gcry_ac_key_t key)
{
  unsigned int i;

  if (key)
    {
      if (key->data)
        {
          for (i = 0; i < key->data->data_n; i++)
            {
              if (key->data->data[i].mpi)
                gcry_mpi_release (key->data->data[i].mpi);
              if (key->data->data[i].name)
                gcry_free (key->data->data[i].name);
            }
          gcry_free (key->data->data);
          gcry_free (key->data);
        }
      gcry_free (key);
    }
}

/* Destroys the key pair KEY_PAIR.  */
void
_gcry_ac_key_pair_destroy (gcry_ac_key_pair_t key_pair)
{
  if (key_pair)
    {
      gcry_ac_key_destroy (key_pair->secret);
      gcry_ac_key_destroy (key_pair->public);
      gcry_free (key_pair);
    }
}

/* Returns the data set contained in the key KEY.  */
gcry_ac_data_t
_gcry_ac_key_data_get (gcry_ac_key_t key)
{
  if (fips_mode ())
    return NULL;
  return key->data;
}

/* Verifies that the key KEY is sane via HANDLE.  */
gcry_error_t
_gcry_ac_key_test (gcry_ac_handle_t handle, gcry_ac_key_t key)
{
  gcry_sexp_t key_sexp;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  key_sexp = NULL;
  err = ac_data_construct (ac_key_identifiers[key->type], 0, 0,
			   handle->algorithm_name, key->data, &key_sexp);
  if (err)
    goto out;

  err = gcry_pk_testkey (key_sexp);

 out:

  gcry_sexp_release (key_sexp);

  return gcry_error (err);
}

/* Stores the number of bits of the key KEY in NBITS via HANDLE.  */
gcry_error_t
_gcry_ac_key_get_nbits (gcry_ac_handle_t handle,
			gcry_ac_key_t key, unsigned int *nbits)
{
  gcry_sexp_t key_sexp;
  gcry_error_t err;
  unsigned int n;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  key_sexp = NULL;

  err = ac_data_construct (ac_key_identifiers[key->type],
			   0, 0, handle->algorithm_name, key->data, &key_sexp);
  if (err)
    goto out;

  n = gcry_pk_get_nbits (key_sexp);
  if (! n)
    {
      err = gcry_error (GPG_ERR_PUBKEY_ALGO);
      goto out;
    }

  *nbits = n;

 out:

  gcry_sexp_release (key_sexp);

  return err;
}

/* Writes the 20 byte long key grip of the key KEY to KEY_GRIP via
   HANDLE.  */
gcry_error_t
_gcry_ac_key_get_grip (gcry_ac_handle_t handle,
		       gcry_ac_key_t key, unsigned char *key_grip)
{
  gcry_sexp_t key_sexp;
  gcry_error_t err;
  unsigned char *ret;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  key_sexp = NULL;
  err = ac_data_construct (ac_key_identifiers[key->type], 0, 0,
			   handle->algorithm_name, key->data, &key_sexp);
  if (err)
    goto out;

  ret = gcry_pk_get_keygrip (key_sexp, key_grip);
  if (! ret)
    {
      err = gcry_error (GPG_ERR_INV_OBJ);
      goto out;
    }

  err = 0;

 out:

  gcry_sexp_release (key_sexp);

  return err;
}




/* 
 * Functions performing cryptographic operations.
 */

/* Encrypts the plain text MPI value DATA_PLAIN with the key public
   KEY under the control of the flags FLAGS and stores the resulting
   data set into DATA_ENCRYPTED.  */
gcry_error_t
_gcry_ac_data_encrypt (gcry_ac_handle_t handle,
		       unsigned int flags,
		       gcry_ac_key_t key,
		       gcry_mpi_t data_plain,
		       gcry_ac_data_t *data_encrypted)
{
  gcry_ac_data_t data_encrypted_new;
  gcry_ac_data_t data_value;
  gcry_sexp_t sexp_request;
  gcry_sexp_t sexp_reply;
  gcry_sexp_t sexp_key;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_encrypted_new = NULL;
  sexp_request = NULL;
  sexp_reply = NULL;
  data_value = NULL;
  sexp_key = NULL;

  if (key->type != GCRY_AC_KEY_PUBLIC)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  err = ac_data_construct (ac_key_identifiers[key->type], 0, 0,
			   handle->algorithm_name, key->data, &sexp_key);
  if (err)
    goto out;

  err = _gcry_ac_data_new (&data_value);
  if (err)
    goto out;

  err = _gcry_ac_data_set (data_value, 0, "value", data_plain);
  if (err)
    goto out;

  err = ac_data_construct ("data", 1, flags, handle->algorithm_name,
			   data_value, &sexp_request);
  if (err)
    goto out;

  /* FIXME: error vs. errcode? */

  err = gcry_pk_encrypt (&sexp_reply, sexp_request, sexp_key);
  if (err)
    goto out;

  /* Extract data.  */
  err = ac_data_extract ("enc-val", handle->algorithm_name,
			 sexp_reply, &data_encrypted_new);
  if (err)
    goto out;

  *data_encrypted = data_encrypted_new;

 out:

  /* Deallocate resources.  */

  gcry_sexp_release (sexp_request);
  gcry_sexp_release (sexp_reply);
  gcry_sexp_release (sexp_key);
  _gcry_ac_data_destroy (data_value);

  return err;
}

/* Decrypts the encrypted data contained in the data set
   DATA_ENCRYPTED with the secret key KEY under the control of the
   flags FLAGS and stores the resulting plain text MPI value in
   DATA_PLAIN.  */
gcry_error_t
_gcry_ac_data_decrypt (gcry_ac_handle_t handle,
		       unsigned int flags,
		       gcry_ac_key_t key,
		       gcry_mpi_t *data_plain,
		       gcry_ac_data_t data_encrypted)
{
  gcry_mpi_t data_decrypted;
  gcry_sexp_t sexp_request;
  gcry_sexp_t sexp_reply;
  gcry_sexp_t sexp_value;
  gcry_sexp_t sexp_key;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  sexp_request = NULL;
  sexp_reply = NULL;
  sexp_value = NULL;
  sexp_key = NULL;

  if (key->type != GCRY_AC_KEY_SECRET)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  err = ac_data_construct (ac_key_identifiers[key->type], 0, 0,
			   handle->algorithm_name, key->data, &sexp_key);
  if (err)
    goto out;

  /* Create S-expression from data.  */
  err = ac_data_construct ("enc-val", 1, flags, handle->algorithm_name,
			   data_encrypted, &sexp_request);
  if (err)
    goto out;

  /* Decrypt.  */
  err = gcry_pk_decrypt (&sexp_reply, sexp_request, sexp_key);
  if (err)
    goto out;

  /* Extract plain text. */
  sexp_value = gcry_sexp_find_token (sexp_reply, "value", 0);
  if (! sexp_value)
    {
      /* FIXME?  */
      err = gcry_error (GPG_ERR_GENERAL);
      goto out;
    }

  data_decrypted = gcry_sexp_nth_mpi (sexp_value, 1, GCRYMPI_FMT_USG);
  if (! data_decrypted)
    {
      err = gcry_error (GPG_ERR_GENERAL);
      goto out;
    }

  *data_plain = data_decrypted;

 out:

  /* Deallocate resources.  */
  gcry_sexp_release (sexp_request);
  gcry_sexp_release (sexp_reply);
  gcry_sexp_release (sexp_value);
  gcry_sexp_release (sexp_key);

  return gcry_error (err);

}

/* Signs the data contained in DATA with the secret key KEY and stores
   the resulting signature data set in DATA_SIGNATURE.  */
gcry_error_t
_gcry_ac_data_sign (gcry_ac_handle_t handle,
		    gcry_ac_key_t key,
		    gcry_mpi_t data,
		    gcry_ac_data_t *data_signature)
{
  gcry_ac_data_t data_signed;
  gcry_ac_data_t data_value;
  gcry_sexp_t sexp_request;
  gcry_sexp_t sexp_reply;
  gcry_sexp_t sexp_key;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_signed = NULL;
  data_value = NULL;
  sexp_request = NULL;
  sexp_reply = NULL;
  sexp_key = NULL;

  if (key->type != GCRY_AC_KEY_SECRET)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  err = ac_data_construct (ac_key_identifiers[key->type], 0, 0,
			   handle->algorithm_name, key->data, &sexp_key);
  if (err)
    goto out;

  err = _gcry_ac_data_new (&data_value);
  if (err)
    goto out;

  err = _gcry_ac_data_set (data_value, 0, "value", data);
  if (err)
    goto out;

  /* Create S-expression holding the data.  */
  err = ac_data_construct ("data", 1, 0, NULL, data_value, &sexp_request);
  if (err)
    goto out;

  /* Sign.  */
  err = gcry_pk_sign (&sexp_reply, sexp_request, sexp_key);
  if (err)
    goto out;

  /* Extract data.  */
  err = ac_data_extract ("sig-val", handle->algorithm_name,
			 sexp_reply, &data_signed);
  if (err)
    goto out;

  /* Done.  */
  *data_signature = data_signed;

 out:

  gcry_sexp_release (sexp_request);
  gcry_sexp_release (sexp_reply);
  gcry_sexp_release (sexp_key);
  _gcry_ac_data_destroy (data_value);

  return gcry_error (err);
}


/* Verifies that the signature contained in the data set
   DATA_SIGNATURE is indeed the result of signing the data contained
   in DATA with the secret key belonging to the public key KEY.  */
gcry_error_t
_gcry_ac_data_verify (gcry_ac_handle_t handle,
		      gcry_ac_key_t key,
		      gcry_mpi_t data,
		      gcry_ac_data_t data_signature)
{
  gcry_sexp_t sexp_signature;
  gcry_ac_data_t data_value;
  gcry_sexp_t sexp_data;
  gcry_sexp_t sexp_key;
  gcry_error_t err;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  sexp_signature = NULL;
  data_value = NULL;
  sexp_data = NULL;
  sexp_key = NULL;

  err = ac_data_construct ("public-key", 0, 0,
			   handle->algorithm_name, key->data, &sexp_key);
  if (err)
    goto out;

  if (key->type != GCRY_AC_KEY_PUBLIC)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  /* Construct S-expression holding the signature data.  */
  err = ac_data_construct ("sig-val", 1, 0, handle->algorithm_name,
			   data_signature, &sexp_signature);
  if (err)
    goto out;

  err = _gcry_ac_data_new (&data_value);
  if (err)
    goto out;

  err = _gcry_ac_data_set (data_value, 0, "value", data);
  if (err)
    goto out;

  /* Construct S-expression holding the data.  */
  err = ac_data_construct ("data", 1, 0, NULL, data_value, &sexp_data);
  if (err)
    goto out;

  /* Verify signature.  */
  err = gcry_pk_verify (sexp_signature, sexp_data, sexp_key);

 out:

  gcry_sexp_release (sexp_signature);
  gcry_sexp_release (sexp_data);
  gcry_sexp_release (sexp_key);
  _gcry_ac_data_destroy (data_value);

  return gcry_error (err);
}




/*
 * Implementation of encoding methods (em).
 */

/* Type for functions that encode or decode (hence the name) a
   message.  */
typedef gcry_error_t (*gcry_ac_em_dencode_t) (unsigned int flags,
						 void *options,
						 gcry_ac_io_t *ac_io_read,
						 gcry_ac_io_t *ac_io_write);

/* Fill the buffer BUFFER which is BUFFER_N bytes long with non-zero
   random bytes of random level LEVEL.  */
static void
em_randomize_nonzero (unsigned char *buffer, size_t buffer_n,
		      gcry_random_level_t level)
{
  unsigned char *buffer_rand;
  unsigned int buffer_rand_n;
  unsigned int zeros;
  unsigned int i;
  unsigned int j;

  for (i = 0; i < buffer_n; i++)
    buffer[i] = 0;
  
  do
    {
      /* Count zeros.  */
      for (i = zeros = 0; i < buffer_n; i++)
	if (! buffer[i])
	  zeros++;

      if (zeros)
	{
	  /* Get random bytes.  */
	  buffer_rand_n = zeros + (zeros / 128);
	  buffer_rand = gcry_random_bytes_secure (buffer_rand_n, level);

	  /* Substitute zeros with non-zero random bytes.  */
	  for (i = j = 0; zeros && (i < buffer_n) && (j < buffer_rand_n); i++)
	    if (! buffer[i])
	      {
		while ((j < buffer_rand_n) && (! buffer_rand[j]))
		  j++;
		if (j < buffer_rand_n)
		  {
		    buffer[i] = buffer_rand[j++];
		    zeros--;
		  }
		else
		  break;
	      }
	  gcry_free (buffer_rand);
	}
    }
  while (zeros);
}

/* Encode a message according to the Encoding Method for Encryption
   `PKCS-V1_5' (EME-PKCS-V1_5).  */
static gcry_error_t
eme_pkcs_v1_5_encode (unsigned int flags, void *opts,
		      gcry_ac_io_t *ac_io_read,
		      gcry_ac_io_t *ac_io_write)
{
  gcry_ac_eme_pkcs_v1_5_t *options;
  gcry_error_t err;
  unsigned char *buffer;
  unsigned char *ps;
  unsigned char *m;
  size_t m_n;
  unsigned int ps_n;
  unsigned int k;

  (void)flags;

  options = opts;
  buffer = NULL;
  m = NULL;

  err = _gcry_ac_io_read_all (ac_io_read, &m, &m_n);
  if (err)
    goto out;

  /* Figure out key length in bytes.  */
  k = options->key_size / 8;

  if (m_n > k - 11)
    {
      /* Key is too short for message.  */
      err = gcry_error (GPG_ERR_TOO_SHORT);
      goto out;
    }

  /* According to this encoding method, the first byte of the encoded
     message is zero.  This byte will be lost anyway, when the encoded
     message is to be converted into an MPI, that's why we skip
     it.  */

  /* Allocate buffer.  */
  buffer = gcry_malloc (k - 1);
  if (! buffer)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  /* Generate an octet string PS of length k - mLen - 3 consisting
     of pseudorandomly generated nonzero octets.  The length of PS
     will be at least eight octets.  */
  ps_n = k - m_n - 3;
  ps = buffer + 1;
  em_randomize_nonzero (ps, ps_n, GCRY_STRONG_RANDOM);

  /* Concatenate PS, the message M, and other padding to form an
     encoded message EM of length k octets as:

     EM = 0x00 || 0x02 || PS || 0x00 || M.  */

  buffer[0] = 0x02;
  buffer[ps_n + 1] = 0x00;
  memcpy (buffer + ps_n + 2, m, m_n);

  err = _gcry_ac_io_write (ac_io_write, buffer, k - 1);

 out:

  gcry_free (buffer);
  gcry_free (m);

  return err;
}

/* Decode a message according to the Encoding Method for Encryption
   `PKCS-V1_5' (EME-PKCS-V1_5).  */
static gcry_error_t
eme_pkcs_v1_5_decode (unsigned int flags, void *opts,
		      gcry_ac_io_t *ac_io_read,
		      gcry_ac_io_t *ac_io_write)
{
  gcry_ac_eme_pkcs_v1_5_t *options;
  unsigned char *buffer;
  unsigned char *em;
  size_t em_n;
  gcry_error_t err;
  unsigned int i;
  unsigned int k;

  (void)flags;

  options = opts;
  buffer = NULL;
  em = NULL;

  err = _gcry_ac_io_read_all (ac_io_read, &em, &em_n);
  if (err)
    goto out;

  /* Figure out key size.  */
  k = options->key_size / 8;

  /* Search for zero byte.  */
  for (i = 0; (i < em_n) && em[i]; i++);

  /* According to this encoding method, the first byte of the encoded
     message should be zero.  This byte is lost.  */

  if (! ((em_n >= 10)
	 && (em_n == (k - 1))
	 && (em[0] == 0x02)
	 && (i < em_n)
	 && ((i - 1) >= 8)))
    {
      err = gcry_error (GPG_ERR_DECRYPT_FAILED);
      goto out;
    }

  i++;
  buffer = gcry_malloc (em_n - i);
  if (! buffer)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  memcpy (buffer, em + i, em_n - i);
  err = _gcry_ac_io_write (ac_io_write, buffer, em_n - i);

 out:

  gcry_free (buffer);
  gcry_free (em);

  return err;
}

static gcry_error_t
emsa_pkcs_v1_5_encode_data_cb (void *opaque,
			       unsigned char *buffer, size_t buffer_n)
{
  gcry_md_hd_t md_handle;

  md_handle = opaque;
  gcry_md_write (md_handle, buffer, buffer_n);

  return 0;
}


/* Encode a message according to the Encoding Method for Signatures
   with Appendix `PKCS-V1_5' (EMSA-PKCS-V1_5).  */
static gcry_error_t
emsa_pkcs_v1_5_encode (unsigned int flags, void *opts,
		       gcry_ac_io_t *ac_io_read,
		       gcry_ac_io_t *ac_io_write)
{
  gcry_ac_emsa_pkcs_v1_5_t *options;
  gcry_error_t err;
  gcry_md_hd_t md;
  unsigned char *t;
  size_t t_n;
  unsigned char *h;
  size_t h_n;
  unsigned char *ps;
  size_t ps_n;
  unsigned char *buffer;
  size_t buffer_n;
  unsigned char asn[100];	/* FIXME, always enough?  */
  size_t asn_n;
  unsigned int i;

  (void)flags;
  
  options = opts;
  buffer = NULL;
  md = NULL;
  ps = NULL;
  t = NULL;

  /* Create hashing handle and get the necessary information.  */
  err = gcry_md_open (&md, options->md, 0);
  if (err)
    goto out;

  asn_n = DIM (asn);
  err = gcry_md_algo_info (options->md, GCRYCTL_GET_ASNOID, asn, &asn_n);
  if (err)
    goto out;

  h_n = gcry_md_get_algo_dlen (options->md);

  err = _gcry_ac_io_process (ac_io_read, emsa_pkcs_v1_5_encode_data_cb, md);
  if (err)
    goto out;

  h = gcry_md_read (md, 0);

  /* Encode the algorithm ID for the hash function and the hash value
     into an ASN.1 value of type DigestInfo with the Distinguished
     Encoding Rules (DER), where the type DigestInfo has the syntax:

     DigestInfo ::== SEQUENCE {
     digestAlgorithm AlgorithmIdentifier,
     digest OCTET STRING
     }

     The first field identifies the hash function and the second
     contains the hash value.  Let T be the DER encoding of the
     DigestInfo value and let tLen be the length in octets of T.  */

  t_n = asn_n + h_n;
  t = gcry_malloc (t_n);
  if (! t)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  for (i = 0; i < asn_n; i++)
    t[i] = asn[i];
  for (i = 0; i < h_n; i++)
    t[asn_n + i] = h[i];

  /* If emLen < tLen + 11, output "intended encoded message length
     too short" and stop.  */
  if (options->em_n < t_n + 11)
    {
      err = gcry_error (GPG_ERR_TOO_SHORT);
      goto out;
    }

  /* Generate an octet string PS consisting of emLen - tLen - 3 octets
     with hexadecimal value 0xFF.  The length of PS will be at least 8
     octets.  */
  ps_n = options->em_n - t_n - 3;
  ps = gcry_malloc (ps_n);
  if (! ps)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
  for (i = 0; i < ps_n; i++)
    ps[i] = 0xFF;

  /* Concatenate PS, the DER encoding T, and other padding to form the
     encoded message EM as:

     EM = 0x00 || 0x01 || PS || 0x00 || T.  */

  buffer_n = ps_n + t_n + 3;
  buffer = gcry_malloc (buffer_n);
  if (! buffer)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }

  buffer[0] = 0x00;
  buffer[1] = 0x01;
  for (i = 0; i < ps_n; i++)
    buffer[2 + i] = ps[i];
  buffer[2 + ps_n] = 0x00;
  for (i = 0; i < t_n; i++)
    buffer[3 + ps_n + i] = t[i];

  err = _gcry_ac_io_write (ac_io_write, buffer, buffer_n);

 out:

  gcry_md_close (md);

  gcry_free (buffer);
  gcry_free (ps);
  gcry_free (t);

  return err;
}

/* `Actions' for data_dencode().  */
typedef enum dencode_action
  {
    DATA_ENCODE,
    DATA_DECODE,
  }
dencode_action_t;

/* Encode or decode a message according to the the encoding method
   METHOD; ACTION specifies wether the message that is contained in
   BUFFER_IN and of length BUFFER_IN_N should be encoded or decoded.
   The resulting message will be stored in a newly allocated buffer in
   BUFFER_OUT and BUFFER_OUT_N.  */
static gcry_error_t
ac_data_dencode (gcry_ac_em_t method, dencode_action_t action,
		 unsigned int flags, void *options,
		 gcry_ac_io_t *ac_io_read,
		 gcry_ac_io_t *ac_io_write)
{
  struct
  {
    gcry_ac_em_t method;
    gcry_ac_em_dencode_t encode;
    gcry_ac_em_dencode_t decode;
  } methods[] =
    {
      { GCRY_AC_EME_PKCS_V1_5,
	eme_pkcs_v1_5_encode, eme_pkcs_v1_5_decode },
      { GCRY_AC_EMSA_PKCS_V1_5,
	emsa_pkcs_v1_5_encode, NULL },
    };
  size_t methods_n;
  gcry_error_t err;
  unsigned int i;

  methods_n = sizeof (methods) / sizeof (*methods);

  for (i = 0; i < methods_n; i++)
    if (methods[i].method == method)
      break;
  if (i == methods_n)
    {
      err = gcry_error (GPG_ERR_NOT_FOUND);	/* FIXME? */
      goto out;
    }

  err = 0;
  switch (action)
    {
    case DATA_ENCODE:
      if (methods[i].encode)
	/* FIXME? */
	err = (*methods[i].encode) (flags, options, ac_io_read, ac_io_write);
      break;

    case DATA_DECODE:
      if (methods[i].decode)
	/* FIXME? */
	err = (*methods[i].decode) (flags, options, ac_io_read, ac_io_write);
      break;

    default:
      err = gcry_error (GPG_ERR_INV_ARG);
      break;
    }

 out:

  return err;
}

/* Encode a message according to the encoding method METHOD.  OPTIONS
   must be a pointer to a method-specific structure
   (gcry_ac_em*_t).  */
gcry_error_t
_gcry_ac_data_encode (gcry_ac_em_t method,
		      unsigned int flags, void *options,
		      gcry_ac_io_t *ac_io_read,
		      gcry_ac_io_t *ac_io_write)
{
  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  return ac_data_dencode (method, DATA_ENCODE, flags, options,
			  ac_io_read, ac_io_write);
}

/* Dencode a message according to the encoding method METHOD.  OPTIONS
   must be a pointer to a method-specific structure
   (gcry_ac_em*_t).  */
gcry_error_t
_gcry_ac_data_decode (gcry_ac_em_t method,
		      unsigned int flags, void *options,
		      gcry_ac_io_t *ac_io_read,
		      gcry_ac_io_t *ac_io_write)
{
  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  return ac_data_dencode (method, DATA_DECODE, flags, options,
			  ac_io_read, ac_io_write);
}

/* Convert an MPI into an octet string.  */
void
_gcry_ac_mpi_to_os (gcry_mpi_t mpi, unsigned char *os, size_t os_n)
{
  unsigned long digit;
  gcry_mpi_t base;
  unsigned int i;
  unsigned int n;
  gcry_mpi_t m;
  gcry_mpi_t d;

  if (fips_mode ())
    return;

  base = gcry_mpi_new (0);
  gcry_mpi_set_ui (base, 256);

  n = 0;
  m = gcry_mpi_copy (mpi);
  while (gcry_mpi_cmp_ui (m, 0))
    {
      n++;
      gcry_mpi_div (m, NULL, m, base, 0);
    }

  gcry_mpi_set (m, mpi);
  d = gcry_mpi_new (0);
  for (i = 0; (i < n) && (i < os_n); i++)
    {
      gcry_mpi_mod (d, m, base);
      _gcry_mpi_get_ui (d, &digit);
      gcry_mpi_div (m, NULL, m, base, 0);
      os[os_n - i - 1] = (digit & 0xFF);
    }

  for (; i < os_n; i++)
    os[os_n - i - 1] = 0;

  gcry_mpi_release (base);
  gcry_mpi_release (d);
  gcry_mpi_release (m);
}

/* Convert an MPI into an newly allocated octet string.  */
gcry_error_t
_gcry_ac_mpi_to_os_alloc (gcry_mpi_t mpi, unsigned char **os, size_t *os_n)
{
  unsigned char *buffer;
  size_t buffer_n;
  gcry_error_t err;
  unsigned int nbits;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  nbits = gcry_mpi_get_nbits (mpi);
  buffer_n = (nbits + 7) / 8;
  buffer = gcry_malloc (buffer_n);
  if (! buffer)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
      
  _gcry_ac_mpi_to_os (mpi, buffer, buffer_n);
  *os = buffer;
  *os_n = buffer_n;
  err = 0;

 out:

  return err;
}


/* Convert an octet string into an MPI.  */
void
_gcry_ac_os_to_mpi (gcry_mpi_t mpi, unsigned char *os, size_t os_n)
{
  unsigned int i;
  gcry_mpi_t xi;
  gcry_mpi_t x;
  gcry_mpi_t a;
  
  if (fips_mode ())
    return;

  a = gcry_mpi_new (0);
  gcry_mpi_set_ui (a, 1);
  x = gcry_mpi_new (0);
  gcry_mpi_set_ui (x, 0);
  xi = gcry_mpi_new (0);

  for (i = 0; i < os_n; i++)
    {
      gcry_mpi_mul_ui (xi, a, os[os_n - i - 1]);
      gcry_mpi_add (x, x, xi);
      gcry_mpi_mul_ui (a, a, 256);
    }
      
  gcry_mpi_release (xi);
  gcry_mpi_release (a);

  gcry_mpi_set (mpi, x);
  gcry_mpi_release (x);		/* FIXME: correct? */
}



/* 
 * Implementation of Encryption Schemes (ES) and Signature Schemes
 * with Appendix (SSA).
 */

/* Schemes consist of two things: encoding methods and cryptographic
   primitives.

   Since encoding methods are accessible through a common API with
   method-specific options passed as an anonymous struct, schemes have
   to provide functions that construct this method-specific structure;
   this is what the functions of type `gcry_ac_dencode_prepare_t' are
   there for.  */

typedef gcry_error_t (*gcry_ac_dencode_prepare_t) (gcry_ac_handle_t handle,
						   gcry_ac_key_t key,
						   void *opts,
						   void *opts_em);

/* The `dencode_prepare' function for ES-PKCS-V1_5.  */
static gcry_error_t
ac_es_dencode_prepare_pkcs_v1_5 (gcry_ac_handle_t handle, gcry_ac_key_t key,
				 void *opts, void *opts_em)
{
  gcry_ac_eme_pkcs_v1_5_t *options_em;
  unsigned int nbits;
  gcry_error_t err;

  (void)opts;

  err = _gcry_ac_key_get_nbits (handle, key, &nbits);
  if (err)
    goto out;

  options_em = opts_em;
  options_em->key_size = nbits;

 out:

  return err;
}

/* The `dencode_prepare' function for SSA-PKCS-V1_5.  */
static gcry_error_t
ac_ssa_dencode_prepare_pkcs_v1_5 (gcry_ac_handle_t handle, gcry_ac_key_t key,
				  void *opts, void *opts_em)
{
  gcry_ac_emsa_pkcs_v1_5_t *options_em;
  gcry_ac_ssa_pkcs_v1_5_t *options;
  gcry_error_t err;
  unsigned int k;

  options_em = opts_em;
  options = opts;

  err = _gcry_ac_key_get_nbits (handle, key, &k);
  if (err)
    goto out;

  k = (k + 7) / 8;
  options_em->md = options->md;
  options_em->em_n = k;

 out:

  return err;
}

/* Type holding the information about each supported
   Encryption/Signature Scheme.  */
typedef struct ac_scheme
{
  gcry_ac_scheme_t scheme;
  gcry_ac_em_t scheme_encoding;
  gcry_ac_dencode_prepare_t dencode_prepare;
  size_t options_em_n;
} ac_scheme_t;

/* List of supported Schemes.  */
static ac_scheme_t ac_schemes[] =
  {
    { GCRY_AC_ES_PKCS_V1_5, GCRY_AC_EME_PKCS_V1_5,
      ac_es_dencode_prepare_pkcs_v1_5,
      sizeof (gcry_ac_eme_pkcs_v1_5_t) },
    { GCRY_AC_SSA_PKCS_V1_5, GCRY_AC_EMSA_PKCS_V1_5,
      ac_ssa_dencode_prepare_pkcs_v1_5,
      sizeof (gcry_ac_emsa_pkcs_v1_5_t) }
  };

/* Lookup a scheme by it's ID.  */
static ac_scheme_t *
ac_scheme_get (gcry_ac_scheme_t scheme)
{
  ac_scheme_t *ac_scheme;
  unsigned int i;

  for (i = 0; i < DIM (ac_schemes); i++)
    if (scheme == ac_schemes[i].scheme)
      break;
  if (i == DIM (ac_schemes))
    ac_scheme = NULL;
  else
    ac_scheme = ac_schemes + i;

  return ac_scheme;
}

/* Prepares the encoding/decoding by creating an according option
   structure.  */
static gcry_error_t
ac_dencode_prepare (gcry_ac_handle_t handle, gcry_ac_key_t key, void *opts,
		    ac_scheme_t scheme, void **opts_em)
{
  gcry_error_t err;
  void *options_em;

  options_em = gcry_malloc (scheme.options_em_n);
  if (! options_em)
    {
      err = gcry_error_from_errno (errno);
      goto out;
    }
  
  err = (*scheme.dencode_prepare) (handle, key, opts, options_em);
  if (err)
    goto out;

  *opts_em = options_em;

 out:

  if (err)
    free (options_em);

  return err;
}

/* Convert a data set into a single MPI; currently, this is only
   supported for data sets containing a single MPI.  */
static gcry_error_t
ac_data_set_to_mpi (gcry_ac_data_t data, gcry_mpi_t *mpi)
{
  gcry_error_t err;
  gcry_mpi_t mpi_new;
  unsigned int elems;

  elems = _gcry_ac_data_length (data);

  if (elems != 1)
    {
      /* FIXME: I guess, we should be more flexible in this respect by
	 allowing the actual encryption/signature schemes to implement
	 this conversion mechanism.  */
      err = gcry_error (GPG_ERR_CONFLICT);
      goto out;
    }

  err = _gcry_ac_data_get_index (data, GCRY_AC_FLAG_COPY, 0, NULL, &mpi_new);
  if (err)
    goto out;

  *mpi = mpi_new;

 out:

  return err;
}

/* Encrypts the plain text message contained in M, which is of size
   M_N, with the public key KEY_PUBLIC according to the Encryption
   Scheme SCHEME_ID.  HANDLE is used for accessing the low-level
   cryptographic primitives.  If OPTS is not NULL, it has to be an
   anonymous structure specific to the chosen scheme (gcry_ac_es_*_t).
   The encrypted message will be stored in C and C_N.  */
gcry_error_t
_gcry_ac_data_encrypt_scheme (gcry_ac_handle_t handle,
			      gcry_ac_scheme_t scheme_id,
			      unsigned int flags, void *opts,
			      gcry_ac_key_t key,
			      gcry_ac_io_t *io_message,
			      gcry_ac_io_t *io_cipher)
{
  gcry_error_t err;
  gcry_ac_io_t io_em;
  unsigned char *em;
  size_t em_n;
  gcry_mpi_t mpi_plain;
  gcry_ac_data_t data_encrypted;
  gcry_mpi_t mpi_encrypted;
  unsigned char *buffer;
  size_t buffer_n;
  void *opts_em;
  ac_scheme_t *scheme;

  (void)flags;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_encrypted = NULL;
  mpi_encrypted = NULL;
  mpi_plain = NULL;
  opts_em = NULL;
  buffer = NULL;
  em = NULL;

  scheme = ac_scheme_get (scheme_id);
  if (! scheme)
    {
      err = gcry_error (GPG_ERR_NO_ENCRYPTION_SCHEME);
      goto out;
    }

  if (key->type != GCRY_AC_KEY_PUBLIC)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  err = ac_dencode_prepare (handle, key, opts, *scheme, &opts_em);
  if (err)
    goto out;

  _gcry_ac_io_init (&io_em, GCRY_AC_IO_WRITABLE,
		    GCRY_AC_IO_STRING, &em, &em_n);

  err = _gcry_ac_data_encode (scheme->scheme_encoding, 0, opts_em,
			      io_message, &io_em);
  if (err)
    goto out;

  mpi_plain = gcry_mpi_snew (0);
  gcry_ac_os_to_mpi (mpi_plain, em, em_n);

  err = _gcry_ac_data_encrypt (handle, 0, key, mpi_plain, &data_encrypted);
  if (err)
    goto out;

  err = ac_data_set_to_mpi (data_encrypted, &mpi_encrypted);
  if (err)
    goto out;

  err = _gcry_ac_mpi_to_os_alloc (mpi_encrypted, &buffer, &buffer_n);
  if (err)
    goto out;

  err = _gcry_ac_io_write (io_cipher, buffer, buffer_n);

 out:

  gcry_ac_data_destroy (data_encrypted);
  gcry_mpi_release (mpi_encrypted);
  gcry_mpi_release (mpi_plain);
  gcry_free (opts_em);
  gcry_free (buffer);
  gcry_free (em);

  return err;
}

/* Decryptes the cipher message contained in C, which is of size C_N,
   with the secret key KEY_SECRET according to the Encryption Scheme
   SCHEME_ID.  Handle is used for accessing the low-level
   cryptographic primitives.  If OPTS is not NULL, it has to be an
   anonymous structure specific to the chosen scheme (gcry_ac_es_*_t).
   The decrypted message will be stored in M and M_N.  */
gcry_error_t
_gcry_ac_data_decrypt_scheme (gcry_ac_handle_t handle,
			      gcry_ac_scheme_t scheme_id,
			      unsigned int flags, void *opts,
			      gcry_ac_key_t key,
			      gcry_ac_io_t *io_cipher,
			      gcry_ac_io_t *io_message)
{
  gcry_ac_io_t io_em;
  gcry_error_t err;
  gcry_ac_data_t data_encrypted;
  unsigned char *em;
  size_t em_n;
  gcry_mpi_t mpi_encrypted;
  gcry_mpi_t mpi_decrypted;
  void *opts_em;
  ac_scheme_t *scheme;
  char *elements_enc;
  size_t elements_enc_n;
  unsigned char *c;
  size_t c_n;

  (void)flags;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_encrypted = NULL;
  mpi_encrypted = NULL;
  mpi_decrypted = NULL;
  elements_enc = NULL;
  opts_em = NULL;
  em = NULL;
  c = NULL;

  scheme = ac_scheme_get (scheme_id);
  if (! scheme)
    {
      err = gcry_error (GPG_ERR_NO_ENCRYPTION_SCHEME);
      goto out;
    }

  if (key->type != GCRY_AC_KEY_SECRET)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  err = _gcry_ac_io_read_all (io_cipher, &c, &c_n);
  if (err)
    goto out;

  mpi_encrypted = gcry_mpi_snew (0);
  gcry_ac_os_to_mpi (mpi_encrypted, c, c_n);

  err = _gcry_pk_get_elements (handle->algorithm, &elements_enc, NULL);
  if (err)
    goto out;

  elements_enc_n = strlen (elements_enc);
  if (elements_enc_n != 1)
    {
      /* FIXME? */
      err = gcry_error (GPG_ERR_CONFLICT);
      goto out;
    }

  err = _gcry_ac_data_new (&data_encrypted);
  if (err)
    goto out;

  err = _gcry_ac_data_set (data_encrypted, GCRY_AC_FLAG_COPY | GCRY_AC_FLAG_DEALLOC,
			   elements_enc, mpi_encrypted);
  if (err)
    goto out;

  err = _gcry_ac_data_decrypt (handle, 0, key, &mpi_decrypted, data_encrypted);
  if (err)
    goto out;

  err = _gcry_ac_mpi_to_os_alloc (mpi_decrypted, &em, &em_n);
  if (err)
    goto out;

  err = ac_dencode_prepare (handle, key, opts, *scheme, &opts_em);
  if (err)
    goto out;

  _gcry_ac_io_init (&io_em, GCRY_AC_IO_READABLE,
		    GCRY_AC_IO_STRING, em, em_n);

  err = _gcry_ac_data_decode (scheme->scheme_encoding, 0, opts_em,
			      &io_em, io_message);
  if (err)
    goto out;

 out:
  
  _gcry_ac_data_destroy (data_encrypted);
  gcry_mpi_release (mpi_encrypted);
  gcry_mpi_release (mpi_decrypted);
  free (elements_enc);
  gcry_free (opts_em);
  gcry_free (em);
  gcry_free (c);

  return err;
}


/* Signs the message contained in M, which is of size M_N, with the
   secret key KEY according to the Signature Scheme SCHEME_ID.  Handle
   is used for accessing the low-level cryptographic primitives.  If
   OPTS is not NULL, it has to be an anonymous structure specific to
   the chosen scheme (gcry_ac_ssa_*_t).  The signed message will be
   stored in S and S_N.  */
gcry_error_t
_gcry_ac_data_sign_scheme (gcry_ac_handle_t handle,
			   gcry_ac_scheme_t scheme_id,
			   unsigned int flags, void *opts,
			   gcry_ac_key_t key,
			   gcry_ac_io_t *io_message,
			   gcry_ac_io_t *io_signature)
{
  gcry_ac_io_t io_em;
  gcry_error_t err;
  gcry_ac_data_t data_signed;
  unsigned char *em;
  size_t em_n;
  gcry_mpi_t mpi;
  void *opts_em;
  unsigned char *buffer;
  size_t buffer_n;
  gcry_mpi_t mpi_signed;
  ac_scheme_t *scheme;

  (void)flags;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  data_signed = NULL;
  mpi_signed = NULL;
  opts_em = NULL;
  buffer = NULL;
  mpi = NULL;
  em = NULL;

  if (key->type != GCRY_AC_KEY_SECRET)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  scheme = ac_scheme_get (scheme_id);
  if (! scheme)
    {
      /* FIXME: adjust api of scheme_get in respect to err codes.  */
      err = gcry_error (GPG_ERR_NO_SIGNATURE_SCHEME);
      goto out;
    }

  err = ac_dencode_prepare (handle, key, opts, *scheme, &opts_em);
  if (err)
    goto out;

  _gcry_ac_io_init (&io_em, GCRY_AC_IO_WRITABLE,
		    GCRY_AC_IO_STRING, &em, &em_n);

  err = _gcry_ac_data_encode (scheme->scheme_encoding, 0, opts_em,
			      io_message, &io_em);
  if (err)
    goto out;

  mpi = gcry_mpi_new (0);
  _gcry_ac_os_to_mpi (mpi, em, em_n);

  err = _gcry_ac_data_sign (handle, key, mpi, &data_signed);
  if (err)
    goto out;

  err = ac_data_set_to_mpi (data_signed, &mpi_signed);
  if (err)
    goto out;

  err = _gcry_ac_mpi_to_os_alloc (mpi_signed, &buffer, &buffer_n);
  if (err)
    goto out;

  err = _gcry_ac_io_write (io_signature, buffer, buffer_n);

 out:

  _gcry_ac_data_destroy (data_signed);
  gcry_mpi_release (mpi_signed);
  gcry_mpi_release (mpi);
  gcry_free (opts_em);
  gcry_free (buffer);
  gcry_free (em);

  return err;
}

/* Verifies that the signature contained in S, which is of length S_N,
   is indeed the result of signing the message contained in M, which
   is of size M_N, with the secret key belonging to the public key
   KEY_PUBLIC.  If OPTS is not NULL, it has to be an anonymous
   structure (gcry_ac_ssa_*_t) specific to the Signature Scheme, whose
   ID is contained in SCHEME_ID.  */
gcry_error_t
_gcry_ac_data_verify_scheme (gcry_ac_handle_t handle,
			     gcry_ac_scheme_t scheme_id,
			     unsigned int flags, void *opts,
			     gcry_ac_key_t key,
			     gcry_ac_io_t *io_message,
			     gcry_ac_io_t *io_signature)
{
  gcry_ac_io_t io_em;
  gcry_error_t err;
  gcry_ac_data_t data_signed;
  unsigned char *em;
  size_t em_n;
  void *opts_em;
  gcry_mpi_t mpi_signature;
  gcry_mpi_t mpi_data;
  ac_scheme_t *scheme;
  char *elements_sig;
  size_t elements_sig_n;
  unsigned char *s;
  size_t s_n;

  (void)flags;

  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  mpi_signature = NULL;
  elements_sig = NULL;
  data_signed = NULL;
  mpi_data = NULL;
  opts_em = NULL;
  em = NULL;
  s = NULL;

  if (key->type != GCRY_AC_KEY_PUBLIC)
    {
      err = gcry_error (GPG_ERR_WRONG_KEY_USAGE);
      goto out;
    }

  scheme = ac_scheme_get (scheme_id);
  if (! scheme)
    {
      err = gcry_error (GPG_ERR_NO_SIGNATURE_SCHEME);
      goto out;
    }

  err = ac_dencode_prepare (handle, key, opts, *scheme, &opts_em);
  if (err)
    goto out;

  _gcry_ac_io_init (&io_em, GCRY_AC_IO_WRITABLE,
		    GCRY_AC_IO_STRING, &em, &em_n);

  err = _gcry_ac_data_encode (scheme->scheme_encoding, 0, opts_em,
			      io_message, &io_em);
  if (err)
    goto out;

  mpi_data = gcry_mpi_new (0);
  _gcry_ac_os_to_mpi (mpi_data, em, em_n);

  err = _gcry_ac_io_read_all (io_signature, &s, &s_n);
  if (err)
    goto out;

  mpi_signature = gcry_mpi_new (0);
  _gcry_ac_os_to_mpi (mpi_signature, s, s_n);

  err = _gcry_pk_get_elements (handle->algorithm, NULL, &elements_sig);
  if (err)
    goto out;

  elements_sig_n = strlen (elements_sig);
  if (elements_sig_n != 1)
    {
      /* FIXME? */
      err = gcry_error (GPG_ERR_CONFLICT);
      goto out;
    }

  err = _gcry_ac_data_new (&data_signed);
  if (err)
    goto out;

  err = _gcry_ac_data_set (data_signed, GCRY_AC_FLAG_COPY | GCRY_AC_FLAG_DEALLOC,
			   elements_sig, mpi_signature);
  if (err)
    goto out;

  gcry_mpi_release (mpi_signature);
  mpi_signature = NULL;
  
  err = _gcry_ac_data_verify (handle, key, mpi_data, data_signed);

 out:

  _gcry_ac_data_destroy (data_signed);
  gcry_mpi_release (mpi_signature);
  gcry_mpi_release (mpi_data);
  free (elements_sig);
  gcry_free (opts_em);
  gcry_free (em);
  gcry_free (s);

  return err;
}


/* 
 * General functions.
 */

gcry_err_code_t
_gcry_ac_init (void)
{
  if (fips_mode ())
    return GPG_ERR_NOT_SUPPORTED;

  return 0;
}
