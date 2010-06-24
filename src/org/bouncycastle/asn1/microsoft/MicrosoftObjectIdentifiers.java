package org.bouncycastle.asn1.microsoft;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface MicrosoftObjectIdentifiers
{
    //
    // Microsoft
    //       iso(1) identified-organization(3) dod(6) internet(1) private(4) enterprise(1) microsoft(311)
    //
    static final DERObjectIdentifier    microsoft               = new DERObjectIdentifier("1.3.6.1.4.1.311");
    static final DERObjectIdentifier    microsoftCertTemplateV1 = new DERObjectIdentifier(microsoft + ".20.2");
    static final DERObjectIdentifier    microsoftCaVersion      = new DERObjectIdentifier(microsoft + ".21.1");
    static final DERObjectIdentifier    microsoftPrevCaCertHash = new DERObjectIdentifier(microsoft + ".21.2");
    static final DERObjectIdentifier    microsoftCertTemplateV2 = new DERObjectIdentifier(microsoft + ".21.7");
    static final DERObjectIdentifier    microsoftAppPolicies    = new DERObjectIdentifier(microsoft + ".21.10");
}
