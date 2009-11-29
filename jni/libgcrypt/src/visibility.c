/* visibility.c - Wrapper for all public functions.
 * Copyright (C) 2007, 2008  Free Software Foundation, Inc.
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
#include <stdarg.h>

#define _GCRY_INCLUDED_BY_VISIBILITY_C 
#include "g10lib.h"
#include "cipher-proto.h"



const char *
gcry_strerror (gcry_error_t err)
{
  return _gcry_strerror (err);
}

const char *
gcry_strsource (gcry_error_t err)
{
  return _gcry_strsource (err);
}

gcry_err_code_t 
gcry_err_code_from_errno (int err)
{
  return _gcry_err_code_from_errno (err);
}

int
gcry_err_code_to_errno (gcry_err_code_t code)
{
  return _gcry_err_code_to_errno (code);
}

gcry_error_t 
gcry_err_make_from_errno (gcry_err_source_t source, int err)
{
  return _gcry_err_make_from_errno (source, err);
}

gcry_err_code_t 
gcry_error_from_errno (int err)
{
  return _gcry_error_from_errno (err);
}

const char *
gcry_check_version (const char *req_version)
{
  return _gcry_check_version (req_version);
}

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

gcry_error_t 
gcry_sexp_new (gcry_sexp_t *retsexp,
               const void *buffer, size_t length,
               int autodetect)
{
  return _gcry_sexp_new (retsexp, buffer, length, autodetect);
}

gcry_error_t 
gcry_sexp_create (gcry_sexp_t *retsexp,
                  void *buffer, size_t length,
                  int autodetect, void (*freefnc) (void *))
{
  return _gcry_sexp_create (retsexp, buffer, length,
                            autodetect, freefnc);
}

gcry_error_t 
gcry_sexp_sscan (gcry_sexp_t *retsexp, size_t *erroff,
                 const char *buffer, size_t length)
{
  return _gcry_sexp_sscan (retsexp, erroff, buffer, length);
}

gcry_error_t 
gcry_sexp_build (gcry_sexp_t *retsexp, size_t *erroff,
                 const char *format, ...)
{
  gcry_error_t err;
  va_list arg_ptr;
  
  va_start (arg_ptr, format);
  err = _gcry_sexp_vbuild (retsexp, erroff, format, arg_ptr);
  va_end (arg_ptr);
  return err;
}

gcry_error_t 
gcry_sexp_build_array (gcry_sexp_t *retsexp, size_t *erroff,
                       const char *format, void **arg_list)
{
  return _gcry_sexp_build_array (retsexp, erroff, format, arg_list);
}

void 
gcry_sexp_release (gcry_sexp_t sexp)
{
  _gcry_sexp_release (sexp);
}

size_t 
gcry_sexp_canon_len (const unsigned char *buffer, size_t length, 
                     size_t *erroff, gcry_error_t *errcode)
{
  return _gcry_sexp_canon_len (buffer, length, erroff, errcode);
}

size_t 
gcry_sexp_sprint (gcry_sexp_t sexp, int mode, void *buffer, size_t maxlength)
{
  return _gcry_sexp_sprint (sexp, mode, buffer, maxlength);
}

void 
gcry_sexp_dump (const gcry_sexp_t a)
{
  _gcry_sexp_dump (a);
}

gcry_sexp_t
gcry_sexp_cons (const gcry_sexp_t a, const gcry_sexp_t b)
{
  return _gcry_sexp_cons (a, b);
}

gcry_sexp_t
gcry_sexp_alist (const gcry_sexp_t *array)
{
  return _gcry_sexp_alist (array);
}

gcry_sexp_t
gcry_sexp_vlist (const gcry_sexp_t a, ...)
{
  /* This is not yet implemented in sexp.c.  */
  (void)a;
  BUG ();
  return NULL;
}

gcry_sexp_t
gcry_sexp_append (const gcry_sexp_t a, const gcry_sexp_t n)
{
  return _gcry_sexp_append (a, n);
}

gcry_sexp_t
gcry_sexp_prepend (const gcry_sexp_t a, const gcry_sexp_t n)
{
  return _gcry_sexp_prepend (a, n);
}


gcry_sexp_t
gcry_sexp_find_token (gcry_sexp_t list, const char *tok, size_t toklen)
{
  return _gcry_sexp_find_token (list, tok, toklen);
}

int
gcry_sexp_length (const gcry_sexp_t list)
{
  return _gcry_sexp_length (list);
}

gcry_sexp_t
gcry_sexp_nth (const gcry_sexp_t list, int number)
{
  return _gcry_sexp_nth (list, number);
}

