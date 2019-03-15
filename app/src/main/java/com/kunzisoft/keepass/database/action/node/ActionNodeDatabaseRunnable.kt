package com.kunzisoft.keepass.database.action.node

import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.action.SaveDatabaseProgressDialogRunnable

abstract class ActionNodeDatabaseRunnable(
        context: FragmentActivity,
        database: Database,
        private val callbackRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : SaveDatabaseProgressDialogRunnable(context, database, null, save) {

    /**
     * Function do to a node action, don't implements run() if used this
     */
    abstract fun nodeAction()

    override fun run() {
        try {
            nodeAction()
            // To save the database
            super.run()
            finishRun(true)
        } catch (e: Exception) {
            finishRun(false, e.message)
        }
    }

    /**
     * Function do get the finish node action, don't implements onFinishRun() if used this
     */
    abstract fun nodeFinish(isSuccess: Boolean, message: String?): ActionNodeValues

    override fun onFinishRun(isSuccess: Boolean, message: String?) {
        callbackRunnable?.apply {
            onActionNodeFinish(nodeFinish(isSuccess, message))
        }

        if (!isSuccess) {
            displayMessage(context)
        }

        super.onFinishRun(isSuccess, message)
    }
}
