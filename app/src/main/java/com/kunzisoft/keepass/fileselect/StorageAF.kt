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
package com.kunzisoft.keepass.fileselect

import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import com.kunzisoft.keepass.R

object StorageAF {

    var ACTION_OPEN_DOCUMENT: String

    init {
        ACTION_OPEN_DOCUMENT = try {
            val openDocument = Intent::class.java.getField("ACTION_OPEN_DOCUMENT")
            openDocument.get(null) as String
        } catch (e: Exception) {
            "android.intent.action.OPEN_DOCUMENT"
        }
    }

    fun useStorageFramework(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.saf_key),
                context.resources.getBoolean(R.bool.settings_saf_default))
    }
}
