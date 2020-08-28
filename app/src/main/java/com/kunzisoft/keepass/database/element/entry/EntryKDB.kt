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
 */

package com.kunzisoft.keepass.database.element.entry

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.database.BinaryAttachment
import com.kunzisoft.keepass.database.element.Attachment
import java.util.*
import kotlin.collections.ArrayList

/**
 * Structure containing information about one entry.
 *
 * <PRE>
 * One entry: [FIELDTYPE(FT)][FIELDSIZE(FS)][FIELDDATA(FD)]
 * [FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)]...
 *
 * [ 2 bytes] FIELDTYPE
 * [ 4 bytes] FIELDSIZE, size of FIELDDATA in bytes
 * [ n bytes] FIELDDATA, n = FIELDSIZE
 *
 * Notes:
 * - Strings are stored in UTF-8 encoded form and are null-terminated.
 * - FIELDTYPE can be one of the FT_ constants.
</PRE> *
 *
 * @author Naomaru Itoi <nao></nao>@phoneid.org>
 * @author Bill Zwicky <wrzwicky></wrzwicky>@pobox.com>
 * @author Dominik Reichl <dominik.reichl></dominik.reichl>@t-online.de>
 * @author Jeremy Jamet <jeremy.jamet></jeremy.jamet>@kunzisoft.com>
 */
class EntryKDB : EntryVersioned<Int, UUID, GroupKDB, EntryKDB>, NodeKDBInterface {

    /** A string describing what is in binaryData  */
    var binaryDescription = ""
    var binaryData: BinaryAttachment? = null

    // Determine if this is a MetaStream entry
    val isMetaStream: Boolean
        get() {
            if (notes.isEmpty()) return false
            if (binaryDescription != PMS_ID_BINDESC) return false
            if (title.isEmpty()) return false
            if (title != PMS_ID_TITLE) return false
            if (username.isEmpty()) return false
            if (username != PMS_ID_USER) return false
            if (url.isEmpty()) return false
            return if (url != PMS_ID_URL) false else icon.isMetaStreamIcon
        }

    override fun initNodeId(): NodeId<UUID> {
        return NodeIdUUID()
    }

    override fun copyNodeId(nodeId: NodeId<UUID>): NodeId<UUID> {
        return NodeIdUUID(nodeId.id)
    }

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        title = parcel.readString() ?: title
        username = parcel.readString() ?: username
        password = parcel.readString() ?: password
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
        binaryDescription = parcel.readString() ?: binaryDescription
        binaryData = parcel.readParcelable(BinaryAttachment::class.java.classLoader)
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDB? {
        return parcel.readParcelable(GroupKDB::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: GroupKDB?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(title)
        dest.writeString(username)
        dest.writeString(password)
        dest.writeString(url)
        dest.writeString(notes)
        dest.writeString(binaryDescription)
        dest.writeParcelable(binaryData, flags)
    }

    fun updateWith(source: EntryKDB) {
        super.updateWith(source)
        title = source.title
        username = source.username
        password = source.password
        url = source.url
        notes = source.notes
        binaryDescription = source.binaryDescription
        binaryData = source.binaryData
    }

    override var username = ""

    /**
     * @return the actual password byte array.
     */
    override var password = ""

    override var url = ""

    override var notes = ""

    override var title = ""

    override val type: Type
        get() = Type.ENTRY

    fun getAttachment(): Attachment? {
        val binary = binaryData
        return if (binary != null)
            Attachment(binaryDescription, binary)
        else null
    }

    fun containsAttachment(): Boolean {
        return binaryData != null
    }

    fun putAttachment(attachment: Attachment) {
        this.binaryDescription = attachment.name
        this.binaryData = attachment.binaryAttachment
    }

    fun removeAttachment(attachment: Attachment) {
        if (this.binaryDescription == attachment.name) {
            this.binaryDescription = ""
            this.binaryData = null
        }
    }

    companion object {

        /** Size of byte buffer needed to hold this struct.  */
        private const val PMS_ID_BINDESC = "bin-stream"
        private const val PMS_ID_TITLE = "Meta-Info"
        private const val PMS_ID_USER = "SYSTEM"
        private const val PMS_ID_URL = "$"

        @JvmField
        val CREATOR: Parcelable.Creator<EntryKDB> = object : Parcelable.Creator<EntryKDB> {
            override fun createFromParcel(parcel: Parcel): EntryKDB {
                return EntryKDB(parcel)
            }

            override fun newArray(size: Int): Array<EntryKDB?> {
                return arrayOfNulls(size)
            }
        }
    }
}
