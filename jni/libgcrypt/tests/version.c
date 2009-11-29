/* version.c  -  This version test should be run first.
   Copyright (C) 2007 Free Software Foundation, Inc.

   This file is part of Libgcrypt.
  
   Libgcrypt is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 2.1 of
   the License, or (at your option) any later version.
  
   Libgcrypt is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.
  
   You should have received a copy of the GNU Lesser General Public
   License along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
   USA.  */

/* This test should be run first because due to a failing config.links
   script or bad configure parameters the just build libgcrypt may
   crash in case MPI function for specific CPU revisions have been
   enabled.  Running this test first will print out information so to
   make it easier to figure out the problem. */
   
  
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include "../src/gcrypt.h"

#define PGM "version"


int
main (int argc, char **argv)
{
  (void)argc;
  (void)argv;

  gcry_control (GCRYCTL_DISABLE_SECMEM, 0);
  if (!gcry_check_version (GCRYPT_VERSION))
    {
      fprintf (stderr, PGM ": version mismatch\n");
      exit (1);
    }

  gcry_control (GCRYCTL_PRINT_CONFIG, NULL);

  return 0;
}

