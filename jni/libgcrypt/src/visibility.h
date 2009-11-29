/* visibility.h - Set visibility attribute
 * Copyright (C) 2007  Free Software Foundation, Inc.
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

#ifndef GCRY_VISIBILITY_H
#define GCRY_VISIBILITY_H

/* Redefine all public symbols with an underscore unless we already
   use the underscore prefixed version internally.  */
#define gcry_check_version          _gcry_check_version
#define gcry_control                _gcry_control

#define gcry_set_allocation_handler _gcry_set_allocation_handler
#define gcry_set_fatalerror_handler _gcry_set_fatalerror_handler
#define gcry_set_gettext_handler    _gcry_set_gettext_handler
#define gcry_set_log_handler        _gcry_set_log_handler
#define gcry_set_outofcore_handler  _gcry_set_outofcore_handler
#define gcry_set_progress_handler   _gcry_set_progress_handler
#define gcry_err_code_from_errno    _gcry_err_code_from_errno
#define gcry_err_code_to_errno      _gcry_err_code_to_errno
#define gcry_err_make_from_errno    _gcry_err_make_from_errno
#define gcry_error_from_errno       _gcry_error_from_errno
#define gcry_strerror               _gcry_strerror
#define gcry_strsource              _gcry_strsource

#define gcry_free                   _gcry_free
#define gcry_malloc                 _gcry_malloc
#define gcry_malloc_secure          _gcry_malloc_secure
#define gcry_calloc                 _gcry_calloc
#define gcry_calloc_secure          _gcry_calloc_secure
#define gcry_realloc                _gcry_realloc
#define gcry_strdup                 _gcry_strdup
#define gcry_is_secure              _gcry_is_secure
#define gcry_xcalloc                _gcry_xcalloc
#define gcry_xcalloc_secure         _gcry_xcalloc_secure
#define gcry_xmalloc                _gcry_xmalloc
#define gcry_xmalloc_secure         _gcry_xmalloc_secure
#define gcry_xrealloc               _gcry_xrealloc
#define gcry_xstrdup                _gcry_xstrdup
                                    
#define gcry_md_algo_info           _gcry_md_algo_info
#define gcry_md_algo_name           _gcry_md_algo_name
#define gcry_md_close               _gcry_md_close
#define gcry_md_copy                _gcry_md_copy
#define gcry_md_ctl                 _gcry_md_ctl
#define gcry_md_enable              _gcry_md_enable
#define gcry_md_get                 _gcry_md_get
#define gcry_md_get_algo            _gcry_md_get_algo
#define gcry_md_get_algo_dlen       _gcry_md_get_algo_dlen
#define gcry_md_hash_buffer         _gcry_md_hash_buffer
#define gcry_md_info                _gcry_md_info
#define gcry_md_is_enabled          _gcry_md_is_enabled
#define gcry_md_is_secure           _gcry_md_is_secure
#define gcry_md_list                _gcry_md_list
#define gcry_md_map_name            _gcry_md_map_name
#define gcry_md_open                _gcry_md_open
#define gcry_md_read                _gcry_md_read
/* gcry_md_register and _gcry_md_register differ.  */
#define gcry_md_unregister          _gcry_md_unregister
#define gcry_md_reset               _gcry_md_reset
#define gcry_md_setkey              _gcry_md_setkey
#define gcry_md_write               _gcry_md_write
#define gcry_md_debug               _gcry_md_debug

#define gcry_cipher_algo_info       _gcry_cipher_algo_info
#define gcry_cipher_algo_name       _gcry_cipher_algo_name
#define gcry_cipher_close           _gcry_cipher_close
#define gcry_cipher_setkey          _gcry_cipher_setkey
#define gcry_cipher_setiv           _gcry_cipher_setiv
#define gcry_cipher_setctr          _gcry_cipher_setctr
#define gcry_cipher_ctl             _gcry_cipher_ctl
#define gcry_cipher_decrypt         _gcry_cipher_decrypt
#define gcry_cipher_encrypt         _gcry_cipher_encrypt
#define gcry_cipher_get_algo_blklen _gcry_cipher_get_algo_blklen
#define gcry_cipher_get_algo_keylen _gcry_cipher_get_algo_keylen
#define gcry_cipher_info            _gcry_cipher_info
#define gcry_cipher_list            _gcry_cipher_list
#define gcry_cipher_map_name        _gcry_cipher_map_name
#define gcry_cipher_mode_from_oid   _gcry_cipher_mode_from_oid
#define gcry_cipher_open            _gcry_cipher_open
/* gcry_cipher_register and  _gcry_cipher_register differ.  */
#define gcry_cipher_unregister      _gcry_cipher_unregister

