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
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.readSerializableCompat
import com.kunzisoft.keepass.utils.UnsignedLong
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import java.util.*

class GroupKDBX : GroupVersioned<UUID, UUID, GroupKDBX, EntryKDBX>, NodeKDBXInterface {

    override var usageCount = UnsignedLong(0)
    override var locationChanged = DateInstant()
    override var customData = CustomData()
    var notes = ""
    var isExpanded = true
    var enableSearching: Boolean? = null
    var enableAutoType: Boolean? = null
    var defaultAutoTypeSequence: String = ""
    var lastTopVisibleEntry: UUID = DatabaseVersioned.UUID_ZERO
    override var tags = Tags()
    override var previousParentGroup: UUID = DatabaseVersioned.UUID_ZERO

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
        locationChanged = parcel.readParcelableCompat() ?: locationChanged
        customData = parcel.readParcelableCompat() ?: CustomData()
        notes = parcel.readString() ?: notes
        isExpanded = parcel.readBooleanCompat()
        val isSearchingEnabled = parcel.readInt()
        enableSearching = if (isSearchingEnabled == -1) null else isSearchingEnabled == 1
        val isAutoTypeEnabled = parcel.readInt()
        enableAutoType = if (isAutoTypeEnabled == -1) null else isAutoTypeEnabled == 1
        defaultAutoTypeSequence = parcel.readString() ?: defaultAutoTypeSequence
        lastTopVisibleEntry = parcel.readSerializableCompat() ?: UUID.randomUUID()
        tags = parcel.readParcelableCompat() ?: tags
        previousParentGroup = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: DatabaseVersioned.UUID_ZERO
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDBX? {
        return parcel.readParcelableCompat()
    }

    override fun writeParentParcelable(parent: GroupKDBX?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(usageCount.toKotlinLong())
        dest.writeParcelable(locationChanged, flags)
        dest.writeParcelable(customData, flags)
        dest.writeString(notes)
        dest.writeBooleanCompat(isExpanded)
        dest.writeInt(if (enableSearching == null) -1 else if (enableSearching!!) 1 else 0)
        dest.writeInt(if (enableAutoType == null) -1 else if (enableAutoType!!) 1 else 0)
        dest.writeString(defaultAutoTypeSequence)
        dest.writeSerializable(lastTopVisibleEntry)
        dest.writeParcelable(tags, flags)
        dest.writeParcelable(ParcelUuid(previousParentGroup), flags)
    }

    fun updateWith(source: GroupKDBX,
                   updateParents: Boolean = true) {
        super.updateWith(source, updateParents)
        usageCount = source.usageCount
        locationChanged = DateInstant(source.locationChanged)
        // Add all custom elements in map
        customData = CustomData(source.customData)
        notes = source.notes
        isExpanded = source.isExpanded
        enableSearching = source.enableSearching
        enableAutoType = source.enableAutoType
        defaultAutoTypeSequence = source.defaultAutoTypeSequence
        lastTopVisibleEntry = source.lastTopVisibleEntry
        tags = source.tags
        previousParentGroup = source.previousParentGroup
    }

    override fun afterAssignNewParent() {
        locationChanged = DateInstant()
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
