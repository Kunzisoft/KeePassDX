/* rsa.c - RSA implementation
 * Copyright (C) 1997, 1998, 1999 by Werner Koch (dd9jn)
 * Copyright (C) 2000, 2001, 2002, 2003, 2008 Free Software Foundation, Inc.
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

/* This code uses an algorithm protected by U.S. Patent #4,405,829
   which expired on September 20, 2000.  The patent holder placed that
   patent into the public domain on Sep 6th, 2000.
*/

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "g10lib.h"
#include "mpi.h"
#include "cipher.h"


typedef struct
{
  gcry_mpi_t n;	    /* modulus */
  gcry_mpi_t e;	    /* exponent */
} RSA_public_key;


typedef struct
{
  gcry_mpi_t n;	    /* public modulus */
  gcry_mpi_t e;	    /* public exponent */
  gcry_mpi_t d;	    /* exponent */
  gcry_mpi_t p;	    /* prime  p. */
  gcry_mpi_t q;	    /* prime  q. */
  gcry_mpi_t u;	    /* inverse of p mod q. */
} RSA_secret_key;


/* A sample 1024 bit RSA key used for the selftests.  */
static const char sample_secret_key[] =
"(private-key"
" (rsa"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
"      2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
"      ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
"      891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)"
"  (e #010001#)"
"  (d #046129f2489d71579be0a75fe029bd6cdb574ebf57ea8a5b0fda942cab943b11"
"      7d7bb95e5d28875e0f9fc5fcc06a72f6d502464dabded78ef6b716177b83d5bd"
"      c543dc5d3fed932e59f5897e92e6f58a0f33424106a3b6fa2cbf877510e4ac21"
"      c3ee47851e97d12996222ac3566d4ccb0b83d164074abf7de655fc2446da1781#)"
"  (p #00e861b700e17e8afe6837e7512e35b6ca11d0ae47d8b85161c67baf64377213"
"      fe52d772f2035b3ca830af41d8a4120e1c1c70d12cc22f00d28d31dd48a8d424f1#)"
"  (q #00f7a7ca5367c661f8e62df34f0d05c10c88e5492348dd7bddc942c9a8f369f9"
"      35a07785d2db805215ed786e4285df1658eed3ce84f469b81b50d358407b4ad361#)"
"  (u #304559a9ead56d2309d203811a641bb1a09626bc8eb36fffa23c968ec5bd891e"
"      ebbafc73ae666e01ba7c8990bae06cc2bbe10b75e69fcacb353a6473079d8e9b#)))";
/* A sample 1024 bit RSA key used for the selftests (public only).  */
static const char sample_public_key[] = 
"(public-key"
" (rsa"
"  (n #00e0ce96f90b6c9e02f3922beada93fe50a875eac6bcc18bb9a9cf2e84965caa"
"      2d1ff95a7f542465c6c0c19d276e4526ce048868a7a914fd343cc3a87dd74291"
"      ffc565506d5bbb25cbac6a0e2dd1f8bcaab0d4a29c2f37c950f363484bf269f7"
"      891440464baf79827e03a36e70b814938eebdc63e964247be75dc58b014b7ea251#)"
"  (e #010001#)))";




static int test_keys (RSA_secret_key *sk, unsigned nbits);
static int  check_secret_key (RSA_secret_key *sk);
static void public (gcry_mpi_t output, gcry_mpi_t input, RSA_public_key *skey);
static void secret (gcry_mpi_t output, gcry_mpi_t input, RSA_secret_key *skey);


/* Check that a freshly generated key actually works.  Returns 0 on success. */
static int
test_keys (RSA_secret_key *sk, unsigned int nbits)
{
  int result = -1; /* Default to failure.  */
  RSA_public_key pk;
  gcry_mpi_t plaintext = gcry_mpi_new (nbits);
  gcry_mpi_t ciphertext = gcry_mpi_new (nbits);
  gcry_mpi_t decr_plaintext = gcry_mpi_new (nbits);
  gcry_mpi_t signature = gcry_mpi_new (nbits);

  /* Put the relevant parameters into a public key structure.  */
  pk.n = sk->n;
  pk.e = sk->e;

  /* Create a random plaintext.  */
  gcry_mpi_randomize (plaintext, nbits, GCRY_WEAK_RANDOM);

  /* Encrypt using the public key.  */
  public (ciphertext, plaintext, &pk);

  /* Check that the cipher text does not match the plaintext.  */
  if (!gcry_mpi_cmp (ciphertext, plaintext))
    goto leave; /* Ciphertext is identical to the plaintext.  */

  /* Decrypt using the secret key.  */
  secret (decr_plaintext, ciphertext, sk);

  /* Check that the decrypted plaintext matches the original plaintext.  */
  if (gcry_mpi_cmp (decr_plaintext, plaintext))
    goto leave; /* Plaintext does not match.  */

  /* Create another random plaintext as data for signature checking.  */
  gcry_mpi_randomize (plaintext, nbits, GCRY_WEAK_RANDOM);

  /* Use the RSA secret function to create a signature of the plaintext.  */
  secret (signature, plaintext, sk);
  
  /* Use the RSA public function to verify this signature.  */
  public (decr_plaintext, signature, &pk);
  if (gcry_mpi_cmp (decr_plaintext, plaintext))
    goto leave; /* Signature does not match.  */

  /* Modify the signature and check that the signing fails.  */
  gcry_mpi_add_ui (signature, signature, 1);
  public (decr_plaintext, signature, &pk);
  if (!gcry_mpi_cmp (decr_plaintext, plaintext))
    goto leave; /* Signature matches but should not.  */

  result = 0; /* All tests succeeded.  */

 leave:
  gcry_mpi_release (signature);
  gcry_mpi_release (decr_plaintext);
  gcry_mpi_release (ciphertext);
  gcry_mpi_release (plaintext);
  return result;
}


