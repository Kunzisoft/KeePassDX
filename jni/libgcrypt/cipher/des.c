/* des.c - DES and Triple-DES encryption/decryption Algorithm
 * Copyright (C) 1998, 1999, 2001, 2002, 2003,
 *               2008  Free Software Foundation, Inc.
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
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * For a description of triple encryption, see:
 *   Bruce Schneier: Applied Cryptography. Second Edition.
 *   John Wiley & Sons, 1996. ISBN 0-471-12845-7. Pages 358 ff.
 * This implementation is according to the definition of DES in FIPS
 * PUB 46-2 from December 1993. 
 */


/*
 * Written by Michael Roth <mroth@nessie.de>, September 1998
 */


/*
 *  U S A G E
 * ===========
 *
 * For DES or Triple-DES encryption/decryption you must initialize a proper
 * encryption context with a key.
 *
 * A DES key is 64bit wide but only 56bits of the key are used. The remaining
 * bits are parity bits and they will _not_ checked in this implementation, but
 * simply ignored.
 *
 * For Triple-DES you could use either two 64bit keys or three 64bit keys.
 * The parity bits will _not_ checked, too.
 *
 * After initializing a context with a key you could use this context to
 * encrypt or decrypt data in 64bit blocks in Electronic Codebook Mode.
 *
 * (In the examples below the slashes at the beginning and ending of comments
 * are omited.)
 *
 * DES Example
 * -----------
 *     unsigned char key[8];
 *     unsigned char plaintext[8];
 *     unsigned char ciphertext[8];
 *     unsigned char recoverd[8];
 *     des_ctx context;
 *
 *     * Fill 'key' and 'plaintext' with some data *
 *     ....
 *
 *     * Set up the DES encryption context *
 *     des_setkey(context, key);
 *
 *     * Encrypt the plaintext *
 *     des_ecb_encrypt(context, plaintext, ciphertext);
 *
 *     * To recover the orginal plaintext from ciphertext use: *
 *     des_ecb_decrypt(context, ciphertext, recoverd);
 *
 *
 * Triple-DES Example
 * ------------------
 *     unsigned char key1[8];
 *     unsigned char key2[8];
 *     unsigned char key3[8];
 *     unsigned char plaintext[8];
 *     unsigned char ciphertext[8];
 *     unsigned char recoverd[8];
 *     tripledes_ctx context;
 *
 *     * If you would like to use two 64bit keys, fill 'key1' and'key2'
 *	 then setup the encryption context: *
 *     tripledes_set2keys(context, key1, key2);
 *
 *     * To use three 64bit keys with Triple-DES use: *
 *     tripledes_set3keys(context, key1, key2, key3);
 *
 *     * Encrypting plaintext with Triple-DES *
 *     tripledes_ecb_encrypt(context, plaintext, ciphertext);
 *
 *     * Decrypting ciphertext to recover the plaintext with Triple-DES *
 *     tripledes_ecb_decrypt(context, ciphertext, recoverd);
 *
 *
 * Selftest
 * --------
 *     char *error_msg;
 *
 *     * To perform a selftest of this DES/Triple-DES implementation use the
 *	 function selftest(). It will return an error string if there are
 *	 some problems with this library. *
 *
 *     if ( (error_msg = selftest()) )
 *     {
 *	   fprintf(stderr, "An error in the DES/Tripple-DES implementation occured: %s\n", error_msg);
 *	   abort();
 *     }
 */


#include <config.h>
#include <stdio.h>
#include <string.h>	       /* memcpy, memcmp */
#include "types.h"             /* for byte and u32 typedefs */
#include "g10lib.h"
#include "cipher.h"

#if defined(__GNUC__) && defined(__GNU_LIBRARY__)
#define working_memcmp memcmp
#else
/*
 * According to the SunOS man page, memcmp returns indeterminate sign
 * depending on whether characters are signed or not.
 */
static int
working_memcmp( const char *a, const char *b, size_t n )
{
    for( ; n; n--, a++, b++ )
	if( *a != *b )
	    return (int)(*(byte*)a) - (int)(*(byte*)b);
    return 0;
}
#endif

/*
 * Encryption/Decryption context of DES
 */
typedef struct _des_ctx
  {
    u32 encrypt_subkeys[32];
    u32 decrypt_subkeys[32];
  }
des_ctx[1];

/*
 * Encryption/Decryption context of Triple-DES
 */
typedef struct _tripledes_ctx
  {
    u32 encrypt_subkeys[96];
    u32 decrypt_subkeys[96];
    struct {
      int no_weak_key;
    } flags;
  }
tripledes_ctx[1];

static void des_key_schedule (const byte *, u32 *);
static int des_setkey (struct _des_ctx *, const byte *);
static int des_ecb_crypt (struct _des_ctx *, const byte *, byte *, int);
static int tripledes_set2keys (struct _tripledes_ctx *,
                               const byte *, const byte *);
static int tripledes_set3keys (struct _tripledes_ctx *,
                               const byte *, const byte *, const byte *);
static int tripledes_ecb_crypt (struct _tripledes_ctx *,
                                const byte *, byte *, int);
static int is_weak_key ( const byte *key );
static const char *selftest (void);

static int initialized;




/*
 * The s-box values are permuted according to the 'primitive function P'
 * and are rotated one bit to the left.
 */
static u32 sbox1[64] =
{
  0x01010400, 0x00000000, 0x00010000, 0x01010404, 0x01010004, 0x00010404, 0x00000004, 0x00010000,
  0x00000400, 0x01010400, 0x01010404, 0x00000400, 0x01000404, 0x01010004, 0x01000000, 0x00000004,
  0x00000404, 0x01000400, 0x01000400, 0x00010400, 0x00010400, 0x01010000, 0x01010000, 0x01000404,
  0x00010004, 0x01000004, 0x01000004, 0x00010004, 0x00000000, 0x00000404, 0x00010404, 0x01000000,
  0x00010000, 0x01010404, 0x00000004, 0x01010000, 0x01010400, 0x01000000, 0x01000000, 0x00000400,
  0x01010004, 0x00010000, 0x00010400, 0x01000004, 0x00000400, 0x00000004, 0x01000404, 0x00010404,
  0x01010404, 0x00010004, 0x01010000, 0x01000404, 0x01000004, 0x00000404, 0x00010404, 0x01010400,
  0x00000404, 0x01000400, 0x01000400, 0x00000000, 0x00010004, 0x00010400, 0x00000000, 0x01010004
};

