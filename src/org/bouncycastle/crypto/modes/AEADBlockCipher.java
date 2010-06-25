package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * A block cipher mode that includes authenticated encryption with a streaming mode and optional associated data.
 * @see org.bouncycastle.crypto.params.AEADParameters
 */
public interface AEADBlockCipher
{
    /**
     * initialise the underlying cipher. Parameter can either be an AEADParameters or a ParametersWithIV object.
     *
     * @param forEncryption true if we are setting up for encryption, false otherwise.
     * @param params the necessary parameters for the underlying cipher to be initialised.
     * @exception IllegalArgumentException if the params argument is inappropriate.
     */
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException;

    /**
     * Return the name of the algorithm.
     * 
     * @return the algorithm name.
     */
    public String getAlgorithmName();

    /**
     * return the cipher this object wraps.
     *
     * @return the cipher this object wraps.
     */
    public BlockCipher getUnderlyingCipher();

    /**
     * encrypt/decrypt a single byte.
     *
     * @param in the byte to be processed.
     * @param out the output buffer the processed byte goes into.
     * @param outOff the offset into the output byte array the processed data starts at.
     * @return the number of bytes written to out.
     * @exception DataLengthException if the output buffer is too small.
     */
    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException;

    /**
     * process a block of bytes from in putting the result into out.
     *
     * @param in the input byte array.
     * @param inOff the offset into the in array where the data to be processed starts.
     * @param len the number of bytes to be processed.
     * @param out the output buffer the processed bytes go into.
     * @param outOff the offset into the output byte array the processed data starts at.
     * @return the number of bytes written to out.
     * @exception DataLengthException if the output buffer is too small.
     */
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException;

    /**
     * Finish the operation either appending or verifying the MAC at the end of the data.
     *
     * @param out space for any resulting output data.
     * @param outOff offset into out to start copying the data at.
     * @return number of bytes written into out.
     * @throws IllegalStateException if the cipher is in an inappropriate state.
     * @throws org.bouncycastle.crypto.InvalidCipherTextException if the MAC fails to match.
     */
    public int doFinal(byte[] out, int outOff)
        throws IllegalStateException, InvalidCipherTextException;

    /**
     * Return the value of the MAC associated with the last stream processed.
     *
     * @return MAC for plaintext data.
     */
    public byte[] getMac();

    /**
     * return the size of the output buffer required for a processBytes
     * an input of len bytes.
     *
     * @param len the length of the input.
     * @return the space required to accommodate a call to processBytes
     * with len bytes of input.
     */
    public int getUpdateOutputSize(int len);

    /**
     * return the size of the output buffer required for a processBytes plus a
     * doFinal with an input of len bytes.
     *
     * @param len the length of the input.
     * @return the space required to accommodate a call to processBytes and doFinal
     * with len bytes of input.
     */
    public int getOutputSize(int len);

    /**
     * Reset the cipher. After resetting the cipher is in the same state
     * as it was after the last init (if there was one).
     */
    public void reset();
}
