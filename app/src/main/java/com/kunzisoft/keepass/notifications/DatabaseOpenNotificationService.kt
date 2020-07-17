/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.closeDatabase

class DatabaseOpenNotificationService: LockNotificationService() {

    override val notificationId: Int = 340

    private fun stopNotificationAndSendLock() {
        // Send lock action
        sendBroadcast(Intent(LOCK_ACTION))
    }

    override fun actionOnLock() {
        closeDatabase()
        // Remove the lock timer (no more needed if it exists)
        TimeoutHelper.cancelLockTimer(this)
        // Service is stopped after receive the broadcast
        super.actionOnLock()
    }

    private fun checkIntent(intent: Intent?) {
        val notificationBuilder = buildNewNotification().apply {
            setSmallIcon(R.drawable.notification_ic_database_open)
            setContentTitle(getString(R.string.database_opened))
            setAutoCancel(false)
        }

        when(intent?.action) {
            ACTION_CLOSE_DATABASE -> {
                startForeground(notificationId, notificationBuilder.build())
                stopNotificationAndSendLock()
            }
            else -> {
                val databaseIntent = Intent(this, GroupActivity::class.java)
                var pendingDatabaseFlag = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pendingDatabaseFlag = PendingIntent.FLAG_IMMUTABLE
                }
                val pendingDatabaseIntent = PendingIntent.getActivity(this, 0, databaseIntent, pendingDatabaseFlag)
                val deleteIntent = Intent(this, DatabaseOpenNotificationService::class.java).apply {
                    action = ACTION_CLOSE_DATABASE
                }
                val pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val database = Database.getInstance()
                if (database.loaded) {
                    startForeground(notificationId, notificationBuilder.apply {
                        setContentText(database.name + " (" + database.version + ")")
                        setContentIntent(pendingDatabaseIntent)
                        // Unfortunately swipe is disabled in lollipop+
                        setDeleteIntent(pendingDeleteIntent)
                        addAction(R.drawable.ic_lock_white_24dp, getString(R.string.lock),
                                pendingDeleteIntent)
                    }.build())
                } else {
                    startForeground(notificationId, notificationBuilder.build())
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        checkIntent(intent)
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        checkIntent(intent)
        return START_STICKY
    }

    companion object {
        const val ACTION_CLOSE_DATABASE = "ACTION_CLOSE_DATABASE"

        fun start(context: Context) {
            // Start the opening notification, keep it active to receive lock
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, DatabaseOpenNotificationService::class.java))
            } else {
                context.startService(Intent(context, DatabaseOpenNotificationService::class.java))
            }
        }

        fun stop(context: Context) {
            // Stop the opening notification
            context.stopService(Intent(context, DatabaseOpenNotificationService::class.java))
        }
    }

}