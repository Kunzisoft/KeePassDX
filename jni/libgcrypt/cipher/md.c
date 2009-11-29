/* md.c  -  message digest dispatcher
 * Copyright (C) 1998, 1999, 2002, 2003, 2006,
 *               2008 Free Software Foundation, Inc.
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
#include <errno.h>

#include "g10lib.h"
#include "cipher.h"
#include "ath.h"

#include "rmd.h"

/* A dummy extraspec so that we do not need to tests the extraspec
   field from the module specification against NULL and instead
   directly test the respective fields of extraspecs.  */
static md_extra_spec_t dummy_extra_spec;


/* This is the list of the digest implementations included in
   libgcrypt.  */
static struct digest_table_entry
{
  gcry_md_spec_t *digest;
  md_extra_spec_t *extraspec;
  unsigned int algorithm;
  int fips_allowed; 
} digest_table[] =
  {
#if USE_CRC    
    /* We allow the CRC algorithms even in FIPS mode because they are
       actually no cryptographic primitives.  */
    { &_gcry_digest_spec_crc32,   
      &dummy_extra_spec,                 GCRY_MD_CRC32, 1 },
    { &_gcry_digest_spec_crc32_rfc1510,  
      &dummy_extra_spec,                 GCRY_MD_CRC32_RFC1510, 1 },
    { &_gcry_digest_spec_crc24_rfc2440,
      &dummy_extra_spec,                 GCRY_MD_CRC24_RFC2440, 1 },
#endif
#if USE_MD4
    { &_gcry_digest_spec_md4,
      &dummy_extra_spec,                 GCRY_MD_MD4 },
#endif
#if USE_MD5
    { &_gcry_digest_spec_md5,
      &dummy_extra_spec,                 GCRY_MD_MD5, 1 },
#endif
#if USE_RMD160
    { &_gcry_digest_spec_rmd160,
      &dummy_extra_spec,                 GCRY_MD_RMD160 },
#endif
#if USE_SHA1
    { &_gcry_digest_spec_sha1, 
      &_gcry_digest_extraspec_sha1,      GCRY_MD_SHA1, 1 },
#endif
#if USE_SHA256
    { &_gcry_digest_spec_sha256,
      &_gcry_digest_extraspec_sha256,    GCRY_MD_SHA256, 1 },
    { &_gcry_digest_spec_sha224,
      &_gcry_digest_extraspec_sha224,    GCRY_MD_SHA224, 1 },
#endif
#if USE_SHA512
    { &_gcry_digest_spec_sha512,
      &_gcry_digest_extraspec_sha512,    GCRY_MD_SHA512, 1 },
    { &_gcry_digest_spec_sha384,
      &_gcry_digest_extraspec_sha384,    GCRY_MD_SHA384, 1 },
#endif
#if USE_TIGER
    { &_gcry_digest_spec_tiger,
      &dummy_extra_spec,                 GCRY_MD_TIGER },
#endif
#if USE_WHIRLPOOL
    { &_gcry_digest_spec_whirlpool,
      &dummy_extra_spec,                 GCRY_MD_WHIRLPOOL },
#endif
    { NULL },
  };

/* List of registered digests.  */
static gcry_module_t digests_registered;

/* This is the lock protecting DIGESTS_REGISTERED.  */
static ath_mutex_t digests_registered_lock = ATH_MUTEX_INITIALIZER;

/* Flag to check wether the default ciphers have already been
   registered.  */
static int default_digests_registered;

typedef struct gcry_md_list
{
  gcry_md_spec_t *digest;
  gcry_module_t module;
  struct gcry_md_list *next;
  size_t actual_struct_size;     /* Allocated size of this structure. */
  PROPERLY_ALIGNED_TYPE context;
} GcryDigestEntry;

/* this structure is put right after the gcry_md_hd_t buffer, so that
 * only one memory block is needed. */
struct gcry_md_context
{
  int  magic;
  size_t actual_handle_size;     /* Allocated size of this handle. */
  int  secure;
  FILE  *debug;
  int finalized;
  GcryDigestEntry *list;
  byte *macpads;
  int macpads_Bsize;             /* Blocksize as used for the HMAC pads. */
};


#define CTX_MAGIC_NORMAL 0x11071961
#define CTX_MAGIC_SECURE 0x16917011

/* Convenient macro for registering the default digests.  */
#define REGISTER_DEFAULT_DIGESTS                   \
  do                                               \
    {                                              \
      ath_mutex_lock (&digests_registered_lock);   \
      if (! default_digests_registered)            \
        {                                          \
          md_register_default ();                  \
          default_digests_registered = 1;          \
        }                                          \
      ath_mutex_unlock (&digests_registered_lock); \
    }                                              \
  while (0)


static const char * digest_algo_to_string( int algo );
static gcry_err_code_t check_digest_algo (int algo);
static gcry_err_code_t md_open (gcry_md_hd_t *h, int algo,
                                int secure, int hmac);
