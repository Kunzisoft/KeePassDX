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

import com.kunzisoft.keepass.database.keeshare.PerDeviceSyncConfig
import junit.framework.TestCase

class PerDeviceSyncConfigTest : TestCase() {

    fun testContainerFileName() {
        assertEquals("P56IOI7.kdbx", PerDeviceSyncConfig.containerFileName("P56IOI7"))
        assertEquals("XKZU3E4.kdbx", PerDeviceSyncConfig.containerFileName("XKZU3E4"))
        assertEquals("abc1234.kdbx", PerDeviceSyncConfig.containerFileName("abc1234"))
    }

    fun testContainerFileNameSanitizesPathTraversal() {
        // Path separators and special chars are stripped
        assertEquals("etcpasswd.kdbx", PerDeviceSyncConfig.containerFileName("../etc/passwd"))
        assertEquals("ABC.kdbx", PerDeviceSyncConfig.containerFileName("A-B-C"))
    }

    fun testContainerFileNameRejectsEmpty() {
        try {
            PerDeviceSyncConfig.containerFileName("---")
            fail("Should have thrown IllegalArgumentException for all-special-char deviceId")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // Note: roundtrip serialization tests for toCustomData/fromCustomData
    // require Android instrumentation (android.util.Base64) and are covered
    // by androidTest, not unit tests.
}
