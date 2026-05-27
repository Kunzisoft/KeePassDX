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

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeTimeInterface
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat

abstract class NodeInfo() : NodeTimeInterface, Parcelable {

    abstract val nodeId: NodeId<*>
    var title: String = ""
    var icon: IconImage = IconImage()
    override var creationTime: DateInstant = DateInstant()
    override var lastModificationTime: DateInstant = DateInstant()
    override var lastAccessTime: DateInstant = DateInstant()
    override var expires: Boolean = false
    override var expiryTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME
    var customData: CustomData = CustomData()
    var tags: Tags = Tags()

    constructor(nodeToCopy: NodeInfo) : this() {
        this.title = nodeToCopy.title
        this.icon = IconImage(nodeToCopy.icon)
        this.creationTime = DateInstant(nodeToCopy.creationTime)
        this.lastModificationTime = DateInstant(nodeToCopy.lastModificationTime)
        this.lastAccessTime = DateInstant(nodeToCopy.lastAccessTime)
        this.expires = nodeToCopy.expires
        this.expiryTime = DateInstant(nodeToCopy.expiryTime)
        this.customData = CustomData(nodeToCopy.customData)
        this.tags = Tags(nodeToCopy.tags)
    }

    constructor(parcel: Parcel) : this() {
        title = parcel.readString() ?: title
        icon = parcel.readParcelableCompat() ?: icon
        creationTime = parcel.readParcelableCompat() ?: creationTime
        lastModificationTime = parcel.readParcelableCompat() ?: lastModificationTime
        lastAccessTime = parcel.readParcelableCompat() ?: lastAccessTime
        expires = parcel.readBooleanCompat()
        expiryTime = parcel.readParcelableCompat() ?: expiryTime
        customData = parcel.readParcelableCompat() ?: customData
        tags = parcel.readParcelableCompat() ?: tags
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(creationTime, flags)
        parcel.writeParcelable(lastModificationTime, flags)
        parcel.writeParcelable(lastAccessTime, flags)
        parcel.writeBooleanCompat(expires)
        parcel.writeParcelable(expiryTime, flags)
        parcel.writeParcelable(customData, flags)
        parcel.writeParcelable(tags, flags)
    }

    override val isCurrentlyExpires: Boolean =
        expires && expiryTime.isCurrentlyExpire()

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeInfo) return false

        if (title != other.title) return false
        if (icon != other.icon) return false
        if (creationTime != other.creationTime) return false
        if (lastModificationTime != other.lastModificationTime) return false
        if (expires != other.expires) return false
        if (expiryTime != other.expiryTime) return false
        if (customData != other.customData) return false
        if (tags != other.tags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + lastModificationTime.hashCode()
        result = 31 * result + expires.hashCode()
        result = 31 * result + expiryTime.hashCode()
        result = 31 * result + customData.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }
}