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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.services.DeviceUnlockNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.SingletonHolderParameter
import java.util.LinkedList

class CipherDatabaseAction(context: Context) {

    private val applicationContext = context.applicationContext
    private val cipherDatabaseDao =
        AppDatabase.getDatabase(applicationContext).cipherDatabaseDao()

    // Temp DAO to easily remove content if object no longer in memory
    private var useTempDao = PreferencesUtil.isTempDeviceUnlockEnable(applicationContext)

    private var mBinder: DeviceUnlockNotificationService.DeviceUnlockBinder? = null
    private var mServiceConnection: ServiceConnection? = null

    private var mDatabaseListeners = LinkedList<CipherDatabaseListener>()
    private var mDeviceUnlockBroadcastReceiver = DeviceUnlockNotificationService.DeviceUnlockReceiver {
        deleteAll()
        removeAllDataAndDetach()
    }

    private fun reloadPreferences() {
        useTempDao = PreferencesUtil.isTempDeviceUnlockEnable(applicationContext)
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
        ContextCompat.registerReceiver(applicationContext, mDeviceUnlockBroadcastReceiver,
            IntentFilter().apply {
                addAction(DeviceUnlockNotificationService.REMOVE_DEVICE_UNLOCK_KEY_ACTION)
            }, ContextCompat.RECEIVER_EXPORTED
        )

        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                mBinder = (serviceBinder as DeviceUnlockNotificationService.DeviceUnlockBinder)
                performedAction.invoke()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                onClear()
            }
        }
        try {
            DeviceUnlockNotificationService.bindService(applicationContext,
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
            applicationContext.unregisterReceiver(mDeviceUnlockBroadcastReceiver)
        } catch (_: Exception) {}

        mServiceConnection?.let {
            DeviceUnlockNotificationService.unbindService(applicationContext, it)
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
        mDatabaseListeners.forEach { listener ->
            listener.onCipherDatabaseCleared()
        }
    }

    interface CipherDatabaseListener {
        fun onCipherDatabaseRetrieved(databaseUri: Uri, cipherDatabase: CipherEncryptDatabase?)
        fun onCipherDatabaseAddedOrUpdated(cipherDatabase: CipherEncryptDatabase)
        fun onCipherDatabaseDeleted(databaseUri: Uri)
        fun onAllCipherDatabasesDeleted()
        fun onCipherDatabaseCleared()
    }

    fun getCipherDatabase(databaseUri: Uri,
                          cipherDatabaseResultListener: ((CipherEncryptDatabase?) -> Unit)? = null) {
        if (useTempDao) {
            serviceActionTask {
                var cipherDatabase: CipherEncryptDatabase? = null
                mBinder?.getCipherDatabase(databaseUri)?.let { cipherDatabaseEntity ->
                    cipherDatabase = CipherEncryptDatabase().apply {
                        this.databaseUri = cipherDatabaseEntity.databaseUri.toUri()
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
                cipherDatabaseResultListener?.invoke(cipherDatabase) ?: run {
                    mDatabaseListeners.forEach { listener ->
                        listener.onCipherDatabaseRetrieved(databaseUri, cipherDatabase)
                    }
                }
            }
        } else {
            IOActionTask(
                {
                    cipherDatabaseDao.getByDatabaseUri(databaseUri.toString())
                        ?.let { cipherDatabaseEntity ->
                            CipherEncryptDatabase().apply {
                                this.databaseUri = cipherDatabaseEntity.databaseUri.toUri()
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
                { cipherDatabase ->
                    cipherDatabaseResultListener?.invoke(cipherDatabase) ?: run {
                        mDatabaseListeners.forEach { listener ->
                            listener.onCipherDatabaseRetrieved(databaseUri, cipherDatabase)
                        }
                    }
                }
            ).execute()
        }
    }

    private fun containsCipherDatabase(databaseUri: Uri?,
                               contains: (Boolean) -> Unit) {
        if (databaseUri == null) {
            contains.invoke(false)
        } else {
            getCipherDatabase(databaseUri) {
                contains.invoke(it != null)
            }
        }
    }

    fun resetCipherParameters(databaseUri: Uri?) {
        containsCipherDatabase(databaseUri) { contains ->
            if (contains) {
                mBinder?.resetTimer()
            }
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
                    cipherDatabaseResultListener?.invoke() ?: run {
                        mDatabaseListeners.forEach { listener ->
                            listener.onCipherDatabaseAddedOrUpdated(cipherEncryptDatabase)
                        }
                    }
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
                        cipherDatabaseResultListener?.invoke() ?: run {
                            mDatabaseListeners.forEach { listener ->
                                listener.onCipherDatabaseAddedOrUpdated(cipherEncryptDatabase)
                            }
                        }
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
                cipherDatabaseResultListener?.invoke() ?: run {
                    mDatabaseListeners.forEach { listener ->
                        listener.onCipherDatabaseDeleted(databaseUri)
                    }
                }
            }
        } else {
            IOActionTask(
                {
                    cipherDatabaseDao.deleteByDatabaseUri(databaseUri.toString())
                },
                {
                    cipherDatabaseResultListener?.invoke() ?: run {
                        mDatabaseListeners.forEach { listener ->
                            listener.onCipherDatabaseDeleted(databaseUri)
                        }
                    }
                }
            ).execute()
        }
        reloadPreferences()
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
        mDatabaseListeners.forEach { listener ->
            listener.onAllCipherDatabasesDeleted()
        }
        // Unbind
        removeAllDataAndDetach()
        reloadPreferences()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction) {
        private val TAG = CipherDatabaseAction::class.java.name
    }
}
