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

#include <string.h>     /* for memcpy() etc.        */

#include "sha1.h"
#include "brg_endian.h"

#if defined(__cplusplus)
extern "C"
{
#endif

#if defined( _MSC_VER ) && ( _MSC_VER > 800 )
#pragma intrinsic(memcpy)
#pragma intrinsic(memset)
#endif

#if 0 && defined(_MSC_VER)
#define rotl32  _lrotl
#define rotr32  _lrotr
#else
#define rotl32(x,n)   (((x) << n) | ((x) >> (32 - n)))
#define rotr32(x,n)   (((x) >> n) | ((x) << (32 - n)))
#endif

#if !defined(bswap_32)
#define bswap_32(x) ((rotr32((x), 24) & 0x00ff00ff) | (rotr32((x), 8) & 0xff00ff00))
#endif

#if (PLATFORM_BYTE_ORDER == IS_LITTLE_ENDIAN)
#define SWAP_BYTES
#else
#undef  SWAP_BYTES
#endif

#if defined(SWAP_BYTES)
#define bsw_32(p,n) \
    { int _i = (n); while(_i--) ((uint32_t*)p)[_i] = bswap_32(((uint32_t*)p)[_i]); }
#else
#define bsw_32(p,n)
#endif

#define SHA1_MASK   (SHA1_BLOCK_SIZE - 1)

#if 0

#define ch(x,y,z)       (((x) & (y)) ^ (~(x) & (z)))
#define parity(x,y,z)   ((x) ^ (y) ^ (z))
#define maj(x,y,z)      (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))

#else   /* Discovered by Rich Schroeppel and Colin Plumb   */

#define ch(x,y,z)       ((z) ^ ((x) & ((y) ^ (z))))
#define parity(x,y,z)   ((x) ^ (y) ^ (z))
#define maj(x,y,z)      (((x) & (y)) | ((z) & ((x) ^ (y))))

#endif

/* Compile 64 bytes of hash data into SHA1 context. Note    */
/* that this routine assumes that the byte order in the     */
/* ctx->wbuf[] at this point is in such an order that low   */
/* address bytes in the ORIGINAL byte stream will go in     */
/* this buffer to the high end of 32-bit words on BOTH big  */
/* and little endian systems                                */

#ifdef ARRAY
#define q(v,n)  v[n]
#else
#define q(v,n)  v##n
#endif

#define one_cycle(v,a,b,c,d,e,f,k,h)            \
    q(v,e) += rotr32(q(v,a),27) +               \
              f(q(v,b),q(v,c),q(v,d)) + k + h;  \
    q(v,b)  = rotr32(q(v,b), 2)

#define five_cycle(v,f,k,i)                 \
    one_cycle(v, 0,1,2,3,4, f,k,hf(i  ));   \
    one_cycle(v, 4,0,1,2,3, f,k,hf(i+1));   \
    one_cycle(v, 3,4,0,1,2, f,k,hf(i+2));   \
    one_cycle(v, 2,3,4,0,1, f,k,hf(i+3));   \
    one_cycle(v, 1,2,3,4,0, f,k,hf(i+4))

VOID_RETURN sha1_compile(sha1_ctx ctx[1])
{   uint32_t    *w = ctx->wbuf;

#ifdef ARRAY
    uint32_t    v[5];
    memcpy(v, ctx->hash, sizeof(ctx->hash));
#else
    uint32_t    v0, v1, v2, v3, v4;
    v0 = ctx->hash[0]; v1 = ctx->hash[1];
    v2 = ctx->hash[2]; v3 = ctx->hash[3];
    v4 = ctx->hash[4];
#endif

#define hf(i)   w[i]

    five_cycle(v, ch, 0x5a827999,  0);
    five_cycle(v, ch, 0x5a827999,  5);
    five_cycle(v, ch, 0x5a827999, 10);
    one_cycle(v,0,1,2,3,4, ch, 0x5a827999, hf(15)); \

#undef  hf
#define hf(i) (w[(i) & 15] = rotl32(                    \
                 w[((i) + 13) & 15] ^ w[((i) + 8) & 15] \
               ^ w[((i) +  2) & 15] ^ w[(i) & 15], 1))

    one_cycle(v,4,0,1,2,3, ch, 0x5a827999, hf(16));
    one_cycle(v,3,4,0,1,2, ch, 0x5a827999, hf(17));
    one_cycle(v,2,3,4,0,1, ch, 0x5a827999, hf(18));
    one_cycle(v,1,2,3,4,0, ch, 0x5a827999, hf(19));

    five_cycle(v, parity, 0x6ed9eba1,  20);
    five_cycle(v, parity, 0x6ed9eba1,  25);
    five_cycle(v, parity, 0x6ed9eba1,  30);
    five_cycle(v, parity, 0x6ed9eba1,  35);

    five_cycle(v, maj, 0x8f1bbcdc,  40);
    five_cycle(v, maj, 0x8f1bbcdc,  45);
    five_cycle(v, maj, 0x8f1bbcdc,  50);
    five_cycle(v, maj, 0x8f1bbcdc,  55);

    five_cycle(v, parity, 0xca62c1d6,  60);
    five_cycle(v, parity, 0xca62c1d6,  65);
    five_cycle(v, parity, 0xca62c1d6,  70);
    five_cycle(v, parity, 0xca62c1d6,  75);

