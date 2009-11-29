/* benchmark.c - for libgcrypt
 * Copyright (C) 2002, 2004, 2005, 2006, 2008 Free Software Foundation, Inc.
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
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <stdarg.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <sys/times.h>
#endif

#ifdef _GCRYPT_IN_LIBGCRYPT
# include "../src/gcrypt.h"
#else
# include <gcrypt.h>
#endif


#define PGM "benchmark"

static int verbose;

/* Do encryption tests with large buffers.  */
static int large_buffers;

/* Number of cipher repetitions.  */
static int cipher_repetitions;

/* Whether fips mode was active at startup.  */
static int in_fips_mode;


static const char sample_private_dsa_key_1024[] =
"(private-key\n"
"  (dsa\n"
"   (p #00A126202D592214C5A8F6016E2C3F4256052ACB1CB17D88E64B1293FAF08F5E4685"
       "03E6F68366B326A56284370EB2103E92D8346A163E44A08FDC422AC8E9E44268557A"
       "853539A6AF39353A59CE5E78FD98B57D0F3E3A7EBC8A256AC9A775BA59689F3004BF"
       "C3035730C4C0C51626C5D7F5852637EC589BB29DAB46C161572E4B#)\n"
"   (q #00DEB5A296421887179ECA1762884DE2AF8185AFC5#)\n"
"   (g #3958B34AE7747194ECBD312F8FEE8CBE3918E94DF9FD11E2912E56318F33BDC38622"
       "B18DDFF393074BCA8BAACF50DF27AEE529F3E8AEECE55C398DAB3A5E04C2EA142312"
       "FACA2FE7F0A88884F8DAC3979EE67598F9A383B2A2325F035C796F352A5C3CDF2CB3"
       "85AD24EC52A6E55247E1BB37D260F79E617D2A4446415B6AD79A#)\n"
"   (y #519E9FE9AB0545A6724E74603B7B04E48DC1437E0284A11EA605A7BA8AB1CF354FD4"
       "ECC93880AC293391C69B558AD84E7AAFA88F11D028CF3A378F241D6B056A90C588F6"
       "66F68D27262B4DA84657D15057D371BCEC1F6504032507D5B881E45FC93A1B973155"
       "D91C57219D090C3ACD75E7C2B9F1176A208AC03D6C12AC28A271#)\n"
"   (x #4186F8A58C5DF46C5BCFC7006BEEBF05E93C0CA7#)\n"
"))\n";

static const char sample_public_dsa_key_1024[] =
"(public-key\n"
"  (dsa\n"
"   (p #00A126202D592214C5A8F6016E2C3F4256052ACB1CB17D88E64B1293FAF08F5E4685"
       "03E6F68366B326A56284370EB2103E92D8346A163E44A08FDC422AC8E9E44268557A"
       "853539A6AF39353A59CE5E78FD98B57D0F3E3A7EBC8A256AC9A775BA59689F3004BF"
       "C3035730C4C0C51626C5D7F5852637EC589BB29DAB46C161572E4B#)\n"
"   (q #00DEB5A296421887179ECA1762884DE2AF8185AFC5#)\n"
"   (g #3958B34AE7747194ECBD312F8FEE8CBE3918E94DF9FD11E2912E56318F33BDC38622"
       "B18DDFF393074BCA8BAACF50DF27AEE529F3E8AEECE55C398DAB3A5E04C2EA142312"
       "FACA2FE7F0A88884F8DAC3979EE67598F9A383B2A2325F035C796F352A5C3CDF2CB3"
       "85AD24EC52A6E55247E1BB37D260F79E617D2A4446415B6AD79A#)\n"
"   (y #519E9FE9AB0545A6724E74603B7B04E48DC1437E0284A11EA605A7BA8AB1CF354FD4"
       "ECC93880AC293391C69B558AD84E7AAFA88F11D028CF3A378F241D6B056A90C588F6"
       "66F68D27262B4DA84657D15057D371BCEC1F6504032507D5B881E45FC93A1B973155"
       "D91C57219D090C3ACD75E7C2B9F1176A208AC03D6C12AC28A271#)\n"
"))\n";


static const char sample_private_dsa_key_2048[] =
"(private-key\n"
"  (dsa\n"
"   (p #00B54636673962B64F7DC23C71ACEF6E7331796F607560B194DFCC0CA370E858A365"
       "A413152FB6EB8C664BD171AC316FE5B381CD084D07377571599880A068EF1382D85C"
       "308B4E9DEAC12D66DE5C4A826EBEB5ED94A62E7301E18927E890589A2F230272A150"
       "C118BC3DC2965AE0D05BE4F65C6137B2BA7EDABB192C3070D202C10AA3F534574970"
       "71454DB8A73DDB6511A5BA98EF1450FD90DE5BAAFC9FD3AC22EBEA612DD075BB7405"
       "D56866D125E33982C046808F7CEBA8E5C0B9F19A6FE451461660A1CBA9EF68891179"
       "0256A573D3B8F35A5C7A0C6C31F2DB90E25A26845252AD9E485EF2D339E7B5890CD4"
       "2F9C9F315ED409171EC35CA04CC06B275577B3#)\n"
"   (q #00DA67989167FDAC4AE3DF9247A716859A30C0CF9C5A6DBA01EABA3481#)\n"
"   (g #48E35DA584A089D05142AA63603FDB00D131B07A0781E2D5A8F9614D2B33D3E40A78"
       "98A9E10CDBB612CF093F95A3E10D09566726F2C12823836B2D9CD974BB695665F3B3"
       "5D219A9724B87F380BD5207EDA0AE38C79E8F18122C3F76E4CEB0ABED3250914987F"
       "B30D4B9E19C04C28A5D4F45560AF586F6A1B41751EAD90AE7F044F4E2A4A50C1F508"
       "4FC202463F478F678B9A19392F0D2961C5391C546EF365368BB46410C9C1CEE96E9F"
       "0C953570C2ED06328B11C90E86E57CAA7FA5ABAA278E22A4C8C08E16EE59F484EC44"
       "2CF55535BAA2C6BEA8833A555372BEFE1E665D3C7DAEF58061D5136331EF4EB61BC3"
       "6EE4425A553AF8885FEA15A88135BE133520#)\n"
