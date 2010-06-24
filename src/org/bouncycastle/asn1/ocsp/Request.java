package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.X509Extensions;

public class Request
    extends ASN1Encodable
{
    CertID            reqCert;
    X509Extensions    singleRequestExtensions;

    public Request(
        CertID          reqCert,
        X509Extensions  singleRequestExtensions)
    {
        this.reqCert = reqCert;
        this.singleRequestExtensions = singleRequestExtensions;
    }

    public Request(
        ASN1Sequence    seq)
    {
        reqCert = CertID.getInstance(seq.getObjectAt(0));

        if (seq.size() == 2)
        {
            singleRequestExtensions = X509Extensions.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }

    public static Request getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static Request getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof Request)
        {
            return (Request)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new Request((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public CertID getReqCert()
    {
        return reqCert;
    }

    public X509Extensions getSingleRequestExtensions()
    {
        return singleRequestExtensions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * Request         ::=     SEQUENCE {
     *     reqCert                     CertID,
     *     singleRequestExtensions     [0] EXPLICIT Extensions OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        v.add(reqCert);

        if (singleRequestExtensions != null)
        {
            v.add(new DERTaggedObject(true, 0, singleRequestExtensions));
        }

        return new DERSequence(v);
    }
}