static u32 sbox2[64] =
{
  0x80108020, 0x80008000, 0x00008000, 0x00108020, 0x00100000, 0x00000020, 0x80100020, 0x80008020,
  0x80000020, 0x80108020, 0x80108000, 0x80000000, 0x80008000, 0x00100000, 0x00000020, 0x80100020,
  0x00108000, 0x00100020, 0x80008020, 0x00000000, 0x80000000, 0x00008000, 0x00108020, 0x80100000,
  0x00100020, 0x80000020, 0x00000000, 0x00108000, 0x00008020, 0x80108000, 0x80100000, 0x00008020,
  0x00000000, 0x00108020, 0x80100020, 0x00100000, 0x80008020, 0x80100000, 0x80108000, 0x00008000,
  0x80100000, 0x80008000, 0x00000020, 0x80108020, 0x00108020, 0x00000020, 0x00008000, 0x80000000,
  0x00008020, 0x80108000, 0x00100000, 0x80000020, 0x00100020, 0x80008020, 0x80000020, 0x00100020,
  0x00108000, 0x00000000, 0x80008000, 0x00008020, 0x80000000, 0x80100020, 0x80108020, 0x00108000
};

static u32 sbox3[64] =
{
  0x00000208, 0x08020200, 0x00000000, 0x08020008, 0x08000200, 0x00000000, 0x00020208, 0x08000200,
  0x00020008, 0x08000008, 0x08000008, 0x00020000, 0x08020208, 0x00020008, 0x08020000, 0x00000208,
  0x08000000, 0x00000008, 0x08020200, 0x00000200, 0x00020200, 0x08020000, 0x08020008, 0x00020208,
  0x08000208, 0x00020200, 0x00020000, 0x08000208, 0x00000008, 0x08020208, 0x00000200, 0x08000000,
  0x08020200, 0x08000000, 0x00020008, 0x00000208, 0x00020000, 0x08020200, 0x08000200, 0x00000000,
  0x00000200, 0x00020008, 0x08020208, 0x08000200, 0x08000008, 0x00000200, 0x00000000, 0x08020008,
  0x08000208, 0x00020000, 0x08000000, 0x08020208, 0x00000008, 0x00020208, 0x00020200, 0x08000008,
  0x08020000, 0x08000208, 0x00000208, 0x08020000, 0x00020208, 0x00000008, 0x08020008, 0x00020200
};

static u32 sbox4[64] =
{
  0x00802001, 0x00002081, 0x00002081, 0x00000080, 0x00802080, 0x00800081, 0x00800001, 0x00002001,
  0x00000000, 0x00802000, 0x00802000, 0x00802081, 0x00000081, 0x00000000, 0x00800080, 0x00800001,
  0x00000001, 0x00002000, 0x00800000, 0x00802001, 0x00000080, 0x00800000, 0x00002001, 0x00002080,
  0x00800081, 0x00000001, 0x00002080, 0x00800080, 0x00002000, 0x00802080, 0x00802081, 0x00000081,
  0x00800080, 0x00800001, 0x00802000, 0x00802081, 0x00000081, 0x00000000, 0x00000000, 0x00802000,
  0x00002080, 0x00800080, 0x00800081, 0x00000001, 0x00802001, 0x00002081, 0x00002081, 0x00000080,
  0x00802081, 0x00000081, 0x00000001, 0x00002000, 0x00800001, 0x00002001, 0x00802080, 0x00800081,
  0x00002001, 0x00002080, 0x00800000, 0x00802001, 0x00000080, 0x00800000, 0x00002000, 0x00802080
};

static u32 sbox5[64] =
{
  0x00000100, 0x02080100, 0x02080000, 0x42000100, 0x00080000, 0x00000100, 0x40000000, 0x02080000,
  0x40080100, 0x00080000, 0x02000100, 0x40080100, 0x42000100, 0x42080000, 0x00080100, 0x40000000,
  0x02000000, 0x40080000, 0x40080000, 0x00000000, 0x40000100, 0x42080100, 0x42080100, 0x02000100,
  0x42080000, 0x40000100, 0x00000000, 0x42000000, 0x02080100, 0x02000000, 0x42000000, 0x00080100,
  0x00080000, 0x42000100, 0x00000100, 0x02000000, 0x40000000, 0x02080000, 0x42000100, 0x40080100,
  0x02000100, 0x40000000, 0x42080000, 0x02080100, 0x40080100, 0x00000100, 0x02000000, 0x42080000,
  0x42080100, 0x00080100, 0x42000000, 0x42080100, 0x02080000, 0x00000000, 0x40080000, 0x42000000,
  0x00080100, 0x02000100, 0x40000100, 0x00080000, 0x00000000, 0x40080000, 0x02080100, 0x40000100
};

static u32 sbox6[64] =
{
  0x20000010, 0x20400000, 0x00004000, 0x20404010, 0x20400000, 0x00000010, 0x20404010, 0x00400000,
  0x20004000, 0x00404010, 0x00400000, 0x20000010, 0x00400010, 0x20004000, 0x20000000, 0x00004010,
  0x00000000, 0x00400010, 0x20004010, 0x00004000, 0x00404000, 0x20004010, 0x00000010, 0x20400010,
  0x20400010, 0x00000000, 0x00404010, 0x20404000, 0x00004010, 0x00404000, 0x20404000, 0x20000000,
  0x20004000, 0x00000010, 0x20400010, 0x00404000, 0x20404010, 0x00400000, 0x00004010, 0x20000010,
  0x00400000, 0x20004000, 0x20000000, 0x00004010, 0x20000010, 0x20404010, 0x00404000, 0x20400000,
  0x00404010, 0x20404000, 0x00000000, 0x20400010, 0x00000010, 0x00004000, 0x20400000, 0x00404010,
  0x00004000, 0x00400010, 0x20004010, 0x00000000, 0x20404000, 0x20000000, 0x00400010, 0x20004010
};