"   (y #66E0D1A69D663466F8FEF2B7C0878DAC93C36A2FB2C05E0306A53B926021D4B92A1C"
       "2FA6860061E88E78CBBBA49B0E12700F07DBF86F72CEB2927EDAC0C7E3969C3A47BB"
       "4E0AE93D8BB3313E93CC7A72DFEEE442EFBC81B3B2AEC9D8DCBE21220FB760201D79"
       "328C41C773866587A44B6954767D022A88072900E964089D9B17133603056C985C4F"
       "8A0B648F297F8D2C3CB43E4371DC6002B5B12CCC085BDB2CFC5074A0587566187EE3"
       "E11A2A459BD94726248BB8D6CC62938E11E284C2C183576FBB51749EB238C4360923"
       "79C08CE1C8CD77EB57404CE9B4744395ACF721487450BADE3220576F2F816248B0A7"
       "14A264330AECCB24DE2A1107847B23490897#)\n"
"   (x #477BD14676E22563C5ABA68025CEBA2A48D485F5B2D4AD4C0EBBD6D0#)\n"
"))\n";


static const char sample_public_dsa_key_2048[] =
"(public-key\n"
"  (dsa\n"
"   (p #00B54636673962B64F7DC23C71ACEF6E7331796F607560B194DFCC0CA370E858A365"
       "A413152FB6EB8C664BD171AC316FE5B381CD084D07377571599880A068EF1382D85C"
       "308B4E9DEAC12D66DE5C4A826EBEB5ED94A62E7301E18927E890589A2F230272A150"
       "C118BC3DC2965AE0D05BE4F65C6137B2BA7EDABB192C3070D202C10AA3F534574970"
       "71454DB8A73DDB6511A5BA98EF1450FD90DE5BAAFC9FD3AC22EBEA612DD075BB7405"
       "D56866D125E33982C046808F7CEBA8E5C0B9F19A6FE451461660A1CBA9EF68891179"
       "0256A573D3B8F35A5C7A0C6C31F2DB90E25A26845252AD9E485EF2D339E7B5890CD4"
       "2F9C9F315ED409171EC35CA04CC06B275577B3#)\n"
"   (q #00DA67989167FDAC4AE3DF9247A716859A30C0CF9C5A6DBA01EABA3481#)\n"
"   (g #48E35DA584A089D05142AA63603FDB00D131B07A0781E2D5A8F9614D2B33D3E40A78"
       "98A9E10CDBB612CF093F95A3E10D09566726F2C12823836B2D9CD974BB695665F3B3"
       "5D219A9724B87F380BD5207EDA0AE38C79E8F18122C3F76E4CEB0ABED3250914987F"
       "B30D4B9E19C04C28A5D4F45560AF586F6A1B41751EAD90AE7F044F4E2A4A50C1F508"
       "4FC202463F478F678B9A19392F0D2961C5391C546EF365368BB46410C9C1CEE96E9F"
       "0C953570C2ED06328B11C90E86E57CAA7FA5ABAA278E22A4C8C08E16EE59F484EC44"
       "2CF55535BAA2C6BEA8833A555372BEFE1E665D3C7DAEF58061D5136331EF4EB61BC3"
       "6EE4425A553AF8885FEA15A88135BE133520#)\n"
"   (y #66E0D1A69D663466F8FEF2B7C0878DAC93C36A2FB2C05E0306A53B926021D4B92A1C"
       "2FA6860061E88E78CBBBA49B0E12700F07DBF86F72CEB2927EDAC0C7E3969C3A47BB"
       "4E0AE93D8BB3313E93CC7A72DFEEE442EFBC81B3B2AEC9D8DCBE21220FB760201D79"
       "328C41C773866587A44B6954767D022A88072900E964089D9B17133603056C985C4F"
       "8A0B648F297F8D2C3CB43E4371DC6002B5B12CCC085BDB2CFC5074A0587566187EE3"
       "E11A2A459BD94726248BB8D6CC62938E11E284C2C183576FBB51749EB238C4360923"
       "79C08CE1C8CD77EB57404CE9B4744395ACF721487450BADE3220576F2F816248B0A7"
       "14A264330AECCB24DE2A1107847B23490897#)\n"
"))\n";


static const char sample_private_dsa_key_3072[] =
"(private-key\n"
"  (dsa\n"
"   (p #00BA73E148AEA5E8B64878AF5BE712B8302B9671C5F3EEB7722A9D0D9868D048C938"
       "877C91C335C7819292E69C7D34264F1578E32EC2DA8408DF75D0EB76E0D3030B84B5"
       "62D8EF93AB53BAB6B8A5DE464F5CA87AEA43BDCF0FB0B7815AA3114CFC84FD916A83"
       "B3D5FD78390189332232E9D037D215313FD002FF46C048B66703F87FAE092AAA0988"
       "AC745336EBE672A01DEDBD52395783579B67CF3AE1D6F1602CCCB12154FA0E00AE46"
       "0D9B289CF709194625BCB919B11038DEFC50ADBBA20C3F320078E4E9529B4F6848E2"
       "AB5E6278DB961FE226F2EEBD201E071C48C5BEF98B4D9BEE42C1C7102D893EBF8902"
       "D7A91266340AFD6CE1D09E52282FFF5B97EAFA3886A3FCF84FF76D1E06538D0D8E60"
       "B3332145785E07D29A5965382DE3470D1D888447FA9C00A2373378FC3FA7B9F7D17E"
       "95A6A5AE1397BE46D976EF2C96E89913AC4A09351CA661BF6F67E30407DA846946C7"
       "62D9BAA6B77825097D3E7B886456BB32E3E74516BF3FD93D71B257AA8F723E01CE33"
       "8015353D3778B02B892AF7#)\n"
