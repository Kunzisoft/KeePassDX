package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public interface CMSObjectIdentifiers
{
    static final DERObjectIdentifier    data = PKCSObjectIdentifiers.data;
    static final DERObjectIdentifier    signedData = PKCSObjectIdentifiers.signedData;
    static final DERObjectIdentifier    envelopedData = PKCSObjectIdentifiers.envelopedData;
    static final DERObjectIdentifier    signedAndEnvelopedData = PKCSObjectIdentifiers.signedAndEnvelopedData;
    static final DERObjectIdentifier    digestedData = PKCSObjectIdentifiers.digestedData;
    static final DERObjectIdentifier    encryptedData = PKCSObjectIdentifiers.encryptedData;
    static final DERObjectIdentifier    authenticatedData = PKCSObjectIdentifiers.id_ct_authData;
    static final DERObjectIdentifier    compressedData = PKCSObjectIdentifiers.id_ct_compressedData;
    static final DERObjectIdentifier    authEnvelopedData = PKCSObjectIdentifiers.id_ct_authEnvelopedData;
}