#define gcry_pk_algo_info           _gcry_pk_algo_info
#define gcry_pk_algo_name           _gcry_pk_algo_name
#define gcry_pk_ctl                 _gcry_pk_ctl
#define gcry_pk_decrypt             _gcry_pk_decrypt
#define gcry_pk_encrypt             _gcry_pk_encrypt
#define gcry_pk_genkey              _gcry_pk_genkey
#define gcry_pk_get_keygrip         _gcry_pk_get_keygrip
#define gcry_pk_get_nbits           _gcry_pk_get_nbits
#define gcry_pk_list                _gcry_pk_list
#define gcry_pk_map_name            _gcry_pk_map_name
/* gcry_pk_register and _gcry_pk_register differ.  */
#define gcry_pk_unregister          _gcry_pk_unregister
#define gcry_pk_sign                _gcry_pk_sign
#define gcry_pk_testkey             _gcry_pk_testkey
#define gcry_pk_verify              _gcry_pk_verify

#define gcry_ac_data_new            _gcry_ac_data_new
#define gcry_ac_data_destroy        _gcry_ac_data_destroy
#define gcry_ac_data_copy           _gcry_ac_data_copy
#define gcry_ac_data_length         _gcry_ac_data_length
#define gcry_ac_data_clear          _gcry_ac_data_clear
#define gcry_ac_data_set            _gcry_ac_data_set
#define gcry_ac_data_get_name       _gcry_ac_data_get_name
#define gcry_ac_data_get_index      _gcry_ac_data_get_index
#define gcry_ac_open                _gcry_ac_open
#define gcry_ac_close               _gcry_ac_close
#define gcry_ac_key_init            _gcry_ac_key_init
#define gcry_ac_key_pair_generate   _gcry_ac_key_pair_generate
#define gcry_ac_key_pair_extract    _gcry_ac_key_pair_extract
#define gcry_ac_key_data_get        _gcry_ac_key_data_get
#define gcry_ac_key_test            _gcry_ac_key_test
#define gcry_ac_key_get_nbits       _gcry_ac_key_get_nbits
#define gcry_ac_key_get_grip        _gcry_ac_key_get_grip
#define gcry_ac_key_destroy         _gcry_ac_key_destroy
#define gcry_ac_key_pair_destroy    _gcry_ac_key_pair_destroy
#define gcry_ac_data_encrypt        _gcry_ac_data_encrypt
#define gcry_ac_data_decrypt        _gcry_ac_data_decrypt
#define gcry_ac_data_sign           _gcry_ac_data_sign
#define gcry_ac_data_verify         _gcry_ac_data_verify
#define gcry_ac_id_to_name          _gcry_ac_id_to_name
#define gcry_ac_name_to_id          _gcry_ac_name_to_id
#define gcry_ac_data_encode         _gcry_ac_data_encode
#define gcry_ac_data_decode         _gcry_ac_data_decode
#define gcry_ac_mpi_to_os           _gcry_ac_mpi_to_os
#define gcry_ac_mpi_to_os_alloc     _gcry_ac_mpi_to_os_alloc
#define gcry_ac_os_to_mpi           _gcry_ac_os_to_mpi
#define gcry_ac_data_encrypt_scheme _gcry_ac_data_encrypt_scheme
#define gcry_ac_data_decrypt_scheme _gcry_ac_data_decrypt_scheme
#define gcry_ac_data_sign_scheme    _gcry_ac_data_sign_scheme
#define gcry_ac_data_verify_scheme  _gcry_ac_data_verify_scheme
#define gcry_ac_data_to_sexp        _gcry_ac_data_to_sexp
#define gcry_ac_data_from_sexp      _gcry_ac_data_from_sexp
#define gcry_ac_io_init             _gcry_ac_io_init
#define gcry_ac_io_init_va          _gcry_ac_io_init_va

#define gcry_prime_check            _gcry_prime_check
#define gcry_prime_generate         _gcry_prime_generate
#define gcry_prime_group_generator  _gcry_prime_group_generator
#define gcry_prime_release_factors  _gcry_prime_release_factors

