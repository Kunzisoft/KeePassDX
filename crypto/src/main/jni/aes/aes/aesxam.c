/*
 ---------------------------------------------------------------------------
 Copyright (c) 1998-2008, Brian Gladman, Worcester, UK. All rights reserved.

 LICENSE TERMS

 The redistribution and use of this software (with or without changes)
 is allowed without the payment of fees or royalties provided that:

  1. source code distributions include the above copyright notice, this
     list of conditions and the following disclaimer;

  2. binary distributions include the above copyright notice, this list
     of conditions and the following disclaimer in their documentation;

  3. the name of the copyright holder is not used to endorse products
     built using this software without specific written permission.

 DISCLAIMER

 This software is provided 'as is' with no explicit or implied warranties
 in respect of its properties, including, but not limited to, correctness
 and/or fitness for purpose.
 ---------------------------------------------------------------------------
 Issue Date: 20/12/2007
*/

//  An example of the use of AES (Rijndael) for file encryption.  This code
//  implements AES in CBC mode with ciphertext stealing when the file length
//  is greater than one block (16 bytes).  This code is an example of how to
//  use AES and is not intended for real use since it does not provide any
//  file integrity checking.
//
//  The Command line is:
//
//      aesxam input_file_name output_file_name [D|E] hexadecimalkey
//
//  where E gives encryption and D decryption of the input file into the
//  output file using the given hexadecimal key string.  The later is a
//  hexadecimal sequence of 32, 48 or 64 digits.  Examples to encrypt or
//  decrypt aes.c into aes.enc are:
//
//      aesxam file.c file.enc E 0123456789abcdeffedcba9876543210
//
//      aesxam file.enc file2.c D 0123456789abcdeffedcba9876543210
//
//  which should return a file 'file2.c' identical to 'file.c'
//
//  CIPHERTEXT STEALING
//
//  Ciphertext stealing modifies the encryption of the last two CBC
//  blocks. It can be applied invariably to the last two plaintext
//  blocks or only applied when the last block is a partial one. In
//  this code it is only applied if there is a partial block.  For
//  a plaintext consisting of N blocks, with the last block possibly
//  a partial one, ciphertext stealing works as shown below (note the
//  reversal of the last two ciphertext blocks).  During decryption
//  the part of the C:N-1 block that is not transmitted (X) can be
//  obtained from the decryption of the penultimate ciphertext block
//  since the bytes in X are xored with the zero padding appended to
//  the last plaintext block.
//
//  This is a picture of the processing of the last
//  plaintext blocks during encryption:
//
//    +---------+   +---------+   +---------+   +-------+-+
//    |  P:N-4  |   |  P:N-3  |   |  P:N-2  |   | P:N-1 |0|
//    +---------+   +---------+   +---------+   +-------+-+
//         |             |             |             |
//         v             v             v             v
//  +----->x      +----->x      +----->x      +----->x   x = xor
//  |      |      |      |      |      |      |      |
//  |      v      |      v      |      v      |      v
//  |    +---+    |    +---+    |    +---+    |    +---+
//  |    | E |    |    | E |    |    | E |    |    | E |
//  |    +---+    |    +---+    |    +---+    |    +---+
//  |      |      |      |      |      |      |      |
//  |      |      |      |      |      v      |  +---+
//  |      |      |      |      | +-------+-+ |  |
//  |      |      |      |      | | C:N-1 |X| |  |
//  |      |      |      |      | +-------+-+ ^  |
//  |      |      |      |      |     ||      |  |
//  |      |      |      |      |     |+------+  |
//  |      |      |      |      |     +----------|--+
//  |      |      |      |      |                |  |
//  |      |      |      |      |      +---------+  |
//  |      |      |      |      |      |            |
//  |      v      |      v      |      v            v
//  | +---------+ | +---------+ | +---------+   +-------+
// -+ |  C:N-4  |-+ |  C:N-3  |-+ |  C:N-2  |   | C:N-1 |
//    +---------+   +---------+   +---------+   +-------+
//
//  And this is a picture of the processing of the last
//  ciphertext blocks during decryption:
//
//    +---------+   +---------+   +---------+   +-------+
// -+ |  C:N-4  |-+ |  C:N-3  |-+ |  C:N-2  |   | C:N-1 |
//  | +---------+ | +---------+ | +---------+   +-------+
//  |      |      |      |      |      |            |
//  |      v      |      v      |      v   +--------|----+
//  |    +---+    |    +---+    |    +---+ |  +--<--+    |
//  |    | D |    |    | D |    |    | D | |  |     |    |
//  |    +---+    |    +---+    |    +---+ |  |     v    v
//  |      |      |      |      |      |   ^  | +-------+-+
//  |      v      |      v      |      v   |  | | C:N-1 |X|
//  +----->x      +----->x      | +-------+-+ | +-------+-+
//         |             |      | |       |X| |      |
//         |             |      | +-------+-+ |      v
//         |             |      |     |       |    +---+
//         |             |      |     |       v    | D |
//         |             |      |     +------>x    +---+
//         |             |      |             |      |
//         |             |      +----->x<-----|------+   x = xor
//         |             |             |      +-----+
//         |             |             |            |
//         v             v             v            v
//    +---------+   +---------+   +---------+   +-------+
//    |  P:N-4  |   |  P:N-3  |   |  P:N-2  |   | P:N-1 |
//    +---------+   +---------+   +---------+   +-------+

