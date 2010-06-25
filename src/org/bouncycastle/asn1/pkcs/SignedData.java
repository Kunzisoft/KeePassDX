package org.bouncycastle.asn1.pkcs;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * a PKCS#7 signed data object.
 */
@SuppressWarnings("unchecked")
public class SignedData
    extends ASN1Encodable
    implements PKCSObjectIdentifiers
{
    private DERInteger              version;
    private ASN1Set                 digestAlgorithms;
    private ContentInfo             contentInfo;
    private ASN1Set                 certificates;
    private ASN1Set                 crls;
    private ASN1Set                 signerInfos;

    public static SignedData getInstance(
        Object  o)
    {
        if (o instanceof SignedData)
        {
            return (SignedData)o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new SignedData((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o);
    }

    public SignedData(
        DERInteger        _version,
        ASN1Set           _digestAlgorithms,
        ContentInfo       _contentInfo,
        ASN1Set           _certificates,
        ASN1Set           _crls,
        ASN1Set           _signerInfos)
    {
        version          = _version;
        digestAlgorithms = _digestAlgorithms;
        contentInfo      = _contentInfo;
        certificates     = _certificates;
        crls             = _crls;
        signerInfos      = _signerInfos;
    }

    public SignedData(
        ASN1Sequence seq)
    {
        Enumeration     e = seq.getObjects();

        version = (DERInteger)e.nextElement();
        digestAlgorithms = ((ASN1Set)e.nextElement());
        contentInfo = ContentInfo.getInstance(e.nextElement());

        while (e.hasMoreElements())
        {
            DERObject o = (DERObject)e.nextElement();

            //
            // an interesting feature of SignedData is that there appear to be varying implementations...
            // for the moment we ignore anything which doesn't fit.
            //
            if (o instanceof DERTaggedObject)
            {
                DERTaggedObject tagged = (DERTaggedObject)o;

                switch (tagged.getTagNo())
                {
                case 0:
                    certificates = ASN1Set.getInstance(tagged, false);
                    break;
                case 1:
                    crls = ASN1Set.getInstance(tagged, false);
                    break;
                default:
                    throw new IllegalArgumentException("unknown tag value " + tagged.getTagNo());
                }
            }
            else
            {
                signerInfos = (ASN1Set)o;
            }
        }
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public ASN1Set getDigestAlgorithms()
    {
        return digestAlgorithms;
    }

    public ContentInfo getContentInfo()
    {
        return contentInfo;
    }

    public ASN1Set getCertificates()
    {
        return certificates;
    }

    public ASN1Set getCRLs()
    {
        return crls;
    }

    public ASN1Set getSignerInfos()
    {
        return signerInfos;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  SignedData ::= SEQUENCE {
     *      version Version,
     *      digestAlgorithms DigestAlgorithmIdentifiers,
     *      contentInfo ContentInfo,
     *      certificates
     *          [0] IMPLICIT ExtendedCertificatesAndCertificates
     *                   OPTIONAL,
     *      crls
     *          [1] IMPLICIT CertificateRevocationLists OPTIONAL,
     *      signerInfos SignerInfos }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(version);
        v.add(digestAlgorithms);
        v.add(contentInfo);

        if (certificates != null)
        {
            v.add(new DERTaggedObject(false, 0, certificates));
        }

        if (crls != null)
        {
            v.add(new DERTaggedObject(false, 1, crls));
        }

        v.add(signerInfos);

        return new BERSequence(v);
    }
}
