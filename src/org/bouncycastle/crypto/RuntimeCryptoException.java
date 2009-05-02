package org.bouncycastle.crypto;

/**
 * the foundation class for the exceptions thrown by the crypto packages.
 */
public class RuntimeCryptoException 
    extends RuntimeException
{
    /**
     * base constructor.
     */
    public RuntimeCryptoException()
    {
    }

    /**
     * create a RuntimeCryptoException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public RuntimeCryptoException(
        String  message)
    {
        super(message);
    }
}
