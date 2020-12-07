package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import kotlinx.coroutines.*

class AdvancedUnlockNotificationService : NotificationService() {

    private lateinit var mTempCipherDao: ArrayList<CipherDatabaseEntity>

    private var mActionTaskBinder = AdvancedUnlockBinder()

    private var notificationTimeoutMilliSecs: Long = 0
    private var mTimerJob: Job? = null

    inner class AdvancedUnlockBinder: Binder() {
        fun getCipherDatabase(databaseUri: Uri): CipherDatabaseEntity? {
            return mTempCipherDao.firstOrNull { it.databaseUri == databaseUri.toString()}
        }
        fun addOrUpdateCipherDatabase(cipherDatabaseEntity: CipherDatabaseEntity) {
            val cipherDatabaseRetrieve = mTempCipherDao.firstOrNull { it.databaseUri == cipherDatabaseEntity.databaseUri }
            cipherDatabaseRetrieve?.replaceContent(cipherDatabaseEntity)
                    ?: mTempCipherDao.add(cipherDatabaseEntity)
        }
        fun deleteByDatabaseUri(databaseUri: Uri) {
            mTempCipherDao.firstOrNull { it.databaseUri == databaseUri.toString() }?.let {
                mTempCipherDao.remove(it)
            }
        }
        fun deleteAll() {
            mTempCipherDao.clear()
        }
    }

    override val notificationId: Int = 593

    override fun retrieveChannelId(): String {
        return CHANNEL_ADVANCED_UNLOCK_ID
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val deleteIntent = Intent(this, AdvancedUnlockNotificationService::class.java).apply {
            action = ACTION_REMOVE_KEYS
        }
        val pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val deviceCredential = PreferencesUtil.isDeviceCredentialUnlockEnable(this)
        val notificationBuilder =  buildNewNotification().apply {
            setSmallIcon(if (deviceCredential) {
                R.drawable.bolt
            } else {
                R.drawable.fingerprint
            })
            intent?.let {
                setContentTitle(getString(R.string.advanced_unlock))
            }
            setContentText(getString(R.string.advanced_unlock_tap_delete))
            setContentIntent(pendingDeleteIntent)
            // Unfortunately swipe is disabled in lollipop+
            setDeleteIntent(pendingDeleteIntent)
        }

        when (intent?.action) {
            ACTION_TIMEOUT -> {
                notificationTimeoutMilliSecs = PreferencesUtil.getAdvancedUnlockTimeout(this)
                // Not necessarily a foreground service
                if (mTimerJob == null && notificationTimeoutMilliSecs != TimeoutHelper.NEVER) {
                    mTimerJob = CoroutineScope(Dispatchers.Main).launch {
                        val maxPos = 100
                        val posDurationMills = notificationTimeoutMilliSecs / maxPos
                        for (pos in maxPos downTo 0) {
                            notificationBuilder.setProgress(maxPos, pos, false)
                            startForeground(notificationId, notificationBuilder.build())
                            delay(posDurationMills)
                            if (pos <= 0) {
                                stopSelf()
                            }
                        }
                        notificationManager?.cancel(notificationId)
                        mTimerJob = null
                        cancel()
                    }
                } else {
                    startForeground(notificationId, notificationBuilder.build())
                }
            }
            ACTION_REMOVE_KEYS -> {
                stopSelf()
            }
            else -> {}
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        mTempCipherDao = ArrayList()
    }

    override fun onDestroy() {
        mTempCipherDao.clear()
        mTimerJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ADVANCED_UNLOCK_ID = "com.kunzisoft.keepass.notification.channel.unlock"

        private const val ACTION_TIMEOUT = "ACTION_TIMEOUT"
        private const val ACTION_REMOVE_KEYS = "ACTION_REMOVE_KEYS"

        fun startServiceForTimeout(context: Context) {
            context.startService(Intent(context, AdvancedUnlockNotificationService::class.java).apply {
                action = ACTION_TIMEOUT
            })
        }
    }
}