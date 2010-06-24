package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class PollRepContent
    extends ASN1Encodable
{
    private DERInteger certReqId;
    private DERInteger checkAfter;
    private PKIFreeText reason;

    private PollRepContent(ASN1Sequence seq)
    {
        certReqId = DERInteger.getInstance(seq.getObjectAt(0));
        checkAfter = DERInteger.getInstance(seq.getObjectAt(1));

        if (seq.size() > 2)
        {
            reason = PKIFreeText.getInstance(seq.getObjectAt(2));
        }
    }

    public static PollRepContent getInstance(Object o)
    {
        if (o instanceof PollRepContent)
        {
            return (PollRepContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new PollRepContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public DERInteger getCertReqId()
    {
        return certReqId;
    }

    public DERInteger getCheckAfter()
    {
        return checkAfter;
    }

    public PKIFreeText getReason()
    {
        return reason;
    }

    /**
     * <pre>
     * PollRepContent ::= SEQUENCE OF SEQUENCE {
     *         certReqId              INTEGER,
     *         checkAfter             INTEGER,  -- time in seconds
     *         reason                 PKIFreeText OPTIONAL
     *     }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(certReqId);
        v.add(checkAfter);

        if (reason != null)
        {
            v.add(reason);
        }
        
        return new DERSequence(v);
    }
}
