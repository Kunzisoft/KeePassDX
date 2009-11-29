/* rmd160.c  -	RIPE-MD160
 * Copyright (C) 1998, 2001, 2002, 2003 Free Software Foundation, Inc.
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
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "g10lib.h"
#include "memory.h"
#include "rmd.h"
#include "cipher.h" /* Only used for the rmd160_hash_buffer() prototype. */

#include "bithelp.h"

/*********************************
 * RIPEMD-160 is not patented, see (as of 25.10.97)
 *   http://www.esat.kuleuven.ac.be/~bosselae/ripemd160.html
 * Note that the code uses Little Endian byteorder, which is good for
 * 386 etc, but we must add some conversion when used on a big endian box.
 *
 *
 * Pseudo-code for RIPEMD-160
 *
 * RIPEMD-160 is an iterative hash function that operates on 32-bit words.
 * The round function takes as input a 5-word chaining variable and a 16-word
 * message block and maps this to a new chaining variable. All operations are
 * defined on 32-bit words. Padding is identical to that of MD4.
 *
 *
 * RIPEMD-160: definitions
 *
 *
 *   nonlinear functions at bit level: exor, mux, -, mux, -
 *
 *   f(j, x, y, z) = x XOR y XOR z		  (0 <= j <= 15)
 *   f(j, x, y, z) = (x AND y) OR (NOT(x) AND z)  (16 <= j <= 31)
 *   f(j, x, y, z) = (x OR NOT(y)) XOR z	  (32 <= j <= 47)
 *   f(j, x, y, z) = (x AND z) OR (y AND NOT(z))  (48 <= j <= 63)
 *   f(j, x, y, z) = x XOR (y OR NOT(z))	  (64 <= j <= 79)
 *
 *
 *   added constants (hexadecimal)
 *
 *   K(j) = 0x00000000	    (0 <= j <= 15)
 *   K(j) = 0x5A827999	   (16 <= j <= 31)	int(2**30 x sqrt(2))
 *   K(j) = 0x6ED9EBA1	   (32 <= j <= 47)	int(2**30 x sqrt(3))
 *   K(j) = 0x8F1BBCDC	   (48 <= j <= 63)	int(2**30 x sqrt(5))
 *   K(j) = 0xA953FD4E	   (64 <= j <= 79)	int(2**30 x sqrt(7))
 *   K'(j) = 0x50A28BE6     (0 <= j <= 15)      int(2**30 x cbrt(2))
 *   K'(j) = 0x5C4DD124    (16 <= j <= 31)      int(2**30 x cbrt(3))
 *   K'(j) = 0x6D703EF3    (32 <= j <= 47)      int(2**30 x cbrt(5))
 *   K'(j) = 0x7A6D76E9    (48 <= j <= 63)      int(2**30 x cbrt(7))
 *   K'(j) = 0x00000000    (64 <= j <= 79)
 *
 *
 *   selection of message word
 *
 *   r(j)      = j		      (0 <= j <= 15)
 *   r(16..31) = 7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8
 *   r(32..47) = 3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12
 *   r(48..63) = 1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2
 *   r(64..79) = 4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13
 *   r0(0..15) = 5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12
 *   r0(16..31)= 6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2
 *   r0(32..47)= 15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13
 *   r0(48..63)= 8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14
 *   r0(64..79)= 12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11
 *
 *
 *   amount for rotate left (rol)
 *
 *   s(0..15)  = 11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8
 *   s(16..31) = 7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12
 *   s(32..47) = 11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5
 *   s(48..63) = 11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12
 *   s(64..79) = 9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6
 *   s'(0..15) = 8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6
 *   s'(16..31)= 9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11
 *   s'(32..47)= 9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5
 *   s'(48..63)= 15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8
 *   s'(64..79)= 8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11
 *
 *
 *   initial value (hexadecimal)
 *
 *   h0 = 0x67452301; h1 = 0xEFCDAB89; h2 = 0x98BADCFE; h3 = 0x10325476;
 *							h4 = 0xC3D2E1F0;
 *
 *
 * RIPEMD-160: pseudo-code
 *
 *   It is assumed that the message after padding consists of t 16-word blocks
 *   that will be denoted with X[i][j], with 0 <= i <= t-1 and 0 <= j <= 15.
 *   The symbol [+] denotes addition modulo 2**32 and rol_s denotes cyclic left
 *   shift (rotate) over s positions.
 *
 *
 *   for i := 0 to t-1 {
 *	 A := h0; B := h1; C := h2; D = h3; E = h4;
 *	 A' := h0; B' := h1; C' := h2; D' = h3; E' = h4;
 *	 for j := 0 to 79 {
 *	     T := rol_s(j)(A [+] f(j, B, C, D) [+] X[i][r(j)] [+] K(j)) [+] E;
 *	     A := E; E := D; D := rol_10(C); C := B; B := T;
 *	     T := rol_s'(j)(A' [+] f(79-j, B', C', D') [+] X[i][r'(j)]
						       [+] K'(j)) [+] E';
 *	     A' := E'; E' := D'; D' := rol_10(C'); C' := B'; B' := T;
 *	 }
 *	 T := h1 [+] C [+] D'; h1 := h2 [+] D [+] E'; h2 := h3 [+] E [+] A';
 *	 h3 := h4 [+] A [+] B'; h4 := h0 [+] B [+] C'; h0 := T;
 *   }
 */