/* Callback used by the prime generation to test whether the exponent
   is suitable. Returns 0 if the test has been passed. */
static int
check_exponent (void *arg, gcry_mpi_t a)
{
  gcry_mpi_t e = arg;
  gcry_mpi_t tmp;
  int result;
  
  mpi_sub_ui (a, a, 1);
  tmp = _gcry_mpi_alloc_like (a);
  result = !gcry_mpi_gcd(tmp, e, a); /* GCD is not 1. */
  gcry_mpi_release (tmp);
  mpi_add_ui (a, a, 1);
  return result;
}

/****************
 * Generate a key pair with a key of size NBITS.  
 * USE_E = 0 let Libcgrypt decide what exponent to use.
 *       = 1 request the use of a "secure" exponent; this is required by some 
 *           specification to be 65537.
 *       > 2 Use this public exponent.  If the given exponent
 *           is not odd one is internally added to it. 
 * TRANSIENT_KEY:  If true, generate the primes using the standard RNG.
 * Returns: 2 structures filled with all needed values
 */
static gpg_err_code_t
generate_std (RSA_secret_key *sk, unsigned int nbits, unsigned long use_e,
              int transient_key)
{
  gcry_mpi_t p, q; /* the two primes */
  gcry_mpi_t d;    /* the private key */
  gcry_mpi_t u;
  gcry_mpi_t t1, t2;
  gcry_mpi_t n;    /* the public key */
  gcry_mpi_t e;    /* the exponent */
  gcry_mpi_t phi;  /* helper: (p-1)(q-1) */
  gcry_mpi_t g;
  gcry_mpi_t f;
  gcry_random_level_t random_level;

  if (fips_mode ())
    {
      if (nbits < 1024)
        return GPG_ERR_INV_VALUE;
      if (transient_key)
        return GPG_ERR_INV_VALUE;
    }

  /* The random quality depends on the transient_key flag.  */
  random_level = transient_key ? GCRY_STRONG_RANDOM : GCRY_VERY_STRONG_RANDOM;

  /* Make sure that nbits is even so that we generate p, q of equal size. */
  if ( (nbits&1) )
    nbits++; 

  if (use_e == 1)   /* Alias for a secure value */
    use_e = 65537;  /* as demanded by Sphinx. */

  /* Public exponent:
     In general we use 41 as this is quite fast and more secure than the
     commonly used 17.  Benchmarking the RSA verify function
     with a 1024 bit key yields (2001-11-08): 
     e=17    0.54 ms
     e=41    0.75 ms
     e=257   0.95 ms
     e=65537 1.80 ms
  */
  e = mpi_alloc( (32+BITS_PER_MPI_LIMB-1)/BITS_PER_MPI_LIMB );
  if (!use_e)
    mpi_set_ui (e, 41);     /* This is a reasonable secure and fast value */
  else 
    {
      use_e |= 1; /* make sure this is odd */
      mpi_set_ui (e, use_e); 
    }
    
  n = gcry_mpi_new (nbits);

  p = q = NULL;
  do
    {
      /* select two (very secret) primes */
      if (p)
        gcry_mpi_release (p);
      if (q)
        gcry_mpi_release (q);
      if (use_e)
        { /* Do an extra test to ensure that the given exponent is
             suitable. */
          p = _gcry_generate_secret_prime (nbits/2, random_level,
                                           check_exponent, e);
          q = _gcry_generate_secret_prime (nbits/2, random_level,
                                           check_exponent, e);
        }
      else
        { /* We check the exponent later. */
          p = _gcry_generate_secret_prime (nbits/2, random_level, NULL, NULL);
          q = _gcry_generate_secret_prime (nbits/2, random_level, NULL, NULL);
        }
      if (mpi_cmp (p, q) > 0 ) /* p shall be smaller than q (for calc of u)*/
        mpi_swap(p,q);
      /* calculate the modulus */
      mpi_mul( n, p, q );
    }
  while ( mpi_get_nbits(n) != nbits );

  /* calculate Euler totient: phi = (p-1)(q-1) */
  t1 = mpi_alloc_secure( mpi_get_nlimbs(p) );
  t2 = mpi_alloc_secure( mpi_get_nlimbs(p) );
  phi = gcry_mpi_snew ( nbits );
  g	= gcry_mpi_snew ( nbits );
  f	= gcry_mpi_snew ( nbits );
  mpi_sub_ui( t1, p, 1 );
  mpi_sub_ui( t2, q, 1 );
  mpi_mul( phi, t1, t2 );
  gcry_mpi_gcd(g, t1, t2);
  mpi_fdiv_q(f, phi, g);

  while (!gcry_mpi_gcd(t1, e, phi)) /* (while gcd is not 1) */
    {
      if (use_e)
        BUG (); /* The prime generator already made sure that we
                   never can get to here. */
      mpi_add_ui (e, e, 2);
    }

  /* calculate the secret key d = e^1 mod phi */
  d = gcry_mpi_snew ( nbits );
  mpi_invm(d, e, f );
  /* calculate the inverse of p and q (used for chinese remainder theorem)*/
  u = gcry_mpi_snew ( nbits );
  mpi_invm(u, p, q );

  if( DBG_CIPHER )
    {
      log_mpidump("  p= ", p );
      log_mpidump("  q= ", q );
      log_mpidump("phi= ", phi );
      log_mpidump("  g= ", g );
      log_mpidump("  f= ", f );
      log_mpidump("  n= ", n );
      log_mpidump("  e= ", e );
      log_mpidump("  d= ", d );
      log_mpidump("  u= ", u );
    }

  gcry_mpi_release (t1);
  gcry_mpi_release (t2);
  gcry_mpi_release (phi);
  gcry_mpi_release (f);
  gcry_mpi_release (g);

  sk->n = n;
  sk->e = e;
  sk->p = p;
  sk->q = q;
  sk->d = d;
  sk->u = u;

  /* Now we can test our keys. */
  if (test_keys (sk, nbits - 64))
    {
      gcry_mpi_release (sk->n); sk->n = NULL;
      gcry_mpi_release (sk->e); sk->e = NULL;
      gcry_mpi_release (sk->p); sk->p = NULL;
      gcry_mpi_release (sk->q); sk->q = NULL;
      gcry_mpi_release (sk->d); sk->d = NULL;
      gcry_mpi_release (sk->u); sk->u = NULL;
      fips_signal_error ("self-test after key generation failed");
      return GPG_ERR_SELFTEST_FAILED;
    }

  return 0;
}