gcry_sexp_t
gcry_sexp_car (const gcry_sexp_t list)
{
  return _gcry_sexp_car (list);
}

gcry_sexp_t
gcry_sexp_cdr (const gcry_sexp_t list)
{
  return _gcry_sexp_cdr (list);
}

gcry_sexp_t
gcry_sexp_cadr (const gcry_sexp_t list)
{
  return _gcry_sexp_cadr (list);
}

const char *
gcry_sexp_nth_data (const gcry_sexp_t list, int number, size_t *datalen)
{
  return _gcry_sexp_nth_data (list, number, datalen);
}

char *
gcry_sexp_nth_string (gcry_sexp_t list, int number)
{
  return _gcry_sexp_nth_string (list, number);
}

gcry_mpi_t
gcry_sexp_nth_mpi (gcry_sexp_t list, int number, int mpifmt)
{
  return _gcry_sexp_nth_mpi (list, number, mpifmt);
}

gcry_mpi_t
gcry_mpi_new (unsigned int nbits)
{
  return _gcry_mpi_new (nbits);
}

gcry_mpi_t
gcry_mpi_snew (unsigned int nbits)
{
  return _gcry_mpi_snew (nbits);
}

void 
gcry_mpi_release (gcry_mpi_t a)
{
  _gcry_mpi_release (a);
}

gcry_mpi_t
gcry_mpi_copy (const gcry_mpi_t a)
{
  return _gcry_mpi_copy (a);
}

gcry_mpi_t
gcry_mpi_set (gcry_mpi_t w, const gcry_mpi_t u)
{
  return _gcry_mpi_set (w, u);
}

gcry_mpi_t
gcry_mpi_set_ui (gcry_mpi_t w, unsigned long u)
{
  return _gcry_mpi_set_ui (w, u);
}

void
gcry_mpi_swap (gcry_mpi_t a, gcry_mpi_t b)
{
  _gcry_mpi_swap (a, b);
}

int
gcry_mpi_cmp (const gcry_mpi_t u, const gcry_mpi_t v)
{
  return _gcry_mpi_cmp (u, v);
}

int
gcry_mpi_cmp_ui (const gcry_mpi_t u, unsigned long v)
{
  return _gcry_mpi_cmp_ui (u, v);
}

gcry_error_t
gcry_mpi_scan (gcry_mpi_t *ret_mpi, enum gcry_mpi_format format,
               const void *buffer, size_t buflen, 
               size_t *nscanned)
{
  return _gcry_mpi_scan (ret_mpi, format, buffer, buflen, nscanned);
}

gcry_error_t
gcry_mpi_print (enum gcry_mpi_format format,
                unsigned char *buffer, size_t buflen,
                size_t *nwritten,
                const gcry_mpi_t a)
{
  return _gcry_mpi_print (format, buffer, buflen, nwritten, a);
}

gcry_error_t
gcry_mpi_aprint (enum gcry_mpi_format format,
                 unsigned char **buffer, size_t *nwritten,
                 const gcry_mpi_t a)
{
  return _gcry_mpi_aprint (format, buffer, nwritten, a);
}

void
gcry_mpi_dump (const gcry_mpi_t a)
{
  _gcry_mpi_dump (a);
}

void
gcry_mpi_add (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
  _gcry_mpi_add (w, u, v);
}

void
gcry_mpi_add_ui (gcry_mpi_t w, gcry_mpi_t u, unsigned long v)
{
  _gcry_mpi_add_ui (w, u, v);
}

void
gcry_mpi_addm (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
  _gcry_mpi_addm (w, u, v, m);
}

void
gcry_mpi_sub (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
  _gcry_mpi_sub (w, u, v);
}

void
gcry_mpi_sub_ui (gcry_mpi_t w, gcry_mpi_t u, unsigned long v )
{
  _gcry_mpi_sub_ui (w, u, v);
}

void
gcry_mpi_subm (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
  _gcry_mpi_subm (w, u, v, m);
}

void
gcry_mpi_mul (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v)
{
  _gcry_mpi_mul (w, u, v);
}

void
gcry_mpi_mul_ui (gcry_mpi_t w, gcry_mpi_t u, unsigned long v )
{
  _gcry_mpi_mul_ui (w, u, v);
}

void
gcry_mpi_mulm (gcry_mpi_t w, gcry_mpi_t u, gcry_mpi_t v, gcry_mpi_t m)
{
  _gcry_mpi_mulm (w, u, v, m);
}

