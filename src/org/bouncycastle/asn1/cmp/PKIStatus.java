package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;

public class PKIStatus
    extends ASN1Encodable
{
    public static final int GRANTED                 = 0;
    public static final int GRANTED_WITH_MODS       = 1;
    public static final int REJECTION               = 2;
    public static final int WAITING                 = 3;
    public static final int REVOCATION_WARNING      = 4;
    public static final int REVOCATION_NOTIFICATION = 5;
    public static final int KEY_UPDATE_WARNING      = 6;

    public static final PKIStatus granted = new PKIStatus(GRANTED);
    public static final PKIStatus grantedWithMods = new PKIStatus(GRANTED_WITH_MODS);
    public static final PKIStatus rejection = new PKIStatus(REJECTION);
    public static final PKIStatus waiting = new PKIStatus(WAITING);
    public static final PKIStatus revocationWarning = new PKIStatus(REVOCATION_WARNING);
    public static final PKIStatus revocationNotification = new PKIStatus(REVOCATION_NOTIFICATION);
    public static final PKIStatus keyUpdateWaiting = new PKIStatus(KEY_UPDATE_WARNING);

    private DERInteger value;

    private PKIStatus(int value)
    {
        this(new DERInteger(value));
    }

    private PKIStatus(DERInteger value)
    {
        this.value = value;
    }

    public static PKIStatus getInstance(Object o)
    {
        if (o instanceof PKIStatus)
        {
            return (PKIStatus)o;
        }

        if (o instanceof DERInteger)
        {
            return new PKIStatus((DERInteger)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public DERObject toASN1Object()
    {
        return value;
    }
}
