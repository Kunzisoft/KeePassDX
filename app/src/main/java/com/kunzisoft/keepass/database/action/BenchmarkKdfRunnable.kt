package com.kunzisoft.keepass.database.action

import android.content.Context
import android.os.Bundle
import com.kunzisoft.keepass.database.crypto.kdf.KdfBenchmark
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.AppUtil.getKdfLimits

class BenchmarkKdfRunnable(
    private val context: Context,
    private val database: Database,
    private val targetTime: Long,
    private val progressTaskUpdater: ProgressTaskUpdater?
): ActionRunnable() {

    override fun onStartRun() {
        progressTaskUpdater?.benchmarking()
    }

    override fun onActionRun() {
        val engine = database.kdfEngine
            ?: throw IllegalStateException("No KDF engine found")
        val masterKey = database.masterKey

        val oldBenchmark = KdfBenchmark(
            iterations = database.kdfEngine?.getKeyRounds(),
            memory = database.kdfEngine?.getMemoryUsage(),
            parallelism = database.kdfEngine?.getParallelism()
        )
        val newBenchmark = engine.calculateBenchmark(
            masterKey = masterKey,
            targetTime = targetTime,
            limits = context.getKdfLimits()
        )
        result.data = Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldBenchmark)
            putParcelable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newBenchmark)
        }
    }

    override fun onFinishRun() {}
}