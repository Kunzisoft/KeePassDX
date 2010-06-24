package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;

public class SPuri
{
    private DERIA5String uri;

    public static SPuri getInstance(
        Object obj)
    {
        if (obj instanceof SPuri)
        {
            return (SPuri) obj;
        }
        else if (obj instanceof DERIA5String)
        {
            return new SPuri((DERIA5String) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SPuri' factory: "
                        + obj.getClass().getName() + ".");
    }

    public SPuri(
        DERIA5String uri)
    {
        this.uri = uri;
    }

    public DERIA5String getUri()
    {
        return uri;
    }

    /**
     * <pre>
     * SPuri ::= IA5String
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return uri.getDERObject();
    }
}
