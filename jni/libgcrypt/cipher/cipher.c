/* cipher.c  -	cipher dispatcher
 * Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003
 *               2005, 2007, 2008 Free Software Foundation, Inc.
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

#define MAX_BLOCKSIZE 16
#define TABLE_SIZE 14
#define CTX_MAGIC_NORMAL 0x24091964
#define CTX_MAGIC_SECURE 0x46919042

#undef NEED_16BYTE_ALIGNED_CONTEXT
#if defined (__i386__) && SIZEOF_UNSIGNED_LONG == 4 && defined (__GNUC__)
#define NEED_16BYTE_ALIGNED_CONTEXT 1
#endif

/* A dummy extraspec so that we do not need to tests the extraspec
   field from the module specification against NULL and instead
   directly test the respective fields of extraspecs.  */
static cipher_extra_spec_t dummy_extra_spec;

/* This is the list of the default ciphers, which are included in
   libgcrypt.  */
static struct cipher_table_entry
{
  gcry_cipher_spec_t *cipher;
  cipher_extra_spec_t *extraspec;
  unsigned int algorithm;
  int fips_allowed;
} cipher_table[] =
  {
#if USE_BLOWFISH
    { &_gcry_cipher_spec_blowfish,
      &dummy_extra_spec,                  GCRY_CIPHER_BLOWFISH },
#endif
#if USE_DES
    { &_gcry_cipher_spec_des,       
      &dummy_extra_spec,                  GCRY_CIPHER_DES },
    { &_gcry_cipher_spec_tripledes,
      &_gcry_cipher_extraspec_tripledes,  GCRY_CIPHER_3DES, 1 },
#endif
#if USE_ARCFOUR
    { &_gcry_cipher_spec_arcfour,    
      &dummy_extra_spec,                  GCRY_CIPHER_ARCFOUR },
#endif
#if USE_CAST5
    { &_gcry_cipher_spec_cast5,      
      &dummy_extra_spec,                  GCRY_CIPHER_CAST5 },
#endif
#if USE_AES
    { &_gcry_cipher_spec_aes,        
      &_gcry_cipher_extraspec_aes,        GCRY_CIPHER_AES,    1 },
    { &_gcry_cipher_spec_aes192,     
      &_gcry_cipher_extraspec_aes192,     GCRY_CIPHER_AES192, 1 },
    { &_gcry_cipher_spec_aes256,     
      &_gcry_cipher_extraspec_aes256,     GCRY_CIPHER_AES256, 1 },
#endif
#if USE_TWOFISH
    { &_gcry_cipher_spec_twofish,
      &dummy_extra_spec,                  GCRY_CIPHER_TWOFISH },
    { &_gcry_cipher_spec_twofish128,     
      &dummy_extra_spec,                  GCRY_CIPHER_TWOFISH128 },
#endif
#if USE_SERPENT
    { &_gcry_cipher_spec_serpent128, 
      &dummy_extra_spec,                  GCRY_CIPHER_SERPENT128 },
    { &_gcry_cipher_spec_serpent192,
      &dummy_extra_spec,                  GCRY_CIPHER_SERPENT192 },
    { &_gcry_cipher_spec_serpent256, 
      &dummy_extra_spec,                  GCRY_CIPHER_SERPENT256 },
#endif
#if USE_RFC2268
    { &_gcry_cipher_spec_rfc2268_40,
      &dummy_extra_spec,                  GCRY_CIPHER_RFC2268_40 },
#endif
#if USE_SEED
    { &_gcry_cipher_spec_seed, 
      &dummy_extra_spec,                  GCRY_CIPHER_SEED },
#endif
#if USE_CAMELLIA
    { &_gcry_cipher_spec_camellia128,
      &dummy_extra_spec,                  GCRY_CIPHER_CAMELLIA128 },
    { &_gcry_cipher_spec_camellia192, 
      &dummy_extra_spec,                  GCRY_CIPHER_CAMELLIA192 },
    { &_gcry_cipher_spec_camellia256,
      &dummy_extra_spec,                  GCRY_CIPHER_CAMELLIA256 },
#endif
    { NULL                    }
  };

/* List of registered ciphers.  */
static gcry_module_t ciphers_registered;

/* This is the lock protecting CIPHERS_REGISTERED.  */
static ath_mutex_t ciphers_registered_lock = ATH_MUTEX_INITIALIZER;

/* Flag to check wether the default ciphers have already been
   registered.  */
static int default_ciphers_registered;

/* Convenient macro for registering the default ciphers.  */
#define REGISTER_DEFAULT_CIPHERS                   \
  do                                               \
    {                                              \
      ath_mutex_lock (&ciphers_registered_lock);   \
      if (! default_ciphers_registered)            \
        {                                          \
          cipher_register_default ();              \
          default_ciphers_registered = 1;          \
        }                                          \
      ath_mutex_unlock (&ciphers_registered_lock); \
    }                                              \
  while (0)


/* A VIA processor with the Padlock engine requires an alignment of
   most data on a 16 byte boundary.  Because we trick out the compiler
   while allocating the context, the align attribute as used in
   rijndael.c does not work on its own.  Thus we need to make sure
   that the entire context structure is a aligned on that boundary.
   We achieve this by defining a new type and use that instead of our
   usual alignment type.  */
typedef union 
{
  PROPERLY_ALIGNED_TYPE foo;
#ifdef NEED_16BYTE_ALIGNED_CONTEXT
  char bar[16] __attribute__ ((aligned (16)));
#endif  
  char c[1];
} cipher_context_alignment_t;


/* The handle structure.  */
struct gcry_cipher_handle
{
  int magic;
  size_t actual_handle_size;     /* Allocated size of this handle. */
  size_t handle_offset;          /* Offset to the malloced block.  */
  gcry_cipher_spec_t *cipher;
  cipher_extra_spec_t *extraspec;
  gcry_module_t module;

  /* The algorithm id.  This is a hack required because the module
     interface does not easily allow to retrieve this value. */
  int algo;  

  /* A structure with function pointers for bulk operations.  Due to
     limitations of the module system (we don't want to change the
     API) we need to keep these function pointers here.  The cipher
     open function intializes them and the actual encryption routines
     use them if they are not NULL.  */
  struct {
    void (*cfb_enc)(void *context, unsigned char *iv, 
                    void *outbuf_arg, const void *inbuf_arg,
                    unsigned int nblocks);
    void (*cfb_dec)(void *context, unsigned char *iv, 
                    void *outbuf_arg, const void *inbuf_arg,
                    unsigned int nblocks);
    void (*cbc_enc)(void *context, unsigned char *iv, 
                    void *outbuf_arg, const void *inbuf_arg,
                    unsigned int nblocks, int cbc_mac);
    void (*cbc_dec)(void *context, unsigned char *iv, 
                    void *outbuf_arg, const void *inbuf_arg,
                    unsigned int nblocks);
  } bulk;


  int mode;
  unsigned int flags;

  /* The initialization vector.  To help code optimization we make
     sure that it is aligned on an unsigned long and u32 boundary.  */
  union {
    unsigned long dummy_iv;         
    u32 dummy_u32_iv;
    unsigned char iv[MAX_BLOCKSIZE];	
  } u_iv;

  unsigned char lastiv[MAX_BLOCKSIZE];
  int unused;  /* Number of unused bytes in the IV. */

  unsigned char ctr[MAX_BLOCKSIZE];     /* For Counter (CTR) mode. */


  /* What follows are two contexts of the cipher in use.  The first
     one needs to be aligned well enough for the cipher operation
     whereas the second one is a copy created by cipher_setkey and
     used by cipher_reset.  That second copy has no need for proper
     aligment because it is only accessed by memcpy.  */
  cipher_context_alignment_t context;
};