/* Helper for generate_x931.  */
static gcry_mpi_t 
gen_x931_parm_xp (unsigned int nbits)
{
  gcry_mpi_t xp;

  xp = gcry_mpi_snew (nbits);
  gcry_mpi_randomize (xp, nbits, GCRY_VERY_STRONG_RANDOM);
      
  /* The requirement for Xp is:

       sqrt{2}*2^{nbits-1} <= xp <= 2^{nbits} - 1

     We set the two high order bits to 1 to satisfy the lower bound.
     By using mpi_set_highbit we make sure that the upper bound is
     satisfied as well.  */
  mpi_set_highbit (xp, nbits-1);
  mpi_set_bit (xp, nbits-2);
  gcry_assert ( mpi_get_nbits (xp) == nbits );
  
  return xp;
}     


/* Helper for generate_x931.  */
static gcry_mpi_t 
gen_x931_parm_xi (void)
{
  gcry_mpi_t xi;

  xi = gcry_mpi_snew (101);
  gcry_mpi_randomize (xi, 101, GCRY_VERY_STRONG_RANDOM);
  mpi_set_highbit (xi, 100);
  gcry_assert ( mpi_get_nbits (xi) == 101 );
  
  return xi;
}     



/* Variant of the standard key generation code using the algorithm
   from X9.31.  Using this algorithm has the advantage that the
   generation can be made deterministic which is required for CAVS
   testing.  */
static gpg_err_code_t
generate_x931 (RSA_secret_key *sk, unsigned int nbits, unsigned long e_value,
               gcry_sexp_t deriveparms, int *swapped)
{
  gcry_mpi_t p, q; /* The two primes.  */
  gcry_mpi_t e;    /* The public exponent.  */
  gcry_mpi_t n;    /* The public key.  */
  gcry_mpi_t d;    /* The private key */
  gcry_mpi_t u;    /* The inverse of p and q.  */
  gcry_mpi_t pm1;  /* p - 1  */
  gcry_mpi_t qm1;  /* q - 1  */
  gcry_mpi_t phi;  /* Euler totient.  */
  gcry_mpi_t f, g; /* Helper.  */

  *swapped = 0;

  if (e_value == 1)   /* Alias for a secure value. */
    e_value = 65537; 

  /* Point 1 of section 4.1:  k = 1024 + 256s with S >= 0  */
  if (nbits < 1024 || (nbits % 256))
    return GPG_ERR_INV_VALUE;
  
  /* Point 2:  2 <= bitlength(e) < 2^{k-2}
     Note that we do not need to check the upper bound because we use
     an unsigned long for E and thus there is no way for E to reach
     that limit.  */
  if (e_value < 3)
    return GPG_ERR_INV_VALUE;
     
  /* Our implementaion requires E to be odd.  */
  if (!(e_value & 1))
    return GPG_ERR_INV_VALUE;

  /* Point 3:  e > 0 or e 0 if it is to be randomly generated.
     We support only a fixed E and thus there is no need for an extra test.  */


  /* Compute or extract the derive parameters.  */
  {
    gcry_mpi_t xp1 = NULL;
    gcry_mpi_t xp2 = NULL;
    gcry_mpi_t xp  = NULL;
    gcry_mpi_t xq1 = NULL;
    gcry_mpi_t xq2 = NULL;
    gcry_mpi_t xq  = NULL;
    gcry_mpi_t tmpval;

    if (!deriveparms)
      {
        /* Not given: Generate them.  */
        xp = gen_x931_parm_xp (nbits/2);
        /* Make sure that |xp - xq| > 2^{nbits - 100} holds.  */
        tmpval = gcry_mpi_snew (nbits/2);
        do
          {
            gcry_mpi_release (xq);
            xq = gen_x931_parm_xp (nbits/2);
            mpi_sub (tmpval, xp, xq);
          }
        while (mpi_get_nbits (tmpval) <= (nbits/2 - 100));
        gcry_mpi_release (tmpval);

        xp1 = gen_x931_parm_xi ();
        xp2 = gen_x931_parm_xi ();
        xq1 = gen_x931_parm_xi ();
        xq2 = gen_x931_parm_xi ();

      }
    else
      {
        /* Parameters to derive the key are given.  */
        struct { const char *name; gcry_mpi_t *value; } tbl[] = {
          { "Xp1", &xp1 },
          { "Xp2", &xp2 },
          { "Xp",  &xp  },
          { "Xq1", &xq1 },
          { "Xq2", &xq2 },
          { "Xq",  &xq  },
          { NULL,  NULL }
        };
        int idx;
        gcry_sexp_t oneparm;
        
        for (idx=0; tbl[idx].name; idx++)
          {
            oneparm = gcry_sexp_find_token (deriveparms, tbl[idx].name, 0);
            if (oneparm)
              {
                *tbl[idx].value = gcry_sexp_nth_mpi (oneparm, 1,
                                                     GCRYMPI_FMT_USG);
                gcry_sexp_release (oneparm);
              }
          }
        for (idx=0; tbl[idx].name; idx++)
          if (!*tbl[idx].value)
            break;
        if (tbl[idx].name)
          {
            /* At least one parameter is missing.  */
            for (idx=0; tbl[idx].name; idx++)
              gcry_mpi_release (*tbl[idx].value);
            return GPG_ERR_MISSING_VALUE;
          }
      }
    
    e = mpi_alloc_set_ui (e_value); 

    /* Find two prime numbers.  */
    p = _gcry_derive_x931_prime (xp, xp1, xp2, e, NULL, NULL);
    q = _gcry_derive_x931_prime (xq, xq1, xq2, e, NULL, NULL);
    gcry_mpi_release (xp);  xp  = NULL;
    gcry_mpi_release (xp1); xp1 = NULL;
    gcry_mpi_release (xp2); xp2 = NULL;
    gcry_mpi_release (xq);  xq  = NULL; 
    gcry_mpi_release (xq1); xq1 = NULL;
    gcry_mpi_release (xq2); xq2 = NULL;
    if (!p || !q)
      {
        gcry_mpi_release (p);
        gcry_mpi_release (q);
        gcry_mpi_release (e);
        return GPG_ERR_NO_PRIME;
      }
  }


  /* Compute the public modulus.  We make sure that p is smaller than
     q to allow the use of the CRT.  */
  if (mpi_cmp (p, q) > 0 )
    {
      mpi_swap (p, q);
      *swapped = 1;
    }
  n = gcry_mpi_new (nbits);
  mpi_mul (n, p, q);

  /* Compute the Euler totient:  phi = (p-1)(q-1)  */
  pm1 = gcry_mpi_snew (nbits/2);
  qm1 = gcry_mpi_snew (nbits/2);
  phi = gcry_mpi_snew (nbits);
  mpi_sub_ui (pm1, p, 1);
  mpi_sub_ui (qm1, q, 1);
  mpi_mul (phi, pm1, qm1);

  g = gcry_mpi_snew (nbits);
  gcry_assert (gcry_mpi_gcd (g, e, phi));

  /* Compute: f = lcm(p-1,q-1) = phi / gcd(p-1,q-1) */
  gcry_mpi_gcd (g, pm1, qm1);
  f = pm1; pm1 = NULL;
  gcry_mpi_release (qm1); qm1 = NULL;
  mpi_fdiv_q (f, phi, g);
  gcry_mpi_release (phi); phi = NULL;
  d = g; g = NULL;
  /* Compute the secret key:  d = e^{-1} mod lcm(p-1,q-1) */
  mpi_invm (d, e, f);

  /* Compute the inverse of p and q.  */
  u = f; f = NULL;
  mpi_invm (u, p, q );

  if( DBG_CIPHER )
    {
      if (*swapped)
        log_debug ("p and q are swapped\n");
      log_mpidump("  p", p );
      log_mpidump("  q", q );
      log_mpidump("  n", n );
      log_mpidump("  e", e );
      log_mpidump("  d", d );
      log_mpidump("  u", u );
    }


  sk->n = n;
  sk->e = e;
  sk->p = p;
  sk->q = q;
  sk->d = d;
  sk->u = u;

  /* Now we can test our keys. */
  if (test_keys (sk, nbits - 64))
    {
      gcry_mpi_release (sk->n); sk->n = NULL;
      gcry_mpi_release (sk->e); sk->e = NULL;
      gcry_mpi_release (sk->p); sk->p = NULL;
      gcry_mpi_release (sk->q); sk->q = NULL;
      gcry_mpi_release (sk->d); sk->d = NULL;
      gcry_mpi_release (sk->u); sk->u = NULL;
      fips_signal_error ("self-test after key generation failed");
      return GPG_ERR_SELFTEST_FAILED;
    }

  return 0;
}