#include <stdio.h>
#include <ctype.h>

#include "aes.h"
#include "rdtsc.h"

#define BLOCK_LEN   16

#define OK           0
#define READ_ERROR  -7
#define WRITE_ERROR -8

//  A Pseudo Random Number Generator (PRNG) used for the
//  Initialisation Vector. The PRNG is George Marsaglia's
//  Multiply-With-Carry (MWC) PRNG that concatenates two
//  16-bit MWC generators:
//      x(n)=36969 * x(n-1) + carry mod 2^16
//      y(n)=18000 * y(n-1) + carry mod 2^16
//  to produce a combined PRNG with a period of about 2^60.
//  The Pentium cycle counter is used to initialise it. This
//  is crude but the IV does not really need to be secret.

#define RAND(a,b) (((a = 36969 * (a & 65535) + (a >> 16)) << 16) + \
                    (b = 18000 * (b & 65535) + (b >> 16))  )

void fillrand(unsigned char *buf, const int len)
{   static unsigned long a[2], mt = 1, count = 4;
    static unsigned char r[4];
    int                  i;

    if(mt) { mt = 0; *(unsigned long long*)a = read_tsc(); }

    for(i = 0; i < len; ++i)
    {
        if(count == 4)
        {
            *(unsigned long*)r = RAND(a[0], a[1]);
            count = 0;
        }

        buf[i] = r[count++];
    }
}