static gcry_err_code_t md_enable (gcry_md_hd_t hd, int algo);
static gcry_err_code_t md_copy (gcry_md_hd_t a, gcry_md_hd_t *b);
static void md_close (gcry_md_hd_t a);
static void md_write (gcry_md_hd_t a, const void *inbuf, size_t inlen);
static void md_final(gcry_md_hd_t a);
static byte *md_read( gcry_md_hd_t a, int algo );
static int md_get_algo( gcry_md_hd_t a );
static int md_digest_length( int algo );
static const byte *md_asn_oid( int algo, size_t *asnlen, size_t *mdlen );
static void md_start_debug ( gcry_md_hd_t a, const char *suffix );
static void md_stop_debug ( gcry_md_hd_t a );




/* Internal function.  Register all the ciphers included in
   CIPHER_TABLE.  Returns zero on success or an error code.  */
static void
md_register_default (void)
{
  gcry_err_code_t err = 0;
  int i;
  
  for (i = 0; !err && digest_table[i].digest; i++)
    {
      if ( fips_mode ())
        {
          if (!digest_table[i].fips_allowed)
            continue;
          if (digest_table[i].algorithm == GCRY_MD_MD5
              && _gcry_enforced_fips_mode () )
            continue;  /* Do not register in enforced fips mode.  */
        }

      err = _gcry_module_add (&digests_registered,
                              digest_table[i].algorithm,
                              (void *) digest_table[i].digest,
                              (void *) digest_table[i].extraspec,
                              NULL);
    }

  if (err)
    BUG ();
}

/* Internal callback function.  */
static int
gcry_md_lookup_func_name (void *spec, void *data)
{
  gcry_md_spec_t *digest = (gcry_md_spec_t *) spec;
  char *name = (char *) data;

  return (! stricmp (digest->name, name));
}

/* Internal callback function.  Used via _gcry_module_lookup.  */
static int
gcry_md_lookup_func_oid (void *spec, void *data)
{
  gcry_md_spec_t *digest = (gcry_md_spec_t *) spec;
  char *oid = (char *) data;
  gcry_md_oid_spec_t *oid_specs = digest->oids;
  int ret = 0, i;

  if (oid_specs)
    {
      for (i = 0; oid_specs[i].oidstring && (! ret); i++)
        if (! stricmp (oid, oid_specs[i].oidstring))
          ret = 1;
    }

  return ret;
}

/* Internal function.  Lookup a digest entry by it's name.  */
static gcry_module_t 
gcry_md_lookup_name (const char *name)
{
  gcry_module_t digest;

  digest = _gcry_module_lookup (digests_registered, (void *) name,
				gcry_md_lookup_func_name);

  return digest;
}

/* Internal function.  Lookup a cipher entry by it's oid.  */
static gcry_module_t
gcry_md_lookup_oid (const char *oid)
{
  gcry_module_t digest;

  digest = _gcry_module_lookup (digests_registered, (void *) oid,
				gcry_md_lookup_func_oid);

  return digest;
}

/* Register a new digest module whose specification can be found in
   DIGEST.  On success, a new algorithm ID is stored in ALGORITHM_ID
   and a pointer representhing this module is stored in MODULE.  */
gcry_error_t
_gcry_md_register (gcry_md_spec_t *digest,
                   md_extra_spec_t *extraspec,
                   unsigned int *algorithm_id,
                   gcry_module_t *module)
{
  gcry_err_code_t err = 0;
  gcry_module_t mod;

  /* We do not support module loading in fips mode.  */
  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  ath_mutex_lock (&digests_registered_lock);
  err = _gcry_module_add (&digests_registered, 0,
			  (void *) digest, 
			  (void *)(extraspec? extraspec : &dummy_extra_spec), 
                          &mod);
  ath_mutex_unlock (&digests_registered_lock);
  
  if (! err)
    {
      *module = mod;
      *algorithm_id = mod->mod_id;
    }

  return gcry_error (err);
}

/* Unregister the digest identified by ID, which must have been
   registered with gcry_digest_register.  */
void
gcry_md_unregister (gcry_module_t module)
{
  ath_mutex_lock (&digests_registered_lock);
  _gcry_module_release (module);
  ath_mutex_unlock (&digests_registered_lock);
}


static int 
search_oid (const char *oid, int *algorithm, gcry_md_oid_spec_t *oid_spec)
{
  gcry_module_t module;
  int ret = 0;

  if (oid && ((! strncmp (oid, "oid.", 4))
	      || (! strncmp (oid, "OID.", 4))))
    oid += 4;

  module = gcry_md_lookup_oid (oid);
  if (module)
    {
      gcry_md_spec_t *digest = module->spec;
      int i;

      for (i = 0; digest->oids[i].oidstring && !ret; i++)
	if (! stricmp (oid, digest->oids[i].oidstring))
	  {
	    if (algorithm)
	      *algorithm = module->mod_id;
	    if (oid_spec)
	      *oid_spec = digest->oids[i];
	    ret = 1;
	  }
      _gcry_module_release (module);
    }

  return ret;
}

