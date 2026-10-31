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

import com.kunzisoft.keepass.model.SearchInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchInfoTest {

    @Test
    fun testWebDomainRegexValid() {
        val validDomains = listOf(
            "example.com",
            "https://example.com",
            "http://example.com",
            "ftp://example.com",
            "sub.example.com",
            "a.b.c.example.com",
            "example.co.uk",
            "example.com.au",
            "localhost",
            "a.com",
            "example.photography",
            "example.international",
            "example.xn--p1ai",
            "my-domain.com",
            "example.com:8080",
            "http://localhost:3000"
        )

        for (domain in validDomains) {
            val searchInfo = SearchInfo().apply {
                webDomain = domain
            }
            assertEquals("Domain should be valid: $domain", domain, searchInfo.webDomain)
            assertTrue("isDomainSearch should be true for: $domain", searchInfo.isDomainSearch)
        }
    }

    @Test
    fun testWebDomainRegexInvalid() {
        val invalidDomains = listOf(
            "",
            "not a domain",
            "http://",
            "https://",
            ".com",
            "example.",
            "http://.com",
            "example..com",
            "-example.com",
            "example-.com",
            "a"
        )

        for (domain in invalidDomains) {
            val searchInfo = SearchInfo().apply {
                webDomain = domain
            }
            assertNull("Domain should be invalid (result in null): '$domain'", searchInfo.webDomain)
            assertFalse("isDomainSearch should be false for: '$domain'", searchInfo.isDomainSearch)
        }

        val nullSearchInfo = SearchInfo().apply {
            webDomain = null
        }
        assertNull(nullSearchInfo.webDomain)
        assertFalse(nullSearchInfo.isDomainSearch)
    }

    @Test
    fun testWebIpRegexValid() {
        val validIps = listOf(
            "127.0.0.1",
            "192.168.1.1",
            "0.0.0.0",
            "255.255.255.255",
            "2001:db8::1",
            "::1",
            "2001:db8:0:0:0:0:2c6:57ab"
        )

        for (ip in validIps) {
            val searchInfo = SearchInfo().apply {
                webDomain = ip
            }
            assertEquals("IP should be valid as webDomain: $ip", ip, searchInfo.webDomain)
            assertTrue("isDomainSearch should be true for IP: $ip", searchInfo.isDomainSearch)
        }
    }

    @Test
    fun testWebIpRegexInvalid() {
        val invalidIps = listOf(
            "256.0.0.1",
            "192.168.1",
            "192.168.1.1.1"
        )

        for (ip in invalidIps) {
            val searchInfo = SearchInfo().apply {
                webDomain = ip
            }
            assertNull("IP should be invalid: $ip", searchInfo.webDomain)
        }
    }

    @Test
    fun testApplicationIdValidation() {
        val validAppIds = listOf(
            "com.example.app",
            "com.example.app123",
            "app.sub.name",
            "com.example_app.sub_pkg"
        )
        for (appId in validAppIds) {
            val searchInfo = SearchInfo().apply {
                applicationId = appId
            }
            assertEquals(appId, searchInfo.applicationId)
            assertTrue(searchInfo.isAppIdSearch)
        }

        val invalidAppIds = listOf(
            "",
            "invalid",
            "123.app",
            "com.example.-app",
            "com.example.",
            ".example.app",
            "com..example",
            "com"
        )
        for (appId in invalidAppIds) {
            val searchInfo = SearchInfo().apply {
                applicationId = appId
            }
            assertNull(searchInfo.applicationId)
            assertFalse(searchInfo.isAppIdSearch)
        }
    }

    @Test
    fun testContainsOnlyNullValues() {
        val searchInfo = SearchInfo()
        assertTrue(searchInfo.containsOnlyNullValues())

        searchInfo.webDomain = "example.com"
        assertFalse(searchInfo.containsOnlyNullValues())

        val searchInfo2 = SearchInfo().apply {
            tags = listOf("tag1")
        }
        assertFalse(searchInfo2.containsOnlyNullValues())
    }

    @Test
    fun testCopyConstructor() {
        val original = SearchInfo().apply {
            manualSelection = true
            isTagSearch = true
            tags = listOf("tag1")
            applicationId = "com.example.app"
            webDomain = "example.com"
            relyingParty = "rp"
            credentialIds = listOf("id1")
            otpString = "otp"
        }

        val copy = SearchInfo(original)
        assertEquals(original, copy)
        assertNotSame(original, copy)
    }
}
