/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePassDX. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.tests.stream

import org.junit.Assert.assertArrayEquals

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Random
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import junit.framework.TestCase

import com.kunzisoft.keepass.stream.HashedBlockInputStream
import com.kunzisoft.keepass.stream.HashedBlockOutputStream

class HashedBlockTest : TestCase() {

    @Throws(IOException::class)
    fun testBlockAligned() {
        testSize(1024, 1024)
    }

    @Throws(IOException::class)
    fun testOffset() {
        testSize(1500, 1024)
    }

    @Throws(IOException::class)
    private fun testSize(blockSize: Int, bufferSize: Int) {
        val orig = ByteArray(blockSize)

        rand.nextBytes(orig)

        val bos = ByteArrayOutputStream()
        val output = HashedBlockOutputStream(bos, bufferSize)
        output.write(orig)
        output.close()

        val encoded = bos.toByteArray()

        val bis = ByteArrayInputStream(encoded)
        val input = HashedBlockInputStream(bis)

        val decoded = ByteArrayOutputStream()
        while (true) {
            val buf = ByteArray(1024)
            val read = input.read(buf)
            if (read == -1) {
                break
            }

            decoded.write(buf, 0, read)
        }

        val out = decoded.toByteArray()

        assertArrayEquals(orig, out)

    }

    @Throws(IOException::class)
    fun testGZIPStream() {
        val testLength = 32000

        val orig = ByteArray(testLength)
        rand.nextBytes(orig)

        val bos = ByteArrayOutputStream()
        val hos = HashedBlockOutputStream(bos)
        val zos = GZIPOutputStream(hos)

        zos.write(orig)
        zos.close()

        val compressed = bos.toByteArray()
        val bis = ByteArrayInputStream(compressed)
        val his = HashedBlockInputStream(bis)
        val zis = GZIPInputStream(his)

        val uncompressed = ByteArray(testLength)

        var read = 0
        while (read != -1 && testLength - read > 0) {
            read += zis.read(uncompressed, read, testLength - read)

        }

        assertArrayEquals("Output not equal to input", orig, uncompressed)


    }

    companion object {

        private val rand = Random()
    }
}