static u32 sbox7[64] =
{
  0x00200000, 0x04200002, 0x04000802, 0x00000000, 0x00000800, 0x04000802, 0x00200802, 0x04200800,
  0x04200802, 0x00200000, 0x00000000, 0x04000002, 0x00000002, 0x04000000, 0x04200002, 0x00000802,
  0x04000800, 0x00200802, 0x00200002, 0x04000800, 0x04000002, 0x04200000, 0x04200800, 0x00200002,
  0x04200000, 0x00000800, 0x00000802, 0x04200802, 0x00200800, 0x00000002, 0x04000000, 0x00200800,
  0x04000000, 0x00200800, 0x00200000, 0x04000802, 0x04000802, 0x04200002, 0x04200002, 0x00000002,
  0x00200002, 0x04000000, 0x04000800, 0x00200000, 0x04200800, 0x00000802, 0x00200802, 0x04200800,
  0x00000802, 0x04000002, 0x04200802, 0x04200000, 0x00200800, 0x00000000, 0x00000002, 0x04200802,
  0x00000000, 0x00200802, 0x04200000, 0x00000800, 0x04000002, 0x04000800, 0x00000800, 0x00200002
};

static u32 sbox8[64] =
{
  0x10001040, 0x00001000, 0x00040000, 0x10041040, 0x10000000, 0x10001040, 0x00000040, 0x10000000,
  0x00040040, 0x10040000, 0x10041040, 0x00041000, 0x10041000, 0x00041040, 0x00001000, 0x00000040,
  0x10040000, 0x10000040, 0x10001000, 0x00001040, 0x00041000, 0x00040040, 0x10040040, 0x10041000,
  0x00001040, 0x00000000, 0x00000000, 0x10040040, 0x10000040, 0x10001000, 0x00041040, 0x00040000,
  0x00041040, 0x00040000, 0x10041000, 0x00001000, 0x00000040, 0x10040040, 0x00001000, 0x00041040,
  0x10001000, 0x00000040, 0x10000040, 0x10040000, 0x10040040, 0x10000000, 0x00040000, 0x10001040,
  0x00000000, 0x10041040, 0x00040040, 0x10000040, 0x10040000, 0x10001000, 0x10001040, 0x00000000,
  0x10041040, 0x00041000, 0x00041000, 0x00001040, 0x00001040, 0x00040040, 0x10000000, 0x10041000
};


/*
 * These two tables are part of the 'permuted choice 1' function.
 * In this implementation several speed improvements are done.
 */
static u32 leftkey_swap[16] =
{
  0x00000000, 0x00000001, 0x00000100, 0x00000101,
  0x00010000, 0x00010001, 0x00010100, 0x00010101,
  0x01000000, 0x01000001, 0x01000100, 0x01000101,
  0x01010000, 0x01010001, 0x01010100, 0x01010101
};

static u32 rightkey_swap[16] =
{
  0x00000000, 0x01000000, 0x00010000, 0x01010000,
  0x00000100, 0x01000100, 0x00010100, 0x01010100,
  0x00000001, 0x01000001, 0x00010001, 0x01010001,
  0x00000101, 0x01000101, 0x00010101, 0x01010101,
};



/*
 * Numbers of left shifts per round for encryption subkeys.
 * To calculate the decryption subkeys we just reverse the
 * ordering of the calculated encryption subkeys. So their
 * is no need for a decryption rotate tab.
 */
static byte encrypt_rotate_tab[16] =
{
  1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1
};



/*
 * Table with weak DES keys sorted in ascending order.
 * In DES their are 64 known keys which are weak. They are weak
 * because they produce only one, two or four different
 * subkeys in the subkey scheduling process.
 * The keys in this table have all their parity bits cleared.
 */
