package org.bouncycastle.crypto;

/**
 * this exception is thrown if a buffer that is meant to have output
 * copied into it turns out to be too short, or if we've been given 
 * insufficient input. In general this exception will get thrown rather
 * than an ArrayOutOfBounds exception.
 */
public class DataLengthException 
    extends RuntimeCryptoException
{
    /**
     * base constructor.
     */
    public DataLengthException()
    {
    }

    /**
     * create a DataLengthException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public DataLengthException(
        String  message)
    {
        super(message);
    }
}
