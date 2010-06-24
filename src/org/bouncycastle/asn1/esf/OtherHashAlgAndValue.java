package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.*;

public class OtherHashAlgAndValue
    extends ASN1Encodable
{
    private AlgorithmIdentifier hashAlgorithm;
    private ASN1OctetString     hashValue;


    public static OtherHashAlgAndValue getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof OtherHashAlgAndValue)
        {
            return (OtherHashAlgAndValue) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new OtherHashAlgAndValue((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'OtherHashAlgAndValue' factory : "
                        + obj.getClass().getName() + ".");
    }

    public OtherHashAlgAndValue(
        ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        hashAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        hashValue = ASN1OctetString.getInstance(seq.getObjectAt(1));
    }

    public OtherHashAlgAndValue(
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     hashValue)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.hashValue = hashValue;
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }

    public ASN1OctetString getHashValue()
    {
        return hashValue;
    }

    /**
     * <pre>
     * OtherHashAlgAndValue ::= SEQUENCE {
     *     hashAlgorithm AlgorithmIdentifier,
     *     hashValue OtherHashValue }
     *
     * OtherHashValue ::= OCTET STRING
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(hashAlgorithm);
        v.add(hashValue);

        return new DERSequence(v);
    }
}