/* These dummy functions are used in case a cipher implementation
   refuses to provide it's own functions.  */

static gcry_err_code_t
dummy_setkey (void *c, const unsigned char *key, unsigned int keylen)
{
  (void)c; 
  (void)key;
  (void)keylen;
  return GPG_ERR_NO_ERROR;
}

static void
dummy_encrypt_block (void *c,
		     unsigned char *outbuf, const unsigned char *inbuf)
{
  (void)c;
  (void)outbuf;
  (void)inbuf;
  BUG();
}

static void
dummy_decrypt_block (void *c,
		     unsigned char *outbuf, const unsigned char *inbuf)
{
  (void)c;
  (void)outbuf;
  (void)inbuf;
  BUG();
}

static void
dummy_encrypt_stream (void *c,
		      unsigned char *outbuf, const unsigned char *inbuf,
		      unsigned int n)
{
  (void)c;
  (void)outbuf;
  (void)inbuf;
  (void)n;
  BUG();
}

static void
dummy_decrypt_stream (void *c,
		      unsigned char *outbuf, const unsigned char *inbuf,
		      unsigned int n)
{
  (void)c;
  (void)outbuf;
  (void)inbuf;
  (void)n;
  BUG();
}


/* Internal function.  Register all the ciphers included in
   CIPHER_TABLE.  Note, that this function gets only used by the macro
   REGISTER_DEFAULT_CIPHERS which protects it using a mutex. */
static void
cipher_register_default (void)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  int i;
  
  for (i = 0; !err && cipher_table[i].cipher; i++)
    {
      if (! cipher_table[i].cipher->setkey)
	cipher_table[i].cipher->setkey = dummy_setkey;
      if (! cipher_table[i].cipher->encrypt)
	cipher_table[i].cipher->encrypt = dummy_encrypt_block;
      if (! cipher_table[i].cipher->decrypt)
	cipher_table[i].cipher->decrypt = dummy_decrypt_block;
      if (! cipher_table[i].cipher->stencrypt)
	cipher_table[i].cipher->stencrypt = dummy_encrypt_stream;
      if (! cipher_table[i].cipher->stdecrypt)
	cipher_table[i].cipher->stdecrypt = dummy_decrypt_stream;

      if ( fips_mode () && !cipher_table[i].fips_allowed )
        continue;

      err = _gcry_module_add (&ciphers_registered,
			      cipher_table[i].algorithm,
			      (void *) cipher_table[i].cipher,
			      (void *) cipher_table[i].extraspec,
			      NULL);
    }

  if (err)
    BUG ();
}

/* Internal callback function.  Used via _gcry_module_lookup.  */
static int
gcry_cipher_lookup_func_name (void *spec, void *data)
{
  gcry_cipher_spec_t *cipher = (gcry_cipher_spec_t *) spec;
  char *name = (char *) data;
  const char **aliases = cipher->aliases;
  int i, ret = ! stricmp (name, cipher->name);

  if (aliases)
    for (i = 0; aliases[i] && (! ret); i++)
      ret = ! stricmp (name, aliases[i]);

  return ret;
}

/* Internal callback function.  Used via _gcry_module_lookup.  */
static int
gcry_cipher_lookup_func_oid (void *spec, void *data)
{
  gcry_cipher_spec_t *cipher = (gcry_cipher_spec_t *) spec;
  char *oid = (char *) data;
  gcry_cipher_oid_spec_t *oid_specs = cipher->oids;
  int ret = 0, i;

  if (oid_specs)
    for (i = 0; oid_specs[i].oid && (! ret); i++)
      if (! stricmp (oid, oid_specs[i].oid))
	ret = 1;

  return ret;
}

/* Internal function.  Lookup a cipher entry by it's name.  */
static gcry_module_t
gcry_cipher_lookup_name (const char *name)
{
  gcry_module_t cipher;

  cipher = _gcry_module_lookup (ciphers_registered, (void *) name,
				gcry_cipher_lookup_func_name);

  return cipher;
}

/* Internal function.  Lookup a cipher entry by it's oid.  */
static gcry_module_t
gcry_cipher_lookup_oid (const char *oid)
{
  gcry_module_t cipher;

  cipher = _gcry_module_lookup (ciphers_registered, (void *) oid,
				gcry_cipher_lookup_func_oid);

  return cipher;
}

/* Register a new cipher module whose specification can be found in
   CIPHER.  On success, a new algorithm ID is stored in ALGORITHM_ID
   and a pointer representhing this module is stored in MODULE.  */
gcry_error_t
_gcry_cipher_register (gcry_cipher_spec_t *cipher,
                       cipher_extra_spec_t *extraspec,
                       int *algorithm_id,
                       gcry_module_t *module)
{
  gcry_err_code_t err = 0;
  gcry_module_t mod;

  /* We do not support module loading in fips mode.  */
  if (fips_mode ())
    return gpg_error (GPG_ERR_NOT_SUPPORTED);

  ath_mutex_lock (&ciphers_registered_lock);
  err = _gcry_module_add (&ciphers_registered, 0,
			  (void *)cipher, 
			  (void *)(extraspec? extraspec : &dummy_extra_spec), 
                          &mod);
  ath_mutex_unlock (&ciphers_registered_lock);

  if (! err)
    {
      *module = mod;
      *algorithm_id = mod->mod_id;
    }

  return gcry_error (err);
}

/* Unregister the cipher identified by MODULE, which must have been
   registered with gcry_cipher_register.  */
void
gcry_cipher_unregister (gcry_module_t module)
{
  ath_mutex_lock (&ciphers_registered_lock);
  _gcry_module_release (module);
  ath_mutex_unlock (&ciphers_registered_lock);
}

/* Locate the OID in the oid table and return the index or -1 when not
   found.  An opitonal "oid." or "OID." prefix in OID is ignored, the
   OID is expected to be in standard IETF dotted notation.  The
   internal algorithm number is returned in ALGORITHM unless it
   ispassed as NULL.  A pointer to the specification of the module
   implementing this algorithm is return in OID_SPEC unless passed as
   NULL.*/
static int 
search_oid (const char *oid, int *algorithm, gcry_cipher_oid_spec_t *oid_spec)
{
  gcry_module_t module;
  int ret = 0;

  if (oid && ((! strncmp (oid, "oid.", 4))
	      || (! strncmp (oid, "OID.", 4))))
    oid += 4;

  module = gcry_cipher_lookup_oid (oid);
  if (module)
    {
      gcry_cipher_spec_t *cipher = module->spec;
      int i;

      for (i = 0; cipher->oids[i].oid && !ret; i++)
	if (! stricmp (oid, cipher->oids[i].oid))
	  {
	    if (algorithm)
	      *algorithm = module->mod_id;
	    if (oid_spec)
	      *oid_spec = cipher->oids[i];
	    ret = 1;
	  }
      _gcry_module_release (module);
    }

  return ret;
}

/* Map STRING to the cipher algorithm identifier.  Returns the
   algorithm ID of the cipher for the given name or 0 if the name is
   not known.  It is valid to pass NULL for STRING which results in a
   return value of 0. */
int
gcry_cipher_map_name (const char *string)
{
  gcry_module_t cipher;
  int ret, algorithm = 0;

  if (! string)
    return 0;

  REGISTER_DEFAULT_CIPHERS;

  /* If the string starts with a digit (optionally prefixed with
     either "OID." or "oid."), we first look into our table of ASN.1
     object identifiers to figure out the algorithm */

  ath_mutex_lock (&ciphers_registered_lock);

  ret = search_oid (string, &algorithm, NULL);
  if (! ret)
    {
      cipher = gcry_cipher_lookup_name (string);
      if (cipher)
	{
	  algorithm = cipher->mod_id;
	  _gcry_module_release (cipher);
	}
    }

  ath_mutex_unlock (&ciphers_registered_lock);
  
  return algorithm;
}


