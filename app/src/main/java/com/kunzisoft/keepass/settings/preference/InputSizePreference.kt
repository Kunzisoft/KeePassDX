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
package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.DataByte

open class InputSizePreference @JvmOverloads constructor(context: Context,
                                                         attrs: AttributeSet? = null,
                                                         defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                         defStyleRes: Int = defStyleAttr)
    : InputNumberPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun setSummary(summary: CharSequence?) {
        var summaryString = summary
        try {
            val memorySize = summary.toString().toLong()
            summaryString = if (memorySize >= 0) {
                // To convert bytes to mebibytes
                DataByte(memorySize, DataByte.ByteFormat.BYTE)
                        .toBetterByteFormat().toString(context)
            } else {
                memorySize.toString()
            }
        } catch (e: Exception) {
        } finally {
            super.setSummary(summaryString)
        }
    }
}
