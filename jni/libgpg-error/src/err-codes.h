/* Output of mkstrtable.awk.  DO NOT EDIT.  */

/* err-codes.h - List of error codes and their description.
   Copyright (C) 2003, 2004 g10 Code GmbH

   This file is part of libgpg-error.

   libgpg-error is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
 
   libgpg-error is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.
 
   You should have received a copy of the GNU Lesser General Public
   License along with libgpg-error; if not, write to the Free
   Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */


/* The purpose of this complex string table is to produce
   optimal code with a minimum of relocations.  */

static const char msgstr[] = 
  gettext_noop ("Success") "\0"
  gettext_noop ("General error") "\0"
  gettext_noop ("Unknown packet") "\0"
  gettext_noop ("Unknown version in packet") "\0"
  gettext_noop ("Invalid public key algorithm") "\0"
  gettext_noop ("Invalid digest algorithm") "\0"
  gettext_noop ("Bad public key") "\0"
  gettext_noop ("Bad secret key") "\0"
  gettext_noop ("Bad signature") "\0"
  gettext_noop ("No public key") "\0"
  gettext_noop ("Checksum error") "\0"
  gettext_noop ("Bad passphrase") "\0"
  gettext_noop ("Invalid cipher algorithm") "\0"
  gettext_noop ("Keyring open") "\0"
  gettext_noop ("Invalid packet") "\0"
  gettext_noop ("Invalid armor") "\0"
  gettext_noop ("No user ID") "\0"
  gettext_noop ("No secret key") "\0"
  gettext_noop ("Wrong secret key used") "\0"
  gettext_noop ("Bad session key") "\0"
  gettext_noop ("Unknown compression algorithm") "\0"
  gettext_noop ("Number is not prime") "\0"
  gettext_noop ("Invalid encoding method") "\0"
  gettext_noop ("Invalid encryption scheme") "\0"
  gettext_noop ("Invalid signature scheme") "\0"
  gettext_noop ("Invalid attribute") "\0"
  gettext_noop ("No value") "\0"
  gettext_noop ("Not found") "\0"
  gettext_noop ("Value not found") "\0"
  gettext_noop ("Syntax error") "\0"
  gettext_noop ("Bad MPI value") "\0"
  gettext_noop ("Invalid passphrase") "\0"
  gettext_noop ("Invalid signature class") "\0"
  gettext_noop ("Resources exhausted") "\0"
  gettext_noop ("Invalid keyring") "\0"
  gettext_noop ("Trust DB error") "\0"
  gettext_noop ("Bad certificate") "\0"
  gettext_noop ("Invalid user ID") "\0"
  gettext_noop ("Unexpected error") "\0"
  gettext_noop ("Time conflict") "\0"
  gettext_noop ("Keyserver error") "\0"
  gettext_noop ("Wrong public key algorithm") "\0"
  gettext_noop ("Tribute to D. A.") "\0"
  gettext_noop ("Weak encryption key") "\0"
  gettext_noop ("Invalid key length") "\0"
  gettext_noop ("Invalid argument") "\0"
  gettext_noop ("Syntax error in URI") "\0"
  gettext_noop ("Invalid URI") "\0"
  gettext_noop ("Network error") "\0"
  gettext_noop ("Unknown host") "\0"
  gettext_noop ("Selftest failed") "\0"
  gettext_noop ("Data not encrypted") "\0"
  gettext_noop ("Data not processed") "\0"
  gettext_noop ("Unusable public key") "\0"
  gettext_noop ("Unusable secret key") "\0"
  gettext_noop ("Invalid value") "\0"
  gettext_noop ("Bad certificate chain") "\0"
  gettext_noop ("Missing certificate") "\0"
  gettext_noop ("No data") "\0"
  gettext_noop ("Bug") "\0"
  gettext_noop ("Not supported") "\0"
  gettext_noop ("Invalid operation code") "\0"
  gettext_noop ("Timeout") "\0"
  gettext_noop ("Internal error") "\0"
  gettext_noop ("EOF (gcrypt)") "\0"
  gettext_noop ("Invalid object") "\0"
  gettext_noop ("Provided object is too short") "\0"
  gettext_noop ("Provided object is too large") "\0"
  gettext_noop ("Missing item in object") "\0"
  gettext_noop ("Not implemented") "\0"
  gettext_noop ("Conflicting use") "\0"
  gettext_noop ("Invalid cipher mode") "\0"
  gettext_noop ("Invalid flag") "\0"
  gettext_noop ("Invalid handle") "\0"
  gettext_noop ("Result truncated") "\0"
  gettext_noop ("Incomplete line") "\0"
  gettext_noop ("Invalid response") "\0"
  gettext_noop ("No agent running") "\0"
  gettext_noop ("agent error") "\0"
  gettext_noop ("Invalid data") "\0"
  gettext_noop ("Unspecific Assuan server fault") "\0"
  gettext_noop ("General Assuan error") "\0"
  gettext_noop ("Invalid session key") "\0"
  gettext_noop ("Invalid S-expression") "\0"
  gettext_noop ("Unsupported algorithm") "\0"
  gettext_noop ("No pinentry") "\0"
  gettext_noop ("pinentry error") "\0"
  gettext_noop ("Bad PIN") "\0"
  gettext_noop ("Invalid name") "\0"
  gettext_noop ("Bad data") "\0"
  gettext_noop ("Invalid parameter") "\0"
  gettext_noop ("Wrong card") "\0"
  gettext_noop ("No dirmngr") "\0"
  gettext_noop ("dirmngr error") "\0"
  gettext_noop ("Certificate revoked") "\0"
  gettext_noop ("No CRL known") "\0"
  gettext_noop ("CRL too old") "\0"
  gettext_noop ("Line too long") "\0"
  gettext_noop ("Not trusted") "\0"
  gettext_noop ("Operation cancelled") "\0"
  gettext_noop ("Bad CA certificate") "\0"
  gettext_noop ("Certificate expired") "\0"
  gettext_noop ("Certificate too young") "\0"
  gettext_noop ("Unsupported certificate") "\0"
  gettext_noop ("Unknown S-expression") "\0"
  gettext_noop ("Unsupported protection") "\0"
  gettext_noop ("Corrupted protection") "\0"
  gettext_noop ("Ambiguous name") "\0"
  gettext_noop ("Card error") "\0"
  gettext_noop ("Card reset required") "\0"
  gettext_noop ("Card removed") "\0"
  gettext_noop ("Invalid card") "\0"
  gettext_noop ("Card not present") "\0"
  gettext_noop ("No PKCS15 application") "\0"
  gettext_noop ("Not confirmed") "\0"
  gettext_noop ("Configuration error") "\0"
  gettext_noop ("No policy match") "\0"
  gettext_noop ("Invalid index") "\0"
  gettext_noop ("Invalid ID") "\0"
  gettext_noop ("No SmartCard daemon") "\0"
  gettext_noop ("SmartCard daemon error") "\0"
  gettext_noop ("Unsupported protocol") "\0"
  gettext_noop ("Bad PIN method") "\0"
  gettext_noop ("Card not initialized") "\0"
  gettext_noop ("Unsupported operation") "\0"
  gettext_noop ("Wrong key usage") "\0"
  gettext_noop ("Nothing found") "\0"
  gettext_noop ("Wrong blob type") "\0"
  gettext_noop ("Missing value") "\0"
  gettext_noop ("Hardware problem") "\0"
  gettext_noop ("PIN blocked") "\0"
  gettext_noop ("Conditions of use not satisfied") "\0"
  gettext_noop ("PINs are not synced") "\0"
  gettext_noop ("Invalid CRL") "\0"
  gettext_noop ("BER error") "\0"
  gettext_noop ("Invalid BER") "\0"
  gettext_noop ("Element not found") "\0"
  gettext_noop ("Identifier not found") "\0"
  gettext_noop ("Invalid tag") "\0"
  gettext_noop ("Invalid length") "\0"
  gettext_noop ("Invalid key info") "\0"
  gettext_noop ("Unexpected tag") "\0"
  gettext_noop ("Not DER encoded") "\0"
  gettext_noop ("No CMS object") "\0"
  gettext_noop ("Invalid CMS object") "\0"
  gettext_noop ("Unknown CMS object") "\0"
  gettext_noop ("Unsupported CMS object") "\0"
  gettext_noop ("Unsupported encoding") "\0"
  gettext_noop ("Unsupported CMS version") "\0"
  gettext_noop ("Unknown algorithm") "\0"
  gettext_noop ("Invalid crypto engine") "\0"
  gettext_noop ("Public key not trusted") "\0"
  gettext_noop ("Decryption failed") "\0"
  gettext_noop ("Key expired") "\0"
  gettext_noop ("Signature expired") "\0"
  gettext_noop ("Encoding problem") "\0"
  gettext_noop ("Invalid state") "\0"
  gettext_noop ("Duplicated value") "\0"
  gettext_noop ("Missing action") "\0"
  gettext_noop ("ASN.1 module not found") "\0"
  gettext_noop ("Invalid OID string") "\0"
  gettext_noop ("Invalid time") "\0"
  gettext_noop ("Invalid CRL object") "\0"
  gettext_noop ("Unsupported CRL version") "\0"
  gettext_noop ("Invalid certificate object") "\0"
  gettext_noop ("Unknown name") "\0"
  gettext_noop ("A locale function failed") "\0"
  gettext_noop ("Not locked") "\0"
  gettext_noop ("Protocol violation") "\0"
  gettext_noop ("Invalid MAC") "\0"
  gettext_noop ("Invalid request") "\0"
  gettext_noop ("Unknown extension") "\0"
  gettext_noop ("Unknown critical extension") "\0"
  gettext_noop ("Locked") "\0"
  gettext_noop ("Unknown option") "\0"
  gettext_noop ("Unknown command") "\0"
  gettext_noop ("Operation not yet finished") "\0"
  gettext_noop ("Buffer too short") "\0"
  gettext_noop ("Invalid length specifier in S-expression") "\0"
  gettext_noop ("String too long in S-expression") "\0"
  gettext_noop ("Unmatched parentheses in S-expression") "\0"
  gettext_noop ("S-expression not canonical") "\0"
  gettext_noop ("Bad character in S-expression") "\0"
  gettext_noop ("Bad quotation in S-expression") "\0"
  gettext_noop ("Zero prefix in S-expression") "\0"
  gettext_noop ("Nested display hints in S-expression") "\0"
  gettext_noop ("Unmatched display hints") "\0"
  gettext_noop ("Unexpected reserved punctuation in S-expression") "\0"
  gettext_noop ("Bad hexadecimal character in S-expression") "\0"
  gettext_noop ("Odd hexadecimal numbers in S-expression") "\0"
  gettext_noop ("Bad octadecimal character in S-expression") "\0"
  gettext_noop ("General IPC error") "\0"
  gettext_noop ("IPC accept call failed") "\0"
  gettext_noop ("IPC connect call failed") "\0"
  gettext_noop ("Invalid IPC response") "\0"
  gettext_noop ("Invalid value passed to IPC") "\0"
  gettext_noop ("Incomplete line passed to IPC") "\0"
  gettext_noop ("Line passed to IPC too long") "\0"
  gettext_noop ("Nested IPC commands") "\0"
  gettext_noop ("No data callback in IPC") "\0"
  gettext_noop ("No inquire callback in IPC") "\0"
  gettext_noop ("Not an IPC server") "\0"
  gettext_noop ("Not an IPC client") "\0"
  gettext_noop ("Problem starting IPC server") "\0"
  gettext_noop ("IPC read error") "\0"
  gettext_noop ("IPC write error") "\0"
  gettext_noop ("Too much data for IPC layer") "\0"
  gettext_noop ("Unexpected IPC command") "\0"
  gettext_noop ("Unknown IPC command") "\0"
  gettext_noop ("IPC syntax error") "\0"
  gettext_noop ("IPC call has been cancelled") "\0"
  gettext_noop ("No input source for IPC") "\0"
  gettext_noop ("No output source for IPC") "\0"
  gettext_noop ("IPC parameter error") "\0"
  gettext_noop ("Unknown IPC inquire") "\0"
  gettext_noop ("User defined error code 1") "\0"
  gettext_noop ("User defined error code 2") "\0"
  gettext_noop ("User defined error code 3") "\0"
  gettext_noop ("User defined error code 4") "\0"
  gettext_noop ("User defined error code 5") "\0"
  gettext_noop ("User defined error code 6") "\0"
  gettext_noop ("User defined error code 7") "\0"
  gettext_noop ("User defined error code 8") "\0"
  gettext_noop ("User defined error code 9") "\0"
  gettext_noop ("User defined error code 10") "\0"
  gettext_noop ("User defined error code 11") "\0"
  gettext_noop ("User defined error code 12") "\0"
  gettext_noop ("User defined error code 13") "\0"
  gettext_noop ("User defined error code 14") "\0"
  gettext_noop ("User defined error code 15") "\0"
  gettext_noop ("User defined error code 16") "\0"
  gettext_noop ("System error w/o errno") "\0"
  gettext_noop ("Unknown system error") "\0"
  gettext_noop ("End of file") "\0"
  gettext_noop ("Unknown error code");

