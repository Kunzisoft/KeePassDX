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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.LOCK_ACTION

class DatabaseOpenNotificationService: LockNotificationService() {

    override val notificationId: Int = 340

    private fun stopNotificationAndSendLock() {
        // Send lock action
        sendBroadcast(Intent(LOCK_ACTION))
        // Stop the service
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            ACTION_CLOSE_DATABASE -> {
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
                    notificationManager?.notify(notificationId, buildNewNotification().apply {
                        setSmallIcon(R.drawable.notification_ic_database_open)
                        setContentTitle(getString(R.string.database_opened))
                        setContentText(database.name + " (" + database.version + ")")
                        setAutoCancel(false)
                        setContentIntent(pendingDatabaseIntent)
                        setDeleteIntent(pendingDeleteIntent)
                    }.build())
                } else {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    companion object {
        const val ACTION_CLOSE_DATABASE = "ACTION_CLOSE_DATABASE"

        fun startIfAllowed(context: Context) {
            if (PreferencesUtil.isPersistentNotificationEnable(context)) {
                // Start the opening notification
                context.startService(Intent(context, DatabaseOpenNotificationService::class.java))
            }
        }

        fun stop(context: Context) {
            // Stop the opening notification
            context.stopService(Intent(context, DatabaseOpenNotificationService::class.java))
        }
    }

}