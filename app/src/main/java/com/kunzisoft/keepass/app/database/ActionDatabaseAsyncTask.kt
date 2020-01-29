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
package com.kunzisoft.keepass.app.database

import android.os.AsyncTask

/**
 * Private class to invoke each method in a separate thread
 */
class ActionDatabaseAsyncTask<T>(
        private val action: () -> T ,
        private val afterActionDatabaseListener: ((T?) -> Unit)? = null
) : AsyncTask<Void, Void, T>() {

    override fun doInBackground(vararg args: Void?): T? {
        return action.invoke()
    }

    override fun onPostExecute(result: T?) {
        afterActionDatabaseListener?.invoke(result)
    }
}