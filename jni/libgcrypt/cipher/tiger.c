/* tiger.c  -  The TIGER hash function
 *	Copyright (C) 1998, 2001, 2002, 2003 Free Software Foundation, Inc.
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
#include "cipher.h"

#ifdef HAVE_U64_TYPEDEF

/* we really need it here, but as this is only experiment we
 * can live without Tiger */

typedef struct {
    u64  a, b, c;
    byte buf[64];
    int  count;
    u32  nblocks;
} TIGER_CONTEXT;


/*********************************
 * Okay, okay, this is not the fastest code - improvements are welcome.
 *
 */

/* Some test vectors:
 * ""                   24F0130C63AC9332 16166E76B1BB925F F373DE2D49584E7A
 * "abc"                F258C1E88414AB2A 527AB541FFC5B8BF 935F7B951C132951
 * "Tiger"              9F00F599072300DD 276ABB38C8EB6DEC 37790C116F9D2BDF
 * "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-"
 *			87FB2A9083851CF7 470D2CF810E6DF9E B586445034A5A386
 * "ABCDEFGHIJKLMNOPQRSTUVWXYZ=abcdefghijklmnopqrstuvwxyz+0123456789"
 *			467DB80863EBCE48 8DF1CD1261655DE9 57896565975F9197
 * "Tiger - A Fast New Hash Function, by Ross Anderson and Eli Biham"
 *			0C410A042968868A 1671DA5A3FD29A72 5EC1E457D3CDB303
 * "Tiger - A Fast New Hash Function, by Ross Anderson and Eli Biham, proc"
 * "eedings of Fast Software Encryption 3, Cambridge."
 *			EBF591D5AFA655CE 7F22894FF87F54AC 89C811B6B0DA3193
 * "Tiger - A Fast New Hash Function, by Ross Anderson and Eli Biham, proc"
 * "eedings of Fast Software Encryption 3, Cambridge, 1996."
 *			3D9AEB03D1BD1A63 57B2774DFD6D5B24 DD68151D503974FC
 * "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-ABCDEF"
 * "GHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-"
 *			00B83EB4E53440C5 76AC6AAEE0A74858 25FD15E70A59FFE4
 */

