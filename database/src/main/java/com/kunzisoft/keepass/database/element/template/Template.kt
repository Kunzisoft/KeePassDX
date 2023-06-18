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
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import java.util.*
import kotlin.collections.ArrayList

class Template : Parcelable {

    var version = 1
    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    var title = ""
    var icon = IconImage()
    var backgroundColor: Int? = null
    var foregroundColor: Int? = null
    var sections: MutableList<TemplateSection> = ArrayList()
        private set

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                section: TemplateSection,
                version: Int = 1)
            : this(uuid, title, icon, ArrayList<TemplateSection>().apply {
        add(section)
    }, version)

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                sections: List<TemplateSection>,
                version: Int = 1)
            : this(uuid, title, icon, null, null, sections, version)

    constructor(uuid: UUID,
                title: String,
                icon: IconImage,
                backgroundColor: Int?,
                foregroundColor: Int?,
                sections: List<TemplateSection>,
                version: Int = 1) {
        this.version = version
        this.uuid = uuid
        this.title = title
        this.icon = icon
        this.backgroundColor = backgroundColor
        this.foregroundColor = foregroundColor
        this.sections.clear()
        this.sections.addAll(sections)
    }

    constructor(template: Template) {
        this.version = template.version
        this.uuid = template.uuid
        this.title = template.title
        this.icon = template.icon
        this.backgroundColor = template.backgroundColor
        this.foregroundColor = template.foregroundColor
        this.sections.clear()
        this.sections.addAll(template.sections)
    }

    constructor(parcel: Parcel) {
        version = parcel.readInt()
        uuid = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: uuid
        title = parcel.readString() ?: title
        icon = parcel.readParcelableCompat() ?: icon
        backgroundColor = parcel.readInt()
        foregroundColor = parcel.readInt()
        parcel.readListCompat(sections)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(version)
        parcel.writeParcelable(ParcelUuid(uuid), flags)
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeInt(backgroundColor ?: -1)
        parcel.writeInt(foregroundColor ?: -1)
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

        val TITLE_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_TITLE,
            TemplateAttributeType.TEXT)
        val USERNAME_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_USERNAME,
            TemplateAttributeType.TEXT)
        val PASSWORD_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_PASSWORD,
            TemplateAttributeType.TEXT,
            true,
            TemplateAttributeOption().apply {
                setNumberLines(3)
                associatePasswordGenerator()
            }
        )
        val URL_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_URL,
            TemplateAttributeType.TEXT,
            false,
            TemplateAttributeOption().apply {
                setLink(true)
            })
        val EXPIRATION_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_EXPIRATION,
            TemplateAttributeType.DATETIME,
            false)
        val NOTES_ATTRIBUTE = TemplateAttribute(
            TemplateField.LABEL_NOTES,
            TemplateAttributeType.TEXT,
            false,
            TemplateAttributeOption().apply {
                setNumberLinesToMany()
            })

        val STANDARD: Template
            get() {
                val sections = mutableListOf<TemplateSection>()
                val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                    add(USERNAME_ATTRIBUTE)
                    add(PASSWORD_ATTRIBUTE)
                    add(URL_ATTRIBUTE)
                    add(EXPIRATION_ATTRIBUTE)
                    add(NOTES_ATTRIBUTE)
                })
                sections.add(mainSection)
                return Template(
                    DatabaseVersioned.UUID_ZERO,
                    TemplateField.LABEL_STANDARD,
                    IconImage(),
                    sections)
            }
    }
}