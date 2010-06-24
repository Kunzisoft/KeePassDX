package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

public class KEKIdentifier
    extends ASN1Encodable
{
    private ASN1OctetString    keyIdentifier;
    private DERGeneralizedTime date;
    private OtherKeyAttribute  other;
    
    public KEKIdentifier(
        byte[]              keyIdentifier,
        DERGeneralizedTime  date,
        OtherKeyAttribute   other)
    {
        this.keyIdentifier = new DEROctetString(keyIdentifier);
        this.date = date;
        this.other = other;
    }
    
    public KEKIdentifier(
        ASN1Sequence seq)
    {
        keyIdentifier = (ASN1OctetString)seq.getObjectAt(0);
        
        switch (seq.size())
        {
        case 1:
            break;
        case 2:
            if (seq.getObjectAt(1) instanceof DERGeneralizedTime)
            {
                date = (DERGeneralizedTime)seq.getObjectAt(1); 
            }
            else
            {
                other = OtherKeyAttribute.getInstance(seq.getObjectAt(1));
            }
            break;
        case 3:
            date  = (DERGeneralizedTime)seq.getObjectAt(1);
            other = OtherKeyAttribute.getInstance(seq.getObjectAt(2));
            break;
        default:
                throw new IllegalArgumentException("Invalid KEKIdentifier");
        }
    }

    /**
     * return a KEKIdentifier object from a tagged object.
     *
     * @param obj the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static KEKIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    
    /**
     * return a KEKIdentifier object from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static KEKIdentifier getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof KEKIdentifier)
        {
            return (KEKIdentifier)obj;
        }
        
        if (obj instanceof ASN1Sequence)
        {
            return new KEKIdentifier((ASN1Sequence)obj);
        }
        
        throw new IllegalArgumentException("Invalid KEKIdentifier: " + obj.getClass().getName());
    }

    public ASN1OctetString getKeyIdentifier()
    {
        return keyIdentifier;
    }

    public DERGeneralizedTime getDate()
    {
        return date;
    }

    public OtherKeyAttribute getOther()
    {
        return other;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * KEKIdentifier ::= SEQUENCE {
     *     keyIdentifier OCTET STRING,
     *     date GeneralizedTime OPTIONAL,
     *     other OtherKeyAttribute OPTIONAL 
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(keyIdentifier);
        
        if (date != null)
        {
            v.add(date);
        }

        if (other != null)
        {
            v.add(other);
        }
        
        return new DERSequence(v);
    }
}
