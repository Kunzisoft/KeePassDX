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
package com.kunzisoft.keepass.database.element

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.database.BinaryFile
import kotlinx.coroutines.*

data class Attachment(var name: String,
                      var binaryFile: BinaryFile) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readParcelable(BinaryFile::class.java.classLoader) ?: BinaryFile()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(binaryFile, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "$name at $binaryFile"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attachment) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Attachment> {
        override fun createFromParcel(parcel: Parcel): Attachment {
            return Attachment(parcel)
        }

        override fun newArray(size: Int): Array<Attachment?> {
            return arrayOfNulls(size)
        }

        fun loadBitmap(attachment: Attachment,
                       binaryCipherKey: Database.LoadedKey?,
                       actionOnFinish: (Bitmap?) -> Unit) {
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    val asyncResult: Deferred<Bitmap?> = async {
                        runCatching {
                            binaryCipherKey?.let { binaryKey ->
                                var bitmap: Bitmap?
                                attachment.binaryFile.getUnGzipInputDataStream(binaryKey).use { bitmapInputStream ->
                                    bitmap = BitmapFactory.decodeStream(bitmapInputStream)
                                }
                                bitmap
                            }
                        }.getOrNull()
                    }
                    withContext(Dispatchers.Main) {
                        actionOnFinish(asyncResult.await())
                    }
                }
            }
        }
    }
}