"   (q #00BFF3F3CC18FA018A5B8155A8695E1E4939660D5E4759322C39D50F3B93E5F68B#)\n"
"   (g #6CCFD8219F5FCE8EF2BEF3262929787140847E38674B1EF8DB20255E212CB6330EC4"
       "DFE8A26AB7ECC5760DEB9BBF59A2B2821D510F1868172222867558B8D204E889C474"
       "7CA30FBF9D8CF41AE5D5BD845174641101593849FF333E6C93A6550931B2B9D56B98"
       "9CAB01729D9D736FA6D24A74D2DDE1E9E648D141473E443DD6BBF0B3CAB64F9FE4FC"
       "134B2EB57437789F75C744DF1FA67FA8A64603E5441BC7ECE29E00BDF262BDC81E8C"
       "7330A18A412DE38E7546D342B89A0AF675A89E6BEF00540EB107A2FE74EA402B0D89"
       "F5C02918DEEEAF8B8737AC866B09B50810AB8D8668834A1B9E1E53866E2B0A926FAB"
       "120A0CDE5B3715FFFE6ACD1AB73588DCC1EC4CE9392FE57F8D1D35811200CB07A0E6"
       "374E2C4B0AEB7E3D077B8545C0E438DCC0F1AE81E186930E99EBC5B91B77E92803E0"
       "21602887851A4FFDB3A7896AC655A0901218C121C5CBB0931E7D5EAC243F37711B5F"
       "D5A62B1B38A83F03D8F6703D8B98DF367FC8A76990335F62173A5391836F0F2413EC"
       "4997AF9EB55C6660B01A#)\n"
"   (y #2320B22434C5DB832B4EC267CC52E78DD5CCFA911E8F0804E7E7F32B186B2D4167AE"
       "4AA6869822E76400492D6A193B0535322C72B0B7AA4A87E33044FDC84BE24C64A053"
       "A37655EE9EABDCDC1FDF63F3F1C677CEB41595DF7DEFE9178D85A3D621B4E4775492"
       "8C0A58D2458D06F9562E4DE2FE6129A64063A99E88E54485B97484A28188C4D33F15"
       "DDC903B6CEA0135E3E3D27B4EA39319696305CE93D7BA7BE00367DBE3AAF43491E71"
       "CBF254744A5567F5D70090D6139E0C990239627B3A1C5B20B6F9F6374B8D8D8A8997"
       "437265BE1E3B4810D4B09254400DE287A0DFFBAEF339E48D422B1D41A37E642BC026"
       "73314701C8FA9792845C129351A87A945A03E6C895860E51D6FB8B7340A94D1A8A7B"
       "FA85AC83B4B14E73AB86CB96C236C8BFB0978B61B2367A7FE4F7891070F56C78D5DD"
       "F5576BFE5BE4F333A4E2664E79528B3294907AADD63F4F2E7AA8147B928D8CD69765"
       "3DB98C4297CB678046ED55C0DBE60BF7142C594603E4D705DC3D17270F9F086EC561"
       "2703D518D8D49FF0EBE6#)\n"
"   (x #00A9FFFC88E67D6F7B810E291C050BAFEA7FC4A75E8D2F16CFED3416FD77607232#)\n"
"))\n";

static const char sample_public_dsa_key_3072[] =
"(public-key\n"
"  (dsa\n"
"   (p #00BA73E148AEA5E8B64878AF5BE712B8302B9671C5F3EEB7722A9D0D9868D048C938"
       "877C91C335C7819292E69C7D34264F1578E32EC2DA8408DF75D0EB76E0D3030B84B5"
       "62D8EF93AB53BAB6B8A5DE464F5CA87AEA43BDCF0FB0B7815AA3114CFC84FD916A83"
       "B3D5FD78390189332232E9D037D215313FD002FF46C048B66703F87FAE092AAA0988"
       "AC745336EBE672A01DEDBD52395783579B67CF3AE1D6F1602CCCB12154FA0E00AE46"
       "0D9B289CF709194625BCB919B11038DEFC50ADBBA20C3F320078E4E9529B4F6848E2"
       "AB5E6278DB961FE226F2EEBD201E071C48C5BEF98B4D9BEE42C1C7102D893EBF8902"
       "D7A91266340AFD6CE1D09E52282FFF5B97EAFA3886A3FCF84FF76D1E06538D0D8E60"
       "B3332145785E07D29A5965382DE3470D1D888447FA9C00A2373378FC3FA7B9F7D17E"
       "95A6A5AE1397BE46D976EF2C96E89913AC4A09351CA661BF6F67E30407DA846946C7"
       "62D9BAA6B77825097D3E7B886456BB32E3E74516BF3FD93D71B257AA8F723E01CE33"
       "8015353D3778B02B892AF7#)\n"
"   (q #00BFF3F3CC18FA018A5B8155A8695E1E4939660D5E4759322C39D50F3B93E5F68B#)\n"
"   (g #6CCFD8219F5FCE8EF2BEF3262929787140847E38674B1EF8DB20255E212CB6330EC4"
       "DFE8A26AB7ECC5760DEB9BBF59A2B2821D510F1868172222867558B8D204E889C474"
       "7CA30FBF9D8CF41AE5D5BD845174641101593849FF333E6C93A6550931B2B9D56B98"
       "9CAB01729D9D736FA6D24A74D2DDE1E9E648D141473E443DD6BBF0B3CAB64F9FE4FC"
       "134B2EB57437789F75C744DF1FA67FA8A64603E5441BC7ECE29E00BDF262BDC81E8C"
       "7330A18A412DE38E7546D342B89A0AF675A89E6BEF00540EB107A2FE74EA402B0D89"
       "F5C02918DEEEAF8B8737AC866B09B50810AB8D8668834A1B9E1E53866E2B0A926FAB"
       "120A0CDE5B3715FFFE6ACD1AB73588DCC1EC4CE9392FE57F8D1D35811200CB07A0E6"
       "374E2C4B0AEB7E3D077B8545C0E438DCC0F1AE81E186930E99EBC5B91B77E92803E0"
       "21602887851A4FFDB3A7896AC655A0901218C121C5CBB0931E7D5EAC243F37711B5F"
       "D5A62B1B38A83F03D8F6703D8B98DF367FC8A76990335F62173A5391836F0F2413EC"
       "4997AF9EB55C6660B01A#)\n"