static u64 sbox1[256] = {
  U64_C(0x02aab17cf7e90c5e) /*    0 */, U64_C(0xac424b03e243a8ec) /*    1 */,
  U64_C(0x72cd5be30dd5fcd3) /*    2 */, U64_C(0x6d019b93f6f97f3a) /*    3 */,
  U64_C(0xcd9978ffd21f9193) /*    4 */, U64_C(0x7573a1c9708029e2) /*    5 */,
  U64_C(0xb164326b922a83c3) /*    6 */, U64_C(0x46883eee04915870) /*    7 */,
  U64_C(0xeaace3057103ece6) /*    8 */, U64_C(0xc54169b808a3535c) /*    9 */,
  U64_C(0x4ce754918ddec47c) /*   10 */, U64_C(0x0aa2f4dfdc0df40c) /*   11 */,
  U64_C(0x10b76f18a74dbefa) /*   12 */, U64_C(0xc6ccb6235ad1ab6a) /*   13 */,
  U64_C(0x13726121572fe2ff) /*   14 */, U64_C(0x1a488c6f199d921e) /*   15 */,
  U64_C(0x4bc9f9f4da0007ca) /*   16 */, U64_C(0x26f5e6f6e85241c7) /*   17 */,
  U64_C(0x859079dbea5947b6) /*   18 */, U64_C(0x4f1885c5c99e8c92) /*   19 */,
  U64_C(0xd78e761ea96f864b) /*   20 */, U64_C(0x8e36428c52b5c17d) /*   21 */,
  U64_C(0x69cf6827373063c1) /*   22 */, U64_C(0xb607c93d9bb4c56e) /*   23 */,
  U64_C(0x7d820e760e76b5ea) /*   24 */, U64_C(0x645c9cc6f07fdc42) /*   25 */,
  U64_C(0xbf38a078243342e0) /*   26 */, U64_C(0x5f6b343c9d2e7d04) /*   27 */,
  U64_C(0xf2c28aeb600b0ec6) /*   28 */, U64_C(0x6c0ed85f7254bcac) /*   29 */,
  U64_C(0x71592281a4db4fe5) /*   30 */, U64_C(0x1967fa69ce0fed9f) /*   31 */,
  U64_C(0xfd5293f8b96545db) /*   32 */, U64_C(0xc879e9d7f2a7600b) /*   33 */,
  U64_C(0x860248920193194e) /*   34 */, U64_C(0xa4f9533b2d9cc0b3) /*   35 */,
  U64_C(0x9053836c15957613) /*   36 */, U64_C(0xdb6dcf8afc357bf1) /*   37 */,
  U64_C(0x18beea7a7a370f57) /*   38 */, U64_C(0x037117ca50b99066) /*   39 */,
  U64_C(0x6ab30a9774424a35) /*   40 */, U64_C(0xf4e92f02e325249b) /*   41 */,
  U64_C(0x7739db07061ccae1) /*   42 */, U64_C(0xd8f3b49ceca42a05) /*   43 */,
  U64_C(0xbd56be3f51382f73) /*   44 */, U64_C(0x45faed5843b0bb28) /*   45 */,
  U64_C(0x1c813d5c11bf1f83) /*   46 */, U64_C(0x8af0e4b6d75fa169) /*   47 */,
  U64_C(0x33ee18a487ad9999) /*   48 */, U64_C(0x3c26e8eab1c94410) /*   49 */,
  U64_C(0xb510102bc0a822f9) /*   50 */, U64_C(0x141eef310ce6123b) /*   51 */,
  U64_C(0xfc65b90059ddb154) /*   52 */, U64_C(0xe0158640c5e0e607) /*   53 */,
  U64_C(0x884e079826c3a3cf) /*   54 */, U64_C(0x930d0d9523c535fd) /*   55 */,
  U64_C(0x35638d754e9a2b00) /*   56 */, U64_C(0x4085fccf40469dd5) /*   57 */,
  U64_C(0xc4b17ad28be23a4c) /*   58 */, U64_C(0xcab2f0fc6a3e6a2e) /*   59 */,
  U64_C(0x2860971a6b943fcd) /*   60 */, U64_C(0x3dde6ee212e30446) /*   61 */,
  U64_C(0x6222f32ae01765ae) /*   62 */, U64_C(0x5d550bb5478308fe) /*   63 */,
  U64_C(0xa9efa98da0eda22a) /*   64 */, U64_C(0xc351a71686c40da7) /*   65 */,
  U64_C(0x1105586d9c867c84) /*   66 */, U64_C(0xdcffee85fda22853) /*   67 */,
  U64_C(0xccfbd0262c5eef76) /*   68 */, U64_C(0xbaf294cb8990d201) /*   69 */,
  U64_C(0xe69464f52afad975) /*   70 */, U64_C(0x94b013afdf133e14) /*   71 */,
  U64_C(0x06a7d1a32823c958) /*   72 */, U64_C(0x6f95fe5130f61119) /*   73 */,
  U64_C(0xd92ab34e462c06c0) /*   74 */, U64_C(0xed7bde33887c71d2) /*   75 */,
  U64_C(0x79746d6e6518393e) /*   76 */, U64_C(0x5ba419385d713329) /*   77 */,
  U64_C(0x7c1ba6b948a97564) /*   78 */, U64_C(0x31987c197bfdac67) /*   79 */,
  U64_C(0xde6c23c44b053d02) /*   80 */, U64_C(0x581c49fed002d64d) /*   81 */,
  U64_C(0xdd474d6338261571) /*   82 */, U64_C(0xaa4546c3e473d062) /*   83 */,
  U64_C(0x928fce349455f860) /*   84 */, U64_C(0x48161bbacaab94d9) /*   85 */,
  U64_C(0x63912430770e6f68) /*   86 */, U64_C(0x6ec8a5e602c6641c) /*   87 */,
  U64_C(0x87282515337ddd2b) /*   88 */, U64_C(0x2cda6b42034b701b) /*   89 */,
  U64_C(0xb03d37c181cb096d) /*   90 */, U64_C(0xe108438266c71c6f) /*   91 */,
  U64_C(0x2b3180c7eb51b255) /*   92 */, U64_C(0xdf92b82f96c08bbc) /*   93 */,
  U64_C(0x5c68c8c0a632f3ba) /*   94 */, U64_C(0x5504cc861c3d0556) /*   95 */,
  U64_C(0xabbfa4e55fb26b8f) /*   96 */, U64_C(0x41848b0ab3baceb4) /*   97 */,
  U64_C(0xb334a273aa445d32) /*   98 */, U64_C(0xbca696f0a85ad881) /*   99 */,
  U64_C(0x24f6ec65b528d56c) /*  100 */, U64_C(0x0ce1512e90f4524a) /*  101 */,
  U64_C(0x4e9dd79d5506d35a) /*  102 */, U64_C(0x258905fac6ce9779) /*  103 */,
  U64_C(0x2019295b3e109b33) /*  104 */, U64_C(0xf8a9478b73a054cc) /*  105 */,
  U64_C(0x2924f2f934417eb0) /*  106 */, U64_C(0x3993357d536d1bc4) /*  107 */,
  U64_C(0x38a81ac21db6ff8b) /*  108 */, U64_C(0x47c4fbf17d6016bf) /*  109 */,
  U64_C(0x1e0faadd7667e3f5) /*  110 */, U64_C(0x7abcff62938beb96) /*  111 */,
  U64_C(0xa78dad948fc179c9) /*  112 */, U64_C(0x8f1f98b72911e50d) /*  113 */,
  U64_C(0x61e48eae27121a91) /*  114 */, U64_C(0x4d62f7ad31859808) /*  115 */,
  U64_C(0xeceba345ef5ceaeb) /*  116 */, U64_C(0xf5ceb25ebc9684ce) /*  117 */,
  U64_C(0xf633e20cb7f76221) /*  118 */, U64_C(0xa32cdf06ab8293e4) /*  119 */,
  U64_C(0x985a202ca5ee2ca4) /*  120 */, U64_C(0xcf0b8447cc8a8fb1) /*  121 */,
  U64_C(0x9f765244979859a3) /*  122 */, U64_C(0xa8d516b1a1240017) /*  123 */,
  U64_C(0x0bd7ba3ebb5dc726) /*  124 */, U64_C(0xe54bca55b86adb39) /*  125 */,
  U64_C(0x1d7a3afd6c478063) /*  126 */, U64_C(0x519ec608e7669edd) /*  127 */,
  U64_C(0x0e5715a2d149aa23) /*  128 */, U64_C(0x177d4571848ff194) /*  129 */,
  U64_C(0xeeb55f3241014c22) /*  130 */, U64_C(0x0f5e5ca13a6e2ec2) /*  131 */,
  U64_C(0x8029927b75f5c361) /*  132 */, U64_C(0xad139fabc3d6e436) /*  133 */,
  U64_C(0x0d5df1a94ccf402f) /*  134 */, U64_C(0x3e8bd948bea5dfc8) /*  135 */,
  U64_C(0xa5a0d357bd3ff77e) /*  136 */, U64_C(0xa2d12e251f74f645) /*  137 */,
  U64_C(0x66fd9e525e81a082) /*  138 */, U64_C(0x2e0c90ce7f687a49) /*  139 */,
  U64_C(0xc2e8bcbeba973bc5) /*  140 */, U64_C(0x000001bce509745f) /*  141 */,
  U64_C(0x423777bbe6dab3d6) /*  142 */, U64_C(0xd1661c7eaef06eb5) /*  143 */,
  U64_C(0xa1781f354daacfd8) /*  144 */, U64_C(0x2d11284a2b16affc) /*  145 */,
  U64_C(0xf1fc4f67fa891d1f) /*  146 */, U64_C(0x73ecc25dcb920ada) /*  147 */,
  U64_C(0xae610c22c2a12651) /*  148 */, U64_C(0x96e0a810d356b78a) /*  149 */,
  U64_C(0x5a9a381f2fe7870f) /*  150 */, U64_C(0xd5ad62ede94e5530) /*  151 */,
  U64_C(0xd225e5e8368d1427) /*  152 */, U64_C(0x65977b70c7af4631) /*  153 */,
  U64_C(0x99f889b2de39d74f) /*  154 */, U64_C(0x233f30bf54e1d143) /*  155 */,
  U64_C(0x9a9675d3d9a63c97) /*  156 */, U64_C(0x5470554ff334f9a8) /*  157 */,
  U64_C(0x166acb744a4f5688) /*  158 */, U64_C(0x70c74caab2e4aead) /*  159 */,
  U64_C(0xf0d091646f294d12) /*  160 */, U64_C(0x57b82a89684031d1) /*  161 */,
  U64_C(0xefd95a5a61be0b6b) /*  162 */, U64_C(0x2fbd12e969f2f29a) /*  163 */,
  U64_C(0x9bd37013feff9fe8) /*  164 */, U64_C(0x3f9b0404d6085a06) /*  165 */,
  U64_C(0x4940c1f3166cfe15) /*  166 */, U64_C(0x09542c4dcdf3defb) /*  167 */,
  U64_C(0xb4c5218385cd5ce3) /*  168 */, U64_C(0xc935b7dc4462a641) /*  169 */,
  U64_C(0x3417f8a68ed3b63f) /*  170 */, U64_C(0xb80959295b215b40) /*  171 */,
  U64_C(0xf99cdaef3b8c8572) /*  172 */, U64_C(0x018c0614f8fcb95d) /*  173 */,
  U64_C(0x1b14accd1a3acdf3) /*  174 */, U64_C(0x84d471f200bb732d) /*  175 */,
  U64_C(0xc1a3110e95e8da16) /*  176 */, U64_C(0x430a7220bf1a82b8) /*  177 */,
  U64_C(0xb77e090d39df210e) /*  178 */, U64_C(0x5ef4bd9f3cd05e9d) /*  179 */,
  U64_C(0x9d4ff6da7e57a444) /*  180 */, U64_C(0xda1d60e183d4a5f8) /*  181 */,
  U64_C(0xb287c38417998e47) /*  182 */, U64_C(0xfe3edc121bb31886) /*  183 */,
  U64_C(0xc7fe3ccc980ccbef) /*  184 */, U64_C(0xe46fb590189bfd03) /*  185 */,
  U64_C(0x3732fd469a4c57dc) /*  186 */, U64_C(0x7ef700a07cf1ad65) /*  187 */,
  U64_C(0x59c64468a31d8859) /*  188 */, U64_C(0x762fb0b4d45b61f6) /*  189 */,
  U64_C(0x155baed099047718) /*  190 */, U64_C(0x68755e4c3d50baa6) /*  191 */,
  U64_C(0xe9214e7f22d8b4df) /*  192 */, U64_C(0x2addbf532eac95f4) /*  193 */,
  U64_C(0x32ae3909b4bd0109) /*  194 */, U64_C(0x834df537b08e3450) /*  195 */,
  U64_C(0xfa209da84220728d) /*  196 */, U64_C(0x9e691d9b9efe23f7) /*  197 */,
  U64_C(0x0446d288c4ae8d7f) /*  198 */, U64_C(0x7b4cc524e169785b) /*  199 */,
  U64_C(0x21d87f0135ca1385) /*  200 */, U64_C(0xcebb400f137b8aa5) /*  201 */,
  U64_C(0x272e2b66580796be) /*  202 */, U64_C(0x3612264125c2b0de) /*  203 */,
  U64_C(0x057702bdad1efbb2) /*  204 */, U64_C(0xd4babb8eacf84be9) /*  205 */,
  U64_C(0x91583139641bc67b) /*  206 */, U64_C(0x8bdc2de08036e024) /*  207 */,
  U64_C(0x603c8156f49f68ed) /*  208 */, U64_C(0xf7d236f7dbef5111) /*  209 */,
  U64_C(0x9727c4598ad21e80) /*  210 */, U64_C(0xa08a0896670a5fd7) /*  211 */,
  U64_C(0xcb4a8f4309eba9cb) /*  212 */, U64_C(0x81af564b0f7036a1) /*  213 */,
  U64_C(0xc0b99aa778199abd) /*  214 */, U64_C(0x959f1ec83fc8e952) /*  215 */,
  U64_C(0x8c505077794a81b9) /*  216 */, U64_C(0x3acaaf8f056338f0) /*  217 */,
  U64_C(0x07b43f50627a6778) /*  218 */, U64_C(0x4a44ab49f5eccc77) /*  219 */,
  U64_C(0x3bc3d6e4b679ee98) /*  220 */, U64_C(0x9cc0d4d1cf14108c) /*  221 */,
  U64_C(0x4406c00b206bc8a0) /*  222 */, U64_C(0x82a18854c8d72d89) /*  223 */,
  U64_C(0x67e366b35c3c432c) /*  224 */, U64_C(0xb923dd61102b37f2) /*  225 */,
  U64_C(0x56ab2779d884271d) /*  226 */, U64_C(0xbe83e1b0ff1525af) /*  227 */,
  U64_C(0xfb7c65d4217e49a9) /*  228 */, U64_C(0x6bdbe0e76d48e7d4) /*  229 */,
  U64_C(0x08df828745d9179e) /*  230 */, U64_C(0x22ea6a9add53bd34) /*  231 */,
  U64_C(0xe36e141c5622200a) /*  232 */, U64_C(0x7f805d1b8cb750ee) /*  233 */,
  U64_C(0xafe5c7a59f58e837) /*  234 */, U64_C(0xe27f996a4fb1c23c) /*  235 */,
  U64_C(0xd3867dfb0775f0d0) /*  236 */, U64_C(0xd0e673de6e88891a) /*  237 */,
  U64_C(0x123aeb9eafb86c25) /*  238 */, U64_C(0x30f1d5d5c145b895) /*  239 */,
  U64_C(0xbb434a2dee7269e7) /*  240 */, U64_C(0x78cb67ecf931fa38) /*  241 */,
  U64_C(0xf33b0372323bbf9c) /*  242 */, U64_C(0x52d66336fb279c74) /*  243 */,
  U64_C(0x505f33ac0afb4eaa) /*  244 */, U64_C(0xe8a5cd99a2cce187) /*  245 */,
  U64_C(0x534974801e2d30bb) /*  246 */, U64_C(0x8d2d5711d5876d90) /*  247 */,
  U64_C(0x1f1a412891bc038e) /*  248 */, U64_C(0xd6e2e71d82e56648) /*  249 */,
  U64_C(0x74036c3a497732b7) /*  250 */, U64_C(0x89b67ed96361f5ab) /*  251 */,
  U64_C(0xffed95d8f1ea02a2) /*  252 */, U64_C(0xe72b3bd61464d43d) /*  253 */,
  U64_C(0xa6300f170bdc4820) /*  254 */, U64_C(0xebc18760ed78a77a) /*  255 */
};
static u64 sbox2[256] = {
  U64_C(0xe6a6be5a05a12138) /*  256 */, U64_C(0xb5a122a5b4f87c98) /*  257 */,
  U64_C(0x563c6089140b6990) /*  258 */, U64_C(0x4c46cb2e391f5dd5) /*  259 */,
  U64_C(0xd932addbc9b79434) /*  260 */, U64_C(0x08ea70e42015aff5) /*  261 */,
  U64_C(0xd765a6673e478cf1) /*  262 */, U64_C(0xc4fb757eab278d99) /*  263 */,
  U64_C(0xdf11c6862d6e0692) /*  264 */, U64_C(0xddeb84f10d7f3b16) /*  265 */,
  U64_C(0x6f2ef604a665ea04) /*  266 */, U64_C(0x4a8e0f0ff0e0dfb3) /*  267 */,
  U64_C(0xa5edeef83dbcba51) /*  268 */, U64_C(0xfc4f0a2a0ea4371e) /*  269 */,
  U64_C(0xe83e1da85cb38429) /*  270 */, U64_C(0xdc8ff882ba1b1ce2) /*  271 */,
  U64_C(0xcd45505e8353e80d) /*  272 */, U64_C(0x18d19a00d4db0717) /*  273 */,
  U64_C(0x34a0cfeda5f38101) /*  274 */, U64_C(0x0be77e518887caf2) /*  275 */,
  U64_C(0x1e341438b3c45136) /*  276 */, U64_C(0xe05797f49089ccf9) /*  277 */,
  U64_C(0xffd23f9df2591d14) /*  278 */, U64_C(0x543dda228595c5cd) /*  279 */,
  U64_C(0x661f81fd99052a33) /*  280 */, U64_C(0x8736e641db0f7b76) /*  281 */,
  U64_C(0x15227725418e5307) /*  282 */, U64_C(0xe25f7f46162eb2fa) /*  283 */,
  U64_C(0x48a8b2126c13d9fe) /*  284 */, U64_C(0xafdc541792e76eea) /*  285 */,
  U64_C(0x03d912bfc6d1898f) /*  286 */, U64_C(0x31b1aafa1b83f51b) /*  287 */,
  U64_C(0xf1ac2796e42ab7d9) /*  288 */, U64_C(0x40a3a7d7fcd2ebac) /*  289 */,
  U64_C(0x1056136d0afbbcc5) /*  290 */, U64_C(0x7889e1dd9a6d0c85) /*  291 */,
  U64_C(0xd33525782a7974aa) /*  292 */, U64_C(0xa7e25d09078ac09b) /*  293 */,
  U64_C(0xbd4138b3eac6edd0) /*  294 */, U64_C(0x920abfbe71eb9e70) /*  295 */,
  U64_C(0xa2a5d0f54fc2625c) /*  296 */, U64_C(0xc054e36b0b1290a3) /*  297 */,
  U64_C(0xf6dd59ff62fe932b) /*  298 */, U64_C(0x3537354511a8ac7d) /*  299 */,
  U64_C(0xca845e9172fadcd4) /*  300 */, U64_C(0x84f82b60329d20dc) /*  301 */,
  U64_C(0x79c62ce1cd672f18) /*  302 */, U64_C(0x8b09a2add124642c) /*  303 */,
  U64_C(0xd0c1e96a19d9e726) /*  304 */, U64_C(0x5a786a9b4ba9500c) /*  305 */,
  U64_C(0x0e020336634c43f3) /*  306 */, U64_C(0xc17b474aeb66d822) /*  307 */,
  U64_C(0x6a731ae3ec9baac2) /*  308 */, U64_C(0x8226667ae0840258) /*  309 */,
  U64_C(0x67d4567691caeca5) /*  310 */, U64_C(0x1d94155c4875adb5) /*  311 */,
  U64_C(0x6d00fd985b813fdf) /*  312 */, U64_C(0x51286efcb774cd06) /*  313 */,
  U64_C(0x5e8834471fa744af) /*  314 */, U64_C(0xf72ca0aee761ae2e) /*  315 */,
  U64_C(0xbe40e4cdaee8e09a) /*  316 */, U64_C(0xe9970bbb5118f665) /*  317 */,
  U64_C(0x726e4beb33df1964) /*  318 */, U64_C(0x703b000729199762) /*  319 */,
  U64_C(0x4631d816f5ef30a7) /*  320 */, U64_C(0xb880b5b51504a6be) /*  321 */,
  U64_C(0x641793c37ed84b6c) /*  322 */, U64_C(0x7b21ed77f6e97d96) /*  323 */,
  U64_C(0x776306312ef96b73) /*  324 */, U64_C(0xae528948e86ff3f4) /*  325 */,
  U64_C(0x53dbd7f286a3f8f8) /*  326 */, U64_C(0x16cadce74cfc1063) /*  327 */,
  U64_C(0x005c19bdfa52c6dd) /*  328 */, U64_C(0x68868f5d64d46ad3) /*  329 */,
  U64_C(0x3a9d512ccf1e186a) /*  330 */, U64_C(0x367e62c2385660ae) /*  331 */,
  U64_C(0xe359e7ea77dcb1d7) /*  332 */, U64_C(0x526c0773749abe6e) /*  333 */,
  U64_C(0x735ae5f9d09f734b) /*  334 */, U64_C(0x493fc7cc8a558ba8) /*  335 */,
  U64_C(0xb0b9c1533041ab45) /*  336 */, U64_C(0x321958ba470a59bd) /*  337 */,
  U64_C(0x852db00b5f46c393) /*  338 */, U64_C(0x91209b2bd336b0e5) /*  339 */,
  U64_C(0x6e604f7d659ef19f) /*  340 */, U64_C(0xb99a8ae2782ccb24) /*  341 */,
  U64_C(0xccf52ab6c814c4c7) /*  342 */, U64_C(0x4727d9afbe11727b) /*  343 */,
  U64_C(0x7e950d0c0121b34d) /*  344 */, U64_C(0x756f435670ad471f) /*  345 */,
  U64_C(0xf5add442615a6849) /*  346 */, U64_C(0x4e87e09980b9957a) /*  347 */,
  U64_C(0x2acfa1df50aee355) /*  348 */, U64_C(0xd898263afd2fd556) /*  349 */,
  U64_C(0xc8f4924dd80c8fd6) /*  350 */, U64_C(0xcf99ca3d754a173a) /*  351 */,
  U64_C(0xfe477bacaf91bf3c) /*  352 */, U64_C(0xed5371f6d690c12d) /*  353 */,
  U64_C(0x831a5c285e687094) /*  354 */, U64_C(0xc5d3c90a3708a0a4) /*  355 */,
  U64_C(0x0f7f903717d06580) /*  356 */, U64_C(0x19f9bb13b8fdf27f) /*  357 */,
  U64_C(0xb1bd6f1b4d502843) /*  358 */, U64_C(0x1c761ba38fff4012) /*  359 */,
  U64_C(0x0d1530c4e2e21f3b) /*  360 */, U64_C(0x8943ce69a7372c8a) /*  361 */,
  U64_C(0xe5184e11feb5ce66) /*  362 */, U64_C(0x618bdb80bd736621) /*  363 */,
  U64_C(0x7d29bad68b574d0b) /*  364 */, U64_C(0x81bb613e25e6fe5b) /*  365 */,
  U64_C(0x071c9c10bc07913f) /*  366 */, U64_C(0xc7beeb7909ac2d97) /*  367 */,
  U64_C(0xc3e58d353bc5d757) /*  368 */, U64_C(0xeb017892f38f61e8) /*  369 */,
  U64_C(0xd4effb9c9b1cc21a) /*  370 */, U64_C(0x99727d26f494f7ab) /*  371 */,
  U64_C(0xa3e063a2956b3e03) /*  372 */, U64_C(0x9d4a8b9a4aa09c30) /*  373 */,
  U64_C(0x3f6ab7d500090fb4) /*  374 */, U64_C(0x9cc0f2a057268ac0) /*  375 */,
  U64_C(0x3dee9d2dedbf42d1) /*  376 */, U64_C(0x330f49c87960a972) /*  377 */,
  U64_C(0xc6b2720287421b41) /*  378 */, U64_C(0x0ac59ec07c00369c) /*  379 */,
  U64_C(0xef4eac49cb353425) /*  380 */, U64_C(0xf450244eef0129d8) /*  381 */,
  U64_C(0x8acc46e5caf4deb6) /*  382 */, U64_C(0x2ffeab63989263f7) /*  383 */,
  U64_C(0x8f7cb9fe5d7a4578) /*  384 */, U64_C(0x5bd8f7644e634635) /*  385 */,
  U64_C(0x427a7315bf2dc900) /*  386 */, U64_C(0x17d0c4aa2125261c) /*  387 */,
  U64_C(0x3992486c93518e50) /*  388 */, U64_C(0xb4cbfee0a2d7d4c3) /*  389 */,
  U64_C(0x7c75d6202c5ddd8d) /*  390 */, U64_C(0xdbc295d8e35b6c61) /*  391 */,
  U64_C(0x60b369d302032b19) /*  392 */, U64_C(0xce42685fdce44132) /*  393 */,
  U64_C(0x06f3ddb9ddf65610) /*  394 */, U64_C(0x8ea4d21db5e148f0) /*  395 */,
  U64_C(0x20b0fce62fcd496f) /*  396 */, U64_C(0x2c1b912358b0ee31) /*  397 */,
  U64_C(0xb28317b818f5a308) /*  398 */, U64_C(0xa89c1e189ca6d2cf) /*  399 */,
  U64_C(0x0c6b18576aaadbc8) /*  400 */, U64_C(0xb65deaa91299fae3) /*  401 */,
  U64_C(0xfb2b794b7f1027e7) /*  402 */, U64_C(0x04e4317f443b5beb) /*  403 */,
  U64_C(0x4b852d325939d0a6) /*  404 */, U64_C(0xd5ae6beefb207ffc) /*  405 */,
  U64_C(0x309682b281c7d374) /*  406 */, U64_C(0xbae309a194c3b475) /*  407 */,
  U64_C(0x8cc3f97b13b49f05) /*  408 */, U64_C(0x98a9422ff8293967) /*  409 */,
  U64_C(0x244b16b01076ff7c) /*  410 */, U64_C(0xf8bf571c663d67ee) /*  411 */,
  U64_C(0x1f0d6758eee30da1) /*  412 */, U64_C(0xc9b611d97adeb9b7) /*  413 */,
  U64_C(0xb7afd5887b6c57a2) /*  414 */, U64_C(0x6290ae846b984fe1) /*  415 */,
  U64_C(0x94df4cdeacc1a5fd) /*  416 */, U64_C(0x058a5bd1c5483aff) /*  417 */,
  U64_C(0x63166cc142ba3c37) /*  418 */, U64_C(0x8db8526eb2f76f40) /*  419 */,
  U64_C(0xe10880036f0d6d4e) /*  420 */, U64_C(0x9e0523c9971d311d) /*  421 */,
  U64_C(0x45ec2824cc7cd691) /*  422 */, U64_C(0x575b8359e62382c9) /*  423 */,
  U64_C(0xfa9e400dc4889995) /*  424 */, U64_C(0xd1823ecb45721568) /*  425 */,
  U64_C(0xdafd983b8206082f) /*  426 */, U64_C(0xaa7d29082386a8cb) /*  427 */,
  U64_C(0x269fcd4403b87588) /*  428 */, U64_C(0x1b91f5f728bdd1e0) /*  429 */,
  U64_C(0xe4669f39040201f6) /*  430 */, U64_C(0x7a1d7c218cf04ade) /*  431 */,
  U64_C(0x65623c29d79ce5ce) /*  432 */, U64_C(0x2368449096c00bb1) /*  433 */,
  U64_C(0xab9bf1879da503ba) /*  434 */, U64_C(0xbc23ecb1a458058e) /*  435 */,
  U64_C(0x9a58df01bb401ecc) /*  436 */, U64_C(0xa070e868a85f143d) /*  437 */,
  U64_C(0x4ff188307df2239e) /*  438 */, U64_C(0x14d565b41a641183) /*  439 */,
  U64_C(0xee13337452701602) /*  440 */, U64_C(0x950e3dcf3f285e09) /*  441 */,
  U64_C(0x59930254b9c80953) /*  442 */, U64_C(0x3bf299408930da6d) /*  443 */,
  U64_C(0xa955943f53691387) /*  444 */, U64_C(0xa15edecaa9cb8784) /*  445 */,
  U64_C(0x29142127352be9a0) /*  446 */, U64_C(0x76f0371fff4e7afb) /*  447 */,
  U64_C(0x0239f450274f2228) /*  448 */, U64_C(0xbb073af01d5e868b) /*  449 */,
  U64_C(0xbfc80571c10e96c1) /*  450 */, U64_C(0xd267088568222e23) /*  451 */,
  U64_C(0x9671a3d48e80b5b0) /*  452 */, U64_C(0x55b5d38ae193bb81) /*  453 */,
  U64_C(0x693ae2d0a18b04b8) /*  454 */, U64_C(0x5c48b4ecadd5335f) /*  455 */,
  U64_C(0xfd743b194916a1ca) /*  456 */, U64_C(0x2577018134be98c4) /*  457 */,
  U64_C(0xe77987e83c54a4ad) /*  458 */, U64_C(0x28e11014da33e1b9) /*  459 */,
  U64_C(0x270cc59e226aa213) /*  460 */, U64_C(0x71495f756d1a5f60) /*  461 */,
  U64_C(0x9be853fb60afef77) /*  462 */, U64_C(0xadc786a7f7443dbf) /*  463 */,
  U64_C(0x0904456173b29a82) /*  464 */, U64_C(0x58bc7a66c232bd5e) /*  465 */,
  U64_C(0xf306558c673ac8b2) /*  466 */, U64_C(0x41f639c6b6c9772a) /*  467 */,
  U64_C(0x216defe99fda35da) /*  468 */, U64_C(0x11640cc71c7be615) /*  469 */,
  U64_C(0x93c43694565c5527) /*  470 */, U64_C(0xea038e6246777839) /*  471 */,
  U64_C(0xf9abf3ce5a3e2469) /*  472 */, U64_C(0x741e768d0fd312d2) /*  473 */,
  U64_C(0x0144b883ced652c6) /*  474 */, U64_C(0xc20b5a5ba33f8552) /*  475 */,
  U64_C(0x1ae69633c3435a9d) /*  476 */, U64_C(0x97a28ca4088cfdec) /*  477 */,
  U64_C(0x8824a43c1e96f420) /*  478 */, U64_C(0x37612fa66eeea746) /*  479 */,
  U64_C(0x6b4cb165f9cf0e5a) /*  480 */, U64_C(0x43aa1c06a0abfb4a) /*  481 */,
  U64_C(0x7f4dc26ff162796b) /*  482 */, U64_C(0x6cbacc8e54ed9b0f) /*  483 */,
  U64_C(0xa6b7ffefd2bb253e) /*  484 */, U64_C(0x2e25bc95b0a29d4f) /*  485 */,
  U64_C(0x86d6a58bdef1388c) /*  486 */, U64_C(0xded74ac576b6f054) /*  487 */,
  U64_C(0x8030bdbc2b45805d) /*  488 */, U64_C(0x3c81af70e94d9289) /*  489 */,
  U64_C(0x3eff6dda9e3100db) /*  490 */, U64_C(0xb38dc39fdfcc8847) /*  491 */,
  U64_C(0x123885528d17b87e) /*  492 */, U64_C(0xf2da0ed240b1b642) /*  493 */,
  U64_C(0x44cefadcd54bf9a9) /*  494 */, U64_C(0x1312200e433c7ee6) /*  495 */,
  U64_C(0x9ffcc84f3a78c748) /*  496 */, U64_C(0xf0cd1f72248576bb) /*  497 */,
  U64_C(0xec6974053638cfe4) /*  498 */, U64_C(0x2ba7b67c0cec4e4c) /*  499 */,
  U64_C(0xac2f4df3e5ce32ed) /*  500 */, U64_C(0xcb33d14326ea4c11) /*  501 */,
  U64_C(0xa4e9044cc77e58bc) /*  502 */, U64_C(0x5f513293d934fcef) /*  503 */,
  U64_C(0x5dc9645506e55444) /*  504 */, U64_C(0x50de418f317de40a) /*  505 */,
  U64_C(0x388cb31a69dde259) /*  506 */, U64_C(0x2db4a83455820a86) /*  507 */,
  U64_C(0x9010a91e84711ae9) /*  508 */, U64_C(0x4df7f0b7b1498371) /*  509 */,
  U64_C(0xd62a2eabc0977179) /*  510 */, U64_C(0x22fac097aa8d5c0e) /*  511 */
};
static u64 sbox3[256] = {
  U64_C(0xf49fcc2ff1daf39b) /*  512 */, U64_C(0x487fd5c66ff29281) /*  513 */,
  U64_C(0xe8a30667fcdca83f) /*  514 */, U64_C(0x2c9b4be3d2fcce63) /*  515 */,
  U64_C(0xda3ff74b93fbbbc2) /*  516 */, U64_C(0x2fa165d2fe70ba66) /*  517 */,
  U64_C(0xa103e279970e93d4) /*  518 */, U64_C(0xbecdec77b0e45e71) /*  519 */,
  U64_C(0xcfb41e723985e497) /*  520 */, U64_C(0xb70aaa025ef75017) /*  521 */,
  U64_C(0xd42309f03840b8e0) /*  522 */, U64_C(0x8efc1ad035898579) /*  523 */,
  U64_C(0x96c6920be2b2abc5) /*  524 */, U64_C(0x66af4163375a9172) /*  525 */,
  U64_C(0x2174abdcca7127fb) /*  526 */, U64_C(0xb33ccea64a72ff41) /*  527 */,
  U64_C(0xf04a4933083066a5) /*  528 */, U64_C(0x8d970acdd7289af5) /*  529 */,
  U64_C(0x8f96e8e031c8c25e) /*  530 */, U64_C(0xf3fec02276875d47) /*  531 */,
  U64_C(0xec7bf310056190dd) /*  532 */, U64_C(0xf5adb0aebb0f1491) /*  533 */,
  U64_C(0x9b50f8850fd58892) /*  534 */, U64_C(0x4975488358b74de8) /*  535 */,
  U64_C(0xa3354ff691531c61) /*  536 */, U64_C(0x0702bbe481d2c6ee) /*  537 */,
  U64_C(0x89fb24057deded98) /*  538 */, U64_C(0xac3075138596e902) /*  539 */,
  U64_C(0x1d2d3580172772ed) /*  540 */, U64_C(0xeb738fc28e6bc30d) /*  541 */,
  U64_C(0x5854ef8f63044326) /*  542 */, U64_C(0x9e5c52325add3bbe) /*  543 */,
  U64_C(0x90aa53cf325c4623) /*  544 */, U64_C(0xc1d24d51349dd067) /*  545 */,
  U64_C(0x2051cfeea69ea624) /*  546 */, U64_C(0x13220f0a862e7e4f) /*  547 */,
  U64_C(0xce39399404e04864) /*  548 */, U64_C(0xd9c42ca47086fcb7) /*  549 */,
  U64_C(0x685ad2238a03e7cc) /*  550 */, U64_C(0x066484b2ab2ff1db) /*  551 */,
  U64_C(0xfe9d5d70efbf79ec) /*  552 */, U64_C(0x5b13b9dd9c481854) /*  553 */,
  U64_C(0x15f0d475ed1509ad) /*  554 */, U64_C(0x0bebcd060ec79851) /*  555 */,
  U64_C(0xd58c6791183ab7f8) /*  556 */, U64_C(0xd1187c5052f3eee4) /*  557 */,
  U64_C(0xc95d1192e54e82ff) /*  558 */, U64_C(0x86eea14cb9ac6ca2) /*  559 */,
  U64_C(0x3485beb153677d5d) /*  560 */, U64_C(0xdd191d781f8c492a) /*  561 */,
  U64_C(0xf60866baa784ebf9) /*  562 */, U64_C(0x518f643ba2d08c74) /*  563 */,
  U64_C(0x8852e956e1087c22) /*  564 */, U64_C(0xa768cb8dc410ae8d) /*  565 */,
  U64_C(0x38047726bfec8e1a) /*  566 */, U64_C(0xa67738b4cd3b45aa) /*  567 */,
  U64_C(0xad16691cec0dde19) /*  568 */, U64_C(0xc6d4319380462e07) /*  569 */,
  U64_C(0xc5a5876d0ba61938) /*  570 */, U64_C(0x16b9fa1fa58fd840) /*  571 */,
  U64_C(0x188ab1173ca74f18) /*  572 */, U64_C(0xabda2f98c99c021f) /*  573 */,
  U64_C(0x3e0580ab134ae816) /*  574 */, U64_C(0x5f3b05b773645abb) /*  575 */,
  U64_C(0x2501a2be5575f2f6) /*  576 */, U64_C(0x1b2f74004e7e8ba9) /*  577 */,
  U64_C(0x1cd7580371e8d953) /*  578 */, U64_C(0x7f6ed89562764e30) /*  579 */,
  U64_C(0xb15926ff596f003d) /*  580 */, U64_C(0x9f65293da8c5d6b9) /*  581 */,
  U64_C(0x6ecef04dd690f84c) /*  582 */, U64_C(0x4782275fff33af88) /*  583 */,
  U64_C(0xe41433083f820801) /*  584 */, U64_C(0xfd0dfe409a1af9b5) /*  585 */,
  U64_C(0x4325a3342cdb396b) /*  586 */, U64_C(0x8ae77e62b301b252) /*  587 */,
  U64_C(0xc36f9e9f6655615a) /*  588 */, U64_C(0x85455a2d92d32c09) /*  589 */,
  U64_C(0xf2c7dea949477485) /*  590 */, U64_C(0x63cfb4c133a39eba) /*  591 */,
  U64_C(0x83b040cc6ebc5462) /*  592 */, U64_C(0x3b9454c8fdb326b0) /*  593 */,
  U64_C(0x56f56a9e87ffd78c) /*  594 */, U64_C(0x2dc2940d99f42bc6) /*  595 */,
  U64_C(0x98f7df096b096e2d) /*  596 */, U64_C(0x19a6e01e3ad852bf) /*  597 */,
  U64_C(0x42a99ccbdbd4b40b) /*  598 */, U64_C(0xa59998af45e9c559) /*  599 */,
  U64_C(0x366295e807d93186) /*  600 */, U64_C(0x6b48181bfaa1f773) /*  601 */,
  U64_C(0x1fec57e2157a0a1d) /*  602 */, U64_C(0x4667446af6201ad5) /*  603 */,
  U64_C(0xe615ebcacfb0f075) /*  604 */, U64_C(0xb8f31f4f68290778) /*  605 */,
  U64_C(0x22713ed6ce22d11e) /*  606 */, U64_C(0x3057c1a72ec3c93b) /*  607 */,
  U64_C(0xcb46acc37c3f1f2f) /*  608 */, U64_C(0xdbb893fd02aaf50e) /*  609 */,
  U64_C(0x331fd92e600b9fcf) /*  610 */, U64_C(0xa498f96148ea3ad6) /*  611 */,
  U64_C(0xa8d8426e8b6a83ea) /*  612 */, U64_C(0xa089b274b7735cdc) /*  613 */,
  U64_C(0x87f6b3731e524a11) /*  614 */, U64_C(0x118808e5cbc96749) /*  615 */,
  U64_C(0x9906e4c7b19bd394) /*  616 */, U64_C(0xafed7f7e9b24a20c) /*  617 */,
  U64_C(0x6509eadeeb3644a7) /*  618 */, U64_C(0x6c1ef1d3e8ef0ede) /*  619 */,
  U64_C(0xb9c97d43e9798fb4) /*  620 */, U64_C(0xa2f2d784740c28a3) /*  621 */,
  U64_C(0x7b8496476197566f) /*  622 */, U64_C(0x7a5be3e6b65f069d) /*  623 */,
  U64_C(0xf96330ed78be6f10) /*  624 */, U64_C(0xeee60de77a076a15) /*  625 */,
  U64_C(0x2b4bee4aa08b9bd0) /*  626 */, U64_C(0x6a56a63ec7b8894e) /*  627 */,
  U64_C(0x02121359ba34fef4) /*  628 */, U64_C(0x4cbf99f8283703fc) /*  629 */,
  U64_C(0x398071350caf30c8) /*  630 */, U64_C(0xd0a77a89f017687a) /*  631 */,
  U64_C(0xf1c1a9eb9e423569) /*  632 */, U64_C(0x8c7976282dee8199) /*  633 */,
  U64_C(0x5d1737a5dd1f7abd) /*  634 */, U64_C(0x4f53433c09a9fa80) /*  635 */,
  U64_C(0xfa8b0c53df7ca1d9) /*  636 */, U64_C(0x3fd9dcbc886ccb77) /*  637 */,
  U64_C(0xc040917ca91b4720) /*  638 */, U64_C(0x7dd00142f9d1dcdf) /*  639 */,
  U64_C(0x8476fc1d4f387b58) /*  640 */, U64_C(0x23f8e7c5f3316503) /*  641 */,
  U64_C(0x032a2244e7e37339) /*  642 */, U64_C(0x5c87a5d750f5a74b) /*  643 */,
  U64_C(0x082b4cc43698992e) /*  644 */, U64_C(0xdf917becb858f63c) /*  645 */,
  U64_C(0x3270b8fc5bf86dda) /*  646 */, U64_C(0x10ae72bb29b5dd76) /*  647 */,
  U64_C(0x576ac94e7700362b) /*  648 */, U64_C(0x1ad112dac61efb8f) /*  649 */,
  U64_C(0x691bc30ec5faa427) /*  650 */, U64_C(0xff246311cc327143) /*  651 */,
  U64_C(0x3142368e30e53206) /*  652 */, U64_C(0x71380e31e02ca396) /*  653 */,
  U64_C(0x958d5c960aad76f1) /*  654 */, U64_C(0xf8d6f430c16da536) /*  655 */,
  U64_C(0xc8ffd13f1be7e1d2) /*  656 */, U64_C(0x7578ae66004ddbe1) /*  657 */,
  U64_C(0x05833f01067be646) /*  658 */, U64_C(0xbb34b5ad3bfe586d) /*  659 */,
  U64_C(0x095f34c9a12b97f0) /*  660 */, U64_C(0x247ab64525d60ca8) /*  661 */,
  U64_C(0xdcdbc6f3017477d1) /*  662 */, U64_C(0x4a2e14d4decad24d) /*  663 */,
  U64_C(0xbdb5e6d9be0a1eeb) /*  664 */, U64_C(0x2a7e70f7794301ab) /*  665 */,
  U64_C(0xdef42d8a270540fd) /*  666 */, U64_C(0x01078ec0a34c22c1) /*  667 */,
  U64_C(0xe5de511af4c16387) /*  668 */, U64_C(0x7ebb3a52bd9a330a) /*  669 */,
  U64_C(0x77697857aa7d6435) /*  670 */, U64_C(0x004e831603ae4c32) /*  671 */,
  U64_C(0xe7a21020ad78e312) /*  672 */, U64_C(0x9d41a70c6ab420f2) /*  673 */,
  U64_C(0x28e06c18ea1141e6) /*  674 */, U64_C(0xd2b28cbd984f6b28) /*  675 */,
  U64_C(0x26b75f6c446e9d83) /*  676 */, U64_C(0xba47568c4d418d7f) /*  677 */,
  U64_C(0xd80badbfe6183d8e) /*  678 */, U64_C(0x0e206d7f5f166044) /*  679 */,
  U64_C(0xe258a43911cbca3e) /*  680 */, U64_C(0x723a1746b21dc0bc) /*  681 */,
  U64_C(0xc7caa854f5d7cdd3) /*  682 */, U64_C(0x7cac32883d261d9c) /*  683 */,
  U64_C(0x7690c26423ba942c) /*  684 */, U64_C(0x17e55524478042b8) /*  685 */,
  U64_C(0xe0be477656a2389f) /*  686 */, U64_C(0x4d289b5e67ab2da0) /*  687 */,
  U64_C(0x44862b9c8fbbfd31) /*  688 */, U64_C(0xb47cc8049d141365) /*  689 */,
  U64_C(0x822c1b362b91c793) /*  690 */, U64_C(0x4eb14655fb13dfd8) /*  691 */,
  U64_C(0x1ecbba0714e2a97b) /*  692 */, U64_C(0x6143459d5cde5f14) /*  693 */,
  U64_C(0x53a8fbf1d5f0ac89) /*  694 */, U64_C(0x97ea04d81c5e5b00) /*  695 */,
  U64_C(0x622181a8d4fdb3f3) /*  696 */, U64_C(0xe9bcd341572a1208) /*  697 */,
  U64_C(0x1411258643cce58a) /*  698 */, U64_C(0x9144c5fea4c6e0a4) /*  699 */,
  U64_C(0x0d33d06565cf620f) /*  700 */, U64_C(0x54a48d489f219ca1) /*  701 */,
  U64_C(0xc43e5eac6d63c821) /*  702 */, U64_C(0xa9728b3a72770daf) /*  703 */,
  U64_C(0xd7934e7b20df87ef) /*  704 */, U64_C(0xe35503b61a3e86e5) /*  705 */,
  U64_C(0xcae321fbc819d504) /*  706 */, U64_C(0x129a50b3ac60bfa6) /*  707 */,
  U64_C(0xcd5e68ea7e9fb6c3) /*  708 */, U64_C(0xb01c90199483b1c7) /*  709 */,
  U64_C(0x3de93cd5c295376c) /*  710 */, U64_C(0xaed52edf2ab9ad13) /*  711 */,
  U64_C(0x2e60f512c0a07884) /*  712 */, U64_C(0xbc3d86a3e36210c9) /*  713 */,
  U64_C(0x35269d9b163951ce) /*  714 */, U64_C(0x0c7d6e2ad0cdb5fa) /*  715 */,
  U64_C(0x59e86297d87f5733) /*  716 */, U64_C(0x298ef221898db0e7) /*  717 */,
  U64_C(0x55000029d1a5aa7e) /*  718 */, U64_C(0x8bc08ae1b5061b45) /*  719 */,
  U64_C(0xc2c31c2b6c92703a) /*  720 */, U64_C(0x94cc596baf25ef42) /*  721 */,
  U64_C(0x0a1d73db22540456) /*  722 */, U64_C(0x04b6a0f9d9c4179a) /*  723 */,
  U64_C(0xeffdafa2ae3d3c60) /*  724 */, U64_C(0xf7c8075bb49496c4) /*  725 */,
  U64_C(0x9cc5c7141d1cd4e3) /*  726 */, U64_C(0x78bd1638218e5534) /*  727 */,
  U64_C(0xb2f11568f850246a) /*  728 */, U64_C(0xedfabcfa9502bc29) /*  729 */,
  U64_C(0x796ce5f2da23051b) /*  730 */, U64_C(0xaae128b0dc93537c) /*  731 */,
  U64_C(0x3a493da0ee4b29ae) /*  732 */, U64_C(0xb5df6b2c416895d7) /*  733 */,
  U64_C(0xfcabbd25122d7f37) /*  734 */, U64_C(0x70810b58105dc4b1) /*  735 */,
  U64_C(0xe10fdd37f7882a90) /*  736 */, U64_C(0x524dcab5518a3f5c) /*  737 */,
  U64_C(0x3c9e85878451255b) /*  738 */, U64_C(0x4029828119bd34e2) /*  739 */,
  U64_C(0x74a05b6f5d3ceccb) /*  740 */, U64_C(0xb610021542e13eca) /*  741 */,
  U64_C(0x0ff979d12f59e2ac) /*  742 */, U64_C(0x6037da27e4f9cc50) /*  743 */,
  U64_C(0x5e92975a0df1847d) /*  744 */, U64_C(0xd66de190d3e623fe) /*  745 */,
  U64_C(0x5032d6b87b568048) /*  746 */, U64_C(0x9a36b7ce8235216e) /*  747 */,
  U64_C(0x80272a7a24f64b4a) /*  748 */, U64_C(0x93efed8b8c6916f7) /*  749 */,
  U64_C(0x37ddbff44cce1555) /*  750 */, U64_C(0x4b95db5d4b99bd25) /*  751 */,
  U64_C(0x92d3fda169812fc0) /*  752 */, U64_C(0xfb1a4a9a90660bb6) /*  753 */,
  U64_C(0x730c196946a4b9b2) /*  754 */, U64_C(0x81e289aa7f49da68) /*  755 */,
  U64_C(0x64669a0f83b1a05f) /*  756 */, U64_C(0x27b3ff7d9644f48b) /*  757 */,
  U64_C(0xcc6b615c8db675b3) /*  758 */, U64_C(0x674f20b9bcebbe95) /*  759 */,
  U64_C(0x6f31238275655982) /*  760 */, U64_C(0x5ae488713e45cf05) /*  761 */,
  U64_C(0xbf619f9954c21157) /*  762 */, U64_C(0xeabac46040a8eae9) /*  763 */,
  U64_C(0x454c6fe9f2c0c1cd) /*  764 */, U64_C(0x419cf6496412691c) /*  765 */,
  U64_C(0xd3dc3bef265b0f70) /*  766 */, U64_C(0x6d0e60f5c3578a9e) /*  767 */
};
static u64 sbox4[256] = {
  U64_C(0x5b0e608526323c55) /*  768 */, U64_C(0x1a46c1a9fa1b59f5) /*  769 */,
  U64_C(0xa9e245a17c4c8ffa) /*  770 */, U64_C(0x65ca5159db2955d7) /*  771 */,
  U64_C(0x05db0a76ce35afc2) /*  772 */, U64_C(0x81eac77ea9113d45) /*  773 */,
  U64_C(0x528ef88ab6ac0a0d) /*  774 */, U64_C(0xa09ea253597be3ff) /*  775 */,
  U64_C(0x430ddfb3ac48cd56) /*  776 */, U64_C(0xc4b3a67af45ce46f) /*  777 */,
  U64_C(0x4ececfd8fbe2d05e) /*  778 */, U64_C(0x3ef56f10b39935f0) /*  779 */,
  U64_C(0x0b22d6829cd619c6) /*  780 */, U64_C(0x17fd460a74df2069) /*  781 */,
  U64_C(0x6cf8cc8e8510ed40) /*  782 */, U64_C(0xd6c824bf3a6ecaa7) /*  783 */,
  U64_C(0x61243d581a817049) /*  784 */, U64_C(0x048bacb6bbc163a2) /*  785 */,
  U64_C(0xd9a38ac27d44cc32) /*  786 */, U64_C(0x7fddff5baaf410ab) /*  787 */,
  U64_C(0xad6d495aa804824b) /*  788 */, U64_C(0xe1a6a74f2d8c9f94) /*  789 */,
  U64_C(0xd4f7851235dee8e3) /*  790 */, U64_C(0xfd4b7f886540d893) /*  791 */,
  U64_C(0x247c20042aa4bfda) /*  792 */, U64_C(0x096ea1c517d1327c) /*  793 */,
  U64_C(0xd56966b4361a6685) /*  794 */, U64_C(0x277da5c31221057d) /*  795 */,
  U64_C(0x94d59893a43acff7) /*  796 */, U64_C(0x64f0c51ccdc02281) /*  797 */,
  U64_C(0x3d33bcc4ff6189db) /*  798 */, U64_C(0xe005cb184ce66af1) /*  799 */,
  U64_C(0xff5ccd1d1db99bea) /*  800 */, U64_C(0xb0b854a7fe42980f) /*  801 */,
  U64_C(0x7bd46a6a718d4b9f) /*  802 */, U64_C(0xd10fa8cc22a5fd8c) /*  803 */,
  U64_C(0xd31484952be4bd31) /*  804 */, U64_C(0xc7fa975fcb243847) /*  805 */,
  U64_C(0x4886ed1e5846c407) /*  806 */, U64_C(0x28cddb791eb70b04) /*  807 */,
  U64_C(0xc2b00be2f573417f) /*  808 */, U64_C(0x5c9590452180f877) /*  809 */,
  U64_C(0x7a6bddfff370eb00) /*  810 */, U64_C(0xce509e38d6d9d6a4) /*  811 */,
  U64_C(0xebeb0f00647fa702) /*  812 */, U64_C(0x1dcc06cf76606f06) /*  813 */,
  U64_C(0xe4d9f28ba286ff0a) /*  814 */, U64_C(0xd85a305dc918c262) /*  815 */,
  U64_C(0x475b1d8732225f54) /*  816 */, U64_C(0x2d4fb51668ccb5fe) /*  817 */,
  U64_C(0xa679b9d9d72bba20) /*  818 */, U64_C(0x53841c0d912d43a5) /*  819 */,
  U64_C(0x3b7eaa48bf12a4e8) /*  820 */, U64_C(0x781e0e47f22f1ddf) /*  821 */,
  U64_C(0xeff20ce60ab50973) /*  822 */, U64_C(0x20d261d19dffb742) /*  823 */,
  U64_C(0x16a12b03062a2e39) /*  824 */, U64_C(0x1960eb2239650495) /*  825 */,
  U64_C(0x251c16fed50eb8b8) /*  826 */, U64_C(0x9ac0c330f826016e) /*  827 */,
  U64_C(0xed152665953e7671) /*  828 */, U64_C(0x02d63194a6369570) /*  829 */,
  U64_C(0x5074f08394b1c987) /*  830 */, U64_C(0x70ba598c90b25ce1) /*  831 */,
  U64_C(0x794a15810b9742f6) /*  832 */, U64_C(0x0d5925e9fcaf8c6c) /*  833 */,
  U64_C(0x3067716cd868744e) /*  834 */, U64_C(0x910ab077e8d7731b) /*  835 */,
  U64_C(0x6a61bbdb5ac42f61) /*  836 */, U64_C(0x93513efbf0851567) /*  837 */,
  U64_C(0xf494724b9e83e9d5) /*  838 */, U64_C(0xe887e1985c09648d) /*  839 */,
  U64_C(0x34b1d3c675370cfd) /*  840 */, U64_C(0xdc35e433bc0d255d) /*  841 */,
  U64_C(0xd0aab84234131be0) /*  842 */, U64_C(0x08042a50b48b7eaf) /*  843 */,
  U64_C(0x9997c4ee44a3ab35) /*  844 */, U64_C(0x829a7b49201799d0) /*  845 */,
  U64_C(0x263b8307b7c54441) /*  846 */, U64_C(0x752f95f4fd6a6ca6) /*  847 */,
  U64_C(0x927217402c08c6e5) /*  848 */, U64_C(0x2a8ab754a795d9ee) /*  849 */,
  U64_C(0xa442f7552f72943d) /*  850 */, U64_C(0x2c31334e19781208) /*  851 */,
  U64_C(0x4fa98d7ceaee6291) /*  852 */, U64_C(0x55c3862f665db309) /*  853 */,
  U64_C(0xbd0610175d53b1f3) /*  854 */, U64_C(0x46fe6cb840413f27) /*  855 */,
  U64_C(0x3fe03792df0cfa59) /*  856 */, U64_C(0xcfe700372eb85e8f) /*  857 */,
  U64_C(0xa7be29e7adbce118) /*  858 */, U64_C(0xe544ee5cde8431dd) /*  859 */,
  U64_C(0x8a781b1b41f1873e) /*  860 */, U64_C(0xa5c94c78a0d2f0e7) /*  861 */,
  U64_C(0x39412e2877b60728) /*  862 */, U64_C(0xa1265ef3afc9a62c) /*  863 */,
  U64_C(0xbcc2770c6a2506c5) /*  864 */, U64_C(0x3ab66dd5dce1ce12) /*  865 */,
  U64_C(0xe65499d04a675b37) /*  866 */, U64_C(0x7d8f523481bfd216) /*  867 */,
  U64_C(0x0f6f64fcec15f389) /*  868 */, U64_C(0x74efbe618b5b13c8) /*  869 */,
  U64_C(0xacdc82b714273e1d) /*  870 */, U64_C(0xdd40bfe003199d17) /*  871 */,
  U64_C(0x37e99257e7e061f8) /*  872 */, U64_C(0xfa52626904775aaa) /*  873 */,
  U64_C(0x8bbbf63a463d56f9) /*  874 */, U64_C(0xf0013f1543a26e64) /*  875 */,
  U64_C(0xa8307e9f879ec898) /*  876 */, U64_C(0xcc4c27a4150177cc) /*  877 */,
  U64_C(0x1b432f2cca1d3348) /*  878 */, U64_C(0xde1d1f8f9f6fa013) /*  879 */,
  U64_C(0x606602a047a7ddd6) /*  880 */, U64_C(0xd237ab64cc1cb2c7) /*  881 */,
  U64_C(0x9b938e7225fcd1d3) /*  882 */, U64_C(0xec4e03708e0ff476) /*  883 */,
  U64_C(0xfeb2fbda3d03c12d) /*  884 */, U64_C(0xae0bced2ee43889a) /*  885 */,
  U64_C(0x22cb8923ebfb4f43) /*  886 */, U64_C(0x69360d013cf7396d) /*  887 */,
  U64_C(0x855e3602d2d4e022) /*  888 */, U64_C(0x073805bad01f784c) /*  889 */,
  U64_C(0x33e17a133852f546) /*  890 */, U64_C(0xdf4874058ac7b638) /*  891 */,
  U64_C(0xba92b29c678aa14a) /*  892 */, U64_C(0x0ce89fc76cfaadcd) /*  893 */,
  U64_C(0x5f9d4e0908339e34) /*  894 */, U64_C(0xf1afe9291f5923b9) /*  895 */,
  U64_C(0x6e3480f60f4a265f) /*  896 */, U64_C(0xeebf3a2ab29b841c) /*  897 */,
  U64_C(0xe21938a88f91b4ad) /*  898 */, U64_C(0x57dfeff845c6d3c3) /*  899 */,
  U64_C(0x2f006b0bf62caaf2) /*  900 */, U64_C(0x62f479ef6f75ee78) /*  901 */,
  U64_C(0x11a55ad41c8916a9) /*  902 */, U64_C(0xf229d29084fed453) /*  903 */,
  U64_C(0x42f1c27b16b000e6) /*  904 */, U64_C(0x2b1f76749823c074) /*  905 */,
  U64_C(0x4b76eca3c2745360) /*  906 */, U64_C(0x8c98f463b91691bd) /*  907 */,
  U64_C(0x14bcc93cf1ade66a) /*  908 */, U64_C(0x8885213e6d458397) /*  909 */,
  U64_C(0x8e177df0274d4711) /*  910 */, U64_C(0xb49b73b5503f2951) /*  911 */,
  U64_C(0x10168168c3f96b6b) /*  912 */, U64_C(0x0e3d963b63cab0ae) /*  913 */,
  U64_C(0x8dfc4b5655a1db14) /*  914 */, U64_C(0xf789f1356e14de5c) /*  915 */,
  U64_C(0x683e68af4e51dac1) /*  916 */, U64_C(0xc9a84f9d8d4b0fd9) /*  917 */,
  U64_C(0x3691e03f52a0f9d1) /*  918 */, U64_C(0x5ed86e46e1878e80) /*  919 */,
  U64_C(0x3c711a0e99d07150) /*  920 */, U64_C(0x5a0865b20c4e9310) /*  921 */,
  U64_C(0x56fbfc1fe4f0682e) /*  922 */, U64_C(0xea8d5de3105edf9b) /*  923 */,
  U64_C(0x71abfdb12379187a) /*  924 */, U64_C(0x2eb99de1bee77b9c) /*  925 */,
  U64_C(0x21ecc0ea33cf4523) /*  926 */, U64_C(0x59a4d7521805c7a1) /*  927 */,
  U64_C(0x3896f5eb56ae7c72) /*  928 */, U64_C(0xaa638f3db18f75dc) /*  929 */,
  U64_C(0x9f39358dabe9808e) /*  930 */, U64_C(0xb7defa91c00b72ac) /*  931 */,
  U64_C(0x6b5541fd62492d92) /*  932 */, U64_C(0x6dc6dee8f92e4d5b) /*  933 */,
  U64_C(0x353f57abc4beea7e) /*  934 */, U64_C(0x735769d6da5690ce) /*  935 */,
  U64_C(0x0a234aa642391484) /*  936 */, U64_C(0xf6f9508028f80d9d) /*  937 */,
  U64_C(0xb8e319a27ab3f215) /*  938 */, U64_C(0x31ad9c1151341a4d) /*  939 */,
  U64_C(0x773c22a57bef5805) /*  940 */, U64_C(0x45c7561a07968633) /*  941 */,
  U64_C(0xf913da9e249dbe36) /*  942 */, U64_C(0xda652d9b78a64c68) /*  943 */,
  U64_C(0x4c27a97f3bc334ef) /*  944 */, U64_C(0x76621220e66b17f4) /*  945 */,
  U64_C(0x967743899acd7d0b) /*  946 */, U64_C(0xf3ee5bcae0ed6782) /*  947 */,
  U64_C(0x409f753600c879fc) /*  948 */, U64_C(0x06d09a39b5926db6) /*  949 */,
  U64_C(0x6f83aeb0317ac588) /*  950 */, U64_C(0x01e6ca4a86381f21) /*  951 */,
  U64_C(0x66ff3462d19f3025) /*  952 */, U64_C(0x72207c24ddfd3bfb) /*  953 */,
  U64_C(0x4af6b6d3e2ece2eb) /*  954 */, U64_C(0x9c994dbec7ea08de) /*  955 */,
  U64_C(0x49ace597b09a8bc4) /*  956 */, U64_C(0xb38c4766cf0797ba) /*  957 */,
  U64_C(0x131b9373c57c2a75) /*  958 */, U64_C(0xb1822cce61931e58) /*  959 */,
  U64_C(0x9d7555b909ba1c0c) /*  960 */, U64_C(0x127fafdd937d11d2) /*  961 */,
  U64_C(0x29da3badc66d92e4) /*  962 */, U64_C(0xa2c1d57154c2ecbc) /*  963 */,
  U64_C(0x58c5134d82f6fe24) /*  964 */, U64_C(0x1c3ae3515b62274f) /*  965 */,
  U64_C(0xe907c82e01cb8126) /*  966 */, U64_C(0xf8ed091913e37fcb) /*  967 */,
  U64_C(0x3249d8f9c80046c9) /*  968 */, U64_C(0x80cf9bede388fb63) /*  969 */,
  U64_C(0x1881539a116cf19e) /*  970 */, U64_C(0x5103f3f76bd52457) /*  971 */,
  U64_C(0x15b7e6f5ae47f7a8) /*  972 */, U64_C(0xdbd7c6ded47e9ccf) /*  973 */,
  U64_C(0x44e55c410228bb1a) /*  974 */, U64_C(0xb647d4255edb4e99) /*  975 */,
  U64_C(0x5d11882bb8aafc30) /*  976 */, U64_C(0xf5098bbb29d3212a) /*  977 */,
  U64_C(0x8fb5ea14e90296b3) /*  978 */, U64_C(0x677b942157dd025a) /*  979 */,
  U64_C(0xfb58e7c0a390acb5) /*  980 */, U64_C(0x89d3674c83bd4a01) /*  981 */,
  U64_C(0x9e2da4df4bf3b93b) /*  982 */, U64_C(0xfcc41e328cab4829) /*  983 */,
  U64_C(0x03f38c96ba582c52) /*  984 */, U64_C(0xcad1bdbd7fd85db2) /*  985 */,
  U64_C(0xbbb442c16082ae83) /*  986 */, U64_C(0xb95fe86ba5da9ab0) /*  987 */,
  U64_C(0xb22e04673771a93f) /*  988 */, U64_C(0x845358c9493152d8) /*  989 */,
  U64_C(0xbe2a488697b4541e) /*  990 */, U64_C(0x95a2dc2dd38e6966) /*  991 */,
  U64_C(0xc02c11ac923c852b) /*  992 */, U64_C(0x2388b1990df2a87b) /*  993 */,
  U64_C(0x7c8008fa1b4f37be) /*  994 */, U64_C(0x1f70d0c84d54e503) /*  995 */,
  U64_C(0x5490adec7ece57d4) /*  996 */, U64_C(0x002b3c27d9063a3a) /*  997 */,
  U64_C(0x7eaea3848030a2bf) /*  998 */, U64_C(0xc602326ded2003c0) /*  999 */,
  U64_C(0x83a7287d69a94086) /* 1000 */, U64_C(0xc57a5fcb30f57a8a) /* 1001 */,
  U64_C(0xb56844e479ebe779) /* 1002 */, U64_C(0xa373b40f05dcbce9) /* 1003 */,
  U64_C(0xd71a786e88570ee2) /* 1004 */, U64_C(0x879cbacdbde8f6a0) /* 1005 */,
  U64_C(0x976ad1bcc164a32f) /* 1006 */, U64_C(0xab21e25e9666d78b) /* 1007 */,
  U64_C(0x901063aae5e5c33c) /* 1008 */, U64_C(0x9818b34448698d90) /* 1009 */,
  U64_C(0xe36487ae3e1e8abb) /* 1010 */, U64_C(0xafbdf931893bdcb4) /* 1011 */,
  U64_C(0x6345a0dc5fbbd519) /* 1012 */, U64_C(0x8628fe269b9465ca) /* 1013 */,
  U64_C(0x1e5d01603f9c51ec) /* 1014 */, U64_C(0x4de44006a15049b7) /* 1015 */,
  U64_C(0xbf6c70e5f776cbb1) /* 1016 */, U64_C(0x411218f2ef552bed) /* 1017 */,
  U64_C(0xcb0c0708705a36a3) /* 1018 */, U64_C(0xe74d14754f986044) /* 1019 */,
  U64_C(0xcd56d9430ea8280e) /* 1020 */, U64_C(0xc12591d7535f5065) /* 1021 */,
  U64_C(0xc83223f1720aef96) /* 1022 */, U64_C(0xc3a0396f7363a51f) /* 1023 */
};

