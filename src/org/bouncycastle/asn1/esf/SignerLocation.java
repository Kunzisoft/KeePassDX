package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;

import java.util.Enumeration;

/**
 * Signer-Location attribute (RFC3126).
 * 
 * <pre>
 *   SignerLocation ::= SEQUENCE {
 *       countryName        [0] DirectoryString OPTIONAL,
 *       localityName       [1] DirectoryString OPTIONAL,
 *       postalAddress      [2] PostalAddress OPTIONAL }
 *
 *   PostalAddress ::= SEQUENCE SIZE(1..6) OF DirectoryString
 * </pre>
 */
@SuppressWarnings("unchecked")
public class SignerLocation
    extends ASN1Encodable 
{
    private DERUTF8String   countryName;
    private DERUTF8String   localityName;
    private ASN1Sequence    postalAddress;
    
    public SignerLocation(
        ASN1Sequence seq)
    {
        Enumeration     e = seq.getObjects();

        while (e.hasMoreElements())
        {
            DERTaggedObject o = (DERTaggedObject)e.nextElement();

            switch (o.getTagNo())
            {
            case 0:
                this.countryName = DERUTF8String.getInstance(o, true);
                break;
            case 1:
                this.localityName = DERUTF8String.getInstance(o, true);
                break;
            case 2:
                if (o.isExplicit())
                {
                    this.postalAddress = ASN1Sequence.getInstance(o, true);
                }
                else    // handle erroneous implicitly tagged sequences
                {
                    this.postalAddress = ASN1Sequence.getInstance(o, false);
                }
                if (postalAddress != null && postalAddress.size() > 6)
                {
                    throw new IllegalArgumentException("postal address must contain less than 6 strings");
                }
                break;
            default:
                throw new IllegalArgumentException("illegal tag");
            }
        }
    }

    public SignerLocation(
        DERUTF8String   countryName,
        DERUTF8String   localityName,
        ASN1Sequence    postalAddress)
    {
        if (postalAddress != null && postalAddress.size() > 6)
        {
            throw new IllegalArgumentException("postal address must contain less than 6 strings");
        }

        if (countryName != null)
        {
            this.countryName = DERUTF8String.getInstance(countryName.toASN1Object());
        }

        if (localityName != null)
        {
            this.localityName = DERUTF8String.getInstance(localityName.toASN1Object());
        }

        if (postalAddress != null)
        {
            this.postalAddress = ASN1Sequence.getInstance(postalAddress.toASN1Object());
        }
    }

    public static SignerLocation getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SignerLocation)
        {
            return (SignerLocation)obj;
        }

        return new SignerLocation(ASN1Sequence.getInstance(obj));
    }

    public DERUTF8String getCountryName()
    {
        return countryName;
    }

    public DERUTF8String getLocalityName()
    {
        return localityName;
    }

    public ASN1Sequence getPostalAddress()
    {
        return postalAddress;
    }

    /**
     * <pre>
     *   SignerLocation ::= SEQUENCE {
     *       countryName        [0] DirectoryString OPTIONAL,
     *       localityName       [1] DirectoryString OPTIONAL,
     *       postalAddress      [2] PostalAddress OPTIONAL }
     *
     *   PostalAddress ::= SEQUENCE SIZE(1..6) OF DirectoryString
     *   
     *   DirectoryString ::= CHOICE {
     *         teletexString           TeletexString (SIZE (1..MAX)),
     *         printableString         PrintableString (SIZE (1..MAX)),
     *         universalString         UniversalString (SIZE (1..MAX)),
     *         utf8String              UTF8String (SIZE (1.. MAX)),
     *         bmpString               BMPString (SIZE (1..MAX)) }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (countryName != null)
        {
            v.add(new DERTaggedObject(true, 0, countryName));
        }

        if (localityName != null)
        {
            v.add(new DERTaggedObject(true, 1, localityName));
        }

        if (postalAddress != null)
        {
            v.add(new DERTaggedObject(true, 2, postalAddress));
        }

        return new DERSequence(v);
    }
}