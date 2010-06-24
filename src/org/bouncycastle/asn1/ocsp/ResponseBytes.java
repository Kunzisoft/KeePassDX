package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class ResponseBytes
    extends ASN1Encodable
{
    DERObjectIdentifier    responseType;
    ASN1OctetString        response;

    public ResponseBytes(
        DERObjectIdentifier responseType,
        ASN1OctetString     response)
    {
        this.responseType = responseType;
        this.response = response;
    }

    public ResponseBytes(
        ASN1Sequence    seq)
    {
        responseType = (DERObjectIdentifier)seq.getObjectAt(0);
        response = (ASN1OctetString)seq.getObjectAt(1);
    }

    public static ResponseBytes getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ResponseBytes getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ResponseBytes)
        {
            return (ResponseBytes)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new ResponseBytes((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public DERObjectIdentifier getResponseType()
    {
        return responseType;
    }

    public ASN1OctetString getResponse()
    {
        return response;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * ResponseBytes ::=       SEQUENCE {
     *     responseType   OBJECT IDENTIFIER,
     *     response       OCTET STRING }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        v.add(responseType);
        v.add(response);

        return new DERSequence(v);
    }
}
