package org.bouncycastle.asn1.iana;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface IANAObjectIdentifiers
{
    // id-SHA1 OBJECT IDENTIFIER ::=    
    // {iso(1) identified-organization(3) dod(6) internet(1) security(5) mechanisms(5) ipsec(8) isakmpOakley(1)}
    //

    static final DERObjectIdentifier    isakmpOakley  = new DERObjectIdentifier("1.3.6.1.5.5.8.1");

    static final DERObjectIdentifier    hmacMD5       = new DERObjectIdentifier(isakmpOakley + ".1");
    static final DERObjectIdentifier    hmacSHA1     = new DERObjectIdentifier(isakmpOakley + ".2");
    
    static final DERObjectIdentifier    hmacTIGER     = new DERObjectIdentifier(isakmpOakley + ".3");
    
    static final DERObjectIdentifier    hmacRIPEMD160 = new DERObjectIdentifier(isakmpOakley + ".4");

}
