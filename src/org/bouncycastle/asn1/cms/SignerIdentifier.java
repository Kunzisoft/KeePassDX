package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class SignerIdentifier
    extends ASN1Encodable
    implements ASN1Choice
{
    private DEREncodable id;
    
    public SignerIdentifier(
        IssuerAndSerialNumber id)
    {
        this.id = id;
    }
    
    public SignerIdentifier(
        ASN1OctetString id)
    {
        this.id = new DERTaggedObject(false, 0, id);
    }
    
    public SignerIdentifier(
        DERObject id)
    {
        this.id = id;
    }
    
    /**
     * return a SignerIdentifier object from the given object.
     *
     * @param o the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static SignerIdentifier getInstance(
        Object o)
    {
        if (o == null || o instanceof SignerIdentifier)
        {
            return (SignerIdentifier)o;
        }
        
        if (o instanceof IssuerAndSerialNumber)
        {
            return new SignerIdentifier((IssuerAndSerialNumber)o);
        }
        
        if (o instanceof ASN1OctetString)
        {
            return new SignerIdentifier((ASN1OctetString)o);
        }
        
        if (o instanceof DERObject)
        {
            return new SignerIdentifier((DERObject)o);
        }
        
        throw new IllegalArgumentException(
             "Illegal object in SignerIdentifier: " + o.getClass().getName());
    } 

    public boolean isTagged()
    {
        return (id instanceof ASN1TaggedObject);
    }

    public DEREncodable getId()
    {
        if (id instanceof ASN1TaggedObject)
        {
            return ASN1OctetString.getInstance((ASN1TaggedObject)id, false);
        }

        return id;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SignerIdentifier ::= CHOICE {
     *     issuerAndSerialNumber IssuerAndSerialNumber,
     *     subjectKeyIdentifier [0] SubjectKeyIdentifier 
     * }
     *
     * SubjectKeyIdentifier ::= OCTET STRING
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return id.getDERObject();
    }
}
