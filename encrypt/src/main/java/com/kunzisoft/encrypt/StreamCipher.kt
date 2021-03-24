package com.kunzisoft.encrypt

import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.DataLengthException

/**
 * Stream cipher to process data
 */
class StreamCipher(private val streamCipher: org.bouncycastle.crypto.StreamCipher) {
    /**
     * Initialise the cipher.
     *
     * @param forEncryption if true the cipher is initialised for
     * encryption, if false for decryption.
     * @param params the key and other data required by the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    @Throws(IllegalArgumentException::class)
    fun init(forEncryption: Boolean, params: CipherParameters?) {
        streamCipher.init(forEncryption, params)
    }

    /**
     * process a block of bytes from in putting the result into out.
     *
     * @param data the input byte array.
     * @return the output buffer.
     * @exception DataLengthException if the output buffer is too small.
     */
    @Throws(DataLengthException::class)
    fun processBytes(data: ByteArray): ByteArray {
        val size = data.size
        val out = ByteArray(size)
        streamCipher.processBytes(data, 0, size, out, 0)
        return out
    }
}