package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class OtherRecipientInfo
    extends ASN1Encodable
{
    private DERObjectIdentifier    oriType;
    private DEREncodable           oriValue;

    public OtherRecipientInfo(
        DERObjectIdentifier     oriType,
        DEREncodable            oriValue)
    {
        this.oriType = oriType;
        this.oriValue = oriValue;
    }
    
    public OtherRecipientInfo(
        ASN1Sequence seq)
    {
        oriType = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
        oriValue = seq.getObjectAt(1);
    }

    /**
     * return a OtherRecipientInfo object from a tagged object.
     *
     * @param obj the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static OtherRecipientInfo getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    
    /**
     * return a OtherRecipientInfo object from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static OtherRecipientInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof OtherRecipientInfo)
        {
            return (OtherRecipientInfo)obj;
        }
        
        if (obj instanceof ASN1Sequence)
        {
            return new OtherRecipientInfo((ASN1Sequence)obj);
        }
        
        throw new IllegalArgumentException("Invalid OtherRecipientInfo: " + obj.getClass().getName());
    }

    public DERObjectIdentifier getType()
    {
        return oriType;
    }

    public DEREncodable getValue()
    {
        return oriValue;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * OtherRecipientInfo ::= SEQUENCE {
     *    oriType OBJECT IDENTIFIER,
     *    oriValue ANY DEFINED BY oriType }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(oriType);
        v.add(oriValue);

        return new DERSequence(v);
    }
}
