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
import com.kunzisoft.keepass.database.exception.LoadDatabaseException

/**
 * Callback after a task is completed.
 */
abstract class ActionRunnable(private var nestedActionRunnable: ActionRunnable? = null,
                              private var executeNestedActionIfResultFalse: Boolean = false)
    : Runnable {

    var result: Result = Result()

    private fun execute() {
        nestedActionRunnable?.let {
            // Pass on result on call finish
            it.result = result
            it.run()
        }
        onFinishRun(result)
    }

    override fun run() {
        execute()
    }

    /**
     * If [success] or [executeNestedActionIfResultFalse] true,
     * launch the nested action runnable if exists and finish,
     * else directly finish
     */
    protected fun finishRun(isSuccess: Boolean,
                            message: String? = null) {
        finishRun(isSuccess, null, message)
    }

    /**
     * If [success] or [executeNestedActionIfResultFalse] true,
     * launch the nested action runnable if exists and finish,
     * else directly finish
     */
    protected fun finishRun(isSuccess: Boolean,
                            exception: LoadDatabaseException?,
                            message: String? = null) {
        result.isSuccess = isSuccess
        result.exception = exception
        result.message = message
        if (isSuccess || executeNestedActionIfResultFalse) {
            execute()
        }
        else
            onFinishRun(result)
    }

    /**
     * Method called when the action is finished
     * @param result 'true' if success action, 'false' elsewhere, with message
     */
    abstract fun onFinishRun(result: Result)

    /**
     * Class to manage result from ActionRunnable
     */
    data class Result(var isSuccess: Boolean = true,
                      var message: String? = null,
                      var exception: LoadDatabaseException? = null,
                      var data: Bundle? = null) {

        fun toBundle(): Bundle {
            return Bundle().apply {
                putBoolean(IS_SUCCESS_KEY, isSuccess)
                putString(MESSAGE_KEY, message)
                putSerializable(EXCEPTION_KEY, exception)
                putBundle(DATA_KEY, data)
            }
        }

        companion object {
            private const val IS_SUCCESS_KEY = "IS_SUCCESS_KEY"
            private const val MESSAGE_KEY = "MESSAGE_KEY"
            private const val EXCEPTION_KEY = "EXCEPTION_KEY"
            private const val DATA_KEY = "DATA_KEY"

            fun fromBundle(bundle: Bundle): Result {
                return Result(bundle.getBoolean(IS_SUCCESS_KEY),
                        bundle.getString(MESSAGE_KEY),
                        bundle.getSerializable(EXCEPTION_KEY) as LoadDatabaseException?,
                        bundle.getBundle(DATA_KEY))
            }
        }
    }
}