#define gcry_random_add_bytes       _gcry_random_add_bytes
#define gcry_random_bytes           _gcry_random_bytes
#define gcry_random_bytes_secure    _gcry_random_bytes_secure
#define gcry_randomize              _gcry_randomize
#define gcry_create_nonce           _gcry_create_nonce

#define gcry_sexp_alist             _gcry_sexp_alist
#define gcry_sexp_append            _gcry_sexp_append
#define gcry_sexp_build             _gcry_sexp_build
#define gcry_sexp_build_array       _gcry_sexp_build_array
#define gcry_sexp_cadr              _gcry_sexp_cadr
#define gcry_sexp_canon_len         _gcry_sexp_canon_len
#define gcry_sexp_car               _gcry_sexp_car
#define gcry_sexp_cdr               _gcry_sexp_cdr
#define gcry_sexp_cons              _gcry_sexp_cons
#define gcry_sexp_create            _gcry_sexp_create
#define gcry_sexp_dump              _gcry_sexp_dump
#define gcry_sexp_find_token        _gcry_sexp_find_token
#define gcry_sexp_length            _gcry_sexp_length
#define gcry_sexp_new               _gcry_sexp_new
#define gcry_sexp_nth               _gcry_sexp_nth
#define gcry_sexp_nth_data          _gcry_sexp_nth_data
#define gcry_sexp_nth_mpi           _gcry_sexp_nth_mpi
#define gcry_sexp_prepend           _gcry_sexp_prepend
#define gcry_sexp_release           _gcry_sexp_release
#define gcry_sexp_sprint            _gcry_sexp_sprint
#define gcry_sexp_sscan             _gcry_sexp_sscan
#define gcry_sexp_vlist             _gcry_sexp_vlist
#define gcry_sexp_nth_string        _gcry_sexp_nth_string

#define gcry_mpi_add                _gcry_mpi_add
#define gcry_mpi_add_ui             _gcry_mpi_add_ui
#define gcry_mpi_addm               _gcry_mpi_addm
#define gcry_mpi_aprint             _gcry_mpi_aprint
#define gcry_mpi_clear_bit          _gcry_mpi_clear_bit
#define gcry_mpi_clear_flag         _gcry_mpi_clear_flag
#define gcry_mpi_clear_highbit      _gcry_mpi_clear_highbit
#define gcry_mpi_cmp                _gcry_mpi_cmp
#define gcry_mpi_cmp_ui             _gcry_mpi_cmp_ui
#define gcry_mpi_copy               _gcry_mpi_copy
#define gcry_mpi_div                _gcry_mpi_div
#define gcry_mpi_dump               _gcry_mpi_dump
#define gcry_mpi_gcd                _gcry_mpi_gcd
#define gcry_mpi_get_flag           _gcry_mpi_get_flag
#define gcry_mpi_get_nbits          _gcry_mpi_get_nbits
#define gcry_mpi_get_opaque         _gcry_mpi_get_opaque
#define gcry_mpi_invm               _gcry_mpi_invm
#define gcry_mpi_mod                _gcry_mpi_mod
#define gcry_mpi_mul                _gcry_mpi_mul
#define gcry_mpi_mul_2exp           _gcry_mpi_mul_2exp
#define gcry_mpi_mul_ui             _gcry_mpi_mul_ui
#define gcry_mpi_mulm               _gcry_mpi_mulm
#define gcry_mpi_new                _gcry_mpi_new
#define gcry_mpi_powm               _gcry_mpi_powm
#define gcry_mpi_print              _gcry_mpi_print
#define gcry_mpi_randomize          _gcry_mpi_randomize
#define gcry_mpi_release            _gcry_mpi_release
#define gcry_mpi_rshift             _gcry_mpi_rshift
#define gcry_mpi_lshift             _gcry_mpi_lshift
#define gcry_mpi_scan               _gcry_mpi_scan
#define gcry_mpi_set                _gcry_mpi_set
#define gcry_mpi_set_bit            _gcry_mpi_set_bit
#define gcry_mpi_set_flag           _gcry_mpi_set_flag
#define gcry_mpi_set_highbit        _gcry_mpi_set_highbit
#define gcry_mpi_set_opaque         _gcry_mpi_set_opaque
#define gcry_mpi_set_ui             _gcry_mpi_set_ui
#define gcry_mpi_snew               _gcry_mpi_snew
#define gcry_mpi_sub                _gcry_mpi_sub
#define gcry_mpi_sub_ui             _gcry_mpi_sub_ui
#define gcry_mpi_subm               _gcry_mpi_subm
#define gcry_mpi_swap               _gcry_mpi_swap
#define gcry_mpi_test_bit           _gcry_mpi_test_bit


