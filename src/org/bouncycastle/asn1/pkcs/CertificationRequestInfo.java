package org.bouncycastle.asn1.pkcs;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * PKCS10 CertificationRequestInfo object.
 * <pre>
 *  CertificationRequestInfo ::= SEQUENCE {
 *   version             INTEGER { v1(0) } (v1,...),
 *   subject             Name,
 *   subjectPKInfo   SubjectPublicKeyInfo{{ PKInfoAlgorithms }},
 *   attributes          [0] Attributes{{ CRIAttributes }}
 *  }
 *
 *  Attributes { ATTRIBUTE:IOSet } ::= SET OF Attribute{{ IOSet }}
 *
 *  Attribute { ATTRIBUTE:IOSet } ::= SEQUENCE {
 *    type    ATTRIBUTE.&id({IOSet}),
 *    values  SET SIZE(1..MAX) OF ATTRIBUTE.&Type({IOSet}{\@type})
 *  }
 * </pre>
 */
public class CertificationRequestInfo
    extends ASN1Encodable
{
    DERInteger              version = new DERInteger(0);
    X509Name                subject;
    SubjectPublicKeyInfo    subjectPKInfo;
    ASN1Set                 attributes = null;

    public static CertificationRequestInfo getInstance(
        Object  obj)
    {
        if (obj instanceof CertificationRequestInfo)
        {
            return (CertificationRequestInfo)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new CertificationRequestInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public CertificationRequestInfo(
        X509Name                subject,
        SubjectPublicKeyInfo    pkInfo,
        ASN1Set                 attributes)
    {
        this.subject = subject;
        this.subjectPKInfo = pkInfo;
        this.attributes = attributes;

        if ((subject == null) || (version == null) || (subjectPKInfo == null))
        {
            throw new IllegalArgumentException("Not all mandatory fields set in CertificationRequestInfo generator.");
        }
    }

    public CertificationRequestInfo(
        ASN1Sequence  seq)
    {
        version = (DERInteger)seq.getObjectAt(0);

        subject = X509Name.getInstance(seq.getObjectAt(1));
        subjectPKInfo = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(2));

        //
        // some CertificationRequestInfo objects seem to treat this field
        // as optional.
        //
        if (seq.size() > 3)
        {
            DERTaggedObject tagobj = (DERTaggedObject)seq.getObjectAt(3);
            attributes = ASN1Set.getInstance(tagobj, false);
        }

        if ((subject == null) || (version == null) || (subjectPKInfo == null))
        {
            throw new IllegalArgumentException("Not all mandatory fields set in CertificationRequestInfo generator.");
        }
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public X509Name getSubject()
    {
        return subject;
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
    {
        return subjectPKInfo;
    }

    public ASN1Set getAttributes()
    {
        return attributes;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(subject);
        v.add(subjectPKInfo);

        if (attributes != null)
        {
            v.add(new DERTaggedObject(false, 0, attributes));
        }

        return new DERSequence(v);
    }
}
