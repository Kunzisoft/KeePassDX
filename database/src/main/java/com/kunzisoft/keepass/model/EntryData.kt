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

import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import kotlinx.parcelize.Parcelize

/**
 * Data class to transfer simple Entry data from a protocol to another,
 * In particular, it allows for targeted data updates
 */
@Parcelize
data class EntryData(
    val nodeId: EntryId? = null,
    val title: String? = null,
    val iconStandard: Int? = null,
    var username: String = "",
    var password: CharArray? = null,
    var url: String? = null,
    var notes: String? = null,
    var backgroundColor: Int? = null,
    var foregroundColor: Int? = null,
    var customFields: MutableList<Field> = mutableListOf(),
    var autoType: AutoType = AutoType(),
    val creationTime: DateInstant? = null,
    val lastModificationTime: DateInstant? = null,
    val expires: Boolean? = null,
    val expiryTime: DateInstant? = null,
    val tags: List<String> = listOf()
) : Parcelable {
    companion object {

        /**
         * Updates an EntryInfo with data; the UUID is ignored, and only non-null data is updated
         */
        fun EntryInfo.setEntryData(data: EntryData) {
            data.title?.let {
                title = it
            }
            data.iconStandard?.let {
                icon = IconImage(IconImageStandard(it))
            }
            data.username.let {
                username = it
            }
            data.password?.let {
                password = it.copyOf()
            }
            data.url?.let {
                url = it
            }
            data.notes?.let {
                notes = it
            }
            data.backgroundColor?.let {
                backgroundColor = it
            }
            data.foregroundColor?.let {
                foregroundColor = it
            }
            data.customFields.let {
                customFields = it.toMutableList()
            }
            data.autoType.let {
                autoType = AutoType(it)
            }
            data.creationTime?.let {
                creationTime = DateInstant(it)
            }
            data.lastModificationTime?.let {
                lastModificationTime = DateInstant(it)
            }
            data.expires?.let {
                expires = it
            }
            data.expiryTime?.let {
                expiryTime = DateInstant(it)
            }
            data.tags.let {
                tags = Tags(tags)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntryData

        if (backgroundColor != other.backgroundColor) return false
        if (foregroundColor != other.foregroundColor) return false
        if (expires != other.expires) return false
        if (title != other.title) return false
        if (iconStandard != other.iconStandard) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (url != other.url) return false
        if (notes != other.notes) return false
        if (customFields != other.customFields) return false
        if (autoType != other.autoType) return false
        if (creationTime != other.creationTime) return false
        if (lastModificationTime != other.lastModificationTime) return false
        if (expiryTime != other.expiryTime) return false
        if (tags != other.tags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor ?: 0
        result = 31 * result + (foregroundColor ?: 0)
        result = 31 * result + expires.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (iconStandard?.hashCode() ?: 0)
        result = 31 * result + username.hashCode()
        result = 31 * result + (password?.contentHashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + customFields.hashCode()
        result = 31 * result + autoType.hashCode()
        result = 31 * result + (creationTime?.hashCode() ?: 0)
        result = 31 * result + (lastModificationTime?.hashCode() ?: 0)
        result = 31 * result + (expiryTime?.hashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        return result
    }

    override fun toString(): String {
        return title ?: username
    }
}