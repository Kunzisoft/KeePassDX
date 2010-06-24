package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.x500.DirectoryString;

/**
 * Some other information of non-restrictive nature regarding the usage of this
 * certificate.
 * 
 * <pre>
 *    AdditionalInformationSyntax ::= DirectoryString (SIZE(1..2048))
 * </pre>
 */
public class AdditionalInformationSyntax extends ASN1Encodable
{
    private DirectoryString information;

    public static AdditionalInformationSyntax getInstance(Object obj)
    {
        if (obj == null || obj instanceof AdditionalInformationSyntax)
        {
            return (AdditionalInformationSyntax)obj;
        }

        if (obj instanceof DERString)
        {
            return new AdditionalInformationSyntax(DirectoryString.getInstance(obj));
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    private AdditionalInformationSyntax(DirectoryString information)
    {
        this.information = information;
    }

    /**
     * Constructor from a given details.
     *
     * @param information The describtion of the information.
     */
    public AdditionalInformationSyntax(String information)
    {
        this(new DirectoryString(information));
    }

    public DirectoryString getInformation()
    {
        return information;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *   AdditionalInformationSyntax ::= DirectoryString (SIZE(1..2048))
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        return information.toASN1Object();
    }
}