void
gcry_mpi_mul_2exp (gcry_mpi_t w, gcry_mpi_t u, unsigned long cnt)
{
  _gcry_mpi_mul_2exp (w, u, cnt);
}

void
gcry_mpi_div (gcry_mpi_t q, gcry_mpi_t r,
              gcry_mpi_t dividend, gcry_mpi_t divisor, int round)
{
  _gcry_mpi_div (q, r, dividend, divisor, round);
}

void
gcry_mpi_mod (gcry_mpi_t r, gcry_mpi_t dividend, gcry_mpi_t divisor)
{
  _gcry_mpi_mod (r, dividend, divisor);
}

void
gcry_mpi_powm (gcry_mpi_t w, const gcry_mpi_t b, const gcry_mpi_t e,
               const gcry_mpi_t m)
{
  _gcry_mpi_powm (w, b, e, m);
}

int
gcry_mpi_gcd (gcry_mpi_t g, gcry_mpi_t a, gcry_mpi_t b)
{
  return _gcry_mpi_gcd (g, a, b);
}

int
gcry_mpi_invm (gcry_mpi_t x, gcry_mpi_t a, gcry_mpi_t m)
{
  return _gcry_mpi_invm (x, a, m);
}


unsigned int
gcry_mpi_get_nbits (gcry_mpi_t a)
{
  return _gcry_mpi_get_nbits (a);
}

int
gcry_mpi_test_bit (gcry_mpi_t a, unsigned int n)
{
  return _gcry_mpi_test_bit (a, n);
}

void
gcry_mpi_set_bit (gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_set_bit (a, n);
}

void
gcry_mpi_clear_bit (gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_clear_bit (a, n);
}

void
gcry_mpi_set_highbit (gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_set_highbit (a, n);
}

void
gcry_mpi_clear_highbit (gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_clear_highbit (a, n);
}

void
gcry_mpi_rshift (gcry_mpi_t x, gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_rshift (x, a, n);
}

void
gcry_mpi_lshift (gcry_mpi_t x, gcry_mpi_t a, unsigned int n)
{
  _gcry_mpi_lshift (x, a, n);
}

gcry_mpi_t
gcry_mpi_set_opaque (gcry_mpi_t a, void *p, unsigned int nbits)
{
  return _gcry_mpi_set_opaque (a, p, nbits);
}

void *
gcry_mpi_get_opaque (gcry_mpi_t a, unsigned int *nbits)
{
  return _gcry_mpi_get_opaque (a, nbits);
}

void
gcry_mpi_set_flag (gcry_mpi_t a, enum gcry_mpi_flag flag)
{
  _gcry_mpi_set_flag (a, flag);
}

void
gcry_mpi_clear_flag (gcry_mpi_t a, enum gcry_mpi_flag flag)
{
  _gcry_mpi_clear_flag (a, flag);
}

int
gcry_mpi_get_flag (gcry_mpi_t a, enum gcry_mpi_flag flag)
{
  return _gcry_mpi_get_flag (a, flag);
}

gcry_error_t
gcry_cipher_open (gcry_cipher_hd_t *handle,
                  int algo, int mode, unsigned int flags)
{
  if (!fips_is_operational ())
    {
      *handle = NULL;
      return gpg_error (fips_not_operational ());
    }

  return _gcry_cipher_open (handle, algo, mode, flags);
}

void
gcry_cipher_close (gcry_cipher_hd_t h)
{
  _gcry_cipher_close (h);
}

gcry_error_t
gcry_cipher_setkey (gcry_cipher_hd_t hd, const void *key, size_t keylen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_setkey (hd, key, keylen);
}

gcry_error_t
gcry_cipher_setiv (gcry_cipher_hd_t hd, const void *iv, size_t ivlen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_setiv (hd, iv, ivlen);
}

gpg_error_t
gcry_cipher_setctr (gcry_cipher_hd_t hd, const void *ctr, size_t ctrlen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_setctr (hd, ctr, ctrlen);
}


gcry_error_t
gcry_cipher_ctl (gcry_cipher_hd_t h, int cmd, void *buffer, size_t buflen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_ctl (h, cmd, buffer, buflen);
}

gcry_error_t
gcry_cipher_info (gcry_cipher_hd_t h, int what, void *buffer, size_t *nbytes)
{
  return _gcry_cipher_info (h, what, buffer, nbytes);
}

gcry_error_t
gcry_cipher_algo_info (int algo, int what, void *buffer, size_t *nbytes)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_algo_info (algo, what, buffer, nbytes);
}