/****************
 * Test wether the secret key is valid.
 * Returns: true if this is a valid key.
 */
static int
check_secret_key( RSA_secret_key *sk )
{
  int rc;
  gcry_mpi_t temp = mpi_alloc( mpi_get_nlimbs(sk->p)*2 );
  
  mpi_mul(temp, sk->p, sk->q );
  rc = mpi_cmp( temp, sk->n );
  mpi_free(temp);
  return !rc;
}



/****************
 * Public key operation. Encrypt INPUT with PKEY and put result into OUTPUT.
 *
 *	c = m^e mod n
 *
 * Where c is OUTPUT, m is INPUT and e,n are elements of PKEY.
 */
static void
public(gcry_mpi_t output, gcry_mpi_t input, RSA_public_key *pkey )
{
  if( output == input )  /* powm doesn't like output and input the same */
    {
      gcry_mpi_t x = mpi_alloc( mpi_get_nlimbs(input)*2 );
      mpi_powm( x, input, pkey->e, pkey->n );
      mpi_set(output, x);
      mpi_free(x);
    }
  else
    mpi_powm( output, input, pkey->e, pkey->n );
}

#if 0
static void
stronger_key_check ( RSA_secret_key *skey )
{
  gcry_mpi_t t = mpi_alloc_secure ( 0 );
  gcry_mpi_t t1 = mpi_alloc_secure ( 0 );
  gcry_mpi_t t2 = mpi_alloc_secure ( 0 );
  gcry_mpi_t phi = mpi_alloc_secure ( 0 );

  /* check that n == p * q */
  mpi_mul( t, skey->p, skey->q);
  if (mpi_cmp( t, skey->n) )
    log_info ( "RSA Oops: n != p * q\n" );

  /* check that p is less than q */
  if( mpi_cmp( skey->p, skey->q ) > 0 )
    {
      log_info ("RSA Oops: p >= q - fixed\n");
      _gcry_mpi_swap ( skey->p, skey->q);
    }

    /* check that e divides neither p-1 nor q-1 */
    mpi_sub_ui(t, skey->p, 1 );
    mpi_fdiv_r(t, t, skey->e );
    if ( !mpi_cmp_ui( t, 0) )
        log_info ( "RSA Oops: e divides p-1\n" );
    mpi_sub_ui(t, skey->q, 1 );
    mpi_fdiv_r(t, t, skey->e );
    if ( !mpi_cmp_ui( t, 0) )
        log_info ( "RSA Oops: e divides q-1\n" );

    /* check that d is correct */
    mpi_sub_ui( t1, skey->p, 1 );
    mpi_sub_ui( t2, skey->q, 1 );
    mpi_mul( phi, t1, t2 );
    gcry_mpi_gcd(t, t1, t2);
    mpi_fdiv_q(t, phi, t);
    mpi_invm(t, skey->e, t );
    if ( mpi_cmp(t, skey->d ) )
      {
        log_info ( "RSA Oops: d is wrong - fixed\n");
        mpi_set (skey->d, t);
        _gcry_log_mpidump ("  fixed d", skey->d);
      }

    /* check for correctness of u */
    mpi_invm(t, skey->p, skey->q );
    if ( mpi_cmp(t, skey->u ) )
      {
        log_info ( "RSA Oops: u is wrong - fixed\n");
        mpi_set (skey->u, t);
        _gcry_log_mpidump ("  fixed u", skey->u);
      }

    log_info ( "RSA secret key check finished\n");

    mpi_free (t);
    mpi_free (t1);
    mpi_free (t2);
    mpi_free (phi);
}
#endif



