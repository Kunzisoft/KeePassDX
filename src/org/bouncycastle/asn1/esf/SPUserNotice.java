package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.x509.NoticeReference;
import org.bouncycastle.asn1.x509.DisplayText;
import org.bouncycastle.asn1.*;

import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class SPUserNotice
{
    private NoticeReference noticeRef;
    private DisplayText     explicitText;

    public static SPUserNotice getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SPUserNotice)
        {
            return (SPUserNotice) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SPUserNotice((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SPUserNotice' factory : "
                        + obj.getClass().getName() + ".");
    }

    public SPUserNotice(
        ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            DEREncodable object = (DEREncodable) e.nextElement();
            if (object instanceof NoticeReference)
            {
                noticeRef = NoticeReference.getInstance(object);
            }
            else if (object instanceof DisplayText)
            {
                explicitText = DisplayText.getInstance(object);
            }
            else
            {
                throw new IllegalArgumentException("Invalid element in 'SPUserNotice'.");
            }
        }
    }

    public SPUserNotice(
        NoticeReference noticeRef,
        DisplayText     explicitText)
    {
        this.noticeRef = noticeRef;
        this.explicitText = explicitText;
    }

    public NoticeReference getNoticeRef()
    {
        return noticeRef;
    }

    public DisplayText getExplicitText()
    {
        return explicitText;
    }

    /**
     * <pre>
     * SPUserNotice ::= SEQUENCE {
     *     noticeRef NoticeReference OPTIONAL,
     *     explicitText DisplayText OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (noticeRef != null)
        {
            v.add(noticeRef);
        }

        if (explicitText != null)
        {
            v.add(explicitText);
        }

        return new DERSequence(v);
    }
}