const char *
gcry_cipher_algo_name (int algorithm)
{
  return _gcry_cipher_algo_name (algorithm);
}

int
gcry_cipher_map_name (const char *name)
{
  return _gcry_cipher_map_name (name);
}

int
gcry_cipher_mode_from_oid (const char *string)
{
  return _gcry_cipher_mode_from_oid (string);
}

gcry_error_t
gcry_cipher_encrypt (gcry_cipher_hd_t h,
                     void *out, size_t outsize,
                     const void *in, size_t inlen)
{
  if (!fips_is_operational ())
    {
      /* Make sure that the plaintext will never make it to OUT. */
      if (out)
        memset (out, 0x42, outsize); 
      return gpg_error (fips_not_operational ());
    }

  return _gcry_cipher_encrypt (h, out, outsize, in, inlen);
}

gcry_error_t
gcry_cipher_decrypt (gcry_cipher_hd_t h,
                     void *out, size_t outsize,
                     const void *in, size_t inlen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_cipher_decrypt (h, out, outsize, in, inlen);
}

size_t
gcry_cipher_get_algo_keylen (int algo)
{
  return _gcry_cipher_get_algo_keylen (algo);
}

size_t
gcry_cipher_get_algo_blklen (int algo)
{
  return _gcry_cipher_get_algo_blklen (algo);
}

gcry_error_t
gcry_cipher_list (int *list, int *list_length)
{
  return _gcry_cipher_list (list, list_length);
}

gcry_error_t
gcry_pk_encrypt (gcry_sexp_t *result, gcry_sexp_t data, gcry_sexp_t pkey)
{
  if (!fips_is_operational ())
    {
      *result = NULL;
      return gpg_error (fips_not_operational ());
    }
  return _gcry_pk_encrypt (result, data, pkey);
}

gcry_error_t
gcry_pk_decrypt (gcry_sexp_t *result, gcry_sexp_t data, gcry_sexp_t skey)
{
  if (!fips_is_operational ())
    {
      *result = NULL;
      return gpg_error (fips_not_operational ());
    }
  return _gcry_pk_decrypt (result, data, skey);
}

gcry_error_t
gcry_pk_sign (gcry_sexp_t *result, gcry_sexp_t data, gcry_sexp_t skey)
{
  if (!fips_is_operational ())
    {
      *result = NULL;
      return gpg_error (fips_not_operational ());
    }
  return _gcry_pk_sign (result, data, skey);
}

gcry_error_t
gcry_pk_verify (gcry_sexp_t sigval, gcry_sexp_t data, gcry_sexp_t pkey)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_pk_verify (sigval, data, pkey);
}

gcry_error_t
gcry_pk_testkey (gcry_sexp_t key)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_pk_testkey (key);
}

gcry_error_t
gcry_pk_genkey (gcry_sexp_t *r_key, gcry_sexp_t s_parms)
{
  if (!fips_is_operational ())
    {
      *r_key = NULL;
      return gpg_error (fips_not_operational ());
    }
  return _gcry_pk_genkey (r_key, s_parms);
}

gcry_error_t
gcry_pk_ctl (int cmd, void *buffer, size_t buflen)
{
  return _gcry_pk_ctl (cmd, buffer, buflen);
}

gcry_error_t
gcry_pk_algo_info (int algo, int what, void *buffer, size_t *nbytes)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_pk_algo_info (algo, what, buffer, nbytes);
}

const char *
gcry_pk_algo_name (int algorithm)
{
  return _gcry_pk_algo_name (algorithm);
}

int
gcry_pk_map_name (const char *name)
{
  return _gcry_pk_map_name (name);
}

unsigned int
gcry_pk_get_nbits (gcry_sexp_t key)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      return 0;
    }

  return _gcry_pk_get_nbits (key);
}

unsigned char *
gcry_pk_get_keygrip (gcry_sexp_t key, unsigned char *array)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      return NULL;
    }
  return _gcry_pk_get_keygrip (key, array);
}

gcry_error_t
gcry_pk_list (int *list, int *list_length)
{
  return _gcry_pk_list (list, list_length);
}

gcry_error_t
gcry_md_open (gcry_md_hd_t *h, int algo, unsigned int flags)
{
  if (!fips_is_operational ())
    {
      *h = NULL;
      return gpg_error (fips_not_operational ());
    }

  return _gcry_md_open (h, algo, flags);
}

void 
gcry_md_close (gcry_md_hd_t hd)
{
  _gcry_md_close (hd);
}

