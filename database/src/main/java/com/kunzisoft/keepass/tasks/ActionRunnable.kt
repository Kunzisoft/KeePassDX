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
package com.kunzisoft.keepass.tasks

import android.os.Bundle
import android.util.Log
import com.kunzisoft.keepass.database.exception.DatabaseException

/**
 * Callback after a task is completed.
 */
abstract class ActionRunnable: Runnable {

    var result: Result = Result()

    override fun run() {
        try {
            onStartRun()
            onActionRun()
            onFinishRun()
        } catch (runException: Exception) {
            setError(runException)
        }
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
        showLog()
    }

    protected fun setError(exception: Exception) {
        result.isSuccess = false
        result.exception = null
        result.message = exception.message
        showLog()
    }

    protected fun setError(exception: DatabaseException) {
        result.isSuccess = false
        result.exception = exception
        result.message = exception.message
        showLog()
    }

    private fun showLog() {
        val message = if (result.message != null) ", message=${result.message}" else ""
        Log.e(TAG, "success=${result.isSuccess}$message", result.exception)
    }

    /**
     * Class to manage result from ActionRunnable
     */
    data class Result(var isSuccess: Boolean = true,
                      var message: String? = null,
                      var exception: DatabaseException? = null,
                      var data: Bundle? = null)

    companion object {
        private const val TAG = "ActionRunnable"
    }
}