/* Some examples:
 * ""                    9c1185a5c5e9fc54612808977ee8f548b2258d31
 * "a"                   0bdc9d2d256b3ee9daae347be6f4dc835a467ffe
 * "abc"                 8eb208f7e05d987a9b044a8e98c6b087f15a0bfc
 * "message digest"      5d0689ef49d2fae572b881b123a85ffa21595f36
 * "a...z"               f71c27109c692c1b56bbdceb5b9d2865b3708dbc
 * "abcdbcde...nopq"     12a053384a9c0c88e405a06c27dcf49ada62eb2b
 * "A...Za...z0...9"     b0e20b6e3116640286ed3a87a5713079b21f5189
 * 8 times "1234567890"  9b752e45573d4b39f4dbd3323cab82bf63326bfb
 * 1 million times "a"   52783243c1697bdbe16d37f97f68f08325dc1528
 */


void
_gcry_rmd160_init (void *context)
{
  RMD160_CONTEXT *hd = context;

  hd->h0 = 0x67452301;
  hd->h1 = 0xEFCDAB89;
  hd->h2 = 0x98BADCFE;
  hd->h3 = 0x10325476;
  hd->h4 = 0xC3D2E1F0;
  hd->nblocks = 0;
  hd->count = 0;
}



/****************
 * Transform the message X which consists of 16 32-bit-words
 */
