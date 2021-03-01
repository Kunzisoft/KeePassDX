package com.kunzisoft.keepass.tasks

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class IconUploadWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {
    override fun doWork(): Result {

        //uploadImages()

        return Result.success()
    }
}