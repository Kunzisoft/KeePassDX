/*
---------------------------------------------------------------------------
Copyright (c) 1998-2010, Brian Gladman, Worcester, UK. All rights reserved.

The redistribution and use of this software (with or without changes)
is allowed without the payment of fees or royalties provided that:

  source code distributions include the above copyright notice, this
  list of conditions and the following disclaimer;

  binary distributions include the above copyright notice, this list
  of conditions and the following disclaimer in their documentation.

This software is provided 'as is' with no explicit or implied warranties
in respect of its operation, including, but not limited to, correctness
and fitness for purpose.
---------------------------------------------------------------------------
Issue Date: 20/12/2007

This is an implementation of RFC2898, which specifies key derivation from
a password and a salt value.
*/

#ifndef PWD2KEY_H
#define PWD2KEY_H

#if defined(__cplusplus)
extern "C"
{
#endif

void derive_key(
        const unsigned char pwd[],   /* the PASSWORD, and   */
        unsigned int pwd_len,        /*    its length       */ 
        const unsigned char salt[],  /* the SALT and its    */
        unsigned int salt_len,       /*    length           */
        unsigned int iter,      /* the number of iterations */
        unsigned char key[],    /* space for the output key */
        unsigned int key_len);  /* and its required length  */

#if defined(__cplusplus)
}
#endif

#endif
