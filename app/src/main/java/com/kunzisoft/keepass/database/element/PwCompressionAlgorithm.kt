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

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.ObjectNameResource
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum


// Note: We can get away with using int's to store unsigned 32-bit ints
//       since we won't do arithmetic on these values (also unlikely to
//       reach negative ids).
enum class PwCompressionAlgorithm : ObjectNameResource, Parcelable {

    None,
    GZip;

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeEnum(this)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun getName(resources: Resources): String {
        return when (this) {
            None -> resources.getString(R.string.compression_none)
            GZip -> resources.getString(R.string.compression_gzip)
        }
    }

    companion object CREATOR : Parcelable.Creator<PwCompressionAlgorithm> {
        override fun createFromParcel(parcel: Parcel): PwCompressionAlgorithm {
            return parcel.readEnum<PwCompressionAlgorithm>() ?: None
        }

        override fun newArray(size: Int): Array<PwCompressionAlgorithm?> {
            return arrayOfNulls(size)
        }
    }

}
