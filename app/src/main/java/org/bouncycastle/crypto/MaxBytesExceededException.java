package org.bouncycastle.crypto;

/**
 * this exception is thrown whenever a cipher requires a change of key, iv
 * or similar after x amount of bytes enciphered
 */
@SuppressWarnings("serial")
public class MaxBytesExceededException
    extends RuntimeCryptoException
{
    /**
     * base constructor.
     */
    public MaxBytesExceededException()
    {
    }

    /**
     * create an with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public MaxBytesExceededException(
        String  message)
    {
        super(message);
    }
}
