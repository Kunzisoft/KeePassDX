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
package com.kunzisoft.keepass.utils

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.settings.PreferencesUtil

class FileDatabaseInfo : FileInfo {

    constructor(context: Context, fileUri: Uri): super(context, fileUri)

    constructor(context: Context, filePath: String): super(context, filePath)

    fun retrieveDatabaseAlias(alias: String): String {
        return when {
            alias.isNotEmpty() -> alias
            PreferencesUtil.isFullFilePathEnable(context) -> filePath ?: ""
            else -> fileName ?: ""
        }
    }

    fun retrieveDatabaseTitle(titleCallback: (String)->Unit) {

        fileUri?.let { fileUri ->
            FileDatabaseHistoryAction.getInstance(context.applicationContext)
                    .getFileDatabaseHistory(fileUri) { fileDatabaseHistoryEntity ->
                titleCallback.invoke(retrieveDatabaseAlias(fileDatabaseHistoryEntity?.databaseAlias
                        ?: ""))
            }
        }
    }

}