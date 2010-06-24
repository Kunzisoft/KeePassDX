package org.bouncycastle.asn1.x9;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface X9ObjectIdentifiers
{
    //
    // X9.62
    //
    // ansi-X9-62 OBJECT IDENTIFIER ::= { iso(1) member-body(2)
    //            us(840) ansi-x962(10045) }
    //
    static final String    ansi_X9_62 = "1.2.840.10045";
    static final String    id_fieldType = ansi_X9_62 + ".1";

    static final DERObjectIdentifier    prime_field
                    = new DERObjectIdentifier(id_fieldType + ".1");

    static final DERObjectIdentifier    characteristic_two_field
                    = new DERObjectIdentifier(id_fieldType + ".2");

    static final DERObjectIdentifier    gnBasis
                    = new DERObjectIdentifier(id_fieldType + ".2.3.1");

    static final DERObjectIdentifier    tpBasis
                    = new DERObjectIdentifier(id_fieldType + ".2.3.2");

    static final DERObjectIdentifier    ppBasis
                    = new DERObjectIdentifier(id_fieldType + ".2.3.3");

    static final String    id_ecSigType = ansi_X9_62 + ".4";

    static final DERObjectIdentifier    ecdsa_with_SHA1
                    = new DERObjectIdentifier(id_ecSigType + ".1");

    static final String    id_publicKeyType = ansi_X9_62 + ".2";

    static final DERObjectIdentifier    id_ecPublicKey
                    = new DERObjectIdentifier(id_publicKeyType + ".1");

    static final DERObjectIdentifier    ecdsa_with_SHA2
                    = new DERObjectIdentifier(id_ecSigType + ".3");

    static final DERObjectIdentifier    ecdsa_with_SHA224
                    = new DERObjectIdentifier(ecdsa_with_SHA2 + ".1");

    static final DERObjectIdentifier    ecdsa_with_SHA256
                    = new DERObjectIdentifier(ecdsa_with_SHA2 + ".2");

    static final DERObjectIdentifier    ecdsa_with_SHA384
                    = new DERObjectIdentifier(ecdsa_with_SHA2 + ".3");

    static final DERObjectIdentifier    ecdsa_with_SHA512
                    = new DERObjectIdentifier(ecdsa_with_SHA2 + ".4");

    //
    // named curves
    //
    static final String     ellipticCurve = ansi_X9_62 + ".3";

    //
    // Two Curves
    //
    static final String     cTwoCurve = ellipticCurve + ".0";
    
    static final DERObjectIdentifier    c2pnb163v1 = new DERObjectIdentifier(cTwoCurve + ".1");
    static final DERObjectIdentifier    c2pnb163v2 = new DERObjectIdentifier(cTwoCurve + ".2");
    static final DERObjectIdentifier    c2pnb163v3 = new DERObjectIdentifier(cTwoCurve + ".3");
    static final DERObjectIdentifier    c2pnb176w1 = new DERObjectIdentifier(cTwoCurve + ".4");
    static final DERObjectIdentifier    c2tnb191v1 = new DERObjectIdentifier(cTwoCurve + ".5");
    static final DERObjectIdentifier    c2tnb191v2 = new DERObjectIdentifier(cTwoCurve + ".6");
    static final DERObjectIdentifier    c2tnb191v3 = new DERObjectIdentifier(cTwoCurve + ".7");
    static final DERObjectIdentifier    c2onb191v4 = new DERObjectIdentifier(cTwoCurve + ".8");
    static final DERObjectIdentifier    c2onb191v5 = new DERObjectIdentifier(cTwoCurve + ".9");
    static final DERObjectIdentifier    c2pnb208w1 = new DERObjectIdentifier(cTwoCurve + ".10");
    static final DERObjectIdentifier    c2tnb239v1 = new DERObjectIdentifier(cTwoCurve + ".11");
    static final DERObjectIdentifier    c2tnb239v2 = new DERObjectIdentifier(cTwoCurve + ".12");
    static final DERObjectIdentifier    c2tnb239v3 = new DERObjectIdentifier(cTwoCurve + ".13");
    static final DERObjectIdentifier    c2onb239v4 = new DERObjectIdentifier(cTwoCurve + ".14");
    static final DERObjectIdentifier    c2onb239v5 = new DERObjectIdentifier(cTwoCurve + ".15");
    static final DERObjectIdentifier    c2pnb272w1 = new DERObjectIdentifier(cTwoCurve + ".16");
    static final DERObjectIdentifier    c2pnb304w1 = new DERObjectIdentifier(cTwoCurve + ".17");
    static final DERObjectIdentifier    c2tnb359v1 = new DERObjectIdentifier(cTwoCurve + ".18");
    static final DERObjectIdentifier    c2pnb368w1 = new DERObjectIdentifier(cTwoCurve + ".19");
    static final DERObjectIdentifier    c2tnb431r1 = new DERObjectIdentifier(cTwoCurve + ".20");
    
    //
    // Prime
    //
    static final String     primeCurve = ellipticCurve + ".1";

    static final DERObjectIdentifier    prime192v1 = new DERObjectIdentifier(primeCurve + ".1");
    static final DERObjectIdentifier    prime192v2 = new DERObjectIdentifier(primeCurve + ".2");
    static final DERObjectIdentifier    prime192v3 = new DERObjectIdentifier(primeCurve + ".3");
    static final DERObjectIdentifier    prime239v1 = new DERObjectIdentifier(primeCurve + ".4");
    static final DERObjectIdentifier    prime239v2 = new DERObjectIdentifier(primeCurve + ".5");
    static final DERObjectIdentifier    prime239v3 = new DERObjectIdentifier(primeCurve + ".6");
    static final DERObjectIdentifier    prime256v1 = new DERObjectIdentifier(primeCurve + ".7");

    //
    // Diffie-Hellman
    //
    // dhpublicnumber OBJECT IDENTIFIER ::= { iso(1) member-body(2)
    //            us(840) ansi-x942(10046) number-type(2) 1 }
    //
    static final DERObjectIdentifier    dhpublicnumber = new DERObjectIdentifier("1.2.840.10046.2.1");

    //
    // DSA
    //
    // dsapublicnumber OBJECT IDENTIFIER ::= { iso(1) member-body(2)
    //            us(840) ansi-x957(10040) number-type(4) 1 }
    static final DERObjectIdentifier    id_dsa = new DERObjectIdentifier("1.2.840.10040.4.1");

    /**
     *   id-dsa-with-sha1 OBJECT IDENTIFIER ::=  { iso(1) member-body(2)
     *         us(840) x9-57 (10040) x9cm(4) 3 }
     */
    public static final DERObjectIdentifier id_dsa_with_sha1 = new DERObjectIdentifier("1.2.840.10040.4.3");

    /**
     * X9.63
     */
    public static final DERObjectIdentifier x9_63_scheme = new DERObjectIdentifier("1.3.133.16.840.63.0");
    public static final DERObjectIdentifier dhSinglePass_stdDH_sha1kdf_scheme = new DERObjectIdentifier(x9_63_scheme + ".2");
    public static final DERObjectIdentifier dhSinglePass_cofactorDH_sha1kdf_scheme = new DERObjectIdentifier(x9_63_scheme + ".3");
    public static final DERObjectIdentifier mqvSinglePass_sha1kdf_scheme = new DERObjectIdentifier(x9_63_scheme + ".16");

    /**
     * X9.42
     */
    public static final DERObjectIdentifier x9_42_schemes = new DERObjectIdentifier("1.2.840.10046.3");
    public static final DERObjectIdentifier dhStatic = new DERObjectIdentifier(x9_42_schemes + ".1");
    public static final DERObjectIdentifier dhEphem = new DERObjectIdentifier(x9_42_schemes + ".2");
    public static final DERObjectIdentifier dhOneFlow = new DERObjectIdentifier(x9_42_schemes + ".3");
    public static final DERObjectIdentifier dhHybrid1 = new DERObjectIdentifier(x9_42_schemes + ".4");
    public static final DERObjectIdentifier dhHybrid2 = new DERObjectIdentifier(x9_42_schemes + ".5");
    public static final DERObjectIdentifier dhHybridOneFlow = new DERObjectIdentifier(x9_42_schemes + ".6");
    public static final DERObjectIdentifier mqv2 = new DERObjectIdentifier(x9_42_schemes + ".7");
    public static final DERObjectIdentifier mqv1 = new DERObjectIdentifier(x9_42_schemes + ".8");
}

