/* mpitests.c  -  basic mpi tests
 *	Copyright (C) 2001, 2002, 2003, 2006 Free Software Foundation, Inc.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA. 
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
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

static int verbose;
static int debug;


static void
die (const char *format, ...)
{
  va_list arg_ptr;

  va_start (arg_ptr, format);
  vfprintf (stderr, format, arg_ptr);
  va_end (arg_ptr);
  exit (1);
}



/* Set up some test patterns */

/* 48 bytes with value 1: this results in 8 limbs for 64bit limbs, 16limb for 32 bit limbs */
unsigned char ones[] = {
  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
  0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
};

/* 48 bytes with value 2: this results in 8 limbs for 64bit limbs, 16limb for 32 bit limbs */
unsigned char twos[] = {
  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
  0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02
};

/* 48 bytes with value 3: this results in 8 limbs for 64bit limbs, 16limb for 32 bit limbs */
unsigned char threes[] = {
  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
  0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
};

/* 48 bytes with value 0x80: this results in 8 limbs for 64bit limbs, 16limb for 32 bit limbs */
unsigned char eighties[] = {
  0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
  0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
  0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80
};

/* 48 bytes with value 0xff: this results in 8 limbs for 64bit limbs, 16limb for 32 bit limbs */
unsigned char manyff[] = {
  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
};



static int 
test_add (void)
{
  gcry_mpi_t one;
  gcry_mpi_t two;
  gcry_mpi_t ff;
  gcry_mpi_t result;
  unsigned char* pc;
  
  gcry_mpi_scan(&one, GCRYMPI_FMT_USG, ones, sizeof(ones), NULL);
  gcry_mpi_scan(&two, GCRYMPI_FMT_USG, twos, sizeof(twos), NULL);
  gcry_mpi_scan(&ff, GCRYMPI_FMT_USG, manyff, sizeof(manyff), NULL);
  result = gcry_mpi_new(0);
  
  gcry_mpi_add(result, one, two);
  gcry_mpi_aprint(GCRYMPI_FMT_HEX, &pc, NULL, result);
  if (verbose)
    printf("Result of one plus two:\n%s\n", pc);
  gcry_free(pc);

  gcry_mpi_add(result, ff, one);
  gcry_mpi_aprint(GCRYMPI_FMT_HEX, &pc, NULL, result);
  if (verbose)
    printf("Result of ff plus one:\n%s\n", pc);
  gcry_free(pc);
  
  gcry_mpi_release(one);
  gcry_mpi_release(two);
  gcry_mpi_release(ff);
  gcry_mpi_release(result);
  return 1;
}


static int 
test_sub (void)
{
  gcry_mpi_t one;
  gcry_mpi_t two;
  gcry_mpi_t result;
  unsigned char* pc;
  
  gcry_mpi_scan(&one, GCRYMPI_FMT_USG, ones, sizeof(ones), NULL);
  gcry_mpi_scan(&two, GCRYMPI_FMT_USG, twos, sizeof(twos), NULL);
  result = gcry_mpi_new(0);
  gcry_mpi_sub(result, two, one);
  
  gcry_mpi_aprint(GCRYMPI_FMT_HEX, &pc, NULL, result);
  if (verbose)
    printf("Result of two minus one:\n%s\n", pc);
  gcry_free(pc);
  
  gcry_mpi_release(one);
  gcry_mpi_release(two);
  gcry_mpi_release(result);
  return 1;
}


static int 
test_mul (void)
{
  gcry_mpi_t two;
  gcry_mpi_t three;
  gcry_mpi_t result;
  unsigned char* pc;
  
  gcry_mpi_scan(&two, GCRYMPI_FMT_USG, twos, sizeof(twos), NULL);
  gcry_mpi_scan(&three, GCRYMPI_FMT_USG, threes, sizeof(threes), NULL);
  result = gcry_mpi_new(0);
  gcry_mpi_mul(result, two, three);
  
  gcry_mpi_aprint(GCRYMPI_FMT_HEX, &pc, NULL, result);
  if (verbose)
    printf("Result of two mul three:\n%s\n", pc);
  gcry_free(pc);
  
  gcry_mpi_release(two);
  gcry_mpi_release(three);
  gcry_mpi_release(result);
  return 1;
}


/* What we test here is that we don't overwrite our args and that
   using thne same mpi for several args works.  */
static int
test_powm (void)
{
  int b_int = 17;
  int e_int = 3;
  int m_int = 19;  
  gcry_mpi_t base = gcry_mpi_set_ui (NULL, b_int);
  gcry_mpi_t exp = gcry_mpi_set_ui (NULL, e_int);
  gcry_mpi_t mod = gcry_mpi_set_ui (NULL, m_int);
  gcry_mpi_t res = gcry_mpi_new (0);

  gcry_mpi_powm (res, base, exp, mod);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (exp, e_int))
    die ("test_powm_ui failed for exp at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (mod, m_int))
    die ("test_powm failed for mod at %d\n", __LINE__);

  /* Check using base for the result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui (exp, e_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (base, base, exp, mod);
  if (gcry_mpi_cmp (res, base))
    die ("test_powm failed at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (exp, e_int))
    die ("test_powm_ui failed for exp at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (mod, m_int))
    die ("test_powm failed for mod at %d\n", __LINE__);

  /* Check using exp for the result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui (exp, e_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (exp, base, exp, mod);
  if (gcry_mpi_cmp (res, exp))
    die ("test_powm failed at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (mod, m_int))
    die ("test_powm failed for mod at %d\n", __LINE__);

  /* Check using mod for the result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui (exp, e_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (mod, base, exp, mod);
  if (gcry_mpi_cmp (res, mod))
    die ("test_powm failed at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (exp, e_int))
    die ("test_powm_ui failed for exp at %d\n", __LINE__);

  /* Now check base ^ base mod mod.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (res, base, base, mod);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (mod, m_int))
    die ("test_powm failed for mod at %d\n", __LINE__);

  /* Check base ^ base mod mod with base as result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (base, base, base, mod);
  if (gcry_mpi_cmp (res, base))
    die ("test_powm failed at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (mod, m_int))
    die ("test_powm failed for mod at %d\n", __LINE__);

  /* Check base ^ base mod mod with mod as result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_set_ui(mod, m_int);
  gcry_mpi_powm (mod, base, base, mod);
  if (gcry_mpi_cmp (res, mod))
    die ("test_powm failed at %d\n", __LINE__);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);

  /* Now check base ^ base mod base.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_powm (res, base, base, base);
  if (gcry_mpi_cmp_ui (base, b_int))
    die ("test_powm failed for base at %d\n", __LINE__);

  /* Check base ^ base mod base with base as result.  */
  gcry_mpi_set_ui (base, b_int);
  gcry_mpi_powm (base, base, base, base);
  if (gcry_mpi_cmp (res, base))
    die ("test_powm failed at %d\n", __LINE__);

  /* Fixme: We should add the rest of the cases of course.  */



  return 1;
}


int 
main (int argc, char* argv[])
{
  if (argc > 1 && !strcmp (argv[1], "--verbose"))
    verbose = 1;
  else if (argc > 1 && !strcmp (argv[1], "--debug"))
    verbose = debug = 1;

  if (!gcry_check_version (GCRYPT_VERSION))
    {
      fputs ("version mismatch\n", stderr);
      exit (1);
    }
  gcry_control(GCRYCTL_DISABLE_SECMEM);

  test_add ();
  test_sub ();
  test_mul ();
  test_powm ();

  return 0;
}

