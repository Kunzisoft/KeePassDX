package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.*;

public class SignaturePolicyIdentifier
    extends ASN1Encodable
{
    private SignaturePolicyId   signaturePolicyId;
    private boolean             isSignaturePolicyImplied;

    public static SignaturePolicyIdentifier getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof SignaturePolicyIdentifier)
        {
            return (SignaturePolicyIdentifier) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SignaturePolicyIdentifier(SignaturePolicyId.getInstance(obj));
        }
        else if (obj instanceof ASN1Null)
        {
            return new SignaturePolicyIdentifier();
        }

        throw new IllegalArgumentException(
                "unknown object in 'SignaturePolicyIdentifier' factory: "
                        + obj.getClass().getName() + ".");
    }

    public SignaturePolicyIdentifier()
    {
        this.isSignaturePolicyImplied = true;
    }

    public SignaturePolicyIdentifier(
        SignaturePolicyId signaturePolicyId)
    {
        this.signaturePolicyId = signaturePolicyId;
        this.isSignaturePolicyImplied = false;
    }

    public SignaturePolicyId getSignaturePolicyId()
    {
        return signaturePolicyId;
    }

    public boolean isSignaturePolicyImplied()
    {
        return isSignaturePolicyImplied;
    }

    /**
     * <pre>
     * SignaturePolicyIdentifier ::= CHOICE{
     *     SignaturePolicyId         SignaturePolicyId,
     *     SignaturePolicyImplied    SignaturePolicyImplied }
     *
     * SignaturePolicyImplied ::= NULL
     * </pre>
     */
    public DERObject toASN1Object()
    {
        if (isSignaturePolicyImplied)
        {
            return new DERNull();
        }
        else
        {
            return signaturePolicyId.getDERObject();
        }
    }
}
