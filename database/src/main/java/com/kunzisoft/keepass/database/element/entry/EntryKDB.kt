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
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.binary.AttachmentPool
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.KEY_ID
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.utils.readParcelableCompat
import java.util.*

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
    private var binaryDataId: Int? = null

    // Determine if this is a MetaStream entry
    fun isMetaStream(): Boolean {
        if (notes.isEmpty()) return false
        if (binaryDescription != PMS_ID_BINDESC) return false
        if (title.isEmpty()) return false
        if (title != PMS_ID_TITLE) return false
        if (username.isEmpty()) return false
        if (username != PMS_ID_USER) return false
        if (url.isEmpty()) return false
        if (url != PMS_ID_URL) return false
        return icon.standard.id == KEY_ID
    }

    fun isMetaStreamDefaultUsername(): Boolean {
        return isMetaStream() && notes == PMS_STREAM_DEFAULTUSER
    }

    private fun setMetaStream() {
        binaryDescription = PMS_ID_BINDESC
        title = PMS_ID_TITLE
        username = PMS_ID_USER
        url = PMS_ID_URL
        icon.standard = IconImageStandard(KEY_ID)
    }

    fun setMetaStreamDefaultUsername() {
        notes = PMS_STREAM_DEFAULTUSER
        setMetaStream()
    }

    fun isMetaStreamDatabaseColor(): Boolean {
        return isMetaStream() && notes == PMS_STREAM_DBCOLOR
    }

    fun setMetaStreamDatabaseColor() {
        notes = PMS_STREAM_DBCOLOR
        setMetaStream()
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
        val rawBinaryDataId = parcel.readInt()
        binaryDataId = if (rawBinaryDataId == -1) null else rawBinaryDataId
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDB? {
        return parcel.readParcelableCompat()
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
        dest.writeInt(binaryDataId ?: -1)
    }

    fun updateWith(source: EntryKDB,
                   updateParents: Boolean = true) {
        super.updateWith(source, updateParents)
        title = source.title
        username = source.username
        password = source.password
        url = source.url
        notes = source.notes
        binaryDescription = source.binaryDescription
        binaryDataId = source.binaryDataId
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

    fun getAttachment(attachmentPool: AttachmentPool): Attachment? {
        binaryDataId?.let { poolId ->
            attachmentPool[poolId]?.let { binary ->
                return Attachment(binaryDescription, binary)
            }
        }
        return null
    }

    fun containsAttachment(): Boolean {
        return binaryDataId != null
    }

    fun getBinary(attachmentPool: AttachmentPool): BinaryData? {
        this.binaryDataId?.let {
            return attachmentPool[it]
        }
        return null
    }

    fun putBinary(binaryData: BinaryData, attachmentPool: AttachmentPool) {
        this.binaryDataId = attachmentPool.put(binaryData)
    }

    fun putAttachment(attachment: Attachment, attachmentPool: AttachmentPool) {
        this.binaryDescription = attachment.name
        this.binaryDataId = attachmentPool.put(attachment.binaryData)
    }

    fun removeAttachment(attachment: Attachment? = null) {
        if (attachment == null || this.binaryDescription == attachment.name) {
            this.binaryDescription = ""
            this.binaryDataId = null
        }
    }

    companion object {

        /** Size of byte buffer needed to hold this struct.  */
        private const val PMS_ID_BINDESC = "bin-stream"
        private const val PMS_ID_TITLE = "Meta-Info"
        private const val PMS_ID_USER = "SYSTEM"
        private const val PMS_ID_URL = "$"

         const val PMS_STREAM_SIMPLESTATE = "Simple UI State"
         const val PMS_STREAM_DEFAULTUSER = "Default User Name"
         const val PMS_STREAM_SEARCHHISTORYITEM = "Search History Item"
         const val PMS_STREAM_CUSTOMKVP = "Custom KVP"
         const val PMS_STREAM_DBCOLOR = "Database Color"
         const val PMS_STREAM_KPXICON2 = "KPX_CUSTOM_ICONS_2"

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
