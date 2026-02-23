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

import com.kunzisoft.keepass.database.keeshare.DeviceIdentity
import junit.framework.TestCase

class DeviceIdentityTest : TestCase() {

    fun testFallbackDeviceIdLength() {
        val fallbackId = DeviceIdentity.generateFallbackDeviceId()
        assertEquals(7, fallbackId.length)
    }

    fun testFallbackDeviceIdIsUppercase() {
        val fallbackId = DeviceIdentity.generateFallbackDeviceId()
        assertEquals(fallbackId, fallbackId.uppercase())
    }

    fun testFallbackDeviceIdUniqueness() {
        val ids = (1..100).map { DeviceIdentity.generateFallbackDeviceId() }.toSet()
        // With 7 hex chars, collisions in 100 trials are astronomically unlikely
        assertTrue("Expected unique fallback IDs", ids.size > 90)
    }

    fun testFallbackDeviceIdIsAlphanumeric() {
        val fallbackId = DeviceIdentity.generateFallbackDeviceId()
        assertTrue("Expected alphanumeric ID", fallbackId.matches(Regex("[A-Z0-9]+")))
    }
}