/****************
 * Map a string to the digest algo
 */
int
gcry_md_map_name (const char *string)
{
  gcry_module_t digest;
  int ret, algorithm = 0;

  if (! string)
    return 0;

  REGISTER_DEFAULT_DIGESTS;

  /* If the string starts with a digit (optionally prefixed with
     either "OID." or "oid."), we first look into our table of ASN.1
     object identifiers to figure out the algorithm */

  ath_mutex_lock (&digests_registered_lock);

  ret = search_oid (string, &algorithm, NULL);
  if (! ret)
    {
      /* Not found, search a matching digest name.  */
      digest = gcry_md_lookup_name (string);
      if (digest)
	{
	  algorithm = digest->mod_id;
	  _gcry_module_release (digest);
	}
    }
  ath_mutex_unlock (&digests_registered_lock);

  return algorithm;
}


/****************
 * Map a digest algo to a string
 */
static const char *
digest_algo_to_string (int algorithm)
{
  const char *name = NULL;
  gcry_module_t digest;

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  digest = _gcry_module_lookup_id (digests_registered, algorithm);
  if (digest)
    {
      name = ((gcry_md_spec_t *) digest->spec)->name;
      _gcry_module_release (digest);
    }
  ath_mutex_unlock (&digests_registered_lock);

  return name;
}

/****************
 * This function simply returns the name of the algorithm or some constant
 * string when there is no algo.  It will never return NULL.
 * Use	the macro gcry_md_test_algo() to check whether the algorithm
 * is valid.
 */
const char *
gcry_md_algo_name (int algorithm)
{
  const char *s = digest_algo_to_string (algorithm);
  return s ? s : "?";
}


static gcry_err_code_t
check_digest_algo (int algorithm)
{
  gcry_err_code_t rc = 0;
  gcry_module_t digest;

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  digest = _gcry_module_lookup_id (digests_registered, algorithm);
  if (digest)
    _gcry_module_release (digest);
  else
    rc = GPG_ERR_DIGEST_ALGO;
  ath_mutex_unlock (&digests_registered_lock);

  return rc;
}



/****************
 * Open a message digest handle for use with algorithm ALGO.
 * More algorithms may be added by md_enable(). The initial algorithm
 * may be 0.
 */
static gcry_err_code_t
md_open (gcry_md_hd_t *h, int algo, int secure, int hmac)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  int bufsize = secure ? 512 : 1024;
  struct gcry_md_context *ctx;
  gcry_md_hd_t hd;
  size_t n;

  /* Allocate a memory area to hold the caller visible buffer with it's
   * control information and the data required by this module. Set the
   * context pointer at the beginning to this area.
   * We have to use this strange scheme because we want to hide the
   * internal data but have a variable sized buffer.
   *
   *	+---+------+---........------+-------------+
   *	!ctx! bctl !  buffer	     ! private	   !
   *	+---+------+---........------+-------------+
   *	  !			      ^
   *	  !---------------------------!
   *
   * We have to make sure that private is well aligned.
   */
  n = sizeof (struct gcry_md_handle) + bufsize;
  n = ((n + sizeof (PROPERLY_ALIGNED_TYPE) - 1)
       / sizeof (PROPERLY_ALIGNED_TYPE)) * sizeof (PROPERLY_ALIGNED_TYPE);

  /* Allocate and set the Context pointer to the private data */
  if (secure)
    hd = gcry_malloc_secure (n + sizeof (struct gcry_md_context));
  else
    hd = gcry_malloc (n + sizeof (struct gcry_md_context));

  if (! hd)
    err = gpg_err_code_from_errno (errno);

  if (! err)
    {
      hd->ctx = ctx = (struct gcry_md_context *) ((char *) hd + n);
      /* Setup the globally visible data (bctl in the diagram).*/
      hd->bufsize = n - sizeof (struct gcry_md_handle) + 1;
      hd->bufpos = 0;

      /* Initialize the private data. */
      memset (hd->ctx, 0, sizeof *hd->ctx);
      ctx->magic = secure ? CTX_MAGIC_SECURE : CTX_MAGIC_NORMAL;
      ctx->actual_handle_size = n + sizeof (struct gcry_md_context);
      ctx->secure = secure;

      if (hmac)
	{
	  switch (algo)
            {
              case GCRY_MD_SHA384:
              case GCRY_MD_SHA512:
                ctx->macpads_Bsize = 128;
                break;
              default:
                ctx->macpads_Bsize = 64;
                break;
            }
          ctx->macpads = gcry_malloc_secure (2*(ctx->macpads_Bsize));
	  if (!ctx->macpads)
	    {
	      err = gpg_err_code_from_errno (errno);
	      md_close (hd);
	    }
	}
    }

  if (! err)
    {
      /* Hmmm, should we really do that? - yes [-wk] */
      _gcry_fast_random_poll ();

      if (algo)
	{
	  err = md_enable (hd, algo);
	  if (err)
	    md_close (hd);
	}
    }

  if (! err)
    *h = hd;

  return err;
}