/****************
 * Secret key operation. Encrypt INPUT with SKEY and put result into OUTPUT.
 *
 *	m = c^d mod n
 *
 * Or faster:
 *
 *      m1 = c ^ (d mod (p-1)) mod p 
 *      m2 = c ^ (d mod (q-1)) mod q 
 *      h = u * (m2 - m1) mod q 
 *      m = m1 + h * p
 *
 * Where m is OUTPUT, c is INPUT and d,n,p,q,u are elements of SKEY.
 */
static void
secret(gcry_mpi_t output, gcry_mpi_t input, RSA_secret_key *skey )
{
  if (!skey->p || !skey->q || !skey->u)
    {
      mpi_powm (output, input, skey->d, skey->n);
    }
  else
    {
      gcry_mpi_t m1 = mpi_alloc_secure( mpi_get_nlimbs(skey->n)+1 );
      gcry_mpi_t m2 = mpi_alloc_secure( mpi_get_nlimbs(skey->n)+1 );
      gcry_mpi_t h  = mpi_alloc_secure( mpi_get_nlimbs(skey->n)+1 );
      
      /* m1 = c ^ (d mod (p-1)) mod p */
      mpi_sub_ui( h, skey->p, 1  );
      mpi_fdiv_r( h, skey->d, h );   
      mpi_powm( m1, input, h, skey->p );
      /* m2 = c ^ (d mod (q-1)) mod q */
      mpi_sub_ui( h, skey->q, 1  );
      mpi_fdiv_r( h, skey->d, h );
      mpi_powm( m2, input, h, skey->q );
      /* h = u * ( m2 - m1 ) mod q */
      mpi_sub( h, m2, m1 );
      if ( mpi_is_neg( h ) ) 
        mpi_add ( h, h, skey->q );
      mpi_mulm( h, skey->u, h, skey->q ); 
      /* m = m2 + h * p */
      mpi_mul ( h, h, skey->p );
      mpi_add ( output, m1, h );
    
      mpi_free ( h );
      mpi_free ( m1 );
      mpi_free ( m2 );
    }
}



/* Perform RSA blinding.  */
static gcry_mpi_t
rsa_blind (gcry_mpi_t x, gcry_mpi_t r, gcry_mpi_t e, gcry_mpi_t n)
{
  /* A helper.  */
  gcry_mpi_t a;

  /* Result.  */
  gcry_mpi_t y;

  a = gcry_mpi_snew (gcry_mpi_get_nbits (n));
  y = gcry_mpi_snew (gcry_mpi_get_nbits (n));
  
  /* Now we calculate: y = (x * r^e) mod n, where r is the random
     number, e is the public exponent, x is the non-blinded data and n
     is the RSA modulus.  */
  gcry_mpi_powm (a, r, e, n);
  gcry_mpi_mulm (y, a, x, n);

  gcry_mpi_release (a);

  return y;
}

/* Undo RSA blinding.  */
static gcry_mpi_t
rsa_unblind (gcry_mpi_t x, gcry_mpi_t ri, gcry_mpi_t n)
{
  gcry_mpi_t y;

  y = gcry_mpi_snew (gcry_mpi_get_nbits (n));

  /* Here we calculate: y = (x * r^-1) mod n, where x is the blinded
     decrypted data, ri is the modular multiplicative inverse of r and
     n is the RSA modulus.  */

  gcry_mpi_mulm (y, ri, x, n);

  return y;
}

/*********************************************
 **************  interface  ******************
 *********************************************/