gcry_error_t
gcry_md_enable (gcry_md_hd_t hd, int algo)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_md_enable (hd, algo);
}

gcry_error_t
gcry_md_copy (gcry_md_hd_t *bhd, gcry_md_hd_t ahd)
{
  if (!fips_is_operational ())
    {
      *bhd = NULL;
      return gpg_error (fips_not_operational ());
    }
  return _gcry_md_copy (bhd, ahd);
}

void
gcry_md_reset (gcry_md_hd_t hd)
{
  _gcry_md_reset (hd);
}

gcry_error_t
gcry_md_ctl (gcry_md_hd_t hd, int cmd, void *buffer, size_t buflen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_md_ctl (hd, cmd, buffer, buflen);
}

void
gcry_md_write (gcry_md_hd_t hd, const void *buffer, size_t length)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      return;
    }
  _gcry_md_write (hd, buffer, length);
}

unsigned char *
gcry_md_read (gcry_md_hd_t hd, int algo)
{
  return _gcry_md_read (hd, algo);
}

void 
gcry_md_hash_buffer (int algo, void *digest,
                     const void *buffer, size_t length)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_error ("called in non-operational state");
    }
  _gcry_md_hash_buffer (algo, digest, buffer, length);
}

int
gcry_md_get_algo (gcry_md_hd_t hd)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_error ("used in non-operational state");
      return 0;
    }
  return _gcry_md_get_algo (hd);
}

unsigned int
gcry_md_get_algo_dlen (int algo)
{
  return _gcry_md_get_algo_dlen (algo);
}

int
gcry_md_is_enabled (gcry_md_hd_t a, int algo)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      return 0;
    }

  return _gcry_md_is_enabled (a, algo);
}

int
gcry_md_is_secure (gcry_md_hd_t a)
{
  return _gcry_md_is_secure (a);
}

gcry_error_t
gcry_md_info (gcry_md_hd_t h, int what, void *buffer, size_t *nbytes)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());

  return _gcry_md_info (h, what, buffer, nbytes);
}

gcry_error_t
gcry_md_algo_info (int algo, int what, void *buffer, size_t *nbytes)
{
  return _gcry_md_algo_info (algo, what, buffer, nbytes);
}

const char *
gcry_md_algo_name (int algo)
{
  return _gcry_md_algo_name (algo);
}

int
gcry_md_map_name (const char* name)
{
  return _gcry_md_map_name (name);
}

gcry_error_t
gcry_md_setkey (gcry_md_hd_t hd, const void *key, size_t keylen)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_md_setkey (hd, key, keylen);
}

void
gcry_md_debug (gcry_md_hd_t hd, const char *suffix)
{
  _gcry_md_debug (hd, suffix);
}

gcry_error_t
gcry_md_list (int *list, int *list_length)
{
  return _gcry_md_list (list, list_length);
}

gcry_error_t
gcry_ac_data_new (gcry_ac_data_t *data)
{
  return _gcry_ac_data_new (data);
}

void
gcry_ac_data_destroy (gcry_ac_data_t data)
{
  _gcry_ac_data_destroy (data);
}

gcry_error_t
gcry_ac_data_copy (gcry_ac_data_t *data_cp, gcry_ac_data_t data)
{
  return _gcry_ac_data_copy (data_cp, data);
}

unsigned int
gcry_ac_data_length (gcry_ac_data_t data)
{
  return _gcry_ac_data_length (data);
}

void
gcry_ac_data_clear (gcry_ac_data_t data)
{
  _gcry_ac_data_clear (data);
}

gcry_error_t
gcry_ac_data_set (gcry_ac_data_t data, unsigned int flags,
                  const char *name, gcry_mpi_t mpi)
{
  return _gcry_ac_data_set (data, flags, name, mpi);
}

gcry_error_t
gcry_ac_data_get_name (gcry_ac_data_t data, unsigned int flags,
                       const char *name, gcry_mpi_t *mpi)
{
  return _gcry_ac_data_get_name (data, flags, name, mpi);
}

gcry_error_t
gcry_ac_data_get_index (gcry_ac_data_t data, unsigned int flags,
                        unsigned int idx, const char **name, gcry_mpi_t *mpi)
{
  return _gcry_ac_data_get_index (data, flags, idx, name, mpi);
}

gcry_error_t
gcry_ac_data_to_sexp (gcry_ac_data_t data, gcry_sexp_t *sexp,
                      const char **identifiers)
{
  return _gcry_ac_data_to_sexp (data, sexp, identifiers);
}