static byte weak_keys[64][8] =
{
  { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, /*w*/
  { 0x00, 0x00, 0x1e, 0x1e, 0x00, 0x00, 0x0e, 0x0e },
  { 0x00, 0x00, 0xe0, 0xe0, 0x00, 0x00, 0xf0, 0xf0 },
  { 0x00, 0x00, 0xfe, 0xfe, 0x00, 0x00, 0xfe, 0xfe },
  { 0x00, 0x1e, 0x00, 0x1e, 0x00, 0x0e, 0x00, 0x0e }, /*sw*/
  { 0x00, 0x1e, 0x1e, 0x00, 0x00, 0x0e, 0x0e, 0x00 },
  { 0x00, 0x1e, 0xe0, 0xfe, 0x00, 0x0e, 0xf0, 0xfe },
  { 0x00, 0x1e, 0xfe, 0xe0, 0x00, 0x0e, 0xfe, 0xf0 },
  { 0x00, 0xe0, 0x00, 0xe0, 0x00, 0xf0, 0x00, 0xf0 }, /*sw*/
  { 0x00, 0xe0, 0x1e, 0xfe, 0x00, 0xf0, 0x0e, 0xfe },
  { 0x00, 0xe0, 0xe0, 0x00, 0x00, 0xf0, 0xf0, 0x00 },
  { 0x00, 0xe0, 0xfe, 0x1e, 0x00, 0xf0, 0xfe, 0x0e },
  { 0x00, 0xfe, 0x00, 0xfe, 0x00, 0xfe, 0x00, 0xfe }, /*sw*/
  { 0x00, 0xfe, 0x1e, 0xe0, 0x00, 0xfe, 0x0e, 0xf0 },
  { 0x00, 0xfe, 0xe0, 0x1e, 0x00, 0xfe, 0xf0, 0x0e },
  { 0x00, 0xfe, 0xfe, 0x00, 0x00, 0xfe, 0xfe, 0x00 },
  { 0x1e, 0x00, 0x00, 0x1e, 0x0e, 0x00, 0x00, 0x0e },
  { 0x1e, 0x00, 0x1e, 0x00, 0x0e, 0x00, 0x0e, 0x00 }, /*sw*/
  { 0x1e, 0x00, 0xe0, 0xfe, 0x0e, 0x00, 0xf0, 0xfe },
  { 0x1e, 0x00, 0xfe, 0xe0, 0x0e, 0x00, 0xfe, 0xf0 },
  { 0x1e, 0x1e, 0x00, 0x00, 0x0e, 0x0e, 0x00, 0x00 },
  { 0x1e, 0x1e, 0x1e, 0x1e, 0x0e, 0x0e, 0x0e, 0x0e }, /*w*/
  { 0x1e, 0x1e, 0xe0, 0xe0, 0x0e, 0x0e, 0xf0, 0xf0 },
  { 0x1e, 0x1e, 0xfe, 0xfe, 0x0e, 0x0e, 0xfe, 0xfe },
  { 0x1e, 0xe0, 0x00, 0xfe, 0x0e, 0xf0, 0x00, 0xfe },
  { 0x1e, 0xe0, 0x1e, 0xe0, 0x0e, 0xf0, 0x0e, 0xf0 }, /*sw*/
  { 0x1e, 0xe0, 0xe0, 0x1e, 0x0e, 0xf0, 0xf0, 0x0e },
  { 0x1e, 0xe0, 0xfe, 0x00, 0x0e, 0xf0, 0xfe, 0x00 },
  { 0x1e, 0xfe, 0x00, 0xe0, 0x0e, 0xfe, 0x00, 0xf0 },
  { 0x1e, 0xfe, 0x1e, 0xfe, 0x0e, 0xfe, 0x0e, 0xfe }, /*sw*/
  { 0x1e, 0xfe, 0xe0, 0x00, 0x0e, 0xfe, 0xf0, 0x00 },
  { 0x1e, 0xfe, 0xfe, 0x1e, 0x0e, 0xfe, 0xfe, 0x0e },
  { 0xe0, 0x00, 0x00, 0xe0, 0xf0, 0x00, 0x00, 0xf0 },
  { 0xe0, 0x00, 0x1e, 0xfe, 0xf0, 0x00, 0x0e, 0xfe },
  { 0xe0, 0x00, 0xe0, 0x00, 0xf0, 0x00, 0xf0, 0x00 }, /*sw*/
  { 0xe0, 0x00, 0xfe, 0x1e, 0xf0, 0x00, 0xfe, 0x0e },
  { 0xe0, 0x1e, 0x00, 0xfe, 0xf0, 0x0e, 0x00, 0xfe },
  { 0xe0, 0x1e, 0x1e, 0xe0, 0xf0, 0x0e, 0x0e, 0xf0 },
  { 0xe0, 0x1e, 0xe0, 0x1e, 0xf0, 0x0e, 0xf0, 0x0e }, /*sw*/
  { 0xe0, 0x1e, 0xfe, 0x00, 0xf0, 0x0e, 0xfe, 0x00 },
  { 0xe0, 0xe0, 0x00, 0x00, 0xf0, 0xf0, 0x00, 0x00 },
  { 0xe0, 0xe0, 0x1e, 0x1e, 0xf0, 0xf0, 0x0e, 0x0e },
  { 0xe0, 0xe0, 0xe0, 0xe0, 0xf0, 0xf0, 0xf0, 0xf0 }, /*w*/
  { 0xe0, 0xe0, 0xfe, 0xfe, 0xf0, 0xf0, 0xfe, 0xfe },
  { 0xe0, 0xfe, 0x00, 0x1e, 0xf0, 0xfe, 0x00, 0x0e },
  { 0xe0, 0xfe, 0x1e, 0x00, 0xf0, 0xfe, 0x0e, 0x00 },
  { 0xe0, 0xfe, 0xe0, 0xfe, 0xf0, 0xfe, 0xf0, 0xfe }, /*sw*/
  { 0xe0, 0xfe, 0xfe, 0xe0, 0xf0, 0xfe, 0xfe, 0xf0 },
  { 0xfe, 0x00, 0x00, 0xfe, 0xfe, 0x00, 0x00, 0xfe },
  { 0xfe, 0x00, 0x1e, 0xe0, 0xfe, 0x00, 0x0e, 0xf0 },
  { 0xfe, 0x00, 0xe0, 0x1e, 0xfe, 0x00, 0xf0, 0x0e },
  { 0xfe, 0x00, 0xfe, 0x00, 0xfe, 0x00, 0xfe, 0x00 }, /*sw*/
  { 0xfe, 0x1e, 0x00, 0xe0, 0xfe, 0x0e, 0x00, 0xf0 },
  { 0xfe, 0x1e, 0x1e, 0xfe, 0xfe, 0x0e, 0x0e, 0xfe },
  { 0xfe, 0x1e, 0xe0, 0x00, 0xfe, 0x0e, 0xf0, 0x00 },
  { 0xfe, 0x1e, 0xfe, 0x1e, 0xfe, 0x0e, 0xfe, 0x0e }, /*sw*/
  { 0xfe, 0xe0, 0x00, 0x1e, 0xfe, 0xf0, 0x00, 0x0e },
  { 0xfe, 0xe0, 0x1e, 0x00, 0xfe, 0xf0, 0x0e, 0x00 },
  { 0xfe, 0xe0, 0xe0, 0xfe, 0xfe, 0xf0, 0xf0, 0xfe },
  { 0xfe, 0xe0, 0xfe, 0xe0, 0xfe, 0xf0, 0xfe, 0xf0 }, /*sw*/
  { 0xfe, 0xfe, 0x00, 0x00, 0xfe, 0xfe, 0x00, 0x00 },
  { 0xfe, 0xfe, 0x1e, 0x1e, 0xfe, 0xfe, 0x0e, 0x0e },
  { 0xfe, 0xfe, 0xe0, 0xe0, 0xfe, 0xfe, 0xf0, 0xf0 },
  { 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe }  /*w*/
};
static unsigned char weak_keys_chksum[20] = {
  0xD0, 0xCF, 0x07, 0x38, 0x93, 0x70, 0x8A, 0x83, 0x7D, 0xD7,
  0x8A, 0x36, 0x65, 0x29, 0x6C, 0x1F, 0x7C, 0x3F, 0xD3, 0x41 
};



/*
 * Macro to swap bits across two words.
 */
#define DO_PERMUTATION(a, temp, b, offset, mask)	\
    temp = ((a>>offset) ^ b) & mask;			\
    b ^= temp;						\
    a ^= temp<<offset;


/*
 * This performs the 'initial permutation' of the data to be encrypted
 * or decrypted. Additionally the resulting two words are rotated one bit
 * to the left.
 */
#define INITIAL_PERMUTATION(left, temp, right)		\
    DO_PERMUTATION(left, temp, right, 4, 0x0f0f0f0f)	\
    DO_PERMUTATION(left, temp, right, 16, 0x0000ffff)	\
    DO_PERMUTATION(right, temp, left, 2, 0x33333333)	\
    DO_PERMUTATION(right, temp, left, 8, 0x00ff00ff)	\
    right =  (right << 1) | (right >> 31);		\
    temp  =  (left ^ right) & 0xaaaaaaaa;		\
    right ^= temp;					\
    left  ^= temp;					\
    left  =  (left << 1) | (left >> 31);

/*
 * The 'inverse initial permutation'.
 */
#define FINAL_PERMUTATION(left, temp, right)		\
    left  =  (left << 31) | (left >> 1);		\
    temp  =  (left ^ right) & 0xaaaaaaaa;		\
    left  ^= temp;					\
    right ^= temp;					\
    right  =  (right << 31) | (right >> 1);		\
    DO_PERMUTATION(right, temp, left, 8, 0x00ff00ff)	\
    DO_PERMUTATION(right, temp, left, 2, 0x33333333)	\
    DO_PERMUTATION(left, temp, right, 16, 0x0000ffff)	\
    DO_PERMUTATION(left, temp, right, 4, 0x0f0f0f0f)


/*
 * A full DES round including 'expansion function', 'sbox substitution'
 * and 'primitive function P' but without swapping the left and right word.
 * Please note: The data in 'from' and 'to' is already rotated one bit to
 * the left, done in the initial permutation.
 */
#define DES_ROUND(from, to, work, subkey)		\
    work = from ^ *subkey++;				\
    to ^= sbox8[  work	    & 0x3f ];			\
    to ^= sbox6[ (work>>8)  & 0x3f ];			\
    to ^= sbox4[ (work>>16) & 0x3f ];			\
    to ^= sbox2[ (work>>24) & 0x3f ];			\
    work = ((from << 28) | (from >> 4)) ^ *subkey++;	\
    to ^= sbox7[  work	    & 0x3f ];			\
    to ^= sbox5[ (work>>8)  & 0x3f ];			\
    to ^= sbox3[ (work>>16) & 0x3f ];			\
    to ^= sbox1[ (work>>24) & 0x3f ];

/*
 * Macros to convert 8 bytes from/to 32bit words.
 */
#define READ_64BIT_DATA(data, left, right)				   \
    left  = (data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3];  \
    right = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | data[7];

#define WRITE_64BIT_DATA(data, left, right)				   \
    data[0] = (left >> 24) &0xff; data[1] = (left >> 16) &0xff; 	   \
    data[2] = (left >> 8) &0xff; data[3] = left &0xff;			   \
    data[4] = (right >> 24) &0xff; data[5] = (right >> 16) &0xff;	   \
    data[6] = (right >> 8) &0xff; data[7] = right &0xff;

/*
 * Handy macros for encryption and decryption of data
 */
#define des_ecb_encrypt(ctx, from, to)	      des_ecb_crypt(ctx, from, to, 0)
#define des_ecb_decrypt(ctx, from, to)	      des_ecb_crypt(ctx, from, to, 1)
#define tripledes_ecb_encrypt(ctx, from, to) tripledes_ecb_crypt(ctx,from,to,0)
#define tripledes_ecb_decrypt(ctx, from, to) tripledes_ecb_crypt(ctx,from,to,1)






/*
 * des_key_schedule():	  Calculate 16 subkeys pairs (even/odd) for
 *			  16 encryption rounds.
 *			  To calculate subkeys for decryption the caller
 *			  have to reorder the generated subkeys.
 *
 *    rawkey:	    8 Bytes of key data
 *    subkey:	    Array of at least 32 u32s. Will be filled
 *		    with calculated subkeys.
 *
 */
static void
des_key_schedule (const byte * rawkey, u32 * subkey)
{
  u32 left, right, work;
  int round;

  READ_64BIT_DATA (rawkey, left, right)

  DO_PERMUTATION (right, work, left, 4, 0x0f0f0f0f)
  DO_PERMUTATION (right, work, left, 0, 0x10101010)

  left = ((leftkey_swap[(left >> 0) & 0xf] << 3)
          | (leftkey_swap[(left >> 8) & 0xf] << 2)
          | (leftkey_swap[(left >> 16) & 0xf] << 1)
          | (leftkey_swap[(left >> 24) & 0xf])
          | (leftkey_swap[(left >> 5) & 0xf] << 7)
          | (leftkey_swap[(left >> 13) & 0xf] << 6)
          | (leftkey_swap[(left >> 21) & 0xf] << 5)
          | (leftkey_swap[(left >> 29) & 0xf] << 4));

  left &= 0x0fffffff;

  right = ((rightkey_swap[(right >> 1) & 0xf] << 3)
           | (rightkey_swap[(right >> 9) & 0xf] << 2)
           | (rightkey_swap[(right >> 17) & 0xf] << 1)
           | (rightkey_swap[(right >> 25) & 0xf])
           | (rightkey_swap[(right >> 4) & 0xf] << 7)
           | (rightkey_swap[(right >> 12) & 0xf] << 6)
           | (rightkey_swap[(right >> 20) & 0xf] << 5)
           | (rightkey_swap[(right >> 28) & 0xf] << 4));

  right &= 0x0fffffff;

  for (round = 0; round < 16; ++round)
    {
      left = ((left << encrypt_rotate_tab[round])
              | (left >> (28 - encrypt_rotate_tab[round]))) & 0x0fffffff;
      right = ((right << encrypt_rotate_tab[round])
               | (right >> (28 - encrypt_rotate_tab[round]))) & 0x0fffffff;

      *subkey++ = (((left << 4) & 0x24000000)
                   | ((left << 28) & 0x10000000)
                   | ((left << 14) & 0x08000000)
                   | ((left << 18) & 0x02080000)
                   | ((left << 6) & 0x01000000)
                   | ((left << 9) & 0x00200000)
                   | ((left >> 1) & 0x00100000)
                   | ((left << 10) & 0x00040000)
                   | ((left << 2) & 0x00020000)
                   | ((left >> 10) & 0x00010000)
                   | ((right >> 13) & 0x00002000)
                   | ((right >> 4) & 0x00001000)
                   | ((right << 6) & 0x00000800)
                   | ((right >> 1) & 0x00000400)
                   | ((right >> 14) & 0x00000200)
                   | (right & 0x00000100)
                   | ((right >> 5) & 0x00000020)
                   | ((right >> 10) & 0x00000010)
                   | ((right >> 3) & 0x00000008)
                   | ((right >> 18) & 0x00000004)
                   | ((right >> 26) & 0x00000002)
                   | ((right >> 24) & 0x00000001));

      *subkey++ = (((left << 15) & 0x20000000)
                   | ((left << 17) & 0x10000000)
                   | ((left << 10) & 0x08000000)
                   | ((left << 22) & 0x04000000)
                   | ((left >> 2) & 0x02000000)
                   | ((left << 1) & 0x01000000)
                   | ((left << 16) & 0x00200000)
                   | ((left << 11) & 0x00100000)
                   | ((left << 3) & 0x00080000)
                   | ((left >> 6) & 0x00040000)
                   | ((left << 15) & 0x00020000)
                   | ((left >> 4) & 0x00010000)
                   | ((right >> 2) & 0x00002000)
                   | ((right << 8) & 0x00001000)
                   | ((right >> 14) & 0x00000808)
                   | ((right >> 9) & 0x00000400)
                   | ((right) & 0x00000200)
                   | ((right << 7) & 0x00000100)
                   | ((right >> 7) & 0x00000020)
                   | ((right >> 3) & 0x00000011)
                   | ((right << 2) & 0x00000004)
                   | ((right >> 21) & 0x00000002));
    }
}


/*
 * Fill a DES context with subkeys calculated from a 64bit key.
 * Does not check parity bits, but simply ignore them.
 * Does not check for weak keys.
 */
static int
des_setkey (struct _des_ctx *ctx, const byte * key)
{
  static const char *selftest_failed;
  int i;

  if (!fips_mode () && !initialized)
    {
      initialized = 1;
      selftest_failed = selftest ();

      if (selftest_failed)
	log_error ("%s\n", selftest_failed);
    }
  if (selftest_failed)
    return GPG_ERR_SELFTEST_FAILED;

  des_key_schedule (key, ctx->encrypt_subkeys);
  _gcry_burn_stack (32);

  for(i=0; i<32; i+=2)
    {
      ctx->decrypt_subkeys[i]	= ctx->encrypt_subkeys[30-i];
      ctx->decrypt_subkeys[i+1] = ctx->encrypt_subkeys[31-i];
    }

  return 0;
}



/*
 * Electronic Codebook Mode DES encryption/decryption of data according
 * to 'mode'.
 */
static int
des_ecb_crypt (struct _des_ctx *ctx, const byte * from, byte * to, int mode)
{
  u32 left, right, work;
  u32 *keys;

  keys = mode ? ctx->decrypt_subkeys : ctx->encrypt_subkeys;

  READ_64BIT_DATA (from, left, right)
  INITIAL_PERMUTATION (left, work, right)

  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)

  FINAL_PERMUTATION (right, work, left)
  WRITE_64BIT_DATA (to, right, left)

  return 0;
}



