/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

package com.kunzisoft.keepass.utils

import com.kunzisoft.keepass.stream.uIntTo4Bytes
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset


/**
 * Tools for slicing and dicing Java and KeePass data types.
 */
object StringDatabaseKDBUtils {

    private val defaultCharset = Charset.forName("UTF-8")

    private val CRLFbuf = byteArrayOf(0x0D, 0x0A)
    private val CRLF = String(CRLFbuf)
    private val SEP = System.getProperty("line.separator")
    private val REPLACE = SEP != CRLF

    fun bytesToString(buf: ByteArray, replaceCRLF: Boolean = true): String {
        // length of null-terminated string (i.e. distance to null) within a byte buffer.
        var len = 0
        while (buf[len].toInt() != 0) {
            len++
        }
        // Get string
        var jstring = String(buf, 0, len, defaultCharset)
        if (replaceCRLF && REPLACE) {
            jstring = jstring.replace(CRLF, SEP!!)
        }
        return jstring
    }

    @Throws(IOException::class)
    fun writeStringToBytes(string: String?, os: OutputStream): Int {
        var str = string
        if (str == null) {
            // Write out a null character
            os.write(uIntTo4Bytes(UnsignedInt(1)))
            os.write(0x00)
            return 0
        }

        if (REPLACE) {
            str = str.replace(SEP!!, CRLF)
        }

        val initial = str.toByteArray(defaultCharset)

        val length = initial.size + 1
        os.write(uIntTo4Bytes(UnsignedInt(length)))
        os.write(initial)
        os.write(0x00)

        return length
    }
}
