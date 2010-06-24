package org.bouncycastle.asn1.ess;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;

public class ContentIdentifier
    extends ASN1Encodable
{
     ASN1OctetString value;

    public static ContentIdentifier getInstance(Object o)
    {
        if (o == null || o instanceof ContentIdentifier)
        {
            return (ContentIdentifier) o;
        }
        else if (o instanceof ASN1OctetString)
        {
            return new ContentIdentifier((ASN1OctetString) o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'ContentIdentifier' factory : "
                        + o.getClass().getName() + ".");
    }

    /**
     * Create from OCTET STRING whose octets represent the identifier.
     */
    public ContentIdentifier(
        ASN1OctetString value)
    {
        this.value = value;
    }

    /**
     * Create from byte array representing the identifier.
     */
    public ContentIdentifier(
        byte[] value)
    {
        this(new DEROctetString(value));
    }
    
    public ASN1OctetString getValue()
    {
        return value;
    }

    /**
     * The definition of ContentIdentifier is
     * <pre>
     * ContentIdentifier ::=  OCTET STRING
     * </pre>
     * id-aa-contentIdentifier OBJECT IDENTIFIER ::= { iso(1)
     *  member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs9(9)
     *  smime(16) id-aa(2) 7 }
     */
    public DERObject toASN1Object()
    {
        return value;
    }
}