/* Create a message digest object for algorithm ALGO.  FLAGS may be
   given as an bitwise OR of the gcry_md_flags values.  ALGO may be
   given as 0 if the algorithms to be used are later set using
   gcry_md_enable. H is guaranteed to be a valid handle or NULL on
   error.  */
gcry_error_t
gcry_md_open (gcry_md_hd_t *h, int algo, unsigned int flags)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  gcry_md_hd_t hd;

  if ((flags & ~(GCRY_MD_FLAG_SECURE | GCRY_MD_FLAG_HMAC)))
    err = GPG_ERR_INV_ARG;
  else
    {
      err = md_open (&hd, algo, (flags & GCRY_MD_FLAG_SECURE),
		     (flags & GCRY_MD_FLAG_HMAC));
    }

  *h = err? NULL : hd;
  return gcry_error (err);
}



static gcry_err_code_t
md_enable (gcry_md_hd_t hd, int algorithm)
{
  struct gcry_md_context *h = hd->ctx;
  gcry_md_spec_t *digest = NULL;
  GcryDigestEntry *entry;
  gcry_module_t module;
  gcry_err_code_t err = 0;

  for (entry = h->list; entry; entry = entry->next)
    if (entry->module->mod_id == algorithm)
      return err; /* already enabled */

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  module = _gcry_module_lookup_id (digests_registered, algorithm);
  ath_mutex_unlock (&digests_registered_lock);
  if (! module)
    {
      log_debug ("md_enable: algorithm %d not available\n", algorithm);
      err = GPG_ERR_DIGEST_ALGO;
    }
 else
    digest = (gcry_md_spec_t *) module->spec;

  
  if (!err && algorithm == GCRY_MD_MD5 && fips_mode ())
    {
      _gcry_inactivate_fips_mode ("MD5 used");
      if (_gcry_enforced_fips_mode () )
        {
          /* We should never get to here because we do not register
             MD5 in enforced fips mode. But better throw an error.  */
          err = GPG_ERR_DIGEST_ALGO;
        }
    }
  
  if (!err)
    {
      size_t size = (sizeof (*entry)
                     + digest->contextsize
                     - sizeof (entry->context));

      /* And allocate a new list entry. */
      if (h->secure)
	entry = gcry_malloc_secure (size);
      else
	entry = gcry_malloc (size);

      if (! entry)
	err = gpg_err_code_from_errno (errno);
      else
	{
	  entry->digest = digest;
	  entry->module = module;
	  entry->next = h->list;
          entry->actual_struct_size = size;
	  h->list = entry;

	  /* And init this instance. */
	  entry->digest->init (&entry->context.c);
	}
    }

  if (err)
    {
      if (module)
	{
	   ath_mutex_lock (&digests_registered_lock);
	   _gcry_module_release (module);
	   ath_mutex_unlock (&digests_registered_lock);
	}
    }

  return err;
}


gcry_error_t
gcry_md_enable (gcry_md_hd_t hd, int algorithm)
{
  return gcry_error (md_enable (hd, algorithm));
}

static gcry_err_code_t
md_copy (gcry_md_hd_t ahd, gcry_md_hd_t *b_hd)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  struct gcry_md_context *a = ahd->ctx;
  struct gcry_md_context *b;
  GcryDigestEntry *ar, *br;
  gcry_md_hd_t bhd;
  size_t n;
  
  if (ahd->bufpos)
    md_write (ahd, NULL, 0);

  n = (char *) ahd->ctx - (char *) ahd;
  if (a->secure)
    bhd = gcry_malloc_secure (n + sizeof (struct gcry_md_context));
  else
    bhd = gcry_malloc (n + sizeof (struct gcry_md_context));

  if (! bhd)
    err = gpg_err_code_from_errno (errno);

  if (! err)
    {
      bhd->ctx = b = (struct gcry_md_context *) ((char *) bhd + n);
      /* No need to copy the buffer due to the write above. */
      gcry_assert (ahd->bufsize == (n - sizeof (struct gcry_md_handle) + 1));
      bhd->bufsize = ahd->bufsize;
      bhd->bufpos = 0;
      gcry_assert (! ahd->bufpos);
      memcpy (b, a, sizeof *a);
      b->list = NULL;
      b->debug = NULL;
      if (a->macpads)
	{
	  b->macpads = gcry_malloc_secure (2*(a->macpads_Bsize));
	  if (! b->macpads)
	    {
	      err = gpg_err_code_from_errno (errno);
	      md_close (bhd);
	    }
	  else
	    memcpy (b->macpads, a->macpads, (2*(a->macpads_Bsize)));
	}
    }

  /* Copy the complete list of algorithms.  The copied list is
     reversed, but that doesn't matter. */
  if (!err)
    {
      for (ar = a->list; ar; ar = ar->next)
        {
          if (a->secure)
            br = gcry_malloc_secure (sizeof *br
                                     + ar->digest->contextsize
                                     - sizeof(ar->context));
          else
            br = gcry_malloc (sizeof *br
                              + ar->digest->contextsize
                              - sizeof (ar->context));
          if (!br)
            {
	      err = gpg_err_code_from_errno (errno);
              md_close (bhd);
              break;
            }

          memcpy (br, ar, (sizeof (*br) + ar->digest->contextsize
                           - sizeof (ar->context)));
          br->next = b->list;
          b->list = br;
          
          /* Add a reference to the module.  */
          ath_mutex_lock (&digests_registered_lock);
          _gcry_module_use (br->module);
          ath_mutex_unlock (&digests_registered_lock);
        }
    }

  if (a->debug && !err)
    md_start_debug (bhd, "unknown");

  if (!err)
    *b_hd = bhd;

  return err;
}

