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
package com.kunzisoft.keepass.database.action

import android.content.Context

import com.kunzisoft.keepass.database.Database
import com.kunzisoft.keepass.database.exception.PwDbOutputException
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper

import java.io.IOException

abstract class SaveDatabaseRunnable(protected var context: Context,
                                    protected var database: Database,
                                    nestedAction: ActionRunnable? = null,
                                    private val save: Boolean) : ActionRunnable(nestedAction) {

    init {
        TimeoutHelper.temporarilyDisableTimeout()
    }

    override fun run() {
        if (save) {
            try {
                database.saveData(context)
            } catch (e: IOException) {
                finishRun(false, e.message)
            } catch (e: PwDbOutputException) {
                finishRun(false, e.message)
            }
        }

        // Need to call super.run() in child class
    }

    override fun onFinishRun(isSuccess: Boolean, message: String?) {
        // Need to call super.onFinishRun(isSuccess, message) in child class

        TimeoutHelper.releaseTemporarilyDisableTimeoutAndLockIfTimeout(context)
    }
}
