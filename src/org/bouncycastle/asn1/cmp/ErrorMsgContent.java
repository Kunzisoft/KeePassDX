package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class ErrorMsgContent
    extends ASN1Encodable
{
    private PKIStatusInfo pKIStatusInfo;
    private DERInteger errorCode;
    private PKIFreeText errorDetails;

    private ErrorMsgContent(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();

        pKIStatusInfo = PKIStatusInfo.getInstance(en.nextElement());

        while (en.hasMoreElements())
        {
            Object o = en.nextElement();

            if (o instanceof DERInteger)
            {
                errorCode = DERInteger.getInstance(o);
            }
            else
            {
                errorDetails = PKIFreeText.getInstance(o);
            }
        }
    }

    public static ErrorMsgContent getInstance(Object o)
    {
        if (o instanceof ErrorMsgContent)
        {
            return (ErrorMsgContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new ErrorMsgContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public PKIStatusInfo getPKIStatusInfo()
    {
        return pKIStatusInfo;
    }

    public DERInteger getErrorCode()
    {
        return errorCode;
    }

    public PKIFreeText getErrorDetails()
    {
        return errorDetails;
    }

    /**
     * <pre>
     * ErrorMsgContent ::= SEQUENCE {
     *                        pKIStatusInfo          PKIStatusInfo,
     *                        errorCode              INTEGER           OPTIONAL,
     *                        -- implementation-specific error codes
     *                        errorDetails           PKIFreeText       OPTIONAL
     *                        -- implementation-specific error details
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(pKIStatusInfo);
        addOptional(v, errorCode);
        addOptional(v, errorDetails);

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