gcry_error_t
gcry_md_copy (gcry_md_hd_t *handle, gcry_md_hd_t hd)
{
  gcry_err_code_t err;

  err = md_copy (hd, handle);
  if (err)
    *handle = NULL;
  return gcry_error (err);
}

/*
 * Reset all contexts and discard any buffered stuff.  This may be used
 * instead of a md_close(); md_open().
 */
void
gcry_md_reset (gcry_md_hd_t a)
{
  GcryDigestEntry *r;

  /* Note: We allow this even in fips non operational mode.  */

  a->bufpos = a->ctx->finalized = 0;

  for (r = a->ctx->list; r; r = r->next)
    {
      memset (r->context.c, 0, r->digest->contextsize);
      (*r->digest->init) (&r->context.c);
    }
  if (a->ctx->macpads)
    md_write (a, a->ctx->macpads, a->ctx->macpads_Bsize); /* inner pad */
}

static void
md_close (gcry_md_hd_t a)
{
  GcryDigestEntry *r, *r2;

  if (! a)
    return;
  if (a->ctx->debug)
    md_stop_debug (a);
  for (r = a->ctx->list; r; r = r2)
    {
      r2 = r->next;
      ath_mutex_lock (&digests_registered_lock);
      _gcry_module_release (r->module);
      ath_mutex_unlock (&digests_registered_lock);
      wipememory (r, r->actual_struct_size);
      gcry_free (r);
    }

  if (a->ctx->macpads)
    {
      wipememory (a->ctx->macpads, 2*(a->ctx->macpads_Bsize));
      gcry_free(a->ctx->macpads);
    }

  wipememory (a, a->ctx->actual_handle_size);
  gcry_free(a);
}

void
gcry_md_close (gcry_md_hd_t hd)
{
  /* Note: We allow this even in fips non operational mode.  */
  md_close (hd);
}

static void
md_write (gcry_md_hd_t a, const void *inbuf, size_t inlen)
{
  GcryDigestEntry *r;
  
  if (a->ctx->debug)
    {
      if (a->bufpos && fwrite (a->buf, a->bufpos, 1, a->ctx->debug) != 1)
	BUG();
      if (inlen && fwrite (inbuf, inlen, 1, a->ctx->debug) != 1)
	BUG();
    }

  for (r = a->ctx->list; r; r = r->next)
    {
      if (a->bufpos)
	(*r->digest->write) (&r->context.c, a->buf, a->bufpos);
      (*r->digest->write) (&r->context.c, inbuf, inlen);
    }
  a->bufpos = 0;
}

void
gcry_md_write (gcry_md_hd_t hd, const void *inbuf, size_t inlen)
{
  md_write (hd, inbuf, inlen);
}

static void
md_final (gcry_md_hd_t a)
{
  GcryDigestEntry *r;

  if (a->ctx->finalized)
    return;

  if (a->bufpos)
    md_write (a, NULL, 0);

  for (r = a->ctx->list; r; r = r->next)
    (*r->digest->final) (&r->context.c);

  a->ctx->finalized = 1;

  if (a->ctx->macpads)
    {
      /* Finish the hmac. */
      int algo = md_get_algo (a);
      byte *p = md_read (a, algo);
      size_t dlen = md_digest_length (algo);
      gcry_md_hd_t om;
      gcry_err_code_t err = md_open (&om, algo, a->ctx->secure, 0);

      if (err)
	_gcry_fatal_error (err, NULL);
      md_write (om, 
                (a->ctx->macpads)+(a->ctx->macpads_Bsize), 
                a->ctx->macpads_Bsize);
      md_write (om, p, dlen);
      md_final (om);
      /* Replace our digest with the mac (they have the same size). */
      memcpy (p, md_read (om, algo), dlen);
      md_close (om);
    }
}

