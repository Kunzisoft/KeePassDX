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
package com.kunzisoft.keepass.app.database

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import com.kunzisoft.keepass.notifications.AdvancedUnlockNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.SingletonHolderParameter

class CipherDatabaseAction(context: Context) {

    private val applicationContext = context.applicationContext
    private val cipherDatabaseDao =
            AppDatabase
                    .getDatabase(applicationContext)
                    .cipherDatabaseDao()

    // Temp DAO to easily remove content if object no longer in memory
    private val useTempDao = PreferencesUtil.isTempAdvancedUnlockEnable(applicationContext)

    private val mIntentAdvancedUnlockService = Intent(applicationContext,
            AdvancedUnlockNotificationService::class.java)
    private var mBinder: AdvancedUnlockNotificationService.AdvancedUnlockBinder? = null
    private var mServiceConnection: ServiceConnection? = null

    fun initialize() {
        applicationContext.startService(mIntentAdvancedUnlockService)
    }

    @Synchronized
    private fun attachService(serviceAttached: () -> Unit) {
        // Check if a service is currently running else do nothing
        if (mBinder != null) {
            serviceAttached.invoke()
        } else if (mServiceConnection == null) {
            mServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                    mBinder = (serviceBinder as AdvancedUnlockNotificationService.AdvancedUnlockBinder)
                    serviceAttached.invoke()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    mBinder = null
                    mServiceConnection = null
                }
            }
            // bind Service
            mServiceConnection?.let {
                applicationContext.bindService(mIntentAdvancedUnlockService,
                        it,
                        Context.BIND_ABOVE_CLIENT)
            }
        }
    }

    fun getCipherDatabase(databaseUri: Uri,
                          cipherDatabaseResultListener: (CipherDatabaseEntity?) -> Unit) {
        if (useTempDao) {
            attachService {
                cipherDatabaseResultListener.invoke(mBinder?.getCipherDatabase(databaseUri))
            }
        } else {
            IOActionTask(
                    {
                        cipherDatabaseDao.getByDatabaseUri(databaseUri.toString())
                    },
                    {
                        cipherDatabaseResultListener.invoke(it)
                    }
            ).execute()
        }
    }

    fun containsCipherDatabase(databaseUri: Uri,
                               contains: (Boolean) -> Unit) {
        getCipherDatabase(databaseUri) {
            contains.invoke(it != null)
        }
    }

    fun addOrUpdateCipherDatabase(cipherDatabaseEntity: CipherDatabaseEntity,
                                  cipherDatabaseResultListener: (() -> Unit)? = null) {
        if (useTempDao) {
            attachService {
                mBinder?.addOrUpdateCipherDatabase(cipherDatabaseEntity)
                cipherDatabaseResultListener?.invoke()
            }
        } else {
            IOActionTask(
                    {
                        val cipherDatabaseRetrieve = cipherDatabaseDao.getByDatabaseUri(cipherDatabaseEntity.databaseUri)
                        // Update values if element not yet in the database
                        if (cipherDatabaseRetrieve == null) {
                            cipherDatabaseDao.add(cipherDatabaseEntity)
                        } else {
                            cipherDatabaseDao.update(cipherDatabaseEntity)
                        }
                    },
                    {
                        cipherDatabaseResultListener?.invoke()
                    }
            ).execute()
        }
    }

    fun deleteByDatabaseUri(databaseUri: Uri,
                            cipherDatabaseResultListener: (() -> Unit)? = null) {
        if (useTempDao) {
            attachService {
                mBinder?.deleteByDatabaseUri(databaseUri)
                cipherDatabaseResultListener?.invoke()
            }
        } else {
            IOActionTask(
                    {
                        cipherDatabaseDao.deleteByDatabaseUri(databaseUri.toString())
                    },
                    {
                        cipherDatabaseResultListener?.invoke()
                    }
            ).execute()
        }
    }

    fun deleteAll() {
        attachService {
            mBinder?.deleteAll()
        }
        IOActionTask(
                {
                    cipherDatabaseDao.deleteAll()
                }
        ).execute()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction)
}