package org.bouncycastle.asn1.misc;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface MiscObjectIdentifiers
{
    //
    // Netscape
    //       iso/itu(2) joint-assign(16) us(840) uscompany(1) netscape(113730) cert-extensions(1) }
    //
    static final String                 netscape                = "2.16.840.1.113730.1";
    static final DERObjectIdentifier    netscapeCertType        = new DERObjectIdentifier(netscape + ".1");
    static final DERObjectIdentifier    netscapeBaseURL         = new DERObjectIdentifier(netscape + ".2");
    static final DERObjectIdentifier    netscapeRevocationURL   = new DERObjectIdentifier(netscape + ".3");
    static final DERObjectIdentifier    netscapeCARevocationURL = new DERObjectIdentifier(netscape + ".4");
    static final DERObjectIdentifier    netscapeRenewalURL      = new DERObjectIdentifier(netscape + ".7");
    static final DERObjectIdentifier    netscapeCApolicyURL     = new DERObjectIdentifier(netscape + ".8");
    static final DERObjectIdentifier    netscapeSSLServerName   = new DERObjectIdentifier(netscape + ".12");
    static final DERObjectIdentifier    netscapeCertComment     = new DERObjectIdentifier(netscape + ".13");
    //
    // Verisign
    //       iso/itu(2) joint-assign(16) us(840) uscompany(1) verisign(113733) cert-extensions(1) }
    //
    static final String                 verisign                = "2.16.840.1.113733.1";

    //
    // CZAG - country, zip, age, and gender
    //
    static final DERObjectIdentifier    verisignCzagExtension   = new DERObjectIdentifier(verisign + ".6.3");
    // D&B D-U-N-S number
    static final DERObjectIdentifier    verisignDnbDunsNumber   = new DERObjectIdentifier(verisign + ".6.15");

    //
    // Novell
    //       iso/itu(2) country(16) us(840) organization(1) novell(113719)
    //
    static final String                 novell                  = "2.16.840.1.113719";
    static final DERObjectIdentifier    novellSecurityAttribs   = new DERObjectIdentifier(novell + ".1.9.4.1");

    //
    // Entrust
    //       iso(1) member-body(16) us(840) nortelnetworks(113533) entrust(7)
    //
    static final String                 entrust                 = "1.2.840.113533.7";
    static final DERObjectIdentifier    entrustVersionExtension = new DERObjectIdentifier(entrust + ".65.0");
}
