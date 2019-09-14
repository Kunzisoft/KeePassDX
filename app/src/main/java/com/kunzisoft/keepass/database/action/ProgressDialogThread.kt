package com.kunzisoft.keepass.database.action

import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_TASK_TITLE_KEY
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

    private var intentDatabaseTask:Intent = Intent(activity, DatabaseTaskNotificationService::class.java)

    init {
        actionRunnableAsyncTask = ActionRunnableAsyncTask(progressTaskDialogFragment,
                {
                    activity.runOnUiThread {
                        intentDatabaseTask.putExtra(DATABASE_TASK_TITLE_KEY, titleId)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            activity.startForegroundService(intentDatabaseTask)
                        } else {
                            activity.startService(intentDatabaseTask)
                        }
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
                        activity.stopService(intentDatabaseTask)
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