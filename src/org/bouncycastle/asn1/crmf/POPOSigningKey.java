package org.bouncycastle.asn1.crmf;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class POPOSigningKey
    extends ASN1Encodable
{
    private POPOSigningKeyInput poposkInput;
    private AlgorithmIdentifier algorithmIdentifier;
    private DERBitString        signature;

    private POPOSigningKey(ASN1Sequence seq)
    {
        int index = 0;

        if (seq.getObjectAt(0) instanceof ASN1TaggedObject)
        {
            poposkInput = POPOSigningKeyInput.getInstance(seq.getObjectAt(index++));
        }
        algorithmIdentifier = AlgorithmIdentifier.getInstance(seq.getObjectAt(index++));
        signature = DERBitString.getInstance(seq.getObjectAt(index));
    }

    public static POPOSigningKey getInstance(Object o)
    {
        if (o instanceof POPOSigningKey)
        {
            return (POPOSigningKey)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new POPOSigningKey((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public static POPOSigningKey getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * <pre>
     * POPOSigningKey ::= SEQUENCE {
     *                      poposkInput           [0] POPOSigningKeyInput OPTIONAL,
     *                      algorithmIdentifier   AlgorithmIdentifier,
     *                      signature             BIT STRING }
     *  -- The signature (using "algorithmIdentifier") is on the
     *  -- DER-encoded value of poposkInput.  NOTE: If the CertReqMsg
     *  -- certReq CertTemplate contains the subject and publicKey values,
     *  -- then poposkInput MUST be omitted and the signature MUST be
     *  -- computed on the DER-encoded value of CertReqMsg certReq.  If
     *  -- the CertReqMsg certReq CertTemplate does not contain the public
     *  -- key and subject values, then poposkInput MUST be present and
     *  -- MUST be signed.  This strategy ensures that the public key is
     *  -- not present in both the poposkInput and CertReqMsg certReq
     *  -- CertTemplate fields.
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (poposkInput != null)
        {
            v.add(poposkInput);
        }

        v.add(algorithmIdentifier);
        v.add(signature);

        return new DERSequence(v);
    }
}
