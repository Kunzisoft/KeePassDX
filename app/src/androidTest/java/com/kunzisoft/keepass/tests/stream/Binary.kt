package com.kunzisoft.keepass.tests.stream

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.database.BinaryAttachment
import com.kunzisoft.keepass.stream.readBytes
import com.kunzisoft.keepass.utils.UriUtil
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class Binary {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val cacheDirectory = UriUtil.getBinaryDir(InstrumentationRegistry.getInstrumentation().targetContext)
    private val fileA = File(cacheDirectory, TEST_FILE_CACHE_A)
    private val fileB = File(cacheDirectory, TEST_FILE_CACHE_B)
    private val fileC = File(cacheDirectory, TEST_FILE_CACHE_C)

    private fun saveBinary(asset: String, binaryAttachment: BinaryAttachment) {
        context.assets.open(asset).use { assetInputStream ->
            binaryAttachment.getOutputDataStream().use { binaryOutputStream ->
                assetInputStream.readBytes(DEFAULT_BUFFER_SIZE) { buffer ->
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
        binaryA.compress()
        binaryB.compress()
        assertEquals("Compress text failed.", binaryA.md5(), binaryB.md5())
        binaryB.decompress()
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
        binaryA.compress()
        binaryB.compress()
        assertEquals("Compress image failed.", binaryA.md5(), binaryB.md5())
        binaryB = BinaryAttachment(fileB, true)
        binaryB.decompress()
        assertEquals("Decompress image failed.", binaryB.md5(), binaryC.md5())
    }

    private fun BinaryAttachment.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return this.getInputDataStream().use { fis ->
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