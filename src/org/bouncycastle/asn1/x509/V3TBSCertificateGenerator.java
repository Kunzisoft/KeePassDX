package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;

/**
 * Generator for Version 3 TBSCertificateStructures.
 * <pre>
 * TBSCertificate ::= SEQUENCE {
 *      version          [ 0 ]  Version DEFAULT v1(0),
 *      serialNumber            CertificateSerialNumber,
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      validity                Validity,
 *      subject                 Name,
 *      subjectPublicKeyInfo    SubjectPublicKeyInfo,
 *      issuerUniqueID    [ 1 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      subjectUniqueID   [ 2 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      extensions        [ 3 ] Extensions OPTIONAL
 *      }
 * </pre>
 *
 */
public class V3TBSCertificateGenerator
{
    DERTaggedObject         version = new DERTaggedObject(0, new DERInteger(2));

    DERInteger              serialNumber;
    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    startDate, endDate;
    X509Name                subject;
    SubjectPublicKeyInfo    subjectPublicKeyInfo;
    X509Extensions          extensions;

    private boolean altNamePresentAndCritical;
    private DERBitString issuerUniqueID;
    private DERBitString subjectUniqueID;

    public V3TBSCertificateGenerator()
    {
    }

    public void setSerialNumber(
        DERInteger  serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

    public void setIssuer(
        X509Name    issuer)
    {
        this.issuer = issuer;
    }

    public void setStartDate(
        DERUTCTime startDate)
    {
        this.startDate = new Time(startDate);
    }

    public void setStartDate(
        Time startDate)
    {
        this.startDate = startDate;
    }

    public void setEndDate(
        DERUTCTime endDate)
    {
        this.endDate = new Time(endDate);
    }

    public void setEndDate(
        Time endDate)
    {
        this.endDate = endDate;
    }

    public void setSubject(
        X509Name    subject)
    {
        this.subject = subject;
    }

    public void setIssuerUniqueID(
        DERBitString uniqueID)
    {
        this.issuerUniqueID = uniqueID;
    }

    public void setSubjectUniqueID(
        DERBitString uniqueID)
    {
        this.subjectUniqueID = uniqueID;
    }

    public void setSubjectPublicKeyInfo(
        SubjectPublicKeyInfo    pubKeyInfo)
    {
        this.subjectPublicKeyInfo = pubKeyInfo;
    }

    public void setExtensions(
        X509Extensions    extensions)
    {
        this.extensions = extensions;
        if (extensions != null)
        {
            X509Extension altName = extensions.getExtension(X509Extensions.SubjectAlternativeName);

            if (altName != null && altName.isCritical())
            {
                altNamePresentAndCritical = true;
            }
        }
    }

    public TBSCertificateStructure generateTBSCertificate()
    {
        if ((serialNumber == null) || (signature == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (subject == null && !altNamePresentAndCritical) || (subjectPublicKeyInfo == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V3 TBScertificate generator");
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(serialNumber);
        v.add(signature);
        v.add(issuer);

        //
        // before and after dates
        //
        ASN1EncodableVector  validity = new ASN1EncodableVector();

        validity.add(startDate);
        validity.add(endDate);

        v.add(new DERSequence(validity));

        if (subject != null)
        {
            v.add(subject);
        }
        else
        {
            v.add(new DERSequence());
        }

        v.add(subjectPublicKeyInfo);

        if (issuerUniqueID != null)
        {
            v.add(new DERTaggedObject(false, 1, issuerUniqueID));
        }

        if (subjectUniqueID != null)
        {
            v.add(new DERTaggedObject(false, 2, subjectUniqueID));
        }

        if (extensions != null)
        {
            v.add(new DERTaggedObject(3, extensions));
        }

        return new TBSCertificateStructure(new DERSequence(v));
    }
}
