package org.bouncycastle.asn1.crmf;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;

import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class CertTemplate
    extends ASN1Encodable
{
    private DERInteger version;
    private DERInteger serialNumber;
    private AlgorithmIdentifier signingAlg;
    private X509Name issuer;
    private OptionalValidity validity;
    private X509Name subject;
    private SubjectPublicKeyInfo publicKey;
    private DERBitString issuerUID;
    private DERBitString subjectUID;
    private X509Extensions extensions;

    private CertTemplate(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();
        while (en.hasMoreElements())
        {
            ASN1TaggedObject tObj = (ASN1TaggedObject)en.nextElement();

            switch (tObj.getTagNo())
            {
            case 0:
                version = DERInteger.getInstance(tObj, false);
                break;
            case 1:
                serialNumber = DERInteger.getInstance(tObj, false);
                break;
            case 2:
                signingAlg = AlgorithmIdentifier.getInstance(tObj, false);
                break;
            case 3:
                issuer = X509Name.getInstance(tObj, true); // CHOICE
                break;
            case 4:
                validity = OptionalValidity.getInstance(ASN1Sequence.getInstance(tObj, false));
                break;
            case 5:
                subject = X509Name.getInstance(tObj, true); // CHOICE
                break;
            case 6:
                publicKey = SubjectPublicKeyInfo.getInstance(tObj, false);
                break;
            case 7:
                issuerUID = DERBitString.getInstance(tObj, false);
                break;
            case 8:
                subjectUID = DERBitString.getInstance(tObj, false);
                break;
            case 9:
                extensions = X509Extensions.getInstance(tObj, false);
                break;
            default:
                throw new IllegalArgumentException("unknown tag: " + tObj.getTagNo());
            }
        }
    }

    public static CertTemplate getInstance(Object o)
    {
        if (o instanceof CertTemplate)
        {
            return (CertTemplate)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CertTemplate((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    /**
     * <pre>
     *  CertTemplate ::= SEQUENCE {
     *      version      [0] Version               OPTIONAL,
     *      serialNumber [1] INTEGER               OPTIONAL,
     *      signingAlg   [2] AlgorithmIdentifier   OPTIONAL,
     *      issuer       [3] Name                  OPTIONAL,
     *      validity     [4] OptionalValidity      OPTIONAL,
     *      subject      [5] Name                  OPTIONAL,
     *      publicKey    [6] SubjectPublicKeyInfo  OPTIONAL,
     *      issuerUID    [7] UniqueIdentifier      OPTIONAL,
     *      subjectUID   [8] UniqueIdentifier      OPTIONAL,
     *      extensions   [9] Extensions            OPTIONAL }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        addOptional(v, 0, false, version);
        addOptional(v, 1, false, serialNumber);
        addOptional(v, 2, false, signingAlg);
        addOptional(v, 3, true, issuer); // CHOICE
        addOptional(v, 4, false, validity);
        addOptional(v, 5, true, subject); // CHOICE
        addOptional(v, 6, false, publicKey);
        addOptional(v, 7, false, issuerUID);
        addOptional(v, 8, false, subjectUID);
        addOptional(v, 9, false, extensions);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, int tagNo, boolean isExplicit, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(new DERTaggedObject(isExplicit, tagNo, obj));
        }
    }
}
