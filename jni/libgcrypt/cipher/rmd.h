/* rmd.h - RIPE-MD hash functions
 *	Copyright (C) 1998, 2001, 2002 Free Software Foundation, Inc.
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
#ifndef G10_RMD_H
#define G10_RMD_H


/* We need this here because random.c must have direct access. */
typedef struct 
{
  u32  h0,h1,h2,h3,h4;
  u32  nblocks;
  byte buf[64];
  int  count;
} RMD160_CONTEXT;

void _gcry_rmd160_init ( void *context );
void _gcry_rmd160_mixblock ( RMD160_CONTEXT *hd, void *blockof64byte );

#endif /*G10_RMD_H*/

