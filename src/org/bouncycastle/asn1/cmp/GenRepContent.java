package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;

public class GenRepContent
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private GenRepContent(ASN1Sequence seq)
    {
        content = seq;
    }

    public static GenRepContent getInstance(Object o)
    {
        if (o instanceof GenRepContent)
        {
            return (GenRepContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new GenRepContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public InfoTypeAndValue[] toInfoTypeAndValueArray()
    {
        InfoTypeAndValue[] result = new InfoTypeAndValue[content.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = InfoTypeAndValue.getInstance(content.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * GenRepContent ::= SEQUENCE OF InfoTypeAndValue
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
