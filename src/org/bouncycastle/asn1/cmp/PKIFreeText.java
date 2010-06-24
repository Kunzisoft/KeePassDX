package org.bouncycastle.asn1.cmp;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

@SuppressWarnings("unchecked")
public class PKIFreeText
    extends ASN1Encodable
{
    ASN1Sequence strings;

    public static PKIFreeText getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static PKIFreeText getInstance(
        Object obj)
    {
        if (obj instanceof PKIFreeText)
        {
            return (PKIFreeText)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new PKIFreeText((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Unknown object in factory: " + obj.getClass().getName());
    }

	public PKIFreeText(
        ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            if (!(e.nextElement() instanceof DERUTF8String))
            {
                throw new IllegalArgumentException("attempt to insert non UTF8 STRING into PKIFreeText");
            }
        }
        
        strings = seq;
    }

    public PKIFreeText(
        DERUTF8String p)
    {
        strings = new DERSequence(p);
    }

    /**
     * Return the number of string elements present.
     * 
     * @return number of elements present.
     */
    public int size()
    {
        return strings.size();
    }
    
    /**
     * Return the UTF8STRING at index i.
     * 
     * @param i index of the string of interest
     * @return the string at index i.
     */
    public DERUTF8String getStringAt(
        int i)
    {
        return (DERUTF8String)strings.getObjectAt(i);
    }
    
    /**
     * <pre>
     * PKIFreeText ::= SEQUENCE SIZE (1..MAX) OF UTF8String
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return strings;
    }
}
