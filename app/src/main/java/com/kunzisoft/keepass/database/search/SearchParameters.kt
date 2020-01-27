/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search

/**
 * Parameters for searching strings in the database.
 */
open class SearchParameters {

    var searchString: String = ""

    var regularExpression = false
    var searchInTitles = true
    var searchInUserNames = true
    var searchInPasswords = false
    var searchInUrls = true
    var searchInGroupNames = false
    var searchInNotes = true
    var ignoreCase = true
    var ignoreExpired = false
    var excludeExpired = false

    constructor()

    /**
     * Copy search parameters
     * @param source
     */
    constructor(source: SearchParameters) {
        regularExpression = source.regularExpression
        searchInTitles = source.searchInTitles
        searchInUserNames = source.searchInUserNames
        searchInPasswords = source.searchInPasswords
        searchInUrls = source.searchInUrls
        searchInGroupNames = source.searchInGroupNames
        searchInNotes = source.searchInNotes
        ignoreCase = source.ignoreCase
        ignoreExpired = source.ignoreExpired
        excludeExpired = source.excludeExpired
    }

    open fun setupNone() {
        searchInTitles = false
        searchInUserNames = false
        searchInPasswords = false
        searchInUrls = false
        searchInGroupNames = false
        searchInNotes = false
    }
}
