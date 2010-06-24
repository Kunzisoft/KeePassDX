package org.bouncycastle.asn1.misc;

import org.bouncycastle.asn1.*;

/**
 * The NetscapeCertType object.
 * <pre>
 *    NetscapeCertType ::= BIT STRING {
 *         SSLClient               (0),
 *         SSLServer               (1),
 *         S/MIME                  (2),
 *         Object Signing          (3),
 *         Reserved                (4),
 *         SSL CA                  (5),
 *         S/MIME CA               (6),
 *         Object Signing CA       (7) }
 * </pre>
 */
public class NetscapeCertType
    extends DERBitString
{
    public static final int        sslClient        = (1 << 7); 
    public static final int        sslServer        = (1 << 6);
    public static final int        smime            = (1 << 5);
    public static final int        objectSigning    = (1 << 4);
    public static final int        reserved         = (1 << 3);
    public static final int        sslCA            = (1 << 2);
    public static final int        smimeCA          = (1 << 1);
    public static final int        objectSigningCA  = (1 << 0);

    /**
     * Basic constructor.
     * 
     * @param usage - the bitwise OR of the Key Usage flags giving the
     * allowed uses for the key.
     * e.g. (X509NetscapeCertType.sslCA | X509NetscapeCertType.smimeCA)
     */
    public NetscapeCertType(
        int usage)
    {
        super(getBytes(usage), getPadBits(usage));
    }

    public NetscapeCertType(
        DERBitString usage)
    {
        super(usage.getBytes(), usage.getPadBits());
    }

    public String toString()
    {
        return "NetscapeCertType: 0x" + Integer.toHexString(data[0] & 0xff);
    }
}
