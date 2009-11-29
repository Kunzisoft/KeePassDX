/* whirlpool.c - Whirlpool hashing algorithm
 * Copyright (C) 2005 Free Software Foundation, Inc.
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
 *
 */

/* This is an implementation of the Whirlpool hashing algorithm, which
   has been developed by Vincent Rijmen and Paulo S. L. M. Barreto;
   it's homepage is located at:
   http://planeta.terra.com.br/informatica/paulobarreto/WhirlpoolPage.html.

   The S-Boxes and the structure of the main transformation function,
   which implements an optimized version of the algorithm, is taken
   from the reference implementation available from
   http://planeta.terra.com.br/informatica/paulobarreto/whirlpool.zip.  */

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "types.h"
#include "g10lib.h"
#include "memory.h"
#include "cipher.h"

#include "bithelp.h"

/* Size of a whirlpool block (in bytes).  */
#define BLOCK_SIZE 64

/* Number of rounds.  */
#define R 10



/* Types.  */
typedef u64 whirlpool_block_t[BLOCK_SIZE / 8];

typedef struct {
  whirlpool_block_t hash_state;
  unsigned char buffer[BLOCK_SIZE];
  size_t count;
  unsigned char length[32];
} whirlpool_context_t;



/* Macros. */

/* Convert the the buffer BUFFER into a block BLOCK, using I as
   counter.  */
#define buffer_to_block(buffer, block, i) \
  for (i = 0; i < 8; i++) \
    (block)[i] = ((u64) (0 \
                         | (((u64) (buffer)[i * 8 + 0]) << 56) \
                         | (((u64) (buffer)[i * 8 + 1]) << 48) \
                         | (((u64) (buffer)[i * 8 + 2]) << 40) \
                         | (((u64) (buffer)[i * 8 + 3]) << 32) \
                         | (((u64) (buffer)[i * 8 + 4]) << 24) \
                         | (((u64) (buffer)[i * 8 + 5]) << 16) \
                         | (((u64) (buffer)[i * 8 + 6]) <<  8) \
                         | (((u64) (buffer)[i * 8 + 7]) <<  0)));

/* Convert the block BLOCK into a buffer BUFFER, using I as
   counter.  */
#define block_to_buffer(buffer, block, i) \
  for (i = 0; i < 8; i++) \
    { \
      (buffer)[i * 8 + 0] = (block[i] >> 56) & 0xFF; \
      (buffer)[i * 8 + 1] = (block[i] >> 48) & 0xFF; \
      (buffer)[i * 8 + 2] = (block[i] >> 40) & 0xFF; \
      (buffer)[i * 8 + 3] = (block[i] >> 32) & 0xFF; \
      (buffer)[i * 8 + 4] = (block[i] >> 24) & 0xFF; \
      (buffer)[i * 8 + 5] = (block[i] >> 16) & 0xFF; \
      (buffer)[i * 8 + 6] = (block[i] >>  8) & 0xFF; \
      (buffer)[i * 8 + 7] = (block[i] >>  0) & 0xFF; \
    }

/* Copy the block BLOCK_SRC to BLOCK_DST, using I as counter.  */
#define block_copy(block_dst, block_src, i) \
  for (i = 0; i < 8; i++) \
    block_dst[i] = block_src[i];

/* XOR the block BLOCK_SRC into BLOCK_DST, using I as counter.  */
#define block_xor(block_dst, block_src, i) \
  for (i = 0; i < 8; i++) \
    block_dst[i] ^= block_src[i];



/* Round constants.  */
static const u64 rc[R] =
  {
    U64_C (0x1823c6e887b8014f),
    U64_C (0x36a6d2f5796f9152),
    U64_C (0x60bc9b8ea30c7b35),
    U64_C (0x1de0d7c22e4bfe57),
    U64_C (0x157737e59ff04ada),
    U64_C (0x58c9290ab1a06b85),
    U64_C (0xbd5d10f4cb3e0567),
    U64_C (0xe427418ba77d95d8),
    U64_C (0xfbee7c66dd17479e),
    U64_C (0xca2dbf07ad5a8333),
  };
  


/* Main lookup boxes.  */
static const u64 C0[256] =
  {
    U64_C (0x18186018c07830d8), U64_C (0x23238c2305af4626),
    U64_C (0xc6c63fc67ef991b8), U64_C (0xe8e887e8136fcdfb),
    U64_C (0x878726874ca113cb), U64_C (0xb8b8dab8a9626d11),
    U64_C (0x0101040108050209), U64_C (0x4f4f214f426e9e0d),
    U64_C (0x3636d836adee6c9b), U64_C (0xa6a6a2a6590451ff),
    U64_C (0xd2d26fd2debdb90c), U64_C (0xf5f5f3f5fb06f70e),
    U64_C (0x7979f979ef80f296), U64_C (0x6f6fa16f5fcede30),
    U64_C (0x91917e91fcef3f6d), U64_C (0x52525552aa07a4f8),
    U64_C (0x60609d6027fdc047), U64_C (0xbcbccabc89766535),
    U64_C (0x9b9b569baccd2b37), U64_C (0x8e8e028e048c018a),
    U64_C (0xa3a3b6a371155bd2), U64_C (0x0c0c300c603c186c),
    U64_C (0x7b7bf17bff8af684), U64_C (0x3535d435b5e16a80),
    U64_C (0x1d1d741de8693af5), U64_C (0xe0e0a7e05347ddb3),
    U64_C (0xd7d77bd7f6acb321), U64_C (0xc2c22fc25eed999c),
    U64_C (0x2e2eb82e6d965c43), U64_C (0x4b4b314b627a9629),
    U64_C (0xfefedffea321e15d), U64_C (0x575741578216aed5),
    U64_C (0x15155415a8412abd), U64_C (0x7777c1779fb6eee8),
    U64_C (0x3737dc37a5eb6e92), U64_C (0xe5e5b3e57b56d79e),
    U64_C (0x9f9f469f8cd92313), U64_C (0xf0f0e7f0d317fd23),
    U64_C (0x4a4a354a6a7f9420), U64_C (0xdada4fda9e95a944),
    U64_C (0x58587d58fa25b0a2), U64_C (0xc9c903c906ca8fcf),
    U64_C (0x2929a429558d527c), U64_C (0x0a0a280a5022145a),
    U64_C (0xb1b1feb1e14f7f50), U64_C (0xa0a0baa0691a5dc9),
    U64_C (0x6b6bb16b7fdad614), U64_C (0x85852e855cab17d9),
    U64_C (0xbdbdcebd8173673c), U64_C (0x5d5d695dd234ba8f),
    U64_C (0x1010401080502090), U64_C (0xf4f4f7f4f303f507),
    U64_C (0xcbcb0bcb16c08bdd), U64_C (0x3e3ef83eedc67cd3),
    U64_C (0x0505140528110a2d), U64_C (0x676781671fe6ce78),
    U64_C (0xe4e4b7e47353d597), U64_C (0x27279c2725bb4e02),
    U64_C (0x4141194132588273), U64_C (0x8b8b168b2c9d0ba7),
    U64_C (0xa7a7a6a7510153f6), U64_C (0x7d7de97dcf94fab2),
    U64_C (0x95956e95dcfb3749), U64_C (0xd8d847d88e9fad56),
    U64_C (0xfbfbcbfb8b30eb70), U64_C (0xeeee9fee2371c1cd),
    U64_C (0x7c7ced7cc791f8bb), U64_C (0x6666856617e3cc71),
    U64_C (0xdddd53dda68ea77b), U64_C (0x17175c17b84b2eaf),
    U64_C (0x4747014702468e45), U64_C (0x9e9e429e84dc211a),
    U64_C (0xcaca0fca1ec589d4), U64_C (0x2d2db42d75995a58),
    U64_C (0xbfbfc6bf9179632e), U64_C (0x07071c07381b0e3f),
    U64_C (0xadad8ead012347ac), U64_C (0x5a5a755aea2fb4b0),
    U64_C (0x838336836cb51bef), U64_C (0x3333cc3385ff66b6),
    U64_C (0x636391633ff2c65c), U64_C (0x02020802100a0412),
    U64_C (0xaaaa92aa39384993), U64_C (0x7171d971afa8e2de),
    U64_C (0xc8c807c80ecf8dc6), U64_C (0x19196419c87d32d1),
    U64_C (0x494939497270923b), U64_C (0xd9d943d9869aaf5f),
    U64_C (0xf2f2eff2c31df931), U64_C (0xe3e3abe34b48dba8),
    U64_C (0x5b5b715be22ab6b9), U64_C (0x88881a8834920dbc),
    U64_C (0x9a9a529aa4c8293e), U64_C (0x262698262dbe4c0b),
    U64_C (0x3232c8328dfa64bf), U64_C (0xb0b0fab0e94a7d59),
    U64_C (0xe9e983e91b6acff2), U64_C (0x0f0f3c0f78331e77),
    U64_C (0xd5d573d5e6a6b733), U64_C (0x80803a8074ba1df4),
    U64_C (0xbebec2be997c6127), U64_C (0xcdcd13cd26de87eb),
    U64_C (0x3434d034bde46889), U64_C (0x48483d487a759032),
    U64_C (0xffffdbffab24e354), U64_C (0x7a7af57af78ff48d),
    U64_C (0x90907a90f4ea3d64), U64_C (0x5f5f615fc23ebe9d),
    U64_C (0x202080201da0403d), U64_C (0x6868bd6867d5d00f),
    U64_C (0x1a1a681ad07234ca), U64_C (0xaeae82ae192c41b7),
    U64_C (0xb4b4eab4c95e757d), U64_C (0x54544d549a19a8ce),
    U64_C (0x93937693ece53b7f), U64_C (0x222288220daa442f),
    U64_C (0x64648d6407e9c863), U64_C (0xf1f1e3f1db12ff2a),
    U64_C (0x7373d173bfa2e6cc), U64_C (0x12124812905a2482),
    U64_C (0x40401d403a5d807a), U64_C (0x0808200840281048),
    U64_C (0xc3c32bc356e89b95), U64_C (0xecec97ec337bc5df),
    U64_C (0xdbdb4bdb9690ab4d), U64_C (0xa1a1bea1611f5fc0),
    U64_C (0x8d8d0e8d1c830791), U64_C (0x3d3df43df5c97ac8),
    U64_C (0x97976697ccf1335b), U64_C (0x0000000000000000),
    U64_C (0xcfcf1bcf36d483f9), U64_C (0x2b2bac2b4587566e),
    U64_C (0x7676c57697b3ece1), U64_C (0x8282328264b019e6),
    U64_C (0xd6d67fd6fea9b128), U64_C (0x1b1b6c1bd87736c3),
    U64_C (0xb5b5eeb5c15b7774), U64_C (0xafaf86af112943be),
    U64_C (0x6a6ab56a77dfd41d), U64_C (0x50505d50ba0da0ea),
    U64_C (0x45450945124c8a57), U64_C (0xf3f3ebf3cb18fb38),
    U64_C (0x3030c0309df060ad), U64_C (0xefef9bef2b74c3c4),
    U64_C (0x3f3ffc3fe5c37eda), U64_C (0x55554955921caac7),
    U64_C (0xa2a2b2a2791059db), U64_C (0xeaea8fea0365c9e9),
    U64_C (0x656589650fecca6a), U64_C (0xbabad2bab9686903),
    U64_C (0x2f2fbc2f65935e4a), U64_C (0xc0c027c04ee79d8e),
    U64_C (0xdede5fdebe81a160), U64_C (0x1c1c701ce06c38fc),
    U64_C (0xfdfdd3fdbb2ee746), U64_C (0x4d4d294d52649a1f),
    U64_C (0x92927292e4e03976), U64_C (0x7575c9758fbceafa),
    U64_C (0x06061806301e0c36), U64_C (0x8a8a128a249809ae),
    U64_C (0xb2b2f2b2f940794b), U64_C (0xe6e6bfe66359d185),
    U64_C (0x0e0e380e70361c7e), U64_C (0x1f1f7c1ff8633ee7),
    U64_C (0x6262956237f7c455), U64_C (0xd4d477d4eea3b53a),
    U64_C (0xa8a89aa829324d81), U64_C (0x96966296c4f43152),
    U64_C (0xf9f9c3f99b3aef62), U64_C (0xc5c533c566f697a3),
    U64_C (0x2525942535b14a10), U64_C (0x59597959f220b2ab),
    U64_C (0x84842a8454ae15d0), U64_C (0x7272d572b7a7e4c5),
    U64_C (0x3939e439d5dd72ec), U64_C (0x4c4c2d4c5a619816),
    U64_C (0x5e5e655eca3bbc94), U64_C (0x7878fd78e785f09f),
    U64_C (0x3838e038ddd870e5), U64_C (0x8c8c0a8c14860598),
    U64_C (0xd1d163d1c6b2bf17), U64_C (0xa5a5aea5410b57e4),
    U64_C (0xe2e2afe2434dd9a1), U64_C (0x616199612ff8c24e),
    U64_C (0xb3b3f6b3f1457b42), U64_C (0x2121842115a54234),
    U64_C (0x9c9c4a9c94d62508), U64_C (0x1e1e781ef0663cee),
    U64_C (0x4343114322528661), U64_C (0xc7c73bc776fc93b1),
    U64_C (0xfcfcd7fcb32be54f), U64_C (0x0404100420140824),
    U64_C (0x51515951b208a2e3), U64_C (0x99995e99bcc72f25),
    U64_C (0x6d6da96d4fc4da22), U64_C (0x0d0d340d68391a65),
    U64_C (0xfafacffa8335e979), U64_C (0xdfdf5bdfb684a369),
    U64_C (0x7e7ee57ed79bfca9), U64_C (0x242490243db44819),
    U64_C (0x3b3bec3bc5d776fe), U64_C (0xabab96ab313d4b9a),
    U64_C (0xcece1fce3ed181f0), U64_C (0x1111441188552299),
    U64_C (0x8f8f068f0c890383), U64_C (0x4e4e254e4a6b9c04),
    U64_C (0xb7b7e6b7d1517366), U64_C (0xebeb8beb0b60cbe0),
    U64_C (0x3c3cf03cfdcc78c1), U64_C (0x81813e817cbf1ffd),
    U64_C (0x94946a94d4fe3540), U64_C (0xf7f7fbf7eb0cf31c),
    U64_C (0xb9b9deb9a1676f18), U64_C (0x13134c13985f268b),
    U64_C (0x2c2cb02c7d9c5851), U64_C (0xd3d36bd3d6b8bb05),
    U64_C (0xe7e7bbe76b5cd38c), U64_C (0x6e6ea56e57cbdc39),
    U64_C (0xc4c437c46ef395aa), U64_C (0x03030c03180f061b),
    U64_C (0x565645568a13acdc), U64_C (0x44440d441a49885e),
    U64_C (0x7f7fe17fdf9efea0), U64_C (0xa9a99ea921374f88),
    U64_C (0x2a2aa82a4d825467), U64_C (0xbbbbd6bbb16d6b0a),
    U64_C (0xc1c123c146e29f87), U64_C (0x53535153a202a6f1),
    U64_C (0xdcdc57dcae8ba572), U64_C (0x0b0b2c0b58271653),
    U64_C (0x9d9d4e9d9cd32701), U64_C (0x6c6cad6c47c1d82b),
    U64_C (0x3131c43195f562a4), U64_C (0x7474cd7487b9e8f3),
    U64_C (0xf6f6fff6e309f115), U64_C (0x464605460a438c4c),
    U64_C (0xacac8aac092645a5), U64_C (0x89891e893c970fb5),
    U64_C (0x14145014a04428b4), U64_C (0xe1e1a3e15b42dfba),
    U64_C (0x16165816b04e2ca6), U64_C (0x3a3ae83acdd274f7),
    U64_C (0x6969b9696fd0d206), U64_C (0x09092409482d1241),
    U64_C (0x7070dd70a7ade0d7), U64_C (0xb6b6e2b6d954716f),
    U64_C (0xd0d067d0ceb7bd1e), U64_C (0xeded93ed3b7ec7d6),
    U64_C (0xcccc17cc2edb85e2), U64_C (0x424215422a578468),
    U64_C (0x98985a98b4c22d2c), U64_C (0xa4a4aaa4490e55ed),
    U64_C (0x2828a0285d885075), U64_C (0x5c5c6d5cda31b886),
    U64_C (0xf8f8c7f8933fed6b), U64_C (0x8686228644a411c2),
  };