/* Include the main header here so that public symbols are mapped to
   the internal underscored ones.  */
#ifdef _GCRY_INCLUDED_BY_VISIBILITY_C
  /* We need to redeclare the deprecated functions without the
     deprecated attribute.  */
# define GCRYPT_NO_DEPRECATED
# include "gcrypt.h"
  gcry_error_t gcry_ac_id_to_name (gcry_ac_id_t algorithm, const char **name);
  gcry_error_t gcry_ac_name_to_id (const char *name, gcry_ac_id_t *algorithm);
#else
# include "gcrypt.h"
#endif

/* Prototypes of functions exported but not ready for use.  */
gcry_err_code_t gcry_md_get (gcry_md_hd_t hd, int algo, 
                             unsigned char *buffer, int buflen);
void gcry_ac_mpi_to_os (gcry_mpi_t mpi, unsigned char *os, size_t os_n);
gcry_error_t gcry_ac_mpi_to_os_alloc (gcry_mpi_t mpi, unsigned char **os,
                                       size_t *os_n);
void gcry_ac_os_to_mpi (gcry_mpi_t mpi, unsigned char *os, size_t os_n);



/* Our use of the ELF visibility feature works by passing
   -fvisibiliy=hidden on the command line and by explicitly marking
   all exported functions as visible.  

   NOTE: When adding new functions, please make sure to add them to
         libgcrypt.vers and libgcrypt.def as well.  */

#ifdef _GCRY_INCLUDED_BY_VISIBILITY_C

/* A macro to flag a function as visible.  Note that we take the
   definition from the mapped name.  */
