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
                              private var executeNestedActionIfResultFalse: Boolean = false) : Runnable {

    protected var isSuccess: Boolean = true
    protected var message: String? = null

    private fun setResult(result: Boolean, message: String? = null) {
        this.isSuccess = result
        this.message = message
    }

    private fun execute() {
        nestedActionRunnable?.let {
            // Pass on result on call finish
            it.setResult(isSuccess, message)
            it.run()
        }
        onFinishRun(isSuccess, message)
    }

    override fun run() {
        execute()
    }

    /**
     * If [success] or [executeNestedActionIfResultFalse] true,
     * launch the nested action runnable if exists and finish,
     * else directly finish
     */
    protected fun finishRun(success: Boolean, message: String? = null) {
        setResult(success, message)
        if (success || executeNestedActionIfResultFalse) {
            execute()
        }
        else
            onFinishRun(isSuccess, message)
    }

    /**
     * Method called when the action is finished
     * @param isSuccess 'true' if success action, 'false' elsewhere
     * @param message
     */
    abstract fun onFinishRun(isSuccess: Boolean, message: String?)

    /**
     * Display a message as a Toast only if [context] is an Activity
     * @param context Context to show the message
     */
    protected fun displayMessage(context: Context) {
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
}
