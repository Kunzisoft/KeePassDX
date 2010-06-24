package org.bouncycastle.asn1.smime;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * Handler class for dealing with S/MIME Capabilities
 */
@SuppressWarnings("unchecked")
public class SMIMECapabilities
    extends ASN1Encodable
{
    /**
     * general preferences
     */
    public static final DERObjectIdentifier preferSignedData = PKCSObjectIdentifiers.preferSignedData;
    public static final DERObjectIdentifier canNotDecryptAny = PKCSObjectIdentifiers.canNotDecryptAny;
    public static final DERObjectIdentifier sMIMECapabilitesVersions = PKCSObjectIdentifiers.sMIMECapabilitiesVersions;

    /**
     * encryption algorithms preferences
     */
    public static final DERObjectIdentifier dES_CBC = new DERObjectIdentifier("1.3.14.3.2.7");
    public static final DERObjectIdentifier dES_EDE3_CBC = PKCSObjectIdentifiers.des_EDE3_CBC;
    public static final DERObjectIdentifier rC2_CBC = PKCSObjectIdentifiers.RC2_CBC;
    
    private ASN1Sequence         capabilities;

    /**
     * return an Attribute object from the given object.
     *
     * @param o the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static SMIMECapabilities getInstance(
        Object o)
    {
        if (o == null || o instanceof SMIMECapabilities)
        {
            return (SMIMECapabilities)o;
        }
        
        if (o instanceof ASN1Sequence)
        {
            return new SMIMECapabilities((ASN1Sequence)o);
        }

        if (o instanceof Attribute)
        {
            return new SMIMECapabilities(
                (ASN1Sequence)(((Attribute)o).getAttrValues().getObjectAt(0)));
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    
    public SMIMECapabilities(
        ASN1Sequence seq)
    {
        capabilities = seq;
    }

    /**
     * returns a vector with 0 or more objects of all the capabilities
     * matching the passed in capability OID. If the OID passed is null the
     * entire set is returned.
     */
    public Vector getCapabilities(
        DERObjectIdentifier capability)
    {
        Enumeration e = capabilities.getObjects();
        Vector      list = new Vector();

        if (capability == null)
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());

                list.addElement(cap);
            }
        }
        else
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());

                if (capability.equals(cap.getCapabilityID()))
                {
                    list.addElement(cap);
                }
            }
        }

        return list;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SMIMECapabilities ::= SEQUENCE OF SMIMECapability
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return capabilities;
    }
}