static gcry_err_code_t
prepare_macpads (gcry_md_hd_t hd, const unsigned char *key, size_t keylen)
{
  int i;
  int algo = md_get_algo (hd);
  unsigned char *helpkey = NULL;
  unsigned char *ipad, *opad;

  if (!algo)
    return GPG_ERR_DIGEST_ALGO; /* Might happen if no algo is enabled.  */

  if ( keylen > hd->ctx->macpads_Bsize ) 
    {
      helpkey = gcry_malloc_secure (md_digest_length (algo));
      if (!helpkey)
        return gpg_err_code_from_errno (errno);
      gcry_md_hash_buffer (algo, helpkey, key, keylen);
      key = helpkey;
      keylen = md_digest_length (algo);
      gcry_assert ( keylen <= hd->ctx->macpads_Bsize );
    }

  memset ( hd->ctx->macpads, 0, 2*(hd->ctx->macpads_Bsize) );
  ipad = hd->ctx->macpads;
  opad = (hd->ctx->macpads)+(hd->ctx->macpads_Bsize);
  memcpy ( ipad, key, keylen );
  memcpy ( opad, key, keylen );
  for (i=0; i < hd->ctx->macpads_Bsize; i++ ) 
    {
      ipad[i] ^= 0x36;
      opad[i] ^= 0x5c;
    }
  gcry_free (helpkey);

  return GPG_ERR_NO_ERROR;
}

gcry_error_t
gcry_md_ctl (gcry_md_hd_t hd, int cmd, void *buffer, size_t buflen)
{
  gcry_err_code_t rc = 0;
  
  switch (cmd)
    {
    case GCRYCTL_FINALIZE:
      md_final (hd);
      break;
    case GCRYCTL_SET_KEY:
      rc = gcry_err_code (gcry_md_setkey (hd, buffer, buflen));
      break;
    case GCRYCTL_START_DUMP:
      md_start_debug (hd, buffer);
      break;
    case GCRYCTL_STOP_DUMP:
      md_stop_debug ( hd );
      break;
    default:
      rc = GPG_ERR_INV_OP;
    }
  return gcry_error (rc);
}

gcry_error_t
gcry_md_setkey (gcry_md_hd_t hd, const void *key, size_t keylen)
{
  gcry_err_code_t rc = GPG_ERR_NO_ERROR;

  if (!hd->ctx->macpads)
    rc = GPG_ERR_CONFLICT;
  else
    {
      rc = prepare_macpads (hd, key, keylen);
      if (! rc)
	gcry_md_reset (hd);
    }

  return gcry_error (rc);
}

/* The new debug interface.  If SUFFIX is a string it creates an debug
   file for the context HD.  IF suffix is NULL, the file is closed and
   debugging is stopped.  */
void
gcry_md_debug (gcry_md_hd_t hd, const char *suffix)
{
  if (suffix)
    md_start_debug (hd, suffix);
  else
    md_stop_debug (hd);
}



/****************
 * if ALGO is null get the digest for the used algo (which should be only one)
 */
static byte *
md_read( gcry_md_hd_t a, int algo )
{
  GcryDigestEntry *r = a->ctx->list;

  if (! algo)
    {
      /* return the first algorithm */
      if (r && r->next)
	log_debug ("more than one algorithm in md_read(0)\n");
      return r->digest->read( &r->context.c );
    }
  else
    {
      for (r = a->ctx->list; r; r = r->next)
	if (r->module->mod_id == algo)
	  return r->digest->read (&r->context.c);
    }
  BUG();
  return NULL;
}

/*
 * Read out the complete digest, this function implictly finalizes
 * the hash.
 */
byte *
gcry_md_read (gcry_md_hd_t hd, int algo)
{
  /* This function is expected to always return a digest, thus we
     can't return an error which we actually should do in
     non-operational state.  */
  gcry_md_ctl (hd, GCRYCTL_FINALIZE, NULL, 0);
  return md_read (hd, algo);
}


/*
 * Read out an intermediate digest.  Not yet functional.
 */
gcry_err_code_t
gcry_md_get (gcry_md_hd_t hd, int algo, byte *buffer, int buflen)
{
  (void)hd;
  (void)algo;
  (void)buffer;
  (void)buflen;

  /*md_digest ... */
  fips_signal_error ("unimplemented function called");
  return GPG_ERR_INTERNAL;
}


/*
 * Shortcut function to hash a buffer with a given algo. The only
 * guaranteed supported algorithms are RIPE-MD160 and SHA-1. The
 * supplied digest buffer must be large enough to store the resulting
 * hash.  No error is returned, the function will abort on an invalid
 * algo.  DISABLED_ALGOS are ignored here.  */
