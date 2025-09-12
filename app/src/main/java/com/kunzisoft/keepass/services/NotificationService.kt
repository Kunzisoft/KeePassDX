package com.kunzisoft.keepass.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.Instant


abstract class NotificationService : Service() {

    protected var notificationManager: NotificationManagerCompat? = null
    private var colorNotificationAccent: Int = 0

    protected var mTimerJob: Job? = null
    private var mReset: Boolean = false

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

    protected fun startForegroundCompat(notificationId: Int,
                                        builder: NotificationCompat.Builder,
                                        type: NotificationServiceType
    ) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceTimer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                FOREGROUND_SERVICE_TYPE_NONE
            }
            val foregroundType = when (type) {
                NotificationServiceType.DATABASE_TASK -> FOREGROUND_SERVICE_TYPE_DATA_SYNC
                NotificationServiceType.ATTACHMENT -> FOREGROUND_SERVICE_TYPE_DATA_SYNC
                NotificationServiceType.CLIPBOARD -> foregroundServiceTimer
                NotificationServiceType.KEYBOARD -> foregroundServiceTimer
                NotificationServiceType.DEVICE_UNLOCK -> foregroundServiceTimer
            }
            startForeground(notificationId, builder.build(), foregroundType)
        } else {
            startForeground(notificationId, builder.build())
        }
    }

    protected fun defineTimerJob(builder: NotificationCompat.Builder,
                                 type: NotificationServiceType,
                                 timeoutMilliseconds: Long,
                                 actionAfterASecond: (() -> Unit)? = null,
                                 actionEnd: () -> Unit) {
        mTimerJob?.cancel()
        mTimerJob = CoroutineScope(Dispatchers.Main).launch {
            if (timeoutMilliseconds > 0) {
                var startInstant = Instant.now().millis
                var currentTime = timeoutMilliseconds
                while (currentTime >= 0) {
                    // Reset the timer if needed
                    if (mReset) {
                        mReset = false
                        startInstant = Instant.now().millis
                        currentTime = timeoutMilliseconds
                    }
                    // Update every second
                    actionAfterASecond?.invoke()
                    builder.setProgress(100,
                        (currentTime * 100 / timeoutMilliseconds).toInt(),
                        false)
                    startForegroundCompat(notificationId, builder, type)
                    delay(1000)
                    currentTime = timeoutMilliseconds - (Instant.now().millis - startInstant)
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

    protected fun resetTimeJob() {
        mReset = true
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        Log.e(javaClass::class.simpleName, "The service took too long to execute")
        cancelNotification()
        stopSelf()
    }

    protected fun cancelNotification() {
        mTimerJob?.cancel()
        mTimerJob = null
        notificationManager?.cancel(notificationId)
    }

    override fun onDestroy() {
        cancelNotification()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "com.kunzisoft.keepass.notification.channel"
        private const val CHANNEL_NAME = "KeePassDX notification"

        fun checkNotificationsPermission(
            context: Context,
            showError: Boolean = true,
            action: () -> Unit
        ) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                action.invoke()
            } else {
                if (showError) {
                    Toast.makeText(
                        context,
                        R.string.warning_copy_permission,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}