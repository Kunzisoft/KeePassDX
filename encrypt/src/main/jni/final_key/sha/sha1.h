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

#ifndef _SHA1_H
#define _SHA1_H

/* define for bit or byte oriented SHA   */
#if 1
#  define SHA1_BITS 0   /* byte oriented */
#else
#  define SHA1_BITS 1   /* bit oriented  */
#endif

#define SHA_1

#include <stdlib.h>
#include "brg_types.h"

#define SHA1_BLOCK_SIZE  64
#define SHA1_DIGEST_SIZE 20

#if defined(__cplusplus)
extern "C"
{
#endif

/* type to hold the SHA256 context  */

typedef struct
{   uint32_t count[2];
    uint32_t hash[SHA1_DIGEST_SIZE >> 2];
    uint32_t wbuf[SHA1_BLOCK_SIZE >> 2];
} sha1_ctx;

/* Note that these prototypes are the same for both bit and */
/* byte oriented implementations. However the length fields */
/* are in bytes or bits as appropriate for the version used */
/* and bit sequences are input as arrays of bytes in which  */
/* bit sequences run from the most to the least significant */
/* end of each byte. The value 'len' in sha1_hash for the   */
/* byte oriented version of SHA1 is limited to 2^29 bytes,  */
/* but multiple calls will handle longer data blocks.       */

VOID_RETURN sha1_compile(sha1_ctx ctx[1]);

VOID_RETURN sha1_begin(sha1_ctx ctx[1]);
VOID_RETURN sha1_hash(const unsigned char data[], unsigned long len, sha1_ctx ctx[1]);
VOID_RETURN sha1_end(unsigned char hval[], sha1_ctx ctx[1]);
VOID_RETURN sha1(unsigned char hval[], const unsigned char data[], unsigned long len);

#if defined(__cplusplus)
}
#endif

#endif
