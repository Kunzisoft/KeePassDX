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
package com.kunzisoft.keepass.tests.keeshare

import com.kunzisoft.keepass.database.keeshare.KeeShareContainer
import junit.framework.TestCase
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

class KeeShareContainerTest : TestCase() {

    fun testDetectUnsignedKdbxFormat() {
        // KDBX signature: 0x9AA2D903 in little-endian, followed by sig2
        val kdbxHeader = byteArrayOf(
            0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte(), // sig1: 0x9AA2D903
            0x67, 0xFB.toByte(), 0x4B, 0xB5.toByte()          // sig2: 0xB54BFB67
        )
        val stream = BufferedInputStream(ByteArrayInputStream(kdbxHeader))
        assertEquals(KeeShareContainer.ContainerType.UNSIGNED_KDBX,
            KeeShareContainer.detectFormat(stream))
    }

    fun testDetectSignedZipFormat() {
        // ZIP local file header: PK\x03\x04
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00)
        val stream = BufferedInputStream(ByteArrayInputStream(zipHeader))
        assertEquals(KeeShareContainer.ContainerType.SIGNED_ZIP,
            KeeShareContainer.detectFormat(stream))
    }

    fun testDetectUnknownFormat() {
        val randomData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val stream = BufferedInputStream(ByteArrayInputStream(randomData))
        assertEquals(KeeShareContainer.ContainerType.UNKNOWN,
            KeeShareContainer.detectFormat(stream))
    }

    fun testDetectEmptyStream() {
        val stream = BufferedInputStream(ByteArrayInputStream(ByteArray(0)))
        assertEquals(KeeShareContainer.ContainerType.UNKNOWN,
            KeeShareContainer.detectFormat(stream))
    }

    fun testDetectFormatDoesNotConsumeStream() {
        val kdbxHeader = byteArrayOf(
            0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte(),
            0x67, 0xFB.toByte(), 0x4B, 0xB5.toByte()
        )
        val stream = BufferedInputStream(ByteArrayInputStream(kdbxHeader))

        // Detect format
        KeeShareContainer.detectFormat(stream)

        // Stream should still be at position 0 (reset after detection)
        val firstByte = stream.read()
        assertEquals(0x03, firstByte)
    }

    fun testDetectTooShortStream() {
        val shortData = byteArrayOf(0x03, 0xD9.toByte())
        val stream = BufferedInputStream(ByteArrayInputStream(shortData))
        assertEquals(KeeShareContainer.ContainerType.UNKNOWN,
            KeeShareContainer.detectFormat(stream))
    }
}