static void
transform ( RMD160_CONTEXT *hd, const unsigned char *data )
{
  register u32 a,b,c,d,e;
  u32 aa,bb,cc,dd,ee,t;
#ifdef WORDS_BIGENDIAN
  u32 x[16];
  {
    int i;
    byte *p2, *p1;
    for (i=0, p1=data, p2=(byte*)x; i < 16; i++, p2 += 4 )
      {
        p2[3] = *p1++;
        p2[2] = *p1++;
        p2[1] = *p1++;
        p2[0] = *p1++;
      }
  }
#else
  /* This version is better because it is always aligned;
   * The performance penalty on a 586-100 is about 6% which
   * is acceptable - because the data is more local it might
   * also be possible that this is faster on some machines.
   * This function (when compiled with -02 on gcc 2.7.2)
   * executes on a 586-100 (39.73 bogomips) at about 1900kb/sec;
   * [measured with a 4MB data and "gpgm --print-md rmd160"] */
  u32 x[16];
  memcpy( x, data, 64 );
#endif


#define K0  0x00000000
#define K1  0x5A827999
#define K2  0x6ED9EBA1
#define K3  0x8F1BBCDC
#define K4  0xA953FD4E
#define KK0 0x50A28BE6
#define KK1 0x5C4DD124
#define KK2 0x6D703EF3
#define KK3 0x7A6D76E9
#define KK4 0x00000000
#define F0(x,y,z)   ( (x) ^ (y) ^ (z) )
#define F1(x,y,z)   ( ((x) & (y)) | (~(x) & (z)) )
#define F2(x,y,z)   ( ((x) | ~(y)) ^ (z) )
#define F3(x,y,z)   ( ((x) & (z)) | ((y) & ~(z)) )
#define F4(x,y,z)   ( (x) ^ ((y) | ~(z)) )
#define R(a,b,c,d,e,f,k,r,s) do { t = a + f(b,c,d) + k + x[r]; \
				  a = rol(t,s) + e;	       \
				  c = rol(c,10);	       \
				} while(0)

  /* left lane */
  a = hd->h0;
  b = hd->h1;
  c = hd->h2;
  d = hd->h3;
  e = hd->h4;
  R( a, b, c, d, e, F0, K0,  0, 11 );
  R( e, a, b, c, d, F0, K0,  1, 14 );
  R( d, e, a, b, c, F0, K0,  2, 15 );
  R( c, d, e, a, b, F0, K0,  3, 12 );
  R( b, c, d, e, a, F0, K0,  4,  5 );
  R( a, b, c, d, e, F0, K0,  5,  8 );
  R( e, a, b, c, d, F0, K0,  6,  7 );
  R( d, e, a, b, c, F0, K0,  7,  9 );
  R( c, d, e, a, b, F0, K0,  8, 11 );
  R( b, c, d, e, a, F0, K0,  9, 13 );
  R( a, b, c, d, e, F0, K0, 10, 14 );
  R( e, a, b, c, d, F0, K0, 11, 15 );
  R( d, e, a, b, c, F0, K0, 12,  6 );
  R( c, d, e, a, b, F0, K0, 13,  7 );
  R( b, c, d, e, a, F0, K0, 14,  9 );
  R( a, b, c, d, e, F0, K0, 15,  8 );
  R( e, a, b, c, d, F1, K1,  7,  7 );
  R( d, e, a, b, c, F1, K1,  4,  6 );
  R( c, d, e, a, b, F1, K1, 13,  8 );
  R( b, c, d, e, a, F1, K1,  1, 13 );
  R( a, b, c, d, e, F1, K1, 10, 11 );
  R( e, a, b, c, d, F1, K1,  6,  9 );
  R( d, e, a, b, c, F1, K1, 15,  7 );
  R( c, d, e, a, b, F1, K1,  3, 15 );
  R( b, c, d, e, a, F1, K1, 12,  7 );
  R( a, b, c, d, e, F1, K1,  0, 12 );
  R( e, a, b, c, d, F1, K1,  9, 15 );
  R( d, e, a, b, c, F1, K1,  5,  9 );
  R( c, d, e, a, b, F1, K1,  2, 11 );
  R( b, c, d, e, a, F1, K1, 14,  7 );
  R( a, b, c, d, e, F1, K1, 11, 13 );
  R( e, a, b, c, d, F1, K1,  8, 12 );
  R( d, e, a, b, c, F2, K2,  3, 11 );
  R( c, d, e, a, b, F2, K2, 10, 13 );
  R( b, c, d, e, a, F2, K2, 14,  6 );
  R( a, b, c, d, e, F2, K2,  4,  7 );
  R( e, a, b, c, d, F2, K2,  9, 14 );
  R( d, e, a, b, c, F2, K2, 15,  9 );
  R( c, d, e, a, b, F2, K2,  8, 13 );
  R( b, c, d, e, a, F2, K2,  1, 15 );
  R( a, b, c, d, e, F2, K2,  2, 14 );
  R( e, a, b, c, d, F2, K2,  7,  8 );
  R( d, e, a, b, c, F2, K2,  0, 13 );
  R( c, d, e, a, b, F2, K2,  6,  6 );
  R( b, c, d, e, a, F2, K2, 13,  5 );
  R( a, b, c, d, e, F2, K2, 11, 12 );
  R( e, a, b, c, d, F2, K2,  5,  7 );
  R( d, e, a, b, c, F2, K2, 12,  5 );
  R( c, d, e, a, b, F3, K3,  1, 11 );
  R( b, c, d, e, a, F3, K3,  9, 12 );
  R( a, b, c, d, e, F3, K3, 11, 14 );
  R( e, a, b, c, d, F3, K3, 10, 15 );
  R( d, e, a, b, c, F3, K3,  0, 14 );
  R( c, d, e, a, b, F3, K3,  8, 15 );
  R( b, c, d, e, a, F3, K3, 12,  9 );
  R( a, b, c, d, e, F3, K3,  4,  8 );
  R( e, a, b, c, d, F3, K3, 13,  9 );
  R( d, e, a, b, c, F3, K3,  3, 14 );
  R( c, d, e, a, b, F3, K3,  7,  5 );
  R( b, c, d, e, a, F3, K3, 15,  6 );
  R( a, b, c, d, e, F3, K3, 14,  8 );
  R( e, a, b, c, d, F3, K3,  5,  6 );
  R( d, e, a, b, c, F3, K3,  6,  5 );
  R( c, d, e, a, b, F3, K3,  2, 12 );
  R( b, c, d, e, a, F4, K4,  4,  9 );
  R( a, b, c, d, e, F4, K4,  0, 15 );
  R( e, a, b, c, d, F4, K4,  5,  5 );
  R( d, e, a, b, c, F4, K4,  9, 11 );
  R( c, d, e, a, b, F4, K4,  7,  6 );
  R( b, c, d, e, a, F4, K4, 12,  8 );
  R( a, b, c, d, e, F4, K4,  2, 13 );
  R( e, a, b, c, d, F4, K4, 10, 12 );
  R( d, e, a, b, c, F4, K4, 14,  5 );
  R( c, d, e, a, b, F4, K4,  1, 12 );
  R( b, c, d, e, a, F4, K4,  3, 13 );
  R( a, b, c, d, e, F4, K4,  8, 14 );
  R( e, a, b, c, d, F4, K4, 11, 11 );
  R( d, e, a, b, c, F4, K4,  6,  8 );
  R( c, d, e, a, b, F4, K4, 15,  5 );
  R( b, c, d, e, a, F4, K4, 13,  6 );

  aa = a; bb = b; cc = c; dd = d; ee = e;

  /* right lane */
  a = hd->h0;
  b = hd->h1;
  c = hd->h2;
  d = hd->h3;
  e = hd->h4;
  R( a, b, c, d, e, F4, KK0,	5,  8);
  R( e, a, b, c, d, F4, KK0, 14,  9);
  R( d, e, a, b, c, F4, KK0,	7,  9);
  R( c, d, e, a, b, F4, KK0,	0, 11);
  R( b, c, d, e, a, F4, KK0,	9, 13);
  R( a, b, c, d, e, F4, KK0,	2, 15);
  R( e, a, b, c, d, F4, KK0, 11, 15);
  R( d, e, a, b, c, F4, KK0,	4,  5);
  R( c, d, e, a, b, F4, KK0, 13,  7);
  R( b, c, d, e, a, F4, KK0,	6,  7);
  R( a, b, c, d, e, F4, KK0, 15,  8);
  R( e, a, b, c, d, F4, KK0,	8, 11);
  R( d, e, a, b, c, F4, KK0,	1, 14);
  R( c, d, e, a, b, F4, KK0, 10, 14);
  R( b, c, d, e, a, F4, KK0,	3, 12);
  R( a, b, c, d, e, F4, KK0, 12,  6);
  R( e, a, b, c, d, F3, KK1,	6,  9);
  R( d, e, a, b, c, F3, KK1, 11, 13);
  R( c, d, e, a, b, F3, KK1,	3, 15);
  R( b, c, d, e, a, F3, KK1,	7,  7);
  R( a, b, c, d, e, F3, KK1,	0, 12);
  R( e, a, b, c, d, F3, KK1, 13,  8);
  R( d, e, a, b, c, F3, KK1,	5,  9);
  R( c, d, e, a, b, F3, KK1, 10, 11);
  R( b, c, d, e, a, F3, KK1, 14,  7);
  R( a, b, c, d, e, F3, KK1, 15,  7);
  R( e, a, b, c, d, F3, KK1,	8, 12);
  R( d, e, a, b, c, F3, KK1, 12,  7);
  R( c, d, e, a, b, F3, KK1,	4,  6);
  R( b, c, d, e, a, F3, KK1,	9, 15);
  R( a, b, c, d, e, F3, KK1,	1, 13);
  R( e, a, b, c, d, F3, KK1,	2, 11);
  R( d, e, a, b, c, F2, KK2, 15,  9);
  R( c, d, e, a, b, F2, KK2,	5,  7);
  R( b, c, d, e, a, F2, KK2,	1, 15);
  R( a, b, c, d, e, F2, KK2,	3, 11);
  R( e, a, b, c, d, F2, KK2,	7,  8);
  R( d, e, a, b, c, F2, KK2, 14,  6);
  R( c, d, e, a, b, F2, KK2,	6,  6);
  R( b, c, d, e, a, F2, KK2,	9, 14);
  R( a, b, c, d, e, F2, KK2, 11, 12);
  R( e, a, b, c, d, F2, KK2,	8, 13);
  R( d, e, a, b, c, F2, KK2, 12,  5);
  R( c, d, e, a, b, F2, KK2,	2, 14);
  R( b, c, d, e, a, F2, KK2, 10, 13);
  R( a, b, c, d, e, F2, KK2,	0, 13);
  R( e, a, b, c, d, F2, KK2,	4,  7);
  R( d, e, a, b, c, F2, KK2, 13,  5);
  R( c, d, e, a, b, F1, KK3,	8, 15);
  R( b, c, d, e, a, F1, KK3,	6,  5);
  R( a, b, c, d, e, F1, KK3,	4,  8);
  R( e, a, b, c, d, F1, KK3,	1, 11);
  R( d, e, a, b, c, F1, KK3,	3, 14);
  R( c, d, e, a, b, F1, KK3, 11, 14);
  R( b, c, d, e, a, F1, KK3, 15,  6);
  R( a, b, c, d, e, F1, KK3,	0, 14);
  R( e, a, b, c, d, F1, KK3,	5,  6);
  R( d, e, a, b, c, F1, KK3, 12,  9);
  R( c, d, e, a, b, F1, KK3,	2, 12);
  R( b, c, d, e, a, F1, KK3, 13,  9);
  R( a, b, c, d, e, F1, KK3,	9, 12);
  R( e, a, b, c, d, F1, KK3,	7,  5);
  R( d, e, a, b, c, F1, KK3, 10, 15);
  R( c, d, e, a, b, F1, KK3, 14,  8);
  R( b, c, d, e, a, F0, KK4, 12,  8);
  R( a, b, c, d, e, F0, KK4, 15,  5);
  R( e, a, b, c, d, F0, KK4, 10, 12);
  R( d, e, a, b, c, F0, KK4,	4,  9);
  R( c, d, e, a, b, F0, KK4,	1, 12);
  R( b, c, d, e, a, F0, KK4,	5,  5);
  R( a, b, c, d, e, F0, KK4,	8, 14);
  R( e, a, b, c, d, F0, KK4,	7,  6);
  R( d, e, a, b, c, F0, KK4,	6,  8);
  R( c, d, e, a, b, F0, KK4,	2, 13);
  R( b, c, d, e, a, F0, KK4, 13,  6);
  R( a, b, c, d, e, F0, KK4, 14,  5);
  R( e, a, b, c, d, F0, KK4,	0, 15);
  R( d, e, a, b, c, F0, KK4,	3, 13);
  R( c, d, e, a, b, F0, KK4,	9, 11);
  R( b, c, d, e, a, F0, KK4, 11, 11);


  t	   = hd->h1 + d + cc;
  hd->h1 = hd->h2 + e + dd;
  hd->h2 = hd->h3 + a + ee;
  hd->h3 = hd->h4 + b + aa;
  hd->h4 = hd->h0 + c + bb;
  hd->h0 = t;
}


