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

import android.os.AsyncTask

import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory

class OpenFileHistoryAsyncTask(private val afterOpenFileHistoryListener: ((fileName: String?, keyFile: String?) -> Unit)?,
                               private val fileHistory: FileDatabaseHistory?)
    : AsyncTask<Int, Void, Void>() {

    private var fileName: String? = null
    private var keyFile: String? = null

    override fun doInBackground(vararg args: Int?): Void? {
        args[0]?.let {
            fileName = fileHistory?.getDatabaseAt(it)
            keyFile = fileHistory?.getKeyFileAt(it)
        }
        return null
    }

    override fun onPostExecute(v: Void?) {
        afterOpenFileHistoryListener?.invoke(fileName, keyFile)
    }
}