/* Given a STRING with an OID in dotted decimal notation, this
   function returns the cipher mode (GCRY_CIPHER_MODE_*) associated
   with that OID or 0 if no mode is known.  Passing NULL for string
   yields a return value of 0. */
int
gcry_cipher_mode_from_oid (const char *string)
{
  gcry_cipher_oid_spec_t oid_spec;
  int ret = 0, mode = 0;

  if (!string)
    return 0;

  ath_mutex_lock (&ciphers_registered_lock);
  ret = search_oid (string, NULL, &oid_spec);
  if (ret)
    mode = oid_spec.mode;
  ath_mutex_unlock (&ciphers_registered_lock);

  return mode;
}


/* Map the cipher algorithm whose ID is contained in ALGORITHM to a
   string representation of the algorithm name.  For unknown algorithm
   IDs this function returns "?".  */
static const char *
cipher_algo_to_string (int algorithm)
{
  gcry_module_t cipher;
  const char *name;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  cipher = _gcry_module_lookup_id (ciphers_registered, algorithm);
  if (cipher)
    {
      name = ((gcry_cipher_spec_t *) cipher->spec)->name;
      _gcry_module_release (cipher);
    }
  else
    name = "?";
  ath_mutex_unlock (&ciphers_registered_lock);

  return name;
}

/* Map the cipher algorithm identifier ALGORITHM to a string
   representing this algorithm.  This string is the default name as
   used by Libgcrypt.  An pointer to an empty string is returned for
   an unknown algorithm.  NULL is never returned. */
const char *
gcry_cipher_algo_name (int algorithm)
{
  return cipher_algo_to_string (algorithm);
}


/* Flag the cipher algorithm with the identifier ALGORITHM as
   disabled.  There is no error return, the function does nothing for
   unknown algorithms.  Disabled algorithms are vitually not available
   in Libgcrypt. */
static void
disable_cipher_algo (int algorithm)
{
  gcry_module_t cipher;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  cipher = _gcry_module_lookup_id (ciphers_registered, algorithm);
  if (cipher)
    {
      if (! (cipher->flags & FLAG_MODULE_DISABLED))
	cipher->flags |= FLAG_MODULE_DISABLED;
      _gcry_module_release (cipher);
    }
  ath_mutex_unlock (&ciphers_registered_lock);
}


/* Return 0 if the cipher algorithm with identifier ALGORITHM is
   available. Returns a basic error code value if it is not
   available.  */
static gcry_err_code_t
check_cipher_algo (int algorithm)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  gcry_module_t cipher;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  cipher = _gcry_module_lookup_id (ciphers_registered, algorithm);
  if (cipher)
    {
      if (cipher->flags & FLAG_MODULE_DISABLED)
	err = GPG_ERR_CIPHER_ALGO;
      _gcry_module_release (cipher);
    }
  else
    err = GPG_ERR_CIPHER_ALGO;
  ath_mutex_unlock (&ciphers_registered_lock);
  
  return err;
}


/* Return the standard length of the key for the cipher algorithm with
   the identifier ALGORITHM.  This function expects a valid algorithm
   and will abort if the algorithm is not available or the length of
   the key is not known. */
static unsigned int
cipher_get_keylen (int algorithm)
{
  gcry_module_t cipher;
  unsigned len = 0;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  cipher = _gcry_module_lookup_id (ciphers_registered, algorithm);
  if (cipher)
    {
      len = ((gcry_cipher_spec_t *) cipher->spec)->keylen;
      if (!len)
	log_bug ("cipher %d w/o key length\n", algorithm);
      _gcry_module_release (cipher);
    }
  else
    log_bug ("cipher %d not found\n", algorithm);
  ath_mutex_unlock (&ciphers_registered_lock);

  return len;
}

/* Return the block length of the cipher algorithm with the identifier
   ALGORITHM.  This function expects a valid algorithm and will abort
   if the algorithm is not available or the length of the key is not
   known. */
static unsigned int
cipher_get_blocksize (int algorithm)
{
  gcry_module_t cipher;
  unsigned len = 0;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  cipher = _gcry_module_lookup_id (ciphers_registered, algorithm);
  if (cipher)
    {
      len = ((gcry_cipher_spec_t *) cipher->spec)->blocksize;
      if (! len)
	  log_bug ("cipher %d w/o blocksize\n", algorithm);
      _gcry_module_release (cipher);
    }
  else
    log_bug ("cipher %d not found\n", algorithm);
  ath_mutex_unlock (&ciphers_registered_lock);

  return len;
}


/*
   Open a cipher handle for use with cipher algorithm ALGORITHM, using
   the cipher mode MODE (one of the GCRY_CIPHER_MODE_*) and return a
   handle in HANDLE.  Put NULL into HANDLE and return an error code if
   something goes wrong.  FLAGS may be used to modify the
   operation.  The defined flags are:

   GCRY_CIPHER_SECURE:  allocate all internal buffers in secure memory.
   GCRY_CIPHER_ENABLE_SYNC:  Enable the sync operation as used in OpenPGP.
   GCRY_CIPHER_CBC_CTS:  Enable CTS mode.
   GCRY_CIPHER_CBC_MAC:  Enable MAC mode.

   Values for these flags may be combined using OR.
 */