static gcry_err_code_t
rsa_generate_ext (int algo, unsigned int nbits, unsigned long evalue,
                  const gcry_sexp_t genparms,
                  gcry_mpi_t *skey, gcry_mpi_t **retfactors,
                  gcry_sexp_t *r_extrainfo)
{
  RSA_secret_key sk;
  gpg_err_code_t ec;
  gcry_sexp_t deriveparms;
  int transient_key = 0;
  int use_x931 = 0;
  gcry_sexp_t l1;

  (void)algo;
  
  *retfactors = NULL; /* We don't return them.  */

  deriveparms = (genparms?
                 gcry_sexp_find_token (genparms, "derive-parms", 0) : NULL);
  if (!deriveparms)
    {
      /* Parse the optional "use-x931" flag. */
      l1 = gcry_sexp_find_token (genparms, "use-x931", 0);
      if (l1)
        {
          use_x931 = 1;
          gcry_sexp_release (l1);
        }
    }

  if (deriveparms || use_x931 || fips_mode ())
    {
      int swapped;
      ec = generate_x931 (&sk, nbits, evalue, deriveparms, &swapped);
      gcry_sexp_release (deriveparms);
      if (!ec && r_extrainfo && swapped)
        {
          ec = gcry_sexp_new (r_extrainfo, 
                              "(misc-key-info(p-q-swapped))", 0, 1);
          if (ec)
            {
              gcry_mpi_release (sk.n); sk.n = NULL;
              gcry_mpi_release (sk.e); sk.e = NULL;
              gcry_mpi_release (sk.p); sk.p = NULL;
              gcry_mpi_release (sk.q); sk.q = NULL;
              gcry_mpi_release (sk.d); sk.d = NULL;
              gcry_mpi_release (sk.u); sk.u = NULL;
            }
        }
    }
  else
    {
      /* Parse the optional "transient-key" flag. */
      l1 = gcry_sexp_find_token (genparms, "transient-key", 0);
      if (l1)
        {
          transient_key = 1;
          gcry_sexp_release (l1);
        }
      /* Generate.  */
      ec = generate_std (&sk, nbits, evalue, transient_key);
    }

  if (!ec)
    {
      skey[0] = sk.n;
      skey[1] = sk.e;
      skey[2] = sk.d;
      skey[3] = sk.p;
      skey[4] = sk.q;
      skey[5] = sk.u;
    }
  
  return ec;
}


static gcry_err_code_t
rsa_generate (int algo, unsigned int nbits, unsigned long evalue,
              gcry_mpi_t *skey, gcry_mpi_t **retfactors)
{
  return rsa_generate_ext (algo, nbits, evalue, NULL, skey, retfactors, NULL);
}


static gcry_err_code_t
rsa_check_secret_key (int algo, gcry_mpi_t *skey)
{
  gcry_err_code_t err = GPG_ERR_NO_ERROR;
  RSA_secret_key sk;

  (void)algo;

  sk.n = skey[0];
  sk.e = skey[1];
  sk.d = skey[2];
  sk.p = skey[3];
  sk.q = skey[4];
  sk.u = skey[5];

  if (!sk.p || !sk.q || !sk.u)
    err = GPG_ERR_NO_OBJ;  /* To check the key we need the optional
                              parameters. */
  else if (!check_secret_key (&sk))
    err = GPG_ERR_PUBKEY_ALGO;

  return err;
}


static gcry_err_code_t
rsa_encrypt (int algo, gcry_mpi_t *resarr, gcry_mpi_t data,
             gcry_mpi_t *pkey, int flags)
{
  RSA_public_key pk;

  (void)algo;
  (void)flags;
  
  pk.n = pkey[0];
  pk.e = pkey[1];
  resarr[0] = mpi_alloc (mpi_get_nlimbs (pk.n));
  public (resarr[0], data, &pk);
  
  return GPG_ERR_NO_ERROR;
}


static gcry_err_code_t
rsa_decrypt (int algo, gcry_mpi_t *result, gcry_mpi_t *data,
             gcry_mpi_t *skey, int flags)
{
  RSA_secret_key sk;
  gcry_mpi_t r = MPI_NULL;	/* Random number needed for blinding.  */
  gcry_mpi_t ri = MPI_NULL;	/* Modular multiplicative inverse of
				   r.  */
  gcry_mpi_t x = MPI_NULL;	/* Data to decrypt.  */
  gcry_mpi_t y;			/* Result.  */

  (void)algo;

  /* Extract private key.  */
  sk.n = skey[0];
  sk.e = skey[1];
  sk.d = skey[2];
  sk.p = skey[3]; /* Optional. */
  sk.q = skey[4]; /* Optional. */
  sk.u = skey[5]; /* Optional. */

  y = gcry_mpi_snew (gcry_mpi_get_nbits (sk.n));

  /* We use blinding by default to mitigate timing attacks which can
     be practically mounted over the network as shown by Brumley and
     Boney in 2003.  */ 
  if (! (flags & PUBKEY_FLAG_NO_BLINDING))
    {
      /* Initialize blinding.  */
      
      /* First, we need a random number r between 0 and n - 1, which
	 is relatively prime to n (i.e. it is neither p nor q).  The
	 random number needs to be only unpredictable, thus we employ
	 the gcry_create_nonce function by using GCRY_WEAK_RANDOM with
	 gcry_mpi_randomize.  */
      r = gcry_mpi_snew (gcry_mpi_get_nbits (sk.n));
      ri = gcry_mpi_snew (gcry_mpi_get_nbits (sk.n));
      
      gcry_mpi_randomize (r, gcry_mpi_get_nbits (sk.n), GCRY_WEAK_RANDOM);
      gcry_mpi_mod (r, r, sk.n);

      /* Calculate inverse of r.  It practically impossible that the
         follwing test fails, thus we do not add code to release
         allocated resources.  */
      if (!gcry_mpi_invm (ri, r, sk.n))
	return GPG_ERR_INTERNAL;
    }

  if (! (flags & PUBKEY_FLAG_NO_BLINDING))
    x = rsa_blind (data[0], r, sk.e, sk.n);
  else
    x = data[0];

  /* Do the encryption.  */
  secret (y, x, &sk);

  if (! (flags & PUBKEY_FLAG_NO_BLINDING))
    {
      /* Undo blinding.  */
      gcry_mpi_t a = gcry_mpi_copy (y);
      
      gcry_mpi_release (y);
      y = rsa_unblind (a, ri, sk.n);

      gcry_mpi_release (a);
    }

  if (! (flags & PUBKEY_FLAG_NO_BLINDING))
    {
      /* Deallocate resources needed for blinding.  */
      gcry_mpi_release (x);
      gcry_mpi_release (r);
      gcry_mpi_release (ri);
    }

  /* Copy out result.  */
  *result = y;
  
  return GPG_ERR_NO_ERROR;
}


