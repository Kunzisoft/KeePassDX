/* hwfeatures.c - Detect hardware features.
 * Copyright (C) 2007  Free Software Foundation, Inc.
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

#include <config.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>

#include "g10lib.h"

/* A bit vector describing the hardware features currently
   available. */
static unsigned int hw_features;


/* Return a bit vector describing the available hardware features.
   The HWF_ constants are used to test for them. */
unsigned int
_gcry_get_hw_features (void)
{
  return hw_features;
}


#if defined (__i386__) && SIZEOF_UNSIGNED_LONG == 4 && defined (__GNUC__)
static void
detect_ia32_gnuc (void)
{
#ifdef ENABLE_PADLOCK_SUPPORT  
  /* The code here is only useful for the PadLock engine thus we don't
     build it if that support has been disabled.  */
  int has_cpuid = 0;
  char vendor_id[12+1];
  
  /* Detect the CPUID feature by testing some undefined behaviour (16
     vs 32 bit pushf/popf). */
  asm volatile
    ("pushf\n\t"                 /* Copy flags to EAX.  */
     "popl %%eax\n\t"
     "movl %%eax, %%ecx\n\t"     /* Save flags into ECX.  */
     "xorl $0x200000, %%eax\n\t" /* Toggle ID bit and copy it to the flags.  */
     "pushl %%eax\n\t"            
     "popf\n\t"                
     "pushf\n\t"                 /* Copy changed flags again to EAX.  */    
     "popl %%eax\n\t"
     "pushl %%ecx\n\t"           /* Restore flags from ECX.  */
     "popf\n\t"
     "xorl %%eax, %%ecx\n\t"     /* Compare flags against saved flags.  */
     "jz .Lno_cpuid%=\n\t"       /* Toggling did not work, thus no CPUID.  */
     "movl $1, %0\n"             /* Worked. true -> HAS_CPUID.  */
     ".Lno_cpuid%=:\n\t"
     : "=r" (has_cpuid)
     :
     : "%eax", "%ecx", "cc"
     );
  
  if (!has_cpuid)
    return;  /* No way.  */
           
  asm volatile
    ("pushl %%ebx\n\t"           /* Save GOT register.  */
     "xorl  %%eax, %%eax\n\t"    /* 0 -> EAX.  */
     "cpuid\n\t"                 /* Get vendor ID.  */
     "movl  %%ebx, (%0)\n\t"     /* EBX,EDX,ECX -> VENDOR_ID.  */
     "movl  %%edx, 4(%0)\n\t"
     "movl  %%ecx, 8(%0)\n\t"
     "popl  %%ebx\n"
     :
     : "S" (&vendor_id[0])
     : "%eax", "%ecx", "%edx", "cc"
     );
  vendor_id[12] = 0;

  /* Check whether this is a VIA CPU and what PadLock features we
     have.  */
  if (!strcmp (vendor_id, "CentaurHauls"))
    {
      asm volatile 
        ("pushl %%ebx\n\t"	        /* Save GOT register.  */
         "movl $0xC0000000, %%eax\n\t"  /* Check for extended centaur  */
         "cpuid\n\t"                    /* feature flags.              */
         "popl %%ebx\n\t"	        /* Restore GOT register. */
         "cmpl $0xC0000001, %%eax\n\t"
         "jb .Lready%=\n\t"             /* EAX < 0xC0000000 => no padlock.  */

         "pushl %%ebx\n\t"	        /* Save GOT register. */
         "movl $0xC0000001, %%eax\n\t"  /* Ask for the extended */
         "cpuid\n\t"                    /* feature flags.       */
         "popl %%ebx\n\t"	        /* Restore GOT register. */

         "movl %%edx, %%eax\n\t"        /* Take copy of feature flags.  */
         "andl $0x0C, %%eax\n\t"        /* Test bits 2 and 3 to see whether */
         "cmpl $0x0C, %%eax\n\t"        /* the RNG exists and is enabled.   */
         "jnz .Lno_rng%=\n\t"
         "orl $1, %0\n"                 /* Set our HWF_PADLOCK_RNG bit.  */

         ".Lno_rng%=:\n\t"
         "movl %%edx, %%eax\n\t"        /* Take copy of feature flags.  */
         "andl $0xC0, %%eax\n\t"        /* Test bits 6 and 7 to see whether */
         "cmpl $0xC0, %%eax\n\t"        /* the ACE exists and is enabled.   */
         "jnz .Lno_ace%=\n\t"
         "orl $2, %0\n"                 /* Set our HWF_PADLOCK_AES bit.  */

         ".Lno_ace%=:\n\t"
         "movl %%edx, %%eax\n\t"        /* Take copy of feature flags.  */
         "andl $0xC00, %%eax\n\t"       /* Test bits 10, 11 to see whether  */
         "cmpl $0xC00, %%eax\n\t"       /* the PHE exists and is enabled.   */
         "jnz .Lno_phe%=\n\t"
         "orl $4, %0\n"                 /* Set our HWF_PADLOCK_SHA bit.  */

         ".Lno_phe%=:\n\t"
         "movl %%edx, %%eax\n\t"        /* Take copy of feature flags.  */
         "andl $0x3000, %%eax\n\t"      /* Test bits 12, 13 to see whether  */
         "cmpl $0x3000, %%eax\n\t"      /* MONTMUL exists and is enabled.   */
         "jnz .Lready%=\n\t"
         "orl $8, %0\n"                 /* Set our HWF_PADLOCK_MMUL bit.  */

         ".Lready%=:\n"
         : "+r" (hw_features)
         :
         : "%eax", "%edx", "cc"
         );
    }
#endif /*ENABLE_PADLOCK_SUPPORT*/
}
#endif /* __i386__ && SIZEOF_UNSIGNED_LONG == 4 && __GNUC__ */



/* Detect the available hardware features.  This function is called
   once right at startup and we assume that no other threads are
   running.  */
void
_gcry_detect_hw_features (void)
{
  hw_features = 0;

  if (fips_mode ())
    return; /* Hardware support is not to be evaluated.  */ 

#if defined (__i386__) && SIZEOF_UNSIGNED_LONG == 4
#ifdef __GNUC__  
  detect_ia32_gnuc ();
#endif
#elif defined (__i386__) && SIZEOF_UNSIGNED_LONG == 8
#ifdef __GNUC__  
#endif
#endif
}
