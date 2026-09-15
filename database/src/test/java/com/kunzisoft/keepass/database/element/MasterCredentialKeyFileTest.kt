package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.utils.CodecUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

@RunWith(RobolectricTestRunner::class)
class MasterCredentialKeyFileTest {

    @Test
    fun testCreateXMLKeyFileDefaultSize() {
        val outputStream = ByteArrayOutputStream()
        MasterCredential.createKeyFile(outputStream, format = MasterCredential.CREATOR.KeyFileFormat.XML_2_0)
        val xmlContent = outputStream.toString("UTF-8")

        // Check Version 2.0
        assertTrue(xmlContent.contains("<Version>2.0</Version>"))

        // Extract Data
        val dataPattern = Pattern.compile("<Data Hash=\"([A-F0-9]{8})\">([A-F0-9]+)</Data>")
        val matcher = dataPattern.matcher(xmlContent)
        assertTrue("Data tag not found or incorrect format", matcher.find())

        val hash = matcher.group(1)!!
        val hexData = matcher.group(2)!!

        // Verify size: 32 bytes = 64 hex characters
        assertEquals("Key material should be 32 bytes (64 hex chars)", 64, hexData.length)

        // Verify hash matches
        val dataBytes = CodecUtil.decodeHex(hexData)
        val expectedHash = com.kunzisoft.encrypt.HashManager.sha256(dataBytes)
            .copyOfRange(0, 4)
            .joinToString("") { "%02X".format(it) }
        
        assertEquals("Hash in XML does not match data", expectedHash, hash)
    }

    @Test
    fun testCreateBinaryKeyFileDefaultSize() {
        val outputStream = ByteArrayOutputStream()
        MasterCredential.createKeyFile(outputStream, format = MasterCredential.CREATOR.KeyFileFormat.RANDOM_BYTES)
        val binaryData = outputStream.toByteArray()

        assertEquals("Binary key should be 32 bytes", 32, binaryData.size)
    }
}