gcry_error_t
gcry_cipher_open (gcry_cipher_hd_t *handle,
		  int algo, int mode, unsigned int flags)
{
  int secure = (flags & GCRY_CIPHER_SECURE);
  gcry_cipher_spec_t *cipher = NULL;
  cipher_extra_spec_t *extraspec = NULL;
  gcry_module_t module = NULL;
  gcry_cipher_hd_t h = NULL;
  gcry_err_code_t err = 0;

  /* If the application missed to call the random poll function, we do
     it here to ensure that it is used once in a while. */
  _gcry_fast_random_poll ();
  
  REGISTER_DEFAULT_CIPHERS;

  /* Fetch the according module and check wether the cipher is marked
     available for use.  */
  ath_mutex_lock (&ciphers_registered_lock);
  module = _gcry_module_lookup_id (ciphers_registered, algo);
  if (module)
    {
      /* Found module.  */

      if (module->flags & FLAG_MODULE_DISABLED)
	{
	  /* Not available for use.  */
	  err = GPG_ERR_CIPHER_ALGO;
	  _gcry_module_release (module);
	}
      else
        {
          cipher = (gcry_cipher_spec_t *) module->spec;
          extraspec = module->extraspec;
        }
    }
  else
    err = GPG_ERR_CIPHER_ALGO;
  ath_mutex_unlock (&ciphers_registered_lock);

  /* check flags */
  if ((! err)
      && ((flags & ~(0 
		     | GCRY_CIPHER_SECURE
		     | GCRY_CIPHER_ENABLE_SYNC
		     | GCRY_CIPHER_CBC_CTS
		     | GCRY_CIPHER_CBC_MAC))
	  || (flags & GCRY_CIPHER_CBC_CTS & GCRY_CIPHER_CBC_MAC)))
    err = GPG_ERR_CIPHER_ALGO;

  /* check that a valid mode has been requested */
  if (! err)
    switch (mode)
      {
      case GCRY_CIPHER_MODE_ECB:
      case GCRY_CIPHER_MODE_CBC:
      case GCRY_CIPHER_MODE_CFB:
      case GCRY_CIPHER_MODE_OFB:
      case GCRY_CIPHER_MODE_CTR:
	if ((cipher->encrypt == dummy_encrypt_block)
	    || (cipher->decrypt == dummy_decrypt_block))
	  err = GPG_ERR_INV_CIPHER_MODE;
	break;

      case GCRY_CIPHER_MODE_STREAM:
	if ((cipher->stencrypt == dummy_encrypt_stream)
	    || (cipher->stdecrypt == dummy_decrypt_stream))
	  err = GPG_ERR_INV_CIPHER_MODE;
	break;

      case GCRY_CIPHER_MODE_NONE:
        /* This mode may be used for debugging.  It copies the main
           text verbatim to the ciphertext.  We do not allow this in
           fips mode or if no debug flag has been set.  */
	if (fips_mode () || !_gcry_get_debug_flag (0))
          err = GPG_ERR_INV_CIPHER_MODE;
	break;

      default:
	err = GPG_ERR_INV_CIPHER_MODE;
      }

  /* Perform selftest here and mark this with a flag in cipher_table?
     No, we should not do this as it takes too long.  Further it does
     not make sense to exclude algorithms with failing selftests at
     runtime: If a selftest fails there is something seriously wrong
     with the system and thus we better die immediately. */

  if (! err)
    {
      size_t size = (sizeof (*h)
                     + 2 * cipher->contextsize
                     - sizeof (cipher_context_alignment_t)
#ifdef NEED_16BYTE_ALIGNED_CONTEXT
                     + 15  /* Space for leading alignment gap.  */
#endif /*NEED_16BYTE_ALIGNED_CONTEXT*/
                     );

      if (secure)
	h = gcry_calloc_secure (1, size);
      else
	h = gcry_calloc (1, size);

      if (! h)
	err = gpg_err_code_from_errno (errno);
      else
	{
          size_t off = 0;

#ifdef NEED_16BYTE_ALIGNED_CONTEXT
          if ( ((unsigned long)h & 0x0f) )
            {
              /* The malloced block is not aligned on a 16 byte
                 boundary.  Correct for this.  */
              off = 16 - ((unsigned long)h & 0x0f);
              h = (void*)((char*)h + off);
            }
#endif /*NEED_16BYTE_ALIGNED_CONTEXT*/

	  h->magic = secure ? CTX_MAGIC_SECURE : CTX_MAGIC_NORMAL;
          h->actual_handle_size = size - off;
          h->handle_offset = off;
	  h->cipher = cipher;
	  h->extraspec = extraspec;
	  h->module = module;
          h->algo = algo;
	  h->mode = mode;
	  h->flags = flags;

          /* Setup bulk encryption routines.  */
          switch (algo)
            {
#ifdef USE_AES
            case GCRY_CIPHER_AES128:
            case GCRY_CIPHER_AES192:
            case GCRY_CIPHER_AES256:
              h->bulk.cfb_enc = _gcry_aes_cfb_enc;
              h->bulk.cfb_dec = _gcry_aes_cfb_dec;
              h->bulk.cbc_enc = _gcry_aes_cbc_enc;
              h->bulk.cbc_dec = _gcry_aes_cbc_dec;
              break;
#endif /*USE_AES*/
              
            default:
              break;
            }
	}
    }

  /* Done.  */

  if (err)
    {
      if (module)
	{
	  /* Release module.  */
	  ath_mutex_lock (&ciphers_registered_lock);
	  _gcry_module_release (module);
	  ath_mutex_unlock (&ciphers_registered_lock);
	}
    }

  *handle = err ? NULL : h;

  return gcry_error (err);
}


/* Release all resources associated with the cipher handle H. H may be
   NULL in which case this is a no-operation. */
void
gcry_cipher_close (gcry_cipher_hd_t h)
{
  size_t off;

  if (!h)
    return;

  if ((h->magic != CTX_MAGIC_SECURE)
      && (h->magic != CTX_MAGIC_NORMAL))
    _gcry_fatal_error(GPG_ERR_INTERNAL,
		      "gcry_cipher_close: already closed/invalid handle");
  else
    h->magic = 0;

  /* Release module.  */
  ath_mutex_lock (&ciphers_registered_lock);
  _gcry_module_release (h->module);
  ath_mutex_unlock (&ciphers_registered_lock);

  /* We always want to wipe out the memory even when the context has
     been allocated in secure memory.  The user might have disabled
     secure memory or is using his own implementation which does not
     do the wiping.  To accomplish this we need to keep track of the
     actual size of this structure because we have no way to known
     how large the allocated area was when using a standard malloc. */
  off = h->handle_offset;
  wipememory (h, h->actual_handle_size);

  gcry_free ((char*)h - off);
}


/* Set the key to be used for the encryption context C to KEY with
   length KEYLEN.  The length should match the required length. */
static gcry_error_t
cipher_setkey (gcry_cipher_hd_t c, byte *key, unsigned int keylen)
{
  gcry_err_code_t ret;

  ret = (*c->cipher->setkey) (&c->context.c, key, keylen);
  if (!ret)
    {
      /* Duplicate initial context.  */
      memcpy ((void *) ((char *) &c->context.c + c->cipher->contextsize),
              (void *) &c->context.c,
              c->cipher->contextsize);
    }

  return gcry_error (ret);
}


/* Set the IV to be used for the encryption context C to IV with
   length IVLEN.  The length should match the required length. */
static void
cipher_setiv( gcry_cipher_hd_t c, const byte *iv, unsigned ivlen )
{
  memset (c->u_iv.iv, 0, c->cipher->blocksize);
  if (iv) 
    {
      if (ivlen != c->cipher->blocksize)
        {
          log_info ("WARNING: cipher_setiv: ivlen=%u blklen=%u\n",
                    ivlen, (unsigned int)c->cipher->blocksize);
          fips_signal_error ("IV length does not match blocklength");
        }
      if (ivlen > c->cipher->blocksize)
        ivlen = c->cipher->blocksize;
      memcpy (c->u_iv.iv, iv, ivlen);
    }
  c->unused = 0;
}


/* Reset the cipher context to the initial context.  This is basically
   the same as an release followed by a new. */
static void
cipher_reset (gcry_cipher_hd_t c)
{
  memcpy (&c->context.c,
	  (char *) &c->context.c + c->cipher->contextsize,
	  c->cipher->contextsize);
  memset (c->u_iv.iv, 0, c->cipher->blocksize);
  memset (c->lastiv, 0, c->cipher->blocksize);
  memset (c->ctr, 0, c->cipher->blocksize);
}


static void
do_ecb_encrypt( gcry_cipher_hd_t c, byte *outbuf, const byte *inbuf,
                unsigned int nblocks )
{
  unsigned int n;
  
  for (n=0; n < nblocks; n++ )
    {
      c->cipher->encrypt ( &c->context.c, outbuf, (byte*)/*arggg*/inbuf );
      inbuf  += c->cipher->blocksize;
      outbuf += c->cipher->blocksize;
    }
}

static void
do_ecb_decrypt( gcry_cipher_hd_t c, byte *outbuf, const byte *inbuf,
                unsigned int nblocks )
{
  unsigned int n;

  for (n=0; n < nblocks; n++ ) 
    {
      c->cipher->decrypt ( &c->context.c, outbuf, (byte*)/*arggg*/inbuf );
      inbuf  += c->cipher->blocksize;
      outbuf += c->cipher->blocksize;
    }
}


