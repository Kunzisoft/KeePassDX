package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DERBitString;

/**
 * The ReasonFlags object.
 * <pre>
 * ReasonFlags ::= BIT STRING {
 *      unused                  (0),
 *      keyCompromise           (1),
 *      cACompromise            (2),
 *      affiliationChanged      (3),
 *      superseded              (4),
 *      cessationOfOperation    (5),
 *      certificateHold         (6),
 *      privilegeWithdrawn      (7),
 *      aACompromise            (8) }
 * </pre>
 */
public class ReasonFlags
    extends DERBitString
{
    /**
     * @deprecated use lower case version
     */
    public static final int UNUSED                  = (1 << 7);
    /**
     * @deprecated use lower case version
     */
    public static final int KEY_COMPROMISE          = (1 << 6);
    /**
     * @deprecated use lower case version
     */
    public static final int CA_COMPROMISE           = (1 << 5);
    /**
     * @deprecated use lower case version
     */
    public static final int AFFILIATION_CHANGED     = (1 << 4);
    /**
     * @deprecated use lower case version
     */
    public static final int SUPERSEDED              = (1 << 3);
    /**
     * @deprecated use lower case version
     */
    public static final int CESSATION_OF_OPERATION  = (1 << 2);
    /**
     * @deprecated use lower case version
     */
    public static final int CERTIFICATE_HOLD        = (1 << 1);
    /**
     * @deprecated use lower case version
     */
    public static final int PRIVILEGE_WITHDRAWN     = (1 << 0);
    /**
     * @deprecated use lower case version
     */
    public static final int AA_COMPROMISE           = (1 << 15);
    
    public static final int unused                  = (1 << 7);
    public static final int keyCompromise           = (1 << 6);
    public static final int cACompromise            = (1 << 5);
    public static final int affiliationChanged      = (1 << 4);
    public static final int superseded              = (1 << 3);
    public static final int cessationOfOperation    = (1 << 2);
    public static final int certificateHold         = (1 << 1);
    public static final int privilegeWithdrawn      = (1 << 0);
    public static final int aACompromise            = (1 << 15);

    /**
     * @param reasons - the bitwise OR of the Key Reason flags giving the
     * allowed uses for the key.
     */
    public ReasonFlags(
        int reasons)
    {
        super(getBytes(reasons), getPadBits(reasons));
    }

    public ReasonFlags(
        DERBitString reasons)
    {
        super(reasons.getBytes(), reasons.getPadBits());
    }
}
