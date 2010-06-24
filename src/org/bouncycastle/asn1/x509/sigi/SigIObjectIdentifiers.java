package org.bouncycastle.asn1.x509.sigi;

import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * Object Identifiers of SigI specifciation (German Signature Law
 * Interoperability specification).
 */
public interface SigIObjectIdentifiers
{
    public final static DERObjectIdentifier id_sigi = new DERObjectIdentifier("1.3.36.8");

    /**
     * Key purpose IDs for German SigI (Signature Interoperability
     * Specification)
     */
    public final static DERObjectIdentifier id_sigi_kp = new DERObjectIdentifier(id_sigi + ".2");

    /**
     * Certificate policy IDs for German SigI (Signature Interoperability
     * Specification)
     */
    public final static DERObjectIdentifier id_sigi_cp = new DERObjectIdentifier(id_sigi + ".1");

    /**
     * Other Name IDs for German SigI (Signature Interoperability Specification)
     */
    public final static DERObjectIdentifier id_sigi_on = new DERObjectIdentifier(id_sigi + ".4");

    /**
     * To be used for for the generation of directory service certificates.
     */
    public static final DERObjectIdentifier id_sigi_kp_directoryService = new DERObjectIdentifier(id_sigi_kp + ".1");

    /**
     * ID for PersonalData
     */
    public static final DERObjectIdentifier id_sigi_on_personalData = new DERObjectIdentifier(id_sigi_on + ".1");

    /**
     * Certificate is conform to german signature law.
     */
    public static final DERObjectIdentifier id_sigi_cp_sigconform = new DERObjectIdentifier(id_sigi_cp + ".1");

}