static const u64 C1[256] =
  {
    U64_C (0xd818186018c07830), U64_C (0x2623238c2305af46),
    U64_C (0xb8c6c63fc67ef991), U64_C (0xfbe8e887e8136fcd),
    U64_C (0xcb878726874ca113), U64_C (0x11b8b8dab8a9626d),
    U64_C (0x0901010401080502), U64_C (0x0d4f4f214f426e9e),
    U64_C (0x9b3636d836adee6c), U64_C (0xffa6a6a2a6590451),
    U64_C (0x0cd2d26fd2debdb9), U64_C (0x0ef5f5f3f5fb06f7),
    U64_C (0x967979f979ef80f2), U64_C (0x306f6fa16f5fcede),
    U64_C (0x6d91917e91fcef3f), U64_C (0xf852525552aa07a4),
    U64_C (0x4760609d6027fdc0), U64_C (0x35bcbccabc897665),
    U64_C (0x379b9b569baccd2b), U64_C (0x8a8e8e028e048c01),
    U64_C (0xd2a3a3b6a371155b), U64_C (0x6c0c0c300c603c18),
    U64_C (0x847b7bf17bff8af6), U64_C (0x803535d435b5e16a),
    U64_C (0xf51d1d741de8693a), U64_C (0xb3e0e0a7e05347dd),
    U64_C (0x21d7d77bd7f6acb3), U64_C (0x9cc2c22fc25eed99),
    U64_C (0x432e2eb82e6d965c), U64_C (0x294b4b314b627a96),
    U64_C (0x5dfefedffea321e1), U64_C (0xd5575741578216ae),
    U64_C (0xbd15155415a8412a), U64_C (0xe87777c1779fb6ee),
    U64_C (0x923737dc37a5eb6e), U64_C (0x9ee5e5b3e57b56d7),
    U64_C (0x139f9f469f8cd923), U64_C (0x23f0f0e7f0d317fd),
    U64_C (0x204a4a354a6a7f94), U64_C (0x44dada4fda9e95a9),
    U64_C (0xa258587d58fa25b0), U64_C (0xcfc9c903c906ca8f),
    U64_C (0x7c2929a429558d52), U64_C (0x5a0a0a280a502214),
    U64_C (0x50b1b1feb1e14f7f), U64_C (0xc9a0a0baa0691a5d),
    U64_C (0x146b6bb16b7fdad6), U64_C (0xd985852e855cab17),
    U64_C (0x3cbdbdcebd817367), U64_C (0x8f5d5d695dd234ba),
    U64_C (0x9010104010805020), U64_C (0x07f4f4f7f4f303f5),
    U64_C (0xddcbcb0bcb16c08b), U64_C (0xd33e3ef83eedc67c),
    U64_C (0x2d0505140528110a), U64_C (0x78676781671fe6ce),
    U64_C (0x97e4e4b7e47353d5), U64_C (0x0227279c2725bb4e),
    U64_C (0x7341411941325882), U64_C (0xa78b8b168b2c9d0b),
    U64_C (0xf6a7a7a6a7510153), U64_C (0xb27d7de97dcf94fa),
    U64_C (0x4995956e95dcfb37), U64_C (0x56d8d847d88e9fad),
    U64_C (0x70fbfbcbfb8b30eb), U64_C (0xcdeeee9fee2371c1),
    U64_C (0xbb7c7ced7cc791f8), U64_C (0x716666856617e3cc),
    U64_C (0x7bdddd53dda68ea7), U64_C (0xaf17175c17b84b2e),
    U64_C (0x454747014702468e), U64_C (0x1a9e9e429e84dc21),
    U64_C (0xd4caca0fca1ec589), U64_C (0x582d2db42d75995a),
    U64_C (0x2ebfbfc6bf917963), U64_C (0x3f07071c07381b0e),
    U64_C (0xacadad8ead012347), U64_C (0xb05a5a755aea2fb4),
    U64_C (0xef838336836cb51b), U64_C (0xb63333cc3385ff66),
    U64_C (0x5c636391633ff2c6), U64_C (0x1202020802100a04),
    U64_C (0x93aaaa92aa393849), U64_C (0xde7171d971afa8e2),
    U64_C (0xc6c8c807c80ecf8d), U64_C (0xd119196419c87d32),
    U64_C (0x3b49493949727092), U64_C (0x5fd9d943d9869aaf),
    U64_C (0x31f2f2eff2c31df9), U64_C (0xa8e3e3abe34b48db),
    U64_C (0xb95b5b715be22ab6), U64_C (0xbc88881a8834920d),
    U64_C (0x3e9a9a529aa4c829), U64_C (0x0b262698262dbe4c),
    U64_C (0xbf3232c8328dfa64), U64_C (0x59b0b0fab0e94a7d),
    U64_C (0xf2e9e983e91b6acf), U64_C (0x770f0f3c0f78331e),
    U64_C (0x33d5d573d5e6a6b7), U64_C (0xf480803a8074ba1d),
    U64_C (0x27bebec2be997c61), U64_C (0xebcdcd13cd26de87),
    U64_C (0x893434d034bde468), U64_C (0x3248483d487a7590),
    U64_C (0x54ffffdbffab24e3), U64_C (0x8d7a7af57af78ff4),
    U64_C (0x6490907a90f4ea3d), U64_C (0x9d5f5f615fc23ebe),
    U64_C (0x3d202080201da040), U64_C (0x0f6868bd6867d5d0),
    U64_C (0xca1a1a681ad07234), U64_C (0xb7aeae82ae192c41),
    U64_C (0x7db4b4eab4c95e75), U64_C (0xce54544d549a19a8),
    U64_C (0x7f93937693ece53b), U64_C (0x2f222288220daa44),
    U64_C (0x6364648d6407e9c8), U64_C (0x2af1f1e3f1db12ff),
    U64_C (0xcc7373d173bfa2e6), U64_C (0x8212124812905a24),
    U64_C (0x7a40401d403a5d80), U64_C (0x4808082008402810),
    U64_C (0x95c3c32bc356e89b), U64_C (0xdfecec97ec337bc5),
    U64_C (0x4ddbdb4bdb9690ab), U64_C (0xc0a1a1bea1611f5f),
    U64_C (0x918d8d0e8d1c8307), U64_C (0xc83d3df43df5c97a),
    U64_C (0x5b97976697ccf133), U64_C (0x0000000000000000),
    U64_C (0xf9cfcf1bcf36d483), U64_C (0x6e2b2bac2b458756),
    U64_C (0xe17676c57697b3ec), U64_C (0xe68282328264b019),
    U64_C (0x28d6d67fd6fea9b1), U64_C (0xc31b1b6c1bd87736),
    U64_C (0x74b5b5eeb5c15b77), U64_C (0xbeafaf86af112943),
    U64_C (0x1d6a6ab56a77dfd4), U64_C (0xea50505d50ba0da0),
    U64_C (0x5745450945124c8a), U64_C (0x38f3f3ebf3cb18fb),
    U64_C (0xad3030c0309df060), U64_C (0xc4efef9bef2b74c3),
    U64_C (0xda3f3ffc3fe5c37e), U64_C (0xc755554955921caa),
    U64_C (0xdba2a2b2a2791059), U64_C (0xe9eaea8fea0365c9),
    U64_C (0x6a656589650fecca), U64_C (0x03babad2bab96869),
    U64_C (0x4a2f2fbc2f65935e), U64_C (0x8ec0c027c04ee79d),
    U64_C (0x60dede5fdebe81a1), U64_C (0xfc1c1c701ce06c38),
    U64_C (0x46fdfdd3fdbb2ee7), U64_C (0x1f4d4d294d52649a),
    U64_C (0x7692927292e4e039), U64_C (0xfa7575c9758fbcea),
    U64_C (0x3606061806301e0c), U64_C (0xae8a8a128a249809),
    U64_C (0x4bb2b2f2b2f94079), U64_C (0x85e6e6bfe66359d1),
    U64_C (0x7e0e0e380e70361c), U64_C (0xe71f1f7c1ff8633e),
    U64_C (0x556262956237f7c4), U64_C (0x3ad4d477d4eea3b5),
    U64_C (0x81a8a89aa829324d), U64_C (0x5296966296c4f431),
    U64_C (0x62f9f9c3f99b3aef), U64_C (0xa3c5c533c566f697),
    U64_C (0x102525942535b14a), U64_C (0xab59597959f220b2),
    U64_C (0xd084842a8454ae15), U64_C (0xc57272d572b7a7e4),
    U64_C (0xec3939e439d5dd72), U64_C (0x164c4c2d4c5a6198),
    U64_C (0x945e5e655eca3bbc), U64_C (0x9f7878fd78e785f0),
    U64_C (0xe53838e038ddd870), U64_C (0x988c8c0a8c148605),
    U64_C (0x17d1d163d1c6b2bf), U64_C (0xe4a5a5aea5410b57),
    U64_C (0xa1e2e2afe2434dd9), U64_C (0x4e616199612ff8c2),
    U64_C (0x42b3b3f6b3f1457b), U64_C (0x342121842115a542),
    U64_C (0x089c9c4a9c94d625), U64_C (0xee1e1e781ef0663c),
    U64_C (0x6143431143225286), U64_C (0xb1c7c73bc776fc93),
    U64_C (0x4ffcfcd7fcb32be5), U64_C (0x2404041004201408),
    U64_C (0xe351515951b208a2), U64_C (0x2599995e99bcc72f),
    U64_C (0x226d6da96d4fc4da), U64_C (0x650d0d340d68391a),
    U64_C (0x79fafacffa8335e9), U64_C (0x69dfdf5bdfb684a3),
    U64_C (0xa97e7ee57ed79bfc), U64_C (0x19242490243db448),
    U64_C (0xfe3b3bec3bc5d776), U64_C (0x9aabab96ab313d4b),
    U64_C (0xf0cece1fce3ed181), U64_C (0x9911114411885522),
    U64_C (0x838f8f068f0c8903), U64_C (0x044e4e254e4a6b9c),
    U64_C (0x66b7b7e6b7d15173), U64_C (0xe0ebeb8beb0b60cb),
    U64_C (0xc13c3cf03cfdcc78), U64_C (0xfd81813e817cbf1f),
    U64_C (0x4094946a94d4fe35), U64_C (0x1cf7f7fbf7eb0cf3),
    U64_C (0x18b9b9deb9a1676f), U64_C (0x8b13134c13985f26),
    U64_C (0x512c2cb02c7d9c58), U64_C (0x05d3d36bd3d6b8bb),
    U64_C (0x8ce7e7bbe76b5cd3), U64_C (0x396e6ea56e57cbdc),
    U64_C (0xaac4c437c46ef395), U64_C (0x1b03030c03180f06),
    U64_C (0xdc565645568a13ac), U64_C (0x5e44440d441a4988),
    U64_C (0xa07f7fe17fdf9efe), U64_C (0x88a9a99ea921374f),
    U64_C (0x672a2aa82a4d8254), U64_C (0x0abbbbd6bbb16d6b),
    U64_C (0x87c1c123c146e29f), U64_C (0xf153535153a202a6),
    U64_C (0x72dcdc57dcae8ba5), U64_C (0x530b0b2c0b582716),
    U64_C (0x019d9d4e9d9cd327), U64_C (0x2b6c6cad6c47c1d8),
    U64_C (0xa43131c43195f562), U64_C (0xf37474cd7487b9e8),
    U64_C (0x15f6f6fff6e309f1), U64_C (0x4c464605460a438c),
    U64_C (0xa5acac8aac092645), U64_C (0xb589891e893c970f),
    U64_C (0xb414145014a04428), U64_C (0xbae1e1a3e15b42df),
    U64_C (0xa616165816b04e2c), U64_C (0xf73a3ae83acdd274),
    U64_C (0x066969b9696fd0d2), U64_C (0x4109092409482d12),
    U64_C (0xd77070dd70a7ade0), U64_C (0x6fb6b6e2b6d95471),
    U64_C (0x1ed0d067d0ceb7bd), U64_C (0xd6eded93ed3b7ec7),
    U64_C (0xe2cccc17cc2edb85), U64_C (0x68424215422a5784),
    U64_C (0x2c98985a98b4c22d), U64_C (0xeda4a4aaa4490e55),
    U64_C (0x752828a0285d8850), U64_C (0x865c5c6d5cda31b8),
    U64_C (0x6bf8f8c7f8933fed), U64_C (0xc28686228644a411),
  };

