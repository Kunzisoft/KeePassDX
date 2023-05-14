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
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Base64
import android.util.Log
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.services.AdvancedUnlockNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.SingletonHolderParameter
import java.util.LinkedList

class CipherDatabaseAction(context: Context) {

    private val applicationContext = context.applicationContext
    private val cipherDatabaseDao =
        AppDatabase.getDatabase(applicationContext).cipherDatabaseDao()

    // Temp DAO to easily remove content if object no longer in memory
    private var useTempDao = PreferencesUtil.isTempAdvancedUnlockEnable(applicationContext)

    private var mBinder: AdvancedUnlockNotificationService.AdvancedUnlockBinder? = null
    private var mServiceConnection: ServiceConnection? = null

    private var mDatabaseListeners = LinkedList<CipherDatabaseListener>()
    private var mAdvancedUnlockBroadcastReceiver = AdvancedUnlockNotificationService.AdvancedUnlockReceiver {
        deleteAll()
        removeAllDataAndDetach()
    }

    fun reloadPreferences() {
        useTempDao = PreferencesUtil.isTempAdvancedUnlockEnable(applicationContext)
    }

    @Synchronized
    private fun serviceActionTask(startService: Boolean = false, performedAction: () -> Unit) {
        // Check if a service is currently running else call action without info
        if (startService && mServiceConnection == null) {
            attachService(performedAction)
        } else {
            performedAction.invoke()
        }
    }

    @Synchronized
    private fun attachService(performedAction: () -> Unit) {
        applicationContext.registerReceiver(mAdvancedUnlockBroadcastReceiver, IntentFilter().apply {
            addAction(AdvancedUnlockNotificationService.REMOVE_ADVANCED_UNLOCK_KEY_ACTION)
        })

        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                mBinder = (serviceBinder as AdvancedUnlockNotificationService.AdvancedUnlockBinder)
                performedAction.invoke()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                onClear()
            }
        }
        try {
            AdvancedUnlockNotificationService.bindService(applicationContext,
                    mServiceConnection!!,
                Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start cipher action", e)
            performedAction.invoke()
        }
    }

    @Synchronized
    private fun detachService() {
        try {
            applicationContext.unregisterReceiver(mAdvancedUnlockBroadcastReceiver)
        } catch (e: Exception) {}

        mServiceConnection?.let {
            AdvancedUnlockNotificationService.unbindService(applicationContext, it)
        }
    }

    private fun removeAllDataAndDetach() {
        detachService()
        onClear()
    }

    fun registerDatabaseListener(listenerCipher: CipherDatabaseListener) {
        mDatabaseListeners.add(listenerCipher)
    }

    fun unregisterDatabaseListener(listenerCipher: CipherDatabaseListener) {
        mDatabaseListeners.remove(listenerCipher)
    }

    private fun onClear() {
        mBinder = null
        mServiceConnection = null
        mDatabaseListeners.forEach {
            it.onCipherDatabaseCleared()
        }
    }

    interface CipherDatabaseListener {
        fun onCipherDatabaseCleared()
    }

    fun getCipherDatabase(databaseUri: Uri,
                          cipherDatabaseResultListener: (CipherEncryptDatabase?) -> Unit) {
        if (useTempDao) {
            serviceActionTask {
                var cipherDatabase: CipherEncryptDatabase? = null
                mBinder?.getCipherDatabase(databaseUri)?.let { cipherDatabaseEntity ->
                    cipherDatabase = CipherEncryptDatabase().apply {
                        this.databaseUri = Uri.parse(cipherDatabaseEntity.databaseUri)
                        this.encryptedValue = Base64.decode(
                            cipherDatabaseEntity.encryptedValue,
                            BASE64_FLAG
                        )
                        this.specParameters = Base64.decode(
                            cipherDatabaseEntity.specParameters,
                            BASE64_FLAG
                        )
                    }
                }
                cipherDatabaseResultListener.invoke(cipherDatabase)
            }
        } else {
            IOActionTask(
                {
                    cipherDatabaseDao.getByDatabaseUri(databaseUri.toString())
                        ?.let { cipherDatabaseEntity ->
                            CipherEncryptDatabase().apply {
                                this.databaseUri = Uri.parse(cipherDatabaseEntity.databaseUri)
                                this.encryptedValue = Base64.decode(
                                    cipherDatabaseEntity.encryptedValue,
                                    Base64.NO_WRAP
                                )
                                this.specParameters = Base64.decode(
                                    cipherDatabaseEntity.specParameters,
                                    Base64.NO_WRAP
                                )
                            }
                        }
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

    fun addOrUpdateCipherDatabase(cipherEncryptDatabase: CipherEncryptDatabase,
                                  cipherDatabaseResultListener: (() -> Unit)? = null) {
        cipherEncryptDatabase.databaseUri?.let { databaseUri ->

            val cipherDatabaseEntity = CipherDatabaseEntity(
                databaseUri.toString(),
                Base64.encodeToString(cipherEncryptDatabase.encryptedValue, BASE64_FLAG),
                Base64.encodeToString(cipherEncryptDatabase.specParameters, BASE64_FLAG),
            )

            if (useTempDao) {
                // The only case to create service (not needed to get an info)
                serviceActionTask(true) {
                    mBinder?.addOrUpdateCipherDatabase(cipherDatabaseEntity)
                    cipherDatabaseResultListener?.invoke()
                }
            } else {
                IOActionTask(
                    {
                        val cipherDatabaseRetrieve =
                            cipherDatabaseDao.getByDatabaseUri(cipherDatabaseEntity.databaseUri)
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
    }

    fun deleteByDatabaseUri(databaseUri: Uri,
                            cipherDatabaseResultListener: (() -> Unit)? = null) {
        if (useTempDao) {
            serviceActionTask {
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
        if (useTempDao) {
            serviceActionTask {
                mBinder?.deleteAll()
            }
        }
        // To erase the residues
        IOActionTask(
            {
                cipherDatabaseDao.deleteAll()
            }
        ).execute()
        // Unbind
        removeAllDataAndDetach()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction) {
        private val TAG = CipherDatabaseAction::class.java.name
    }
}