int encfile(FILE *fin, FILE *fout, aes_encrypt_ctx ctx[1])
{   unsigned char dbuf[3 * BLOCK_LEN];
    unsigned long i, len, wlen = BLOCK_LEN;

    // When ciphertext stealing is used, we three ciphertext blocks so
    // we use a buffer that is three times the block length.  The buffer
    // pointers b1, b2 and b3 point to the buffer positions of three
    // ciphertext blocks, b3 being the most recent and b1 being the
    // oldest. We start with the IV in b1 and the block to be decrypted
    // in b2.

    // set a random IV

    fillrand(dbuf, BLOCK_LEN);

    // read the first file block
    len = (unsigned long) fread((char*)dbuf + BLOCK_LEN, 1, BLOCK_LEN, fin);

    if(len < BLOCK_LEN)
    {   // if the file length is less than one block

        // xor the file bytes with the IV bytes
        for(i = 0; i < len; ++i)
            dbuf[i + BLOCK_LEN] ^= dbuf[i];

        // encrypt the top 16 bytes of the buffer
        aes_encrypt(dbuf + len, dbuf + len, ctx);

        len += BLOCK_LEN;
        // write the IV and the encrypted file bytes
        if(fwrite((char*)dbuf, 1, len, fout) != len)
            return WRITE_ERROR;

        return OK;
    }
    else    // if the file length is more 16 bytes
    {   unsigned char *b1 = dbuf, *b2 = b1 + BLOCK_LEN, *b3 = b2 + BLOCK_LEN, *bt;

        // write the IV
        if(fwrite((char*)dbuf, 1, BLOCK_LEN, fout) != BLOCK_LEN)
            return WRITE_ERROR;

        for( ; ; )
        {
            // read the next block to see if ciphertext stealing is needed
            len = (unsigned long)fread((char*)b3, 1, BLOCK_LEN, fin);

            // do CBC chaining prior to encryption for current block (in b2)
            for(i = 0; i < BLOCK_LEN; ++i)
                b1[i] ^= b2[i];

            // encrypt the block (now in b1)
            aes_encrypt(b1, b1, ctx);

            if(len != 0 && len != BLOCK_LEN)    // use ciphertext stealing
            {
                // set the length of the last block
                wlen = len;

                // xor ciphertext into last block
                for(i = 0; i < len; ++i)
                    b3[i] ^= b1[i];

                // move 'stolen' ciphertext into last block
                for(i = len; i < BLOCK_LEN; ++i)
                    b3[i] = b1[i];

                // encrypt this block
                aes_encrypt(b3, b3, ctx);

                // and write it as the second to last encrypted block
                if(fwrite((char*)b3, 1, BLOCK_LEN, fout) != BLOCK_LEN)
                    return WRITE_ERROR;
            }

            // write the encrypted block
            if(fwrite((char*)b1, 1, wlen, fout) != wlen)
                return WRITE_ERROR;

            if(len != BLOCK_LEN)
                return OK;

            // advance the buffer pointers
            bt = b3, b3 = b2, b2 = b1, b1 = bt;
        }
    }
}

int decfile(FILE *fin, FILE *fout, aes_decrypt_ctx ctx[1])
{   unsigned char dbuf[3 * BLOCK_LEN], buf[BLOCK_LEN];
    unsigned long i, len, wlen = BLOCK_LEN;

    // When ciphertext stealing is used, we three ciphertext blocks so
    // we use a buffer that is three times the block length.  The buffer
    // pointers b1, b2 and b3 point to the buffer positions of three
    // ciphertext blocks, b3 being the most recent and b1 being the
    // oldest. We start with the IV in b1 and the block to be decrypted
    // in b2.

    len = (unsigned long)fread((char*)dbuf, 1, 2 * BLOCK_LEN, fin);

    if(len < 2 * BLOCK_LEN) // the original file is less than one block in length
    {
        len -= BLOCK_LEN;
        // decrypt from position len to position len + BLOCK_LEN
        aes_decrypt(dbuf + len, dbuf + len, ctx);

        // undo the CBC chaining
        for(i = 0; i < len; ++i)
            dbuf[i] ^= dbuf[i + BLOCK_LEN];

        // output the decrypted bytes
        if(fwrite((char*)dbuf, 1, len, fout) != len)
            return WRITE_ERROR;

        return OK;
    }
    else
    {   unsigned char *b1 = dbuf, *b2 = b1 + BLOCK_LEN, *b3 = b2 + BLOCK_LEN, *bt;

        for( ; ; )  // while some ciphertext remains, prepare to decrypt block b2
        {
            // read in the next block to see if ciphertext stealing is needed
            len = fread((char*)b3, 1, BLOCK_LEN, fin);

            // decrypt the b2 block
            aes_decrypt(b2, buf, ctx);

            if(len == 0 || len == BLOCK_LEN)    // no ciphertext stealing
            {
                // unchain CBC using the previous ciphertext block in b1
                for(i = 0; i < BLOCK_LEN; ++i)
                    buf[i] ^= b1[i];
            }
            else    // partial last block - use ciphertext stealing
            {
                wlen = len;

                // produce last 'len' bytes of plaintext by xoring with
                // the lowest 'len' bytes of next block b3 - C[N-1]
                for(i = 0; i < len; ++i)
                    buf[i] ^= b3[i];

                // reconstruct the C[N-1] block in b3 by adding in the
                // last (BLOCK_LEN - len) bytes of C[N-2] in b2
                for(i = len; i < BLOCK_LEN; ++i)
                    b3[i] = buf[i];

                // decrypt the C[N-1] block in b3
                aes_decrypt(b3, b3, ctx);

                // produce the last but one plaintext block by xoring with
                // the last but two ciphertext block
                for(i = 0; i < BLOCK_LEN; ++i)
                    b3[i] ^= b1[i];

                // write decrypted plaintext blocks
                if(fwrite((char*)b3, 1, BLOCK_LEN, fout) != BLOCK_LEN)
                    return WRITE_ERROR;
            }

            // write the decrypted plaintext block
            if(fwrite((char*)buf, 1, wlen, fout) != wlen)
                return WRITE_ERROR;

            if(len != BLOCK_LEN)
                return OK;

            // advance the buffer pointers
            bt = b1, b1 = b2, b2 = b3, b3 = bt;
        }
    }
}