gcry_error_t
gcry_ac_data_from_sexp (gcry_ac_data_t *data, gcry_sexp_t sexp,
                        const char **identifiers)
{
  return _gcry_ac_data_from_sexp (data, sexp, identifiers);
}

void
gcry_ac_io_init (gcry_ac_io_t *ac_io, gcry_ac_io_mode_t mode,
                 gcry_ac_io_type_t type, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, type);
  _gcry_ac_io_init_va (ac_io, mode, type, arg_ptr);
  va_end (arg_ptr);
}

void
gcry_ac_io_init_va (gcry_ac_io_t *ac_io, gcry_ac_io_mode_t mode,
                    gcry_ac_io_type_t type, va_list ap)
{
  _gcry_ac_io_init_va (ac_io, mode, type, ap);
}

gcry_error_t
gcry_ac_open (gcry_ac_handle_t *handle,
              gcry_ac_id_t algorithm, unsigned int flags)
{
  return _gcry_ac_open (handle, algorithm, flags);
}

void
gcry_ac_close (gcry_ac_handle_t handle)
{
  _gcry_ac_close (handle);
}

gcry_error_t
gcry_ac_key_init (gcry_ac_key_t *key, gcry_ac_handle_t handle,
                  gcry_ac_key_type_t type, gcry_ac_data_t data)
{
  return _gcry_ac_key_init (key, handle, type, data);
}

gcry_error_t
gcry_ac_key_pair_generate (gcry_ac_handle_t handle,
                           unsigned int nbits, void *spec,
                           gcry_ac_key_pair_t *key_pair,
                           gcry_mpi_t **miscdata)
{
  return _gcry_ac_key_pair_generate ( handle, nbits, spec, key_pair, miscdata);
}

gcry_ac_key_t
gcry_ac_key_pair_extract (gcry_ac_key_pair_t keypair, gcry_ac_key_type_t which)
{
  return _gcry_ac_key_pair_extract (keypair, which);
}

gcry_ac_data_t
gcry_ac_key_data_get (gcry_ac_key_t key)
{
  return _gcry_ac_key_data_get (key);
}

gcry_error_t
gcry_ac_key_test (gcry_ac_handle_t handle, gcry_ac_key_t key)
{
  return _gcry_ac_key_test (handle, key);
}

gcry_error_t
gcry_ac_key_get_nbits (gcry_ac_handle_t handle,
                       gcry_ac_key_t key, unsigned int *nbits)
{
  return _gcry_ac_key_get_nbits (handle, key, nbits);
}

gcry_error_t
gcry_ac_key_get_grip (gcry_ac_handle_t handle, gcry_ac_key_t key,
                      unsigned char *key_grip)
{
  return _gcry_ac_key_get_grip (handle, key, key_grip);
}

void
gcry_ac_key_destroy (gcry_ac_key_t key)
{
  _gcry_ac_key_destroy (key);
}

void
gcry_ac_key_pair_destroy (gcry_ac_key_pair_t key_pair)
{
  _gcry_ac_key_pair_destroy (key_pair);
}

gcry_error_t
gcry_ac_data_encode (gcry_ac_em_t method, unsigned int flags, void *options,
                     gcry_ac_io_t *io_read, gcry_ac_io_t *io_write)
{
  return _gcry_ac_data_encode (method, flags, options, io_read, io_write);
}

gcry_error_t
gcry_ac_data_decode (gcry_ac_em_t method, unsigned int flags, void *options,
                     gcry_ac_io_t *io_read, gcry_ac_io_t *io_write)
{
  return _gcry_ac_data_decode (method, flags, options, io_read,  io_write);
}

gcry_error_t
gcry_ac_data_encrypt (gcry_ac_handle_t handle,
                      unsigned int flags,
                      gcry_ac_key_t key,
                      gcry_mpi_t data_plain,
                      gcry_ac_data_t *data_encrypted)
{
  return _gcry_ac_data_encrypt (handle, flags, key, 
                                data_plain, data_encrypted);
}

gcry_error_t
gcry_ac_data_decrypt (gcry_ac_handle_t handle,
                      unsigned int flags,
                      gcry_ac_key_t key,
                      gcry_mpi_t *data_plain,
                      gcry_ac_data_t data_encrypted)
{
  return _gcry_ac_data_decrypt (handle, flags, key,
                                data_plain, data_encrypted);
}

gcry_error_t
gcry_ac_data_sign (gcry_ac_handle_t handle,
                   gcry_ac_key_t key,
                   gcry_mpi_t data,
                   gcry_ac_data_t *data_signature)
{
  return _gcry_ac_data_sign (handle, key, data, data_signature);
}

