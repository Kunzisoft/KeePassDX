package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Generator for X.509 extensions
 */
@SuppressWarnings("unchecked")
public class X509ExtensionsGenerator
{
    private Hashtable extensions = new Hashtable();
    private Vector extOrdering = new Vector();

    /**
     * Reset the generator
     */
    public void reset()
    {
        extensions = new Hashtable();
        extOrdering = new Vector();
    }

    /**
     * Add an extension with the given oid and the passed in value to be included
     * in the OCTET STRING associated with the extension.
     *
     * @param oid  OID for the extension.
     * @param critical  true if critical, false otherwise.
     * @param value the ASN.1 object to be included in the extension.
     */
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        DEREncodable        value)
    {
        try
        {
            this.addExtension(oid, critical, value.getDERObject().getEncoded(ASN1Encodable.DER));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("error encoding value: " + e);
        }
    }

    /**
     * Add an extension with the given oid and the passed in byte array to be wrapped in the
     * OCTET STRING associated with the extension.
     *
     * @param oid OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value the byte array to be wrapped.
     */
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        byte[]              value)
    {
        if (extensions.containsKey(oid))
        {
            throw new IllegalArgumentException("extension " + oid + " already added");
        }

        extOrdering.addElement(oid);
        extensions.put(oid, new X509Extension(critical, new DEROctetString(value)));
    }

    /**
     * Return true if there are no extension present in this generator.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty()
    {
        return extOrdering.isEmpty();
    }

    /**
     * Generate an X509Extensions object based on the current state of the generator.
     *
     * @return  an X09Extensions object.
     */
    public X509Extensions generate()
    {
        return new X509Extensions(extOrdering, extensions);
    }
}
