package com.kunzisoft.keepass.tests.stream

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.BinaryFile
import com.kunzisoft.keepass.stream.readAllBytes
import com.kunzisoft.keepass.utils.UriUtil
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.io.InputStream

class BinaryDataTest {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val cacheDirectory = UriUtil.getBinaryDir(InstrumentationRegistry.getInstrumentation().targetContext)
    private val fileA = File(cacheDirectory, TEST_FILE_CACHE_A)
    private val fileB = File(cacheDirectory, TEST_FILE_CACHE_B)
    private val fileC = File(cacheDirectory, TEST_FILE_CACHE_C)

    private val loadedKey = Database.LoadedKey.generateNewCipherKey()

    private fun saveBinary(asset: String, binaryData: BinaryFile) {
        context.assets.open(asset).use { assetInputStream ->
            binaryData.getOutputDataStream(loadedKey).use { binaryOutputStream ->
                assetInputStream.readAllBytes(DEFAULT_BUFFER_SIZE) { buffer ->
                    binaryOutputStream.write(buffer)
                }
            }
        }
    }

    @Test
    fun testSaveTextInCache() {
        val binaryA = BinaryFile(fileA)
        val binaryB = BinaryFile(fileB)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        saveBinary(TEST_TEXT_ASSET, binaryB)
        assertEquals("Save text binary length failed.", binaryA.getSize(), binaryB.getSize())
        assertEquals("Save text binary MD5 failed.", binaryA.binaryHash(), binaryB.binaryHash())
    }

    @Test
    fun testSaveImageInCache() {
        val binaryA = BinaryFile(fileA)
        val binaryB = BinaryFile(fileB)
        saveBinary(TEST_IMAGE_ASSET, binaryA)
        saveBinary(TEST_IMAGE_ASSET, binaryB)
        assertEquals("Save image binary length failed.", binaryA.getSize(), binaryB.getSize())
        assertEquals("Save image binary failed.", binaryA.binaryHash(), binaryB.binaryHash())
    }

    @Test
    fun testCompressText() {
        val binaryA = BinaryFile(fileA)
        val binaryB = BinaryFile(fileB)
        val binaryC = BinaryFile(fileC)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        saveBinary(TEST_TEXT_ASSET, binaryB)
        saveBinary(TEST_TEXT_ASSET, binaryC)
        binaryA.compress(loadedKey)
        binaryB.compress(loadedKey)
        assertEquals("Compress text length failed.", binaryA.getSize(), binaryB.getSize())
        assertEquals("Compress text MD5 failed.", binaryA.binaryHash(), binaryB.binaryHash())
        binaryB.decompress(loadedKey)
        assertEquals("Decompress text length failed.", binaryB.getSize(), binaryC.getSize())
        assertEquals("Decompress text MD5 failed.", binaryB.binaryHash(), binaryC.binaryHash())
    }

    @Test
    fun testCompressImage() {
        val binaryA = BinaryFile(fileA)
        var binaryB = BinaryFile(fileB)
        val binaryC = BinaryFile(fileC)
        saveBinary(TEST_IMAGE_ASSET, binaryA)
        saveBinary(TEST_IMAGE_ASSET, binaryB)
        saveBinary(TEST_IMAGE_ASSET, binaryC)
        binaryA.compress(loadedKey)
        binaryB.compress(loadedKey)
        assertEquals("Compress image length failed.", binaryA.getSize(), binaryA.getSize())
        assertEquals("Compress image failed.", binaryA.binaryHash(), binaryA.binaryHash())
        binaryB = BinaryFile(fileB, true)
        binaryB.decompress(loadedKey)
        assertEquals("Decompress image length failed.", binaryB.getSize(), binaryC.getSize())
        assertEquals("Decompress image failed.", binaryB.binaryHash(), binaryC.binaryHash())
    }

    @Test
    fun testReadText() {
        val binaryA = BinaryFile(fileA)
        saveBinary(TEST_TEXT_ASSET, binaryA)
        assert(streamAreEquals(context.assets.open(TEST_TEXT_ASSET),
                binaryA.getInputDataStream(loadedKey)))
    }

    @Test
    fun testReadImage() {
        val binaryA = BinaryFile(fileA)
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

    companion object {
        private const val TEST_FILE_CACHE_A = "testA"
        private const val TEST_FILE_CACHE_B = "testB"
        private const val TEST_FILE_CACHE_C = "testC"
        private const val TEST_IMAGE_ASSET = "test_image.png"
        private const val TEST_TEXT_ASSET = "test_text.txt"
    }
}