"   (y #2320B22434C5DB832B4EC267CC52E78DD5CCFA911E8F0804E7E7F32B186B2D4167AE"
       "4AA6869822E76400492D6A193B0535322C72B0B7AA4A87E33044FDC84BE24C64A053"
       "A37655EE9EABDCDC1FDF63F3F1C677CEB41595DF7DEFE9178D85A3D621B4E4775492"
       "8C0A58D2458D06F9562E4DE2FE6129A64063A99E88E54485B97484A28188C4D33F15"
       "DDC903B6CEA0135E3E3D27B4EA39319696305CE93D7BA7BE00367DBE3AAF43491E71"
       "CBF254744A5567F5D70090D6139E0C990239627B3A1C5B20B6F9F6374B8D8D8A8997"
       "437265BE1E3B4810D4B09254400DE287A0DFFBAEF339E48D422B1D41A37E642BC026"
       "73314701C8FA9792845C129351A87A945A03E6C895860E51D6FB8B7340A94D1A8A7B"
       "FA85AC83B4B14E73AB86CB96C236C8BFB0978B61B2367A7FE4F7891070F56C78D5DD"
       "F5576BFE5BE4F333A4E2664E79528B3294907AADD63F4F2E7AA8147B928D8CD69765"
       "3DB98C4297CB678046ED55C0DBE60BF7142C594603E4D705DC3D17270F9F086EC561"
       "2703D518D8D49FF0EBE6#)\n"
"))\n";


#define DIM(v)		     (sizeof(v)/sizeof((v)[0]))
#define DIMof(type,member)   DIM(((type *)0)->member)
#define BUG() do {fprintf ( stderr, "Ooops at %s:%d\n", __FILE__ , __LINE__ );\
		  exit(2);} while(0)


/* Helper for the start and stop timer. */
#ifdef _WIN32
struct {
  FILETIME creation_time, exit_time, kernel_time, user_time;
} started_at, stopped_at;
#else
static clock_t started_at, stopped_at;
#endif

static void
die (const char *format, ...)
{
  va_list arg_ptr ;

  va_start( arg_ptr, format ) ;
  putchar ('\n');
  fputs ( PGM ": ", stderr);
  vfprintf (stderr, format, arg_ptr );
  va_end(arg_ptr);
  exit (1);
}

static void
show_sexp (const char *prefix, gcry_sexp_t a)
{
  char *buf;
  size_t size;

  fputs (prefix, stderr);
  size = gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, NULL, 0);
  buf = malloc (size);
  if (!buf)
    die ("out of core\n");

  gcry_sexp_sprint (a, GCRYSEXP_FMT_ADVANCED, buf, size);
  fprintf (stderr, "%.*s", (int)size, buf);
}


static void
start_timer (void)
{
#ifdef _WIN32
  GetProcessTimes (GetCurrentProcess (),
                   &started_at.creation_time, &started_at.exit_time,
                   &started_at.kernel_time, &started_at.user_time);
  stopped_at = started_at;
#else
  struct tms tmp;

  times (&tmp);
  started_at = stopped_at = tmp.tms_utime;
#endif
}

static void
stop_timer (void)
{
#ifdef _WIN32
  GetProcessTimes (GetCurrentProcess (),
                   &stopped_at.creation_time, &stopped_at.exit_time,
                   &stopped_at.kernel_time, &stopped_at.user_time);
#else
  struct tms tmp;

  times (&tmp);
  stopped_at = tmp.tms_utime;
#endif
}

static const char *
elapsed_time (void)
{
  static char buf[50];
#if _WIN32
  unsigned long long t1, t2, t;

  t1 = (((unsigned long long)started_at.kernel_time.dwHighDateTime << 32)
        + started_at.kernel_time.dwLowDateTime);
  t1 += (((unsigned long long)started_at.user_time.dwHighDateTime << 32)
        + started_at.user_time.dwLowDateTime);
  t2 = (((unsigned long long)stopped_at.kernel_time.dwHighDateTime << 32)
        + stopped_at.kernel_time.dwLowDateTime);
  t2 += (((unsigned long long)stopped_at.user_time.dwHighDateTime << 32)
        + stopped_at.user_time.dwLowDateTime);
  t = (t2 - t1)/10000;
  snprintf (buf, sizeof buf, "%5.0fms", (double)t );
#else
  snprintf (buf, sizeof buf, "%5.0fms",
            (((double) (stopped_at - started_at))/CLOCKS_PER_SEC)*10000000);
#endif
  return buf;
}


static void
random_bench (int very_strong)
{
  char buf[128];
  int i;

  printf ("%-10s", "random");

  if (!very_strong)
    {
      start_timer ();
      for (i=0; i < 100; i++)
        gcry_randomize (buf, sizeof buf, GCRY_STRONG_RANDOM);
      stop_timer ();
      printf (" %s", elapsed_time ());
    }

  start_timer ();
  for (i=0; i < 100; i++)
    gcry_randomize (buf, 8,
                    very_strong? GCRY_VERY_STRONG_RANDOM:GCRY_STRONG_RANDOM);
  stop_timer ();
  printf (" %s", elapsed_time ());

  putchar ('\n');
  if (verbose)
    gcry_control (GCRYCTL_DUMP_RANDOM_STATS);
}



static void
md_bench ( const char *algoname )
{
  int algo;
  gcry_md_hd_t hd;
  int i;
  char buf[1000];
  gcry_error_t err = GPG_ERR_NO_ERROR;

  if (!algoname)
    {
      for (i=1; i < 400; i++)
        if (in_fips_mode && i == GCRY_MD_MD5)
          ; /* Don't use MD5 in fips mode.  */
        else if ( !gcry_md_test_algo (i) )
          md_bench (gcry_md_algo_name (i));
      return;
    }

  algo = gcry_md_map_name (algoname);
  if (!algo)
    {
      fprintf (stderr, PGM ": invalid hash algorithm `%s'\n", algoname);
      exit (1);
    }

  err = gcry_md_open (&hd, algo, 0);
  if (err)
    {
      fprintf (stderr, PGM ": error opening hash algorithm `%s'\n", algoname);
      exit (1);
    }

  for (i=0; i < sizeof buf; i++)
    buf[i] = i;

  printf ("%-12s", gcry_md_algo_name (algo));

  start_timer ();
  for (i=0; i < 1000; i++)
    gcry_md_write (hd, buf, sizeof buf);
  gcry_md_final (hd);
  stop_timer ();
  printf (" %s", elapsed_time ());

  gcry_md_reset (hd);
  start_timer ();
  for (i=0; i < 10000; i++)
    gcry_md_write (hd, buf, sizeof buf/10);
  gcry_md_final (hd);
  stop_timer ();
  printf (" %s", elapsed_time ());

  gcry_md_reset (hd);
  start_timer ();
  for (i=0; i < 1000000; i++)
    gcry_md_write (hd, "", 1);
  gcry_md_final (hd);
  stop_timer ();
  printf (" %s", elapsed_time ());

  gcry_md_close (hd);
  putchar ('\n');
}

