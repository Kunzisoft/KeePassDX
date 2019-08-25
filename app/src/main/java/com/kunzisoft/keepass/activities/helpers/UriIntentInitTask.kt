package com.kunzisoft.keepass.activities.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistory
import com.kunzisoft.keepass.utils.UriUtil

import java.io.File
import java.lang.ref.WeakReference

class UriIntentInitTask(private val weakContext: WeakReference<Context>,
                        private val uriIntentInitTaskCallback: UriIntentInitTaskCallback,
                        private val isKeyFileNeeded: Boolean)
    : AsyncTask<Intent, Void, Int>() {

    private var databaseUri: Uri? = null
    private var keyFileUri: Uri? = null

    override fun doInBackground(vararg args: Intent): Int? {
        val intent = args[0]
        val action = intent.action

        // If is a view intent
        if (action != null && action == VIEW_INTENT) {
            val incoming = intent.data
            databaseUri = incoming
            keyFileUri = ClipDataCompat.getUriFromIntent(intent, KEY_KEYFILE)

            if (incoming == null) {
                return R.string.error_can_not_handle_uri
            } else if (incoming.scheme == "file") {
                val fileName = incoming.path

                if (fileName?.isNotEmpty() == true) {
                    // No file name
                    return R.string.file_not_found
                }

                val dbFile = File(fileName)
                if (!dbFile.exists()) {
                    // File does not exist
                    return R.string.file_not_found
                }

                return null
            } else if (incoming.scheme == "content") {
                return null
            } else {
                return R.string.error_can_not_handle_uri
            }

        } else {
            databaseUri = UriUtil.parseUriFile(intent.getStringExtra(KEY_FILENAME))
            keyFileUri = UriUtil.parseUriFile(intent.getStringExtra(KEY_KEYFILE))

            return null
        }
    }

    public override fun onPostExecute(result: Int?) {

        if (isKeyFileNeeded && (keyFileUri == null || keyFileUri!!.toString().isEmpty())) {
            // Retrieve KeyFile in a thread if needed
            databaseUri?.let { databaseUriNotNull ->
                FileDatabaseHistory.getInstance(weakContext)
                        .getKeyFileUriByDatabaseUri(databaseUriNotNull)  {
                            uriIntentInitTaskCallback.onPostInitTask(databaseUri, it, result)
                        }
            }
        } else {
            uriIntentInitTaskCallback.onPostInitTask(databaseUri, keyFileUri, result)
        }
    }

    companion object {
        const val KEY_FILENAME = "fileName"
        const val KEY_KEYFILE = "keyFile"
        private const val VIEW_INTENT = "android.intent.action.VIEW"
    }
}
