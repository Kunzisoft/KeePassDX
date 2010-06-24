package org.bouncycastle.asn1.nist;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface NISTObjectIdentifiers
{
    //
    // NIST
    //     iso/itu(2) joint-assign(16) us(840) organization(1) gov(101) csor(3) 

    //
    // nistalgorithms(4)
    //
    static final String                 nistAlgorithm          = "2.16.840.1.101.3.4";

    static final DERObjectIdentifier    id_sha256               = new DERObjectIdentifier(nistAlgorithm + ".2.1");
    static final DERObjectIdentifier    id_sha384               = new DERObjectIdentifier(nistAlgorithm + ".2.2");
    static final DERObjectIdentifier    id_sha512               = new DERObjectIdentifier(nistAlgorithm + ".2.3");
    static final DERObjectIdentifier    id_sha224               = new DERObjectIdentifier(nistAlgorithm + ".2.4");
    
    static final String                 aes                     = nistAlgorithm + ".1";
    
    static final DERObjectIdentifier    id_aes128_ECB           = new DERObjectIdentifier(aes + ".1"); 
    static final DERObjectIdentifier    id_aes128_CBC           = new DERObjectIdentifier(aes + ".2");
    static final DERObjectIdentifier    id_aes128_OFB           = new DERObjectIdentifier(aes + ".3"); 
    static final DERObjectIdentifier    id_aes128_CFB           = new DERObjectIdentifier(aes + ".4"); 
    static final DERObjectIdentifier    id_aes128_wrap          = new DERObjectIdentifier(aes + ".5");
    static final DERObjectIdentifier    id_aes128_GCM           = new DERObjectIdentifier(aes + ".6");
    static final DERObjectIdentifier    id_aes128_CCM           = new DERObjectIdentifier(aes + ".7");
    
    static final DERObjectIdentifier    id_aes192_ECB           = new DERObjectIdentifier(aes + ".21"); 
    static final DERObjectIdentifier    id_aes192_CBC           = new DERObjectIdentifier(aes + ".22"); 
    static final DERObjectIdentifier    id_aes192_OFB           = new DERObjectIdentifier(aes + ".23"); 
    static final DERObjectIdentifier    id_aes192_CFB           = new DERObjectIdentifier(aes + ".24"); 
    static final DERObjectIdentifier    id_aes192_wrap          = new DERObjectIdentifier(aes + ".25");
    static final DERObjectIdentifier    id_aes192_GCM           = new DERObjectIdentifier(aes + ".26");
    static final DERObjectIdentifier    id_aes192_CCM           = new DERObjectIdentifier(aes + ".27");
    
    static final DERObjectIdentifier    id_aes256_ECB           = new DERObjectIdentifier(aes + ".41"); 
    static final DERObjectIdentifier    id_aes256_CBC           = new DERObjectIdentifier(aes + ".42");
    static final DERObjectIdentifier    id_aes256_OFB           = new DERObjectIdentifier(aes + ".43"); 
    static final DERObjectIdentifier    id_aes256_CFB           = new DERObjectIdentifier(aes + ".44"); 
    static final DERObjectIdentifier    id_aes256_wrap          = new DERObjectIdentifier(aes + ".45"); 
    static final DERObjectIdentifier    id_aes256_GCM           = new DERObjectIdentifier(aes + ".46");
    static final DERObjectIdentifier    id_aes256_CCM           = new DERObjectIdentifier(aes + ".47");

    //
    // signatures
    //
    static final DERObjectIdentifier    id_dsa_with_sha2        = new DERObjectIdentifier(nistAlgorithm + ".3"); 

    static final DERObjectIdentifier    dsa_with_sha224         = new DERObjectIdentifier(id_dsa_with_sha2 + ".1"); 
    static final DERObjectIdentifier    dsa_with_sha256         = new DERObjectIdentifier(id_dsa_with_sha2 + ".2");
    static final DERObjectIdentifier    dsa_with_sha384         = new DERObjectIdentifier(id_dsa_with_sha2 + ".3");
    static final DERObjectIdentifier    dsa_with_sha512         = new DERObjectIdentifier(id_dsa_with_sha2 + ".4"); 
}
