package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public interface CommitmentTypeIdentifier
{
    public static final DERObjectIdentifier proofOfOrigin = PKCSObjectIdentifiers.id_cti_ets_proofOfOrigin;
    public static final DERObjectIdentifier proofOfReceipt = PKCSObjectIdentifiers.id_cti_ets_proofOfReceipt;
    public static final DERObjectIdentifier proofOfDelivery = PKCSObjectIdentifiers.id_cti_ets_proofOfDelivery;
    public static final DERObjectIdentifier proofOfSender = PKCSObjectIdentifiers.id_cti_ets_proofOfSender;
    public static final DERObjectIdentifier proofOfApproval = PKCSObjectIdentifiers.id_cti_ets_proofOfApproval;
    public static final DERObjectIdentifier proofOfCreation = PKCSObjectIdentifiers.id_cti_ets_proofOfCreation;
}