int main(int argc, char *argv[])
{   FILE            *fin = 0, *fout = 0;
    char            *cp, ch, key[32];
    int             i, by = 0, key_len, err = 0;

    if(argc != 5 || toupper(*argv[3]) != 'D' && toupper(*argv[3]) != 'E')
    {
        printf("usage: aesxam in_filename out_filename [d/e] key_in_hex\n");
        err = -1; goto exit;
    }

    aes_init();     // in case dynamic AES tables are being used

    cp = argv[4];   // this is a pointer to the hexadecimal key digits
    i = 0;          // this is a count for the input digits processed

    while(i < 64 && *cp)        // the maximum key length is 32 bytes and
    {                           // hence at most 64 hexadecimal digits
        ch = toupper(*cp++);    // process a hexadecimal digit
        if(ch >= '0' && ch <= '9')
            by = (by << 4) + ch - '0';
        else if(ch >= 'A' && ch <= 'F')
            by = (by << 4) + ch - 'A' + 10;
        else                    // error if not hexadecimal
        {
            printf("key must be in hexadecimal notation\n");
            err = -2; goto exit;
        }

        // store a key byte for each pair of hexadecimal digits
        if(i++ & 1)
            key[i / 2 - 1] = by & 0xff;
    }

    if(*cp)
    {
        printf("The key value is too long\n");
        err = -3; goto exit;
    }
    else if(i < 32 || (i & 15))
    {
        printf("The key length must be 32, 48 or 64 hexadecimal digits\n");
        err = -4; goto exit;
    }

    key_len = i / 2;

    if(!(fin = fopen(argv[1], "rb")))   // try to open the input file
    {
        printf("The input file: %s could not be opened\n", argv[1]);
        err = -5; goto exit;
    }

    if(!(fout = fopen(argv[2], "wb")))  // try to open the output file
    {
        printf("The output file: %s could not be opened\n", argv[2]);
        err = -6; goto exit;
    }

    if(toupper(*argv[3]) == 'E') // encryption in Cipher Block Chaining mode
    {   aes_encrypt_ctx ctx[1];

        aes_encrypt_key((unsigned char*)key, key_len, ctx);

        err = encfile(fin, fout, ctx);
    }
    else                         // decryption in Cipher Block Chaining mode
    {   aes_decrypt_ctx ctx[1];

        aes_decrypt_key((unsigned char*)key, key_len, ctx);

        err = decfile(fin, fout, ctx);
    }
exit:
    if(err == READ_ERROR)
        printf("Error reading from input file: %s\n", argv[1]);

    if(err == WRITE_ERROR)
        printf("Error writing to output file: %s\n", argv[2]);

    if(fout)
        fclose(fout);

    if(fin)
        fclose(fin);

    return err;
}