static void
cipher_bench ( const char *algoname )
{
  static int header_printed;
  int algo;
  gcry_cipher_hd_t hd;
  int i;
  int keylen, blklen;
  char key[128];
  char *outbuf, *buf;
  size_t allocated_buflen, buflen;
  int repetitions;
  static struct { int mode; const char *name; int blocked; } modes[] = {
    { GCRY_CIPHER_MODE_ECB, "ECB", 1 },
    { GCRY_CIPHER_MODE_CBC, "CBC", 1 },
    { GCRY_CIPHER_MODE_CFB, "CFB", 0 },
    { GCRY_CIPHER_MODE_OFB, "OFB", 0 },
    { GCRY_CIPHER_MODE_CTR, "CTR", 0 },
    { GCRY_CIPHER_MODE_STREAM, "STREAM", 0 },
    {0}
  };
  int modeidx;
  gcry_error_t err = GPG_ERR_NO_ERROR;


  if (!algoname)
    {
      for (i=1; i < 400; i++)
        if ( !gcry_cipher_test_algo (i) )
          cipher_bench (gcry_cipher_algo_name (i));
      return;
    }

  if (large_buffers)
    {
      allocated_buflen = 1024 * 100;
      repetitions = 10;
    }
  else
    {
      allocated_buflen = 1024;
      repetitions = 1000;
    }
  repetitions *= cipher_repetitions;

  buf = gcry_xmalloc (allocated_buflen);
  outbuf = gcry_xmalloc (allocated_buflen);

  if (!header_printed)
    {
      if (cipher_repetitions != 1)
        printf ("Running each test %d times.\n", cipher_repetitions);
      printf ("%-12s", "");
      for (modeidx=0; modes[modeidx].mode; modeidx++)
        printf (" %-15s", modes[modeidx].name );
      putchar ('\n');
      printf ("%-12s", "");
      for (modeidx=0; modes[modeidx].mode; modeidx++)
        printf (" ---------------" );
      putchar ('\n');
      header_printed = 1;
    }

  algo = gcry_cipher_map_name (algoname);
  if (!algo)
    {
      fprintf (stderr, PGM ": invalid cipher algorithm `%s'\n", algoname);
      exit (1);
    }

  keylen = gcry_cipher_get_algo_keylen (algo);
  if (!keylen)
    {
      fprintf (stderr, PGM ": failed to get key length for algorithm `%s'\n",
	       algoname);
      exit (1);
    }
  if ( keylen > sizeof key )
    {
        fprintf (stderr, PGM ": algo %d, keylength problem (%d)\n",
                 algo, keylen );
        exit (1);
    }
  for (i=0; i < keylen; i++)
    key[i] = i + (clock () & 0xff);

  blklen = gcry_cipher_get_algo_blklen (algo);
  if (!blklen)
    {
      fprintf (stderr, PGM ": failed to get block length for algorithm `%s'\n",
	       algoname);
      exit (1);
    }

  printf ("%-12s", gcry_cipher_algo_name (algo));
  fflush (stdout);

  for (modeidx=0; modes[modeidx].mode; modeidx++)
    {
      if ((blklen > 1 && modes[modeidx].mode == GCRY_CIPHER_MODE_STREAM)
          | (blklen == 1 && modes[modeidx].mode != GCRY_CIPHER_MODE_STREAM))
        {
          printf ("                " );
          continue;
        }

      for (i=0; i < sizeof buf; i++)
        buf[i] = i;

      err = gcry_cipher_open (&hd, algo, modes[modeidx].mode, 0);
      if (err)
        {
          fprintf (stderr, PGM ": error opening cipher `%s'\n", algoname);
          exit (1);
        }
      
      err = gcry_cipher_setkey (hd, key, keylen);
      if (err)
      { 
          fprintf (stderr, "gcry_cipher_setkey failed: %s\n",
		   gpg_strerror (err));
          gcry_cipher_close (hd);
          exit (1);
        }

      buflen = allocated_buflen;
      if (modes[modeidx].blocked)
        buflen = (buflen / blklen) * blklen;
      
      start_timer ();
      for (i=err=0; !err && i < repetitions; i++)
        err = gcry_cipher_encrypt ( hd, outbuf, buflen, buf, buflen);
      stop_timer ();

      printf (" %s", elapsed_time ());
      fflush (stdout);
      gcry_cipher_close (hd);
      if (err)
        { 
          fprintf (stderr, "gcry_cipher_encrypt failed: %s\n",
                   gpg_strerror (err) );
          exit (1);
        }

      err = gcry_cipher_open (&hd, algo, modes[modeidx].mode, 0);
      if (err)
        {
          fprintf (stderr, PGM ": error opening cipher `%s'/n", algoname);
          exit (1);
        }
      
      err = gcry_cipher_setkey (hd, key, keylen);
      if (err)
        { 
          fprintf (stderr, "gcry_cipher_setkey failed: %s\n",
                   gpg_strerror (err));
          gcry_cipher_close (hd);
          exit (1);
        }

      start_timer ();
      for (i=err=0; !err && i < repetitions; i++)
        err = gcry_cipher_decrypt ( hd, outbuf, buflen,  buf, buflen);
      stop_timer ();
      printf (" %s", elapsed_time ());
      fflush (stdout);
      gcry_cipher_close (hd);
      if (err)
        { 
          fprintf (stderr, "gcry_cipher_decrypt failed: %s\n",
                   gpg_strerror (err) );
          exit (1);
        }
    }

  putchar ('\n');
  gcry_free (buf);
  gcry_free (outbuf);
}



