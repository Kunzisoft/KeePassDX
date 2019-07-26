package com.kunzisoft.keepass.database.action

import android.os.AsyncTask
import android.support.annotation.StringRes
import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.timeout.TimeoutHelper

open class ProgressDialogThread(private val activity: FragmentActivity,
                                private val actionRunnable: (ProgressTaskUpdater?)-> ActionRunnable,
                                @StringRes private val titleId: Int,
                                @StringRes private val messageId: Int? = null,
                                @StringRes private val warningId: Int? = null) {

    private val progressTaskDialogFragment = ProgressTaskDialogFragment.build(
            titleId,
            messageId,
            warningId)
    private var actionRunnableAsyncTask: ActionRunnableAsyncTask? = null
    var actionFinishInUIThread: ActionRunnable? = null

    init {
        actionRunnableAsyncTask = ActionRunnableAsyncTask(progressTaskDialogFragment,
                {
                    activity.runOnUiThread {
                        TimeoutHelper.temporarilyDisableTimeout()
                        // Show the dialog
                        ProgressTaskDialogFragment.start(activity, progressTaskDialogFragment)
                    }
                }, { result ->
                    activity.runOnUiThread {
                        actionFinishInUIThread?.onFinishRun(result)
                        // Remove the progress task
                        ProgressTaskDialogFragment.stop(activity)
                        TimeoutHelper.releaseTemporarilyDisableTimeoutAndLockIfTimeout(activity)
                    }
                })
    }

    fun start() {
        actionRunnableAsyncTask?.execute(actionRunnable)
    }


    private class ActionRunnableAsyncTask(private val progressTaskUpdater: ProgressTaskUpdater,
                                          private val onPreExecute: () -> Unit,
                                          private val onPostExecute: (result: ActionRunnable.Result) -> Unit)
        : AsyncTask<((ProgressTaskUpdater?)-> ActionRunnable), Void, ActionRunnable.Result>() {

        override fun onPreExecute() {
            super.onPreExecute()
            onPreExecute.invoke()
        }

        override fun doInBackground(vararg actionRunnables: ((ProgressTaskUpdater?)-> ActionRunnable)?): ActionRunnable.Result {
            var resultTask = ActionRunnable.Result(false)
            actionRunnables.forEach {
                it?.invoke(progressTaskUpdater)?.apply {
                    run()
                    resultTask = result
                }
            }
            return resultTask
        }

        override fun onPostExecute(result: ActionRunnable.Result) {
            super.onPostExecute(result)
            onPostExecute.invoke(result)
        }
    }
}