static void
tiger_init( void *context )
{
  TIGER_CONTEXT *hd = context;

  hd->a = 0x0123456789abcdefLL;
  hd->b = 0xfedcba9876543210LL;
  hd->c = 0xf096a5b4c3b2e187LL;
  hd->nblocks = 0;
  hd->count = 0;
}

static void
tiger_round( u64 *ra, u64 *rb, u64 *rc, u64 x, int mul )
{
  u64 a = *ra;
  u64 b = *rb;
  u64 c = *rc;
  
  c ^= x;
  a -= (  sbox1[  c        & 0xff ] ^ sbox2[ (c >> 16) & 0xff ]
        ^ sbox3[ (c >> 32) & 0xff ] ^ sbox4[ (c >> 48) & 0xff ]);
  b += (  sbox4[ (c >>  8) & 0xff ] ^ sbox3[ (c >> 24) & 0xff ]
        ^ sbox2[ (c >> 40) & 0xff ] ^ sbox1[ (c >> 56) & 0xff ]);
  b *= mul;
  
  *ra = a;
  *rb = b;
  *rc = c;
}


static void
pass( u64 *ra, u64 *rb, u64 *rc, u64 *x, int mul )
{
  u64 a = *ra;
  u64 b = *rb;
  u64 c = *rc;

  tiger_round( &a, &b, &c, x[0], mul );
  tiger_round( &b, &c, &a, x[1], mul );
  tiger_round( &c, &a, &b, x[2], mul );
  tiger_round( &a, &b, &c, x[3], mul );
  tiger_round( &b, &c, &a, x[4], mul );
  tiger_round( &c, &a, &b, x[5], mul );
  tiger_round( &a, &b, &c, x[6], mul );
  tiger_round( &b, &c, &a, x[7], mul );

  *ra = a;
  *rb = b;
  *rc = c;
}


