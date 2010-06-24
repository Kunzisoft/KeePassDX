package org.bouncycastle.asn1.tsp;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;


@SuppressWarnings("unchecked")
public class TimeStampResp
    extends ASN1Encodable
{
    PKIStatusInfo pkiStatusInfo;

    ContentInfo timeStampToken;

    public static TimeStampResp getInstance(Object o)
    {
        if (o == null || o instanceof TimeStampResp)
        {
            return (TimeStampResp) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new TimeStampResp((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'TimeStampResp' factory : "
                        + o.getClass().getName() + ".");
    }

    public TimeStampResp(ASN1Sequence seq)
    {

        Enumeration e = seq.getObjects();

        // status
        pkiStatusInfo = PKIStatusInfo.getInstance(e.nextElement());

        if (e.hasMoreElements())
        {
            timeStampToken = ContentInfo.getInstance(e.nextElement());
        }
    }

    public TimeStampResp(PKIStatusInfo pkiStatusInfo, ContentInfo timeStampToken)
    {
        this.pkiStatusInfo = pkiStatusInfo;
        this.timeStampToken = timeStampToken;
    }

    public PKIStatusInfo getStatus()
    {
        return pkiStatusInfo;
    }

    public ContentInfo getTimeStampToken()
    {
        return timeStampToken;
    }

    /**
     * <pre>
     * TimeStampResp ::= SEQUENCE  {
     *   status                  PKIStatusInfo,
     *   timeStampToken          TimeStampToken     OPTIONAL  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(pkiStatusInfo);
        if (timeStampToken != null)
        {
            v.add(timeStampToken);
        }

        return new DERSequence(v);
    }
}
