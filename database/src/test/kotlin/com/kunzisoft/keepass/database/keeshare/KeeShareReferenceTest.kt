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
package com.kunzisoft.keepass.database.keeshare

import android.util.Base64
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.CustomDataItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeeShareReferenceTest {

    @Test
    fun parseImportOnlyReference() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <KeeShare>
                <Type><Import/></Type>
                <Group>AAAAAAAAAAAAAAAAAAAAAA==</Group>
                <Path>${Base64.encodeToString("/shared/passwords.kdbx".toByteArray(), Base64.NO_WRAP)}</Path>
                <Password>${Base64.encodeToString("secret".toByteArray(), Base64.NO_WRAP)}</Password>
                <KeepGroups>False</KeepGroups>
            </KeeShare>
        """.trimIndent()
        val encoded = Base64.encodeToString(xml.toByteArray(), Base64.NO_WRAP)

        val ref = KeeShareReference.deserialize(encoded)
        assertNotNull(ref)
        assertEquals(KeeShareReference.IMPORT_FROM, ref!!.type)
        assertTrue(ref.isImporting)
        assertFalse(ref.isExporting)
        assertEquals("/shared/passwords.kdbx", ref.path)
        assertEquals("secret", ref.password)
        assertFalse(ref.keepGroups)
    }

    @Test
    fun parseSynchronizeReference() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <KeeShare>
                <Type><Import/><Export/></Type>
                <Group>AAAAAAAAAAAAAAAAAAAAAA==</Group>
                <Path>${Base64.encodeToString("/sync/db.kdbx".toByteArray(), Base64.NO_WRAP)}</Path>
                <Password></Password>
                <KeepGroups>True</KeepGroups>
            </KeeShare>
        """.trimIndent()
        val encoded = Base64.encodeToString(xml.toByteArray(), Base64.NO_WRAP)

        val ref = KeeShareReference.deserialize(encoded)
        assertNotNull(ref)
        assertEquals(KeeShareReference.SYNCHRONIZE, ref!!.type)
        assertTrue(ref.isImporting)
        assertTrue(ref.isExporting)
        assertTrue(ref.keepGroups)
    }

    @Test
    fun parseLegacyIntegerType() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <KeeShare>
                <Type>3</Type>
                <Group>AAAAAAAAAAAAAAAAAAAAAA==</Group>
                <Path>${Base64.encodeToString("/path".toByteArray(), Base64.NO_WRAP)}</Path>
                <Password></Password>
            </KeeShare>
        """.trimIndent()
        val encoded = Base64.encodeToString(xml.toByteArray(), Base64.NO_WRAP)

        val ref = KeeShareReference.deserialize(encoded)
        assertNotNull(ref)
        assertEquals(KeeShareReference.SYNCHRONIZE, ref!!.type)
    }

    @Test
    fun returnNullForInactiveEmptyPath() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <KeeShare>
                <Type>0</Type>
                <Group>AAAAAAAAAAAAAAAAAAAAAA==</Group>
                <Path></Path>
                <Password></Password>
            </KeeShare>
        """.trimIndent()
        val encoded = Base64.encodeToString(xml.toByteArray(), Base64.NO_WRAP)

        assertNull(KeeShareReference.deserialize(encoded))
    }

    @Test
    fun returnNullForGarbage() {
        assertNull(KeeShareReference.deserialize("not-valid-at-all!!!"))
    }

    @Test
    fun fromCustomDataFindsReference() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <KeeShare>
                <Type><Import/></Type>
                <Group>AAAAAAAAAAAAAAAAAAAAAA==</Group>
                <Path>${Base64.encodeToString("/test".toByteArray(), Base64.NO_WRAP)}</Path>
                <Password></Password>
            </KeeShare>
        """.trimIndent()
        val encoded = Base64.encodeToString(xml.toByteArray(), Base64.NO_WRAP)

        val customData = CustomData()
        customData.put(CustomDataItem(KeeShareReference.CUSTOM_DATA_KEY, encoded))
        val ref = KeeShareReference.fromCustomData(customData)
        assertNotNull(ref)
        assertEquals("/test", ref!!.path)
    }

    @Test
    fun fromCustomDataReturnsNullWhenMissing() {
        assertNull(KeeShareReference.fromCustomData(CustomData()))
    }

    @Test
    fun roundTripSerializeDeserialize() {
        val original = KeeShareReference(
            type = KeeShareReference.SYNCHRONIZE,
            path = "/shared/db.kdbx",
            password = "mypass",
            keepGroups = true
        )
        val serialized = original.serialize()
        val parsed = KeeShareReference.deserialize(serialized)
        assertNotNull(parsed)
        assertEquals(original.type, parsed!!.type)
        assertEquals(original.path, parsed.path)
        assertEquals(original.password, parsed.password)
        assertEquals(original.keepGroups, parsed.keepGroups)
    }
}