static void
rsa_bench (int iterations, int print_header, int no_blinding)
{
  gpg_error_t err;
  int p_sizes[] = { 1024, 2048, 3072, 4096 };
  int testno;

  if (print_header)
    printf ("Algorithm         generate %4d*sign %4d*verify\n"
            "------------------------------------------------\n",
            iterations, iterations );
  for (testno=0; testno < DIM (p_sizes); testno++)
    {
      gcry_sexp_t key_spec, key_pair, pub_key, sec_key;
      gcry_mpi_t x;
      gcry_sexp_t data;
      gcry_sexp_t sig = NULL;
      int count;

      printf ("RSA %3d bit    ", p_sizes[testno]);
      fflush (stdout);

      err = gcry_sexp_build (&key_spec, NULL,
                             gcry_fips_mode_active ()
                             ? "(genkey (RSA (nbits %d)))"
                             : "(genkey (RSA (nbits %d)(transient-key)))",
                             p_sizes[testno]);
      if (err)
        die ("creating S-expression failed: %s\n", gcry_strerror (err));

      start_timer ();
      err = gcry_pk_genkey (&key_pair, key_spec);
      if (err)
        die ("creating %d bit RSA key failed: %s\n",
             p_sizes[testno], gcry_strerror (err));

      pub_key = gcry_sexp_find_token (key_pair, "public-key", 0);
      if (! pub_key)
        die ("public part missing in key\n");
      sec_key = gcry_sexp_find_token (key_pair, "private-key", 0);
      if (! sec_key)
        die ("private part missing in key\n");
      gcry_sexp_release (key_pair);
      gcry_sexp_release (key_spec);

      stop_timer ();
      printf ("   %s", elapsed_time ());
      fflush (stdout);

      x = gcry_mpi_new (p_sizes[testno]);
      gcry_mpi_randomize (x, p_sizes[testno]-8, GCRY_WEAK_RANDOM);
      err = gcry_sexp_build (&data, NULL,
                             "(data (flags raw) (value %m))", x);
      gcry_mpi_release (x);
      if (err)
        die ("converting data failed: %s\n", gcry_strerror (err));

      start_timer ();
      for (count=0; count < iterations; count++)
        {
          gcry_sexp_release (sig);
          err = gcry_pk_sign (&sig, data, sec_key);
          if (err)
            die ("signing failed (%d): %s\n", count, gpg_strerror (err));
        }
      stop_timer ();
      printf ("   %s", elapsed_time ());
      fflush (stdout);

      start_timer ();
      for (count=0; count < iterations; count++)
        {
          err = gcry_pk_verify (sig, data, pub_key);
          if (err)
            {
              putchar ('\n');
              show_sexp ("seckey:\n", sec_key);
              show_sexp ("data:\n", data);
              show_sexp ("sig:\n", sig);
              die ("verify failed (%d): %s\n", count, gpg_strerror (err));
            }
        }
      stop_timer ();
      printf ("     %s", elapsed_time ());

      if (no_blinding)
        {
          fflush (stdout);
          x = gcry_mpi_new (p_sizes[testno]);
          gcry_mpi_randomize (x, p_sizes[testno]-8, GCRY_WEAK_RANDOM);
          err = gcry_sexp_build (&data, NULL,
                                 "(data (flags no-blinding) (value %m))", x);
          gcry_mpi_release (x);
          if (err)
            die ("converting data failed: %s\n", gcry_strerror (err));

          start_timer ();
          for (count=0; count < iterations; count++)
            {
              gcry_sexp_release (sig);
              err = gcry_pk_sign (&sig, data, sec_key);
              if (err)
                die ("signing failed (%d): %s\n", count, gpg_strerror (err));
            }
          stop_timer ();
          printf ("   %s", elapsed_time ());
          fflush (stdout);
        }

      putchar ('\n');
      fflush (stdout);

      gcry_sexp_release (sig);
      gcry_sexp_release (data);
      gcry_sexp_release (sec_key);
      gcry_sexp_release (pub_key);
    }
}



static void
dsa_bench (int iterations, int print_header)
{
  gpg_error_t err;
  gcry_sexp_t pub_key[3], sec_key[3];
  int p_sizes[3] = { 1024, 2048, 3072 };
  int q_sizes[3] = { 160, 224, 256 };
  gcry_sexp_t data;
  gcry_sexp_t sig;
  int i, j;

  err = gcry_sexp_sscan (pub_key+0, NULL, sample_public_dsa_key_1024,
                         strlen (sample_public_dsa_key_1024));
  if (!err)
    err = gcry_sexp_sscan (sec_key+0, NULL, sample_private_dsa_key_1024,
                           strlen (sample_private_dsa_key_1024));
  if (!err)
    err = gcry_sexp_sscan (pub_key+1, NULL, sample_public_dsa_key_2048,
                           strlen (sample_public_dsa_key_2048));
  if (!err)
    err = gcry_sexp_sscan (sec_key+1, NULL, sample_private_dsa_key_2048,
                           strlen (sample_private_dsa_key_2048));
  if (!err)
    err = gcry_sexp_sscan (pub_key+2, NULL, sample_public_dsa_key_3072,
                           strlen (sample_public_dsa_key_3072));
  if (!err)
    err = gcry_sexp_sscan (sec_key+2, NULL, sample_private_dsa_key_3072,
                           strlen (sample_private_dsa_key_3072));
  if (err)
    {
      fprintf (stderr, PGM ": converting sample keys failed: %s\n",
               gcry_strerror (err));
      exit (1);
    }

  if (print_header)
    printf ("Algorithm         generate %4d*sign %4d*verify\n"
            "------------------------------------------------\n",
            iterations, iterations );
  for (i=0; i < DIM (q_sizes); i++)
    {
      gcry_mpi_t x;

      x = gcry_mpi_new (q_sizes[i]);
      gcry_mpi_randomize (x, q_sizes[i], GCRY_WEAK_RANDOM);
      err = gcry_sexp_build (&data, NULL, "(data (flags raw) (value %m))", x);
      gcry_mpi_release (x);
      if (err)
        {
          fprintf (stderr, PGM ": converting data failed: %s\n",
                   gcry_strerror (err));
          exit (1);
        }

      printf ("DSA %d/%d             -", p_sizes[i], q_sizes[i]);
      fflush (stdout);

      start_timer ();
      for (j=0; j < iterations; j++)
        {
          err = gcry_pk_sign (&sig, data, sec_key[i]);
          if (err)
            {
              putchar ('\n');
              fprintf (stderr, PGM ": signing failed: %s\n",
                       gpg_strerror (err));
              exit (1);
            }
        }
      stop_timer ();
      printf ("   %s", elapsed_time ());
      fflush (stdout);

      start_timer ();
      for (j=0; j < iterations; j++)
        {
          err = gcry_pk_verify (sig, data, pub_key[i]);
          if (err)
            {
              putchar ('\n');
              fprintf (stderr, PGM ": verify failed: %s\n",
                       gpg_strerror (err));
              exit (1);
            }
        }
      stop_timer ();
      printf ("     %s\n", elapsed_time ());
      fflush (stdout);

      gcry_sexp_release (sig);
      gcry_sexp_release (data);
    }


  for (i=0; i < DIM (q_sizes); i++)
    {
      gcry_sexp_release (sec_key[i]);
      gcry_sexp_release (pub_key[i]);
    }
}


