package org.bouncycastle.asn1.crmf;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class CertReqMsg
    extends ASN1Encodable
{
    private CertRequest certReq;
    private ProofOfPossession pop;
    private ASN1Sequence regInfo;

    private CertReqMsg(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();

        certReq = CertRequest.getInstance(en.nextElement());
        while (en.hasMoreElements())
        {
            Object o = en.nextElement();

            if (o instanceof ASN1TaggedObject)
            {
                pop = ProofOfPossession.getInstance(o);
            }
            else
            {
                regInfo = ASN1Sequence.getInstance(o);
            }
        }
    }

    public static CertReqMsg getInstance(Object o)
    {
        if (o instanceof CertReqMsg)
        {
            return (CertReqMsg)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CertReqMsg((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public CertRequest getCertReq()
    {
        return certReq;
    }

    public ProofOfPossession getPop()
    {
        return pop;
    }

    public AttributeTypeAndValue[] getRegInfo()
    {
        if (regInfo == null)
        {
            return null;
        }

        AttributeTypeAndValue[] results = new AttributeTypeAndValue[regInfo.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = AttributeTypeAndValue.getInstance(regInfo.getObjectAt(i));
        }

        return results;
    }

    /**
     * <pre>
     * CertReqMsg ::= SEQUENCE {
     *                    certReq   CertRequest,
     *                    pop       ProofOfPossession  OPTIONAL,
     *                    -- content depends upon key type
     *                    regInfo   SEQUENCE SIZE(1..MAX) OF AttributeTypeAndValue OPTIONAL }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(certReq);

        addOptional(v, pop);
        addOptional(v, regInfo);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(obj);
        }
    }
}
