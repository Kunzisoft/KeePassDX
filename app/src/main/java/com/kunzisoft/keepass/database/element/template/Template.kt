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
import com.kunzisoft.keepass.database.element.template.TemplateAttribute.CREATOR.OPTION_PASSWORD_GENERATOR
import java.util.*
import kotlin.collections.ArrayList

class Template : Parcelable {

    var version = 1
    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    var title = ""
    var icon = IconImage()
    var sections: MutableList<TemplateSection> = ArrayList()
        private set

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                section: TemplateSection,
                version: Int = 1): this(uuid, title, icon, ArrayList<TemplateSection>().apply {
        add(section)
    }, version)

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                sections: List<TemplateSection>,
                version: Int = 1) {
        this.version = version
        this.uuid = uuid
        this.title = title
        this.icon = icon
        this.sections.clear()
        this.sections.addAll(sections)
        this.sections.add(TemplateSection()) // Add dynamic section at end
    }

    constructor(parcel: Parcel) {
        version = parcel.readInt()
        uuid = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)?.uuid ?: uuid
        title = parcel.readString() ?: title
        icon = parcel.readParcelable(IconImage::class.java.classLoader) ?: icon
        parcel.readList(sections, TemplateSection::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(version)
        parcel.writeParcelable(ParcelUuid(uuid), flags)
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeList(sections)
    }

    override fun describeContents(): Int {
        return 0
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

        const val STANDARD_TITLE = "title"
        const val STANDARD_USERNAME = "username"
        const val STANDARD_PASSWORD = "password"
        const val STANDARD_URL = "url"
        const val STANDARD_EXPIRATION = "expires"
        const val STANDARD_NOTES = "notes"

        val STANDARD: Template
            get() {
                val sections = ArrayList<TemplateSection>()
                val mainSection = TemplateSection(ArrayList<TemplateAttribute>().apply {
                    add(TemplateAttribute(STANDARD_USERNAME, TemplateType.INLINE))
                    add(TemplateAttribute(STANDARD_PASSWORD,
                            TemplateType.INLINE,
                            true,
                            ArrayList<String>().apply { add(OPTION_PASSWORD_GENERATOR) })
                    )
                    add(TemplateAttribute(STANDARD_URL, TemplateType.URL))
                    add(TemplateAttribute(STANDARD_EXPIRATION, TemplateType.DATETIME))
                    add(TemplateAttribute(STANDARD_NOTES, TemplateType.MULTILINE))
                })
                sections.add(mainSection)
                return Template(DatabaseVersioned.UUID_ZERO, "Standard", IconImage(), sections)
            }

        fun isStandardFieldName(name: String): Boolean {
            return ArrayList<String>().apply {
                add(STANDARD_TITLE)
                add(STANDARD_USERNAME)
                add(STANDARD_PASSWORD)
                add(STANDARD_URL)
                add(STANDARD_EXPIRATION)
                add(STANDARD_NOTES)
            }.firstOrNull { it.equals(name, true) } != null
        }
    }
}