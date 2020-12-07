package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity

class AdvancedUnlockNotificationService : NotificationService() {

    private lateinit var mTempCipherDao: ArrayList<CipherDatabaseEntity>

    private var mActionTaskBinder = AdvancedUnlockBinder()

    inner class AdvancedUnlockBinder: Binder() {
        fun getTempCipherDao(): MutableList<CipherDatabaseEntity> {
            return mTempCipherDao
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
        startForeground(notificationId, notificationBuilder.build())

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