static gcry_err_code_t
rsa_sign (int algo, gcry_mpi_t *resarr, gcry_mpi_t data, gcry_mpi_t *skey)
{
  RSA_secret_key sk;

  (void)algo;
  
  sk.n = skey[0];
  sk.e = skey[1];
  sk.d = skey[2];
  sk.p = skey[3];
  sk.q = skey[4];
  sk.u = skey[5];
  resarr[0] = mpi_alloc( mpi_get_nlimbs (sk.n));
  secret (resarr[0], data, &sk);

  return GPG_ERR_NO_ERROR;
}


static gcry_err_code_t
rsa_verify (int algo, gcry_mpi_t hash, gcry_mpi_t *data, gcry_mpi_t *pkey,
		  int (*cmp) (void *opaque, gcry_mpi_t tmp),
		  void *opaquev)
{
  RSA_public_key pk;
  gcry_mpi_t result;
  gcry_err_code_t rc;

  (void)algo;
  (void)cmp;
  (void)opaquev;

  pk.n = pkey[0];
  pk.e = pkey[1];
  result = gcry_mpi_new ( 160 );
  public( result, data[0], &pk );
#ifdef IS_DEVELOPMENT_VERSION
  if (DBG_CIPHER)
    {
      log_mpidump ("rsa verify result:", result );
      log_mpidump ("             hash:", hash );
    }
#endif /*IS_DEVELOPMENT_VERSION*/
  /*rc = (*cmp)( opaquev, result );*/
  rc = mpi_cmp (result, hash) ? GPG_ERR_BAD_SIGNATURE : GPG_ERR_NO_ERROR;
  gcry_mpi_release (result);
  
  return rc;
}


static unsigned int
rsa_get_nbits (int algo, gcry_mpi_t *pkey)
{
  (void)algo;

  return mpi_get_nbits (pkey[0]);
}


/* Compute a keygrip.  MD is the hash context which we are going to
   update.  KEYPARAM is an S-expression with the key parameters, this
   is usually a public key but may also be a secret key.  An example
   of such an S-expression is:

      (rsa
        (n #00B...#)
        (e #010001#))
        
   PKCS-15 says that for RSA only the modulus should be hashed -
   however, it is not clear wether this is meant to use the raw bytes
   (assuming this is an unsigned integer) or whether the DER required
   0 should be prefixed.  We hash the raw bytes.  */
static gpg_err_code_t
compute_keygrip (gcry_md_hd_t md, gcry_sexp_t keyparam)
{
  gcry_sexp_t l1;
  const char *data;
  size_t datalen;

  l1 = gcry_sexp_find_token (keyparam, "n", 1);
  if (!l1)
    return GPG_ERR_NO_OBJ;

  data = gcry_sexp_nth_data (l1, 1, &datalen);
  if (!data)
    {
      gcry_sexp_release (l1);
      return GPG_ERR_NO_OBJ;
    }

  gcry_md_write (md, data, datalen);
  gcry_sexp_release (l1);

  return 0;
}




/* 
     Self-test section.
 */

static const char *
selftest_sign_1024 (gcry_sexp_t pkey, gcry_sexp_t skey)
{
  static const char sample_data[] = 
    "(data (flags pkcs1)"
    " (hash sha1 #11223344556677889900aabbccddeeff10203040#))";
  static const char sample_data_bad[] = 
    "(data (flags pkcs1)"
    " (hash sha1 #11223344556677889900aabbccddeeff80203040#))";

  const char *errtxt = NULL;
  gcry_error_t err;
  gcry_sexp_t data = NULL;
  gcry_sexp_t data_bad = NULL;
  gcry_sexp_t sig = NULL;

  err = gcry_sexp_sscan (&data, NULL,
                         sample_data, strlen (sample_data));
  if (!err)
    err = gcry_sexp_sscan (&data_bad, NULL, 
                           sample_data_bad, strlen (sample_data_bad));
  if (err)
    {
      errtxt = "converting data failed";
      goto leave;
    }

  err = gcry_pk_sign (&sig, data, skey);
  if (err)
    {
      errtxt = "signing failed";
      goto leave;
    }
  err = gcry_pk_verify (sig, data, pkey);
  if (err)
    {
      errtxt = "verify failed";
      goto leave;
    }
  err = gcry_pk_verify (sig, data_bad, pkey);
  if (gcry_err_code (err) != GPG_ERR_BAD_SIGNATURE)
    {
      errtxt = "bad signature not detected";
      goto leave;
    }


 leave:
  gcry_sexp_release (sig);
  gcry_sexp_release (data_bad);
  gcry_sexp_release (data);
  return errtxt;
}



/* Given an S-expression ENCR_DATA of the form:

   (enc-val
    (rsa
     (a a-value)))

   as returned by gcry_pk_decrypt, return the the A-VALUE.  On error,
   return NULL.  */
static gcry_mpi_t
extract_a_from_sexp (gcry_sexp_t encr_data)
{
  gcry_sexp_t l1, l2, l3;
  gcry_mpi_t a_value;

  l1 = gcry_sexp_find_token (encr_data, "enc-val", 0);
  if (!l1)
    return NULL;
  l2 = gcry_sexp_find_token (l1, "rsa", 0);
  gcry_sexp_release (l1);
  if (!l2)
    return NULL;
  l3 = gcry_sexp_find_token (l2, "a", 0);
  gcry_sexp_release (l2);
  if (!l3)
    return NULL;
  a_value = gcry_sexp_nth_mpi (l3, 1, 0);
  gcry_sexp_release (l3);

  return a_value;
}


