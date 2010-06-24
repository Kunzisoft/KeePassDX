package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public interface CMSAttributes
{
    public static final DERObjectIdentifier  contentType = PKCSObjectIdentifiers.pkcs_9_at_contentType;
    public static final DERObjectIdentifier  messageDigest = PKCSObjectIdentifiers.pkcs_9_at_messageDigest;
    public static final DERObjectIdentifier  signingTime = PKCSObjectIdentifiers.pkcs_9_at_signingTime;
    public static final DERObjectIdentifier  counterSignature = PKCSObjectIdentifiers.pkcs_9_at_counterSignature;
    public static final DERObjectIdentifier  contentHint = PKCSObjectIdentifiers.id_aa_contentHint;
}