#ifdef GCRY_USE_VISIBILITY
# define MARK_VISIBLE(name) \
    extern __typeof__ (_##name) name __attribute__ ((visibility("default")));
# define MARK_VISIBLEX(name) \
    extern __typeof__ (name) name __attribute__ ((visibility("default")));
#else
# define MARK_VISIBLE(name) /* */
# define MARK_VISIBLEX(name) /* */
#endif


/* First undef all redefined symbols so that we set the attribute on
   the correct version name.  */
#undef gcry_check_version         
#undef gcry_control               

#undef gcry_set_allocation_handler
#undef gcry_set_fatalerror_handler
#undef gcry_set_gettext_handler   
#undef gcry_set_log_handler       
#undef gcry_set_outofcore_handler 
#undef gcry_set_progress_handler  
#undef gcry_err_code_from_errno   
#undef gcry_err_code_to_errno     
#undef gcry_err_make_from_errno   
#undef gcry_error_from_errno      
#undef gcry_strerror              
#undef gcry_strsource             

#undef gcry_free                  
#undef gcry_malloc                
#undef gcry_malloc_secure         
#undef gcry_calloc                
#undef gcry_calloc_secure         
#undef gcry_realloc               
#undef gcry_strdup                
#undef gcry_is_secure             
#undef gcry_xcalloc               
#undef gcry_xcalloc_secure        
#undef gcry_xmalloc               
#undef gcry_xmalloc_secure        
#undef gcry_xrealloc              
#undef gcry_xstrdup               
                                   
#undef gcry_md_algo_info          
#undef gcry_md_algo_name          
#undef gcry_md_close              
#undef gcry_md_copy               
#undef gcry_md_ctl                
#undef gcry_md_enable             
#undef gcry_md_get                
#undef gcry_md_get_algo           
#undef gcry_md_get_algo_dlen      
#undef gcry_md_hash_buffer        
#undef gcry_md_info               
#undef gcry_md_is_enabled         
#undef gcry_md_is_secure          
#undef gcry_md_list               
#undef gcry_md_map_name           
#undef gcry_md_open               
#undef gcry_md_read               
/* gcry_md_register is not anymore a macro.  */
#undef gcry_md_unregister         
#undef gcry_md_reset              
#undef gcry_md_setkey             
#undef gcry_md_write              
#undef gcry_md_debug              

#undef gcry_cipher_algo_info      
#undef gcry_cipher_algo_name      
#undef gcry_cipher_close          
#undef gcry_cipher_setkey
#undef gcry_cipher_setiv
#undef gcry_cipher_setctr
#undef gcry_cipher_ctl            
#undef gcry_cipher_decrypt        
#undef gcry_cipher_encrypt        
#undef gcry_cipher_get_algo_blklen
#undef gcry_cipher_get_algo_keylen
#undef gcry_cipher_info           
#undef gcry_cipher_list           
#undef gcry_cipher_map_name       
#undef gcry_cipher_mode_from_oid  
#undef gcry_cipher_open           
/* gcry_cipher_register is not anymore a macro.  */
#undef gcry_cipher_unregister     

#undef gcry_pk_algo_info          
#undef gcry_pk_algo_name          
#undef gcry_pk_ctl                
#undef gcry_pk_decrypt            
#undef gcry_pk_encrypt            
#undef gcry_pk_genkey             
#undef gcry_pk_get_keygrip        
#undef gcry_pk_get_nbits          
#undef gcry_pk_list               
#undef gcry_pk_map_name           
/* gcry_pk_register is not anymore a macro.  */
#undef gcry_pk_unregister         
#undef gcry_pk_sign               
#undef gcry_pk_testkey            
#undef gcry_pk_verify             

#undef gcry_ac_data_new           
#undef gcry_ac_data_destroy       
#undef gcry_ac_data_copy          
#undef gcry_ac_data_length        
#undef gcry_ac_data_clear         
#undef gcry_ac_data_set           
#undef gcry_ac_data_get_name      
#undef gcry_ac_data_get_index     
#undef gcry_ac_open               
#undef gcry_ac_close              
#undef gcry_ac_key_init           
#undef gcry_ac_key_pair_generate  
#undef gcry_ac_key_pair_extract   
#undef gcry_ac_key_data_get       
#undef gcry_ac_key_test           
#undef gcry_ac_key_get_nbits      
#undef gcry_ac_key_get_grip       
#undef gcry_ac_key_destroy        
#undef gcry_ac_key_pair_destroy   
#undef gcry_ac_data_encrypt       
#undef gcry_ac_data_decrypt       
#undef gcry_ac_data_sign          
#undef gcry_ac_data_verify        
#undef gcry_ac_id_to_name         
#undef gcry_ac_name_to_id         
#undef gcry_ac_data_encode        
#undef gcry_ac_data_decode        
#undef gcry_ac_mpi_to_os          
#undef gcry_ac_mpi_to_os_alloc    
#undef gcry_ac_os_to_mpi          
#undef gcry_ac_data_encrypt_scheme
#undef gcry_ac_data_decrypt_scheme
#undef gcry_ac_data_sign_scheme   
#undef gcry_ac_data_verify_scheme 
#undef gcry_ac_data_to_sexp       
#undef gcry_ac_data_from_sexp     
#undef gcry_ac_io_init            
#undef gcry_ac_io_init_va         

#undef gcry_prime_check           
#undef gcry_prime_generate        
#undef gcry_prime_group_generator 
#undef gcry_prime_release_factors 

#undef gcry_random_add_bytes      
#undef gcry_random_bytes          
#undef gcry_random_bytes_secure   
#undef gcry_randomize             
#undef gcry_create_nonce          

#undef gcry_sexp_alist            
#undef gcry_sexp_append           
#undef gcry_sexp_build            
#undef gcry_sexp_build_array      
#undef gcry_sexp_cadr             
#undef gcry_sexp_canon_len        
#undef gcry_sexp_car              
#undef gcry_sexp_cdr              
#undef gcry_sexp_cons             
#undef gcry_sexp_create           
#undef gcry_sexp_dump             
#undef gcry_sexp_find_token       
#undef gcry_sexp_length           
#undef gcry_sexp_new              
#undef gcry_sexp_nth              
#undef gcry_sexp_nth_data         
#undef gcry_sexp_nth_mpi          
#undef gcry_sexp_prepend          
#undef gcry_sexp_release          
#undef gcry_sexp_sprint           
#undef gcry_sexp_sscan            
#undef gcry_sexp_vlist            
#undef gcry_sexp_nth_string       

#undef gcry_mpi_add               
#undef gcry_mpi_add_ui            
#undef gcry_mpi_addm              
#undef gcry_mpi_aprint            
#undef gcry_mpi_clear_bit         
#undef gcry_mpi_clear_flag        
#undef gcry_mpi_clear_highbit     
#undef gcry_mpi_cmp               
#undef gcry_mpi_cmp_ui            
#undef gcry_mpi_copy              
#undef gcry_mpi_div               
#undef gcry_mpi_dump              
#undef gcry_mpi_gcd               
#undef gcry_mpi_get_flag          
#undef gcry_mpi_get_nbits         
#undef gcry_mpi_get_opaque        
#undef gcry_mpi_invm              
#undef gcry_mpi_mod               
#undef gcry_mpi_mul               
#undef gcry_mpi_mul_2exp          
#undef gcry_mpi_mul_ui            
#undef gcry_mpi_mulm              
#undef gcry_mpi_new               
#undef gcry_mpi_powm              
#undef gcry_mpi_print             
#undef gcry_mpi_randomize         
#undef gcry_mpi_release           
#undef gcry_mpi_rshift            
#undef gcry_mpi_lshift            
#undef gcry_mpi_scan              
#undef gcry_mpi_set               
#undef gcry_mpi_set_bit           
#undef gcry_mpi_set_flag          
#undef gcry_mpi_set_highbit       
#undef gcry_mpi_set_opaque        
#undef gcry_mpi_set_ui            
#undef gcry_mpi_snew              
#undef gcry_mpi_sub               
#undef gcry_mpi_sub_ui            
#undef gcry_mpi_subm              
#undef gcry_mpi_swap              
#undef gcry_mpi_test_bit          


/* Now mark all symbols.  */

MARK_VISIBLE (gcry_check_version)
MARK_VISIBLE (gcry_control)

MARK_VISIBLE (gcry_set_allocation_handler)
MARK_VISIBLE (gcry_set_fatalerror_handler)
MARK_VISIBLE (gcry_set_gettext_handler)
MARK_VISIBLE (gcry_set_log_handler)
MARK_VISIBLE (gcry_set_outofcore_handler)
MARK_VISIBLE (gcry_set_progress_handler)
MARK_VISIBLE (gcry_err_code_from_errno)
MARK_VISIBLE (gcry_err_code_to_errno)
MARK_VISIBLE (gcry_err_make_from_errno)
MARK_VISIBLE (gcry_error_from_errno)
MARK_VISIBLE (gcry_strerror)
MARK_VISIBLE (gcry_strsource)

MARK_VISIBLE (gcry_free)
MARK_VISIBLE (gcry_malloc)
MARK_VISIBLE (gcry_malloc_secure)
MARK_VISIBLE (gcry_calloc)
MARK_VISIBLE (gcry_calloc_secure)
MARK_VISIBLE (gcry_realloc)
MARK_VISIBLE (gcry_strdup)
MARK_VISIBLE (gcry_is_secure)
MARK_VISIBLE (gcry_xcalloc)
MARK_VISIBLE (gcry_xcalloc_secure)
MARK_VISIBLE (gcry_xmalloc)
MARK_VISIBLE (gcry_xmalloc_secure)
MARK_VISIBLE (gcry_xrealloc)
MARK_VISIBLE (gcry_xstrdup)

MARK_VISIBLE (gcry_md_algo_info)
MARK_VISIBLE (gcry_md_algo_name)
MARK_VISIBLE (gcry_md_close)
MARK_VISIBLE (gcry_md_copy)
MARK_VISIBLE (gcry_md_ctl)
MARK_VISIBLE (gcry_md_enable)
MARK_VISIBLE (gcry_md_get)
MARK_VISIBLE (gcry_md_get_algo)
MARK_VISIBLE (gcry_md_get_algo_dlen)
MARK_VISIBLE (gcry_md_hash_buffer)
MARK_VISIBLE (gcry_md_info)
MARK_VISIBLE (gcry_md_is_enabled)
MARK_VISIBLE (gcry_md_is_secure)
MARK_VISIBLE (gcry_md_list)
MARK_VISIBLE (gcry_md_map_name)
MARK_VISIBLE (gcry_md_open)
MARK_VISIBLE (gcry_md_read)
MARK_VISIBLEX(gcry_md_register)
MARK_VISIBLE (gcry_md_reset)
MARK_VISIBLE (gcry_md_setkey)
MARK_VISIBLE (gcry_md_unregister)
MARK_VISIBLE (gcry_md_write)
MARK_VISIBLE (gcry_md_debug)

MARK_VISIBLE (gcry_cipher_algo_info)
MARK_VISIBLE (gcry_cipher_algo_name)
MARK_VISIBLE (gcry_cipher_close)
MARK_VISIBLE (gcry_cipher_setkey)
MARK_VISIBLE (gcry_cipher_setiv)
MARK_VISIBLE (gcry_cipher_setctr)
MARK_VISIBLE (gcry_cipher_ctl)
MARK_VISIBLE (gcry_cipher_decrypt)
MARK_VISIBLE (gcry_cipher_encrypt)
MARK_VISIBLE (gcry_cipher_get_algo_blklen)
MARK_VISIBLE (gcry_cipher_get_algo_keylen)
MARK_VISIBLE (gcry_cipher_info)
MARK_VISIBLE (gcry_cipher_list)
MARK_VISIBLE (gcry_cipher_map_name)
MARK_VISIBLE (gcry_cipher_mode_from_oid)
MARK_VISIBLE (gcry_cipher_open)
MARK_VISIBLEX(gcry_cipher_register)
MARK_VISIBLE (gcry_cipher_unregister)

MARK_VISIBLE (gcry_pk_algo_info)
MARK_VISIBLE (gcry_pk_algo_name)
MARK_VISIBLE (gcry_pk_ctl)
MARK_VISIBLE (gcry_pk_decrypt)
MARK_VISIBLE (gcry_pk_encrypt)
MARK_VISIBLE (gcry_pk_genkey)
MARK_VISIBLE (gcry_pk_get_keygrip)
MARK_VISIBLE (gcry_pk_get_nbits)
MARK_VISIBLE (gcry_pk_list)
MARK_VISIBLE (gcry_pk_map_name)
MARK_VISIBLEX(gcry_pk_register)
MARK_VISIBLE (gcry_pk_sign)
MARK_VISIBLE (gcry_pk_testkey)
MARK_VISIBLE (gcry_pk_unregister)
MARK_VISIBLE (gcry_pk_verify)

MARK_VISIBLE (gcry_ac_data_new)
MARK_VISIBLE (gcry_ac_data_destroy)
MARK_VISIBLE (gcry_ac_data_copy)
MARK_VISIBLE (gcry_ac_data_length)
MARK_VISIBLE (gcry_ac_data_clear)
MARK_VISIBLE (gcry_ac_data_set)
MARK_VISIBLE (gcry_ac_data_get_name)
MARK_VISIBLE (gcry_ac_data_get_index)
MARK_VISIBLE (gcry_ac_open)
MARK_VISIBLE (gcry_ac_close)
MARK_VISIBLE (gcry_ac_key_init)
MARK_VISIBLE (gcry_ac_key_pair_generate)
MARK_VISIBLE (gcry_ac_key_pair_extract)
MARK_VISIBLE (gcry_ac_key_data_get)
MARK_VISIBLE (gcry_ac_key_test)
MARK_VISIBLE (gcry_ac_key_get_nbits)
MARK_VISIBLE (gcry_ac_key_get_grip)
MARK_VISIBLE (gcry_ac_key_destroy)
MARK_VISIBLE (gcry_ac_key_pair_destroy)
MARK_VISIBLE (gcry_ac_data_encrypt)
MARK_VISIBLE (gcry_ac_data_decrypt)
MARK_VISIBLE (gcry_ac_data_sign)
MARK_VISIBLE (gcry_ac_data_verify)
MARK_VISIBLE (gcry_ac_id_to_name)
MARK_VISIBLE (gcry_ac_name_to_id)
/* MARK_VISIBLE (gcry_ac_list) Not defined although it is in
        libgcrypt.vers. */
MARK_VISIBLE (gcry_ac_data_encode)
MARK_VISIBLE (gcry_ac_data_decode)
MARK_VISIBLE (gcry_ac_mpi_to_os)
MARK_VISIBLE (gcry_ac_mpi_to_os_alloc)
MARK_VISIBLE (gcry_ac_os_to_mpi)
MARK_VISIBLE (gcry_ac_data_encrypt_scheme)
MARK_VISIBLE (gcry_ac_data_decrypt_scheme)
MARK_VISIBLE (gcry_ac_data_sign_scheme)
MARK_VISIBLE (gcry_ac_data_verify_scheme)
MARK_VISIBLE (gcry_ac_data_to_sexp)
MARK_VISIBLE (gcry_ac_data_from_sexp)
MARK_VISIBLE (gcry_ac_io_init)
MARK_VISIBLE (gcry_ac_io_init_va)

MARK_VISIBLE (gcry_prime_check)
MARK_VISIBLE (gcry_prime_generate)
MARK_VISIBLE (gcry_prime_group_generator)
MARK_VISIBLE (gcry_prime_release_factors)

MARK_VISIBLE (gcry_random_add_bytes)
MARK_VISIBLE (gcry_random_bytes)
MARK_VISIBLE (gcry_random_bytes_secure)
MARK_VISIBLE (gcry_randomize)
MARK_VISIBLE (gcry_create_nonce)

MARK_VISIBLE (gcry_sexp_alist)
MARK_VISIBLE (gcry_sexp_append)
MARK_VISIBLE (gcry_sexp_build)
MARK_VISIBLE (gcry_sexp_build_array)
MARK_VISIBLE (gcry_sexp_cadr)
MARK_VISIBLE (gcry_sexp_canon_len)
MARK_VISIBLE (gcry_sexp_car)
MARK_VISIBLE (gcry_sexp_cdr)
MARK_VISIBLE (gcry_sexp_cons)
MARK_VISIBLE (gcry_sexp_create)
MARK_VISIBLE (gcry_sexp_dump)
MARK_VISIBLE (gcry_sexp_find_token)
MARK_VISIBLE (gcry_sexp_length)
MARK_VISIBLE (gcry_sexp_new)
MARK_VISIBLE (gcry_sexp_nth)
MARK_VISIBLE (gcry_sexp_nth_data)
MARK_VISIBLE (gcry_sexp_nth_mpi)
MARK_VISIBLE (gcry_sexp_prepend)
MARK_VISIBLE (gcry_sexp_release)
MARK_VISIBLE (gcry_sexp_sprint)
MARK_VISIBLE (gcry_sexp_sscan)
MARK_VISIBLE (gcry_sexp_vlist)
MARK_VISIBLE (gcry_sexp_nth_string)

MARK_VISIBLE (gcry_mpi_add)
MARK_VISIBLE (gcry_mpi_add_ui)
MARK_VISIBLE (gcry_mpi_addm)
MARK_VISIBLE (gcry_mpi_aprint)
MARK_VISIBLE (gcry_mpi_clear_bit)
MARK_VISIBLE (gcry_mpi_clear_flag)
MARK_VISIBLE (gcry_mpi_clear_highbit)
MARK_VISIBLE (gcry_mpi_cmp)
MARK_VISIBLE (gcry_mpi_cmp_ui)
MARK_VISIBLE (gcry_mpi_copy)
MARK_VISIBLE (gcry_mpi_div)
MARK_VISIBLE (gcry_mpi_dump)
MARK_VISIBLE (gcry_mpi_gcd)
MARK_VISIBLE (gcry_mpi_get_flag)
MARK_VISIBLE (gcry_mpi_get_nbits)
MARK_VISIBLE (gcry_mpi_get_opaque)
MARK_VISIBLE (gcry_mpi_invm)
MARK_VISIBLE (gcry_mpi_mod)
MARK_VISIBLE (gcry_mpi_mul)
MARK_VISIBLE (gcry_mpi_mul_2exp)
MARK_VISIBLE (gcry_mpi_mul_ui)
MARK_VISIBLE (gcry_mpi_mulm)
MARK_VISIBLE (gcry_mpi_new)
MARK_VISIBLE (gcry_mpi_powm)
MARK_VISIBLE (gcry_mpi_print)
MARK_VISIBLE (gcry_mpi_randomize)
MARK_VISIBLE (gcry_mpi_release)
MARK_VISIBLE (gcry_mpi_rshift)
MARK_VISIBLE (gcry_mpi_lshift)
MARK_VISIBLE (gcry_mpi_scan)
MARK_VISIBLE (gcry_mpi_set)
MARK_VISIBLE (gcry_mpi_set_bit)
MARK_VISIBLE (gcry_mpi_set_flag)
MARK_VISIBLE (gcry_mpi_set_highbit)
MARK_VISIBLE (gcry_mpi_set_opaque)
MARK_VISIBLE (gcry_mpi_set_ui)
MARK_VISIBLE (gcry_mpi_snew)
MARK_VISIBLE (gcry_mpi_sub)
MARK_VISIBLE (gcry_mpi_sub_ui)
MARK_VISIBLE (gcry_mpi_subm)
MARK_VISIBLE (gcry_mpi_swap)
MARK_VISIBLE (gcry_mpi_test_bit)



#undef MARK_VISIBLE
#endif /*_GCRY_INCLUDED_BY_VISIBILITY_C*/

#endif /*GCRY_VISIBILITY_H*/
