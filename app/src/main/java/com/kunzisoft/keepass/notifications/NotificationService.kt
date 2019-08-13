package com.kunzisoft.keepass.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.TypedValue
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish


abstract class NotificationService : Service() {

    protected var notificationManager: NotificationManager? = null
    private var colorNotificationAccent: Int = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_KEEPASS,
                    CHANNEL_NAME_KEEPASS,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
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
                .setGroup(GROUP_KEEPASS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }

}

const val CHANNEL_ID_KEEPASS = "com.kunzisoft.keepass.notification.channel"
const val CHANNEL_NAME_KEEPASS = "KeePass DX notification"
const val GROUP_KEEPASS = "GROUP_KEEPASS"