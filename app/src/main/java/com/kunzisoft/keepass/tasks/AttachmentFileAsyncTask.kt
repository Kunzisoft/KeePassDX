package com.kunzisoft.keepass.tasks

import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.kunzisoft.keepass.database.element.security.BinaryAttachment

class AttachmentFileAsyncTask(
        private val fileUri: Uri,
        private val contentResolver: ContentResolver,
        private val onStart: (()->Unit)? = null,
        private val onUpdate: ((percent: Int)->Unit)? = null,
        private val onFinish: ((Boolean)->Unit)? = null
        ): AsyncTask<BinaryAttachment, Int, Boolean>() {

    private val updateMinFrequency = 1000
    private var previousSaveTime = System.currentTimeMillis()

    override fun onPreExecute() {
        super.onPreExecute()
        onStart?.invoke()
    }

    override fun doInBackground(vararg params: BinaryAttachment?): Boolean {
        try {
            params[0]?.download(fileUri, contentResolver, 1024) { percent ->
                publishProgress(percent)
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        val percent = values[0] ?: 0

        val currentTime = System.currentTimeMillis()
        if (previousSaveTime + updateMinFrequency < currentTime) {
            onUpdate?.invoke(percent)
            Log.d(TAG, "Download file $fileUri : $percent%")
            previousSaveTime = currentTime
        }
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        onFinish?.invoke(result)
    }

    companion object {
        private val TAG = AttachmentFileAsyncTask::class.java.name
    }
}