/* Update the message digest with the contents
 * of INBUF with length INLEN.
 */
static void
rmd160_write ( void *context, const void *inbuf_arg, size_t inlen)
{
  const unsigned char *inbuf = inbuf_arg;
  RMD160_CONTEXT *hd = context;

  if( hd->count == 64 )  /* flush the buffer */
    {
      transform( hd, hd->buf );
      _gcry_burn_stack (108+5*sizeof(void*));
      hd->count = 0;
      hd->nblocks++;
    }
  if( !inbuf )
    return;
  if( hd->count ) 
    {
      for( ; inlen && hd->count < 64; inlen-- )
        hd->buf[hd->count++] = *inbuf++;
      rmd160_write( hd, NULL, 0 );
      if( !inlen )
        return;
    }

  while( inlen >= 64 )
    {
      transform( hd, inbuf );
      hd->count = 0;
      hd->nblocks++;
      inlen -= 64;
      inbuf += 64;
    }
  _gcry_burn_stack (108+5*sizeof(void*));
  for( ; inlen && hd->count < 64; inlen-- )
    hd->buf[hd->count++] = *inbuf++;
}

/****************
 * Apply the rmd160 transform function on the buffer which must have
 * a length 64 bytes. Do not use this function together with the
 * other functions, use rmd160_init to initialize internal variables.
 * Returns: 16 bytes in buffer with the mixed contentes of buffer.
 */