void
gcry_md_hash_buffer (int algo, void *digest,
                     const void *buffer, size_t length)
{
  if (algo == GCRY_MD_SHA1)
    _gcry_sha1_hash_buffer (digest, buffer, length);
  else if (algo == GCRY_MD_RMD160 && !fips_mode () )
    _gcry_rmd160_hash_buffer (digest, buffer, length);
  else
    {
      /* For the others we do not have a fast function, so we use the
	 normal functions. */
      gcry_md_hd_t h;
      gpg_err_code_t err;

      if (algo == GCRY_MD_MD5 && fips_mode ())
        {
          _gcry_inactivate_fips_mode ("MD5 used");
          if (_gcry_enforced_fips_mode () )
            {
              /* We should never get to here because we do not register
                 MD5 in enforced fips mode.  */
              _gcry_fips_noreturn ();
            }
        }

      err = md_open (&h, algo, 0, 0);
      if (err)
	log_bug ("gcry_md_open failed for algo %d: %s",
                 algo, gpg_strerror (gcry_error(err)));
      md_write (h, (byte *) buffer, length);
      md_final (h);
      memcpy (digest, md_read (h, algo), md_digest_length (algo));
      md_close (h);
    }
}

static int
md_get_algo (gcry_md_hd_t a)
{
  GcryDigestEntry *r = a->ctx->list;

  if (r && r->next)
    {
      fips_signal_error ("possible usage error");
      log_error ("WARNING: more than one algorithm in md_get_algo()\n");
    }
  return r ? r->module->mod_id : 0;
}

int
gcry_md_get_algo (gcry_md_hd_t hd)
{
  return md_get_algo (hd);
}


/****************
 * Return the length of the digest
 */
static int
md_digest_length (int algorithm)
{
  gcry_module_t digest;
  int mdlen = 0;

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  digest = _gcry_module_lookup_id (digests_registered, algorithm);
  if (digest)
    {
      mdlen = ((gcry_md_spec_t *) digest->spec)->mdlen;
      _gcry_module_release (digest);
    }
  ath_mutex_unlock (&digests_registered_lock);

  return mdlen;
}

/****************
 * Return the length of the digest in bytes.
 * This function will return 0 in case of errors.
 */
unsigned int
gcry_md_get_algo_dlen (int algorithm)
{
  return md_digest_length (algorithm);
}


/* Hmmm: add a mode to enumerate the OIDs
 *	to make g10/sig-check.c more portable */
static const byte *
md_asn_oid (int algorithm, size_t *asnlen, size_t *mdlen)
{
  const byte *asnoid = NULL;
  gcry_module_t digest;

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  digest = _gcry_module_lookup_id (digests_registered, algorithm);
  if (digest)
    {
      if (asnlen)
	*asnlen = ((gcry_md_spec_t *) digest->spec)->asnlen;
      if (mdlen)
	*mdlen = ((gcry_md_spec_t *) digest->spec)->mdlen;
      asnoid = ((gcry_md_spec_t *) digest->spec)->asnoid;
      _gcry_module_release (digest);
    }
  else
    log_bug ("no ASN.1 OID for md algo %d\n", algorithm);
  ath_mutex_unlock (&digests_registered_lock);

  return asnoid;
}



/****************
 * Return information about the given cipher algorithm
 * WHAT select the kind of information returned:
 *  GCRYCTL_TEST_ALGO:
 *	Returns 0 when the specified algorithm is available for use.
 *	buffer and nbytes must be zero.
 *  GCRYCTL_GET_ASNOID:
 *	Return the ASNOID of the algorithm in buffer. if buffer is NULL, only
 *	the required length is returned.
 *
 * Note:  Because this function is in most cases used to return an
 * integer value, we can make it easier for the caller to just look at
 * the return value.  The caller will in all cases consult the value
 * and thereby detecting whether a error occured or not (i.e. while checking
 * the block size)
 */
gcry_error_t
gcry_md_algo_info (int algo, int what, void *buffer, size_t *nbytes)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  switch (what)
    {
    case GCRYCTL_TEST_ALGO:
      if (buffer || nbytes)
	err = GPG_ERR_INV_ARG;
      else
	err = check_digest_algo (algo);
      break;

    case GCRYCTL_GET_ASNOID:
      /* We need to check that the algo is available because
         md_asn_oid would otherwise raise an assertion. */
      err = check_digest_algo (algo);
      if (!err)
        {
          const char unsigned *asn;
          size_t asnlen;
          
          asn = md_asn_oid (algo, &asnlen, NULL);
          if (buffer && (*nbytes >= asnlen))
	  {
	    memcpy (buffer, asn, asnlen);
	    *nbytes = asnlen;
	  }
          else if (!buffer && nbytes)
            *nbytes = asnlen;
          else
            {
              if (buffer)
                err = GPG_ERR_TOO_SHORT;
              else
                err = GPG_ERR_INV_ARG;
            }
        }
      break;

  default:
    err = GPG_ERR_INV_OP;
  }

  return gcry_error (err);
}


