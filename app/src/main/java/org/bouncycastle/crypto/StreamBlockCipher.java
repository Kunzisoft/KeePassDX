package org.bouncycastle.crypto;

/**
 * a wrapper for block ciphers with a single byte block size, so that they
 * can be treated like stream ciphers.
 */
public class StreamBlockCipher
    implements StreamCipher
{
    private BlockCipher  cipher;

    private byte[]  oneByte = new byte[1];

    /**
     * basic constructor.
     *
     * @param cipher the block cipher to be wrapped.
     * @exception IllegalArgumentException if the cipher has a block size other than
     * one.
     */
    public StreamBlockCipher(
        BlockCipher cipher)
    {
        if (cipher.getBlockSize() != 1)
        {
            throw new IllegalArgumentException("block cipher block size != 1.");
        }

        this.cipher = cipher;
    }

    /**
     * initialise the underlying cipher.
     *
     * @param forEncryption true if we are setting up for encryption, false otherwise.
     * @param params the necessary parameters for the underlying cipher to be initialised.
     */
    public void init(
        boolean forEncryption,
        CipherParameters params)
    {
        cipher.init(forEncryption, params);
    }

    /**
     * return the name of the algorithm we are wrapping.
     *
     * @return the name of the algorithm we are wrapping.
     */
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    /**
     * encrypt/decrypt a single byte returning the result.
     *
     * @param in the byte to be processed.
     * @return the result of processing the input byte.
     */
    public byte returnByte(
        byte    in)
    {
        oneByte[0] = in;

        cipher.processBlock(oneByte, 0, oneByte, 0);

        return oneByte[0];
    }

    /**
     * process a block of bytes from in putting the result into out.
     * 
     * @param in the input byte array.
     * @param inOff the offset into the in array where the data to be processed starts.
     * @param len the number of bytes to be processed.
     * @param out the output buffer the processed bytes go into.   
     * @param outOff the offset into the output byte array the processed data stars at.
     * @exception DataLengthException if the output buffer is too small.
     */
    public void processBytes(
        byte[]  in,
        int     inOff,
        int     len,
        byte[]  out,
        int     outOff)
        throws DataLengthException
    {
        if (outOff + len > out.length)
        {
            throw new DataLengthException("output buffer too small in processBytes()");
        }

        for (int i = 0; i != len; i++)
        {
                cipher.processBlock(in, inOff + i, out, outOff + i);
        }
    }

    /**
     * reset the underlying cipher. This leaves it in the same state
     * it was at after the last init (if there was one).
     */
    public void reset()
    {
        cipher.reset();
    }
}
