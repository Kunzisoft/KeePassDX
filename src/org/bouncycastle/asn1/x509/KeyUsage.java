package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DERBitString;

/**
 * The KeyUsage object.
 * <pre>
 *    id-ce-keyUsage OBJECT IDENTIFIER ::=  { id-ce 15 }
 *
 *    KeyUsage ::= BIT STRING {
 *         digitalSignature        (0),
 *         nonRepudiation          (1),
 *         keyEncipherment         (2),
 *         dataEncipherment        (3),
 *         keyAgreement            (4),
 *         keyCertSign             (5),
 *         cRLSign                 (6),
 *         encipherOnly            (7),
 *         decipherOnly            (8) }
 * </pre>
 */
public class KeyUsage
    extends DERBitString
{
    public static final int        digitalSignature = (1 << 7); 
    public static final int        nonRepudiation   = (1 << 6);
    public static final int        keyEncipherment  = (1 << 5);
    public static final int        dataEncipherment = (1 << 4);
    public static final int        keyAgreement     = (1 << 3);
    public static final int        keyCertSign      = (1 << 2);
    public static final int        cRLSign          = (1 << 1);
    public static final int        encipherOnly     = (1 << 0);
    public static final int        decipherOnly     = (1 << 15);

    public static DERBitString getInstance(Object obj)   // needs to be DERBitString for other VMs
    {
        if (obj instanceof KeyUsage)
        {
            return (KeyUsage)obj;
        }

        if (obj instanceof X509Extension)
        {
            return new KeyUsage(DERBitString.getInstance(X509Extension.convertValueToObject((X509Extension)obj)));
        }

        return new KeyUsage(DERBitString.getInstance(obj));
    }
    
    /**
     * Basic constructor.
     * 
     * @param usage - the bitwise OR of the Key Usage flags giving the
     * allowed uses for the key.
     * e.g. (KeyUsage.keyEncipherment | KeyUsage.dataEncipherment)
     */
    public KeyUsage(
        int usage)
    {
        super(getBytes(usage), getPadBits(usage));
    }

    public KeyUsage(
        DERBitString usage)
    {
        super(usage.getBytes(), usage.getPadBits());
    }

    public String toString()
    {
        if (data.length == 1)
        {
            return "KeyUsage: 0x" + Integer.toHexString(data[0] & 0xff);
        }
        return "KeyUsage: 0x" + Integer.toHexString((data[1] & 0xff) << 8 | (data[0] & 0xff));
    }
}
