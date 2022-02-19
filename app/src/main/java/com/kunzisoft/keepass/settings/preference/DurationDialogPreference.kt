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
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.kunzisoft.keepass.R

class DurationDialogPreference @JvmOverloads constructor(context: Context,
                                                         attrs: AttributeSet? = null,
                                                         defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                         defStyleRes: Int = defStyleAttr)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var mDuration: Long = 0L

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_duration
    }

    /**
     * Get current duration of preference
     */
    fun getDuration(): Long {
        return if (mDuration >= 0) mDuration else -1
    }

    /**
     * Assign [duration] of preference
     */
    fun setDuration(duration: Long) {
        persistString(duration.toString())
        notifyChanged()
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            mDuration = getPersistedString(mDuration.toString()).toLongOrNull() ?: mDuration
        } else {
            mDuration = defaultValue?.toString()?.toLongOrNull() ?: mDuration
            persistString(mDuration.toString())
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return try {
            a.getString(index)?.toLongOrNull() ?: mDuration
        } catch (e: Exception) {
            mDuration
        }
    }

    // Was previously a string
    override fun persistString(value: String?): Boolean {
        mDuration = value?.toLongOrNull() ?: mDuration
        return super.persistString(value)
    }
}
