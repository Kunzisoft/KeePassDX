package com.kunzisoft.keepass.tests.stream

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.BinaryAttachment
import com.kunzisoft.keepass.stream.readAllBytes
import com.kunzisoft.keepass.utils.UriUtil
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.security.MessageDigest

class BinaryAttachmentTest {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val cacheDirectory = UriUtil.getBinaryDir(InstrumentationRegistry.getInstrumentation().targetContext)
    private val fileA = File(cacheDirectory, TEST_FILE_CACHE_A)
    private val fileB = File(cacheDirectory, TEST_FILE_CACHE_B)
    private val fileC = File(cacheDirectory, TEST_FILE_CACHE_C)

    private val loadedKey = Database.LoadedKey.generateNewCipherKey()

    private fun saveBinary(asset: String, binaryAttachment: BinaryAttachment) {
        context.assets.open(asset).use { assetInputStream ->
            binaryAttachment.getOutputDataStream(loadedKey).use { binaryOutputStream ->
                assetInputStream.readAllBytes(DEFAULT_BUFFER_SIZE) { buffer ->
                    binaryOutputStream.write(buffer)
                }
            }
        }
    }

    @Test
    fun testSaveTextInCache() {
        val binaryA = BinaryAttachment(fileA)
        val binaryB = BinaryAttachment(fileB)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        saveBinary(TEST_TEXT_ASSET, binaryB)
        assertEquals("Save text binary failed.", binaryA.md5(), binaryB.md5())
    }

    @Test
    fun testSaveImageInCache() {
        val binaryA = BinaryAttachment(fileA)
        val binaryB = BinaryAttachment(fileB)
        saveBinary(TEST_IMAGE_ASSET, binaryA)
        saveBinary(TEST_IMAGE_ASSET, binaryB)
        assertEquals("Save image binary failed.", binaryA.md5(), binaryB.md5())
    }

    @Test
    fun testCompressText() {
        val binaryA = BinaryAttachment(fileA)
        val binaryB = BinaryAttachment(fileB)
        val binaryC = BinaryAttachment(fileC)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        saveBinary(TEST_TEXT_ASSET, binaryB)
        saveBinary(TEST_TEXT_ASSET, binaryC)
        binaryA.compress(loadedKey)
        binaryB.compress(loadedKey)
        assertEquals("Compress text failed.", binaryA.md5(), binaryB.md5())
        binaryB.decompress(loadedKey)
        assertEquals("Decompress text failed.", binaryB.md5(), binaryC.md5())
    }

    @Test
    fun testCompressImage() {
        val binaryA = BinaryAttachment(fileA)
        var binaryB = BinaryAttachment(fileB)
        val binaryC = BinaryAttachment(fileC)
        saveBinary(TEST_IMAGE_ASSET, binaryA)
        saveBinary(TEST_IMAGE_ASSET, binaryB)
        saveBinary(TEST_IMAGE_ASSET, binaryC)
        binaryA.compress(loadedKey)
        binaryB.compress(loadedKey)
        assertEquals("Compress image failed.", binaryA.md5(), binaryB.md5())
        binaryB = BinaryAttachment(fileB, true)
        binaryB.decompress(loadedKey)
        assertEquals("Decompress image failed.", binaryB.md5(), binaryC.md5())
    }

    @Test
    fun testReadText() {
        val binaryA = BinaryAttachment(fileA)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        assert(streamAreEquals(context.assets.open(TEST_TEXT_ASSET),
                binaryA.getInputDataStream(loadedKey)))
    }

    @Test
    fun testReadImage() {
        val binaryA = BinaryAttachment(fileA)
        saveBinary(TEST_IMAGE_ASSET, binaryA)
        assert(streamAreEquals(context.assets.open(TEST_IMAGE_ASSET),
                binaryA.getInputDataStream(loadedKey)))
    }

    private fun streamAreEquals(inputStreamA: InputStream,
                                inputStreamB: InputStream): Boolean {
        val bufferA = ByteArray(DEFAULT_BUFFER_SIZE)
        val bufferB = ByteArray(DEFAULT_BUFFER_SIZE)
        val dataInputStreamB = DataInputStream(inputStreamB)
        try {
            var len: Int
            while (inputStreamA.read(bufferA).also { len = it } > 0) {
                dataInputStreamB.readFully(bufferB, 0, len)
                for (i in 0 until len) {
                    if (bufferA[i] != bufferB[i])
                        return false
                }
            }
            return inputStreamB.read() < 0 // is the end of the second file also.
        } catch (e: Exception) {
            return false
        }
        finally {
            inputStreamA.close()
            inputStreamB.close()
        }
    }

    private fun BinaryAttachment.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return this.getInputDataStream(loadedKey).use { fis ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            generateSequence {
                when (val bytesRead = fis.read(buffer)) {
                    -1 -> null
                    else -> bytesRead
                }
            }.forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }

    companion object {
        private const val TEST_FILE_CACHE_A = "testA"
        private const val TEST_FILE_CACHE_B = "testB"
        private const val TEST_FILE_CACHE_C = "testC"
        private const val TEST_IMAGE_ASSET = "test_image.png"
        private const val TEST_TEXT_ASSET = "test_text.txt"
    }
}