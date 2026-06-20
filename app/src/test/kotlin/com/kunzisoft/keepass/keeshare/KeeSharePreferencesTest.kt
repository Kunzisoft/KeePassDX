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
package com.kunzisoft.keepass.keeshare

import com.kunzisoft.keepass.settings.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeeSharePreferencesTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun storeAndRetrieveContainerUri() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val uri = "content://com.android.providers.downloads.documents/tree/raw%3A%2Fsync%2Fshared.kdbx"
        PreferencesUtil.setKeeShareContainerUri(context, uuid, uri)
        assertEquals(uri, PreferencesUtil.getKeeShareContainerUri(context, uuid))
    }

    @Test
    fun returnNullForUnknownGroup() {
        assertNull(PreferencesUtil.getKeeShareContainerUri(context, "nonexistent-uuid"))
    }

    @Test
    fun removeContainerUri() {
        val uuid = "test-uuid"
        PreferencesUtil.setKeeShareContainerUri(context, uuid, "content://test")
        PreferencesUtil.setKeeShareContainerUri(context, uuid, null)
        assertNull(PreferencesUtil.getKeeShareContainerUri(context, uuid))
    }

    @Test
    fun multipleGroupsStoredIndependently() {
        val uuid1 = "uuid-1"
        val uuid2 = "uuid-2"
        PreferencesUtil.setKeeShareContainerUri(context, uuid1, "content://file1")
        PreferencesUtil.setKeeShareContainerUri(context, uuid2, "content://file2")
        assertEquals("content://file1", PreferencesUtil.getKeeShareContainerUri(context, uuid1))
        assertEquals("content://file2", PreferencesUtil.getKeeShareContainerUri(context, uuid2))
    }

    @Test
    fun overwriteExistingUri() {
        val uuid = "overwrite-uuid"
        PreferencesUtil.setKeeShareContainerUri(context, uuid, "content://old")
        PreferencesUtil.setKeeShareContainerUri(context, uuid, "content://new")
        assertEquals("content://new", PreferencesUtil.getKeeShareContainerUri(context, uuid))
    }
}