static void
do_cbc_encrypt (gcry_cipher_hd_t c, unsigned char *outbuf, 
                const unsigned char *inbuf, unsigned int nbytes )
{
  unsigned int n;
  unsigned char *ivp;
  int i;
  size_t blocksize = c->cipher->blocksize;
  unsigned nblocks = nbytes / blocksize;

  if ((c->flags & GCRY_CIPHER_CBC_CTS) && nbytes > blocksize) 
    {
      if ((nbytes % blocksize) == 0)
	nblocks--;
    }

  if (c->bulk.cbc_enc)
    {
      c->bulk.cbc_enc (&c->context.c, c->u_iv.iv, outbuf, inbuf, nblocks,
                       (c->flags & GCRY_CIPHER_CBC_MAC)); 
      inbuf  += nblocks * blocksize;
      if (!(c->flags & GCRY_CIPHER_CBC_MAC))
        outbuf += nblocks * blocksize;
    }
  else
    {
      for (n=0; n < nblocks; n++ )
        {
          for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
            outbuf[i] = inbuf[i] ^ *ivp++;
          c->cipher->encrypt ( &c->context.c, outbuf, outbuf );
          memcpy (c->u_iv.iv, outbuf, blocksize );
          inbuf  += blocksize;
          if (!(c->flags & GCRY_CIPHER_CBC_MAC))
            outbuf += blocksize;
        }
    }

  if ((c->flags & GCRY_CIPHER_CBC_CTS) && nbytes > blocksize)
    {
      /* We have to be careful here, since outbuf might be equal to
         inbuf.  */
      int restbytes;
      unsigned char b;

      if ((nbytes % blocksize) == 0)
        restbytes = blocksize;
      else
        restbytes = nbytes % blocksize;

      outbuf -= blocksize;
      for (ivp = c->u_iv.iv, i = 0; i < restbytes; i++)
        {
          b = inbuf[i];
          outbuf[blocksize + i] = outbuf[i];
          outbuf[i] = b ^ *ivp++;
        }
      for (; i < blocksize; i++)
        outbuf[i] = 0 ^ *ivp++;
      
      c->cipher->encrypt (&c->context.c, outbuf, outbuf);
      memcpy (c->u_iv.iv, outbuf, blocksize);
    }
}


static void
do_cbc_decrypt (gcry_cipher_hd_t c, unsigned char *outbuf, 
                const unsigned char *inbuf, unsigned int nbytes)
{
  unsigned int n;
  unsigned char *ivp;
  int i;
  size_t blocksize = c->cipher->blocksize;
  unsigned int nblocks = nbytes / blocksize;

  if ((c->flags & GCRY_CIPHER_CBC_CTS) && nbytes > blocksize)
    {
      nblocks--;
      if ((nbytes % blocksize) == 0)
	nblocks--;
      memcpy (c->lastiv, c->u_iv.iv, blocksize);
    }

  if (c->bulk.cbc_dec)
    {
      c->bulk.cbc_dec (&c->context.c, c->u_iv.iv, outbuf, inbuf, nblocks); 
      inbuf  += nblocks * blocksize;
      outbuf += nblocks * blocksize;
    }
  else
    {
      for (n=0; n < nblocks; n++ ) 
        {
          /* Because outbuf and inbuf might be the same, we have to
           * save the original ciphertext block.  We use LASTIV for
           * this here because it is not used otherwise. */
          memcpy (c->lastiv, inbuf, blocksize);
          c->cipher->decrypt ( &c->context.c, outbuf, inbuf );
          for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
	    outbuf[i] ^= *ivp++;
          memcpy(c->u_iv.iv, c->lastiv, blocksize );
          inbuf  += c->cipher->blocksize;
          outbuf += c->cipher->blocksize;
        }
    }

  if ((c->flags & GCRY_CIPHER_CBC_CTS) && nbytes > blocksize) 
    {
      int restbytes;
      
      if ((nbytes % blocksize) == 0)
        restbytes = blocksize;
      else
        restbytes = nbytes % blocksize;
      
      memcpy (c->lastiv, c->u_iv.iv, blocksize );         /* Save Cn-2. */
      memcpy (c->u_iv.iv, inbuf + blocksize, restbytes ); /* Save Cn. */

      c->cipher->decrypt ( &c->context.c, outbuf, inbuf );
      for (ivp=c->u_iv.iv,i=0; i < restbytes; i++ )
        outbuf[i] ^= *ivp++;
      
      memcpy(outbuf + blocksize, outbuf, restbytes);
      for(i=restbytes; i < blocksize; i++)
        c->u_iv.iv[i] = outbuf[i];
      c->cipher->decrypt (&c->context.c, outbuf, c->u_iv.iv);
      for(ivp=c->lastiv,i=0; i < blocksize; i++ )
        outbuf[i] ^= *ivp++;
      /* c->lastiv is now really lastlastiv, does this matter? */
    }
}


