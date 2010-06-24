package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREnumerated;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.CRLReason;

public class RevokedInfo
    extends ASN1Encodable
{
    private DERGeneralizedTime  revocationTime;
    private CRLReason           revocationReason;

    public RevokedInfo(
        DERGeneralizedTime  revocationTime,
        CRLReason           revocationReason)
    {
        this.revocationTime = revocationTime;
        this.revocationReason = revocationReason;
    }

    public RevokedInfo(
        ASN1Sequence    seq)
    {
        this.revocationTime = (DERGeneralizedTime)seq.getObjectAt(0);

        if (seq.size() > 1)
        {
            this.revocationReason = new CRLReason(DEREnumerated.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true));
        }
    }

    public static RevokedInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static RevokedInfo getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof RevokedInfo)
        {
            return (RevokedInfo)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new RevokedInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public DERGeneralizedTime getRevocationTime()
    {
        return revocationTime;
    }

    public CRLReason getRevocationReason()
    {
        return revocationReason;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * RevokedInfo ::= SEQUENCE {
     *      revocationTime              GeneralizedTime,
     *      revocationReason    [0]     EXPLICIT CRLReason OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(revocationTime);
        if (revocationReason != null)
        {
            v.add(new DERTaggedObject(true, 0, revocationReason));
        }

        return new DERSequence(v);
    }
}
