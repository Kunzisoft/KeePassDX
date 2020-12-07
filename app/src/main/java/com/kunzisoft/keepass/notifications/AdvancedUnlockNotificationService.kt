package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity

class AdvancedUnlockNotificationService : NotificationService() {

    private lateinit var mTempCipherDao: ArrayList<CipherDatabaseEntity>

    private var mActionTaskBinder = AdvancedUnlockBinder()

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
        val notificationBuilder =  buildNewNotification().apply {
            setSmallIcon(R.drawable.bolt)
            intent?.let {
                setContentTitle(getString(R.string.advanced_unlock))
            }
            setContentText(getString(R.string.advanced_unlock_tap_delete))
            setContentIntent(pendingDeleteIntent)
            // Unfortunately swipe is disabled in lollipop+
            setDeleteIntent(pendingDeleteIntent)
        }
        // Not necessarilly a foreground service
        // startForeground(notificationId, notificationBuilder.build())
        notificationManager?.notify(notificationId, notificationBuilder.build())

        if (intent?.action == ACTION_REMOVE_KEYS) {
            stopSelf()
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        mTempCipherDao = ArrayList()
    }

    override fun onDestroy() {
        mTempCipherDao.clear()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ADVANCED_UNLOCK_ID = "com.kunzisoft.keepass.notification.channel.unlock"

        const val ACTION_REMOVE_KEYS = "ACTION_REMOVE_KEYS"
    }
}