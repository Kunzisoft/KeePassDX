package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEREnumerated;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class OCSPResponse
    extends ASN1Encodable
{
    OCSPResponseStatus    responseStatus;
    ResponseBytes        responseBytes;

    public OCSPResponse(
        OCSPResponseStatus  responseStatus,
        ResponseBytes       responseBytes)
    {
        this.responseStatus = responseStatus;
        this.responseBytes = responseBytes;
    }

    public OCSPResponse(
        ASN1Sequence    seq)
    {
        responseStatus = new OCSPResponseStatus(
                            DEREnumerated.getInstance(seq.getObjectAt(0)));

        if (seq.size() == 2)
        {
            responseBytes = ResponseBytes.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }

    public static OCSPResponse getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static OCSPResponse getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof OCSPResponse)
        {
            return (OCSPResponse)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new OCSPResponse((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public OCSPResponseStatus getResponseStatus()
    {
        return responseStatus;
    }

    public ResponseBytes getResponseBytes()
    {
        return responseBytes;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * OCSPResponse ::= SEQUENCE {
     *     responseStatus         OCSPResponseStatus,
     *     responseBytes          [0] EXPLICIT ResponseBytes OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        v.add(responseStatus);

        if (responseBytes != null)
        {
            v.add(new DERTaggedObject(true, 0, responseBytes));
        }

        return new DERSequence(v);
    }
}
