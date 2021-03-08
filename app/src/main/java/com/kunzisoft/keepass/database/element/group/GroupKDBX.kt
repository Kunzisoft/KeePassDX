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
package com.kunzisoft.keepass.database.element.group

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.utils.UnsignedLong
import java.util.*

class GroupKDBX : GroupVersioned<UUID, UUID, GroupKDBX, EntryKDBX>, NodeKDBXInterface {

    private val customData = HashMap<String, String>()
    var notes = ""

    var isExpanded = true
    var defaultAutoTypeSequence = ""
    var enableAutoType: Boolean? = null
    var enableSearching: Boolean? = null
    var lastTopVisibleEntry: UUID = DatabaseVersioned.UUID_ZERO

    override var expires: Boolean = false

    override val type: Type
        get() = Type.GROUP

    override fun initNodeId(): NodeId<UUID> {
        return NodeIdUUID()
    }

    override fun copyNodeId(nodeId: NodeId<UUID>): NodeId<UUID> {
        return NodeIdUUID(nodeId.id)
    }

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        usageCount = UnsignedLong(parcel.readLong())
        locationChanged = parcel.readParcelable(DateInstant::class.java.classLoader) ?: locationChanged
        // TODO customData = ParcelableUtil.readStringParcelableMap(parcel);
        notes = parcel.readString() ?: notes
        isExpanded = parcel.readByte().toInt() != 0
        defaultAutoTypeSequence = parcel.readString() ?: defaultAutoTypeSequence
        val isAutoTypeEnabled = parcel.readInt()
        enableAutoType = if (isAutoTypeEnabled == -1) null else isAutoTypeEnabled == 1
        val isSearchingEnabled = parcel.readInt()
        enableSearching = if (isSearchingEnabled == -1) null else isSearchingEnabled == 1
        lastTopVisibleEntry = parcel.readSerializable() as UUID
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDBX? {
        return parcel.readParcelable(GroupKDBX::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: GroupKDBX?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(usageCount.toKotlinLong())
        dest.writeParcelable(locationChanged, flags)
        // TODO ParcelableUtil.writeStringParcelableMap(dest, customData);
        dest.writeString(notes)
        dest.writeByte((if (isExpanded) 1 else 0).toByte())
        dest.writeString(defaultAutoTypeSequence)
        dest.writeInt(if (enableAutoType == null) -1 else if (enableAutoType!!) 1 else 0)
        dest.writeInt(if (enableSearching == null) -1 else if (enableSearching!!) 1 else 0)
        dest.writeSerializable(lastTopVisibleEntry)
    }

    fun updateWith(source: GroupKDBX) {
        super.updateWith(source)
        usageCount = source.usageCount
        locationChanged = DateInstant(source.locationChanged)
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

    override var usageCount = UnsignedLong(0)

    override var locationChanged = DateInstant()

    override fun afterAssignNewParent() {
        locationChanged = DateInstant()
    }

    override fun putCustomData(key: String, value: String) {
        customData[key] = value
    }

    override fun containsCustomData(): Boolean {
        return customData.isNotEmpty()
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<GroupKDBX> = object : Parcelable.Creator<GroupKDBX> {
            override fun createFromParcel(parcel: Parcel): GroupKDBX {
                return GroupKDBX(parcel)
            }

            override fun newArray(size: Int): Array<GroupKDBX?> {
                return arrayOfNulls(size)
            }
        }
    }
}