static void
md_start_debug ( gcry_md_hd_t md, const char *suffix )
{
  static int idx=0;
  char buf[50];

  if (fips_mode ())
    return;
  
  if ( md->ctx->debug )
    {
      log_debug("Oops: md debug already started\n");
      return;
    }
  idx++;
  snprintf (buf, DIM(buf)-1, "dbgmd-%05d.%.10s", idx, suffix );
  md->ctx->debug = fopen(buf, "w");
  if ( !md->ctx->debug )
    log_debug("md debug: can't open %s\n", buf );
}

static void
md_stop_debug( gcry_md_hd_t md )
{
  if ( md->ctx->debug )
    {
      if ( md->bufpos )
        md_write ( md, NULL, 0 );
      fclose (md->ctx->debug);
      md->ctx->debug = NULL;
    }

#ifdef HAVE_U64_TYPEDEF
  {  /* a kludge to pull in the __muldi3 for Solaris */
    volatile u32 a = (u32)(ulong)md;
    volatile u64 b = 42;
    volatile u64 c;
    c = a * b;
  }
#endif
}



/*
 * Return information about the digest handle.
 *  GCRYCTL_IS_SECURE:
 *	Returns 1 when the handle works on secured memory
 *	otherwise 0 is returned.  There is no error return.
 *  GCRYCTL_IS_ALGO_ENABLED:
 *     Returns 1 if the algo is enabled for that handle.
 *     The algo must be passed as the address of an int.
 */
gcry_error_t
gcry_md_info (gcry_md_hd_t h, int cmd, void *buffer, size_t *nbytes)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  switch (cmd)
    {
    case GCRYCTL_IS_SECURE:
      *nbytes = h->ctx->secure;
      break;

    case GCRYCTL_IS_ALGO_ENABLED:
      {
	GcryDigestEntry *r;
	int algo;

	if ( !buffer || (nbytes && (*nbytes != sizeof (int))))
	  err = GPG_ERR_INV_ARG;
	else
	  {
	    algo = *(int*)buffer;
	    
	    *nbytes = 0;
	    for(r=h->ctx->list; r; r = r->next ) {
	      if (r->module->mod_id == algo)
		{
		  *nbytes = 1;
		  break;
		}
	    }
	  }
	break;
      }

  default:
    err = GPG_ERR_INV_OP;
  }

  return gcry_error (err);
}


/* Explicitly initialize this module.  */
gcry_err_code_t
_gcry_md_init (void)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  REGISTER_DEFAULT_DIGESTS;

  return err;
}


int
gcry_md_is_secure (gcry_md_hd_t a) 
{
  size_t value;

  if (gcry_md_info (a, GCRYCTL_IS_SECURE, NULL, &value))
    value = 1; /* It seems to be better to assume secure memory on
                  error. */
  return value;
}


int
gcry_md_is_enabled (gcry_md_hd_t a, int algo) 
{
  size_t value;

  value = sizeof algo;
  if (gcry_md_info (a, GCRYCTL_IS_ALGO_ENABLED, &algo, &value))
    value = 0;
  return value;
}

/* Get a list consisting of the IDs of the loaded message digest
   modules.  If LIST is zero, write the number of loaded message
   digest modules to LIST_LENGTH and return.  If LIST is non-zero, the
   first *LIST_LENGTH algorithm IDs are stored in LIST, which must be
   of according size.  In case there are less message digest modules
   than *LIST_LENGTH, *LIST_LENGTH is updated to the correct
   number.  */
gcry_error_t
gcry_md_list (int *list, int *list_length)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  ath_mutex_lock (&digests_registered_lock);
  err = _gcry_module_list (digests_registered, list, list_length);
  ath_mutex_unlock (&digests_registered_lock);

  return err;
}


/* Run the selftests for digest algorithm ALGO with optional reporting
   function REPORT.  */
gpg_error_t
_gcry_md_selftest (int algo, int extended, selftest_report_func_t report)
{
  gcry_module_t module = NULL;
  cipher_extra_spec_t *extraspec = NULL;
  gcry_err_code_t ec = 0;

  REGISTER_DEFAULT_DIGESTS;

  ath_mutex_lock (&digests_registered_lock);
  module = _gcry_module_lookup_id (digests_registered, algo);
  if (module && !(module->flags & FLAG_MODULE_DISABLED))
    extraspec = module->extraspec;
  ath_mutex_unlock (&digests_registered_lock);
  if (extraspec && extraspec->selftest)
    ec = extraspec->selftest (algo, extended, report);
  else
    {
      ec = GPG_ERR_DIGEST_ALGO;
      if (report)
        report ("digest", algo, "module", 
                module && !(module->flags & FLAG_MODULE_DISABLED)?
                "no selftest available" :
                module? "algorithm disabled" : "algorithm not found");
    }

  if (module)
    {
      ath_mutex_lock (&digests_registered_lock);
      _gcry_module_release (module);
      ath_mutex_unlock (&digests_registered_lock);
    }
  return gpg_error (ec);
}
