package com.kunzisoft.keepass.stream

import java.io.IOException
import java.io.InputStream

interface ReadBytes {
    /**
     * Called after each buffer fill
     * @param buffer filled
     */
    @Throws(IOException::class)
    fun read(buffer: ByteArray)
}

@Throws(IOException::class)
fun readFromStream(inputStream: InputStream, bufferSize: Int, readBytes: ReadBytes) {
    val buffer = ByteArray(bufferSize)
    var read = 0
    while (read != -1) {
        read = inputStream.read(buffer, 0, buffer.size)
        if (read != -1) {
            val optimizedBuffer: ByteArray = if (buffer.size == read) {
                buffer
            } else {
                buffer.copyOf(read)
            }
            readBytes.read(optimizedBuffer)
        }
    }
}
