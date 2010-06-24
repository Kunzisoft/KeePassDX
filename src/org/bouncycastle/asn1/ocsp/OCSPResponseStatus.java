package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.DEREnumerated;

public class OCSPResponseStatus
    extends DEREnumerated
{
    public static final int SUCCESSFUL = 0;
    public static final int MALFORMED_REQUEST = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int TRY_LATER = 3;
    public static final int SIG_REQUIRED = 5;
    public static final int UNAUTHORIZED = 6;

    /**
     * The OCSPResponseStatus enumeration.
     * <pre>
     * OCSPResponseStatus ::= ENUMERATED {
     *     successful            (0),  --Response has valid confirmations
     *     malformedRequest      (1),  --Illegal confirmation request
     *     internalError         (2),  --Internal error in issuer
     *     tryLater              (3),  --Try again later
     *                                 --(4) is not used
     *     sigRequired           (5),  --Must sign the request
     *     unauthorized          (6)   --Request unauthorized
     * }
     * </pre>
     */
    public OCSPResponseStatus(
        int value)
    {
        super(value);
    }

    public OCSPResponseStatus(
        DEREnumerated value)
    {
        super(value.getValue().intValue());
    }
}