void
_gcry_rmd160_mixblock ( RMD160_CONTEXT *hd, void *blockof64byte )
{
  char *p = blockof64byte;

  transform ( hd, blockof64byte );
#define X(a) do { *(u32*)p = hd->h##a ; p += 4; } while(0)
  X(0);
  X(1);
  X(2);
  X(3);
  X(4);
#undef X
}


/* The routine terminates the computation
 */

static void
rmd160_final( void *context )
{
  RMD160_CONTEXT *hd = context;
  u32 t, msb, lsb;
  byte *p;
  
  rmd160_write(hd, NULL, 0); /* flush */;

  t = hd->nblocks;
  /* multiply by 64 to make a byte count */
  lsb = t << 6;
  msb = t >> 26;
  /* add the count */
  t = lsb;
  if( (lsb += hd->count) < t )
    msb++;
  /* multiply by 8 to make a bit count */
  t = lsb;
  lsb <<= 3;
  msb <<= 3;
  msb |= t >> 29;

  if( hd->count < 56 )  /* enough room */
    {
      hd->buf[hd->count++] = 0x80; /* pad */
      while( hd->count < 56 )
        hd->buf[hd->count++] = 0;  /* pad */
    }
  else  /* need one extra block */
    {
      hd->buf[hd->count++] = 0x80; /* pad character */
      while( hd->count < 64 )
        hd->buf[hd->count++] = 0;
      rmd160_write(hd, NULL, 0);  /* flush */;
      memset(hd->buf, 0, 56 ); /* fill next block with zeroes */
    }
  /* append the 64 bit count */
  hd->buf[56] = lsb	   ;
  hd->buf[57] = lsb >>  8;
  hd->buf[58] = lsb >> 16;
  hd->buf[59] = lsb >> 24;
  hd->buf[60] = msb	   ;
  hd->buf[61] = msb >>  8;
  hd->buf[62] = msb >> 16;
  hd->buf[63] = msb >> 24;
  transform( hd, hd->buf );
  _gcry_burn_stack (108+5*sizeof(void*));

  p = hd->buf;
#ifdef WORDS_BIGENDIAN
#define X(a) do { *p++ = hd->h##a	   ; *p++ = hd->h##a >> 8;	\
	          *p++ = hd->h##a >> 16; *p++ = hd->h##a >> 24; } while(0)
#else /* little endian */
#define X(a) do { *(u32*)p = hd->h##a ; p += 4; } while(0)
#endif
  X(0);
  X(1);
  X(2);
  X(3);
  X(4);
#undef X
}

