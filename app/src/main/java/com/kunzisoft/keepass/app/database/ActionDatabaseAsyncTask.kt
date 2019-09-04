package com.kunzisoft.keepass.app.database

import android.os.AsyncTask

/**
 * Private class to invoke each method in a separate thread
 */
class ActionDatabaseAsyncTask<T>(
        private val action: () -> T ,
        private val afterActionDatabaseListener: ((T?) -> Unit)? = null
) : AsyncTask<Void, Void, T>() {

    override fun doInBackground(vararg args: Void?): T? {
        return action.invoke()
    }

    override fun onPostExecute(result: T?) {
        afterActionDatabaseListener?.invoke(result)
    }
}