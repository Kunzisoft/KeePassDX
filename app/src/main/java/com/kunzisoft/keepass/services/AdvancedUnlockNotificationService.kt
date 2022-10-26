package com.kunzisoft.keepass.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper

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

    override fun retrieveChannelName(): String {
        return getString(R.string.advanced_unlock)
    }

    override fun onCreate() {
        super.onCreate()
        mTempCipherDao = ArrayList()
    }

    // It's simpler to use pendingIntent to perform REMOVE_ADVANCED_UNLOCK_KEY_ACTION
    // because can be directly broadcast to another module or app
    @SuppressLint("LaunchActivityFromNotification")
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        val pendingDeleteIntent = PendingIntent.getBroadcast(this,
            4577,
            Intent(REMOVE_ADVANCED_UNLOCK_KEY_ACTION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            })
        val biometricUnlockEnabled = PreferencesUtil.isBiometricUnlockEnable(this)
        val notificationBuilder = buildNewNotification().apply {
            setSmallIcon(if (biometricUnlockEnabled) {
                R.drawable.notification_ic_fingerprint_unlock_24dp
            } else {
                R.drawable.notification_ic_device_unlock_24dp
            })
            setContentTitle(getString(R.string.advanced_unlock))
            setContentText(getString(R.string.advanced_unlock_tap_delete))
            setContentIntent(pendingDeleteIntent)
            // Unfortunately swipe is disabled in lollipop+
            setDeleteIntent(pendingDeleteIntent)
        }

        val notificationTimeoutMilliSecs = PreferencesUtil.getAdvancedUnlockTimeout(this)
        // Not necessarily a foreground service
        if (mTimerJob == null && notificationTimeoutMilliSecs != TimeoutHelper.NEVER) {
            defineTimerJob(notificationBuilder, notificationTimeoutMilliSecs) {
                sendBroadcast(Intent(REMOVE_ADVANCED_UNLOCK_KEY_ACTION))
            }
        } else {
            startForeground(notificationId, notificationBuilder.build())
        }

        return mActionTaskBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        mTempCipherDao.clear()
        super.onDestroy()
    }

    class AdvancedUnlockReceiver(var removeKeyAction: () -> Unit): BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let {
                when (it) {
                    REMOVE_ADVANCED_UNLOCK_KEY_ACTION -> {
                        removeKeyAction.invoke()
                    }
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ADVANCED_UNLOCK_ID = "com.kunzisoft.keepass.notification.channel.unlock"
        const val REMOVE_ADVANCED_UNLOCK_KEY_ACTION = "com.kunzisoft.keepass.REMOVE_ADVANCED_UNLOCK_KEY"

        // Only one service connection
        fun bindService(context: Context, serviceConnection: ServiceConnection, flags: Int) {
            context.bindService(Intent(context,
                AdvancedUnlockNotificationService::class.java),
                    serviceConnection,
                    flags)
        }

        fun unbindService(context: Context, serviceConnection: ServiceConnection) {
            context.unbindService(serviceConnection)
        }
    }
}
