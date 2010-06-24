package org.bouncycastle.asn1.ntt;

import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * From RFC 3657
 */
public interface NTTObjectIdentifiers
{
    public static final DERObjectIdentifier id_camellia128_cbc = new DERObjectIdentifier("1.2.392.200011.61.1.1.1.2");
    public static final DERObjectIdentifier id_camellia192_cbc = new DERObjectIdentifier("1.2.392.200011.61.1.1.1.3");
    public static final DERObjectIdentifier id_camellia256_cbc = new DERObjectIdentifier("1.2.392.200011.61.1.1.1.4");

    public static final DERObjectIdentifier id_camellia128_wrap = new DERObjectIdentifier("1.2.392.200011.61.1.1.3.2");
    public static final DERObjectIdentifier id_camellia192_wrap = new DERObjectIdentifier("1.2.392.200011.61.1.1.3.3");
    public static final DERObjectIdentifier id_camellia256_wrap = new DERObjectIdentifier("1.2.392.200011.61.1.1.3.4");
}
