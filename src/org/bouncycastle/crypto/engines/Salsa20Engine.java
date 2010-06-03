package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.MaxBytesExceededException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implementation of Daniel J. Bernstein's Salsa20 stream cipher, Snuffle 2005
 */

public class Salsa20Engine
    implements StreamCipher
{
    /** Constants */
    private final static int stateSize = 16; // 16, 32 bit ints = 64 bytes
    
    private final static byte[]
        sigma = toByteArray("expand 32-byte k"),
        tau   = toByteArray("expand 16-byte k");
     
    // No sure why the version in org.bouncycastle.util.Strings was throwing NoSuchMethodErrors 
    private static byte[] toByteArray(String string)
    {
        byte[] bytes = new byte[string.length()];

        for (int i = 0; i != bytes.length; i++)
        {
            char ch = string.charAt(i);

            bytes[i] = (byte)ch;
        }

        return bytes;
    }


    /*
     * variables to hold the state of the engine
     * during encryption and decryption
     */
    private int         index = 0;
    private int[]       engineState = new int[stateSize]; // state
    private int[]       x = new int[stateSize] ; // internal buffer
    private byte[]      keyStream   = new byte[stateSize * 4], // expanded state, 64 bytes
                        workingKey  = null,
                        workingIV   = null;
    private boolean     initialised = false;
    
    /*
     * internal counter
     */
    private int cW0, cW1, cW2;
    

    /**
     * initialise a Salsa20 cipher.
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean             forEncryption, 
        CipherParameters     params)
    {
        /* 
        * Salsa20 encryption and decryption is completely
        * symmetrical, so the 'forEncryption' is 
        * irrelevant. (Like 90% of stream ciphers)
        */

        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException("Salsa20 Init parameters must include an IV");
        }

        ParametersWithIV ivParams = (ParametersWithIV) params;

        byte[] iv = ivParams.getIV();

        if (iv == null || iv.length != 8)
        {
            throw new IllegalArgumentException("Salsa20 requires exactly 8 bytes of IV");
        }

        if (!(ivParams.getParameters() instanceof KeyParameter))
        {
            throw new IllegalArgumentException("Salsa20 Init parameters must include a key");
        }

        KeyParameter key = (KeyParameter) ivParams.getParameters();

        workingKey = key.getKey();
        workingIV = iv;

        setKey(workingKey, workingIV);
    }

    public String getAlgorithmName()
    {
        return "Salsa20";
    }

    public byte returnByte(byte in)
    {
        if (limitExceeded())
        {
            throw new MaxBytesExceededException("2^70 byte limit per IV; Change IV");
        }
        
        if (index == 0)
        {
            salsa20WordToByte(engineState, keyStream);
            engineState[8]++;
            if (engineState[8] == 0)
            {
                engineState[9]++;
            }
        }
        byte out = (byte)(keyStream[index]^in);
        index = (index + 1) & 63;
    
        return out;
    }

    public void processBytes(
        byte[]     in, 
        int     inOff, 
        int     len, 
        byte[]     out, 
        int     outOff)
    {
        if (!initialised)
        {
            throw new IllegalStateException(getAlgorithmName()+" not initialised");
        }
        
        if ((inOff + len) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + len) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }

        if (limitExceeded(len))
        {
            throw new MaxBytesExceededException("2^70 byte limit per IV would be exceeded; Change IV");
        }

        for (int i = 0; i < len; i++)
        {
            if (index == 0)
            {
                salsa20WordToByte(engineState, keyStream);
                engineState[8]++;
                if (engineState[8] == 0)
                {
                    engineState[9]++;
                }
            }
            out[i+outOff] = (byte)(keyStream[index]^in[i+inOff]);
            index = (index + 1) & 63;
        }
    }

    public void reset()
    {
        setKey(workingKey, workingIV);
    }

    // Private implementation

    private void setKey(byte[] keyBytes, byte[] ivBytes)
    {
        workingKey = keyBytes;
        workingIV  = ivBytes;

        index = 0;
        resetCounter();
        int offset = 0;
        byte[] constants;
        
        // Key
        engineState[1] = byteToIntLittle(workingKey, 0);
        engineState[2] = byteToIntLittle(workingKey, 4);
        engineState[3] = byteToIntLittle(workingKey, 8);
        engineState[4] = byteToIntLittle(workingKey, 12);
        
        if (workingKey.length == 32)
        {
            constants = sigma;
            offset = 16;
        }
        else
        {
            constants = tau;
        }
        
        engineState[11] = byteToIntLittle(workingKey, offset);
        engineState[12] = byteToIntLittle(workingKey, offset+4);
        engineState[13] = byteToIntLittle(workingKey, offset+8);
        engineState[14] = byteToIntLittle(workingKey, offset+12);
        engineState[0 ] = byteToIntLittle(constants, 0);
        engineState[5 ] = byteToIntLittle(constants, 4);
        engineState[10] = byteToIntLittle(constants, 8);
        engineState[15] = byteToIntLittle(constants, 12);
        
        // IV
        engineState[6] = byteToIntLittle(workingIV, 0);
        engineState[7] = byteToIntLittle(workingIV, 4);
        engineState[8] = engineState[9] = 0;
        
        initialised = true;
    }
    
    /**
     * Salsa20 function
     *
     * @param   input   input data
     *
     * @return  keystream
     */    
    private void salsa20WordToByte(int[] input, byte[] output)
    {
        System.arraycopy(input, 0, x, 0, input.length);

        for (int i = 0; i < 10; i++)
        {
            x[ 4] ^= rotl((x[ 0]+x[12]), 7);
            x[ 8] ^= rotl((x[ 4]+x[ 0]), 9);
            x[12] ^= rotl((x[ 8]+x[ 4]),13);
            x[ 0] ^= rotl((x[12]+x[ 8]),18);
            x[ 9] ^= rotl((x[ 5]+x[ 1]), 7);
            x[13] ^= rotl((x[ 9]+x[ 5]), 9);
            x[ 1] ^= rotl((x[13]+x[ 9]),13);
            x[ 5] ^= rotl((x[ 1]+x[13]),18);
            x[14] ^= rotl((x[10]+x[ 6]), 7);
            x[ 2] ^= rotl((x[14]+x[10]), 9);
            x[ 6] ^= rotl((x[ 2]+x[14]),13);
            x[10] ^= rotl((x[ 6]+x[ 2]),18);
            x[ 3] ^= rotl((x[15]+x[11]), 7);
            x[ 7] ^= rotl((x[ 3]+x[15]), 9);
            x[11] ^= rotl((x[ 7]+x[ 3]),13);
            x[15] ^= rotl((x[11]+x[ 7]),18);
            x[ 1] ^= rotl((x[ 0]+x[ 3]), 7);
            x[ 2] ^= rotl((x[ 1]+x[ 0]), 9);
            x[ 3] ^= rotl((x[ 2]+x[ 1]),13);
            x[ 0] ^= rotl((x[ 3]+x[ 2]),18);
            x[ 6] ^= rotl((x[ 5]+x[ 4]), 7);
            x[ 7] ^= rotl((x[ 6]+x[ 5]), 9);
            x[ 4] ^= rotl((x[ 7]+x[ 6]),13);
            x[ 5] ^= rotl((x[ 4]+x[ 7]),18);
            x[11] ^= rotl((x[10]+x[ 9]), 7);
            x[ 8] ^= rotl((x[11]+x[10]), 9);
            x[ 9] ^= rotl((x[ 8]+x[11]),13);
            x[10] ^= rotl((x[ 9]+x[ 8]),18);
            x[12] ^= rotl((x[15]+x[14]), 7);
            x[13] ^= rotl((x[12]+x[15]), 9);
            x[14] ^= rotl((x[13]+x[12]),13);
            x[15] ^= rotl((x[14]+x[13]),18);
        }

        int offset = 0;
        for (int i = 0; i < stateSize; i++)
        {
            intToByteLittle(x[i] + input[i], output, offset);
            offset += 4;
        }

        for (int i = stateSize; i < x.length; i++)
        {
            intToByteLittle(x[i], output, offset);
            offset += 4;
        }
    }
    
    /**
     * 32 bit word to 4 byte array in little endian order
     *
     * @param   x   value to 'unpack'
     *
     * @return  value of x expressed as a byte[] array in little endian order
     */
    private byte[] intToByteLittle(int x, byte[] out, int off)
    {
        out[off] = (byte)x;
        out[off + 1] = (byte)(x >>> 8);
        out[off + 2] = (byte)(x >>> 16);
        out[off + 3] = (byte)(x >>> 24);
        return out;
    }
    
    /**
     * Rotate left
     *
     * @param   x   value to rotate
     * @param   y   amount to rotate x
     *
     * @return  rotated x
     */
    private int rotl(int x, int y)
    {
        return (x << y) | (x >>> -y);
    }
    
    /**
     * Pack byte[] array into an int in little endian order
     *
     * @param   x       byte array to 'pack'
     * @param   offset  only x[offset]..x[offset+3] will be packed
     *
     * @return  x[offset]..x[offset+3] 'packed' into an int in little-endian order
     */
    private int byteToIntLittle(byte[] x, int offset)
    {
        return ((x[offset] & 255)) |
               ((x[offset + 1] & 255) <<  8) |
               ((x[offset + 2] & 255) << 16) |
                (x[offset + 3] << 24);
    }

    private void resetCounter()
    {
        cW0 = 0;
        cW1 = 0;
        cW2 = 0;
    }

    private boolean limitExceeded()
    {
        cW0++;
        if (cW0 == 0)
        {
            cW1++;
            if (cW1 == 0)
            {
                cW2++;
                return (cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
            }
        }

        return false;
    }

    /*
     * this relies on the fact len will always be positive.
     */
    private boolean limitExceeded(int len)
    {
        if (cW0 >= 0)
        {
            cW0 += len;
        }
        else
        {
            cW0 += len;
            if (cW0 >= 0)
            {
                cW1++;
                if (cW1 == 0)
                {
                    cW2++;
                    return (cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
                }
            }
        }

        return false;
    }
}
