/* Reworked version of the bouncycastle ChaCh7539 engine that isn't in spongycastle 1.55 */
package org.spongycastle.crypto.engines;

import org.spongycastle.util.Pack;
import org.spongycastle.util.Strings;

/**
 * Implementation of Daniel J. Bernstein's ChaCha stream cipher.
 */
public class ChaCha7539Engine extends Salsa20Engine
{
    /**
     * Creates a 20 rounds ChaCha engine.
     */
    public ChaCha7539Engine()
    {
        super();
    }

    public String getAlgorithmName()
    {
        return "ChaCha7539-" + rounds;
    }

    protected int getNonceSize()
    {
        return 12;
    }

    protected void advanceCounter(long diff)
    {
        int hi = (int)(diff >>> 32);
        int lo = (int)diff;

        if (hi > 0)
        {
            throw new IllegalStateException("attempt to increase counter past 2^32.");
        }

        int oldState = engineState[12];

        engineState[12] += lo;

        if (oldState != 0 && engineState[12] < oldState)
        {
            throw new IllegalStateException("attempt to increase counter past 2^32.");
        }
    }

    protected void advanceCounter()
    {
        if (++engineState[12] == 0)
        {
            throw new IllegalStateException("attempt to increase counter past 2^32.");
        }
    }

    protected void retreatCounter(long diff)
    {
        int hi = (int)(diff >>> 32);
        int lo = (int)diff;

        if (hi != 0)
        {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }

        if ((engineState[12] & 0xffffffffL) >= (lo & 0xffffffffL))
        {
            engineState[12] -= lo;
        }
        else
        {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
    }

    protected void retreatCounter()
    {
        if (engineState[12] == 0)
        {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }

        --engineState[12];
    }

    protected long getCounter()
    {
        return engineState[12] & 0xffffffffL;
    }

    protected void resetCounter()
    {
        engineState[12] = 0;
    }

    private static int[] littleEndianToInt(byte[] bs, int off, int count)
    {
        int[] ns = new int[count];
        for (int i = 0; i < ns.length; ++i)
        {
            ns[i] = Pack.littleEndianToInt(bs, off);
            off += 4;
        }
        return ns;
    }


    private final static int[] TAU_SIGMA = ChaCha7539Engine.littleEndianToInt(Strings.toByteArray("expand 16-byte k" + "expand 32-byte k"), 0, 8);

    protected void packTauOrSigma(int keyLength, int[] state, int stateOffset)
    {
        int tsOff = (keyLength - 16) / 4;
        state[stateOffset    ] = TAU_SIGMA[tsOff    ];
        state[stateOffset + 1] = TAU_SIGMA[tsOff + 1];
        state[stateOffset + 2] = TAU_SIGMA[tsOff + 2];
        state[stateOffset + 3] = TAU_SIGMA[tsOff + 3];
    }

    protected void setKey(byte[] keyBytes, byte[] ivBytes)
    {
        if (keyBytes != null)
        {
            if (keyBytes.length != 32)
            {
                throw new IllegalArgumentException(getAlgorithmName() + " requires 256 bit key");
            }

            packTauOrSigma(keyBytes.length, engineState, 0);

            // Key
            Pack.littleEndianToInt(keyBytes, 0, engineState, 4, 8);
        }

        // IV
        Pack.littleEndianToInt(ivBytes, 0, engineState, 13, 3);
    }

    protected void generateKeyStream(byte[] output)
    {
        ChaChaEngine.chachaCore(rounds, engineState, x);
        Pack.intToLittleEndian(x, output, 0);
    }
}