static const u64 C2[256] =
  {
    U64_C (0x30d818186018c078), U64_C (0x462623238c2305af),
    U64_C (0x91b8c6c63fc67ef9), U64_C (0xcdfbe8e887e8136f),
    U64_C (0x13cb878726874ca1), U64_C (0x6d11b8b8dab8a962),
    U64_C (0x0209010104010805), U64_C (0x9e0d4f4f214f426e),
    U64_C (0x6c9b3636d836adee), U64_C (0x51ffa6a6a2a65904),
    U64_C (0xb90cd2d26fd2debd), U64_C (0xf70ef5f5f3f5fb06),
    U64_C (0xf2967979f979ef80), U64_C (0xde306f6fa16f5fce),
    U64_C (0x3f6d91917e91fcef), U64_C (0xa4f852525552aa07),
    U64_C (0xc04760609d6027fd), U64_C (0x6535bcbccabc8976),
    U64_C (0x2b379b9b569baccd), U64_C (0x018a8e8e028e048c),
    U64_C (0x5bd2a3a3b6a37115), U64_C (0x186c0c0c300c603c),
    U64_C (0xf6847b7bf17bff8a), U64_C (0x6a803535d435b5e1),
    U64_C (0x3af51d1d741de869), U64_C (0xddb3e0e0a7e05347),
    U64_C (0xb321d7d77bd7f6ac), U64_C (0x999cc2c22fc25eed),
    U64_C (0x5c432e2eb82e6d96), U64_C (0x96294b4b314b627a),
    U64_C (0xe15dfefedffea321), U64_C (0xaed5575741578216),
    U64_C (0x2abd15155415a841), U64_C (0xeee87777c1779fb6),
    U64_C (0x6e923737dc37a5eb), U64_C (0xd79ee5e5b3e57b56),
    U64_C (0x23139f9f469f8cd9), U64_C (0xfd23f0f0e7f0d317),
    U64_C (0x94204a4a354a6a7f), U64_C (0xa944dada4fda9e95),
    U64_C (0xb0a258587d58fa25), U64_C (0x8fcfc9c903c906ca),
    U64_C (0x527c2929a429558d), U64_C (0x145a0a0a280a5022),
    U64_C (0x7f50b1b1feb1e14f), U64_C (0x5dc9a0a0baa0691a),
    U64_C (0xd6146b6bb16b7fda), U64_C (0x17d985852e855cab),
    U64_C (0x673cbdbdcebd8173), U64_C (0xba8f5d5d695dd234),
    U64_C (0x2090101040108050), U64_C (0xf507f4f4f7f4f303),
    U64_C (0x8bddcbcb0bcb16c0), U64_C (0x7cd33e3ef83eedc6),
    U64_C (0x0a2d050514052811), U64_C (0xce78676781671fe6),
    U64_C (0xd597e4e4b7e47353), U64_C (0x4e0227279c2725bb),
    U64_C (0x8273414119413258), U64_C (0x0ba78b8b168b2c9d),
    U64_C (0x53f6a7a7a6a75101), U64_C (0xfab27d7de97dcf94),
    U64_C (0x374995956e95dcfb), U64_C (0xad56d8d847d88e9f),
    U64_C (0xeb70fbfbcbfb8b30), U64_C (0xc1cdeeee9fee2371),
    U64_C (0xf8bb7c7ced7cc791), U64_C (0xcc716666856617e3),
    U64_C (0xa77bdddd53dda68e), U64_C (0x2eaf17175c17b84b),
    U64_C (0x8e45474701470246), U64_C (0x211a9e9e429e84dc),
    U64_C (0x89d4caca0fca1ec5), U64_C (0x5a582d2db42d7599),
    U64_C (0x632ebfbfc6bf9179), U64_C (0x0e3f07071c07381b),
    U64_C (0x47acadad8ead0123), U64_C (0xb4b05a5a755aea2f),
    U64_C (0x1bef838336836cb5), U64_C (0x66b63333cc3385ff),
    U64_C (0xc65c636391633ff2), U64_C (0x041202020802100a),
    U64_C (0x4993aaaa92aa3938), U64_C (0xe2de7171d971afa8),
    U64_C (0x8dc6c8c807c80ecf), U64_C (0x32d119196419c87d),
    U64_C (0x923b494939497270), U64_C (0xaf5fd9d943d9869a),
    U64_C (0xf931f2f2eff2c31d), U64_C (0xdba8e3e3abe34b48),
    U64_C (0xb6b95b5b715be22a), U64_C (0x0dbc88881a883492),
    U64_C (0x293e9a9a529aa4c8), U64_C (0x4c0b262698262dbe),
    U64_C (0x64bf3232c8328dfa), U64_C (0x7d59b0b0fab0e94a),
    U64_C (0xcff2e9e983e91b6a), U64_C (0x1e770f0f3c0f7833),
    U64_C (0xb733d5d573d5e6a6), U64_C (0x1df480803a8074ba),
    U64_C (0x6127bebec2be997c), U64_C (0x87ebcdcd13cd26de),
    U64_C (0x68893434d034bde4), U64_C (0x903248483d487a75),
    U64_C (0xe354ffffdbffab24), U64_C (0xf48d7a7af57af78f),
    U64_C (0x3d6490907a90f4ea), U64_C (0xbe9d5f5f615fc23e),
    U64_C (0x403d202080201da0), U64_C (0xd00f6868bd6867d5),
    U64_C (0x34ca1a1a681ad072), U64_C (0x41b7aeae82ae192c),
    U64_C (0x757db4b4eab4c95e), U64_C (0xa8ce54544d549a19),
    U64_C (0x3b7f93937693ece5), U64_C (0x442f222288220daa),
    U64_C (0xc86364648d6407e9), U64_C (0xff2af1f1e3f1db12),
    U64_C (0xe6cc7373d173bfa2), U64_C (0x248212124812905a),
    U64_C (0x807a40401d403a5d), U64_C (0x1048080820084028),
    U64_C (0x9b95c3c32bc356e8), U64_C (0xc5dfecec97ec337b),
    U64_C (0xab4ddbdb4bdb9690), U64_C (0x5fc0a1a1bea1611f),
    U64_C (0x07918d8d0e8d1c83), U64_C (0x7ac83d3df43df5c9),
    U64_C (0x335b97976697ccf1), U64_C (0x0000000000000000),
    U64_C (0x83f9cfcf1bcf36d4), U64_C (0x566e2b2bac2b4587),
    U64_C (0xece17676c57697b3), U64_C (0x19e68282328264b0),
    U64_C (0xb128d6d67fd6fea9), U64_C (0x36c31b1b6c1bd877),
    U64_C (0x7774b5b5eeb5c15b), U64_C (0x43beafaf86af1129),
    U64_C (0xd41d6a6ab56a77df), U64_C (0xa0ea50505d50ba0d),
    U64_C (0x8a5745450945124c), U64_C (0xfb38f3f3ebf3cb18),
    U64_C (0x60ad3030c0309df0), U64_C (0xc3c4efef9bef2b74),
    U64_C (0x7eda3f3ffc3fe5c3), U64_C (0xaac755554955921c),
    U64_C (0x59dba2a2b2a27910), U64_C (0xc9e9eaea8fea0365),
    U64_C (0xca6a656589650fec), U64_C (0x6903babad2bab968),
    U64_C (0x5e4a2f2fbc2f6593), U64_C (0x9d8ec0c027c04ee7),
    U64_C (0xa160dede5fdebe81), U64_C (0x38fc1c1c701ce06c),
    U64_C (0xe746fdfdd3fdbb2e), U64_C (0x9a1f4d4d294d5264),
    U64_C (0x397692927292e4e0), U64_C (0xeafa7575c9758fbc),
    U64_C (0x0c3606061806301e), U64_C (0x09ae8a8a128a2498),
    U64_C (0x794bb2b2f2b2f940), U64_C (0xd185e6e6bfe66359),
    U64_C (0x1c7e0e0e380e7036), U64_C (0x3ee71f1f7c1ff863),
    U64_C (0xc4556262956237f7), U64_C (0xb53ad4d477d4eea3),
    U64_C (0x4d81a8a89aa82932), U64_C (0x315296966296c4f4),
    U64_C (0xef62f9f9c3f99b3a), U64_C (0x97a3c5c533c566f6),
    U64_C (0x4a102525942535b1), U64_C (0xb2ab59597959f220),
    U64_C (0x15d084842a8454ae), U64_C (0xe4c57272d572b7a7),
    U64_C (0x72ec3939e439d5dd), U64_C (0x98164c4c2d4c5a61),
    U64_C (0xbc945e5e655eca3b), U64_C (0xf09f7878fd78e785),
    U64_C (0x70e53838e038ddd8), U64_C (0x05988c8c0a8c1486),
    U64_C (0xbf17d1d163d1c6b2), U64_C (0x57e4a5a5aea5410b),
    U64_C (0xd9a1e2e2afe2434d), U64_C (0xc24e616199612ff8),
    U64_C (0x7b42b3b3f6b3f145), U64_C (0x42342121842115a5),
    U64_C (0x25089c9c4a9c94d6), U64_C (0x3cee1e1e781ef066),
    U64_C (0x8661434311432252), U64_C (0x93b1c7c73bc776fc),
    U64_C (0xe54ffcfcd7fcb32b), U64_C (0x0824040410042014),
    U64_C (0xa2e351515951b208), U64_C (0x2f2599995e99bcc7),
    U64_C (0xda226d6da96d4fc4), U64_C (0x1a650d0d340d6839),
    U64_C (0xe979fafacffa8335), U64_C (0xa369dfdf5bdfb684),
    U64_C (0xfca97e7ee57ed79b), U64_C (0x4819242490243db4),
    U64_C (0x76fe3b3bec3bc5d7), U64_C (0x4b9aabab96ab313d),
    U64_C (0x81f0cece1fce3ed1), U64_C (0x2299111144118855),
    U64_C (0x03838f8f068f0c89), U64_C (0x9c044e4e254e4a6b),
    U64_C (0x7366b7b7e6b7d151), U64_C (0xcbe0ebeb8beb0b60),
    U64_C (0x78c13c3cf03cfdcc), U64_C (0x1ffd81813e817cbf),
    U64_C (0x354094946a94d4fe), U64_C (0xf31cf7f7fbf7eb0c),
    U64_C (0x6f18b9b9deb9a167), U64_C (0x268b13134c13985f),
    U64_C (0x58512c2cb02c7d9c), U64_C (0xbb05d3d36bd3d6b8),
    U64_C (0xd38ce7e7bbe76b5c), U64_C (0xdc396e6ea56e57cb),
    U64_C (0x95aac4c437c46ef3), U64_C (0x061b03030c03180f),
    U64_C (0xacdc565645568a13), U64_C (0x885e44440d441a49),
    U64_C (0xfea07f7fe17fdf9e), U64_C (0x4f88a9a99ea92137),
    U64_C (0x54672a2aa82a4d82), U64_C (0x6b0abbbbd6bbb16d),
    U64_C (0x9f87c1c123c146e2), U64_C (0xa6f153535153a202),
    U64_C (0xa572dcdc57dcae8b), U64_C (0x16530b0b2c0b5827),
    U64_C (0x27019d9d4e9d9cd3), U64_C (0xd82b6c6cad6c47c1),
    U64_C (0x62a43131c43195f5), U64_C (0xe8f37474cd7487b9),
    U64_C (0xf115f6f6fff6e309), U64_C (0x8c4c464605460a43),
    U64_C (0x45a5acac8aac0926), U64_C (0x0fb589891e893c97),
    U64_C (0x28b414145014a044), U64_C (0xdfbae1e1a3e15b42),
    U64_C (0x2ca616165816b04e), U64_C (0x74f73a3ae83acdd2),
    U64_C (0xd2066969b9696fd0), U64_C (0x124109092409482d),
    U64_C (0xe0d77070dd70a7ad), U64_C (0x716fb6b6e2b6d954),
    U64_C (0xbd1ed0d067d0ceb7), U64_C (0xc7d6eded93ed3b7e),
    U64_C (0x85e2cccc17cc2edb), U64_C (0x8468424215422a57),
    U64_C (0x2d2c98985a98b4c2), U64_C (0x55eda4a4aaa4490e),
    U64_C (0x50752828a0285d88), U64_C (0xb8865c5c6d5cda31),
    U64_C (0xed6bf8f8c7f8933f), U64_C (0x11c28686228644a4),
  };

