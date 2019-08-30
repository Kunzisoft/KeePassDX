package com.kunzisoft.keepass.database.action.node

import androidx.fragment.app.FragmentActivity
import android.util.Log
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.database.element.Database

abstract class ActionNodeDatabaseRunnable(
        context: FragmentActivity,
        database: Database,
        private val callbackRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : SaveDatabaseRunnable(context, database, save) {

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
            Log.e("ActionNodeDBRunnable", e.message)
            finishRun(false, e.message)
        }
    }

    /**
     * Function do get the finish node action, don't implements onFinishRun() if used this
     */
    abstract fun nodeFinish(result: Result): ActionNodeValues

    override fun onFinishRun(result: Result) {
        callbackRunnable?.apply {
            onActionNodeFinish(nodeFinish(result))
        }

        if (!result.isSuccess) {
            displayMessage(context)
        }

        super.onFinishRun(result)
    }

    companion object {
        const val NODE_POSITION_FOR_ACTION_NATURAL_ORDER_KEY = "NODE_POSITION_FOR_ACTION_NATURAL_ORDER_KEY"
    }
}