gcry_error_t
gcry_ac_data_verify (gcry_ac_handle_t handle,
                     gcry_ac_key_t key,
                     gcry_mpi_t data,
                     gcry_ac_data_t data_signature)
{
  return _gcry_ac_data_verify (handle, key, data, data_signature);
}

gcry_error_t
gcry_ac_data_encrypt_scheme (gcry_ac_handle_t handle,
                             gcry_ac_scheme_t scheme,
                             unsigned int flags, void *opts,
                             gcry_ac_key_t key,
                             gcry_ac_io_t *io_message,
                             gcry_ac_io_t *io_cipher)
{
  return _gcry_ac_data_encrypt_scheme (handle, scheme, flags, opts, key,
                                       io_message, io_cipher);
}

gcry_error_t
gcry_ac_data_decrypt_scheme (gcry_ac_handle_t handle,
                             gcry_ac_scheme_t scheme,
                             unsigned int flags, void *opts,
                             gcry_ac_key_t key,
                             gcry_ac_io_t *io_cipher,
                             gcry_ac_io_t *io_message)
{
  return _gcry_ac_data_decrypt_scheme (handle, scheme, flags, opts, key,
                                       io_cipher, io_message);
}

gcry_error_t
gcry_ac_data_sign_scheme (gcry_ac_handle_t handle,
                          gcry_ac_scheme_t scheme,
                          unsigned int flags, void *opts,
                          gcry_ac_key_t key,
                          gcry_ac_io_t *io_message,
                          gcry_ac_io_t *io_signature)
{
  return _gcry_ac_data_sign_scheme (handle, scheme, flags, opts, key,
                                    io_message, io_signature);
}

gcry_error_t
gcry_ac_data_verify_scheme (gcry_ac_handle_t handle,
                            gcry_ac_scheme_t scheme,
                            unsigned int flags, void *opts,
                            gcry_ac_key_t key,
                            gcry_ac_io_t *io_message,
                            gcry_ac_io_t *io_signature)
{
  return _gcry_ac_data_verify_scheme (handle, scheme, flags, opts, key,
                                      io_message, io_signature);
}

gcry_error_t
gcry_ac_id_to_name (gcry_ac_id_t algorithm, const char **name) 
{
  /* This function is deprecated.  We implement it in terms of the
     suggested replacement.  */
  const char *tmp = _gcry_pk_algo_name (algorithm);
  if (!*tmp)
    return gcry_error (GPG_ERR_PUBKEY_ALGO);
  *name = tmp;
  return 0;
}

gcry_error_t
gcry_ac_name_to_id (const char *name, gcry_ac_id_t *algorithm)
{
  /* This function is deprecated.  We implement it in terms of the
     suggested replacement.  */
  int algo = _gcry_pk_map_name (name);
  if (!algo)
    return gcry_error (GPG_ERR_PUBKEY_ALGO);
  *algorithm = algo;
  return 0;
}

void
gcry_randomize (void *buffer, size_t length, enum gcry_random_level level)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_fatal_error ("called in non-operational state");
      fips_noreturn (); 
    }
  _gcry_randomize (buffer, length, level);
}

gcry_error_t
gcry_random_add_bytes (const void *buffer, size_t length, int quality)
{
  if (!fips_is_operational ())
    return gpg_error (fips_not_operational ());
  return _gcry_random_add_bytes (buffer, length, quality);
}

void *
gcry_random_bytes (size_t nbytes, enum gcry_random_level level)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_fatal_error ("called in non-operational state");
      fips_noreturn (); 
    }

  return _gcry_random_bytes (nbytes,level);
}

void *
gcry_random_bytes_secure (size_t nbytes, enum gcry_random_level level)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_fatal_error ("called in non-operational state");
      fips_noreturn (); 
    }

  return _gcry_random_bytes_secure (nbytes, level);
}

void
gcry_mpi_randomize (gcry_mpi_t w,
                    unsigned int nbits, enum gcry_random_level level)
{
  _gcry_mpi_randomize (w, nbits, level);
}

void
gcry_create_nonce (void *buffer, size_t length)
{
  if (!fips_is_operational ())
    {
      (void)fips_not_operational ();
      fips_signal_fatal_error ("called in non-operational state");
      fips_noreturn (); 
    }
  _gcry_create_nonce (buffer, length);
}