static const u64 C3[256] =
  {
    U64_C (0x7830d818186018c0), U64_C (0xaf462623238c2305),
    U64_C (0xf991b8c6c63fc67e), U64_C (0x6fcdfbe8e887e813),
    U64_C (0xa113cb878726874c), U64_C (0x626d11b8b8dab8a9),
    U64_C (0x0502090101040108), U64_C (0x6e9e0d4f4f214f42),
    U64_C (0xee6c9b3636d836ad), U64_C (0x0451ffa6a6a2a659),
    U64_C (0xbdb90cd2d26fd2de), U64_C (0x06f70ef5f5f3f5fb),
    U64_C (0x80f2967979f979ef), U64_C (0xcede306f6fa16f5f),
    U64_C (0xef3f6d91917e91fc), U64_C (0x07a4f852525552aa),
    U64_C (0xfdc04760609d6027), U64_C (0x766535bcbccabc89),
    U64_C (0xcd2b379b9b569bac), U64_C (0x8c018a8e8e028e04),
    U64_C (0x155bd2a3a3b6a371), U64_C (0x3c186c0c0c300c60),
    U64_C (0x8af6847b7bf17bff), U64_C (0xe16a803535d435b5),
    U64_C (0x693af51d1d741de8), U64_C (0x47ddb3e0e0a7e053),
    U64_C (0xacb321d7d77bd7f6), U64_C (0xed999cc2c22fc25e),
    U64_C (0x965c432e2eb82e6d), U64_C (0x7a96294b4b314b62),
    U64_C (0x21e15dfefedffea3), U64_C (0x16aed55757415782),
    U64_C (0x412abd15155415a8), U64_C (0xb6eee87777c1779f),
    U64_C (0xeb6e923737dc37a5), U64_C (0x56d79ee5e5b3e57b),
    U64_C (0xd923139f9f469f8c), U64_C (0x17fd23f0f0e7f0d3),
    U64_C (0x7f94204a4a354a6a), U64_C (0x95a944dada4fda9e),
    U64_C (0x25b0a258587d58fa), U64_C (0xca8fcfc9c903c906),
    U64_C (0x8d527c2929a42955), U64_C (0x22145a0a0a280a50),
    U64_C (0x4f7f50b1b1feb1e1), U64_C (0x1a5dc9a0a0baa069),
    U64_C (0xdad6146b6bb16b7f), U64_C (0xab17d985852e855c),
    U64_C (0x73673cbdbdcebd81), U64_C (0x34ba8f5d5d695dd2),
    U64_C (0x5020901010401080), U64_C (0x03f507f4f4f7f4f3),
    U64_C (0xc08bddcbcb0bcb16), U64_C (0xc67cd33e3ef83eed),
    U64_C (0x110a2d0505140528), U64_C (0xe6ce78676781671f),
    U64_C (0x53d597e4e4b7e473), U64_C (0xbb4e0227279c2725),
    U64_C (0x5882734141194132), U64_C (0x9d0ba78b8b168b2c),
    U64_C (0x0153f6a7a7a6a751), U64_C (0x94fab27d7de97dcf),
    U64_C (0xfb374995956e95dc), U64_C (0x9fad56d8d847d88e),
    U64_C (0x30eb70fbfbcbfb8b), U64_C (0x71c1cdeeee9fee23),
    U64_C (0x91f8bb7c7ced7cc7), U64_C (0xe3cc716666856617),
    U64_C (0x8ea77bdddd53dda6), U64_C (0x4b2eaf17175c17b8),
    U64_C (0x468e454747014702), U64_C (0xdc211a9e9e429e84),
    U64_C (0xc589d4caca0fca1e), U64_C (0x995a582d2db42d75),
    U64_C (0x79632ebfbfc6bf91), U64_C (0x1b0e3f07071c0738),
    U64_C (0x2347acadad8ead01), U64_C (0x2fb4b05a5a755aea),
    U64_C (0xb51bef838336836c), U64_C (0xff66b63333cc3385),
    U64_C (0xf2c65c636391633f), U64_C (0x0a04120202080210),
    U64_C (0x384993aaaa92aa39), U64_C (0xa8e2de7171d971af),
    U64_C (0xcf8dc6c8c807c80e), U64_C (0x7d32d119196419c8),
    U64_C (0x70923b4949394972), U64_C (0x9aaf5fd9d943d986),
    U64_C (0x1df931f2f2eff2c3), U64_C (0x48dba8e3e3abe34b),
    U64_C (0x2ab6b95b5b715be2), U64_C (0x920dbc88881a8834),
    U64_C (0xc8293e9a9a529aa4), U64_C (0xbe4c0b262698262d),
    U64_C (0xfa64bf3232c8328d), U64_C (0x4a7d59b0b0fab0e9),
    U64_C (0x6acff2e9e983e91b), U64_C (0x331e770f0f3c0f78),
    U64_C (0xa6b733d5d573d5e6), U64_C (0xba1df480803a8074),
    U64_C (0x7c6127bebec2be99), U64_C (0xde87ebcdcd13cd26),
    U64_C (0xe468893434d034bd), U64_C (0x75903248483d487a),
    U64_C (0x24e354ffffdbffab), U64_C (0x8ff48d7a7af57af7),
    U64_C (0xea3d6490907a90f4), U64_C (0x3ebe9d5f5f615fc2),
    U64_C (0xa0403d202080201d), U64_C (0xd5d00f6868bd6867),
    U64_C (0x7234ca1a1a681ad0), U64_C (0x2c41b7aeae82ae19),
    U64_C (0x5e757db4b4eab4c9), U64_C (0x19a8ce54544d549a),
    U64_C (0xe53b7f93937693ec), U64_C (0xaa442f222288220d),
    U64_C (0xe9c86364648d6407), U64_C (0x12ff2af1f1e3f1db),
    U64_C (0xa2e6cc7373d173bf), U64_C (0x5a24821212481290),
    U64_C (0x5d807a40401d403a), U64_C (0x2810480808200840),
    U64_C (0xe89b95c3c32bc356), U64_C (0x7bc5dfecec97ec33),
    U64_C (0x90ab4ddbdb4bdb96), U64_C (0x1f5fc0a1a1bea161),
    U64_C (0x8307918d8d0e8d1c), U64_C (0xc97ac83d3df43df5),
    U64_C (0xf1335b97976697cc), U64_C (0x0000000000000000),
    U64_C (0xd483f9cfcf1bcf36), U64_C (0x87566e2b2bac2b45),
    U64_C (0xb3ece17676c57697), U64_C (0xb019e68282328264),
    U64_C (0xa9b128d6d67fd6fe), U64_C (0x7736c31b1b6c1bd8),
    U64_C (0x5b7774b5b5eeb5c1), U64_C (0x2943beafaf86af11),
    U64_C (0xdfd41d6a6ab56a77), U64_C (0x0da0ea50505d50ba),
    U64_C (0x4c8a574545094512), U64_C (0x18fb38f3f3ebf3cb),
    U64_C (0xf060ad3030c0309d), U64_C (0x74c3c4efef9bef2b),
    U64_C (0xc37eda3f3ffc3fe5), U64_C (0x1caac75555495592),
    U64_C (0x1059dba2a2b2a279), U64_C (0x65c9e9eaea8fea03),
    U64_C (0xecca6a656589650f), U64_C (0x686903babad2bab9),
    U64_C (0x935e4a2f2fbc2f65), U64_C (0xe79d8ec0c027c04e),
    U64_C (0x81a160dede5fdebe), U64_C (0x6c38fc1c1c701ce0),
    U64_C (0x2ee746fdfdd3fdbb), U64_C (0x649a1f4d4d294d52),
    U64_C (0xe0397692927292e4), U64_C (0xbceafa7575c9758f),
    U64_C (0x1e0c360606180630), U64_C (0x9809ae8a8a128a24),
    U64_C (0x40794bb2b2f2b2f9), U64_C (0x59d185e6e6bfe663),
    U64_C (0x361c7e0e0e380e70), U64_C (0x633ee71f1f7c1ff8),
    U64_C (0xf7c4556262956237), U64_C (0xa3b53ad4d477d4ee),
    U64_C (0x324d81a8a89aa829), U64_C (0xf4315296966296c4),
    U64_C (0x3aef62f9f9c3f99b), U64_C (0xf697a3c5c533c566),
    U64_C (0xb14a102525942535), U64_C (0x20b2ab59597959f2),
    U64_C (0xae15d084842a8454), U64_C (0xa7e4c57272d572b7),
    U64_C (0xdd72ec3939e439d5), U64_C (0x6198164c4c2d4c5a),
    U64_C (0x3bbc945e5e655eca), U64_C (0x85f09f7878fd78e7),
    U64_C (0xd870e53838e038dd), U64_C (0x8605988c8c0a8c14),
    U64_C (0xb2bf17d1d163d1c6), U64_C (0x0b57e4a5a5aea541),
    U64_C (0x4dd9a1e2e2afe243), U64_C (0xf8c24e616199612f),
    U64_C (0x457b42b3b3f6b3f1), U64_C (0xa542342121842115),
    U64_C (0xd625089c9c4a9c94), U64_C (0x663cee1e1e781ef0),
    U64_C (0x5286614343114322), U64_C (0xfc93b1c7c73bc776),
    U64_C (0x2be54ffcfcd7fcb3), U64_C (0x1408240404100420),
    U64_C (0x08a2e351515951b2), U64_C (0xc72f2599995e99bc),
    U64_C (0xc4da226d6da96d4f), U64_C (0x391a650d0d340d68),
    U64_C (0x35e979fafacffa83), U64_C (0x84a369dfdf5bdfb6),
    U64_C (0x9bfca97e7ee57ed7), U64_C (0xb44819242490243d),
    U64_C (0xd776fe3b3bec3bc5), U64_C (0x3d4b9aabab96ab31),
    U64_C (0xd181f0cece1fce3e), U64_C (0x5522991111441188),
    U64_C (0x8903838f8f068f0c), U64_C (0x6b9c044e4e254e4a),
    U64_C (0x517366b7b7e6b7d1), U64_C (0x60cbe0ebeb8beb0b),
    U64_C (0xcc78c13c3cf03cfd), U64_C (0xbf1ffd81813e817c),
    U64_C (0xfe354094946a94d4), U64_C (0x0cf31cf7f7fbf7eb),
    U64_C (0x676f18b9b9deb9a1), U64_C (0x5f268b13134c1398),
    U64_C (0x9c58512c2cb02c7d), U64_C (0xb8bb05d3d36bd3d6),
    U64_C (0x5cd38ce7e7bbe76b), U64_C (0xcbdc396e6ea56e57),
    U64_C (0xf395aac4c437c46e), U64_C (0x0f061b03030c0318),
    U64_C (0x13acdc565645568a), U64_C (0x49885e44440d441a),
    U64_C (0x9efea07f7fe17fdf), U64_C (0x374f88a9a99ea921),
    U64_C (0x8254672a2aa82a4d), U64_C (0x6d6b0abbbbd6bbb1),
    U64_C (0xe29f87c1c123c146), U64_C (0x02a6f153535153a2),
    U64_C (0x8ba572dcdc57dcae), U64_C (0x2716530b0b2c0b58),
    U64_C (0xd327019d9d4e9d9c), U64_C (0xc1d82b6c6cad6c47),
    U64_C (0xf562a43131c43195), U64_C (0xb9e8f37474cd7487),
    U64_C (0x09f115f6f6fff6e3), U64_C (0x438c4c464605460a),
    U64_C (0x2645a5acac8aac09), U64_C (0x970fb589891e893c),
    U64_C (0x4428b414145014a0), U64_C (0x42dfbae1e1a3e15b),
    U64_C (0x4e2ca616165816b0), U64_C (0xd274f73a3ae83acd),
    U64_C (0xd0d2066969b9696f), U64_C (0x2d12410909240948),
    U64_C (0xade0d77070dd70a7), U64_C (0x54716fb6b6e2b6d9),
    U64_C (0xb7bd1ed0d067d0ce), U64_C (0x7ec7d6eded93ed3b),
    U64_C (0xdb85e2cccc17cc2e), U64_C (0x578468424215422a),
    U64_C (0xc22d2c98985a98b4), U64_C (0x0e55eda4a4aaa449),
    U64_C (0x8850752828a0285d), U64_C (0x31b8865c5c6d5cda),
    U64_C (0x3fed6bf8f8c7f893), U64_C (0xa411c28686228644),
  };

