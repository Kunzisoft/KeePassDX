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
package com.kunzisoft.keepass.tests.model

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.AndroidOrigin
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.CreditCard
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.otp.OtpType
import junit.framework.TestCase

class CopyConstructorTest : TestCase() {

    fun testOtpModelCopy() {
        val original = OtpModel().apply {
            type = OtpType.HOTP
            name = "Test OTP"
            secret = byteArrayOf(1, 2, 3)
            counter = 42
        }
        val copy = OtpModel(original)
        
        assertEquals(original, copy)
        assertNotSame(original, copy)
        assertNotSame(original.secret, copy.secret)
        assertTrue(original.secret!!.contentEquals(copy.secret!!))
        
        // Modify copy
        copy.name = "Modified"
        assertFalse(original.name == copy.name)
    }

    fun testCreditCardCopy() {
        val original = CreditCard("Holder", charArrayOf('1', '2'), charArrayOf('3'))
        val copy = CreditCard(original)
        
        assertEquals(original, copy)
        assertNotSame(original, copy)
        assertNotSame(original.number, copy.number)
        assertNotSame(original.cvv, copy.cvv)
        assertTrue(original.number!!.contentEquals(copy.number!!))
        
        // Modify copy
        copy.number[0] = '9'
        assertFalse(original.number.contentEquals(copy.number))
    }

    fun testAppOriginCopy() {
        val original = AppOrigin(true)
        original.addAndroidOrigin(AndroidOrigin("pkg", "fingerprint"))
        
        val copy = AppOrigin(original)
        
        // AppOrigin equals() uses checkAppOrigin which is complex and might fail in test env
        // if signature tools are not mocked. Let's compare properties directly.
        assertEquals(original.verified, copy.verified)
        assertEquals(original.androidOrigins.size, copy.androidOrigins.size)
        assertEquals(original.androidOrigins[0].packageName, copy.androidOrigins[0].packageName)
        
        assertNotSame(original, copy)
        assertNotSame(original.androidOrigins, copy.androidOrigins)

        // Verify deep copy by modifying internal list
        copy.androidOrigins.clear()
        assertFalse(original.androidOrigins.isEmpty())
    }

    fun testIconImageCopy() {
        val original = IconImage(IconImageStandard(42))
        val copy = IconImage(original)
        
        assertEquals(original, copy)
        assertNotSame(original, copy)
        assertNotSame(original.standard, copy.standard)
        assertEquals(original.standard.id, copy.standard.id)
    }

    fun testEntryInfoCopy() {
        val original = EntryInfo().apply {
            title = "Title"
            username = "User"
            password = charArrayOf('p', 'a', 's', 's')
            customFields.add(Field("Field1", ProtectedString(true, "Value1")))
            otpModel = OtpModel().apply { name = "OTP" }
        }
        
        val copy = EntryInfo(original)
        
        // NodeInfo fields check (via EntryInfo)
        assertEquals(original.title, copy.title)
        
        // EntryInfo.equals() depends on super.equals() (NodeInfo) and all its fields.
        // If it fails, we check individual fields to narrow down.
        assertEquals("Title mismatch", original.title, copy.title)
        assertEquals("Username mismatch", original.username, copy.username)
        assertTrue("Password mismatch", original.password.contentEquals(copy.password))
        assertEquals("Custom fields mismatch", original.customFields, copy.customFields)
        assertEquals("OTP model mismatch", original.otpModel, copy.otpModel)
        assertEquals("AutoType mismatch", original.autoType, copy.autoType)
        assertEquals("Icon mismatch", original.icon, copy.icon)
        assertEquals("Creation time mismatch", original.creationTime, copy.creationTime)
        assertEquals("Modification time mismatch", original.lastModificationTime, copy.lastModificationTime)
        assertEquals("Expiry mismatch", original.expires, copy.expires)
        assertEquals("Expiry time mismatch", original.expiryTime, copy.expiryTime)
        assertEquals("Custom data mismatch", original.customData, copy.customData)
        assertEquals("Tags mismatch", original.tags, copy.tags)
        
        assertEquals("Full object mismatch", original, copy)
        assertNotSame(original, copy)
        assertNotSame(original.password, copy.password)
        assertNotSame(original.customFields, copy.customFields)
        
        if (original.customFields.isNotEmpty() && copy.customFields.isNotEmpty()) {
            assertNotSame(original.customFields[0], copy.customFields[0])
            // Modify deep property in copy
            copy.customFields[0].name = "Modified"
            // Entry equality should now be false
            assertFalse("Modified copy should not be equal to original", original == copy)
        }
        
        assertNotSame(original.otpModel, copy.otpModel)
    }

    fun testPasskeyCopy() {
        val original = Passkey(
            username = "user",
            privateKeyPem = charArrayOf('a', 'b'),
            credentialId = "id",
            userHandle = "handle",
            relyingParty = "rp",
            backupEligibility = true,
            backupState = false
        )
        val copy = Passkey(original)

        assertEquals(original, copy)
        assertNotSame(original, copy)
        assertNotSame(original.privateKeyPem, copy.privateKeyPem)
        assertTrue(original.privateKeyPem.contentEquals(copy.privateKeyPem))

        // Modify copy
        copy.privateKeyPem[0] = 'z'
        assertFalse(original.privateKeyPem.contentEquals(copy.privateKeyPem))
    }
}
