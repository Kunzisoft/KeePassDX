package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;

public class PollReqContent
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private PollReqContent(ASN1Sequence seq)
    {
        content = seq;
    }

    public static PollReqContent getInstance(Object o)
    {
        if (o instanceof PollReqContent)
        {
            return (PollReqContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new PollReqContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public DERInteger[][] getCertReqIds()
    {
        DERInteger[][] result = new DERInteger[content.size()][];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = seqenceToDERIntegerArray((ASN1Sequence)content.getObjectAt(i));
        }

        return result;
    }

    private DERInteger[] seqenceToDERIntegerArray(ASN1Sequence seq)
    {
         DERInteger[] result = new DERInteger[seq.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = DERInteger.getInstance(seq.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * PollReqContent ::= SEQUENCE OF SEQUENCE {
     *                        certReqId              INTEGER
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
