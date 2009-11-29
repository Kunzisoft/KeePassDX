/* fips186-dsa.c - FIPS 186 DSA tests
 *	Copyright (C) 2008 Free Software Foundation, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#ifdef _GCRYPT_IN_LIBGCRYPT
# include "../src/gcrypt.h"
#else
# include <gcrypt.h>
#endif


#define my_isascii(c) (!((c) & 0x80))
#define digitp(p)   (*(p) >= '0' && *(p) <= '9')
#define hexdigitp(a) (digitp (a)                     \
                      || (*(a) >= 'A' && *(a) <= 'F')  \
                      || (*(a) >= 'a' && *(a) <= 'f'))
#define xtoi_1(p)   (*(p) <= '9'? (*(p)- '0'): \
                     *(p) <= 'F'? (*(p)-'A'+10):(*(p)-'a'+10))
#define xtoi_2(p)   ((xtoi_1(p) * 16) + xtoi_1((p)+1))
#define DIM(v)		     (sizeof(v)/sizeof((v)[0]))
#define DIMof(type,member)   DIM(((type *)0)->member)

static int verbose;
static int error_count;

static void
info (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
}

static void
fail (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  error_count++;
}

static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}

static void
show_sexp (const char *prefix, gcry_sexp_t a)
{
  char *buf;
  size_t size;

  if (prefix)
    fputs (prefix, stderr);
  size = gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, NULL, 0);
  buf = gcry_xmalloc (size);

  gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, buf, size);
  fprintf (stderr, "%.*s", (int)size, buf);
  gcry_free (buf);
}

static gcry_mpi_t
mpi_from_string (const char *string)
{
  gpg_error_t err;
  gcry_mpi_t a;

  err = gcry_mpi_scan (&a, GCRYMPI_FMT_HEX, string, 0, NULL);
  if (err)
    die ("error converting string to mpi: %s\n", gpg_strerror (err));
  return a;
}

/* Convert STRING consisting of hex characters into its binary
   representation and return it as an allocated buffer. The valid
   length of the buffer is returned at R_LENGTH.  The string is
   delimited by end of string.  The function returns NULL on
   error.  */
static void *
data_from_hex (const char *string, size_t *r_length)
{
  const char *s;
  unsigned char *buffer;
  size_t length;

  buffer = gcry_xmalloc (strlen(string)/2+1);
  length = 0;
  for (s=string; *s; s +=2 )
    {
      if (!hexdigitp (s) || !hexdigitp (s+1))
        die ("error parsing hex string `%s'\n", string);
      ((unsigned char*)buffer)[length++] = xtoi_2 (s);
    }
  *r_length = length;
  return buffer;
}


static void
extract_cmp_mpi (gcry_sexp_t sexp, const char *name, const char *expected)
{
  gcry_sexp_t l1;
  gcry_mpi_t a, b;

  l1 = gcry_sexp_find_token (sexp, name, 0);
  a = gcry_sexp_nth_mpi (l1, 1, GCRYMPI_FMT_USG);
  b = mpi_from_string (expected);
  if (!a) 
    fail ("parameter \"%s\" missing in key\n", name);
  else if ( gcry_mpi_cmp (a, b) )
    fail ("parameter \"%s\" does not match expected value\n", name);
  gcry_mpi_release (b);
  gcry_mpi_release (a);
  gcry_sexp_release (l1);
}


static void
extract_cmp_data (gcry_sexp_t sexp, const char *name, const char *expected)
{
  gcry_sexp_t l1;
  const void *a;
  size_t alen;
  void *b;
  size_t blen;

  l1 = gcry_sexp_find_token (sexp, name, 0);
  a = gcry_sexp_nth_data (l1, 1, &alen);
  b = data_from_hex (expected, &blen);
  if (!a) 
    fail ("parameter \"%s\" missing in key\n", name);
  else if ( alen != blen || memcmp (a, b, alen) )
    fail ("parameter \"%s\" does not match expected value\n", name);
  gcry_free (b);
  gcry_sexp_release (l1);
}

