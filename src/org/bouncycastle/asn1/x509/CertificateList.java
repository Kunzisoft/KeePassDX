
package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;

/**
 * PKIX RFC-2459
 *
 * The X.509 v2 CRL syntax is as follows.  For signature calculation,
 * the data that is to be signed is ASN.1 DER encoded.
 *
 * <pre>
 * CertificateList  ::=  SEQUENCE  {
 *      tbsCertList          TBSCertList,
 *      signatureAlgorithm   AlgorithmIdentifier,
 *      signatureValue       BIT STRING  }
 * </pre>
 */
@SuppressWarnings("unchecked")
public class CertificateList
    extends ASN1Encodable
{
    TBSCertList            tbsCertList;
    AlgorithmIdentifier    sigAlgId;
    DERBitString           sig;

    public static CertificateList getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static CertificateList getInstance(
        Object  obj)
    {
        if (obj instanceof CertificateList)
        {
            return (CertificateList)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new CertificateList((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public CertificateList(
        ASN1Sequence seq)
    {
        if (seq.size() == 3)
        {
            tbsCertList = TBSCertList.getInstance(seq.getObjectAt(0));
            sigAlgId = AlgorithmIdentifier.getInstance(seq.getObjectAt(1));
            sig = DERBitString.getInstance(seq.getObjectAt(2));
        }
        else
        {
            throw new IllegalArgumentException("sequence wrong size for CertificateList");
        }
    }

    public TBSCertList getTBSCertList()
    {
        return tbsCertList;
    }

    public TBSCertList.CRLEntry[] getRevokedCertificates()
    {
        return tbsCertList.getRevokedCertificates();
    }

    public Enumeration getRevokedCertificateEnumeration()
    {
        return tbsCertList.getRevokedCertificateEnumeration();
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return sigAlgId;
    }

    public DERBitString getSignature()
    {
        return sig;
    }

    public int getVersion()
    {
        return tbsCertList.getVersion();
    }

    public X509Name getIssuer()
    {
        return tbsCertList.getIssuer();
    }

    public Time getThisUpdate()
    {
        return tbsCertList.getThisUpdate();
    }

    public Time getNextUpdate()
    {
        return tbsCertList.getNextUpdate();
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCertList);
        v.add(sigAlgId);
        v.add(sig);

        return new DERSequence(v);
    }
}