#ifdef ARRAY
    ctx->hash[0] += v[0]; ctx->hash[1] += v[1];
    ctx->hash[2] += v[2]; ctx->hash[3] += v[3];
    ctx->hash[4] += v[4];
#else
    ctx->hash[0] += v0; ctx->hash[1] += v1;
    ctx->hash[2] += v2; ctx->hash[3] += v3;
    ctx->hash[4] += v4;
#endif
}

VOID_RETURN sha1_begin(sha1_ctx ctx[1])
{
    memset(ctx, 0, sizeof(sha1_ctx));
    ctx->hash[0] = 0x67452301;
    ctx->hash[1] = 0xefcdab89;
    ctx->hash[2] = 0x98badcfe;
    ctx->hash[3] = 0x10325476;
    ctx->hash[4] = 0xc3d2e1f0;
}

/* SHA1 hash data in an array of bytes into hash buffer and */
/* call the hash_compile function as required. For both the */
/* bit and byte orientated versions, the block length 'len' */
/* must not be greater than 2^32 - 1 bits (2^29 - 1 bytes)  */ 

VOID_RETURN sha1_hash(const unsigned char data[], unsigned long len, sha1_ctx ctx[1])
{   uint32_t pos = (uint32_t)((ctx->count[0] >> 3) & SHA1_MASK);
    const unsigned char *sp = data;
    unsigned char *w = (unsigned char*)ctx->wbuf;
#if SHA1_BITS == 1
    uint32_t ofs = (ctx->count[0] & 7);
#else
    len <<= 3;
#endif
    if((ctx->count[0] += len) < len)
        ++(ctx->count[1]);
#if SHA1_BITS == 1
    if(ofs)                 /* if not on a byte boundary    */
    {
        if(ofs + len < 8)   /* if no added bytes are needed */
        {
            w[pos] |= (*sp >> ofs);
        }
        else                /* otherwise and add bytes      */
        {   unsigned char part = w[pos];

            while((int)(ofs + (len -= 8)) >= 0)
            {
                w[pos++] = part | (*sp >> ofs);
                part = *sp++ << (8 - ofs);
                if(pos == SHA1_BLOCK_SIZE)
                {
                    bsw_32(w, SHA1_BLOCK_SIZE >> 2);
                    sha1_compile(ctx); pos = 0;
                }
            }

            w[pos] = part;
        }
    }
    else    /* data is byte aligned */
#endif
    {   uint32_t space = SHA1_BLOCK_SIZE - pos;

        while(len >= (space << 3))
        {
            memcpy(w + pos, sp, space);
            bsw_32(w, SHA1_BLOCK_SIZE >> 2);
            sha1_compile(ctx); 
            sp += space; len -= (space << 3); 
            space = SHA1_BLOCK_SIZE; pos = 0;
        }
        memcpy(w + pos, sp, (len + 7 * SHA1_BITS) >> 3);
    }
}

/* SHA1 final padding and digest calculation  */

VOID_RETURN sha1_end(unsigned char hval[], sha1_ctx ctx[1])
{   uint32_t    i = (uint32_t)((ctx->count[0] >> 3) & SHA1_MASK), m1;

    /* put bytes in the buffer in an order in which references to   */
    /* 32-bit words will put bytes with lower addresses into the    */
    /* top of 32 bit words on BOTH big and little endian machines   */
    bsw_32(ctx->wbuf, (i + 3 + SHA1_BITS) >> 2);

    /* we now need to mask valid bytes and add the padding which is */
    /* a single 1 bit and as many zero bits as necessary. Note that */
    /* we can always add the first padding byte here because the    */
    /* buffer always has at least one empty slot                    */
    m1 = (unsigned char)0x80 >> (ctx->count[0] & 7);
    ctx->wbuf[i >> 2] &= ((0xffffff00 | (~m1 + 1)) << 8 * (~i & 3));
    ctx->wbuf[i >> 2] |= (m1 << 8 * (~i & 3));

    /* we need 9 or more empty positions, one for the padding byte  */
    /* (above) and eight for the length count. If there is not      */
    /* enough space, pad and empty the buffer                       */
    if(i > SHA1_BLOCK_SIZE - 9)
    {
        if(i < 60) ctx->wbuf[15] = 0;
        sha1_compile(ctx);
        i = 0;
    }
    else    /* compute a word index for the empty buffer positions  */
        i = (i >> 2) + 1;

    while(i < 14) /* and zero pad all but last two positions        */
        ctx->wbuf[i++] = 0;

    /* the following 32-bit length fields are assembled in the      */
    /* wrong byte order on little endian machines but this is       */
    /* corrected later since they are only ever used as 32-bit      */
    /* word values.                                                 */
    ctx->wbuf[14] = ctx->count[1];
    ctx->wbuf[15] = ctx->count[0];
    sha1_compile(ctx);

    /* extract the hash value as bytes in case the hash buffer is   */
    /* misaligned for 32-bit words                                  */
    for(i = 0; i < SHA1_DIGEST_SIZE; ++i)
        hval[i] = ((ctx->hash[i >> 2] >> (8 * (~i & 3))) & 0xff);
}

VOID_RETURN sha1(unsigned char hval[], const unsigned char data[], unsigned long len)
{   sha1_ctx    cx[1];

    sha1_begin(cx); sha1_hash(data, len, cx); sha1_end(hval, cx);
}

#if defined(__cplusplus)
}
#endif
