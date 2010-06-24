package org.bouncycastle.asn1.cmp;

import org.bouncycastle.asn1.DERBitString;

/**
 * <pre>
 * PKIFailureInfo ::= BIT STRING {
 * badAlg               (0),
 *   -- unrecognized or unsupported Algorithm Identifier
 * badMessageCheck      (1), -- integrity check failed (e.g., signature did not verify)
 * badRequest           (2),
 *   -- transaction not permitted or supported
 * badTime              (3), -- messageTime was not sufficiently close to the system time, as defined by local policy
 * badCertId            (4), -- no certificate could be found matching the provided criteria
 * badDataFormat        (5),
 *   -- the data submitted has the wrong format
 * wrongAuthority       (6), -- the authority indicated in the request is different from the one creating the response token
 * incorrectData        (7), -- the requester's data is incorrect (for notary services)
 * missingTimeStamp     (8), -- when the timestamp is missing but should be there (by policy)
 * badPOP               (9)  -- the proof-of-possession failed
 * timeNotAvailable    (14),
 *   -- the TSA's time source is not available
 * unacceptedPolicy    (15),
 *   -- the requested TSA policy is not supported by the TSA
 * unacceptedExtension (16),
 *   -- the requested extension is not supported by the TSA
 *  addInfoNotAvailable (17)
 *    -- the additional information requested could not be understood
 *    -- or is not available
 *  systemFailure       (25)
 *    -- the request cannot be handled due to system failure 
 * </pre>
 */
public class PKIFailureInfo
    extends DERBitString
{


    public static final int badAlg               = (1 << 7); // unrecognized or unsupported Algorithm Identifier
    public static final int badMessageCheck      = (1 << 6); // integrity check failed (e.g., signature did not verify)
    public static final int badRequest           = (1 << 5);
    public static final int badTime              = (1 << 4); // -- messageTime was not sufficiently close to the system time, as defined by local policy
    public static final int badCertId            = (1 << 3); // no certificate could be found matching the provided criteria
    public static final int badDataFormat        = (1 << 2);
    public static final int wrongAuthority       = (1 << 1); // the authority indicated in the request is different from the one creating the response token
    public static final int incorrectData        = 1;        // the requester's data is incorrect (for notary services)
    public static final int missingTimeStamp     = (1 << 15); // when the timestamp is missing but should be there (by policy)
    public static final int badPOP               = (1 << 14); // the proof-of-possession failed
    public static final int timeNotAvailable     = (1 << 9); // the TSA's time source is not available
    public static final int unacceptedPolicy     = (1 << 8); // the requested TSA policy is not supported by the TSA
    public static final int unacceptedExtension  = (1 << 23); //the requested extension is not supported by the TSA
    public static final int addInfoNotAvailable  = (1 << 22); //the additional information requested could not be understood or is not available
    public static final int systemFailure        = (1 << 30); //the request cannot be handled due to system failure
    
    /** @deprecated use lower case version */
    public static final int BAD_ALG                   = badAlg; // unrecognized or unsupported Algorithm Identifier
    /** @deprecated use lower case version */
    public static final int BAD_MESSAGE_CHECK         = badMessageCheck;
    /** @deprecated use lower case version */
    public static final int BAD_REQUEST               = badRequest; // transaction not permitted or supported
    /** @deprecated use lower case version */
    public static final int BAD_TIME                  = badTime;
    /** @deprecated use lower case version */
    public static final int BAD_CERT_ID               = badCertId;
    /** @deprecated use lower case version */
    public static final int BAD_DATA_FORMAT           = badDataFormat; // the data submitted has the wrong format
    /** @deprecated use lower case version */
    public static final int WRONG_AUTHORITY           = wrongAuthority;
    /** @deprecated use lower case version */
    public static final int INCORRECT_DATA            = incorrectData;
    /** @deprecated use lower case version */
    public static final int MISSING_TIME_STAMP        = missingTimeStamp;
    /** @deprecated use lower case version */
    public static final int BAD_POP                   = badPOP;
    /** @deprecated use lower case version */
    public static final int TIME_NOT_AVAILABLE        = timeNotAvailable;
    /** @deprecated use lower case version */
    public static final int UNACCEPTED_POLICY         = unacceptedPolicy;
    /** @deprecated use lower case version */
    public static final int UNACCEPTED_EXTENSION      = unacceptedExtension;
    /** @deprecated use lower case version */
    public static final int ADD_INFO_NOT_AVAILABLE    = addInfoNotAvailable; 
    /** @deprecated use lower case version */
    public static final int SYSTEM_FAILURE            = systemFailure; 
    /**
     * Basic constructor.
     */
    public PKIFailureInfo(
        int info)
    {
        super(getBytes(info), getPadBits(info));
    }

    public PKIFailureInfo(
        DERBitString info)
    {
        super(info.getBytes(), info.getPadBits());
    }
    
    public String toString()
    {
        return "PKIFailureInfo: 0x" + Integer.toHexString(this.intValue());
    }
}
