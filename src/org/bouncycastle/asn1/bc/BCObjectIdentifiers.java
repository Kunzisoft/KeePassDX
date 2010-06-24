package org.bouncycastle.asn1.bc;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface BCObjectIdentifiers
{
    /**
     *  iso.org.dod.internet.private.enterprise.legion-of-the-bouncy-castle
     *
     *  1.3.6.1.4.1.22554
     */
    public static final DERObjectIdentifier bc = new DERObjectIdentifier("1.3.6.1.4.1.22554");

    /**
     * pbe(1) algorithms
     */
    public static final DERObjectIdentifier bc_pbe = new DERObjectIdentifier(bc.getId() + ".1");

    /**
     * SHA-1(1)
     */
    public static final DERObjectIdentifier bc_pbe_sha1 = new DERObjectIdentifier(bc_pbe.getId() + ".1");

    /**
     * SHA-2(2) . (SHA-256(1)|SHA-384(2)|SHA-512(3)|SHA-224(4))
     */
    public static final DERObjectIdentifier bc_pbe_sha256 = new DERObjectIdentifier(bc_pbe.getId() + ".2.1");
    public static final DERObjectIdentifier bc_pbe_sha384 = new DERObjectIdentifier(bc_pbe.getId() + ".2.2");
    public static final DERObjectIdentifier bc_pbe_sha512 = new DERObjectIdentifier(bc_pbe.getId() + ".2.3");
    public static final DERObjectIdentifier bc_pbe_sha224 = new DERObjectIdentifier(bc_pbe.getId() + ".2.4");

    /**
     * PKCS-5(1)|PKCS-12(2)
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs5 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12 = new DERObjectIdentifier(bc_pbe_sha1.getId() + ".2");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs5 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".1");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12 = new DERObjectIdentifier(bc_pbe_sha256.getId() + ".2");

    /**
     * AES(1) . (CBC-128(2)|CBC-192(22)|CBC-256(42))
     */
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha1_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha1_pkcs12.getId() + ".1.42");

    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes128_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.2");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes192_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.22");
    public static final DERObjectIdentifier bc_pbe_sha256_pkcs12_aes256_cbc = new DERObjectIdentifier(bc_pbe_sha256_pkcs12.getId() + ".1.42");
}
