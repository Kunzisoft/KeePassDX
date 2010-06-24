package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.util.Strings;

import java.io.IOException;

/**
 * It turns out that the number of standard ways the fields in a DN should be 
 * encoded into their ASN.1 counterparts is rapidly approaching the
 * number of machines on the internet. By default the X509Name class 
 * will produce UTF8Strings in line with the current recommendations (RFC 3280).
 * <p>
 * An example of an encoder look like below:
 * <pre>
 * public class X509DirEntryConverter
 *     extends X509NameEntryConverter
 * {
 *     public DERObject getConvertedValue(
 *         DERObjectIdentifier  oid,
 *         String               value)
 *     {
 *         if (str.length() != 0 && str.charAt(0) == '#')
 *         {
 *             return convertHexEncoded(str, 1);
 *         }
 *         if (oid.equals(EmailAddress))
 *         {
 *             return new DERIA5String(str);
 *         }
 *         else if (canBePrintable(str))
 *         {
 *             return new DERPrintableString(str);
 *         }
 *         else if (canBeUTF8(str))
 *         {
 *             return new DERUTF8String(str);
 *         }
 *         else
 *         {
 *             return new DERBMPString(str);
 *         }
 *     }
 * }
 */
public abstract class X509NameEntryConverter
{
    /**
     * Convert an inline encoded hex string rendition of an ASN.1
     * object back into its corresponding ASN.1 object.
     * 
     * @param str the hex encoded object
     * @param off the index at which the encoding starts
     * @return the decoded object
     */
    protected DERObject convertHexEncoded(
        String  str,
        int     off)
        throws IOException
    {
        str = Strings.toLowerCase(str);
        byte[] data = new byte[(str.length() - off) / 2];
        for (int index = 0; index != data.length; index++)
        {
            char left = str.charAt((index * 2) + off);
            char right = str.charAt((index * 2) + off + 1);
            
            if (left < 'a')
            {
                data[index] = (byte)((left - '0') << 4);
            }
            else
            {
                data[index] = (byte)((left - 'a' + 10) << 4);
            }
            if (right < 'a')
            {
                data[index] |= (byte)(right - '0');
            }
            else
            {
                data[index] |= (byte)(right - 'a' + 10);
            }
        }

        ASN1InputStream aIn = new ASN1InputStream(data);
                                            
        return aIn.readObject();
    }
    
    /**
     * return true if the passed in String can be represented without
     * loss as a PrintableString, false otherwise.
     */
    protected boolean canBePrintable(
        String  str)
    {
        return DERPrintableString.isPrintableString(str);
    }
    
    /**
     * Convert the passed in String value into the appropriate ASN.1
     * encoded object.
     * 
     * @param oid the oid associated with the value in the DN.
     * @param value the value of the particular DN component.
     * @return the ASN.1 equivalent for the value.
     */
    public abstract DERObject getConvertedValue(DERObjectIdentifier oid, String value);
}