static const u64 C4[256] =
  {
    U64_C (0xc07830d818186018), U64_C (0x05af462623238c23),
    U64_C (0x7ef991b8c6c63fc6), U64_C (0x136fcdfbe8e887e8),
    U64_C (0x4ca113cb87872687), U64_C (0xa9626d11b8b8dab8),
    U64_C (0x0805020901010401), U64_C (0x426e9e0d4f4f214f),
    U64_C (0xadee6c9b3636d836), U64_C (0x590451ffa6a6a2a6),
    U64_C (0xdebdb90cd2d26fd2), U64_C (0xfb06f70ef5f5f3f5),
    U64_C (0xef80f2967979f979), U64_C (0x5fcede306f6fa16f),
    U64_C (0xfcef3f6d91917e91), U64_C (0xaa07a4f852525552),
    U64_C (0x27fdc04760609d60), U64_C (0x89766535bcbccabc),
    U64_C (0xaccd2b379b9b569b), U64_C (0x048c018a8e8e028e),
    U64_C (0x71155bd2a3a3b6a3), U64_C (0x603c186c0c0c300c),
    U64_C (0xff8af6847b7bf17b), U64_C (0xb5e16a803535d435),
    U64_C (0xe8693af51d1d741d), U64_C (0x5347ddb3e0e0a7e0),
    U64_C (0xf6acb321d7d77bd7), U64_C (0x5eed999cc2c22fc2),
    U64_C (0x6d965c432e2eb82e), U64_C (0x627a96294b4b314b),
    U64_C (0xa321e15dfefedffe), U64_C (0x8216aed557574157),
    U64_C (0xa8412abd15155415), U64_C (0x9fb6eee87777c177),
    U64_C (0xa5eb6e923737dc37), U64_C (0x7b56d79ee5e5b3e5),
    U64_C (0x8cd923139f9f469f), U64_C (0xd317fd23f0f0e7f0),
    U64_C (0x6a7f94204a4a354a), U64_C (0x9e95a944dada4fda),
    U64_C (0xfa25b0a258587d58), U64_C (0x06ca8fcfc9c903c9),
    U64_C (0x558d527c2929a429), U64_C (0x5022145a0a0a280a),
    U64_C (0xe14f7f50b1b1feb1), U64_C (0x691a5dc9a0a0baa0),
    U64_C (0x7fdad6146b6bb16b), U64_C (0x5cab17d985852e85),
    U64_C (0x8173673cbdbdcebd), U64_C (0xd234ba8f5d5d695d),
    U64_C (0x8050209010104010), U64_C (0xf303f507f4f4f7f4),
    U64_C (0x16c08bddcbcb0bcb), U64_C (0xedc67cd33e3ef83e),
    U64_C (0x28110a2d05051405), U64_C (0x1fe6ce7867678167),
    U64_C (0x7353d597e4e4b7e4), U64_C (0x25bb4e0227279c27),
    U64_C (0x3258827341411941), U64_C (0x2c9d0ba78b8b168b),
    U64_C (0x510153f6a7a7a6a7), U64_C (0xcf94fab27d7de97d),
    U64_C (0xdcfb374995956e95), U64_C (0x8e9fad56d8d847d8),
    U64_C (0x8b30eb70fbfbcbfb), U64_C (0x2371c1cdeeee9fee),
    U64_C (0xc791f8bb7c7ced7c), U64_C (0x17e3cc7166668566),
    U64_C (0xa68ea77bdddd53dd), U64_C (0xb84b2eaf17175c17),
    U64_C (0x02468e4547470147), U64_C (0x84dc211a9e9e429e),
    U64_C (0x1ec589d4caca0fca), U64_C (0x75995a582d2db42d),
    U64_C (0x9179632ebfbfc6bf), U64_C (0x381b0e3f07071c07),
    U64_C (0x012347acadad8ead), U64_C (0xea2fb4b05a5a755a),
    U64_C (0x6cb51bef83833683), U64_C (0x85ff66b63333cc33),
    U64_C (0x3ff2c65c63639163), U64_C (0x100a041202020802),
    U64_C (0x39384993aaaa92aa), U64_C (0xafa8e2de7171d971),
    U64_C (0x0ecf8dc6c8c807c8), U64_C (0xc87d32d119196419),
    U64_C (0x7270923b49493949), U64_C (0x869aaf5fd9d943d9),
    U64_C (0xc31df931f2f2eff2), U64_C (0x4b48dba8e3e3abe3),
    U64_C (0xe22ab6b95b5b715b), U64_C (0x34920dbc88881a88),
    U64_C (0xa4c8293e9a9a529a), U64_C (0x2dbe4c0b26269826),
    U64_C (0x8dfa64bf3232c832), U64_C (0xe94a7d59b0b0fab0),
    U64_C (0x1b6acff2e9e983e9), U64_C (0x78331e770f0f3c0f),
    U64_C (0xe6a6b733d5d573d5), U64_C (0x74ba1df480803a80),
    U64_C (0x997c6127bebec2be), U64_C (0x26de87ebcdcd13cd),
    U64_C (0xbde468893434d034), U64_C (0x7a75903248483d48),
    U64_C (0xab24e354ffffdbff), U64_C (0xf78ff48d7a7af57a),
    U64_C (0xf4ea3d6490907a90), U64_C (0xc23ebe9d5f5f615f),
    U64_C (0x1da0403d20208020), U64_C (0x67d5d00f6868bd68),
    U64_C (0xd07234ca1a1a681a), U64_C (0x192c41b7aeae82ae),
    U64_C (0xc95e757db4b4eab4), U64_C (0x9a19a8ce54544d54),
    U64_C (0xece53b7f93937693), U64_C (0x0daa442f22228822),
    U64_C (0x07e9c86364648d64), U64_C (0xdb12ff2af1f1e3f1),
    U64_C (0xbfa2e6cc7373d173), U64_C (0x905a248212124812),
    U64_C (0x3a5d807a40401d40), U64_C (0x4028104808082008),
    U64_C (0x56e89b95c3c32bc3), U64_C (0x337bc5dfecec97ec),
    U64_C (0x9690ab4ddbdb4bdb), U64_C (0x611f5fc0a1a1bea1),
    U64_C (0x1c8307918d8d0e8d), U64_C (0xf5c97ac83d3df43d),
    U64_C (0xccf1335b97976697), U64_C (0x0000000000000000),
    U64_C (0x36d483f9cfcf1bcf), U64_C (0x4587566e2b2bac2b),
    U64_C (0x97b3ece17676c576), U64_C (0x64b019e682823282),
    U64_C (0xfea9b128d6d67fd6), U64_C (0xd87736c31b1b6c1b),
    U64_C (0xc15b7774b5b5eeb5), U64_C (0x112943beafaf86af),
    U64_C (0x77dfd41d6a6ab56a), U64_C (0xba0da0ea50505d50),
    U64_C (0x124c8a5745450945), U64_C (0xcb18fb38f3f3ebf3),
    U64_C (0x9df060ad3030c030), U64_C (0x2b74c3c4efef9bef),
    U64_C (0xe5c37eda3f3ffc3f), U64_C (0x921caac755554955),
    U64_C (0x791059dba2a2b2a2), U64_C (0x0365c9e9eaea8fea),
    U64_C (0x0fecca6a65658965), U64_C (0xb9686903babad2ba),
    U64_C (0x65935e4a2f2fbc2f), U64_C (0x4ee79d8ec0c027c0),
    U64_C (0xbe81a160dede5fde), U64_C (0xe06c38fc1c1c701c),
    U64_C (0xbb2ee746fdfdd3fd), U64_C (0x52649a1f4d4d294d),
    U64_C (0xe4e0397692927292), U64_C (0x8fbceafa7575c975),
    U64_C (0x301e0c3606061806), U64_C (0x249809ae8a8a128a),
    U64_C (0xf940794bb2b2f2b2), U64_C (0x6359d185e6e6bfe6),
    U64_C (0x70361c7e0e0e380e), U64_C (0xf8633ee71f1f7c1f),
    U64_C (0x37f7c45562629562), U64_C (0xeea3b53ad4d477d4),
    U64_C (0x29324d81a8a89aa8), U64_C (0xc4f4315296966296),
    U64_C (0x9b3aef62f9f9c3f9), U64_C (0x66f697a3c5c533c5),
    U64_C (0x35b14a1025259425), U64_C (0xf220b2ab59597959),
    U64_C (0x54ae15d084842a84), U64_C (0xb7a7e4c57272d572),
    U64_C (0xd5dd72ec3939e439), U64_C (0x5a6198164c4c2d4c),
    U64_C (0xca3bbc945e5e655e), U64_C (0xe785f09f7878fd78),
    U64_C (0xddd870e53838e038), U64_C (0x148605988c8c0a8c),
    U64_C (0xc6b2bf17d1d163d1), U64_C (0x410b57e4a5a5aea5),
    U64_C (0x434dd9a1e2e2afe2), U64_C (0x2ff8c24e61619961),
    U64_C (0xf1457b42b3b3f6b3), U64_C (0x15a5423421218421),
    U64_C (0x94d625089c9c4a9c), U64_C (0xf0663cee1e1e781e),
    U64_C (0x2252866143431143), U64_C (0x76fc93b1c7c73bc7),
    U64_C (0xb32be54ffcfcd7fc), U64_C (0x2014082404041004),
    U64_C (0xb208a2e351515951), U64_C (0xbcc72f2599995e99),
    U64_C (0x4fc4da226d6da96d), U64_C (0x68391a650d0d340d),
    U64_C (0x8335e979fafacffa), U64_C (0xb684a369dfdf5bdf),
    U64_C (0xd79bfca97e7ee57e), U64_C (0x3db4481924249024),
    U64_C (0xc5d776fe3b3bec3b), U64_C (0x313d4b9aabab96ab),
    U64_C (0x3ed181f0cece1fce), U64_C (0x8855229911114411),
    U64_C (0x0c8903838f8f068f), U64_C (0x4a6b9c044e4e254e),
    U64_C (0xd1517366b7b7e6b7), U64_C (0x0b60cbe0ebeb8beb),
    U64_C (0xfdcc78c13c3cf03c), U64_C (0x7cbf1ffd81813e81),
    U64_C (0xd4fe354094946a94), U64_C (0xeb0cf31cf7f7fbf7),
    U64_C (0xa1676f18b9b9deb9), U64_C (0x985f268b13134c13),
    U64_C (0x7d9c58512c2cb02c), U64_C (0xd6b8bb05d3d36bd3),
    U64_C (0x6b5cd38ce7e7bbe7), U64_C (0x57cbdc396e6ea56e),
    U64_C (0x6ef395aac4c437c4), U64_C (0x180f061b03030c03),
    U64_C (0x8a13acdc56564556), U64_C (0x1a49885e44440d44),
    U64_C (0xdf9efea07f7fe17f), U64_C (0x21374f88a9a99ea9),
    U64_C (0x4d8254672a2aa82a), U64_C (0xb16d6b0abbbbd6bb),
    U64_C (0x46e29f87c1c123c1), U64_C (0xa202a6f153535153),
    U64_C (0xae8ba572dcdc57dc), U64_C (0x582716530b0b2c0b),
    U64_C (0x9cd327019d9d4e9d), U64_C (0x47c1d82b6c6cad6c),
    U64_C (0x95f562a43131c431), U64_C (0x87b9e8f37474cd74),
    U64_C (0xe309f115f6f6fff6), U64_C (0x0a438c4c46460546),
    U64_C (0x092645a5acac8aac), U64_C (0x3c970fb589891e89),
    U64_C (0xa04428b414145014), U64_C (0x5b42dfbae1e1a3e1),
    U64_C (0xb04e2ca616165816), U64_C (0xcdd274f73a3ae83a),
    U64_C (0x6fd0d2066969b969), U64_C (0x482d124109092409),
    U64_C (0xa7ade0d77070dd70), U64_C (0xd954716fb6b6e2b6),
    U64_C (0xceb7bd1ed0d067d0), U64_C (0x3b7ec7d6eded93ed),
    U64_C (0x2edb85e2cccc17cc), U64_C (0x2a57846842421542),
    U64_C (0xb4c22d2c98985a98), U64_C (0x490e55eda4a4aaa4),
    U64_C (0x5d8850752828a028), U64_C (0xda31b8865c5c6d5c),
    U64_C (0x933fed6bf8f8c7f8), U64_C (0x44a411c286862286),
  };

