/* testapi.c - for libgcrypt
 *	Copyright (C) 2000, 2002 Free Software Foundation, Inc.
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
 */

#include <stdio.h>
#include <stdlib.h>
#include <gcrypt.h>


#define BUG() do {fprintf ( stderr, "Ooops at %s:%d\n", __FILE__ , __LINE__ );\
		  exit(2);} while(0)

/* an ElGamal public key */
struct {
    const char *p,*g,*y;
} elg_testkey1 = {
  "0x9D559F31A6D30492C383213844AEBB7772963A85D3239F3611AAB93A2A985F64FB735B9259EC326BF5720F909980D609D37C288C9223B0350FBE493C3B5AF54CA23031E952E92F8A3DBEDBC5A684993D452CD54F85B85160166FCD25BD7AB6AE9B1EB4FCC9D300DAFF081C4CBA6694906D3E3FF18196A5CCF7F0A6182962166B",
  "0x5",
  "0x9640024BB2A277205813FF685048AA27E2B192B667163E7C59E381E27003D044C700C531CE8FD4AA781B463BC9FFE74956AF09A38A098322B1CF72FC896F009E3A6BFF053D3B1D1E1994BF9CC07FA12963D782F027B51511DDE8C5F43421FBC12734A9C070F158C729A370BEE5FC51A772219438EDA8202C35FA3F5D8CD1997B"
};

void
test_sexp ( int argc, char **argv )
{
    int rc, nbits;
    gcry_sexp_t sexp;
    gcry_mpi_t key[3];
    size_t n;
    char *buf;

    if ( gcry_mpi_scan( &key[0], GCRYMPI_FMT_HEX, elg_testkey1.p, NULL ) )
	BUG();
    if ( gcry_mpi_scan( &key[1], GCRYMPI_FMT_HEX, elg_testkey1.g, NULL ) )
	BUG();
    if ( gcry_mpi_scan( &key[2], GCRYMPI_FMT_HEX, elg_testkey1.y, NULL ) )
	BUG();

    /* get nbits from a key */
    rc = gcry_sexp_build ( &sexp, NULL,
			   "(public-key(elg(p%m)(g%m)(y%m)))",
				  key[0], key[1], key[2] );
    fputs ( "DUMP of PK:\n", stderr );
    gcry_sexp_dump ( sexp );
    {  gcry_sexp_t x;
       x = gcry_sexp_cdr ( sexp );
       fputs ( "DUMP of CDR:\n", stderr );
       gcry_sexp_dump ( x );
       gcry_sexp_release ( x );
    }
    nbits = gcry_pk_get_nbits( sexp );
    printf ( "elg_testkey1 - nbits=%d\n", nbits );
    n = gcry_sexp_sprint ( sexp, 0, NULL, 0 );
    buf = gcry_xmalloc ( n );
    n = gcry_sexp_sprint ( sexp, 0, buf, n );
    printf ( "sprint length=%u\n", (unsigned int)n );
    gcry_free ( buf );
    gcry_sexp_release( sexp );
}


void
test_genkey ( int argc, char **argv )
{
    int rc, nbits = 1024;
    gcry_sexp_t s_parms, s_key;

    gcry_control( GCRYCTL_INIT_SECMEM, 16384, 0 );
    rc = gcry_sexp_build ( &s_parms, NULL, "(genkey(dsa(nbits %d)))", nbits );
    rc = gcry_pk_genkey( &s_key, s_parms );
    if ( rc ) {
	fprintf ( stderr, "genkey failed: %s\n", gpg_strerror (rc) );
	return;
    }
    gcry_sexp_release( s_parms );
    gcry_sexp_dump ( s_key );
    gcry_sexp_release( s_key );
}

int
main( int argc, char **argv )
{
    if ( argc < 2 )
	printf("%s\n", gcry_check_version ( NULL ) );
    else if ( !strcmp ( argv[1], "version") )
	printf("%s\n", gcry_check_version ( argc > 2 ? argv[2] : NULL ) );
    else if ( !strcmp ( argv[1], "sexp" ) )
	test_sexp ( argc-2, argv+2 );
    else if ( !strcmp ( argv[1], "genkey" ) )
	test_genkey ( argc-2, argv+2 );
    else {
	fprintf (stderr, "usage: testapi mode-string [mode-args]\n");
	return 1;
    }

    return 0;
}
