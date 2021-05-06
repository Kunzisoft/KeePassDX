/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
 */
package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.Field
import java.util.*
import kotlin.collections.ArrayList

class Template : Parcelable {

    var version = 1
    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    var title = ""
    var icon = IconImage()
    var attributes: List<TemplateAttribute> = ArrayList()
        private set

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                attributes: List<TemplateAttribute>,
                version: Int = 1) {
        this.version = version
        this.uuid = uuid
        this.title = title
        this.icon = icon
        this.attributes = attributes
    }

    constructor(parcel: Parcel) {
        version = parcel.readInt()
        uuid = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)?.uuid ?: uuid
        title = parcel.readString() ?: title
        icon = parcel.readParcelable(IconImage::class.java.classLoader) ?: icon
        parcel.readList(attributes, TemplateAttribute::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(version)
        parcel.writeParcelable(ParcelUuid(uuid), flags)
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeList(attributes)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getFields(): List<Field> {
        return attributes
                .sortedBy { it.position }
                .map { attribute ->
                    val protected = ProtectedString(attribute.type.protected, "")
                    when (attribute.type) {
                        TemplateType.INLINE -> {
                            Field(attribute.title, protected)
                        }
                        else -> {
                            // TODO other types
                            Field(attribute.title, protected)
                        }
                    }
                }
    }

    fun containsAttributeWithTitle(title: String): Boolean {
        return attributes.firstOrNull { attribute -> attribute.title.equals(title, true) } != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Template) return false

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Template> {
        override fun createFromParcel(parcel: Parcel): Template {
            return Template(parcel)
        }

        override fun newArray(size: Int): Array<Template?> {
            return arrayOfNulls(size)
        }

        val STANDARD = Template(DatabaseVersioned.UUID_ZERO, "Standard", IconImage(), ArrayList())
    }
}