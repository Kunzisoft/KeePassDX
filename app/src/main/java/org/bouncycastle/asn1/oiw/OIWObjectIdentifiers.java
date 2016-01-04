package org.bouncycastle.asn1.oiw;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface OIWObjectIdentifiers
{
    // id-SHA1 OBJECT IDENTIFIER ::=    
    //   {iso(1) identified-organization(3) oiw(14) secsig(3) algorithms(2) 26 }    //
    static final DERObjectIdentifier    md4WithRSA              = new DERObjectIdentifier("1.3.14.3.2.2");
    static final DERObjectIdentifier    md5WithRSA              = new DERObjectIdentifier("1.3.14.3.2.3");
    static final DERObjectIdentifier    md4WithRSAEncryption    = new DERObjectIdentifier("1.3.14.3.2.4");
    
    static final DERObjectIdentifier    desECB                  = new DERObjectIdentifier("1.3.14.3.2.6");
    static final DERObjectIdentifier    desCBC                  = new DERObjectIdentifier("1.3.14.3.2.7");
    static final DERObjectIdentifier    desOFB                  = new DERObjectIdentifier("1.3.14.3.2.8");
    static final DERObjectIdentifier    desCFB                  = new DERObjectIdentifier("1.3.14.3.2.9");

    static final DERObjectIdentifier    desEDE                  = new DERObjectIdentifier("1.3.14.3.2.17");
    
    static final DERObjectIdentifier    idSHA1                  = new DERObjectIdentifier("1.3.14.3.2.26");

    static final DERObjectIdentifier    dsaWithSHA1             = new DERObjectIdentifier("1.3.14.3.2.27");

    static final DERObjectIdentifier    sha1WithRSA             = new DERObjectIdentifier("1.3.14.3.2.29");
    
    // ElGamal Algorithm OBJECT IDENTIFIER ::=    
    // {iso(1) identified-organization(3) oiw(14) dirservsig(7) algorithm(2) encryption(1) 1 }
    //
    static final DERObjectIdentifier    elGamalAlgorithm        = new DERObjectIdentifier("1.3.14.7.2.1.1");

}