static const char *
selftest_encr_1024 (gcry_sexp_t pkey, gcry_sexp_t skey)
{
  const char *errtxt = NULL;
  gcry_error_t err;
  const unsigned int nbits = 1000; /* Encrypt 1000 random bits.  */
  gcry_mpi_t plaintext = NULL;
  gcry_sexp_t plain = NULL;
  gcry_sexp_t encr  = NULL;
  gcry_mpi_t  ciphertext = NULL;
  gcry_sexp_t decr  = NULL;
  gcry_mpi_t  decr_plaintext = NULL;
  gcry_sexp_t tmplist = NULL;

  /* Create plaintext.  The plaintext is actually a big integer number.  */
  plaintext = gcry_mpi_new (nbits);
  gcry_mpi_randomize (plaintext, nbits, GCRY_WEAK_RANDOM);
  
  /* Put the plaintext into an S-expression.  */
  err = gcry_sexp_build (&plain, NULL,
                         "(data (flags raw) (value %m))", plaintext);
  if (err)
    {
      errtxt = "converting data failed";
      goto leave;
    }

  /* Encrypt.  */
  err = gcry_pk_encrypt (&encr, plain, pkey);
  if (err)
    {
      errtxt = "encrypt failed";
      goto leave;
    }

  /* Extraxt the ciphertext from the returned S-expression.  */
  /*gcry_sexp_dump (encr);*/
  ciphertext = extract_a_from_sexp (encr);
  if (!ciphertext)
    {
      errtxt = "gcry_pk_decrypt returned garbage";
      goto leave;
    }

  /* Check that the ciphertext does no match the plaintext.  */
  /* _gcry_log_mpidump ("plaintext", plaintext); */
  /* _gcry_log_mpidump ("ciphertxt", ciphertext); */
  if (!gcry_mpi_cmp (plaintext, ciphertext))
    {
      errtxt = "ciphertext matches plaintext";
      goto leave;
    }

  /* Decrypt.  */
  err = gcry_pk_decrypt (&decr, encr, skey);
  if (err)
    {
      errtxt = "decrypt failed";
      goto leave;
    }

  /* Extract the decrypted data from the S-expression.  Note that the
     output of gcry_pk_decrypt depends on whether a flags lists occurs
     in its input data.  Because we passed the output of
     gcry_pk_encrypt directly to gcry_pk_decrypt, such a flag value
     won't be there as of today.  To be prepared for future changes we
     take care of it anyway.  */
  tmplist = gcry_sexp_find_token (decr, "value", 0);
  if (tmplist)
    decr_plaintext = gcry_sexp_nth_mpi (tmplist, 1, GCRYMPI_FMT_USG);
  else
    decr_plaintext = gcry_sexp_nth_mpi (decr, 0, GCRYMPI_FMT_USG);
  if (!decr_plaintext)
    {
      errtxt = "decrypt returned no plaintext";
      goto leave;
    }
  
  /* Check that the decrypted plaintext matches the original  plaintext.  */
  if (gcry_mpi_cmp (plaintext, decr_plaintext))
    {
      errtxt = "mismatch";
      goto leave;
    }

 leave:
  gcry_sexp_release (tmplist);
  gcry_mpi_release (decr_plaintext);
  gcry_sexp_release (decr);
  gcry_mpi_release (ciphertext);
  gcry_sexp_release (encr);
  gcry_sexp_release (plain);
  gcry_mpi_release (plaintext);
  return errtxt;
}


static gpg_err_code_t
selftests_rsa (selftest_report_func_t report)
{
  const char *what;
  const char *errtxt;
  gcry_error_t err;
  gcry_sexp_t skey = NULL;
  gcry_sexp_t pkey = NULL;
  
  /* Convert the S-expressions into the internal representation.  */
  what = "convert";
  err = gcry_sexp_sscan (&skey, NULL, 
                         sample_secret_key, strlen (sample_secret_key));
  if (!err)
    err = gcry_sexp_sscan (&pkey, NULL, 
                           sample_public_key, strlen (sample_public_key));
  if (err)
    {
      errtxt = gcry_strerror (err);
      goto failed;
    }

  what = "key consistency";
  err = gcry_pk_testkey (skey);
  if (err)
    {
      errtxt = gcry_strerror (err);
      goto failed;
    }

  what = "sign";
  errtxt = selftest_sign_1024 (pkey, skey);
  if (errtxt)
    goto failed;

  what = "encrypt";
  errtxt = selftest_encr_1024 (pkey, skey);
  if (errtxt)
    goto failed;

  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);
  return 0; /* Succeeded. */

 failed:
  gcry_sexp_release (pkey);
  gcry_sexp_release (skey);
  if (report)
    report ("pubkey", GCRY_PK_RSA, what, errtxt);
  return GPG_ERR_SELFTEST_FAILED;
}


/* Run a full self-test for ALGO and return 0 on success.  */
static gpg_err_code_t
run_selftests (int algo, int extended, selftest_report_func_t report)
{
  gpg_err_code_t ec;

  (void)extended;

  switch (algo)
    {
    case GCRY_PK_RSA:
      ec = selftests_rsa (report);
      break;
    default:
      ec = GPG_ERR_PUBKEY_ALGO;
      break;
        
    }
  return ec;
}




static const char *rsa_names[] =
  {
    "rsa",
    "openpgp-rsa",
    "oid.1.2.840.113549.1.1.1",
    NULL,
  };

gcry_pk_spec_t _gcry_pubkey_spec_rsa =
  {
    "RSA", rsa_names,
    "ne", "nedpqu", "a", "s", "n",
    GCRY_PK_USAGE_SIGN | GCRY_PK_USAGE_ENCR,
    rsa_generate,
    rsa_check_secret_key,
    rsa_encrypt,
    rsa_decrypt,
    rsa_sign,
    rsa_verify,
    rsa_get_nbits,
  };
pk_extra_spec_t _gcry_pubkey_extraspec_rsa = 
  {
    run_selftests,
    rsa_generate_ext,
    compute_keygrip
  };

