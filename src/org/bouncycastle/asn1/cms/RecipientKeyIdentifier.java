package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class RecipientKeyIdentifier
    extends ASN1Encodable
{
    private ASN1OctetString      subjectKeyIdentifier;
    private DERGeneralizedTime   date;
    private OtherKeyAttribute    other;

    public RecipientKeyIdentifier(
        ASN1OctetString         subjectKeyIdentifier,
        DERGeneralizedTime      date,
        OtherKeyAttribute       other)
    {
        this.subjectKeyIdentifier = subjectKeyIdentifier;
        this.date = date;
        this.other = other;
    }
    
    public RecipientKeyIdentifier(
        ASN1Sequence seq)
    {
        subjectKeyIdentifier = ASN1OctetString.getInstance(
                                                    seq.getObjectAt(0));
        
        switch(seq.size())
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
                other = OtherKeyAttribute.getInstance(seq.getObjectAt(2));
            }
            break;
        case 3:
            date  = (DERGeneralizedTime)seq.getObjectAt(1);
            other = OtherKeyAttribute.getInstance(seq.getObjectAt(2));
            break;
        default:
            throw new IllegalArgumentException("Invalid RecipientKeyIdentifier");
        }
    }

    /**
     * return a RecipientKeyIdentifier object from a tagged object.
     *
     * @param _ato the tagged object holding the object we want.
     * @param _explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static RecipientKeyIdentifier getInstance(ASN1TaggedObject _ato, boolean _explicit)
    {
        return getInstance(ASN1Sequence.getInstance(_ato, _explicit));
    }
    
    /**
     * return a RecipientKeyIdentifier object from the given object.
     *
     * @param _obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static RecipientKeyIdentifier getInstance(Object _obj)
    {
        if(_obj == null || _obj instanceof RecipientKeyIdentifier)
        {
            return (RecipientKeyIdentifier)_obj;
        }
        
        if(_obj instanceof ASN1Sequence)
        {
            return new RecipientKeyIdentifier((ASN1Sequence)_obj);
        }
        
        throw new IllegalArgumentException("Invalid RecipientKeyIdentifier: " + _obj.getClass().getName());
    } 

    public ASN1OctetString getSubjectKeyIdentifier()
    {
        return subjectKeyIdentifier;
    }

    public DERGeneralizedTime getDate()
    {
        return date;
    }

    public OtherKeyAttribute getOtherKeyAttribute()
    {
        return other;
    }


    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * RecipientKeyIdentifier ::= SEQUENCE {
     *     subjectKeyIdentifier SubjectKeyIdentifier,
     *     date GeneralizedTime OPTIONAL,
     *     other OtherKeyAttribute OPTIONAL 
     * }
     *
     * SubjectKeyIdentifier ::= OCTET STRING
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(subjectKeyIdentifier);
        
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
