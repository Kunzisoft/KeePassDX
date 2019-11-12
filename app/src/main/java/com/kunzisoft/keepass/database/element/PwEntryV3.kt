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
 */

package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable

import java.io.UnsupportedEncodingException
import java.util.Arrays
import java.util.UUID


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
class PwEntryV3 : PwEntry<Int, UUID, PwGroupV3, PwEntryV3>, PwNodeV3Interface {

    /** A string describing what is in pBinaryData  */
    var binaryDesc = ""
    /**
     * @return the actual binaryData byte array.
     */
    var binaryData: ByteArray = ByteArray(0)
        private set

    // Determine if this is a MetaStream entry
    val isMetaStream: Boolean
        get() {
            if (Arrays.equals(binaryData, ByteArray(0))) return false
            if (notes.isEmpty()) return false
            if (binaryDesc != PMS_ID_BINDESC) return false
            if (title.isEmpty()) return false
            if (title != PMS_ID_TITLE) return false
            if (username.isEmpty()) return false
            if (username != PMS_ID_USER) return false
            if (url.isEmpty()) return false
            return if (url != PMS_ID_URL) false else icon.isMetaStreamIcon
        }

    override fun initNodeId(): PwNodeId<UUID> {
        return PwNodeIdUUID()
    }

    override fun copyNodeId(nodeId: PwNodeId<UUID>): PwNodeId<UUID> {
        return PwNodeIdUUID(nodeId.id)
    }

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        title = parcel.readString() ?: title
        username = parcel.readString() ?: username
        parcel.readByteArray(passwordBytes)
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
        binaryDesc = parcel.readString() ?: binaryDesc
        parcel.readByteArray(binaryData)
    }

    override fun readParentParcelable(parcel: Parcel): PwGroupV3? {
        return parcel.readParcelable(PwGroupV3::class.java.classLoader)
    }

    override fun writeParentParcelable(parent: PwGroupV3?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(title)
        dest.writeString(username)
        dest.writeByteArray(passwordBytes)
        dest.writeString(url)
        dest.writeString(notes)
        dest.writeString(binaryDesc)
        dest.writeByteArray(binaryData)
    }

    fun updateWith(source: PwEntryV3) {
        super.updateWith(source)
        title = source.title
        username = source.username

        val passLen = source.passwordBytes.size
        passwordBytes = ByteArray(passLen)
        System.arraycopy(source.passwordBytes, 0, passwordBytes, 0, passLen)

        url = source.url
        notes = source.notes
        binaryDesc = source.binaryDesc

        val descLen = source.binaryData.size
        binaryData = ByteArray(descLen)
        System.arraycopy(source.binaryData, 0, binaryData, 0, descLen)
    }

    override var username = ""

    var passwordBytes: ByteArray = ByteArray(0)
        private set

    /** Securely erase old password before copying new.  */
    fun setPassword(buf: ByteArray, offset: Int, len: Int) {
        fill(passwordBytes, 0.toByte())
        passwordBytes = ByteArray(len)
        System.arraycopy(buf, offset, passwordBytes, 0, len)
    }

    /**
     * @return the actual password byte array.
     */
    override var password: String
        get() = String(passwordBytes)
        set(pass) {
            var password: ByteArray
            try {
                password = pass.toByteArray(charset("UTF-8"))
                setPassword(password, 0, password.size)
            } catch (e: UnsupportedEncodingException) {
                password = pass.toByteArray()
                setPassword(password, 0, password.size)
            }

        }

    override var url = ""

    override var notes = ""

    override var title = ""

    override val type: Type
        get() = Type.ENTRY

    fun setBinaryData(buf: ByteArray, offset: Int, len: Int) {
        /** Securely erase old data before copying new.  */
        fill(binaryData, 0.toByte())
        binaryData = ByteArray(len)
        System.arraycopy(buf, offset, binaryData, 0, len)
    }

    companion object {

        /** Size of byte buffer needed to hold this struct.  */
        private const val PMS_ID_BINDESC = "bin-stream"
        private const val PMS_ID_TITLE = "Meta-Info"
        private const val PMS_ID_USER = "SYSTEM"
        private const val PMS_ID_URL = "$"

        @JvmField
        val CREATOR: Parcelable.Creator<PwEntryV3> = object : Parcelable.Creator<PwEntryV3> {
            override fun createFromParcel(`in`: Parcel): PwEntryV3 {
                return PwEntryV3(`in`)
            }

            override fun newArray(size: Int): Array<PwEntryV3?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * fill byte array
         */
        private fun fill(array: ByteArray, value: Byte) {
            for (i in array.indices)
                array[i] = value
        }
    }
}