static const u64 C5[256] =
  {
    U64_C (0x18c07830d8181860), U64_C (0x2305af462623238c),
    U64_C (0xc67ef991b8c6c63f), U64_C (0xe8136fcdfbe8e887),
    U64_C (0x874ca113cb878726), U64_C (0xb8a9626d11b8b8da),
    U64_C (0x0108050209010104), U64_C (0x4f426e9e0d4f4f21),
    U64_C (0x36adee6c9b3636d8), U64_C (0xa6590451ffa6a6a2),
    U64_C (0xd2debdb90cd2d26f), U64_C (0xf5fb06f70ef5f5f3),
    U64_C (0x79ef80f2967979f9), U64_C (0x6f5fcede306f6fa1),
    U64_C (0x91fcef3f6d91917e), U64_C (0x52aa07a4f8525255),
    U64_C (0x6027fdc04760609d), U64_C (0xbc89766535bcbcca),
    U64_C (0x9baccd2b379b9b56), U64_C (0x8e048c018a8e8e02),
    U64_C (0xa371155bd2a3a3b6), U64_C (0x0c603c186c0c0c30),
    U64_C (0x7bff8af6847b7bf1), U64_C (0x35b5e16a803535d4),
    U64_C (0x1de8693af51d1d74), U64_C (0xe05347ddb3e0e0a7),
    U64_C (0xd7f6acb321d7d77b), U64_C (0xc25eed999cc2c22f),
    U64_C (0x2e6d965c432e2eb8), U64_C (0x4b627a96294b4b31),
    U64_C (0xfea321e15dfefedf), U64_C (0x578216aed5575741),
    U64_C (0x15a8412abd151554), U64_C (0x779fb6eee87777c1),
    U64_C (0x37a5eb6e923737dc), U64_C (0xe57b56d79ee5e5b3),
    U64_C (0x9f8cd923139f9f46), U64_C (0xf0d317fd23f0f0e7),
    U64_C (0x4a6a7f94204a4a35), U64_C (0xda9e95a944dada4f),
    U64_C (0x58fa25b0a258587d), U64_C (0xc906ca8fcfc9c903),
    U64_C (0x29558d527c2929a4), U64_C (0x0a5022145a0a0a28),
    U64_C (0xb1e14f7f50b1b1fe), U64_C (0xa0691a5dc9a0a0ba),
    U64_C (0x6b7fdad6146b6bb1), U64_C (0x855cab17d985852e),
    U64_C (0xbd8173673cbdbdce), U64_C (0x5dd234ba8f5d5d69),
    U64_C (0x1080502090101040), U64_C (0xf4f303f507f4f4f7),
    U64_C (0xcb16c08bddcbcb0b), U64_C (0x3eedc67cd33e3ef8),
    U64_C (0x0528110a2d050514), U64_C (0x671fe6ce78676781),
    U64_C (0xe47353d597e4e4b7), U64_C (0x2725bb4e0227279c),
    U64_C (0x4132588273414119), U64_C (0x8b2c9d0ba78b8b16),
    U64_C (0xa7510153f6a7a7a6), U64_C (0x7dcf94fab27d7de9),
    U64_C (0x95dcfb374995956e), U64_C (0xd88e9fad56d8d847),
    U64_C (0xfb8b30eb70fbfbcb), U64_C (0xee2371c1cdeeee9f),
    U64_C (0x7cc791f8bb7c7ced), U64_C (0x6617e3cc71666685),
    U64_C (0xdda68ea77bdddd53), U64_C (0x17b84b2eaf17175c),
    U64_C (0x4702468e45474701), U64_C (0x9e84dc211a9e9e42),
    U64_C (0xca1ec589d4caca0f), U64_C (0x2d75995a582d2db4),
    U64_C (0xbf9179632ebfbfc6), U64_C (0x07381b0e3f07071c),
    U64_C (0xad012347acadad8e), U64_C (0x5aea2fb4b05a5a75),
    U64_C (0x836cb51bef838336), U64_C (0x3385ff66b63333cc),
    U64_C (0x633ff2c65c636391), U64_C (0x02100a0412020208),
    U64_C (0xaa39384993aaaa92), U64_C (0x71afa8e2de7171d9),
    U64_C (0xc80ecf8dc6c8c807), U64_C (0x19c87d32d1191964),
    U64_C (0x497270923b494939), U64_C (0xd9869aaf5fd9d943),
    U64_C (0xf2c31df931f2f2ef), U64_C (0xe34b48dba8e3e3ab),
    U64_C (0x5be22ab6b95b5b71), U64_C (0x8834920dbc88881a),
    U64_C (0x9aa4c8293e9a9a52), U64_C (0x262dbe4c0b262698),
    U64_C (0x328dfa64bf3232c8), U64_C (0xb0e94a7d59b0b0fa),
    U64_C (0xe91b6acff2e9e983), U64_C (0x0f78331e770f0f3c),
    U64_C (0xd5e6a6b733d5d573), U64_C (0x8074ba1df480803a),
    U64_C (0xbe997c6127bebec2), U64_C (0xcd26de87ebcdcd13),
    U64_C (0x34bde468893434d0), U64_C (0x487a75903248483d),
    U64_C (0xffab24e354ffffdb), U64_C (0x7af78ff48d7a7af5),
    U64_C (0x90f4ea3d6490907a), U64_C (0x5fc23ebe9d5f5f61),
    U64_C (0x201da0403d202080), U64_C (0x6867d5d00f6868bd),
    U64_C (0x1ad07234ca1a1a68), U64_C (0xae192c41b7aeae82),
    U64_C (0xb4c95e757db4b4ea), U64_C (0x549a19a8ce54544d),
    U64_C (0x93ece53b7f939376), U64_C (0x220daa442f222288),
    U64_C (0x6407e9c86364648d), U64_C (0xf1db12ff2af1f1e3),
    U64_C (0x73bfa2e6cc7373d1), U64_C (0x12905a2482121248),
    U64_C (0x403a5d807a40401d), U64_C (0x0840281048080820),
    U64_C (0xc356e89b95c3c32b), U64_C (0xec337bc5dfecec97),
    U64_C (0xdb9690ab4ddbdb4b), U64_C (0xa1611f5fc0a1a1be),
    U64_C (0x8d1c8307918d8d0e), U64_C (0x3df5c97ac83d3df4),
    U64_C (0x97ccf1335b979766), U64_C (0x0000000000000000),
    U64_C (0xcf36d483f9cfcf1b), U64_C (0x2b4587566e2b2bac),
    U64_C (0x7697b3ece17676c5), U64_C (0x8264b019e6828232),
    U64_C (0xd6fea9b128d6d67f), U64_C (0x1bd87736c31b1b6c),
    U64_C (0xb5c15b7774b5b5ee), U64_C (0xaf112943beafaf86),
    U64_C (0x6a77dfd41d6a6ab5), U64_C (0x50ba0da0ea50505d),
    U64_C (0x45124c8a57454509), U64_C (0xf3cb18fb38f3f3eb),
    U64_C (0x309df060ad3030c0), U64_C (0xef2b74c3c4efef9b),
    U64_C (0x3fe5c37eda3f3ffc), U64_C (0x55921caac7555549),
    U64_C (0xa2791059dba2a2b2), U64_C (0xea0365c9e9eaea8f),
    U64_C (0x650fecca6a656589), U64_C (0xbab9686903babad2),
    U64_C (0x2f65935e4a2f2fbc), U64_C (0xc04ee79d8ec0c027),
    U64_C (0xdebe81a160dede5f), U64_C (0x1ce06c38fc1c1c70),
    U64_C (0xfdbb2ee746fdfdd3), U64_C (0x4d52649a1f4d4d29),
    U64_C (0x92e4e03976929272), U64_C (0x758fbceafa7575c9),
    U64_C (0x06301e0c36060618), U64_C (0x8a249809ae8a8a12),
    U64_C (0xb2f940794bb2b2f2), U64_C (0xe66359d185e6e6bf),
    U64_C (0x0e70361c7e0e0e38), U64_C (0x1ff8633ee71f1f7c),
    U64_C (0x6237f7c455626295), U64_C (0xd4eea3b53ad4d477),
    U64_C (0xa829324d81a8a89a), U64_C (0x96c4f43152969662),
    U64_C (0xf99b3aef62f9f9c3), U64_C (0xc566f697a3c5c533),
    U64_C (0x2535b14a10252594), U64_C (0x59f220b2ab595979),
    U64_C (0x8454ae15d084842a), U64_C (0x72b7a7e4c57272d5),
    U64_C (0x39d5dd72ec3939e4), U64_C (0x4c5a6198164c4c2d),
    U64_C (0x5eca3bbc945e5e65), U64_C (0x78e785f09f7878fd),
    U64_C (0x38ddd870e53838e0), U64_C (0x8c148605988c8c0a),
    U64_C (0xd1c6b2bf17d1d163), U64_C (0xa5410b57e4a5a5ae),
    U64_C (0xe2434dd9a1e2e2af), U64_C (0x612ff8c24e616199),
    U64_C (0xb3f1457b42b3b3f6), U64_C (0x2115a54234212184),
    U64_C (0x9c94d625089c9c4a), U64_C (0x1ef0663cee1e1e78),
    U64_C (0x4322528661434311), U64_C (0xc776fc93b1c7c73b),
    U64_C (0xfcb32be54ffcfcd7), U64_C (0x0420140824040410),
    U64_C (0x51b208a2e3515159), U64_C (0x99bcc72f2599995e),
    U64_C (0x6d4fc4da226d6da9), U64_C (0x0d68391a650d0d34),
    U64_C (0xfa8335e979fafacf), U64_C (0xdfb684a369dfdf5b),
    U64_C (0x7ed79bfca97e7ee5), U64_C (0x243db44819242490),
    U64_C (0x3bc5d776fe3b3bec), U64_C (0xab313d4b9aabab96),
    U64_C (0xce3ed181f0cece1f), U64_C (0x1188552299111144),
    U64_C (0x8f0c8903838f8f06), U64_C (0x4e4a6b9c044e4e25),
    U64_C (0xb7d1517366b7b7e6), U64_C (0xeb0b60cbe0ebeb8b),
    U64_C (0x3cfdcc78c13c3cf0), U64_C (0x817cbf1ffd81813e),
    U64_C (0x94d4fe354094946a), U64_C (0xf7eb0cf31cf7f7fb),
    U64_C (0xb9a1676f18b9b9de), U64_C (0x13985f268b13134c),
    U64_C (0x2c7d9c58512c2cb0), U64_C (0xd3d6b8bb05d3d36b),
    U64_C (0xe76b5cd38ce7e7bb), U64_C (0x6e57cbdc396e6ea5),
    U64_C (0xc46ef395aac4c437), U64_C (0x03180f061b03030c),
    U64_C (0x568a13acdc565645), U64_C (0x441a49885e44440d),
    U64_C (0x7fdf9efea07f7fe1), U64_C (0xa921374f88a9a99e),
    U64_C (0x2a4d8254672a2aa8), U64_C (0xbbb16d6b0abbbbd6),
    U64_C (0xc146e29f87c1c123), U64_C (0x53a202a6f1535351),
    U64_C (0xdcae8ba572dcdc57), U64_C (0x0b582716530b0b2c),
    U64_C (0x9d9cd327019d9d4e), U64_C (0x6c47c1d82b6c6cad),
    U64_C (0x3195f562a43131c4), U64_C (0x7487b9e8f37474cd),
    U64_C (0xf6e309f115f6f6ff), U64_C (0x460a438c4c464605),
    U64_C (0xac092645a5acac8a), U64_C (0x893c970fb589891e),
    U64_C (0x14a04428b4141450), U64_C (0xe15b42dfbae1e1a3),
    U64_C (0x16b04e2ca6161658), U64_C (0x3acdd274f73a3ae8),
    U64_C (0x696fd0d2066969b9), U64_C (0x09482d1241090924),
    U64_C (0x70a7ade0d77070dd), U64_C (0xb6d954716fb6b6e2),
    U64_C (0xd0ceb7bd1ed0d067), U64_C (0xed3b7ec7d6eded93),
    U64_C (0xcc2edb85e2cccc17), U64_C (0x422a578468424215),
    U64_C (0x98b4c22d2c98985a), U64_C (0xa4490e55eda4a4aa),
    U64_C (0x285d8850752828a0), U64_C (0x5cda31b8865c5c6d),
    U64_C (0xf8933fed6bf8f8c7), U64_C (0x8644a411c2868622),
  };

static const u64 C6[256] =
  {
    U64_C (0x6018c07830d81818), U64_C (0x8c2305af46262323),
    U64_C (0x3fc67ef991b8c6c6), U64_C (0x87e8136fcdfbe8e8),
    U64_C (0x26874ca113cb8787), U64_C (0xdab8a9626d11b8b8),
    U64_C (0x0401080502090101), U64_C (0x214f426e9e0d4f4f),
    U64_C (0xd836adee6c9b3636), U64_C (0xa2a6590451ffa6a6),
    U64_C (0x6fd2debdb90cd2d2), U64_C (0xf3f5fb06f70ef5f5),
    U64_C (0xf979ef80f2967979), U64_C (0xa16f5fcede306f6f),
    U64_C (0x7e91fcef3f6d9191), U64_C (0x5552aa07a4f85252),
    U64_C (0x9d6027fdc0476060), U64_C (0xcabc89766535bcbc),
    U64_C (0x569baccd2b379b9b), U64_C (0x028e048c018a8e8e),
    U64_C (0xb6a371155bd2a3a3), U64_C (0x300c603c186c0c0c),
    U64_C (0xf17bff8af6847b7b), U64_C (0xd435b5e16a803535),
    U64_C (0x741de8693af51d1d), U64_C (0xa7e05347ddb3e0e0),
    U64_C (0x7bd7f6acb321d7d7), U64_C (0x2fc25eed999cc2c2),
    U64_C (0xb82e6d965c432e2e), U64_C (0x314b627a96294b4b),
    U64_C (0xdffea321e15dfefe), U64_C (0x41578216aed55757),
    U64_C (0x5415a8412abd1515), U64_C (0xc1779fb6eee87777),
    U64_C (0xdc37a5eb6e923737), U64_C (0xb3e57b56d79ee5e5),
    U64_C (0x469f8cd923139f9f), U64_C (0xe7f0d317fd23f0f0),
    U64_C (0x354a6a7f94204a4a), U64_C (0x4fda9e95a944dada),
    U64_C (0x7d58fa25b0a25858), U64_C (0x03c906ca8fcfc9c9),
    U64_C (0xa429558d527c2929), U64_C (0x280a5022145a0a0a),
    U64_C (0xfeb1e14f7f50b1b1), U64_C (0xbaa0691a5dc9a0a0),
    U64_C (0xb16b7fdad6146b6b), U64_C (0x2e855cab17d98585),
    U64_C (0xcebd8173673cbdbd), U64_C (0x695dd234ba8f5d5d),
    U64_C (0x4010805020901010), U64_C (0xf7f4f303f507f4f4),
    U64_C (0x0bcb16c08bddcbcb), U64_C (0xf83eedc67cd33e3e),
    U64_C (0x140528110a2d0505), U64_C (0x81671fe6ce786767),
    U64_C (0xb7e47353d597e4e4), U64_C (0x9c2725bb4e022727),
    U64_C (0x1941325882734141), U64_C (0x168b2c9d0ba78b8b),
    U64_C (0xa6a7510153f6a7a7), U64_C (0xe97dcf94fab27d7d),
    U64_C (0x6e95dcfb37499595), U64_C (0x47d88e9fad56d8d8),
    U64_C (0xcbfb8b30eb70fbfb), U64_C (0x9fee2371c1cdeeee),
    U64_C (0xed7cc791f8bb7c7c), U64_C (0x856617e3cc716666),
    U64_C (0x53dda68ea77bdddd), U64_C (0x5c17b84b2eaf1717),
    U64_C (0x014702468e454747), U64_C (0x429e84dc211a9e9e),
    U64_C (0x0fca1ec589d4caca), U64_C (0xb42d75995a582d2d),
    U64_C (0xc6bf9179632ebfbf), U64_C (0x1c07381b0e3f0707),
    U64_C (0x8ead012347acadad), U64_C (0x755aea2fb4b05a5a),
    U64_C (0x36836cb51bef8383), U64_C (0xcc3385ff66b63333),
    U64_C (0x91633ff2c65c6363), U64_C (0x0802100a04120202),
    U64_C (0x92aa39384993aaaa), U64_C (0xd971afa8e2de7171),
    U64_C (0x07c80ecf8dc6c8c8), U64_C (0x6419c87d32d11919),
    U64_C (0x39497270923b4949), U64_C (0x43d9869aaf5fd9d9),
    U64_C (0xeff2c31df931f2f2), U64_C (0xabe34b48dba8e3e3),
    U64_C (0x715be22ab6b95b5b), U64_C (0x1a8834920dbc8888),
    U64_C (0x529aa4c8293e9a9a), U64_C (0x98262dbe4c0b2626),
    U64_C (0xc8328dfa64bf3232), U64_C (0xfab0e94a7d59b0b0),
    U64_C (0x83e91b6acff2e9e9), U64_C (0x3c0f78331e770f0f),
    U64_C (0x73d5e6a6b733d5d5), U64_C (0x3a8074ba1df48080),
    U64_C (0xc2be997c6127bebe), U64_C (0x13cd26de87ebcdcd),
    U64_C (0xd034bde468893434), U64_C (0x3d487a7590324848),
    U64_C (0xdbffab24e354ffff), U64_C (0xf57af78ff48d7a7a),
    U64_C (0x7a90f4ea3d649090), U64_C (0x615fc23ebe9d5f5f),
    U64_C (0x80201da0403d2020), U64_C (0xbd6867d5d00f6868),
    U64_C (0x681ad07234ca1a1a), U64_C (0x82ae192c41b7aeae),
    U64_C (0xeab4c95e757db4b4), U64_C (0x4d549a19a8ce5454),
    U64_C (0x7693ece53b7f9393), U64_C (0x88220daa442f2222),
    U64_C (0x8d6407e9c8636464), U64_C (0xe3f1db12ff2af1f1),
    U64_C (0xd173bfa2e6cc7373), U64_C (0x4812905a24821212),
    U64_C (0x1d403a5d807a4040), U64_C (0x2008402810480808),
    U64_C (0x2bc356e89b95c3c3), U64_C (0x97ec337bc5dfecec),
    U64_C (0x4bdb9690ab4ddbdb), U64_C (0xbea1611f5fc0a1a1),
    U64_C (0x0e8d1c8307918d8d), U64_C (0xf43df5c97ac83d3d),
    U64_C (0x6697ccf1335b9797), U64_C (0x0000000000000000),
    U64_C (0x1bcf36d483f9cfcf), U64_C (0xac2b4587566e2b2b),
    U64_C (0xc57697b3ece17676), U64_C (0x328264b019e68282),
    U64_C (0x7fd6fea9b128d6d6), U64_C (0x6c1bd87736c31b1b),
    U64_C (0xeeb5c15b7774b5b5), U64_C (0x86af112943beafaf),
    U64_C (0xb56a77dfd41d6a6a), U64_C (0x5d50ba0da0ea5050),
    U64_C (0x0945124c8a574545), U64_C (0xebf3cb18fb38f3f3),
    U64_C (0xc0309df060ad3030), U64_C (0x9bef2b74c3c4efef),
    U64_C (0xfc3fe5c37eda3f3f), U64_C (0x4955921caac75555),
    U64_C (0xb2a2791059dba2a2), U64_C (0x8fea0365c9e9eaea),
    U64_C (0x89650fecca6a6565), U64_C (0xd2bab9686903baba),
    U64_C (0xbc2f65935e4a2f2f), U64_C (0x27c04ee79d8ec0c0),
    U64_C (0x5fdebe81a160dede), U64_C (0x701ce06c38fc1c1c),
    U64_C (0xd3fdbb2ee746fdfd), U64_C (0x294d52649a1f4d4d),
    U64_C (0x7292e4e039769292), U64_C (0xc9758fbceafa7575),
    U64_C (0x1806301e0c360606), U64_C (0x128a249809ae8a8a),
    U64_C (0xf2b2f940794bb2b2), U64_C (0xbfe66359d185e6e6),
    U64_C (0x380e70361c7e0e0e), U64_C (0x7c1ff8633ee71f1f),
    U64_C (0x956237f7c4556262), U64_C (0x77d4eea3b53ad4d4),
    U64_C (0x9aa829324d81a8a8), U64_C (0x6296c4f431529696),
    U64_C (0xc3f99b3aef62f9f9), U64_C (0x33c566f697a3c5c5),
    U64_C (0x942535b14a102525), U64_C (0x7959f220b2ab5959),
    U64_C (0x2a8454ae15d08484), U64_C (0xd572b7a7e4c57272),
    U64_C (0xe439d5dd72ec3939), U64_C (0x2d4c5a6198164c4c),
    U64_C (0x655eca3bbc945e5e), U64_C (0xfd78e785f09f7878),
    U64_C (0xe038ddd870e53838), U64_C (0x0a8c148605988c8c),
    U64_C (0x63d1c6b2bf17d1d1), U64_C (0xaea5410b57e4a5a5),
    U64_C (0xafe2434dd9a1e2e2), U64_C (0x99612ff8c24e6161),
    U64_C (0xf6b3f1457b42b3b3), U64_C (0x842115a542342121),
    U64_C (0x4a9c94d625089c9c), U64_C (0x781ef0663cee1e1e),
    U64_C (0x1143225286614343), U64_C (0x3bc776fc93b1c7c7),
    U64_C (0xd7fcb32be54ffcfc), U64_C (0x1004201408240404),
    U64_C (0x5951b208a2e35151), U64_C (0x5e99bcc72f259999),
    U64_C (0xa96d4fc4da226d6d), U64_C (0x340d68391a650d0d),
    U64_C (0xcffa8335e979fafa), U64_C (0x5bdfb684a369dfdf),
    U64_C (0xe57ed79bfca97e7e), U64_C (0x90243db448192424),
    U64_C (0xec3bc5d776fe3b3b), U64_C (0x96ab313d4b9aabab),
    U64_C (0x1fce3ed181f0cece), U64_C (0x4411885522991111),
    U64_C (0x068f0c8903838f8f), U64_C (0x254e4a6b9c044e4e),
    U64_C (0xe6b7d1517366b7b7), U64_C (0x8beb0b60cbe0ebeb),
    U64_C (0xf03cfdcc78c13c3c), U64_C (0x3e817cbf1ffd8181),
    U64_C (0x6a94d4fe35409494), U64_C (0xfbf7eb0cf31cf7f7),
    U64_C (0xdeb9a1676f18b9b9), U64_C (0x4c13985f268b1313),
    U64_C (0xb02c7d9c58512c2c), U64_C (0x6bd3d6b8bb05d3d3),
    U64_C (0xbbe76b5cd38ce7e7), U64_C (0xa56e57cbdc396e6e),
    U64_C (0x37c46ef395aac4c4), U64_C (0x0c03180f061b0303),
    U64_C (0x45568a13acdc5656), U64_C (0x0d441a49885e4444),
    U64_C (0xe17fdf9efea07f7f), U64_C (0x9ea921374f88a9a9),
    U64_C (0xa82a4d8254672a2a), U64_C (0xd6bbb16d6b0abbbb),
    U64_C (0x23c146e29f87c1c1), U64_C (0x5153a202a6f15353),
    U64_C (0x57dcae8ba572dcdc), U64_C (0x2c0b582716530b0b),
    U64_C (0x4e9d9cd327019d9d), U64_C (0xad6c47c1d82b6c6c),
    U64_C (0xc43195f562a43131), U64_C (0xcd7487b9e8f37474),
    U64_C (0xfff6e309f115f6f6), U64_C (0x05460a438c4c4646),
    U64_C (0x8aac092645a5acac), U64_C (0x1e893c970fb58989),
    U64_C (0x5014a04428b41414), U64_C (0xa3e15b42dfbae1e1),
    U64_C (0x5816b04e2ca61616), U64_C (0xe83acdd274f73a3a),
    U64_C (0xb9696fd0d2066969), U64_C (0x2409482d12410909),
    U64_C (0xdd70a7ade0d77070), U64_C (0xe2b6d954716fb6b6),
    U64_C (0x67d0ceb7bd1ed0d0), U64_C (0x93ed3b7ec7d6eded),
    U64_C (0x17cc2edb85e2cccc), U64_C (0x15422a5784684242),
    U64_C (0x5a98b4c22d2c9898), U64_C (0xaaa4490e55eda4a4),
    U64_C (0xa0285d8850752828), U64_C (0x6d5cda31b8865c5c),
    U64_C (0xc7f8933fed6bf8f8), U64_C (0x228644a411c28686),
  };

