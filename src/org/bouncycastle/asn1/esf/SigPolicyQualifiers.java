package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.*;

public class SigPolicyQualifiers
    extends ASN1Encodable
{
    ASN1Sequence qualifiers;

    public static SigPolicyQualifiers getInstance(
        Object obj)
    {
        if (obj instanceof SigPolicyQualifiers)
        {
            return (SigPolicyQualifiers) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SigPolicyQualifiers((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SigPolicyQualifiers' factory: "
                        + obj.getClass().getName() + ".");
    }

    public SigPolicyQualifiers(
        ASN1Sequence seq)
    {
        qualifiers = seq;
    }

    public SigPolicyQualifiers(
        SigPolicyQualifierInfo[] qualifierInfos)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i=0; i < qualifierInfos.length; i++)
        {
            v.add(qualifierInfos[i]);
        }
        qualifiers = new DERSequence(v);
    }

    /**
     * Return the number of qualifier info elements present.
     *
     * @return number of elements present.
     */
    public int size()
    {
        return qualifiers.size();
    }

    /**
     * Return the SigPolicyQualifierInfo at index i.
     *
     * @param i index of the string of interest
     * @return the string at index i.
     */
    public SigPolicyQualifierInfo getStringAt(
        int i)
    {
        return SigPolicyQualifierInfo.getInstance(qualifiers.getObjectAt(i));
    }

    /**
     * <pre>
     * SigPolicyQualifiers ::= SEQUENCE SIZE (1..MAX) OF SigPolicyQualifierInfo
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return qualifiers;
    }
}