static byte *
rmd160_read( void *context )
{
  RMD160_CONTEXT *hd = context;

  return hd->buf;
}



/****************
 * Shortcut functions which puts the hash value of the supplied buffer
 * into outbuf which must have a size of 20 bytes.
 */
void
_gcry_rmd160_hash_buffer (void *outbuf, const void *buffer, size_t length )
{
  RMD160_CONTEXT hd;

  _gcry_rmd160_init ( &hd );
  rmd160_write ( &hd, buffer, length );
  rmd160_final ( &hd );
  memcpy ( outbuf, hd.buf, 20 );
}

static byte asn[15] = /* Object ID is 1.3.36.3.2.1 */
  { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03,
    0x02, 0x01, 0x05, 0x00, 0x04, 0x14 };

static gcry_md_oid_spec_t oid_spec_rmd160[] =
  {
    /* rsaSignatureWithripemd160 */
    { "1.3.36.3.3.1.2" },
    /* TeleTrust hash algorithm.  */
    { "1.3.36.3.2.1" },
    { NULL }
  };

gcry_md_spec_t _gcry_digest_spec_rmd160 =
  {
    "RIPEMD160", asn, DIM (asn), oid_spec_rmd160, 20,
    _gcry_rmd160_init, rmd160_write, rmd160_final, rmd160_read,
    sizeof (RMD160_CONTEXT)
  };
