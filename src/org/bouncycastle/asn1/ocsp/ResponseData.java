package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.X509Extensions;

public class ResponseData
    extends ASN1Encodable
{
    private static final DERInteger V1 = new DERInteger(0);
    
    private boolean             versionPresent;
    
    private DERInteger          version;
    private ResponderID         responderID;
    private DERGeneralizedTime  producedAt;
    private ASN1Sequence        responses;
    private X509Extensions      responseExtensions;

    public ResponseData(
        DERInteger          version,
        ResponderID         responderID,
        DERGeneralizedTime  producedAt,
        ASN1Sequence        responses,
        X509Extensions      responseExtensions)
    {
        this.version = version;
        this.responderID = responderID;
        this.producedAt = producedAt;
        this.responses = responses;
        this.responseExtensions = responseExtensions;
    }
    
    public ResponseData(
        ResponderID         responderID,
        DERGeneralizedTime  producedAt,
        ASN1Sequence        responses,
        X509Extensions      responseExtensions)
    {
        this(V1, responderID, producedAt, responses, responseExtensions);
    }
    
    public ResponseData(
        ASN1Sequence    seq)
    {
        int index = 0;

        if (seq.getObjectAt(0) instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)seq.getObjectAt(0);

            if (o.getTagNo() == 0)
            {
                this.versionPresent = true;
                this.version = DERInteger.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(0), true);
                index++;
            }
            else
            {
                this.version = V1;
            }
        }
        else
        {
            this.version = V1;
        }

        this.responderID = ResponderID.getInstance(seq.getObjectAt(index++));
        this.producedAt = (DERGeneralizedTime)seq.getObjectAt(index++);
        this.responses = (ASN1Sequence)seq.getObjectAt(index++);

        if (seq.size() > index)
        {
            this.responseExtensions = X509Extensions.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(index), true);
        }
    }

    public static ResponseData getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ResponseData getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ResponseData)
        {
            return (ResponseData)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new ResponseData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public ResponderID getResponderID()
    {
        return responderID;
    }

    public DERGeneralizedTime getProducedAt()
    {
        return producedAt;
    }

    public ASN1Sequence getResponses()
    {
        return responses;
    }

    public X509Extensions getResponseExtensions()
    {
        return responseExtensions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * ResponseData ::= SEQUENCE {
     *     version              [0] EXPLICIT Version DEFAULT v1,
     *     responderID              ResponderID,
     *     producedAt               GeneralizedTime,
     *     responses                SEQUENCE OF SingleResponse,
     *     responseExtensions   [1] EXPLICIT Extensions OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (versionPresent || !version.equals(V1))
        {
            v.add(new DERTaggedObject(true, 0, version));
        }

        v.add(responderID);
        v.add(producedAt);
        v.add(responses);
        if (responseExtensions != null)
        {
            v.add(new DERTaggedObject(true, 1, responseExtensions));
        }

        return new DERSequence(v);
    }
}
