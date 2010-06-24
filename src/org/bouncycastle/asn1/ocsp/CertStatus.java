package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class CertStatus
    extends ASN1Encodable
    implements ASN1Choice
{
    private int             tagNo;
    private DEREncodable    value;

    /**
     * create a CertStatus object with a tag of zero.
     */
    public CertStatus()
    {
        tagNo = 0;
        value = new DERNull();
    }

    public CertStatus(
        RevokedInfo info)
    {
        tagNo = 1;
        value = info;
    }

    public CertStatus(
        int tagNo,
        DEREncodable    value)
    {
        this.tagNo = tagNo;
        this.value = value;
    }

    public CertStatus(
        ASN1TaggedObject    choice)
    {
        this.tagNo = choice.getTagNo();

        switch (choice.getTagNo())
        {
        case 0:
            value = new DERNull();
            break;
        case 1:
            value = RevokedInfo.getInstance(choice, false);
            break;
        case 2:
            value = new DERNull();
        }
    }

    public static CertStatus getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof CertStatus)
        {
            return (CertStatus)obj;
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new CertStatus((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public static CertStatus getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject()); // must be explicitly tagged
    }
    
    public int getTagNo()
    {
        return tagNo;
    }

    public DEREncodable getStatus()
    {
        return value;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  CertStatus ::= CHOICE {
     *                  good        [0]     IMPLICIT NULL,
     *                  revoked     [1]     IMPLICIT RevokedInfo,
     *                  unknown     [2]     IMPLICIT UnknownInfo }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return new DERTaggedObject(false, tagNo, value);
    }
}
