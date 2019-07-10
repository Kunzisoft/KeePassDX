package com.kunzisoft.keepass.activities.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.compat.ClipDataCompat
import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory
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

                if (keyFileUri == null) {
                    keyFileUri = getKeyFileUri(databaseUri)
                }
            } else if (incoming.scheme == "content") {
                if (keyFileUri == null) {
                    keyFileUri = getKeyFileUri(databaseUri)
                }
            } else {
                return R.string.error_can_not_handle_uri
            }

        } else {
            databaseUri = UriUtil.parseDefaultFile(intent.getStringExtra(KEY_FILENAME))
            keyFileUri = UriUtil.parseDefaultFile(intent.getStringExtra(KEY_KEYFILE))

            if (keyFileUri == null || keyFileUri!!.toString().isEmpty()) {
                keyFileUri = getKeyFileUri(databaseUri)
            }
        }
        return null
    }

    public override fun onPostExecute(result: Int?) {
        uriIntentInitTaskCallback.onPostInitTask(databaseUri, keyFileUri, result)
    }

    private fun getKeyFileUri(databaseUri: Uri?): Uri? {
        return if (isKeyFileNeeded) {
            FileDatabaseHistory.getInstance(weakContext).getKeyFileUriByDatabaseUri(databaseUri!!)
        } else {
            null
        }
    }

    companion object {
        const val KEY_FILENAME = "fileName"
        const val KEY_KEYFILE = "keyFile"
        private const val VIEW_INTENT = "android.intent.action.VIEW"
    }
}
