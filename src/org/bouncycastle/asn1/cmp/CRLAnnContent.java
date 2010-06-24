package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.x509.CertificateList;

public class CRLAnnContent
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private CRLAnnContent(ASN1Sequence seq)
    {
        content = seq;
    }

    public static CRLAnnContent getInstance(Object o)
    {
        if (o instanceof CRLAnnContent)
        {
            return (CRLAnnContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CRLAnnContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public CertificateList[] toCertificateListArray()
    {
        CertificateList[] result = new CertificateList[content.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = CertificateList.getInstance(content.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * CRLAnnContent ::= SEQUENCE OF CertificateList
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
