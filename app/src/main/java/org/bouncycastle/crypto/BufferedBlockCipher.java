package org.bouncycastle.crypto;


/**
 * A wrapper class that allows block ciphers to be used to process data in
 * a piecemeal fashion. The BufferedBlockCipher outputs a block only when the
 * buffer is full and more data is being added, or on a doFinal.
 * <p>
 * Note: in the case where the underlying cipher is either a CFB cipher or an
 * OFB one the last block may not be a multiple of the block size.
 */
public class BufferedBlockCipher
{
    protected byte[]        buf;
    protected int           bufOff;

    protected boolean       forEncryption;
    protected BlockCipher   cipher;

    protected boolean       partialBlockOkay;
    protected boolean       pgpCFB;

    /**
     * constructor for subclasses
     */
    protected BufferedBlockCipher()
    {
    }

    /**
     * Create a buffered block cipher without padding.
     *
     * @param cipher the underlying block cipher this buffering object wraps.
     */
    public BufferedBlockCipher(
        BlockCipher     cipher)
    {
        this.cipher = cipher;

        buf = new byte[cipher.getBlockSize()];
        bufOff = 0;

        //
        // check if we can handle partial blocks on doFinal.
        //
        String  name = cipher.getAlgorithmName();
        int     idx = name.indexOf('/') + 1;

        pgpCFB = (idx > 0 && name.startsWith("PGP", idx));

        if (pgpCFB)
        {
            partialBlockOkay = true;
        }
        else
        {
            partialBlockOkay = (idx > 0 && (name.startsWith("CFB", idx) || name.startsWith("OFB", idx) || name.startsWith("OpenPGP", idx) || name.startsWith("SIC", idx) || name.startsWith("GCTR", idx)));
        }
    }

    /**
     * return the cipher this object wraps.
     *
     * @return the cipher this object wraps.
     */
    public BlockCipher getUnderlyingCipher()
    {
        return cipher;
    }

    /**
     * initialise the cipher.
     *
     * @param forEncryption if true the cipher is initialised for
     *  encryption, if false for decryption.
     * @param params the key and other data required by the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean             forEncryption,
        CipherParameters    params)
        throws IllegalArgumentException
    {
        this.forEncryption = forEncryption;

        reset();

        cipher.init(forEncryption, params);
    }

    /**
     * return the blocksize for the underlying cipher.
     *
     * @return the blocksize for the underlying cipher.
     */
    public int getBlockSize()
    {
        return cipher.getBlockSize();
    }

    /**
     * return the size of the output buffer required for an update 
     * an input of len bytes.
     *
     * @param len the length of the input.
     * @return the space required to accommodate a call to update
     * with len bytes of input.
     */
    public int getUpdateOutputSize(
        int len)
    {
        int total       = len + bufOff;
        int leftOver;

        if (pgpCFB)
        {
            leftOver    = total % buf.length - (cipher.getBlockSize() + 2);
        }
        else
        {
            leftOver    = total % buf.length;
        }

        return total - leftOver;
    }

    /**
     * return the size of the output buffer required for an update plus a
     * doFinal with an input of 'length' bytes.
     *
     * @param length the length of the input.
     * @return the space required to accommodate a call to update and doFinal
     * with 'length' bytes of input.
     */
    public int getOutputSize(
        int length)
    {
        // Note: Can assume partialBlockOkay is true for purposes of this calculation
        return length + bufOff;
    }

    /**
     * process a single byte, producing an output block if neccessary.
     *
     * @param in the input byte.
     * @param out the space for any output that might be produced.
     * @param outOff the offset from which the output will be copied.
     * @return the number of output bytes copied to out.
     * @exception DataLengthException if there isn't enough space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     */
    public int processByte(
        byte        in,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        int         resultLen = 0;

        buf[bufOff++] = in;

        if (bufOff == buf.length)
        {
            resultLen = cipher.processBlock(buf, 0, out, outOff);
            bufOff = 0;
        }

        return resultLen;
    }

    /**
     * process an array of bytes, producing output if necessary.
     *
     * @param in the input byte array.
     * @param inOff the offset at which the input data starts.
     * @param len the number of bytes to be copied out of the input array.
     * @param out the space for any output that might be produced.
     * @param outOff the offset from which the output will be copied.
     * @return the number of output bytes copied to out.
     * @exception DataLengthException if there isn't enough space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     */
    public int processBytes(
        byte[]      in,
        int         inOff,
        int         len,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        if (len < 0)
        {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }

        int blockSize   = getBlockSize();
        int length      = getUpdateOutputSize(len);
        
        if (length > 0)
        {
            if ((outOff + length) > out.length)
            {
                throw new DataLengthException("output buffer too short");
            }
        }

        int resultLen = 0;
        int gapLen = buf.length - bufOff;

        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);

            resultLen += cipher.processBlock(buf, 0, out, outOff);

            bufOff = 0;
            len -= gapLen;
            inOff += gapLen;

            while (len > buf.length)
            {
                resultLen += cipher.processBlock(in, inOff, out, outOff + resultLen);

                len -= blockSize;
                inOff += blockSize;
            }
        }

        System.arraycopy(in, inOff, buf, bufOff, len);

        bufOff += len;

        if (bufOff == buf.length)
        {
            resultLen += cipher.processBlock(buf, 0, out, outOff + resultLen);
            bufOff = 0;
        }

        return resultLen;
    }

    /**
     * Process the last block in the buffer.
     *
     * @param out the array the block currently being held is copied into.
     * @param outOff the offset at which the copying starts.
     * @return the number of output bytes copied to out.
     * @exception DataLengthException if there is insufficient space in out for
     * the output, or the input is not block size aligned and should be.
     * @exception IllegalStateException if the underlying cipher is not
     * initialised.
     * @exception InvalidCipherTextException if padding is expected and not found.
     * @exception DataLengthException if the input is not block size
     * aligned.
     */
    public int doFinal(
        byte[]  out,
        int     outOff)
        throws DataLengthException, IllegalStateException, InvalidCipherTextException
    {
        int resultLen = 0;

        if (outOff + bufOff > out.length)
        {
            throw new DataLengthException("output buffer too short for doFinal()");
        }

        if (bufOff != 0 && partialBlockOkay)
        {
            cipher.processBlock(buf, 0, buf, 0);
            resultLen = bufOff;
            bufOff = 0;
            System.arraycopy(buf, 0, out, outOff, resultLen);
        }
        else if (bufOff != 0)
        {
            throw new DataLengthException("data not block size aligned");
        }

        reset();

        return resultLen;
    }

    /**
     * Reset the buffer and cipher. After resetting the object is in the same
     * state as it was after the last init (if there was one).
     */
    public void reset()
    {
        //
        // clean the buffer.
        //
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = 0;
        }

        bufOff = 0;

        //
        // reset the underlying cipher.
        //
        cipher.reset();
    }
}