static const int msgidx[] =
  {
    0,
    8,
    22,
    37,
    63,
    92,
    117,
    132,
    147,
    161,
    175,
    190,
    205,
    230,
    243,
    258,
    272,
    283,
    297,
    319,
    335,
    365,
    385,
    409,
    435,
    460,
    478,
    487,
    497,
    513,
    526,
    540,
    559,
    583,
    603,
    619,
    634,
    650,
    666,
    683,
    697,
    713,
    740,
    757,
    777,
    796,
    813,
    833,
    845,
    859,
    872,
    888,
    907,
    926,
    946,
    966,
    980,
    1002,
    1022,
    1030,
    1034,
    1048,
    1071,
    1079,
    1094,
    1107,
    1122,
    1151,
    1180,
    1203,
    1219,
    1235,
    1255,
    1268,
    1283,
    1300,
    1316,
    1333,
    1350,
    1362,
    1375,
    1406,
    1427,
    1447,
    1468,
    1490,
    1502,
    1517,
    1525,
    1538,
    1547,
    1565,
    1576,
    1587,
    1601,
    1621,
    1634,
    1646,
    1660,
    1672,
    1692,
    1711,
    1731,
    1753,
    1777,
    1798,
    1821,
    1842,
    1857,
    1868,
    1888,
    1901,
    1914,
    1931,
    1953,
    1967,
    1987,
    2003,
    2017,
    2028,
    2048,
    2071,
    2092,
    2107,
    2128,
    2150,
    2166,
    2180,
    2196,
    2210,
    2227,
    2239,
    2271,
    2291,
    2303,
    2313,
    2325,
    2343,
    2364,
    2376,
    2391,
    2408,
    2423,
    2439,
    2453,
    2472,
    2491,
    2514,
    2535,
    2559,
    2577,
    2599,
    2622,
    2640,
    2652,
    2670,
    2687,
    2701,
    2718,
    2733,
    2756,
    2775,
    2788,
    2807,
    2831,
    2858,
    2871,
    2896,
    2907,
    2926,
    2938,
    2954,
    2972,
    2999,
    3006,
    3021,
    3037,
    3064,
    3081,
    3122,
    3154,
    3192,
    3219,
    3249,
    3279,
    3307,
    3344,
    3368,
    3416,
    3458,
    3498,
    3540,
    3558,
    3581,
    3605,
    3626,
    3654,
    3684,
    3712,
    3732,
    3756,
    3783,
    3801,
    3819,
    3847,
    3862,
    3878,
    3906,
    3929,
    3949,
    3966,
    3994,
    4018,
    4043,
    4063,
    4083,
    4109,
    4135,
    4161,
    4187,
    4213,
    4239,
    4265,
    4291,
    4317,
    4344,
    4371,
    4398,
    4425,
    4452,
    4479,
    4506,
    4529,
    4550,
    4562
  };

#define msgidxof(code) (0 ? -1 \
  : ((code >= 0) && (code <= 175)) ? (code - 0) \
  : ((code >= 199) && (code <= 213)) ? (code - 23) \
  : ((code >= 257) && (code <= 271)) ? (code - 66) \
  : ((code >= 273) && (code <= 281)) ? (code - 67) \
  : ((code >= 1024) && (code <= 1039)) ? (code - 809) \
  : ((code >= 16381) && (code <= 16383)) ? (code - 16150) \
  : 16384 - 16150)
