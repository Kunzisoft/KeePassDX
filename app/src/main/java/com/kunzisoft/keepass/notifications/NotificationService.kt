package com.kunzisoft.keepass.notifications

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


abstract class NotificationService : Service() {

    protected var notificationManager: NotificationManagerCompat? = null
    private var colorNotificationAccent: Int = 0

    protected abstract val notificationId: Int

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = NotificationManagerCompat.from(this)

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager?.getNotificationChannel(CHANNEL_ID_KEEPASS) == null) {
                val channel = NotificationChannel(CHANNEL_ID_KEEPASS,
                        CHANNEL_NAME_KEEPASS,
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
        return NotificationCompat.Builder(this, CHANNEL_ID_KEEPASS)
                .setColor(colorNotificationAccent)
                //TODO .setGroup(GROUP_KEEPASS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }

    // TODO only for > lollipop
    protected fun buildSummaryNotification(): NotificationCompat.Builder {
        return buildNewNotification().apply {
            // TODO Ic setSmallIcon(R.drawable.notification_ic_data_usage_24dp)
            setGroupSummary(true)
        }
    }

    override fun onDestroy() {
        notificationManager?.cancel(notificationId)

        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID_KEEPASS = "com.kunzisoft.keepass.notification.channel"
        const val CHANNEL_NAME_KEEPASS = "KeePassDX notification"
        const val GROUP_KEEPASS = "GROUP_KEEPASS"
        const val SUMMARY_ID = 0
    }
}