/*
 * Fill a Triple-DES context with subkeys calculated from two 64bit keys.
 * Does not check the parity bits of the keys, but simply ignore them.
 * Does not check for weak keys.
 */
static int
tripledes_set2keys (struct _tripledes_ctx *ctx,
		    const byte * key1,
		    const byte * key2)
{
  int i;

  des_key_schedule (key1, ctx->encrypt_subkeys);
  des_key_schedule (key2, &(ctx->decrypt_subkeys[32]));
  _gcry_burn_stack (32);

  for(i=0; i<32; i+=2)
    {
      ctx->decrypt_subkeys[i]	 = ctx->encrypt_subkeys[30-i];
      ctx->decrypt_subkeys[i+1]  = ctx->encrypt_subkeys[31-i];

      ctx->encrypt_subkeys[i+32] = ctx->decrypt_subkeys[62-i];
      ctx->encrypt_subkeys[i+33] = ctx->decrypt_subkeys[63-i];

      ctx->encrypt_subkeys[i+64] = ctx->encrypt_subkeys[i];
      ctx->encrypt_subkeys[i+65] = ctx->encrypt_subkeys[i+1];

      ctx->decrypt_subkeys[i+64] = ctx->decrypt_subkeys[i];
      ctx->decrypt_subkeys[i+65] = ctx->decrypt_subkeys[i+1];
    }

  return 0;
}