static void
do_cfb_encrypt( gcry_cipher_hd_t c, unsigned char *outbuf, 
                const unsigned char *inbuf, unsigned int nbytes )
{
  unsigned char *ivp;
  size_t blocksize = c->cipher->blocksize;
  size_t blocksize_x_2 = blocksize + blocksize;
  
  if ( nbytes <= c->unused )
    {
      /* Short enough to be encoded by the remaining XOR mask. */
      /* XOR the input with the IV and store input into IV. */
      for (ivp=c->u_iv.iv+c->cipher->blocksize - c->unused;
           nbytes;
           nbytes--, c->unused-- )
        *outbuf++ = (*ivp++ ^= *inbuf++);
      return;
    }

  if ( c->unused )
    {
      /* XOR the input with the IV and store input into IV */
      nbytes -= c->unused;
      for(ivp=c->u_iv.iv+blocksize - c->unused; c->unused; c->unused-- )
        *outbuf++ = (*ivp++ ^= *inbuf++);
    }

  /* Now we can process complete blocks.  We use a loop as long as we
     have at least 2 blocks and use conditions for the rest.  This
     also allows to use a bulk encryption function if available.  */
  if (nbytes >= blocksize_x_2 && c->bulk.cfb_enc)
    {
      unsigned int nblocks = nbytes / blocksize;
      c->bulk.cfb_enc (&c->context.c, c->u_iv.iv, outbuf, inbuf, nblocks); 
      outbuf += nblocks * blocksize;
      inbuf  += nblocks * blocksize;
      nbytes -= nblocks * blocksize;
    }
  else
    {
      while ( nbytes >= blocksize_x_2 )
        {
          int i;
          /* Encrypt the IV. */
          c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
          /* XOR the input with the IV and store input into IV.  */
          for(ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
            *outbuf++ = (*ivp++ ^= *inbuf++);
          nbytes -= blocksize;
        }
    }

  if ( nbytes >= blocksize )
    {
      int i;
      /* Save the current IV and then encrypt the IV. */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      /* XOR the input with the IV and store input into IV */
      for(ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
        *outbuf++ = (*ivp++ ^= *inbuf++);
      nbytes -= blocksize;
    }
  if ( nbytes ) 
    {
      /* Save the current IV and then encrypt the IV. */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      c->unused = blocksize;
      /* Apply the XOR. */
      c->unused -= nbytes;
      for(ivp=c->u_iv.iv; nbytes; nbytes-- )
        *outbuf++ = (*ivp++ ^= *inbuf++);
    }
}


static void
do_cfb_decrypt( gcry_cipher_hd_t c, unsigned char *outbuf, 
                const unsigned char *inbuf, unsigned int nbytes )
{
  unsigned char *ivp;
  unsigned long temp;
  int i;
  size_t blocksize = c->cipher->blocksize;
  size_t blocksize_x_2 = blocksize + blocksize;
  
  if (nbytes <= c->unused)
    {
      /* Short enough to be encoded by the remaining XOR mask. */
      /* XOR the input with the IV and store input into IV. */
      for (ivp=c->u_iv.iv+blocksize - c->unused;
           nbytes; 
           nbytes--, c->unused--)
        {
          temp = *inbuf++;
          *outbuf++ = *ivp ^ temp;
          *ivp++ = temp;
        }
      return;
    }
  
  if (c->unused)
    {
      /* XOR the input with the IV and store input into IV. */
      nbytes -= c->unused;
      for (ivp=c->u_iv.iv+blocksize - c->unused; c->unused; c->unused-- )
        {
          temp = *inbuf++;
          *outbuf++ = *ivp ^ temp;
          *ivp++ = temp;
        }
    }
  
  /* Now we can process complete blocks.  We use a loop as long as we
     have at least 2 blocks and use conditions for the rest.  This
     also allows to use a bulk encryption function if available.  */
  if (nbytes >= blocksize_x_2 && c->bulk.cfb_dec)
    {
      unsigned int nblocks = nbytes / blocksize;
      c->bulk.cfb_dec (&c->context.c, c->u_iv.iv, outbuf, inbuf, nblocks); 
      outbuf += nblocks * blocksize;
      inbuf  += nblocks * blocksize;
      nbytes -= nblocks * blocksize;
    }
  else
    {
      while (nbytes >= blocksize_x_2 )
        {
          /* Encrypt the IV. */
          c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
          /* XOR the input with the IV and store input into IV. */
          for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
            {
              temp = *inbuf++;
              *outbuf++ = *ivp ^ temp;
              *ivp++ = temp;
            }
          nbytes -= blocksize;
        }
    }

  if (nbytes >= blocksize )
    {
      /* Save the current IV and then encrypt the IV. */
      memcpy ( c->lastiv, c->u_iv.iv, blocksize);
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      /* XOR the input with the IV and store input into IV */
      for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
        {
          temp = *inbuf++;
          *outbuf++ = *ivp ^ temp;
          *ivp++ = temp;
        }
      nbytes -= blocksize;
    }

  if (nbytes)
    { 
      /* Save the current IV and then encrypt the IV. */
      memcpy ( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      c->unused = blocksize;
      /* Apply the XOR. */
      c->unused -= nbytes;
      for (ivp=c->u_iv.iv; nbytes; nbytes-- )
        {
          temp = *inbuf++;
          *outbuf++ = *ivp ^ temp;
          *ivp++ = temp;
        }
    }
}


static void
do_ofb_encrypt( gcry_cipher_hd_t c,
                byte *outbuf, const byte *inbuf, unsigned nbytes )
{
  byte *ivp;
  size_t blocksize = c->cipher->blocksize;

  if ( nbytes <= c->unused )
    {
      /* Short enough to be encoded by the remaining XOR mask. */
      /* XOR the input with the IV */
      for (ivp=c->u_iv.iv+c->cipher->blocksize - c->unused;
           nbytes;
           nbytes--, c->unused-- )
        *outbuf++ = (*ivp++ ^ *inbuf++);
      return;
    }

  if( c->unused )
    {
      nbytes -= c->unused;
      for(ivp=c->u_iv.iv+blocksize - c->unused; c->unused; c->unused-- )
        *outbuf++ = (*ivp++ ^ *inbuf++);
    }

  /* Now we can process complete blocks. */
  while ( nbytes >= blocksize )
    {
      int i;
      /* Encrypt the IV (and save the current one). */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      
      for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
        *outbuf++ = (*ivp++ ^ *inbuf++);
      nbytes -= blocksize;
    }
  if ( nbytes )
    { /* process the remaining bytes */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      c->unused = blocksize;
      c->unused -= nbytes;
      for(ivp=c->u_iv.iv; nbytes; nbytes-- )
        *outbuf++ = (*ivp++ ^ *inbuf++);
    }
}

static void
do_ofb_decrypt( gcry_cipher_hd_t c,
                byte *outbuf, const byte *inbuf, unsigned int nbytes )
{
  byte *ivp;
  size_t blocksize = c->cipher->blocksize;
  
  if( nbytes <= c->unused )
    {
      /* Short enough to be encoded by the remaining XOR mask. */
      for (ivp=c->u_iv.iv+blocksize - c->unused; nbytes; nbytes--,c->unused--)
        *outbuf++ = *ivp++ ^ *inbuf++;
      return;
    }

  if ( c->unused )
    {
      nbytes -= c->unused;
      for (ivp=c->u_iv.iv+blocksize - c->unused; c->unused; c->unused-- )
        *outbuf++ = *ivp++ ^ *inbuf++;
    }

  /* Now we can process complete blocks. */
  while ( nbytes >= blocksize )
    {
      int i;
      /* Encrypt the IV (and save the current one). */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      for (ivp=c->u_iv.iv,i=0; i < blocksize; i++ )
        *outbuf++ = *ivp++ ^ *inbuf++;
      nbytes -= blocksize;
    }
  if ( nbytes ) 
    { /* Process the remaining bytes. */
      /* Encrypt the IV (and save the current one). */
      memcpy( c->lastiv, c->u_iv.iv, blocksize );
      c->cipher->encrypt ( &c->context.c, c->u_iv.iv, c->u_iv.iv );
      c->unused = blocksize;
      c->unused -= nbytes;
      for (ivp=c->u_iv.iv; nbytes; nbytes-- )
        *outbuf++ = *ivp++ ^ *inbuf++;
    }
}


static void
do_ctr_encrypt( gcry_cipher_hd_t c, byte *outbuf, const byte *inbuf,
                unsigned int nbytes )
{
  unsigned int n;
  byte tmp[MAX_BLOCKSIZE];
  int i;

  for(n=0; n < nbytes; n++)
    {
      if ((n % c->cipher->blocksize) == 0)
	{
	  c->cipher->encrypt (&c->context.c, tmp, c->ctr);

	  for (i = c->cipher->blocksize; i > 0; i--)
	    {
	      c->ctr[i-1]++;
	      if (c->ctr[i-1] != 0)
		break;
	    }
	}

      /* XOR input with encrypted counter and store in output. */
      outbuf[n] = inbuf[n] ^ tmp[n % c->cipher->blocksize];
    }
}

static void
do_ctr_decrypt( gcry_cipher_hd_t c, byte *outbuf, const byte *inbuf,
                unsigned int nbytes )
{
  do_ctr_encrypt (c, outbuf, inbuf, nbytes);
}


/****************
 * Encrypt INBUF to OUTBUF with the mode selected at open.
 * inbuf and outbuf may overlap or be the same.
 * Depending on the mode some contraints apply to NBYTES.
 */
static gcry_err_code_t
cipher_encrypt (gcry_cipher_hd_t c, byte *outbuf,
		const byte *inbuf, unsigned int nbytes)
{
    gcry_err_code_t rc = GPG_ERR_NO_ERROR;

    switch( c->mode ) {
      case GCRY_CIPHER_MODE_ECB:
	if (!(nbytes%c->cipher->blocksize))
            do_ecb_encrypt(c, outbuf, inbuf, nbytes/c->cipher->blocksize );
        else 
            rc = GPG_ERR_INV_ARG;
	break;
      case GCRY_CIPHER_MODE_CBC:
	if (!(nbytes%c->cipher->blocksize)
            || (nbytes > c->cipher->blocksize
                && (c->flags & GCRY_CIPHER_CBC_CTS)))
            do_cbc_encrypt(c, outbuf, inbuf, nbytes );
        else 
            rc = GPG_ERR_INV_ARG;
	break;
      case GCRY_CIPHER_MODE_CFB:
	do_cfb_encrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_OFB:
	do_ofb_encrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_CTR:
	do_ctr_encrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_STREAM:
        c->cipher->stencrypt ( &c->context.c,
                               outbuf, (byte*)/*arggg*/inbuf, nbytes );
        break;
      case GCRY_CIPHER_MODE_NONE:
       	if (fips_mode () || !_gcry_get_debug_flag (0))
          {
            fips_signal_error ("cipher mode NONE used");
            rc = GPG_ERR_INV_CIPHER_MODE;
          }
        else
          {
            if ( inbuf != outbuf )
              memmove (outbuf, inbuf, nbytes);
          }
	break;
      default:
        log_fatal("cipher_encrypt: invalid mode %d\n", c->mode );
        rc = GPG_ERR_INV_CIPHER_MODE;
        break;
    }
    return rc;
}


/****************
 * Encrypt IN and write it to OUT.  If IN is NULL, in-place encryption has
 * been requested.
 */
gcry_error_t
gcry_cipher_encrypt (gcry_cipher_hd_t h, void *out, size_t outsize,
                     const void *in, size_t inlen)
{
  gcry_err_code_t err;

  if (!in)
    {
      /* Caller requested in-place encryption. */
      /* Actually cipher_encrypt() does not need to know about it, but
       * we may change it in the future to get better performance.  */
      err = cipher_encrypt (h, out, out, outsize);
    }
  else if (outsize < ((h->flags & GCRY_CIPHER_CBC_MAC) ?
                      h->cipher->blocksize : inlen))
    err = GPG_ERR_TOO_SHORT;
  else if ((h->mode == GCRY_CIPHER_MODE_ECB
	    || (h->mode == GCRY_CIPHER_MODE_CBC
		&& (! ((h->flags & GCRY_CIPHER_CBC_CTS)
		       && (inlen > h->cipher->blocksize)))))
	   && (inlen % h->cipher->blocksize))
    err = GPG_ERR_INV_ARG;
  else
    err = cipher_encrypt (h, out, in, inlen);

  if (err && out)
    memset (out, 0x42, outsize); /* Failsafe: Make sure that the
                                    plaintext will never make it into
                                    OUT. */

  return gcry_error (err);
}



/****************
 * Decrypt INBUF to OUTBUF with the mode selected at open.
 * inbuf and outbuf may overlap or be the same.
 * Depending on the mode some some contraints apply to NBYTES.
 */
static gcry_err_code_t
cipher_decrypt (gcry_cipher_hd_t c, byte *outbuf, const byte *inbuf,
		unsigned int nbytes)
{
    gcry_err_code_t rc = GPG_ERR_NO_ERROR;

    switch( c->mode ) {
      case GCRY_CIPHER_MODE_ECB:
	if (!(nbytes%c->cipher->blocksize))
            do_ecb_decrypt(c, outbuf, inbuf, nbytes/c->cipher->blocksize );
        else 
            rc = GPG_ERR_INV_ARG;
	break;
      case GCRY_CIPHER_MODE_CBC:
	if (!(nbytes%c->cipher->blocksize)
            || (nbytes > c->cipher->blocksize
                && (c->flags & GCRY_CIPHER_CBC_CTS)))
            do_cbc_decrypt(c, outbuf, inbuf, nbytes );
        else 
            rc = GPG_ERR_INV_ARG;
	break;
      case GCRY_CIPHER_MODE_CFB:
	do_cfb_decrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_OFB:
	do_ofb_decrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_CTR:
	do_ctr_decrypt(c, outbuf, inbuf, nbytes );
	break;
      case GCRY_CIPHER_MODE_STREAM:
        c->cipher->stdecrypt ( &c->context.c,
                               outbuf, (byte*)/*arggg*/inbuf, nbytes );
        break;
      case GCRY_CIPHER_MODE_NONE:
       	if (fips_mode () || !_gcry_get_debug_flag (0))
          {
            fips_signal_error ("cipher mode NONE used");
            rc = GPG_ERR_INV_CIPHER_MODE;
          }
        else
          {
            if (inbuf != outbuf)
              memmove (outbuf, inbuf, nbytes);
          }
	break;
      default:
        log_fatal ("cipher_decrypt: invalid mode %d\n", c->mode );
        rc = GPG_ERR_INV_CIPHER_MODE;
        break;
    }
    return rc;
}


gcry_error_t
gcry_cipher_decrypt (gcry_cipher_hd_t h, void *out, size_t outsize,
		     const void *in, size_t inlen)
{
  gcry_err_code_t err = 0;

  if (!in)
    {
      /* Caller requested in-place encryption. */
      /* Actually cipher_encrypt() does not need to know about it, but
       * we may change it in the future to get better performance.  */
      err = cipher_decrypt (h, out, out, outsize);
    }
  else if (outsize < inlen)
    err = GPG_ERR_TOO_SHORT;
  else if (((h->mode == GCRY_CIPHER_MODE_ECB)
	    || ((h->mode == GCRY_CIPHER_MODE_CBC)
		&& (! ((h->flags & GCRY_CIPHER_CBC_CTS)
		       && (inlen > h->cipher->blocksize)))))
	   && (inlen % h->cipher->blocksize) != 0)
    err = GPG_ERR_INV_ARG;
  else
    err = cipher_decrypt (h, out, in, inlen);

  return gcry_error (err);
}



/****************
 * Used for PGP's somewhat strange CFB mode. Only works if
 * the corresponding flag is set.
 */
static void
cipher_sync (gcry_cipher_hd_t c)
{
  if ((c->flags & GCRY_CIPHER_ENABLE_SYNC) && c->unused)
    {
      memmove (c->u_iv.iv + c->unused,
               c->u_iv.iv, c->cipher->blocksize - c->unused);
      memcpy (c->u_iv.iv,
              c->lastiv + c->cipher->blocksize - c->unused, c->unused);
      c->unused = 0;
    }
}


gcry_error_t
_gcry_cipher_setkey (gcry_cipher_hd_t hd, const void *key, size_t keylen)
{
  return cipher_setkey (hd, (void*)key, keylen);
}


gcry_error_t
_gcry_cipher_setiv (gcry_cipher_hd_t hd, const void *iv, size_t ivlen)
{
  cipher_setiv (hd, iv, ivlen);
  return 0;
}

/* Set counter for CTR mode.  (CTR,CTRLEN) must denote a buffer of
   block size length, or (NULL,0) to set the CTR to the all-zero
   block. */
gpg_error_t
_gcry_cipher_setctr (gcry_cipher_hd_t hd, const void *ctr, size_t ctrlen)
{
  if (ctr && ctrlen == hd->cipher->blocksize)
    memcpy (hd->ctr, ctr, hd->cipher->blocksize);
  else if (!ctr || !ctrlen)
    memset (hd->ctr, 0, hd->cipher->blocksize);
  else
    return gpg_error (GPG_ERR_INV_ARG);
  return 0;
}


gcry_error_t
gcry_cipher_ctl( gcry_cipher_hd_t h, int cmd, void *buffer, size_t buflen)
{
  gcry_err_code_t rc = GPG_ERR_NO_ERROR;

  switch (cmd)
    {
    case GCRYCTL_SET_KEY:  /* Deprecated; use gcry_cipher_setkey.  */
      rc = cipher_setkey( h, buffer, buflen );
      break;

    case GCRYCTL_SET_IV:   /* Deprecated; use gcry_cipher_setiv.  */
      cipher_setiv( h, buffer, buflen );
      break;

    case GCRYCTL_RESET:
      cipher_reset (h);
      break;

    case GCRYCTL_CFB_SYNC:
      cipher_sync( h );
      break;

    case GCRYCTL_SET_CBC_CTS:
      if (buflen)
	if (h->flags & GCRY_CIPHER_CBC_MAC)
	  rc = GPG_ERR_INV_FLAG;
	else
	  h->flags |= GCRY_CIPHER_CBC_CTS;
      else
	h->flags &= ~GCRY_CIPHER_CBC_CTS;
      break;

    case GCRYCTL_SET_CBC_MAC:
      if (buflen)
	if (h->flags & GCRY_CIPHER_CBC_CTS)
	  rc = GPG_ERR_INV_FLAG;
	else
	  h->flags |= GCRY_CIPHER_CBC_MAC;
      else
	h->flags &= ~GCRY_CIPHER_CBC_MAC;
      break;

    case GCRYCTL_DISABLE_ALGO:
      /* This command expects NULL for H and BUFFER to point to an
         integer with the algo number.  */
      if( h || !buffer || buflen != sizeof(int) )
	return gcry_error (GPG_ERR_CIPHER_ALGO);
      disable_cipher_algo( *(int*)buffer );
      break;

    case GCRYCTL_SET_CTR: /* Deprecated; use gcry_cipher_setctr.  */
      if (buffer && buflen == h->cipher->blocksize)
	memcpy (h->ctr, buffer, h->cipher->blocksize);
      else if (buffer == NULL || buflen == 0)
	memset (h->ctr, 0, h->cipher->blocksize);
      else
	rc = GPG_ERR_INV_ARG;
      break;

    case 61:  /* Disable weak key detection (private).  */
      if (h->extraspec->set_extra_info)
        rc = h->extraspec->set_extra_info 
          (&h->context.c, CIPHER_INFO_NO_WEAK_KEY, NULL, 0);
      else
        rc = GPG_ERR_NOT_SUPPORTED;
      break;

    case 62: /* Return current input vector (private).  */
      /* This is the input block as used in CFB and OFB mode which has
         initially been set as IV.  The returned format is: 
           1 byte  Actual length of the block in bytes.
           n byte  The block.
         If the provided buffer is too short, an error is returned. */
      if (buflen < (1 + h->cipher->blocksize))
        rc = GPG_ERR_TOO_SHORT;
      else
        {
          unsigned char *ivp;
          unsigned char *dst = buffer;
          int n = h->unused;
          
          if (!n)
            n = h->cipher->blocksize;
          gcry_assert (n <= h->cipher->blocksize);
          *dst++ = n;
          ivp = h->u_iv.iv + h->cipher->blocksize - n;
          while (n--)
            *dst++ = *ivp++;
        }
      break;

    default:
      rc = GPG_ERR_INV_OP;
    }

  return gcry_error (rc);
}


/* Return information about the cipher handle H.  CMD is the kind of
   information requested.  BUFFER and NBYTES are reserved for now.

   There are no values for CMD yet defined.  

   The fucntion always returns GPG_ERR_INV_OP.
   
 */
gcry_error_t
gcry_cipher_info (gcry_cipher_hd_t h, int cmd, void *buffer, size_t *nbytes)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  (void)h;
  (void)buffer;
  (void)nbytes;

  switch (cmd)
    {
    default:
      err = GPG_ERR_INV_OP;
    }

  return gcry_error (err);
}

/* Return information about the given cipher algorithm ALGO.

   WHAT select the kind of information returned:

    GCRYCTL_GET_KEYLEN:
  	Return the length of the key.  If the algorithm ALGO
  	supports multiple key lengths, the maximum supported key length
  	is returned.  The key length is returned as number of octets.
  	BUFFER and NBYTES must be zero.

    GCRYCTL_GET_BLKLEN:
  	Return the blocklength of the algorithm ALGO counted in octets.
  	BUFFER and NBYTES must be zero.

    GCRYCTL_TEST_ALGO:
  	Returns 0 if the specified algorithm ALGO is available for use.
  	BUFFER and NBYTES must be zero.
  
   Note: Because this function is in most cases used to return an
   integer value, we can make it easier for the caller to just look at
   the return value.  The caller will in all cases consult the value
   and thereby detecting whether a error occured or not (i.e. while
   checking the block size)
 */
gcry_error_t
gcry_cipher_algo_info (int algo, int what, void *buffer, size_t *nbytes)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  unsigned int ui;

  switch (what)
    {
    case GCRYCTL_GET_KEYLEN:
      if (buffer || (! nbytes))
	err = GPG_ERR_CIPHER_ALGO;
      else
	{
	  ui = cipher_get_keylen (algo);
	  if ((ui > 0) && (ui <= 512))
	    *nbytes = (size_t) ui / 8;
	  else
	    /* The only reason is an invalid algo or a strange
	       blocksize.  */
	    err = GPG_ERR_CIPHER_ALGO;
	}
      break;

    case GCRYCTL_GET_BLKLEN:
      if (buffer || (! nbytes))
	err = GPG_ERR_CIPHER_ALGO;
      else
	{
	  ui = cipher_get_blocksize (algo);
	  if ((ui > 0) && (ui < 10000))
	    *nbytes = ui;
	  else
	    /* The only reason is an invalid algo or a strange
	       blocksize.  */
	    err = GPG_ERR_CIPHER_ALGO;
	}
      break;

    case GCRYCTL_TEST_ALGO:
      if (buffer || nbytes)
	err = GPG_ERR_INV_ARG;
      else
	err = check_cipher_algo (algo);
      break;

      default:
	err = GPG_ERR_INV_OP;
    }

  return gcry_error (err);
}


/* This function returns length of the key for algorithm ALGO.  If the
   algorithm supports multiple key lengths, the maximum supported key
   length is returned.  On error 0 is returned.  The key length is
   returned as number of octets.

   This is a convenience functions which should be preferred over
   gcry_cipher_algo_info because it allows for proper type
   checking.  */
size_t
gcry_cipher_get_algo_keylen (int algo) 
{
  size_t n;

  if (gcry_cipher_algo_info (algo, GCRYCTL_GET_KEYLEN, NULL, &n))
    n = 0;
  return n;
}

/* This functions returns the blocklength of the algorithm ALGO
   counted in octets.  On error 0 is returned.

   This is a convenience functions which should be preferred over
   gcry_cipher_algo_info because it allows for proper type
   checking.  */
size_t
gcry_cipher_get_algo_blklen (int algo) 
{
  size_t n;

  if (gcry_cipher_algo_info( algo, GCRYCTL_GET_BLKLEN, NULL, &n))
    n = 0;
  return n;
}

/* Explicitly initialize this module.  */
gcry_err_code_t
_gcry_cipher_init (void)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  REGISTER_DEFAULT_CIPHERS;

  return err;
}

