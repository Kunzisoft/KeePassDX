package org.bouncycastle.asn1.gnu;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface GNUObjectIdentifiers
{
    public static final DERObjectIdentifier GNU = new DERObjectIdentifier("1.3.6.1.4.1.11591.1"); // GNU Radius
    public static final DERObjectIdentifier GnuPG = new DERObjectIdentifier("1.3.6.1.4.1.11591.2"); // GnuPG (Ägypten)
    public static final DERObjectIdentifier notation = new DERObjectIdentifier("1.3.6.1.4.1.11591.2.1"); // notation
    public static final DERObjectIdentifier pkaAddress = new DERObjectIdentifier("1.3.6.1.4.1.11591.2.1.1"); // pkaAddress
    public static final DERObjectIdentifier GnuRadar = new DERObjectIdentifier("1.3.6.1.4.1.11591.3"); // GNU Radar
    public static final DERObjectIdentifier digestAlgorithm = new DERObjectIdentifier("1.3.6.1.4.1.11591.12"); // digestAlgorithm
    public static final DERObjectIdentifier Tiger_192 = new DERObjectIdentifier("1.3.6.1.4.1.11591.12.2"); // TIGER/192
    public static final DERObjectIdentifier encryptionAlgorithm = new DERObjectIdentifier("1.3.6.1.4.1.11591.13"); // encryptionAlgorithm
    public static final DERObjectIdentifier Serpent = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2"); // Serpent
    public static final DERObjectIdentifier Serpent_128_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.1"); // Serpent-128-ECB
    public static final DERObjectIdentifier Serpent_128_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.2"); // Serpent-128-CBC
    public static final DERObjectIdentifier Serpent_128_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.3"); // Serpent-128-OFB
    public static final DERObjectIdentifier Serpent_128_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.4"); // Serpent-128-CFB
    public static final DERObjectIdentifier Serpent_192_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.21"); // Serpent-192-ECB
    public static final DERObjectIdentifier Serpent_192_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.22"); // Serpent-192-CBC
    public static final DERObjectIdentifier Serpent_192_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.23"); // Serpent-192-OFB
    public static final DERObjectIdentifier Serpent_192_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.24"); // Serpent-192-CFB
    public static final DERObjectIdentifier Serpent_256_ECB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.41"); // Serpent-256-ECB
    public static final DERObjectIdentifier Serpent_256_CBC = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.42"); // Serpent-256-CBC
    public static final DERObjectIdentifier Serpent_256_OFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.43"); // Serpent-256-OFB
    public static final DERObjectIdentifier Serpent_256_CFB = new DERObjectIdentifier("1.3.6.1.4.1.11591.13.2.44"); // Serpent-256-CFB
    public static final DERObjectIdentifier CRC = new DERObjectIdentifier("1.3.6.1.4.1.11591.14"); // CRC algorithms
    public static final DERObjectIdentifier CRC32 = new DERObjectIdentifier("1.3.6.1.4.1.11591.14.1"); // CRC 32
}
