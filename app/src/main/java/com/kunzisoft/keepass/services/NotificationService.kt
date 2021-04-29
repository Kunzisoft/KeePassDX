package com.kunzisoft.keepass.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import kotlinx.coroutines.*


abstract class NotificationService : Service() {

    protected var notificationManager: NotificationManagerCompat? = null
    private var colorNotificationAccent: Int = 0

    protected var mTimerJob: Job? = null

    protected abstract val notificationId: Int

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    open fun retrieveChannelId(): String {
        return CHANNEL_ID
    }

    open fun retrieveChannelName(): String {
        return CHANNEL_NAME
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = NotificationManagerCompat.from(this)

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager?.getNotificationChannel(retrieveChannelId()) == null) {
                val channel = NotificationChannel(retrieveChannelId(),
                        retrieveChannelName(),
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }

        // Get the color
        setTheme(Stylish.getThemeId(this))
        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        colorNotificationAccent = typedValue.data
    }

    protected fun buildNewNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, retrieveChannelId())
                .setColor(colorNotificationAccent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }

    protected fun buildSummaryNotification(): NotificationCompat.Builder {
        return buildNewNotification().apply {
            setGroupSummary(true)
        }
    }

    protected fun defineTimerJob(builder: NotificationCompat.Builder,
                                 timeoutMilliseconds: Long,
                                 actionAfterASecond: (() -> Unit)? = null,
                                 actionEnd: () -> Unit) {
        mTimerJob?.cancel()
        mTimerJob = CoroutineScope(Dispatchers.Main).launch {
            if (timeoutMilliseconds > 0) {
                val timeoutInSeconds = timeoutMilliseconds / 1000L
                for (currentTime in timeoutInSeconds downTo 0) {
                    actionAfterASecond?.invoke()
                    builder.setProgress(100,
                            (currentTime * 100 / timeoutInSeconds).toInt(),
                            false)
                    startForeground(notificationId, builder.build())
                    delay(1000)
                    if (currentTime <= 0) {
                        actionEnd()
                    }
                }
            } else {
                // If timeout is 0, run action once
                actionEnd()
            }
            notificationManager?.cancel(notificationId)
            mTimerJob = null
            cancel()
        }
    }

    override fun onDestroy() {
        mTimerJob?.cancel()
        mTimerJob = null
        notificationManager?.cancel(notificationId)

        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "com.kunzisoft.keepass.notification.channel"
        private const val CHANNEL_NAME = "KeePassDX notification"
    }
}