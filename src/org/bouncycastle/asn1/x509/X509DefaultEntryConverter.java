package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERUTF8String;

import java.io.IOException;

/**
 * The default converter for X509 DN entries when going from their
 * string value to ASN.1 strings.
 */
public class X509DefaultEntryConverter
    extends X509NameEntryConverter
{
    /**
     * Apply default coversion for the given value depending on the oid
     * and the character range of the value.
     * 
     * @param oid the object identifier for the DN entry
     * @param value the value associated with it
     * @return the ASN.1 equivalent for the string value.
     */
    public DERObject getConvertedValue(
        DERObjectIdentifier  oid,
        String               value)
    {
        if (value.length() != 0 && value.charAt(0) == '#')
        {
            try
            {
                return convertHexEncoded(value, 1);
            }
            catch (IOException e)
            {
                throw new RuntimeException("can't recode value for oid " + oid.getId());
            }
        }
        else
        {
            if (value.length() != 0 && value.charAt(0) == '\\')
            {
                value = value.substring(1);
            }
            if (oid.equals(X509Name.EmailAddress) || oid.equals(X509Name.DC))
            {
                return new DERIA5String(value);
            }
            else if (oid.equals(X509Name.DATE_OF_BIRTH))  // accept time string as well as # (for compatibility)
            {
                return new DERGeneralizedTime(value);
            }
            else if (oid.equals(X509Name.C) || oid.equals(X509Name.SN) || oid.equals(X509Name.DN_QUALIFIER)
                || oid.equals(X509Name.TELEPHONE_NUMBER))
            {
                 return new DERPrintableString(value);
            }
        }
        
        return new DERUTF8String(value);
    }
}