static void
key_schedule( u64 *x )
{
  x[0] -= x[7] ^ 0xa5a5a5a5a5a5a5a5LL;
  x[1] ^= x[0];
  x[2] += x[1];
  x[3] -= x[2] ^ ((~x[1]) << 19 );
  x[4] ^= x[3];
  x[5] += x[4];
  x[6] -= x[5] ^ ((~x[4]) >> 23 );
  x[7] ^= x[6];
  x[0] += x[7];
  x[1] -= x[0] ^ ((~x[7]) << 19 );
  x[2] ^= x[1];
  x[3] += x[2];
  x[4] -= x[3] ^ ((~x[2]) >> 23 );
  x[5] ^= x[4];
  x[6] += x[5];
  x[7] -= x[6] ^ 0x0123456789abcdefLL;
}


/****************
 * Transform the message DATA which consists of 512 bytes (8 words)
 */
static void
transform ( TIGER_CONTEXT *hd, const unsigned char *data )
{
  u64 a,b,c,aa,bb,cc;
  u64 x[8];
#ifdef WORDS_BIGENDIAN
#define MKWORD(d,n) \
		(  ((u64)(d)[8*(n)+7]) << 56 | ((u64)(d)[8*(n)+6]) << 48  \
		 | ((u64)(d)[8*(n)+5]) << 40 | ((u64)(d)[8*(n)+4]) << 32  \
		 | ((u64)(d)[8*(n)+3]) << 24 | ((u64)(d)[8*(n)+2]) << 16  \
		 | ((u64)(d)[8*(n)+1]) << 8  | ((u64)(d)[8*(n)	])	 )
  x[0] = MKWORD(data, 0);
  x[1] = MKWORD(data, 1);
  x[2] = MKWORD(data, 2);
  x[3] = MKWORD(data, 3);
  x[4] = MKWORD(data, 4);
  x[5] = MKWORD(data, 5);
  x[6] = MKWORD(data, 6);
  x[7] = MKWORD(data, 7);
#undef MKWORD
#else
  memcpy( &x[0], data, 64 );
#endif

  /* save */
  a = aa = hd->a;
  b = bb = hd->b;
  c = cc = hd->c;

  pass( &a, &b, &c, x, 5);
  key_schedule( x );
  pass( &c, &a, &b, x, 7);
  key_schedule( x );
  pass( &b, &c, &a, x, 9);

  /* feedforward */
  a ^= aa;
  b -= bb;
  c += cc;
  /* store */
  hd->a = a;
  hd->b = b;
  hd->c = c;
}



