package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The extendedKeyUsage object.
 * <pre>
 *      extendedKeyUsage ::= SEQUENCE SIZE (1..MAX) OF KeyPurposeId
 * </pre>
 */
@SuppressWarnings("unchecked")
public class ExtendedKeyUsage
    extends ASN1Encodable
{
    Hashtable     usageTable = new Hashtable();
    ASN1Sequence  seq;

    public static ExtendedKeyUsage getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ExtendedKeyUsage getInstance(
        Object obj)
    {
        if (obj instanceof ExtendedKeyUsage) 
        {
            return (ExtendedKeyUsage)obj;
        }
        
        if(obj instanceof ASN1Sequence) 
        {
            return new ExtendedKeyUsage((ASN1Sequence)obj);
        }

        if (obj instanceof X509Extension)
        {
            return getInstance(X509Extension.convertValueToObject((X509Extension)obj));
        }

        throw new IllegalArgumentException("Invalid ExtendedKeyUsage: " + obj.getClass().getName());
    }

    public ExtendedKeyUsage(
        KeyPurposeId  usage)
    {
        this.seq = new DERSequence(usage);

        this.usageTable.put(usage, usage);
    }
    
    public ExtendedKeyUsage(
        ASN1Sequence  seq)
    {
        this.seq = seq;

        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            Object  o = e.nextElement();
            if (!(o instanceof DERObjectIdentifier))
            {
                throw new IllegalArgumentException("Only DERObjectIdentifiers allowed in ExtendedKeyUsage.");
            }
            this.usageTable.put(o, o);
        }
    }

    public ExtendedKeyUsage(
        Vector  usages)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        Enumeration         e = usages.elements();

        while (e.hasMoreElements())
        {
            DERObject  o = (DERObject)e.nextElement();

            v.add(o);
            this.usageTable.put(o, o);
        }

        this.seq = new DERSequence(v);
    }

    public boolean hasKeyPurposeId(
        KeyPurposeId keyPurposeId)
    {
        return (usageTable.get(keyPurposeId) != null);
    }
    
    /**
     * Returns all extended key usages.
     * The returned vector contains DERObjectIdentifiers.
     * @return A vector with all key purposes.
     */
    public Vector getUsages()
    {
        Vector temp = new Vector();
        for (Enumeration it = usageTable.elements(); it.hasMoreElements();)
        {
            temp.addElement(it.nextElement());
        }
        return temp;
    }

    public int size()
    {
        return usageTable.size();
    }
    
    public DERObject toASN1Object()
    {
        return seq;
    }
}