static const u64 C7[256] =
  {
    U64_C (0x186018c07830d818), U64_C (0x238c2305af462623),
    U64_C (0xc63fc67ef991b8c6), U64_C (0xe887e8136fcdfbe8),
    U64_C (0x8726874ca113cb87), U64_C (0xb8dab8a9626d11b8),
    U64_C (0x0104010805020901), U64_C (0x4f214f426e9e0d4f),
    U64_C (0x36d836adee6c9b36), U64_C (0xa6a2a6590451ffa6),
    U64_C (0xd26fd2debdb90cd2), U64_C (0xf5f3f5fb06f70ef5),
    U64_C (0x79f979ef80f29679), U64_C (0x6fa16f5fcede306f),
    U64_C (0x917e91fcef3f6d91), U64_C (0x525552aa07a4f852),
    U64_C (0x609d6027fdc04760), U64_C (0xbccabc89766535bc),
    U64_C (0x9b569baccd2b379b), U64_C (0x8e028e048c018a8e),
    U64_C (0xa3b6a371155bd2a3), U64_C (0x0c300c603c186c0c),
    U64_C (0x7bf17bff8af6847b), U64_C (0x35d435b5e16a8035),
    U64_C (0x1d741de8693af51d), U64_C (0xe0a7e05347ddb3e0),
    U64_C (0xd77bd7f6acb321d7), U64_C (0xc22fc25eed999cc2),
    U64_C (0x2eb82e6d965c432e), U64_C (0x4b314b627a96294b),
    U64_C (0xfedffea321e15dfe), U64_C (0x5741578216aed557),
    U64_C (0x155415a8412abd15), U64_C (0x77c1779fb6eee877),
    U64_C (0x37dc37a5eb6e9237), U64_C (0xe5b3e57b56d79ee5),
    U64_C (0x9f469f8cd923139f), U64_C (0xf0e7f0d317fd23f0),
    U64_C (0x4a354a6a7f94204a), U64_C (0xda4fda9e95a944da),
    U64_C (0x587d58fa25b0a258), U64_C (0xc903c906ca8fcfc9),
    U64_C (0x29a429558d527c29), U64_C (0x0a280a5022145a0a),
    U64_C (0xb1feb1e14f7f50b1), U64_C (0xa0baa0691a5dc9a0),
    U64_C (0x6bb16b7fdad6146b), U64_C (0x852e855cab17d985),
    U64_C (0xbdcebd8173673cbd), U64_C (0x5d695dd234ba8f5d),
    U64_C (0x1040108050209010), U64_C (0xf4f7f4f303f507f4),
    U64_C (0xcb0bcb16c08bddcb), U64_C (0x3ef83eedc67cd33e),
    U64_C (0x05140528110a2d05), U64_C (0x6781671fe6ce7867),
    U64_C (0xe4b7e47353d597e4), U64_C (0x279c2725bb4e0227),
    U64_C (0x4119413258827341), U64_C (0x8b168b2c9d0ba78b),
    U64_C (0xa7a6a7510153f6a7), U64_C (0x7de97dcf94fab27d),
    U64_C (0x956e95dcfb374995), U64_C (0xd847d88e9fad56d8),
    U64_C (0xfbcbfb8b30eb70fb), U64_C (0xee9fee2371c1cdee),
    U64_C (0x7ced7cc791f8bb7c), U64_C (0x66856617e3cc7166),
    U64_C (0xdd53dda68ea77bdd), U64_C (0x175c17b84b2eaf17),
    U64_C (0x47014702468e4547), U64_C (0x9e429e84dc211a9e),
    U64_C (0xca0fca1ec589d4ca), U64_C (0x2db42d75995a582d),
    U64_C (0xbfc6bf9179632ebf), U64_C (0x071c07381b0e3f07),
    U64_C (0xad8ead012347acad), U64_C (0x5a755aea2fb4b05a),
    U64_C (0x8336836cb51bef83), U64_C (0x33cc3385ff66b633),
    U64_C (0x6391633ff2c65c63), U64_C (0x020802100a041202),
    U64_C (0xaa92aa39384993aa), U64_C (0x71d971afa8e2de71),
    U64_C (0xc807c80ecf8dc6c8), U64_C (0x196419c87d32d119),
    U64_C (0x4939497270923b49), U64_C (0xd943d9869aaf5fd9),
    U64_C (0xf2eff2c31df931f2), U64_C (0xe3abe34b48dba8e3),
    U64_C (0x5b715be22ab6b95b), U64_C (0x881a8834920dbc88),
    U64_C (0x9a529aa4c8293e9a), U64_C (0x2698262dbe4c0b26),
    U64_C (0x32c8328dfa64bf32), U64_C (0xb0fab0e94a7d59b0),
    U64_C (0xe983e91b6acff2e9), U64_C (0x0f3c0f78331e770f),
    U64_C (0xd573d5e6a6b733d5), U64_C (0x803a8074ba1df480),
    U64_C (0xbec2be997c6127be), U64_C (0xcd13cd26de87ebcd),
    U64_C (0x34d034bde4688934), U64_C (0x483d487a75903248),
    U64_C (0xffdbffab24e354ff), U64_C (0x7af57af78ff48d7a),
    U64_C (0x907a90f4ea3d6490), U64_C (0x5f615fc23ebe9d5f),
    U64_C (0x2080201da0403d20), U64_C (0x68bd6867d5d00f68),
    U64_C (0x1a681ad07234ca1a), U64_C (0xae82ae192c41b7ae),
    U64_C (0xb4eab4c95e757db4), U64_C (0x544d549a19a8ce54),
    U64_C (0x937693ece53b7f93), U64_C (0x2288220daa442f22),
    U64_C (0x648d6407e9c86364), U64_C (0xf1e3f1db12ff2af1),
    U64_C (0x73d173bfa2e6cc73), U64_C (0x124812905a248212),
    U64_C (0x401d403a5d807a40), U64_C (0x0820084028104808),
    U64_C (0xc32bc356e89b95c3), U64_C (0xec97ec337bc5dfec),
    U64_C (0xdb4bdb9690ab4ddb), U64_C (0xa1bea1611f5fc0a1),
    U64_C (0x8d0e8d1c8307918d), U64_C (0x3df43df5c97ac83d),
    U64_C (0x976697ccf1335b97), U64_C (0x0000000000000000),
    U64_C (0xcf1bcf36d483f9cf), U64_C (0x2bac2b4587566e2b),
    U64_C (0x76c57697b3ece176), U64_C (0x82328264b019e682),
    U64_C (0xd67fd6fea9b128d6), U64_C (0x1b6c1bd87736c31b),
    U64_C (0xb5eeb5c15b7774b5), U64_C (0xaf86af112943beaf),
    U64_C (0x6ab56a77dfd41d6a), U64_C (0x505d50ba0da0ea50),
    U64_C (0x450945124c8a5745), U64_C (0xf3ebf3cb18fb38f3),
    U64_C (0x30c0309df060ad30), U64_C (0xef9bef2b74c3c4ef),
    U64_C (0x3ffc3fe5c37eda3f), U64_C (0x554955921caac755),
    U64_C (0xa2b2a2791059dba2), U64_C (0xea8fea0365c9e9ea),
    U64_C (0x6589650fecca6a65), U64_C (0xbad2bab9686903ba),
    U64_C (0x2fbc2f65935e4a2f), U64_C (0xc027c04ee79d8ec0),
    U64_C (0xde5fdebe81a160de), U64_C (0x1c701ce06c38fc1c),
    U64_C (0xfdd3fdbb2ee746fd), U64_C (0x4d294d52649a1f4d),
    U64_C (0x927292e4e0397692), U64_C (0x75c9758fbceafa75),
    U64_C (0x061806301e0c3606), U64_C (0x8a128a249809ae8a),
    U64_C (0xb2f2b2f940794bb2), U64_C (0xe6bfe66359d185e6),
    U64_C (0x0e380e70361c7e0e), U64_C (0x1f7c1ff8633ee71f),
    U64_C (0x62956237f7c45562), U64_C (0xd477d4eea3b53ad4),
    U64_C (0xa89aa829324d81a8), U64_C (0x966296c4f4315296),
    U64_C (0xf9c3f99b3aef62f9), U64_C (0xc533c566f697a3c5),
    U64_C (0x25942535b14a1025), U64_C (0x597959f220b2ab59),
    U64_C (0x842a8454ae15d084), U64_C (0x72d572b7a7e4c572),
    U64_C (0x39e439d5dd72ec39), U64_C (0x4c2d4c5a6198164c),
    U64_C (0x5e655eca3bbc945e), U64_C (0x78fd78e785f09f78),
    U64_C (0x38e038ddd870e538), U64_C (0x8c0a8c148605988c),
    U64_C (0xd163d1c6b2bf17d1), U64_C (0xa5aea5410b57e4a5),
    U64_C (0xe2afe2434dd9a1e2), U64_C (0x6199612ff8c24e61),
    U64_C (0xb3f6b3f1457b42b3), U64_C (0x21842115a5423421),
    U64_C (0x9c4a9c94d625089c), U64_C (0x1e781ef0663cee1e),
    U64_C (0x4311432252866143), U64_C (0xc73bc776fc93b1c7),
    U64_C (0xfcd7fcb32be54ffc), U64_C (0x0410042014082404),
    U64_C (0x515951b208a2e351), U64_C (0x995e99bcc72f2599),
    U64_C (0x6da96d4fc4da226d), U64_C (0x0d340d68391a650d),
    U64_C (0xfacffa8335e979fa), U64_C (0xdf5bdfb684a369df),
    U64_C (0x7ee57ed79bfca97e), U64_C (0x2490243db4481924),
    U64_C (0x3bec3bc5d776fe3b), U64_C (0xab96ab313d4b9aab),
    U64_C (0xce1fce3ed181f0ce), U64_C (0x1144118855229911),
    U64_C (0x8f068f0c8903838f), U64_C (0x4e254e4a6b9c044e),
    U64_C (0xb7e6b7d1517366b7), U64_C (0xeb8beb0b60cbe0eb),
    U64_C (0x3cf03cfdcc78c13c), U64_C (0x813e817cbf1ffd81),
    U64_C (0x946a94d4fe354094), U64_C (0xf7fbf7eb0cf31cf7),
    U64_C (0xb9deb9a1676f18b9), U64_C (0x134c13985f268b13),
    U64_C (0x2cb02c7d9c58512c), U64_C (0xd36bd3d6b8bb05d3),
    U64_C (0xe7bbe76b5cd38ce7), U64_C (0x6ea56e57cbdc396e),
    U64_C (0xc437c46ef395aac4), U64_C (0x030c03180f061b03),
    U64_C (0x5645568a13acdc56), U64_C (0x440d441a49885e44),
    U64_C (0x7fe17fdf9efea07f), U64_C (0xa99ea921374f88a9),
    U64_C (0x2aa82a4d8254672a), U64_C (0xbbd6bbb16d6b0abb),
    U64_C (0xc123c146e29f87c1), U64_C (0x535153a202a6f153),
    U64_C (0xdc57dcae8ba572dc), U64_C (0x0b2c0b582716530b),
    U64_C (0x9d4e9d9cd327019d), U64_C (0x6cad6c47c1d82b6c),
    U64_C (0x31c43195f562a431), U64_C (0x74cd7487b9e8f374),
    U64_C (0xf6fff6e309f115f6), U64_C (0x4605460a438c4c46),
    U64_C (0xac8aac092645a5ac), U64_C (0x891e893c970fb589),
    U64_C (0x145014a04428b414), U64_C (0xe1a3e15b42dfbae1),
    U64_C (0x165816b04e2ca616), U64_C (0x3ae83acdd274f73a),
    U64_C (0x69b9696fd0d20669), U64_C (0x092409482d124109),
    U64_C (0x70dd70a7ade0d770), U64_C (0xb6e2b6d954716fb6),
    U64_C (0xd067d0ceb7bd1ed0), U64_C (0xed93ed3b7ec7d6ed),
    U64_C (0xcc17cc2edb85e2cc), U64_C (0x4215422a57846842),
    U64_C (0x985a98b4c22d2c98), U64_C (0xa4aaa4490e55eda4),
    U64_C (0x28a0285d88507528), U64_C (0x5c6d5cda31b8865c),
    U64_C (0xf8c7f8933fed6bf8), U64_C (0x86228644a411c286),
  };



