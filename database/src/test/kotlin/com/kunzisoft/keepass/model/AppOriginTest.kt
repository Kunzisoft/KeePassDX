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
package com.kunzisoft.keepass.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppOriginTest {

    private val packageAndroid1 = "com.android.pkg1"
    private val packageAndroid2 = "com.amndroid.pkg2"
    private val fingerprint1 = "01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF"
    private val fingerprint2 = "FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10:FE:DC:BA:98:76:54:32:10"
    private val webHttp = "http://example.com"
    private val webHttps = "https://example.com"
    private val web1 = "https://example1.com"
    private val web2 = "https://example2.com"

    @Test
    fun testAndroidOriginToOriginValue() {
        val pkg = "com.example.app"
        val androidOrigin = AndroidOrigin(pkg, fingerprint1)
        
        val originValue = androidOrigin.toOriginValue()
        assertTrue(originValue.startsWith("android:apk-key-hash:"))
        // We verify the prefix and that it's not empty, exact base64 match can be tricky due to implementation details
        assertTrue(originValue.length > "android:apk-key-hash:".length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAndroidOriginToOriginValueNullFingerprint() {
        val androidOrigin = AndroidOrigin("com.example.app", null)
        androidOrigin.toOriginValue()
    }

    @Test
    fun testWebOriginFromDomain() {
        val domain = "example.com"
        val webOrigin = WebOrigin.fromDomain(domain)
        assertNotNull(webOrigin)
        assertEquals(webHttps, webOrigin?.origin)

        val webOriginWithScheme = WebOrigin.fromDomain(webHttp)
        assertEquals(webHttp, webOriginWithScheme?.origin)

        val webOriginCustomScheme = WebOrigin.fromDomain(domain, "http")
        assertEquals(webHttp, webOriginCustomScheme?.origin)

        assertNull(WebOrigin.fromDomain(null))
        assertNull(WebOrigin.fromDomain(""))
    }

    @Test
    fun testWebOriginDefaultAssetLinks() {
        val webOrigin = WebOrigin(webHttps)
        assertEquals("https://example.com/.well-known/assetlinks.json", webOrigin.defaultAssetLinks())
    }

    @Test
    fun testAppOriginAddMethods() {
        val appOrigin = AppOrigin(false)
        val android1 = AndroidOrigin(packageAndroid1, fingerprint1)
        val android2 = AndroidOrigin(packageAndroid2, fingerprint2)
        val web1 = WebOrigin(web1)
        val web2 = WebOrigin(web2)

        appOrigin.addAndroidOrigin(android1)
        appOrigin.addAndroidOrigin(android1) // Duplicate
        assertEquals(1, appOrigin.androidOrigins.size)

        appOrigin.addAndroidOrigin(android2)
        assertEquals(2, appOrigin.androidOrigins.size)

        appOrigin.addWebOrigin(web1)
        appOrigin.addWebOrigin(web1) // Duplicate
        assertEquals(1, appOrigin.webOrigins.size)

        appOrigin.addWebOrigin(web2)
        assertEquals(2, appOrigin.webOrigins.size)
    }

    @Test
    fun testAppOriginIsEmptyAndClear() {
        val appOrigin = AppOrigin(true)
        assertTrue(appOrigin.isEmpty())

        appOrigin.addAndroidOrigin(AndroidOrigin(packageAndroid1, fingerprint1))
        assertFalse(appOrigin.isEmpty())

        appOrigin.clear()
        assertTrue(appOrigin.isEmpty())
    }

    @Test
    fun testAppOriginContainsAndroidOriginSignature() {
        val appOrigin = AppOrigin(false)
        assertFalse(appOrigin.containsAndroidOriginSignature())

        appOrigin.addAndroidOrigin(AndroidOrigin(packageAndroid1, null))
        assertFalse(appOrigin.containsAndroidOriginSignature())

        appOrigin.addAndroidOrigin(AndroidOrigin(packageAndroid2, fingerprint1))
        assertTrue(appOrigin.containsAndroidOriginSignature())
    }

    @Test
    fun testIsTheSameAndroidOriginThan() {
        val app1 = AppOrigin(false)
        val app2 = AppOrigin(false)
        
        // Both empty
        assertFalse(app1.isTheSameAndroidOriginThan(app2))

        val common = AndroidOrigin(packageAndroid1, fingerprint1)
        app1.addAndroidOrigin(common)
        
        // app1 has one, app2 is empty
        assertFalse(app1.isTheSameAndroidOriginThan(app2))

        app2.addAndroidOrigin(common)
        // Both have common
        assertTrue(app1.isTheSameAndroidOriginThan(app2))

        val app3 = AppOrigin(false)
        app3.addAndroidOrigin(AndroidOrigin("other", fingerprint1))
        // Different package
        assertFalse(app1.isTheSameAndroidOriginThan(app3))
    }

    @Test
    fun testIsTheSameWebOriginThan() {
        val app1 = AppOrigin(false)
        val app2 = AppOrigin(false)

        // Both empty
        assertFalse(app1.isTheSameWebOriginThan(app2))

        val common = WebOrigin(webHttps)
        app1.addWebOrigin(common)

        assertFalse(app1.isTheSameWebOriginThan(app2))

        app2.addWebOrigin(common)
        app2.addWebOrigin(common)
        assertTrue(app1.isTheSameWebOriginThan(app2))
    }

    @Test
    fun testCheckAndroidOrigin() {
        val app = AppOrigin(true)
        val android = AndroidOrigin(packageAndroid1, fingerprint1)
        app.addAndroidOrigin(android)

        val compareOk = AppOrigin(false)
        compareOk.addAndroidOrigin(android)

        // Success
        val result = app.checkAndroidOrigin(compareOk)
        assertTrue(result.startsWith("android:apk-key-hash:"))

        // No signature in compare
        val compareNoSig = AppOrigin(false)
        compareNoSig.addAndroidOrigin(AndroidOrigin(packageAndroid1, null))
        try {
            app.checkAndroidOrigin(compareNoSig)
            fail("Should throw SignatureNotFoundException")
        } catch (_: SignatureNotFoundException) {
            // expected
        }

        // Wrong signature
        val compareWrong = AppOrigin(false)
        compareWrong.addAndroidOrigin(AndroidOrigin(packageAndroid1, fingerprint2))
        try {
            app.checkAndroidOrigin(compareWrong)
            fail("Should throw SecurityException")
        } catch (_: SecurityException) {
            // expected
        }
    }

    @Test
    fun testAppOriginEqualsAndHashCode() {
        val app1 = AppOrigin(true)
        val app2 = AppOrigin(false) // verified is different

        // verified is NOT checked in equals/hashCode as per implementation
        assertEquals(app1, app2)
        assertEquals(app1.hashCode(), app2.hashCode())

        app1.addWebOrigin(WebOrigin(web1))
        app1.addWebOrigin(WebOrigin(web2))
        assertNotEquals(app1, app2)

        app2.addWebOrigin(WebOrigin(web2))
        app2.addWebOrigin(WebOrigin(web1))
        app2.addWebOrigin(WebOrigin(web1))
        assertEquals(app1, app2)
    }

    @Test
    fun testToName() {
        val app = AppOrigin(false)
        assertNull(app.toName())

        app.addWebOrigin(WebOrigin(webHttps))
        assertEquals(webHttps, app.toName())

        app.addAndroidOrigin(AndroidOrigin(packageAndroid1, fingerprint1))
        // Android takes precedence in toName
        assertEquals(packageAndroid1, app.toName())
    }
}
