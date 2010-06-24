package org.bouncycastle.asn1.ess;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

public class ContentHints
    extends ASN1Encodable
{
    private DERUTF8String contentDescription;
    private DERObjectIdentifier contentType;

    public static ContentHints getInstance(Object o)
    {
        if (o == null || o instanceof ContentHints)
        {
            return (ContentHints)o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new ContentHints((ASN1Sequence)o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'ContentHints' factory : "
                        + o.getClass().getName() + ".");
    }

    /**
     * constructor
     */
    private ContentHints(ASN1Sequence seq)
    {
        DEREncodable field = seq.getObjectAt(0);
        if (field.getDERObject() instanceof DERUTF8String)
        {
            contentDescription = DERUTF8String.getInstance(field);
            contentType = DERObjectIdentifier.getInstance(seq.getObjectAt(1));
        }
        else
        {
            contentType = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
        }
    }

    public ContentHints(
        DERObjectIdentifier contentType)
    {
        this.contentType = contentType;
        this.contentDescription = null;
    }

    public ContentHints(
        DERObjectIdentifier contentType,
        DERUTF8String contentDescription)
    {
        this.contentType = contentType;
        this.contentDescription = contentDescription;
    }

    public DERObjectIdentifier getContentType()
    {
        return contentType;
    }

    public DERUTF8String getContentDescription()
    {
        return contentDescription;
    }

    /**
     * <pre>
     * ContentHints ::= SEQUENCE {
     *   contentDescription UTF8String (SIZE (1..MAX)) OPTIONAL,
     *   contentType ContentType }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (contentDescription != null)
        {
            v.add(contentDescription);
        }

        v.add(contentType);

        return new DERSequence(v);
    }
}
