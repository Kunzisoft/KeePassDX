package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Generator for PBE derived keys and ivs as defined by PKCS 5 V2.0 Scheme 1.
 * Note this generator is limited to the size of the hash produced by the
 * digest used to drive it.
 * <p>
 * The document this implementation is based on can be found at
 * <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html>
 * RSA's PKCS5 Page</a>
 */
public class PKCS5S1ParametersGenerator
    extends PBEParametersGenerator
{
    private Digest  digest;

    /**
     * Construct a PKCS 5 Scheme 1 Parameters generator. 
     *
     * @param digest the digest to be used as the source of derived keys.
     */
    public PKCS5S1ParametersGenerator(
        Digest  digest)
    {
        this.digest = digest;
    }

    /**
     * the derived key function, the ith hash of the password and the salt.
     */
    private byte[] generateDerivedKey()
    {
        byte[] digestBytes = new byte[digest.getDigestSize()];

        digest.update(password, 0, password.length);
        digest.update(salt, 0, salt.length);

        digest.doFinal(digestBytes, 0);
        for (int i = 1; i < iterationCount; i++)
        {
            digest.update(digestBytes, 0, digestBytes.length);
            digest.doFinal(digestBytes, 0);
        }

        return digestBytes;
    }

    /**
     * Generate a key parameter derived from the password, salt, and iteration
     * count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     * @exception IllegalArgumentException if the key length larger than the base hash size.
     */
    public CipherParameters generateDerivedParameters(
        int keySize)
    {
        keySize = keySize / 8;

        if (keySize > digest.getDigestSize())
        {
            throw new IllegalArgumentException(
                   "Can't generate a derived key " + keySize + " bytes long.");
        }

        byte[]  dKey = generateDerivedKey();

        return new KeyParameter(dKey, 0, keySize);
    }

    /**
     * Generate a key with initialisation vector parameter derived from
     * the password, salt, and iteration count we are currently initialised
     * with.
     *
     * @param keySize the size of the key we want (in bits)
     * @param ivSize the size of the iv we want (in bits)
     * @return a ParametersWithIV object.
     * @exception IllegalArgumentException if keySize + ivSize is larger than the base hash size.
     */
    public CipherParameters generateDerivedParameters(
        int     keySize,
        int     ivSize)
    {
        keySize = keySize / 8;
        ivSize = ivSize / 8;

        if ((keySize + ivSize) > digest.getDigestSize())
        {
            throw new IllegalArgumentException(
                   "Can't generate a derived key " + (keySize + ivSize) + " bytes long.");
        }

        byte[]  dKey = generateDerivedKey();

        return new ParametersWithIV(new KeyParameter(dKey, 0, keySize), dKey, keySize, ivSize);
    }

    /**
     * Generate a key parameter for use with a MAC derived from the password,
     * salt, and iteration count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     * @exception IllegalArgumentException if the key length larger than the base hash size.
     */
    public CipherParameters generateDerivedMacParameters(
        int keySize)
    {
        return generateDerivedParameters(keySize);
    }
}