/*
 * Fill a Triple-DES context with subkeys calculated from three 64bit keys.
 * Does not check the parity bits of the keys, but simply ignore them.
 * Does not check for weak keys.
 */
static int
tripledes_set3keys (struct _tripledes_ctx *ctx,
		    const byte * key1,
		    const byte * key2,
		    const byte * key3)
{
  static const char *selftest_failed;
  int i;

  if (!fips_mode () && !initialized)
    {
      initialized = 1;
      selftest_failed = selftest ();

      if (selftest_failed)
	log_error ("%s\n", selftest_failed);
    }
  if (selftest_failed)
    return GPG_ERR_SELFTEST_FAILED;

  des_key_schedule (key1, ctx->encrypt_subkeys);
  des_key_schedule (key2, &(ctx->decrypt_subkeys[32]));
  des_key_schedule (key3, &(ctx->encrypt_subkeys[64]));
  _gcry_burn_stack (32);

  for(i=0; i<32; i+=2)
    {
      ctx->decrypt_subkeys[i]	 = ctx->encrypt_subkeys[94-i];
      ctx->decrypt_subkeys[i+1]  = ctx->encrypt_subkeys[95-i];

      ctx->encrypt_subkeys[i+32] = ctx->decrypt_subkeys[62-i];
      ctx->encrypt_subkeys[i+33] = ctx->decrypt_subkeys[63-i];

      ctx->decrypt_subkeys[i+64] = ctx->encrypt_subkeys[30-i];
      ctx->decrypt_subkeys[i+65] = ctx->encrypt_subkeys[31-i];
     }

  return 0;
}



/*
 * Electronic Codebook Mode Triple-DES encryption/decryption of data
 * according to 'mode'.  Sometimes this mode is named 'EDE' mode
 * (Encryption-Decryption-Encryption).
 */
static int
tripledes_ecb_crypt (struct _tripledes_ctx *ctx, const byte * from,
                     byte * to, int mode)
{
  u32 left, right, work;
  u32 *keys;

  keys = mode ? ctx->decrypt_subkeys : ctx->encrypt_subkeys;

  READ_64BIT_DATA (from, left, right)
  INITIAL_PERMUTATION (left, work, right)

  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)

  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)
  DES_ROUND (left, right, work, keys) DES_ROUND (right, left, work, keys)

  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)
  DES_ROUND (right, left, work, keys) DES_ROUND (left, right, work, keys)

  FINAL_PERMUTATION (right, work, left)
  WRITE_64BIT_DATA (to, right, left)

  return 0;
}





/*
 * Check whether the 8 byte key is weak.
 * Does not check the parity bits of the key but simple ignore them.
 */
static int
is_weak_key ( const byte *key )
{
  byte work[8];
  int i, left, right, middle, cmp_result;

  /* clear parity bits */
  for(i=0; i<8; ++i)
     work[i] = key[i] & 0xfe;

  /* binary search in the weak key table */
  left = 0;
  right = 63;
  while(left <= right)
    {
      middle = (left + right) / 2;

      if ( !(cmp_result=working_memcmp(work, weak_keys[middle], 8)) )
	  return -1;

      if ( cmp_result > 0 )
	  left = middle + 1;
      else
	  right = middle - 1;
    }

  return 0;
}



