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
package com.kunzisoft.keepass.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppOrigin(
    val appIdentifiers: MutableList<AppIdentifier> = mutableListOf(),
    val webDomains: MutableList<String> = mutableListOf()
) : Parcelable {

    fun addIdentifier(appIdentifier: AppIdentifier) {
        appIdentifiers.add(appIdentifier)
    }

    fun addWebDomain(webDomain: String) {
        this.webDomains.add(webDomain)
    }

    fun removeAppElement(appIdentifier: AppIdentifier) {
        appIdentifiers.remove(appIdentifier)
    }

    fun removeWebDomain(webDomain: String) {
        this.webDomains.remove(webDomain)
    }

    fun clear() {
        appIdentifiers.clear()
        webDomains.clear()
    }

    fun isEmpty(): Boolean {
        return appIdentifiers.isEmpty() && webDomains.isEmpty()
    }
}

@Parcelize
data class AppIdentifier(
    val id: String,
    val signature: String? = null,
) : Parcelable