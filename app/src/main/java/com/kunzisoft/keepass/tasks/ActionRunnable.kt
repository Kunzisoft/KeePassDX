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
package com.kunzisoft.keepass.tasks

import android.os.Bundle
import com.kunzisoft.keepass.database.exception.DatabaseException
import java.lang.Exception

/**
 * Callback after a task is completed.
 */
abstract class ActionRunnable: Runnable {

    var result: Result = Result()

    override fun run() {
        onStartRun()
        onActionRun()
        onFinishRun()
    }

    abstract fun onStartRun()

    abstract fun onActionRun()

    /**
     * Method called when the action is finished
     */
    abstract fun onFinishRun()

    protected fun setError(message: String) {
        result.isSuccess = false
        result.exception = null
        result.message = message
    }

    protected fun setError(exception: Exception) {
        result.isSuccess = false
        result.exception = null
        result.message = exception.message
    }

    protected fun setError(exception: DatabaseException) {
        result.isSuccess = false
        result.exception = exception
        result.message = exception.message
    }

    /**
     * Class to manage result from ActionRunnable
     */
    data class Result(var isSuccess: Boolean = true,
                      var message: String? = null,
                      var exception: DatabaseException? = null,
                      var data: Bundle? = null)
}
