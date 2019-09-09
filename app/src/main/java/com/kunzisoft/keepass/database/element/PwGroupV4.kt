/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable

import java.util.HashMap
import java.util.UUID

class PwGroupV4 : PwGroup<UUID, PwGroupV4, PwEntryV4>, NodeV4Interface {

    // TODO Encapsulate
    override var icon: PwIcon
        get() {
            return if (iconCustom.isUnknown)
                super.icon
            else
                iconCustom
        }
        set(value) {
            if (value is PwIconStandard)
                iconCustom = PwIconCustom.UNKNOWN_ICON
            super.icon = value
        }
    var iconCustom = PwIconCustom.UNKNOWN_ICON
    private val customData = HashMap<String, String>()
    var notes = ""
    var isExpanded = true
    var defaultAutoTypeSequence = ""
    var enableAutoType: Boolean? = null
    var enableSearching: Boolean? = null
    var lastTopVisibleEntry: UUID = PwDatabase.UUID_ZERO

    override val type: Type
        get() = Type.GROUP

    override fun initNodeId(): PwNodeId<UUID> {
        return PwNodeIdUUID()
    }

    override fun copyNodeId(nodeId: PwNodeId<UUID>): PwNodeId<UUID> {
        return PwNodeIdUUID(nodeId.id)
    }

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        iconCustom = parcel.readParcelable(PwIconCustom::class.java.classLoader)
        usageCount = parcel.readLong()
        locationChanged = parcel.readParcelable(PwDate::class.java.classLoader)
        // TODO customData = MemoryUtil.readStringParcelableMap(in);
        notes = parcel.readString()
        isExpanded = parcel.readByte().toInt() != 0
        defaultAutoTypeSequence = parcel.readString()
        enableAutoType = parcel.readByte().toInt() != 0
        enableSearching = parcel.readByte().toInt() != 0
        lastTopVisibleEntry = parcel.readSerializable() as UUID
    }

    override fun readParentParcelable(parcel: Parcel): PwGroupV4? {
        return parcel.readParcelable(PwGroupV4::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: PwGroupV4?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeParcelable(iconCustom, flags)
        dest.writeLong(usageCount)
        dest.writeParcelable(locationChanged, flags)
        // TODO MemoryUtil.writeStringParcelableMap(dest, customData);
        dest.writeString(notes)
        dest.writeByte((if (isExpanded) 1 else 0).toByte())
        dest.writeString(defaultAutoTypeSequence)
        dest.writeByte((if (enableAutoType == null) -1 else if (enableAutoType!!) 1 else 0).toByte())
        dest.writeByte((if (enableSearching == null) -1 else if (enableSearching!!) 1 else 0).toByte())
        dest.writeSerializable(lastTopVisibleEntry)
    }

    fun updateWith(source: PwGroupV4) {
        super.updateWith(source)
        iconCustom = PwIconCustom(source.iconCustom)
        usageCount = source.usageCount
        locationChanged = PwDate(source.locationChanged)
        // Add all custom elements in map
        customData.clear()
        for ((key, value) in source.customData) {
            customData[key] = value
        }
        notes = source.notes
        isExpanded = source.isExpanded
        defaultAutoTypeSequence = source.defaultAutoTypeSequence
        enableAutoType = source.enableAutoType
        enableSearching = source.enableSearching
        lastTopVisibleEntry = source.lastTopVisibleEntry
    }

    override var usageCount: Long = 0

    override var locationChanged = PwDate()

    override fun afterAssignNewParent() {
        locationChanged = PwDate()
    }

    fun putCustomData(key: String, value: String) {
        customData[key] = value
    }

    fun containsCustomData(): Boolean {
        return customData.size > 0
    }

    override fun allowAddEntryIfIsRoot(): Boolean {
        return true
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<PwGroupV4> = object : Parcelable.Creator<PwGroupV4> {
            override fun createFromParcel(parcel: Parcel): PwGroupV4 {
                return PwGroupV4(parcel)
            }

            override fun newArray(size: Int): Array<PwGroupV4?> {
                return arrayOfNulls(size)
            }
        }
    }
}
