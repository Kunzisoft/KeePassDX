package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;

/**
 * Generator for Version 1 TBSCertificateStructures.
 * <pre>
 * TBSCertificate ::= SEQUENCE {
 *      version          [ 0 ]  Version DEFAULT v1(0),
 *      serialNumber            CertificateSerialNumber,
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      validity                Validity,
 *      subject                 Name,
 *      subjectPublicKeyInfo    SubjectPublicKeyInfo,
 *      }
 * </pre>
 *
 */
public class V1TBSCertificateGenerator
{
    DERTaggedObject         version = new DERTaggedObject(0, new DERInteger(0));

    DERInteger              serialNumber;
    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    startDate, endDate;
    X509Name                subject;
    SubjectPublicKeyInfo    subjectPublicKeyInfo;

    public V1TBSCertificateGenerator()
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
        Time startDate)
    {
        this.startDate = startDate;
    }

    public void setStartDate(
        DERUTCTime startDate)
    {
        this.startDate = new Time(startDate);
    }

    public void setEndDate(
        Time endDate)
    {
        this.endDate = endDate;
    }

    public void setEndDate(
        DERUTCTime endDate)
    {
        this.endDate = new Time(endDate);
    }

    public void setSubject(
        X509Name    subject)
    {
        this.subject = subject;
    }

    public void setSubjectPublicKeyInfo(
        SubjectPublicKeyInfo    pubKeyInfo)
    {
        this.subjectPublicKeyInfo = pubKeyInfo;
    }

    public TBSCertificateStructure generateTBSCertificate()
    {
        if ((serialNumber == null) || (signature == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (subject == null) || (subjectPublicKeyInfo == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V1 TBScertificate generator");
        }

        ASN1EncodableVector  seq = new ASN1EncodableVector();

        // seq.add(version); - not required as default value.
        seq.add(serialNumber);
        seq.add(signature);
        seq.add(issuer);

        //
        // before and after dates
        //
        ASN1EncodableVector  validity = new ASN1EncodableVector();

        validity.add(startDate);
        validity.add(endDate);

        seq.add(new DERSequence(validity));

        seq.add(subject);

        seq.add(subjectPublicKeyInfo);

        return new TBSCertificateStructure(new DERSequence(seq));
    }
}