/* Update the message digest with the contents
 * of INBUF with length INLEN.
 */
static void
tiger_write ( void *context, const void *inbuf_arg, size_t inlen)
{
  const unsigned char *inbuf = inbuf_arg;
  TIGER_CONTEXT *hd = context;

  if( hd->count == 64 ) /* flush the buffer */
    {
      transform( hd, hd->buf );
      _gcry_burn_stack (21*8+11*sizeof(void*));
      hd->count = 0;
      hd->nblocks++;
    }
  if( !inbuf )
    return;
  if( hd->count ) 
    {
      for( ; inlen && hd->count < 64; inlen-- )
        hd->buf[hd->count++] = *inbuf++;
      tiger_write( hd, NULL, 0 );
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
  _gcry_burn_stack (21*8+11*sizeof(void*));
  for( ; inlen && hd->count < 64; inlen-- )
    hd->buf[hd->count++] = *inbuf++;
}



/* The routine terminates the computation
 */
static void
tiger_final( void *context )
{
  TIGER_CONTEXT *hd = context;
  u32 t, msb, lsb;
  byte *p;

  tiger_write(hd, NULL, 0); /* flush */;

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
      hd->buf[hd->count++] = 0x01; /* pad */
      while( hd->count < 56 )
        hd->buf[hd->count++] = 0;  /* pad */
    }
  else  /* need one extra block */
    {
      hd->buf[hd->count++] = 0x01; /* pad character */
      while( hd->count < 64 )
        hd->buf[hd->count++] = 0;
      tiger_write(hd, NULL, 0);  /* flush */;
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
  _gcry_burn_stack (21*8+11*sizeof(void*));

  p = hd->buf;
#ifdef WORDS_BIGENDIAN
#define X(a) do { *(u64*)p = hd->a ; p += 8; } while(0)
#else /* little endian */
#define X(a) do { *p++ = hd->a >> 56; *p++ = hd->a >> 48; \
	          *p++ = hd->a >> 40; *p++ = hd->a >> 32; \
	          *p++ = hd->a >> 24; *p++ = hd->a >> 16; \
	          *p++ = hd->a >>  8; *p++ = hd->a;       } while(0)
#endif
  X(a);
  X(b);
  X(c);
#undef X
}

static byte *
tiger_read( void *context )
{
  TIGER_CONTEXT *hd = context;

  return hd->buf;
}

static byte asn[19] = /* Object ID is 1.3.6.1.4.1.11591.12.2 */
  { 0x30, 0x29, 0x30, 0x0d, 0x06, 0x09, 0x2b, 0x06,
    0x01, 0x04, 0x01, 0xda, 0x47, 0x0c, 0x02,
    0x05, 0x00, 0x04, 0x18 };

static gcry_md_oid_spec_t oid_spec_tiger[] =
  {
    /* GNU.digestAlgorithm TIGER */
    { "1.3.6.1.4.1.11591.12.2" },
    { NULL }
  };

gcry_md_spec_t _gcry_digest_spec_tiger =
  {
    "TIGER192", asn, DIM (asn), oid_spec_tiger, 24,
    tiger_init, tiger_write, tiger_final, tiger_read,
    sizeof (TIGER_CONTEXT)
  };

#endif /* HAVE_U64_TYPEDEF */
