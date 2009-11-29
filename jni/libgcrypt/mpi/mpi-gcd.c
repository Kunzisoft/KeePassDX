/* mpi-gcd.c  -  MPI functions
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
#include "mpi-internal.h"

/****************
 * Find the greatest common divisor G of A and B.
 * Return: true if this 1, false in all other cases
 */
int
gcry_mpi_gcd( gcry_mpi_t g, gcry_mpi_t xa, gcry_mpi_t xb )
{
    gcry_mpi_t a, b;

    a = mpi_copy(xa);
    b = mpi_copy(xb);

    /* TAOCP Vol II, 4.5.2, Algorithm A */
    a->sign = 0;
    b->sign = 0;
    while( gcry_mpi_cmp_ui( b, 0 ) ) {
	_gcry_mpi_fdiv_r( g, a, b ); /* g used as temorary variable */
	mpi_set(a,b);
	mpi_set(b,g);
    }
    mpi_set(g, a);

    mpi_free(a);
    mpi_free(b);
    return !gcry_mpi_cmp_ui( g, 1);
}



