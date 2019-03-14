package com.kunzisoft.keepass.database.action

import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

class ProgressDialogRunnable(val context: FragmentActivity,
                             private val titleId: Int,
                             private val actionRunnable: (ProgressTaskUpdater)-> ActionRunnable)
    : ActionRunnable() {

    override fun run() {
        // Show the dialog
        val progressTaskUpdater: ProgressTaskUpdater = ProgressTaskDialogFragment.start(
                context.supportFragmentManager,
                titleId)

        // Do the action
        actionRunnable.invoke(progressTaskUpdater).run()
        super.run()
    }

    override fun onFinishRun(isSuccess: Boolean, message: String?) {
        // Remove the progress task
        ProgressTaskDialogFragment.stop(context)
    }
}