static void
extract_cmp_int (gcry_sexp_t sexp, const char *name, int expected)
{
  gcry_sexp_t l1;
  char *a;

  l1 = gcry_sexp_find_token (sexp, name, 0);
  a = gcry_sexp_nth_string (l1, 1);
  if (!a) 
    fail ("parameter \"%s\" missing in key\n", name);
  else if ( strtoul (a, NULL, 10) != expected )
    fail ("parameter \"%s\" does not match expected value\n", name);
  gcry_free (a);
  gcry_sexp_release (l1);
}


static void
check_dsa_gen_186_2 (void)
{
  static struct {
    int nbits;
    const char *p, *q, *g;
    const char *seed;
    int counter;
    const char *h;
  } tbl[] = {
    /* These tests are from FIPS 186-2, B.3.1.  */
    {
      1024,
      "d3aed1876054db831d0c1348fbb1ada72507e5fbf9a62cbd47a63aeb7859d6921"
      "4adeb9146a6ec3f43520f0fd8e3125dd8bbc5d87405d1ac5f82073cd762a3f8d7"
      "74322657c9da88a7d2f0e1a9ceb84a39cb40876179e6a76e400498de4bb9379b0"
      "5f5feb7b91eb8fea97ee17a955a0a8a37587a272c4719d6feb6b54ba4ab69",
      "9c916d121de9a03f71fb21bc2e1c0d116f065a4f",
      "8157c5f68ca40b3ded11c353327ab9b8af3e186dd2e8dade98761a0996dda99ab"
      "0250d3409063ad99efae48b10c6ab2bba3ea9a67b12b911a372a2bba260176fad"
      "b4b93247d9712aad13aa70216c55da9858f7a298deb670a403eb1e7c91b847f1e"
      "ccfbd14bd806fd42cf45dbb69cd6d6b43add2a78f7d16928eaa04458dea44",
      "0cb1990c1fd3626055d7a0096f8fa99807399871",
      98,
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "0000000000000000000000000000000000000000000000000000000000002"
    },
    {
      1024,
      "f5c73304080353357de1b5967597c27d65f70aa2fe9b6aed1d0afc2b499adf22f"
      "8e37937096d88548ac36c4a067f8353c7fed73f96f0d688b19b0624aedbae5dbb"
      "0ee8835a4c269288c0e1d69479e701ee266bb767af39d748fe7d6afc73fdf44be"
      "3eb6e661e599670061203e75fc8b3dbd59e40b54f358d0097013a0f3867f9",
      "f8751166cf4f6f3b07c081fd2a9071f23ca1988d",
      "1e288a442e02461c418ed67a66d24cacbeb8936fbde62ff995f5fd569dee6be62"
      "4e4f0f9f8c8093f5d192ab3b3f9ae3f2665d95d27fb10e382f45cd356e7f4eb7a"
      "665db432113ed06478f93b7cf188ec7a1ee97aec8f91ea7bfceaf8b6e7e5a349c"
      "4ad3225362ef440c57cbc6e69df15b6699caac85f733555075f04781b2b33",
      "34b3520d45d240a8861b82c8b61ffa16e67b5cce",
      622,
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "0000000000000000000000000000000000000000000000000000000000002",
    },
    {
      1024,
      "c6c6f4f4eed927fb1c3b0c81010967e530658e6f9698ebe058b4f47b2dc8fcbc7"
      "b69296b9e8b6cf55681181fe72492668061b262b0046a0d409902e269b0cb69a4"
      "55ed1a086caf41927f5912bf0e0cbc45ee81a4f98bf6146f6168a228aec80e9cc"
      "1162d6f6aa412efe82d4f18b95e34ab790daac5bd7aef0b22fa08ba5dbaad",
      "d32b29f065c1394a30490b6fcbf812a32a8634ab",
      "06f973c879e2e89345d0ac04f9c34ad69b9eff1680f18d1c8f3e1596c2e8fa8e1"
      "ecef6830409e9012d4788bef6ec7414d09c981b47c941b77f39dfc49caff5e714"
      "c97abe25a7a8b5d1fe88700bb96eff91cca64d53700a28b1146d81bad1212d231"
      "80154c95a01f5aeebb553a8365c38a5ebe05539b51734233776ce9aff98b2",
      "b6ec750da2f824cb42c5f7e28c81350d97f75125",
      185,
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "0000000000000000000000000000000000000000000000000000000000002",
    },
    {
      1024,
      "b827a9dc9221a6ed1bec7b64d61232aacb2812f888b0a0b3a95033d7a22e77d0b"
      "ff23bfeed0fb1281b21b8ff7421f0c727d1fb8aa2b843d6885f067e763f83d41f"
      "d800ab15a7e2b12f71ec2058ee7bd62cd72c26989b272e519785da57bfa1f974b"
      "c652e1a2d6cfb68477de5635fd019b37add656cff0b802558b31b6d2851e5",
      "de822c03445b77cec4ad3a6fb0ca39ff97059ddf",
      "65a9e2d43a378d7063813104586868cacf2fccd51aec1e0b6af8ba3e66dee6371"
      "681254c3fb5e3929d65e3c4bcd20abd4ddc7cf815623e17b9fc92f02b8d44278b"
      "848480ffd193104cf5612639511e45bd247708ff6028bd3824f8844c263b46c69"
      "1f2076f8cd13c5d0be95f1f2a1a17ab1f7e5bc73500bac27d57b473ba9748",
      "cd2221dd73815a75224e9fde7faf52829b81ac7a",
      62,
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "0000000000000000000000000000000000000000000000000000000000002",
    },
    {
      1024,
      "898a8d93e295c8ef2ffd46976225a1543640640d155a576fafa0be32136165803"
      "ba2eff2782a2be75cc9ec65db6bd3238cca695b3a5a14726a2a314775c377d891"
      "354b3de6c89e714a05599ca04132c987f889f72c4fe298ccb31f711c03b07e1d9"
      "8d72af590754cf3847398b60cecd55a4611692b308809560a83880404c227",
      "c6d786643d2acfc6b8d576863fda8cfbfbd5e03f",
      "2fd38b8d21c58e8fb5315a177b8d5dc4c450d574e69348b7b9da367c26e72438d"
      "af8372e7f0bee84ef5dcbbc3727194a2228431192f1779be24837f22a0e14d10d"
      "5344da1b8b403df9f9b2655095b3d0f67418ed6cd989f35aa4232e4b7001764fb"
      "e85d6b2c716980f13272fc4271ac1e234f7e24c023cfc2d2dc0aa1e9af2fb",
      "73483e697599871af983a281e3afa22e0ed86b68",
      272,
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "00000000000000000000000000000000000000000000000000000000000000000"
      "0000000000000000000000000000000000000000000000000000000000002",
    },

    /* These tests are generated by the OpenSSL FIPS version.  */
    { 
      1024,
      "A404363903FDCE86839BCFD953AAD2DA2B0E70CAED3B5FF5D68F15A1C4BB0A793C"
      "A9D58FC956804C5901DE0AF99F345ED1A8617C687864BAC044B7C3C3E732A2B255"
      "EC986AA76EA8CB0E0815B3E0E605650AF7D8058EE7E8EBCDEFFDAB8100D3FC1033"
      "11BA3AB232EF06BB74BA9A949EC0C7ED324C19B202F4AB725BBB4080C9",
      "C643946CEA8748E12D430C48DB038F9165814389",
      "59B7E7BA0033CCE8E6837173420FBB382A784D4154A3C166043F5A68CB92945D16"
      "892D4CC5585F2D28C780E75A6C20A379E2B58304C1E5FC0D8C15E4E89C4498C8BC"
      "B90FB36ED8DC0489B9D0BC09EC4411FB0BFADF25485EEAB6700BE0ACF5C44A6ED7"
      "44A015382FF9B8DA7EAA00DEA135FADC59212DBBFFC1537336FA4B7225",
      "02708ab36e3f0bfd67ec3b8bd8829d03b84f56bd",
      50,
      "02"
    },
    {
      1024,
      "9C664033DB8B203D826F896D2293C62EF9351D5CFD0F4C0AD7EFDA4DDC7F15987"
      "6A3C68CAB2586B44FD1BD4DEF7A17905D88D321DD77C4E1720D848CA21D79F9B3"
      "D8F537338E09B44E9F481E8DA3C56569F63146596A050EF8FAEE8ACA32C666450"
      "04F675C8806EB4025B0A5ECC39CE89983EA40A183A7CF5208BA958045ABD5",
      "AD0D8CBA369AF6CD0D2BAC0B4CFCAF0A1F9BCDF7",
      "74D717F7092A2AF725FDD6C2561D1DBE5AEE40203C638BA8B9F49003857873701"
      "95A44E515C4E8B344F5CDC7F4A6D38097CD57675E7643AB9700692C69F0A99B0E"
      "039FDDDFCA8CEB607BDB4ADF2834DE1690F5823FC8199FB8F6F29E5A583B6786A"
      "C14C7E67106C3B30568CBB9383F89287D578159778EB18216799D16D46498",
      "6481a12a50384888ee84b61024f7c9c685d6ac96",
      289,
      "02"
    },
    {
      1024,

      "B0DFB602EB8462B1DC8C2214A52B587D3E6842CCF1C38D0F7C7F967ED30CF6828"
      "1E2675B3BAB594755FB1634E66B4C23936F0725A358F8DFF3C307E2601FD66D63"
      "5B17270450C50BD2BEC29E0E9A471DF1C15B0191517952268A2763D4BD28B8503"
      "B3399686272B76B11227F693D7833105EF70C2289C3194CF4527024B272DF",
      "EA649C04911FAB5A41440287A517EF752A40354B",
      "88C5A4563ECB949763E0B696CD04B21321360F54C0EE7B23E2CEDC30E9E486162"
      "01BFB1619E7C54B653D1F890C50E04B29205F5E3E2F93A13B0751AF25491C5194"
      "93C09DDF6B9C173B3846DFB0E7A5C870BBFC78419260C90E20315410691C8326C"
      "858D7063E7921F3F601158E912C7EE487FF259202BEEB10F6D9E99190F696",
      "5bf9d17bc62fbbf3d569c92bd4505586b2e5ef1a",
      626,
      "02"
    },
    {
      1024,
      "F783C08D7F9463E48BA87893805C4B34B63C85DF7EBDD9EBEE94DB4AF4E4A415C"
      "F0F3793AE55096BA1199598798FA8403B28DED7F7C7AFD54FD535861A0150EF4D"
      "5871465B13837CCF46BEB0A22F8D38DC7D6AE0E14A3845FD0C027CFA97791B977"
      "CE2808BAD9B43CE69390C0F40016056722D82C0D7B1B27413D026A39D7DAD",
      "A40D9EE456AED4C8A653FDB47B6629C0B843FE8F",
      "DF876263E21F263AE6DA57409BD517DCEADB9216048F066D6B58867F8E59A5EEE"
      "700283A946C1455534618979BE6C227673C1B803910262BD93BC94D5089850614"
      "F3E29AB64E8C989A7E3E28FE670FFA3EE21DEEEC1AB0B60E1D8E2AA39663BADD7"
      "2C9F957D7F3D4F17D9FDAD050EB373A6DEFD09F5DA752EAFE046836E14B67",
      "8a9a57706f69f4f566252cdf6d5cbfdf2020150b",
      397,
      "02"
    },
    { 
      1024,
      "D40E4F6461E145859CCF60FD57962840BD75FFF12C22F76626F566842252AD068"
      "29745F0147056354F6C016CF12762B0E331787925B8128CF5AF81F9B176A51934"
      "96D792430FF83C7B79BD595BDA10787B34600787FA552EFE3662F37B99AAD3F3A"
      "093732680A01345192A19BECCE6BF5D498E44ED6BED5B0BA72AAD49E8276B",
      "D12F1BD0AA78B99247FD9F18EAFEE5C136686EA5",
      "468EBD20C99449C1E440E6F8E452C6A6BC7551C555FE5E94996E20CFD4DA3B9CC"
      "58499D6CC2374CCF9C392715A537DE10CFCA8A6A37AFBD187CF6B88D26881E5F5"
      "7521D9D2C9BBA51E7B87B070BBE73F5C5FE31E752CAF88183516D8503BAAC1159"
      "928EF50DEE52D96F396B93FB4138D786464C315401A853E57C9A0F9D25839",
      "30b3599944a914a330a3f49d11ec88f555422aef",
      678,
      "02"
    }
  };
  gpg_error_t err;
  int tno;
  gcry_sexp_t key_spec, key, pub_key, sec_key, seed_values;
  gcry_sexp_t l1;

  for (tno = 0; tno < DIM (tbl); tno++)
    {
      if (verbose)
        info ("generating FIPS 186-2 test key %d\n", tno);

      {
        void *data;
        size_t datalen;

        data = data_from_hex (tbl[tno].seed, &datalen);
        err = gcry_sexp_build (&key_spec, NULL,
                               "(genkey (dsa (nbits %d)(use-fips186-2)"
                               "(derive-parms(seed %b))))",
                               tbl[tno].nbits, (int)datalen, data);
        gcry_free (data);
      }
      if (err)
        die ("error creating S-expression %d: %s\n", tno, gpg_strerror (err));

      err = gcry_pk_genkey (&key, key_spec);
      gcry_sexp_release (key_spec);
      if (err)
        {
          fail ("error generating key %d: %s\n", tno, gpg_strerror (err));
          continue;
        }
      
      if (verbose > 1)
        show_sexp ("generated key:\n", key);

      pub_key = gcry_sexp_find_token (key, "public-key", 0);
      if (!pub_key)
        fail ("public part missing in key %d\n", tno);

      sec_key = gcry_sexp_find_token (key, "private-key", 0);
      if (!sec_key)
        fail ("private part missing in key %d\n", tno);

      l1 = gcry_sexp_find_token (key, "misc-key-info", 0);
      if (!l1)
        fail ("misc_key_info part missing in key %d\n", tno);
      seed_values = gcry_sexp_find_token (l1, "seed-values", 0);
      if (!seed_values)
        fail ("seed-values part missing in key %d\n", tno);
      gcry_sexp_release (l1);

      extract_cmp_mpi (sec_key, "p", tbl[tno].p);
      extract_cmp_mpi (sec_key, "q", tbl[tno].q);
      extract_cmp_mpi (sec_key, "g", tbl[tno].g);

      extract_cmp_data (seed_values, "seed", tbl[tno].seed);
      extract_cmp_int (seed_values, "counter", tbl[tno].counter);
      extract_cmp_mpi (seed_values, "h", tbl[tno].h);

      gcry_sexp_release (seed_values);
      gcry_sexp_release (sec_key);
      gcry_sexp_release (pub_key);
      gcry_sexp_release (key);
    }
}



int
main (int argc, char **argv)
{
  int debug = 0;

  if (argc > 1 && !strcmp (argv[1], "--verbose"))
    verbose = 1;
  else if (argc > 1 && !strcmp (argv[1], "--debug"))
    {
      verbose = 2; 
      debug = 1;
    }

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (!gcry_check_version ("1.4.4"))
    die ("version mismatch\n");
  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);
  if (debug)
    gcry_control (GCRYCTL_SET_DEBUG_FLAGS, 1u, 0);
  /* No valuable keys are create, so we can speed up our RNG. */
  gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);


  check_dsa_gen_186_2 ();


  return error_count ? 1 : 0;
}
