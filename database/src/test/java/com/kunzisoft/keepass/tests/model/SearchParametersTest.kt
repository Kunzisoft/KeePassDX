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

import com.kunzisoft.keepass.database.search.SearchParameters
import junit.framework.TestCase

class SearchParametersTest : TestCase() {

    fun testSearchParametersCopy() {
        val original = SearchParameters().apply {
            searchQuery = "test query"
            allowEmptyQuery = false
            caseSensitive = true
            isRegex = true
            searchInTitles = false
            searchInUsernames = false
            searchInPasswords = true
            searchInAppIds = false
            searchInUrls = false
            searchByDomain = true
            searchBySubDomain = true
            searchInRelyingParty = true
            credentialIds = listOf("id1", "id2")
            searchInExpired = true
            searchInNotes = false
            searchInOTP = true
            searchInOther = false
            searchInUUIDs = true
            searchInTags = true
            tagsToSearch = listOf("tag1", "tag2")
            searchInCurrentGroup = true
            searchInSearchableGroup = false
            searchInRecycleBin = true
            searchInTemplates = true
        }
        
        val copy = original.copy()
        
        // Check all fields
        assertEquals(original.searchQuery, copy.searchQuery)
        assertEquals(original.allowEmptyQuery, copy.allowEmptyQuery)
        assertEquals(original.caseSensitive, copy.caseSensitive)
        assertEquals(original.isRegex, copy.isRegex)
        assertEquals(original.searchInTitles, copy.searchInTitles)
        assertEquals(original.searchInUsernames, copy.searchInUsernames)
        assertEquals(original.searchInPasswords, copy.searchInPasswords)
        assertEquals(original.searchInAppIds, copy.searchInAppIds)
        assertEquals(original.searchInUrls, copy.searchInUrls)
        assertEquals(original.searchByDomain, copy.searchByDomain)
        assertEquals(original.searchBySubDomain, copy.searchBySubDomain)
        assertEquals(original.searchInRelyingParty, copy.searchInRelyingParty)
        assertEquals(original.credentialIds, copy.credentialIds)
        assertEquals(original.searchInExpired, copy.searchInExpired)
        assertEquals(original.searchInNotes, copy.searchInNotes)
        assertEquals(original.searchInOTP, copy.searchInOTP)
        assertEquals(original.searchInOther, copy.searchInOther)
        assertEquals(original.searchInUUIDs, copy.searchInUUIDs)
        assertEquals(original.searchInTags, copy.searchInTags)
        assertEquals(original.tagsToSearch, copy.tagsToSearch)
        assertEquals(original.searchInCurrentGroup, copy.searchInCurrentGroup)
        assertEquals(original.searchInSearchableGroup, copy.searchInSearchableGroup)
        assertEquals(original.searchInRecycleBin, copy.searchInRecycleBin)
        assertEquals(original.searchInTemplates, copy.searchInTemplates)
        
        // Check references
        assertNotSame(original, copy)
        assertNotSame(original.credentialIds, copy.credentialIds)
        assertNotSame(original.tagsToSearch, copy.tagsToSearch)
        
        // Check deep copy for lists
        val modifiedTags = original.tagsToSearch.toMutableList()
        modifiedTags.add("tag3")
        // This doesn't affect original because it's a new list, but let's re-assign to original
        // SearchParameters lists are immutable (List<String>), so re-assignment is the way
        original.tagsToSearch = modifiedTags
        assertFalse(original.tagsToSearch.size == copy.tagsToSearch.size)
    }
}