gcry_error_t
gcry_prime_generate (gcry_mpi_t *prime,
                     unsigned int prime_bits,
                     unsigned int factor_bits,
                     gcry_mpi_t **factors,
                     gcry_prime_check_func_t cb_func,
                     void *cb_arg,
                     gcry_random_level_t random_level,
                     unsigned int flags)
{
  return _gcry_prime_generate (prime, prime_bits, factor_bits, factors,
                               cb_func, cb_arg, random_level, flags);
}

gcry_error_t
gcry_prime_group_generator (gcry_mpi_t *r_g,
                            gcry_mpi_t prime, gcry_mpi_t *factors,
                            gcry_mpi_t start_g)
{
  return _gcry_prime_group_generator (r_g, prime, factors, start_g);
}

void
gcry_prime_release_factors (gcry_mpi_t *factors)
{
  _gcry_prime_release_factors (factors);
}

gcry_error_t
gcry_prime_check (gcry_mpi_t x, unsigned int flags)
{
  return _gcry_prime_check (x, flags);
}

void
gcry_set_progress_handler (gcry_handler_progress_t cb, void *cb_data)
{
  _gcry_set_progress_handler (cb, cb_data);
}

void
gcry_set_allocation_handler (gcry_handler_alloc_t func_alloc,
                             gcry_handler_alloc_t func_alloc_secure,
                             gcry_handler_secure_check_t func_secure_check,
                             gcry_handler_realloc_t func_realloc,
                             gcry_handler_free_t func_free)
{
  _gcry_set_allocation_handler (func_alloc, func_alloc_secure,
                                func_secure_check, func_realloc, func_free);
}

void
gcry_set_outofcore_handler (gcry_handler_no_mem_t h, void *opaque)
{
  _gcry_set_outofcore_handler (h, opaque);
}

void
gcry_set_fatalerror_handler (gcry_handler_error_t fnc, void *opaque)
{
  _gcry_set_fatalerror_handler (fnc, opaque);
}

void
gcry_set_log_handler (gcry_handler_log_t f, void *opaque)
{
  _gcry_set_log_handler (f, opaque);
}

void
gcry_set_gettext_handler (const char *(*f)(const char*))
{
  _gcry_set_gettext_handler (f);
}

void *
gcry_malloc (size_t n)
{
  return _gcry_malloc (n);
}

void *
gcry_calloc (size_t n, size_t m)
{
  return _gcry_calloc (n, m);
}

void *
gcry_malloc_secure (size_t n)
{
  return _gcry_malloc_secure (n);
}

void *
gcry_calloc_secure (size_t n, size_t m)
{
  return _gcry_calloc_secure (n,m);
}

void *
gcry_realloc (void *a, size_t n)
{
  return _gcry_realloc (a, n);
}

char *
gcry_strdup (const char *string)
{
  return _gcry_strdup (string);
}

void *
gcry_xmalloc (size_t n)
{
  return _gcry_xmalloc (n);
}

void *
gcry_xcalloc (size_t n, size_t m)
{
  return _gcry_xcalloc (n, m);
}

void *
gcry_xmalloc_secure (size_t n)
{
  return _gcry_xmalloc_secure (n);
}

void *
gcry_xcalloc_secure (size_t n, size_t m)
{
  return _gcry_xcalloc_secure (n, m);
}

void *
gcry_xrealloc (void *a, size_t n)
{
  return _gcry_xrealloc (a, n);
}

char *
gcry_xstrdup (const char *a)
{
  return _gcry_xstrdup (a);
}

void
gcry_free (void *a)
{
  _gcry_free (a);
}

int
gcry_is_secure (const void *a)
{
  return _gcry_is_secure (a);
}


gcry_error_t
gcry_cipher_register (gcry_cipher_spec_t *cipher, int *algorithm_id,
                      gcry_module_t *module)
{
  return _gcry_cipher_register (cipher, NULL, algorithm_id, module);
}

void
gcry_cipher_unregister (gcry_module_t module)
{
  _gcry_cipher_unregister (module);
}

gcry_error_t
gcry_pk_register (gcry_pk_spec_t *pubkey, unsigned int *algorithm_id,
                  gcry_module_t *module)
{
  return _gcry_pk_register (pubkey, NULL, algorithm_id, module);
}

void
gcry_pk_unregister (gcry_module_t module)
{
  _gcry_pk_unregister (module);
}

gcry_error_t
gcry_md_register (gcry_md_spec_t *digest, unsigned int *algorithm_id,
                  gcry_module_t *module)
{
  return _gcry_md_register (digest, NULL, algorithm_id, module);
}

void
gcry_md_unregister (gcry_module_t module)
{
  _gcry_md_unregister (module);
}
