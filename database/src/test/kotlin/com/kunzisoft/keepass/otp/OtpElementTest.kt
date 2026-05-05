package com.kunzisoft.keepass.otp

import com.kunzisoft.keepass.utils.CodecUtil
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class OtpElementTest {

    @Test
    fun testHexSecret() {
        val otpElement = OtpElement()
        // "Hello" in hex
        val hexSecret = "48656c6c6f".toCharArray()
        otpElement.setHexSecret(hexSecret)
        
        val expected = "Hello".toByteArray()
        assertArrayEquals(expected, otpElement.otpModel.secret)
    }

    @Test
    fun testBase32Secret() {
        val otpElement = OtpElement()
        // "Hello" in Base32 is JBSWY3DP
        val base32Secret = "JBSWY3DP".toCharArray()
        otpElement.setBase32Secret(base32Secret)
        
        val expected = "Hello".toByteArray()
        assertArrayEquals(expected, otpElement.otpModel.secret)
        
        assertArrayEquals(base32Secret, otpElement.getBase32Secret())
    }

    @Test
    fun testBase32SecretNoPadding() {
        val otpElement = OtpElement()
        // "Hello" in Base32 is JBSWY3DP
        val base32Secret = "JBSWY3DP".toCharArray()
        otpElement.setBase32Secret(base32Secret)

        // getBase32Secret returns no padding
        assertArrayEquals("JBSWY3DP".toCharArray(), otpElement.getBase32Secret())
    }

    @Test
    fun testBase32SecretLowercaseAndSpaces() {
        val otpElement = OtpElement()
        val base32Secret = "jbsw y3dp".toCharArray()
        otpElement.setBase32Secret(base32Secret)
        
        val expected = "Hello".toByteArray()
        assertArrayEquals(expected, otpElement.otpModel.secret)
    }

    @Test
    fun testCodecUtilBase32() {
        val data = "KeePassDX".toByteArray()
        val encoded = CodecUtil.encodeBase32(data)
        val decoded = CodecUtil.decodeBase32(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test
    fun testCodecUtilHex() {
        val hex = "ABCDEF0123456789".toCharArray()
        val decoded = CodecUtil.decodeHex(hex)
        val expected = byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01.toByte(), 0x23.toByte(), 0x45.toByte(), 0x67.toByte(), 0x89.toByte())
        assertArrayEquals(expected, decoded)
    }
}
