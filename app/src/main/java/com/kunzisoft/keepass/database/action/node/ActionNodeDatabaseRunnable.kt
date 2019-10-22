package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.database.element.Database

abstract class ActionNodeDatabaseRunnable(
        context: Context,
        database: Database,
        private val callbackRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : SaveDatabaseRunnable(context, database, save) {

    /**
     * Function do to a node action, don't implements run() if used this
     */
    abstract fun nodeAction()

    protected fun saveDatabase() {
        super.run()
    }

    override fun run() {
        nodeAction()
    }

    /**
     * Function do get the finish node action, don't implements onFinishRun() if used this
     */
    abstract fun nodeFinish(result: Result): ActionNodeValues

    override fun onFinishRun(result: Result) {
        callbackRunnable?.apply {
            onActionNodeFinish(nodeFinish(result))
        }
        super.onFinishRun(result)
    }
}
