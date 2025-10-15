/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.tests.utils

import com.kunzisoft.keepass.utils.UUIDUtils.asBytes
import com.kunzisoft.keepass.utils.UUIDUtils.asHexString
import com.kunzisoft.keepass.utils.UUIDUtils.asUUID
import junit.framework.TestCase
import java.util.UUID

class UUIDTest: TestCase() {

    fun testUUIDHexString() {
        val randomUUID = UUID.randomUUID()
        val hexStringUUID = randomUUID.asHexString()
        val retrievedUUID = hexStringUUID?.asUUID()
        assertEquals(randomUUID, retrievedUUID)
    }

    fun testUUIDString() {
        val staticUUID = "4be0643f-1d98-573b-97cd-ca98a65347dd"
        val stringUUID = UUID.fromString(staticUUID).asBytes().asUUID().toString()
        assertEquals(staticUUID, stringUUID)
    }

    fun testUUIDBytes() {
        val randomUUID = UUID.randomUUID()
        val byteArrayUUID = randomUUID.asBytes()
        val retrievedUUID = byteArrayUUID.asUUID()
        assertEquals(randomUUID, retrievedUUID)
    }
}