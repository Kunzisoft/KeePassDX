package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBoolean;

import java.io.IOException;

/**
 * an object for the elements in the X.509 V3 extension block.
 */
public class X509Extension
{
    boolean             critical;
    ASN1OctetString      value;

    public X509Extension(
        DERBoolean              critical,
        ASN1OctetString         value)
    {
        this.critical = critical.isTrue();
        this.value = value;
    }

    public X509Extension(
        boolean                 critical,
        ASN1OctetString         value)
    {
        this.critical = critical;
        this.value = value;
    }

    public boolean isCritical()
    {
        return critical;
    }

    public ASN1OctetString getValue()
    {
        return value;
    }

    public int hashCode()
    {
        if (this.isCritical())
        {
            return this.getValue().hashCode();
        }

        
        return ~this.getValue().hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (!(o instanceof X509Extension))
        {
            return false;
        }

        X509Extension   other = (X509Extension)o;

        return other.getValue().equals(this.getValue())
            && (other.isCritical() == this.isCritical());
    }

    /**
     * Convert the value of the passed in extension to an object
     * @param ext the extension to parse
     * @return the object the value string contains
     * @exception IllegalArgumentException if conversion is not possible
     */
    public static ASN1Object convertValueToObject(
        X509Extension ext)
        throws IllegalArgumentException
    {
        try
        {
            return ASN1Object.fromByteArray(ext.getValue().getOctets());
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("can't convert extension: " +  e);
        }
    }
}
