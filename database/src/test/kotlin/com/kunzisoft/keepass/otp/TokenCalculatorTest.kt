/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenCalculatorTest {

    private val rfc4226Secret = "12345678901234567890".toByteArray()

    @Test
    fun testGetHotpToken() {
        // Test vectors from RFC 4226
        assertArrayEquals("755224".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 0, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("287082".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 1, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("359152".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 2, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("969429".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 3, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("338314".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 4, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("254676".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 5, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("287922".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 6, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("162583".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 7, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("399871".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 8, 6, TokenCalculator.HashAlgorithm.SHA1))
        assertArrayEquals("520489".toCharArray(), TokenCalculator.getHotpToken(rfc4226Secret, 9, 6, TokenCalculator.HashAlgorithm.SHA1))
    }

    @Test
    fun testTotp_RFC6238() {
        // Test vectors from RFC 6238 (using T=59, T0=0, Step=30 -> Counter=1)
        // With digits=8, it should be 94287082. With digits=6, it should be 287082.
        val time = 59L
        val digits = 8
        val result = TokenCalculator.getTotpRfc6238Token(rfc4226Secret, 30, time, digits, TokenCalculator.HashAlgorithm.SHA1)
        assertEquals(94287082, result)

        val token = TokenCalculator.formatTokenString(result, digits)
        assertArrayEquals("94287082".toCharArray(), token)
    }

    @Test
    fun testFormatTokenString() {
        assertArrayEquals("000123".toCharArray(), TokenCalculator.formatTokenString(123, 6))
        assertArrayEquals("123456".toCharArray(), TokenCalculator.formatTokenString(123456, 6))
        assertArrayEquals("00000".toCharArray(), TokenCalculator.formatTokenString(0, 5))
    }

    @Test
    fun testHashAlgorithmFromString() {
        assertEquals(TokenCalculator.HashAlgorithm.SHA1, TokenCalculator.HashAlgorithm.fromString("sha1"))
        assertEquals(TokenCalculator.HashAlgorithm.SHA256, TokenCalculator.HashAlgorithm.fromString("SHA-256"))
        assertEquals(TokenCalculator.HashAlgorithm.SHA512, TokenCalculator.HashAlgorithm.fromString("sha_512"))
        assertEquals(TokenCalculator.HashAlgorithm.SHA1, TokenCalculator.HashAlgorithm.fromString("invalid"))
    }

    @Test
    fun testSteamToken() {
        // Steam tokens use a different character set and digits=5 usually.
        // We just verify it returns 5 characters from the STEAMCHARS set.
        val secret = "test-secret".toByteArray()
        val token = TokenCalculator.getTotpSteamToken(secret, 30, 5, TokenCalculator.HashAlgorithm.SHA1)
        assertEquals(5, token.size)
        val steamChars = "23456789BCDFGHJKMNPQRTVWXY"
        for (c in token) {
            assert(steamChars.contains(c))
        }
    }
}