/*
 * Performs a selftest of this DES/Triple-DES implementation.
 * Returns an string with the error text on failure.
 * Returns NULL if all is ok.
 */
static const char *
selftest (void)
{
  /*
   * Check if 'u32' is really 32 bits wide. This DES / 3DES implementation
   * need this.
   */
  if (sizeof (u32) != 4)
    return "Wrong word size for DES configured.";

  /*
   * DES Maintenance Test
   */
  {
    int i;
    byte key[8] =
      {0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55};
    byte input[8] =
      {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
    byte result[8] =
      {0x24, 0x6e, 0x9d, 0xb9, 0xc5, 0x50, 0x38, 0x1a};
    byte temp1[8], temp2[8], temp3[8];
    des_ctx des;

    for (i = 0; i < 64; ++i)
      {
	des_setkey (des, key);
	des_ecb_encrypt (des, input, temp1);
	des_ecb_encrypt (des, temp1, temp2);
	des_setkey (des, temp2);
	des_ecb_decrypt (des, temp1, temp3);
	memcpy (key, temp3, 8);
	memcpy (input, temp1, 8);
      }
    if (memcmp (temp3, result, 8))
      return "DES maintenance test failed.";
  }


  /*
   * Self made Triple-DES test	(Does somebody know an official test?)
   */
  {
    int i;
    byte input[8] =
      {0xfe, 0xdc, 0xba, 0x98, 0x76, 0x54, 0x32, 0x10};
    byte key1[8] =
      {0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde, 0xf0};
    byte key2[8] =
      {0x11, 0x22, 0x33, 0x44, 0xff, 0xaa, 0xcc, 0xdd};
    byte result[8] =
      {0x7b, 0x38, 0x3b, 0x23, 0xa2, 0x7d, 0x26, 0xd3};

    tripledes_ctx des3;

    for (i = 0; i < 16; ++i)
      {
	tripledes_set2keys (des3, key1, key2);
	tripledes_ecb_encrypt (des3, input, key1);
	tripledes_ecb_decrypt (des3, input, key2);
	tripledes_set3keys (des3, key1, input, key2);
	tripledes_ecb_encrypt (des3, input, input);
      }
    if (memcmp (input, result, 8))
      return "Triple-DES test failed.";
  }
  
  /*
   * More Triple-DES test.  These are testvectors as used by SSLeay,
   * thanks to Jeroen C. van Gelderen.
   */
  { 
    struct { byte key[24]; byte plain[8]; byte cipher[8]; } testdata[] = {
      { { 0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01  },
        { 0x95,0xF8,0xA5,0xE5,0xDD,0x31,0xD9,0x00  },
        { 0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00  }
      },
      
      { { 0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01  },
        { 0x9D,0x64,0x55,0x5A,0x9A,0x10,0xB8,0x52, },
        { 0x00,0x00,0x00,0x10,0x00,0x00,0x00,0x00  }
      },
      { { 0x38,0x49,0x67,0x4C,0x26,0x02,0x31,0x9E,
          0x38,0x49,0x67,0x4C,0x26,0x02,0x31,0x9E,
          0x38,0x49,0x67,0x4C,0x26,0x02,0x31,0x9E  },
        { 0x51,0x45,0x4B,0x58,0x2D,0xDF,0x44,0x0A  },
        { 0x71,0x78,0x87,0x6E,0x01,0xF1,0x9B,0x2A  }
      },
      { { 0x04,0xB9,0x15,0xBA,0x43,0xFE,0xB5,0xB6,
          0x04,0xB9,0x15,0xBA,0x43,0xFE,0xB5,0xB6,
          0x04,0xB9,0x15,0xBA,0x43,0xFE,0xB5,0xB6  },
        { 0x42,0xFD,0x44,0x30,0x59,0x57,0x7F,0xA2  },
        { 0xAF,0x37,0xFB,0x42,0x1F,0x8C,0x40,0x95  }
      },
      { { 0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF,
          0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF,
          0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF  },
        { 0x73,0x6F,0x6D,0x65,0x64,0x61,0x74,0x61  },
        { 0x3D,0x12,0x4F,0xE2,0x19,0x8B,0xA3,0x18  }
      },
      { { 0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF,
          0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,
          0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF  },
        { 0x73,0x6F,0x6D,0x65,0x64,0x61,0x74,0x61  },
        { 0xFB,0xAB,0xA1,0xFF,0x9D,0x05,0xE9,0xB1  }
      },
      { { 0x01,0x23,0x45,0x67,0x89,0xAB,0xCD,0xEF,
          0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,
          0xFE,0xDC,0xBA,0x98,0x76,0x54,0x32,0x10  },
        { 0x73,0x6F,0x6D,0x65,0x64,0x61,0x74,0x61  },
        { 0x18,0xd7,0x48,0xe5,0x63,0x62,0x05,0x72  }
      },
      { { 0x03,0x52,0x02,0x07,0x67,0x20,0x82,0x17,
          0x86,0x02,0x87,0x66,0x59,0x08,0x21,0x98,
          0x64,0x05,0x6A,0xBD,0xFE,0xA9,0x34,0x57  },
        { 0x73,0x71,0x75,0x69,0x67,0x67,0x6C,0x65  },
        { 0xc0,0x7d,0x2a,0x0f,0xa5,0x66,0xfa,0x30  }
      },
      { { 0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x80,0x01,0x01,0x01,0x01,0x01,0x01,0x01,
          0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x02  },
        { 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00  },
        { 0xe6,0xe6,0xdd,0x5b,0x7e,0x72,0x29,0x74  }
      },
      { { 0x10,0x46,0x10,0x34,0x89,0x98,0x80,0x20,
          0x91,0x07,0xD0,0x15,0x89,0x19,0x01,0x01,
          0x19,0x07,0x92,0x10,0x98,0x1A,0x01,0x01  },
        { 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00  },
        { 0xe1,0xef,0x62,0xc3,0x32,0xfe,0x82,0x5b  }
      }
    };

    byte		result[8];
    int		i;
    tripledes_ctx	des3;

    for (i=0; i<sizeof(testdata)/sizeof(*testdata); ++i)
      {
        tripledes_set3keys (des3, testdata[i].key,
                            testdata[i].key + 8, testdata[i].key + 16);
        
        tripledes_ecb_encrypt (des3, testdata[i].plain, result);
        if (memcmp (testdata[i].cipher, result, 8))
          return "Triple-DES SSLeay test failed on encryption.";

        tripledes_ecb_decrypt (des3, testdata[i].cipher, result);
        if (memcmp (testdata[i].plain, result, 8))
          return  "Triple-DES SSLeay test failed on decryption.";;
      }
  }

  /*
   * Check the weak key detection. We simply assume that the table
   * with weak keys is ok and check every key in the table if it is
   * detected... (This test is a little bit stupid).
   */
  {
    int i;
    unsigned char *p;
    gcry_md_hd_t h;

    if (_gcry_md_open (&h, GCRY_MD_SHA1, 0))
      return "SHA1 not available";

    for (i = 0; i < 64; ++i)
      _gcry_md_write (h, weak_keys[i], 8);
    p = _gcry_md_read (h, GCRY_MD_SHA1);
    i = memcmp (p, weak_keys_chksum, 20);
    _gcry_md_close (h);
    if (i)
      return "weak key table defect";

    for (i = 0; i < 64; ++i)
      if (!is_weak_key(weak_keys[i]))
        return "DES weak key detection failed";
  }

  return 0;
}


static gcry_err_code_t
do_tripledes_setkey ( void *context, const byte *key, unsigned keylen )
{
  struct _tripledes_ctx *ctx = (struct _tripledes_ctx *) context;

  if( keylen != 24 )
    return GPG_ERR_INV_KEYLEN;

  tripledes_set3keys ( ctx, key, key+8, key+16);

  if (ctx->flags.no_weak_key)
    ; /* Detection has been disabled.  */
  else if (is_weak_key (key) || is_weak_key (key+8) || is_weak_key (key+16))
    {
      _gcry_burn_stack (64);
      return GPG_ERR_WEAK_KEY;
    }
  _gcry_burn_stack (64);

  return GPG_ERR_NO_ERROR;
}


static gcry_err_code_t
do_tripledes_set_extra_info (void *context, int what,
                             const void *buffer, size_t buflen)
{
  struct _tripledes_ctx *ctx = (struct _tripledes_ctx *)context;
  gpg_err_code_t ec = 0;

  (void)buffer;
  (void)buflen;

  switch (what)
    {
    case CIPHER_INFO_NO_WEAK_KEY:
      ctx->flags.no_weak_key = 1;
      break;

    default:
      ec = GPG_ERR_INV_OP; 
      break;
    }
  return ec;
}


static void
do_tripledes_encrypt( void *context, byte *outbuf, const byte *inbuf )
{
  struct _tripledes_ctx *ctx = (struct _tripledes_ctx *) context;

  tripledes_ecb_encrypt ( ctx, inbuf, outbuf );
  _gcry_burn_stack (32);
}

static void
do_tripledes_decrypt( void *context, byte *outbuf, const byte *inbuf )
{
  struct _tripledes_ctx *ctx = (struct _tripledes_ctx *) context;
  tripledes_ecb_decrypt ( ctx, inbuf, outbuf );
  _gcry_burn_stack (32);
}

static gcry_err_code_t
do_des_setkey (void *context, const byte *key, unsigned keylen)
{
  struct _des_ctx *ctx = (struct _des_ctx *) context;

  if (keylen != 8)
    return GPG_ERR_INV_KEYLEN;

  des_setkey (ctx, key);

  if (is_weak_key (key)) {
    _gcry_burn_stack (64);
    return GPG_ERR_WEAK_KEY;
  }
  _gcry_burn_stack (64);

  return GPG_ERR_NO_ERROR;
}


static void
do_des_encrypt( void *context, byte *outbuf, const byte *inbuf )
{
  struct _des_ctx *ctx = (struct _des_ctx *) context;

  des_ecb_encrypt ( ctx, inbuf, outbuf );
  _gcry_burn_stack (32);
}

static void
do_des_decrypt( void *context, byte *outbuf, const byte *inbuf )
{
  struct _des_ctx *ctx = (struct _des_ctx *) context;

  des_ecb_decrypt ( ctx, inbuf, outbuf );
  _gcry_burn_stack (32);
}




/* 
     Self-test section.
 */


/* Selftest for TripleDES.  */
static gpg_err_code_t
selftest_fips (int extended, selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  
  (void)extended; /* No extended tests available.  */

  what = "low-level";
  errtxt = selftest ();
  if (errtxt)
    goto failed;

  /* The low-level self-tests are quite extensive and thus we can do
     without high level tests.  This is also justified because we have
     no custom block code implementation for 3des but always use the
     standard high level block code.  */

  return 0; /* Succeeded. */

 failed:
  if (report)
    report ("cipher", GCRY_CIPHER_3DES, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}



/* Run a full self-test for ALGO and return 0 on success.  */
static gpg_err_code_t
run_selftests (int algo, int extended, selftest_report_func_t report)
{
  gpg_err_code_t ec;

  switch (algo)
    {
    case GCRY_CIPHER_3DES:
      ec = selftest_fips (extended, report);
      break;
    default:
      ec = GPG_ERR_CIPHER_ALGO;
      break;
        
    }
  return ec;
}



gcry_cipher_spec_t _gcry_cipher_spec_des =
  {
    "DES", NULL, NULL, 8, 64, sizeof (struct _des_ctx),
    do_des_setkey, do_des_encrypt, do_des_decrypt
  };

static gcry_cipher_oid_spec_t oids_tripledes[] =
  {
    { "1.2.840.113549.3.7", GCRY_CIPHER_MODE_CBC },
    /* Teletrust specific OID for 3DES. */
    { "1.3.36.3.1.3.2.1",   GCRY_CIPHER_MODE_CBC },
    /* pbeWithSHAAnd3_KeyTripleDES_CBC */
    { "1.2.840.113549.1.12.1.3", GCRY_CIPHER_MODE_CBC },
    { NULL }
  };

gcry_cipher_spec_t _gcry_cipher_spec_tripledes =
  {
    "3DES", NULL, oids_tripledes, 8, 192, sizeof (struct _tripledes_ctx),
    do_tripledes_setkey, do_tripledes_encrypt, do_tripledes_decrypt
  };

cipher_extra_spec_t _gcry_cipher_extraspec_tripledes = 
  {
    run_selftests,
    do_tripledes_set_extra_info
  };
