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
    private fun getTempCipherDao(tempCipherDaoRetrieved: (MutableList<CipherDatabaseEntity>?) -> Unit) {
        // Check if a service is currently running else do nothing
        if (mBinder != null) {
            tempCipherDaoRetrieved.invoke(mBinder?.getTempCipherDao())
        } else if (mServiceConnection == null) {
            mServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                    mBinder = (serviceBinder as AdvancedUnlockNotificationService.AdvancedUnlockBinder)
                    tempCipherDaoRetrieved.invoke(mBinder?.getTempCipherDao())
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
            getTempCipherDao { tempCipherDao ->
                cipherDatabaseResultListener.invoke(tempCipherDao?.firstOrNull { it.databaseUri == databaseUri.toString()})
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
            getTempCipherDao { tempCipherDao ->
                val cipherDatabaseRetrieve = tempCipherDao?.firstOrNull { it.databaseUri == cipherDatabaseEntity.databaseUri }
                cipherDatabaseRetrieve?.replaceContent(cipherDatabaseEntity)
                        ?: tempCipherDao?.add(cipherDatabaseEntity)
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
        getTempCipherDao { tempCipherDao ->
            tempCipherDao?.firstOrNull { it.databaseUri == databaseUri.toString() }?.let {
                tempCipherDao.remove(it)
            }
        }
        IOActionTask(
                {
                    cipherDatabaseDao.deleteByDatabaseUri(databaseUri.toString())
                },
                {
                    cipherDatabaseResultListener?.invoke()
                }
        ).execute()
    }

    fun deleteAll() {
        getTempCipherDao { tempCipherDao ->
            tempCipherDao?.clear()
        }
        IOActionTask(
                {
                    cipherDatabaseDao.deleteAll()
                }
        ).execute()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction)
}