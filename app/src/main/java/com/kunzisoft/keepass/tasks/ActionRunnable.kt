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

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast

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
    protected fun finishRun(isSuccess: Boolean, message: String? = null) {
        result.isSuccess = isSuccess
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
     * Display a message as a Toast only if [context] is an Activity
     * @param context Context to show the message
     */
    protected fun displayMessage(context: Context) {
        val message = result.message
        Log.i(ActionRunnable::class.java.name, message)
        try {
            (context as Activity).runOnUiThread {
                message?.let {
                    if (it.isNotEmpty()) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (exception: ClassCastException) {}
    }

    /**
     * Class to manage result from ActionRunnable
     */
    data class Result(var isSuccess: Boolean = true, var message: String? = null)
}