static void
ecc_bench (int iterations, int print_header)
{
#if USE_ECC
  gpg_error_t err;
  int p_sizes[] = { 192, 224, 256, 384, 521 };
  int testno;

  if (print_header)
    printf ("Algorithm         generate %4d*sign %4d*verify\n"
            "------------------------------------------------\n",
            iterations, iterations );
  for (testno=0; testno < DIM (p_sizes); testno++)
    {
      gcry_sexp_t key_spec, key_pair, pub_key, sec_key;
      gcry_mpi_t x;
      gcry_sexp_t data;
      gcry_sexp_t sig = NULL;
      int count;

      printf ("ECDSA %3d bit ", p_sizes[testno]);
      fflush (stdout);

      err = gcry_sexp_build (&key_spec, NULL,
                             "(genkey (ECDSA (nbits %d)))", p_sizes[testno]);
      if (err)
        die ("creating S-expression failed: %s\n", gcry_strerror (err));

      start_timer ();
      err = gcry_pk_genkey (&key_pair, key_spec);
      if (err)
        die ("creating %d bit ECC key failed: %s\n",
             p_sizes[testno], gcry_strerror (err));

      pub_key = gcry_sexp_find_token (key_pair, "public-key", 0);
      if (! pub_key)
        die ("public part missing in key\n");
      sec_key = gcry_sexp_find_token (key_pair, "private-key", 0);
      if (! sec_key)
        die ("private part missing in key\n");
      gcry_sexp_release (key_pair);
      gcry_sexp_release (key_spec);

      stop_timer ();
      printf ("     %s", elapsed_time ());
      fflush (stdout);

      x = gcry_mpi_new (p_sizes[testno]);
      gcry_mpi_randomize (x, p_sizes[testno], GCRY_WEAK_RANDOM);
      err = gcry_sexp_build (&data, NULL, "(data (flags raw) (value %m))", x);
      gcry_mpi_release (x);
      if (err)
        die ("converting data failed: %s\n", gcry_strerror (err));

      start_timer ();
      for (count=0; count < iterations; count++)
        {
          gcry_sexp_release (sig);
          err = gcry_pk_sign (&sig, data, sec_key);
          if (err)
            die ("signing failed: %s\n", gpg_strerror (err));
        }
      stop_timer ();
      printf ("   %s", elapsed_time ());
      fflush (stdout);

      start_timer ();
      for (count=0; count < iterations; count++)
        {
          err = gcry_pk_verify (sig, data, pub_key);
          if (err)
            {
              putchar ('\n');
              show_sexp ("seckey:\n", sec_key);
              show_sexp ("data:\n", data);
              show_sexp ("sig:\n", sig);
              die ("verify failed: %s\n", gpg_strerror (err));
            }
        }
      stop_timer ();
      printf ("     %s\n", elapsed_time ());
      fflush (stdout);

      gcry_sexp_release (sig);
      gcry_sexp_release (data);
      gcry_sexp_release (sec_key);
      gcry_sexp_release (pub_key);
    }
#endif /*USE_ECC*/
}



static void
do_powm ( const char *n_str, const char *e_str, const char *m_str)
{
  gcry_mpi_t e, n, msg, cip;
  gcry_error_t err;
  int i;

  err = gcry_mpi_scan (&n, GCRYMPI_FMT_HEX, n_str, 0, 0);
  if (err) BUG ();
  err = gcry_mpi_scan (&e, GCRYMPI_FMT_HEX, e_str, 0, 0);
  if (err) BUG ();
  err = gcry_mpi_scan (&msg, GCRYMPI_FMT_HEX, m_str, 0, 0);
  if (err) BUG ();

  cip = gcry_mpi_new (0);

  start_timer ();
  for (i=0; i < 1000; i++)
    gcry_mpi_powm (cip, msg, e, n);
  stop_timer ();
  printf (" %s", elapsed_time ()); fflush (stdout);
/*    { */
/*      char *buf; */

/*      if (gcry_mpi_aprint (GCRYMPI_FMT_HEX, (void**)&buf, NULL, cip)) */
/*        BUG (); */
/*      printf ("result: %s\n", buf); */
/*      gcry_free (buf); */
/*    } */
  gcry_mpi_release (cip);
  gcry_mpi_release (msg);
  gcry_mpi_release (n);
  gcry_mpi_release (e);
}


