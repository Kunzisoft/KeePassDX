/*
---------------------------------------------------------------------------
Copyright (c) 1998-2010, Brian Gladman, Worcester, UK. All rights reserved.

The redistribution and use of this software (with or without changes)
is allowed without the payment of fees or royalties provided that:

  source code distributions include the above copyright notice, this
  list of conditions and the following disclaimer;

  binary distributions include the above copyright notice, this list
  of conditions and the following disclaimer in their documentation.

This software is provided 'as is' with no explicit or implied warranties
in respect of its operation, including, but not limited to, correctness
and fitness for purpose.
---------------------------------------------------------------------------
Issue Date: 20/12/2007

This is an implementation of HMAC, the FIPS standard keyed hash function
*/

#ifndef _HMAC2_H
#define _HMAC2_H

#include <stdlib.h>
#include <memory.h>

#if defined(__cplusplus)
extern "C"
{
#endif

#if !defined(_SHA1_H)
#  include "sha1.h"
#endif

#if !defined(_SHA2_H)
#  include "sha2.h"
#endif

#if !defined(_SHA2_H)
#define HMAC_BLOCK_SIZE      SHA1_BLOCK_SIZE
#define HMAC_MAX_OUTPUT_SIZE SHA1_DIGEST_SIZE
#else
#define HMAC_BLOCK_SIZE      SHA2_MAX_BLOCK_SIZE  
#define HMAC_MAX_OUTPUT_SIZE SHA2_MAX_DIGEST_SIZE
#endif

#define HMAC_IN_DATA  0xffffffff

enum hmac_hash  
{ 
#ifdef _SHA1_H
    HMAC_SHA1, 
#endif
#ifdef _SHA2_H
# ifdef SHA_224 
    HMAC_SHA224, 
# endif
# ifdef SHA_256
    HMAC_SHA256, 
# endif
# ifdef SHA_384
    HMAC_SHA384, 
# endif
# ifdef SHA_512
    HMAC_SHA512, 
    HMAC_SHA512_256,
    HMAC_SHA512_224,
    HMAC_SHA512_192,
    HMAC_SHA512_128
# endif
#endif
};

typedef VOID_RETURN hf_begin(void*);
typedef VOID_RETURN hf_hash(const void*, unsigned long len, void*);
typedef VOID_RETURN hf_end(void*, void*);

typedef struct
{   hf_begin        *f_begin;
    hf_hash         *f_hash;
    hf_end          *f_end;
    unsigned char   key[HMAC_BLOCK_SIZE];
    union
    {
#ifdef _SHA1_H
       sha1_ctx    u_sha1;
#endif
#ifdef _SHA2_H
# ifdef SHA_224
        sha224_ctx  u_sha224;
# endif
# ifdef SHA_256
        sha256_ctx  u_sha256;
# endif
# ifdef SHA_384
        sha384_ctx  u_sha384;
# endif
# ifdef SHA_512
        sha512_ctx  u_sha512;
# endif
#endif
    } sha_ctx[1];
    unsigned long   input_len;
    unsigned long   output_len;
    unsigned long   klen;
} hmac_ctx;

/* returns the length of hash digest for the hash used  */
/* mac_len must not be greater than this                */
int hmac_sha_begin(enum hmac_hash hash, hmac_ctx cx[1]);

int  hmac_sha_key(const unsigned char key[], unsigned long key_len, hmac_ctx cx[1]);

void hmac_sha_data(const unsigned char data[], unsigned long data_len, hmac_ctx cx[1]);

void hmac_sha_end(unsigned char mac[], unsigned long mac_len, hmac_ctx cx[1]);

void hmac_sha(enum hmac_hash hash, const unsigned char key[], unsigned long key_len,
          const unsigned char data[], unsigned long data_len,
          unsigned char mac[], unsigned long mac_len);

#if defined(__cplusplus)
}
#endif

#endif
