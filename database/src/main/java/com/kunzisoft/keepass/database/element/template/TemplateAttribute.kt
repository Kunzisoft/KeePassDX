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
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum

data class TemplateAttribute(var label: String,
                             var type: TemplateAttributeType,
                             var protected: Boolean = false,
                             var options: TemplateAttributeOption = TemplateAttributeOption(),
                             var action: TemplateAttributeAction = TemplateAttributeAction.NONE): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readEnum<TemplateAttributeType>() ?: TemplateAttributeType.TEXT,
        parcel.readByte() != 0.toByte(),
        parcel.readParcelableCompat() ?: TemplateAttributeOption(),
        parcel.readEnum<TemplateAttributeAction>() ?: TemplateAttributeAction.NONE)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeEnum(type)
        parcel.writeByte(if (protected) 1 else 0)
        parcel.writeParcelable(options, flags)
        parcel.writeEnum(action)
    }

    override fun describeContents(): Int {
        return 0
    }

    var alias: String?
        get() {
            return options.alias
        }
        set(value) {
            options.alias = value
        }

    var default: String
        get() {
            return this.options.default
        }
        set(value) {
            this.options.default = value
        }

    companion object CREATOR : Parcelable.Creator<TemplateAttribute> {
        override fun createFromParcel(parcel: Parcel): TemplateAttribute {
            return TemplateAttribute(parcel)
        }

        override fun newArray(size: Int): Array<TemplateAttribute?> {
            return arrayOfNulls(size)
        }
    }
}