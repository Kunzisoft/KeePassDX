package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.*;

public class SignaturePolicyId
    extends ASN1Encodable
{
    private DERObjectIdentifier  sigPolicyId;
    private OtherHashAlgAndValue sigPolicyHash;
    private SigPolicyQualifiers  sigPolicyQualifiers;


    public static SignaturePolicyId getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SignaturePolicyId)
        {
            return (SignaturePolicyId) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SignaturePolicyId((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException(
                "Unknown object in 'SignaturePolicyId' factory : "
                        + obj.getClass().getName() + ".");
    }

    public SignaturePolicyId(
        ASN1Sequence seq)
    {
        if (seq.size() != 2 && seq.size() != 3)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        sigPolicyId = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
        sigPolicyHash = OtherHashAlgAndValue.getInstance(seq.getObjectAt(1));

        if (seq.size() == 3)
        {
            sigPolicyQualifiers = SigPolicyQualifiers.getInstance(seq.getObjectAt(2));
        }
    }

    public SignaturePolicyId(
        DERObjectIdentifier   sigPolicyIdentifier,
        OtherHashAlgAndValue  sigPolicyHash)
    {
        this(sigPolicyIdentifier, sigPolicyHash, null);
    }

    public SignaturePolicyId(
        DERObjectIdentifier   sigPolicyId,
        OtherHashAlgAndValue  sigPolicyHash,
        SigPolicyQualifiers   sigPolicyQualifiers)
    {
        this.sigPolicyId = sigPolicyId;
        this.sigPolicyHash = sigPolicyHash;
        this.sigPolicyQualifiers = sigPolicyQualifiers;
    }

    public DERObjectIdentifier getSigPolicyId()
    {
        return sigPolicyId;
    }

    public OtherHashAlgAndValue getSigPolicyHash()
    {
        return sigPolicyHash;
    }

    public SigPolicyQualifiers getSigPolicyQualifiers()
    {
        return sigPolicyQualifiers;
    }

    /**
     * <pre>
     * SignaturePolicyId ::= SEQUENCE {
     *     sigPolicyId SigPolicyId,
     *     sigPolicyHash SigPolicyHash,
     *     sigPolicyQualifiers SEQUENCE SIZE (1..MAX) OF SigPolicyQualifierInfo OPTIONAL}
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(sigPolicyId);
        v.add(sigPolicyHash);
        if (sigPolicyQualifiers != null)
        {
            v.add(sigPolicyQualifiers);
        }

        return new DERSequence(v);
    }
}
