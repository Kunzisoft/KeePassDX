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
*/

#ifndef _SHA2_H
#define _SHA2_H

#include <stdlib.h>

/* define for bit or byte oriented SHA   */
#if 1
#  define SHA2_BITS 0   /* byte oriented */
#else
#  define SHA2_BITS 1   /* bit oriented  */
#endif

/* define the hash functions that you need  */
/* define for 64-bit SHA384 and SHA512      */
#define SHA_64BIT
#define SHA_2   /* for dynamic hash length  */
#define SHA_224
#define SHA_256
#ifdef SHA_64BIT
#  define SHA_384
#  define SHA_512
#  define NEED_uint64_t
#endif

#define SHA2_MAX_DIGEST_SIZE   64
#define SHA2_MAX_BLOCK_SIZE   128

#include "brg_types.h"

#if defined(__cplusplus)
extern "C"
{
#endif

/* Note that the following function prototypes are the same */
/* for both the bit and byte oriented implementations.  But */
/* the length fields are in bytes or bits as is appropriate */
/* for the version used.  Bit sequences are arrays of bytes */
/* in which bit sequence indexes increase from the most to  */
/* the least significant end of each byte.  The value 'len' */
/* in sha<nnn>_hash for the byte oriented versions of SHA2  */
/* is limited to 2^29 bytes, but multiple calls will handle */
/* longer data blocks.                                      */

#define SHA224_DIGEST_SIZE  28
#define SHA224_BLOCK_SIZE   64

#define SHA256_DIGEST_SIZE  32
#define SHA256_BLOCK_SIZE   64

/* type to hold the SHA256 (and SHA224) context */

typedef struct
{   uint32_t count[2];
    uint32_t hash[SHA256_DIGEST_SIZE >> 2];
    uint32_t wbuf[SHA256_BLOCK_SIZE >> 2];
} sha256_ctx;

typedef sha256_ctx  sha224_ctx;

VOID_RETURN sha256_compile(sha256_ctx ctx[1]);

VOID_RETURN sha224_begin(sha224_ctx ctx[1]);
#define sha224_hash sha256_hash
VOID_RETURN sha224_end(unsigned char hval[], sha224_ctx ctx[1]);
VOID_RETURN sha224(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha256_begin(sha256_ctx ctx[1]);
VOID_RETURN sha256_hash(const unsigned char data[], unsigned long len, sha256_ctx ctx[1]);
VOID_RETURN sha256_end(unsigned char hval[], sha256_ctx ctx[1]);
VOID_RETURN sha256(unsigned char hval[], const unsigned char data[], unsigned long len);

#ifndef SHA_64BIT

typedef struct
{   union
    { sha256_ctx  ctx256[1];
    } uu[1];
    uint32_t    sha2_len;
} sha2_ctx;

#else

#define SHA384_DIGEST_SIZE  48
#define SHA384_BLOCK_SIZE  128

#define SHA512_DIGEST_SIZE  64
#define SHA512_BLOCK_SIZE  128

#define SHA512_128_DIGEST_SIZE 16
#define SHA512_128_BLOCK_SIZE  SHA512_BLOCK_SIZE

#define SHA512_192_DIGEST_SIZE 24
#define SHA512_192_BLOCK_SIZE  SHA512_BLOCK_SIZE

#define SHA512_224_DIGEST_SIZE 28
#define SHA512_224_BLOCK_SIZE  SHA512_BLOCK_SIZE

#define SHA512_256_DIGEST_SIZE 32
#define SHA512_256_BLOCK_SIZE  SHA512_BLOCK_SIZE

/* type to hold the SHA384 (and SHA512) context */

typedef struct
{   uint64_t count[2];
    uint64_t hash[SHA512_DIGEST_SIZE >> 3];
    uint64_t wbuf[SHA512_BLOCK_SIZE >> 3];
} sha512_ctx;

typedef sha512_ctx  sha384_ctx;

typedef struct
{   union
    { sha256_ctx  ctx256[1];
      sha512_ctx  ctx512[1];
    } uu[1];
    uint32_t    sha2_len;
} sha2_ctx;

VOID_RETURN sha512_compile(sha512_ctx ctx[1]);

VOID_RETURN sha384_begin(sha384_ctx ctx[1]);
#define sha384_hash sha512_hash
VOID_RETURN sha384_end(unsigned char hval[], sha384_ctx ctx[1]);
VOID_RETURN sha384(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha512_begin(sha512_ctx ctx[1]);
VOID_RETURN sha512_hash(const unsigned char data[], unsigned long len, sha512_ctx ctx[1]);
VOID_RETURN sha512_end(unsigned char hval[], sha512_ctx ctx[1]);
VOID_RETURN sha512(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha512_256_begin(sha512_ctx ctx[1]);
#define sha512_256_hash sha512_hash
VOID_RETURN sha512_256_end(unsigned char hval[], sha512_ctx ctx[1]);
VOID_RETURN sha512_256(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha512_224_begin(sha512_ctx ctx[1]);
#define sha512_224_hash sha512_hash
VOID_RETURN sha512_224_end(unsigned char hval[], sha512_ctx ctx[1]);
VOID_RETURN sha512_224(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha512_192_begin(sha512_ctx ctx[1]);
#define sha512_192_hash sha512_hash
VOID_RETURN sha512_192_end(unsigned char hval[], sha512_ctx ctx[1]);
VOID_RETURN sha512_192(unsigned char hval[], const unsigned char data[], unsigned long len);

VOID_RETURN sha512_128_begin(sha512_ctx ctx[1]);
#define sha512_128_hash sha512_hash
VOID_RETURN sha512_128_end(unsigned char hval[], sha512_ctx ctx[1]);
VOID_RETURN sha512_128(unsigned char hval[], const unsigned char data[], unsigned long len);

INT_RETURN  sha2_begin(unsigned long size, sha2_ctx ctx[1]);
VOID_RETURN sha2_hash(const unsigned char data[], unsigned long len, sha2_ctx ctx[1]);
VOID_RETURN sha2_end(unsigned char hval[], sha2_ctx ctx[1]);
INT_RETURN  sha2(unsigned char hval[], unsigned long size, const unsigned char data[], unsigned long len);

#endif

#if defined(__cplusplus)
}
#endif

#endif