/* Get a list consisting of the IDs of the loaded cipher modules.  If
   LIST is zero, write the number of loaded cipher modules to
   LIST_LENGTH and return.  If LIST is non-zero, the first
   *LIST_LENGTH algorithm IDs are stored in LIST, which must be of
   according size.  In case there are less cipher modules than
   *LIST_LENGTH, *LIST_LENGTH is updated to the correct number.  */
gcry_error_t
gcry_cipher_list (int *list, int *list_length)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;

  ath_mutex_lock (&ciphers_registered_lock);
  err = _gcry_module_list (ciphers_registered, list, list_length);
  ath_mutex_unlock (&ciphers_registered_lock);

  return err;
}


/* Run the selftests for cipher algorithm ALGO with optional reporting
   function REPORT.  */
gpg_error_t
_gcry_cipher_selftest (int algo, int extended, selftest_report_func_t report)
{
  gcry_module_t module = NULL;
  cipher_extra_spec_t *extraspec = NULL;
  gcry_err_code_t ec = 0;

  REGISTER_DEFAULT_CIPHERS;

  ath_mutex_lock (&ciphers_registered_lock);
  module = _gcry_module_lookup_id (ciphers_registered, algo);
  if (module && !(module->flags & FLAG_MODULE_DISABLED))
    extraspec = module->extraspec;
  ath_mutex_unlock (&ciphers_registered_lock);
  if (extraspec && extraspec->selftest)
    ec = extraspec->selftest (algo, extended, report);
  else
    {
      ec = GPG_ERR_CIPHER_ALGO;
      if (report)
        report ("cipher", algo, "module", 
                module && !(module->flags & FLAG_MODULE_DISABLED)?
                "no selftest available" :
                module? "algorithm disabled" : "algorithm not found");
    }

  if (module)
    {
      ath_mutex_lock (&ciphers_registered_lock);
      _gcry_module_release (module);
      ath_mutex_unlock (&ciphers_registered_lock);
    }
  return gpg_error (ec);
}
