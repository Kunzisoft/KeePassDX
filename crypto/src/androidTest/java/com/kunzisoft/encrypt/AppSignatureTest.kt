/*
 * Copyright 202 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.encrypt

import com.kunzisoft.encrypt.HashManager.fingerprintToUrlSafeBase64
import org.junit.Assert
import org.junit.Test

class AppSignatureTest {

    @Test
    fun testSingleSignature() {
        // Generate random input
        val fingerprint = "A7:5C:63:72:A0:B6:7D:B0:16:86:B4:7D:F6:8C:91:51:6E:E1:62:29:EE:C4:C0:C6:7D:35:5E:32:20:7C:66:17"
        val expected = "p1xjcqC2fbAWhrR99oyRUW7hYinuxMDGfTVeMiB8Zhc"

        Assert.assertEquals("Check fingerprint app", expected, fingerprintToUrlSafeBase64(fingerprint))
    }

    @Test
    fun testMultipleSignature() {
        // Generate random input
        val fingerprint = "A7:5C:63:72:A0:B6:7D:B0:16:86:B4:7D:F6:8C:91:51:6E:E1:62:29:EE:C4:C0:C6:7D:35:5E:32:20:7C:66:17##SIG##DB:25:8A:A6:19:08:9B:D1:3D:BA:71:9E:5A:DA:EC:FF:7F:12:C8:8F:67:AD:68:3C:1F:BC:F2:28:B3:88:BD:91"
        val expected = "p1xjcqC2fbAWhrR99oyRUW7hYinuxMDGfTVeMiB8Zhc"

        Assert.assertEquals("Check fingerprint app", expected, fingerprintToUrlSafeBase64(fingerprint))
    }
}