static void
whirlpool_init (void *ctx)
{
  whirlpool_context_t *context = ctx;

  memset (context, 0, sizeof (*context));
}


/*
 * Transform block.
 */
static void
whirlpool_transform (whirlpool_context_t *context, const unsigned char *data)
{
  whirlpool_block_t data_block;
  whirlpool_block_t key;
  whirlpool_block_t state;
  whirlpool_block_t block;
  unsigned int r;
  unsigned int i;

  buffer_to_block (data, data_block, i);
  block_copy (key, context->hash_state, i);
  block_copy (state, context->hash_state, i);
  block_xor (state, data_block, i);

  for (r = 0; r < R; r++)
    {
      /* Compute round key K^r.  */

      block[0] = (C0[(key[0] >> 56) & 0xFF] ^ C1[(key[7] >> 48) & 0xFF] ^
		  C2[(key[6] >> 40) & 0xFF] ^ C3[(key[5] >> 32) & 0xFF] ^
		  C4[(key[4] >> 24) & 0xFF] ^ C5[(key[3] >> 16) & 0xFF] ^
		  C6[(key[2] >>  8) & 0xFF] ^ C7[(key[1] >>  0) & 0xFF] ^ rc[r]);
      block[1] = (C0[(key[1] >> 56) & 0xFF] ^ C1[(key[0] >> 48) & 0xFF] ^
		  C2[(key[7] >> 40) & 0xFF] ^ C3[(key[6] >> 32) & 0xFF] ^
		  C4[(key[5] >> 24) & 0xFF] ^ C5[(key[4] >> 16) & 0xFF] ^
		  C6[(key[3] >>  8) & 0xFF] ^ C7[(key[2] >>  0) & 0xFF]);
      block[2] = (C0[(key[2] >> 56) & 0xFF] ^ C1[(key[1] >> 48) & 0xFF] ^
		  C2[(key[0] >> 40) & 0xFF] ^ C3[(key[7] >> 32) & 0xFF] ^
		  C4[(key[6] >> 24) & 0xFF] ^ C5[(key[5] >> 16) & 0xFF] ^
		  C6[(key[4] >>  8) & 0xFF] ^ C7[(key[3] >>  0) & 0xFF]);
      block[3] = (C0[(key[3] >> 56) & 0xFF] ^ C1[(key[2] >> 48) & 0xFF] ^
		  C2[(key[1] >> 40) & 0xFF] ^ C3[(key[0] >> 32) & 0xFF] ^
		  C4[(key[7] >> 24) & 0xFF] ^ C5[(key[6] >> 16) & 0xFF] ^
		  C6[(key[5] >>  8) & 0xFF] ^ C7[(key[4] >>  0) & 0xFF]);
      block[4] = (C0[(key[4] >> 56) & 0xFF] ^ C1[(key[3] >> 48) & 0xFF] ^
		  C2[(key[2] >> 40) & 0xFF] ^ C3[(key[1] >> 32) & 0xFF] ^
		  C4[(key[0] >> 24) & 0xFF] ^ C5[(key[7] >> 16) & 0xFF] ^
		  C6[(key[6] >>  8) & 0xFF] ^ C7[(key[5] >>  0) & 0xFF]);
      block[5] = (C0[(key[5] >> 56) & 0xFF] ^ C1[(key[4] >> 48) & 0xFF] ^
		  C2[(key[3] >> 40) & 0xFF] ^ C3[(key[2] >> 32) & 0xFF] ^
		  C4[(key[1] >> 24) & 0xFF] ^ C5[(key[0] >> 16) & 0xFF] ^
		  C6[(key[7] >>  8) & 0xFF] ^ C7[(key[6] >>  0) & 0xFF]);
      block[6] = (C0[(key[6] >> 56) & 0xFF] ^ C1[(key[5] >> 48) & 0xFF] ^
		  C2[(key[4] >> 40) & 0xFF] ^ C3[(key[3] >> 32) & 0xFF] ^
		  C4[(key[2] >> 24) & 0xFF] ^ C5[(key[1] >> 16) & 0xFF] ^
		  C6[(key[0] >>  8) & 0xFF] ^ C7[(key[7] >>  0) & 0xFF]);
      block[7] = (C0[(key[7] >> 56) & 0xFF] ^ C1[(key[6] >> 48) & 0xFF] ^
		  C2[(key[5] >> 40) & 0xFF] ^ C3[(key[4] >> 32) & 0xFF] ^
		  C4[(key[3] >> 24) & 0xFF] ^ C5[(key[2] >> 16) & 0xFF] ^
		  C6[(key[1] >>  8) & 0xFF] ^ C7[(key[0] >>  0) & 0xFF]);
      block_copy (key, block, i);

      /* Apply r-th round transformation.  */

      block[0] = (C0[(state[0] >> 56) & 0xFF] ^ C1[(state[7] >> 48) & 0xFF] ^
		  C2[(state[6] >> 40) & 0xFF] ^ C3[(state[5] >> 32) & 0xFF] ^
		  C4[(state[4] >> 24) & 0xFF] ^ C5[(state[3] >> 16) & 0xFF] ^
		  C6[(state[2] >>  8) & 0xFF] ^ C7[(state[1] >>  0) & 0xFF] ^ key[0]);
      block[1] = (C0[(state[1] >> 56) & 0xFF] ^ C1[(state[0] >> 48) & 0xFF] ^
		  C2[(state[7] >> 40) & 0xFF] ^ C3[(state[6] >> 32) & 0xFF] ^
		  C4[(state[5] >> 24) & 0xFF] ^ C5[(state[4] >> 16) & 0xFF] ^
		  C6[(state[3] >>  8) & 0xFF] ^ C7[(state[2] >>  0) & 0xFF] ^ key[1]);
      block[2] = (C0[(state[2] >> 56) & 0xFF] ^ C1[(state[1] >> 48) & 0xFF] ^
		  C2[(state[0] >> 40) & 0xFF] ^ C3[(state[7] >> 32) & 0xFF] ^
		  C4[(state[6] >> 24) & 0xFF] ^ C5[(state[5] >> 16) & 0xFF] ^
		  C6[(state[4] >>  8) & 0xFF] ^ C7[(state[3] >>  0) & 0xFF] ^ key[2]);
      block[3] = (C0[(state[3] >> 56) & 0xFF] ^ C1[(state[2] >> 48) & 0xFF] ^
		  C2[(state[1] >> 40) & 0xFF] ^ C3[(state[0] >> 32) & 0xFF] ^
		  C4[(state[7] >> 24) & 0xFF] ^ C5[(state[6] >> 16) & 0xFF] ^
		  C6[(state[5] >>  8) & 0xFF] ^ C7[(state[4] >>  0) & 0xFF] ^ key[3]);
      block[4] = (C0[(state[4] >> 56) & 0xFF] ^ C1[(state[3] >> 48) & 0xFF] ^
		  C2[(state[2] >> 40) & 0xFF] ^ C3[(state[1] >> 32) & 0xFF] ^
		  C4[(state[0] >> 24) & 0xFF] ^ C5[(state[7] >> 16) & 0xFF] ^
		  C6[(state[6] >>  8) & 0xFF] ^ C7[(state[5] >>  0) & 0xFF] ^ key[4]);
      block[5] = (C0[(state[5] >> 56) & 0xFF] ^ C1[(state[4] >> 48) & 0xFF] ^
		  C2[(state[3] >> 40) & 0xFF] ^ C3[(state[2] >> 32) & 0xFF] ^
		  C4[(state[1] >> 24) & 0xFF] ^ C5[(state[0] >> 16) & 0xFF] ^
		  C6[(state[7] >>  8) & 0xFF] ^ C7[(state[6] >>  0) & 0xFF] ^ key[5]);
      block[6] = (C0[(state[6] >> 56) & 0xFF] ^ C1[(state[5] >> 48) & 0xFF] ^
		  C2[(state[4] >> 40) & 0xFF] ^ C3[(state[3] >> 32) & 0xFF] ^
		  C4[(state[2] >> 24) & 0xFF] ^ C5[(state[1] >> 16) & 0xFF] ^
		  C6[(state[0] >>  8) & 0xFF] ^ C7[(state[7] >>  0) & 0xFF] ^ key[6]);
      block[7] = (C0[(state[7] >> 56) & 0xFF] ^ C1[(state[6] >> 48) & 0xFF] ^
		  C2[(state[5] >> 40) & 0xFF] ^ C3[(state[4] >> 32) & 0xFF] ^
		  C4[(state[3] >> 24) & 0xFF] ^ C5[(state[2] >> 16) & 0xFF] ^
		  C6[(state[1] >>  8) & 0xFF] ^ C7[(state[0] >>  0) & 0xFF] ^ key[7]);
      block_copy (state, block, i);
    }

  /* Compression.  */

  block_xor (context->hash_state, data_block, i);
  block_xor (context->hash_state, state, i);
}

static void
whirlpool_add (whirlpool_context_t *context,
	       const void *buffer_arg, size_t buffer_n)
{
  const unsigned char *buffer = buffer_arg;
  u64 buffer_size;
  unsigned int carry;
  unsigned int i;

  buffer_size = buffer_n;

  if (context->count == BLOCK_SIZE)
    {
      /* Flush the buffer.  */
      whirlpool_transform (context, context->buffer);
      /*_gcry_burn_stack (80+6*sizeof(void*));*/ /* FIXME */
      context->count = 0;
    }
  if (! buffer)
    return; /* Nothing to add.  */

  if (context->count)
    {
      while (buffer_n && (context->count < BLOCK_SIZE))
	{
	  context->buffer[context->count++] = *buffer++;
	  buffer_n--;
	}
      whirlpool_add (context, NULL, 0);
      if (!buffer_n)
	/* Done.  */
        return;
    }
  /*_gcry_burn_stack (80+6*sizeof(void*));*/ /* FIXME */

  while (buffer_n >= BLOCK_SIZE) 
    {
      whirlpool_transform (context, buffer);
      context->count = 0;
      buffer_n -= BLOCK_SIZE;
      buffer += BLOCK_SIZE;
    }
  while (buffer_n && (context->count < BLOCK_SIZE))
    {
      context->buffer[context->count++] = *buffer++;
      buffer_n--;
    }

  /* Update bit counter.  */
  carry = 0;
  buffer_size <<= 3;
  for (i = 1; i <= 32; i++)
    {
      if (! (buffer_size || carry))
	break;

      carry += context->length[32 - i] + (buffer_size & 0xFF);
      context->length[32 - i] = carry;
      buffer_size >>= 8;
      carry >>= 8;
    }
  gcry_assert (! (buffer_size || carry));
}

static void
whirlpool_write (void *ctx, const void *buffer, size_t buffer_n)
{
  whirlpool_context_t *context = ctx;

  whirlpool_add (context, buffer, buffer_n);
}

static void
whirlpool_final (void *ctx)
{
  whirlpool_context_t *context = ctx;
  unsigned int i;

  /* Flush.  */
  whirlpool_add (context, NULL, 0);

  /* Pad.  */
  context->buffer[context->count++] = 0x80;

  if (context->count > 32)
    {
      /* An extra block is necessary.  */
      while (context->count < 64)
	context->buffer[context->count++] = 0;
      whirlpool_add (context, NULL, 0);
    }
  while (context->count < 32)
    context->buffer[context->count++] = 0;

  /* Add length of message.  */
  memcpy (context->buffer + context->count, context->length, 32);
  context->count += 32;
  whirlpool_add (context, NULL, 0);

  block_to_buffer (context->buffer, context->hash_state, i);
}

static byte *
whirlpool_read (void *ctx)
{
  whirlpool_context_t *context = ctx;

  return context->buffer;
}

gcry_md_spec_t _gcry_digest_spec_whirlpool =
  {
    "WHIRLPOOL", NULL, 0, NULL, 64,
    whirlpool_init, whirlpool_write, whirlpool_final, whirlpool_read,
    sizeof (whirlpool_context_t)
  };