static void
mpi_bench (void)
{
  printf ("%-10s", "powm"); fflush (stdout);

  do_powm (
"20A94417D4D5EF2B2DA99165C7DC87DADB3979B72961AF90D09D59BA24CB9A10166FDCCC9C659F2B9626EC23F3FA425F564A072BA941B03FA81767CC289E4",
           "29", 
"B870187A323F1ECD5B8A0B4249507335A1C4CE8394F38FD76B08C78A42C58F6EA136ACF90DFE8603697B1694A3D81114D6117AC1811979C51C4DD013D52F8"
           );
  do_powm (
           "20A94417D4D5EF2B2DA99165C7DC87DADB3979B72961AF90D09D59BA24CB9A10166FDCCC9C659F2B9626EC23F3FA425F564A072BA941B03FA81767CC289E41071F0246879A442658FBD18C1771571E7073EEEB2160BA0CBFB3404D627069A6CFBD53867AD2D9D40231648000787B5C84176B4336144644AE71A403CA40716",
           "29", 
           "B870187A323F1ECD5B8A0B4249507335A1C4CE8394F38FD76B08C78A42C58F6EA136ACF90DFE8603697B1694A3D81114D6117AC1811979C51C4DD013D52F8FC4EE4BB446B83E48ABED7DB81CBF5E81DE4759E8D68AC985846D999F96B0D8A80E5C69D272C766AB8A23B40D50A4FA889FBC2BD2624222D8EB297F4BAEF8593847"
           );
  do_powm (
           "20A94417D4D5EF2B2DA99165C7DC87DADB3979B72961AF90D09D59BA24CB9A10166FDCCC9C659F2B9626EC23F3FA425F564A072BA941B03FA81767CC289E41071F0246879A442658FBD18C1771571E7073EEEB2160BA0CBFB3404D627069A6CFBD53867AD2D9D40231648000787B5C84176B4336144644AE71A403CA4071620A94417D4D5EF2B2DA99165C7DC87DADB3979B72961AF90D09D59BA24CB9A10166FDCCC9C659F2B9626EC23F3FA425F564A072BA941B03FA81767CC289E41071F0246879A442658FBD18C1771571E7073EEEB2160BA0CBFB3404D627069A6CFBD53867AD2D9D40231648000787B5C84176B4336144644AE71A403CA40716",
           "29", 
           "B870187A323F1ECD5B8A0B4249507335A1C4CE8394F38FD76B08C78A42C58F6EA136ACF90DFE8603697B1694A3D81114D6117AC1811979C51C4DD013D52F8FC4EE4BB446B83E48ABED7DB81CBF5E81DE4759E8D68AC985846D999F96B0D8A80E5C69D272C766AB8A23B40D50A4FA889FBC2BD2624222D8EB297F4BAEF8593847B870187A323F1ECD5B8A0B4249507335A1C4CE8394F38FD76B08C78A42C58F6EA136ACF90DFE8603697B1694A3D81114D6117AC1811979C51C4DD013D52F8FC4EE4BB446B83E48ABED7DB81CBF5E81DE4759E8D68AC985846D999F96B0D8A80E5C69D272C766AB8A23B40D50A4FA889FBC2BD2624222D8EB297F4BAEF8593847"
           );

  putchar ('\n');


}


int
main( int argc, char **argv )
{
  int last_argc = -1;
  int no_blinding = 0;
  int use_random_daemon = 0;

  if (argc)
    { argc--; argv++; }

  while (argc && last_argc != argc )
    {
      last_argc = argc;
      if (!strcmp (*argv, "--"))
        {
          argc--; argv++;
          break;
        }
      else if (!strcmp (*argv, "--help"))
        {
          fputs ("usage: benchmark "
                 "[md|cipher|random|mpi|rsa|dsa|ecc [algonames]]\n",
                 stdout);
          exit (0);
        }
      else if (!strcmp (*argv, "--verbose"))
        {
          verbose++;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--use-random-daemon"))
        {
          use_random_daemon = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--no-blinding"))
        {
          no_blinding = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--large-buffers"))
        {
          large_buffers = 1;
          argc--; argv++;
        }
      else if (!strcmp (*argv, "--cipher-repetition"))
        {
          argc--; argv++;
          if (argc)
            {
              cipher_repetitions = atoi(*argv);
              argc--; argv++;
            }
        }
      else if (!strcmp (*argv, "--fips"))
        {
          argc--; argv++;
          /* This command needs to be called before gcry_check_version.  */
          gcry_control (GCRYCTL_FORCE_FIPS_MODE, 0);
        }
    }          

  gcry_control (GCRYCTL_SET_VERBOSITY, (int)verbose);

  if (!gcry_check_version (GCRYPT_VERSION))
    {
      fprintf (stderr, PGM ": version mismatch\n");
      exit (1);
    }

  if (gcry_fips_mode_active ())
    in_fips_mode = 1;
  else
    gcry_control (GCRYCTL_DISABLE_SECMEM, 0);

  if (use_random_daemon)
    gcry_control (GCRYCTL_USE_RANDOM_DAEMON, 1);

  gcry_control (GCRYCTL_INITIALIZATION_FINISHED, 0);


  if (cipher_repetitions < 1)
    cipher_repetitions = 1;
  
  if ( !argc )
    {
      gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);
      md_bench (NULL);
      putchar ('\n');
      cipher_bench (NULL);
      putchar ('\n');
      rsa_bench (100, 1, no_blinding);
      dsa_bench (100, 0);
      ecc_bench (100, 0);
      putchar ('\n');
      mpi_bench ();
      putchar ('\n');
      random_bench (0);
    }
  else if ( !strcmp (*argv, "random") || !strcmp (*argv, "strongrandom"))
    {
      if (argc == 1)
        random_bench ((**argv == 's'));
      else if (argc == 2)
        {
          gcry_control (GCRYCTL_SET_RANDOM_SEED_FILE, argv[1]);
          random_bench ((**argv == 's'));
          gcry_control (GCRYCTL_UPDATE_RANDOM_SEED_FILE);
        }
      else
        fputs ("usage: benchmark [strong]random [seedfile]\n", stdout);
    }
  else if ( !strcmp (*argv, "md"))
    {
      if (argc == 1)
        md_bench (NULL);
      else
        for (argc--, argv++; argc; argc--, argv++)
          md_bench ( *argv );
    }
  else if ( !strcmp (*argv, "cipher"))
    {
      if (argc == 1)
        cipher_bench (NULL);
      else
        for (argc--, argv++; argc; argc--, argv++)
          cipher_bench ( *argv );
    }
  else if ( !strcmp (*argv, "mpi"))
    {
        mpi_bench ();
    }
  else if ( !strcmp (*argv, "rsa"))
    {
        gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);
        rsa_bench (100, 1, no_blinding);
    }
  else if ( !strcmp (*argv, "dsa"))
    {
        gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);
        dsa_bench (100, 1);
    }
  else if ( !strcmp (*argv, "ecc"))
    {
        gcry_control (GCRYCTL_ENABLE_QUICK_RANDOM, 0);
        ecc_bench (100, 1);
    }
  else
    {
      fprintf (stderr, PGM ": bad arguments\n");
      return 1;
    }


  if (in_fips_mode && !gcry_fips_mode_active ())
    fprintf (stderr, PGM ": FIPS mode is not anymore active\n");
  
  return 0;
}

