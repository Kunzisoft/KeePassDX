package com.kunzisoft.keepass.database.action

import android.os.AsyncTask
import android.support.annotation.StringRes
import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

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

    init {
        actionRunnableAsyncTask = ActionRunnableAsyncTask(progressTaskDialogFragment,
                {
                    activity.runOnUiThread {
                        // Show the dialog
                        ProgressTaskDialogFragment.start(activity, progressTaskDialogFragment)
                    }
                }, {
                    activity.runOnUiThread {
                        // Remove the progress task
                        ProgressTaskDialogFragment.stop(activity)
                    }
                })
    }

    fun start() {
        actionRunnableAsyncTask?.execute(actionRunnable)
    }


    private class ActionRunnableAsyncTask(private val progressTaskUpdater: ProgressTaskUpdater,
                                          private val onPreExecute: () -> Unit,
                                          private val onPostExecute: () -> Unit)
        : AsyncTask<((ProgressTaskUpdater?)-> ActionRunnable), Void, Void>() {

        override fun onPreExecute() {
            super.onPreExecute()
            onPreExecute.invoke()
        }

        override fun doInBackground(vararg actionRunnables: ((ProgressTaskUpdater?)-> ActionRunnable)?): Void? {
            actionRunnables.forEach {
                it?.invoke(progressTaskUpdater)?.run()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            onPostExecute.invoke()
        }
    }
}