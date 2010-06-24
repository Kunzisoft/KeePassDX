package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.*;

public class SigPolicyQualifierInfo
    extends ASN1Encodable
{
    private DERObjectIdentifier  sigPolicyQualifierId;
    private DEREncodable         sigQualifier;

    public SigPolicyQualifierInfo(
        DERObjectIdentifier   sigPolicyQualifierId,
        DEREncodable          sigQualifier)
    {
        this.sigPolicyQualifierId = sigPolicyQualifierId;
        this.sigQualifier = sigQualifier;
    }

    public SigPolicyQualifierInfo(
        ASN1Sequence seq)
    {
        sigPolicyQualifierId = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
        sigQualifier = seq.getObjectAt(1);
    }

    public static SigPolicyQualifierInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SigPolicyQualifierInfo)
        {
            return (SigPolicyQualifierInfo) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SigPolicyQualifierInfo((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SigPolicyQualifierInfo' factory: "
                        + obj.getClass().getName() + ".");
    }

    public DERObjectIdentifier getSigPolicyQualifierId()
    {
        return sigPolicyQualifierId;
    }

    public DEREncodable getSigQualifier()
    {
        return sigQualifier;
    }

    /**
     * <pre>
     * SigPolicyQualifierInfo ::= SEQUENCE {
     *    sigPolicyQualifierId SigPolicyQualifierId,
     *    sigQualifier ANY DEFINED BY sigPolicyQualifierId }
     *
     * SigPolicyQualifierId ::= OBJECT IDENTIFIER
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(sigPolicyQualifierId);
        v.add(sigQualifier);

        return new DERSequence(v);
    }
}
