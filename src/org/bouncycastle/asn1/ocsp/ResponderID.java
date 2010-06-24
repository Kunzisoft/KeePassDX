package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.X509Name;

public class ResponderID
    extends ASN1Encodable
    implements ASN1Choice
{
    private DEREncodable    value;

    public ResponderID(
        ASN1OctetString    value)
    {
        this.value = value;
    }

    public ResponderID(
        X509Name    value)
    {
        this.value = value;
    }

    public static ResponderID getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ResponderID)
        {
            return (ResponderID)obj;
        }
        else if (obj instanceof DEROctetString)
        {
            return new ResponderID((DEROctetString)obj);
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)obj;

            if (o.getTagNo() == 1)
            {
                return new ResponderID(X509Name.getInstance(o, true));
            }
            else
            {
                return new ResponderID(ASN1OctetString.getInstance(o, true));
            }
        }

        return new ResponderID(X509Name.getInstance(obj));
    }

    public static ResponderID getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject()); // must be explicitly tagged
    }
    
    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * ResponderID ::= CHOICE {
     *      byName          [1] Name,
     *      byKey           [2] KeyHash }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        if (value instanceof ASN1OctetString)
        {
            return new DERTaggedObject(true, 2, value);
        }

        return new DERTaggedObject(true, 1, value);
    }
}
