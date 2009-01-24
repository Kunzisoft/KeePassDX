package org.bouncycastle1.crypto.prng;

import org.bouncycastle1.crypto.Digest;

/**
 * Random generation based on the digest with counter. Calling addSeedMaterial will
 * always increase the entropy of the hash.
 * <p>
 * Internal access to the digest is synchronized so a single one of these can be shared.
 * </p>
 */
public class DigestRandomGenerator
    implements RandomGenerator
{
    private long                counter;
    private Digest              digest;
    private byte[]              state;

    // public constructors
    public DigestRandomGenerator(
        Digest digest)
    {
        this.digest = digest;
        this.state = new byte[digest.getDigestSize()];
        this.counter = 1;
    }

    public void addSeedMaterial(byte[] inSeed)
    {
        synchronized (this)
        {
            digestUpdate(inSeed);
        }
    }

    public void addSeedMaterial(long rSeed)
    {
        synchronized (this)
        {
            for (int i = 0; i != 8; i++)
            {
                digestUpdate((byte)rSeed);
                rSeed >>>= 8;
            }
        }
    }

    public void nextBytes(byte[] bytes)
    {
        nextBytes(bytes, 0, bytes.length);
    }

    public void nextBytes(byte[] bytes, int start, int len)
    {
        synchronized (this)
        {
            int stateOff = 0;

            digestDoFinal(state);

            int end = start + len;
            for (int i = start; i != end; i++)
            {
                if (stateOff == state.length)
                {
                    digestUpdate(counter++);
                    digestUpdate(state);
                    digestDoFinal(state);
                    stateOff = 0;
                }
                bytes[i] = state[stateOff++];
            }

            digestUpdate(counter++);
            digestUpdate(state);
        }
    }

    private void digestUpdate(long seed)
    {
        for (int i = 0; i != 8; i++)
        {
            digest.update((byte)seed);
            seed >>>= 8;
        }
    }

    private void digestUpdate(byte[] inSeed)
    {
        digest.update(inSeed, 0, inSeed.length);
    }

    private void digestDoFinal(byte[] result)
    {
        digest.doFinal(result, 0);
    }
}
