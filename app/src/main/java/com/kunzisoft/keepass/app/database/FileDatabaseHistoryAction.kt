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

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.SingletonHolderParameter
import com.kunzisoft.keepass.utils.decodeUri
import com.kunzisoft.keepass.utils.parseUri
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileDatabaseHistoryAction(private val applicationContext: Context) {

    private val databaseFileHistoryDao =
            AppDatabase.getDatabase(applicationContext).fileDatabaseHistoryDao()

    fun getDatabaseFile(
        databaseUri: Uri,
        databaseFileResult: (DatabaseFile?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                val fileDatabaseHistoryEntity =
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                val fileDatabaseInfo = FileDatabaseInfo(
                    applicationContext,
                    databaseUri,
                )
                DatabaseFile(
                    databaseUri = databaseUri,
                    keyFileUri = fileDatabaseHistoryEntity?.keyFileUri?.parseUri(),
                    hardwareKey = HardwareKey.getHardwareKeyFromString(fileDatabaseHistoryEntity?.hardwareKey),
                    readOnly = fileDatabaseHistoryEntity?.readOnly,
                    userVerification = fileDatabaseHistoryEntity?.userVerification,
                    databaseDecodedPath = fileDatabaseHistoryEntity?.databaseUri?.decodeUri(),
                    databaseAlias = fileDatabaseInfo.retrieveDatabaseAlias(
                        fileDatabaseHistoryEntity?.databaseAlias ?: "",
                    ),
                    databaseFileExists = fileDatabaseInfo.exists,
                    databaseLastModified = fileDatabaseInfo.getLastModificationString(),
                    databaseSize = fileDatabaseInfo.getSizeString(),
                )
            }
            databaseFileResult.invoke(result)
        }
    }

    fun getKeyFileUriByDatabaseUri(
        databaseUri: Uri,
        keyFileUriResultListener: (Uri?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
            }
            result?.let { fileHistoryEntity ->
                fileHistoryEntity.keyFileUri?.let { keyFileUri ->
                    keyFileUriResultListener.invoke(keyFileUri.parseUri())
                }
            } ?: keyFileUriResultListener.invoke(null)
        }
    }

    fun getDatabaseFileList(databaseFileListResult: (List<DatabaseFile>) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                val hideBrokenLocations =
                    PreferencesUtil.hideBrokenLocations(
                        applicationContext,
                    )
                // Show only uri accessible
                val databaseFileListLoaded = mutableListOf<DatabaseFile>()
                databaseFileHistoryDao.getAll().forEach { fileDatabaseHistoryEntity ->
                    val fileDatabaseInfo = FileDatabaseInfo(
                        applicationContext,
                        fileDatabaseHistoryEntity.databaseUri,
                    )
                    if (hideBrokenLocations && fileDatabaseInfo.exists
                        || !hideBrokenLocations
                    ) {
                        databaseFileListLoaded.add(
                            DatabaseFile(
                                databaseUri = fileDatabaseHistoryEntity.databaseUri.parseUri(),
                                keyFileUri = fileDatabaseHistoryEntity.keyFileUri?.parseUri(),
                                hardwareKey = HardwareKey.getHardwareKeyFromString(
                                    fileDatabaseHistoryEntity.hardwareKey,
                                ),
                                readOnly = fileDatabaseHistoryEntity.readOnly,
                                userVerification = fileDatabaseHistoryEntity.userVerification,
                                databaseDecodedPath = fileDatabaseHistoryEntity.databaseUri.decodeUri(),
                                databaseAlias = fileDatabaseInfo.retrieveDatabaseAlias(
                                    fileDatabaseHistoryEntity.databaseAlias,
                                ),
                                databaseFileExists = fileDatabaseInfo.exists,
                                databaseLastModified = fileDatabaseInfo.getLastModificationString(),
                                databaseSize = fileDatabaseInfo.getSizeString(),
                            )
                        )
                    }
                }
                databaseFileListLoaded
            }
            databaseFileListResult.invoke(result)
        }
    }

    fun addOrUpdateDatabaseUri(
        databaseUri: Uri,
        keyFileUri: Uri? = null,
        hardwareKey: HardwareKey? = null,
        databaseFileAddedOrUpdatedResult: ((DatabaseFile?) -> Unit)? = null
    ) {
        addOrUpdateDatabaseFile(DatabaseFile(
            databaseUri,
            keyFileUri,
            hardwareKey
        ), databaseFileAddedOrUpdatedResult)
    }

    fun addOrUpdateDatabaseFile(
        databaseFileToAddOrUpdate: DatabaseFile,
        databaseFileAddedOrUpdatedResult: ((DatabaseFile?) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                databaseFileToAddOrUpdate.databaseUri?.let { databaseUri ->
                    // Try to get info in database first
                    val fileDatabaseHistoryRetrieve =
                        databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())

                    // Complete alias if not exists
                    val fileDatabaseHistory =
                        FileDatabaseHistoryEntity(
                            databaseUri.toString(),
                            databaseAlias = databaseFileToAddOrUpdate.databaseAlias
                                ?: fileDatabaseHistoryRetrieve?.databaseAlias
                                ?: "",
                            keyFileUri = databaseFileToAddOrUpdate.keyFileUri?.toString(),
                            hardwareKey = databaseFileToAddOrUpdate.hardwareKey?.value,
                            readOnly = databaseFileToAddOrUpdate.readOnly
                                ?: fileDatabaseHistoryRetrieve?.readOnly,
                            userVerification = databaseFileToAddOrUpdate.userVerification
                                ?: fileDatabaseHistoryRetrieve?.userVerification
                                ?: PreferencesUtil.isUserVerificationModeEnabledByDefault(
                                    applicationContext,
                                ),
                            updated = System.currentTimeMillis(),
                        )

                    // Update values if history element not yet in the database
                    try {
                        if (fileDatabaseHistoryRetrieve == null) {
                            databaseFileHistoryDao.add(fileDatabaseHistory)
                        } else {
                            databaseFileHistoryDao.update(fileDatabaseHistory)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to add or update database history", e)
                    }

                    val fileDatabaseInfo =
                        FileDatabaseInfo(
                            applicationContext,
                            fileDatabaseHistory.databaseUri,
                        )
                    DatabaseFile(
                        databaseUri = fileDatabaseHistory.databaseUri.parseUri(),
                        keyFileUri = fileDatabaseHistory.keyFileUri?.parseUri(),
                        hardwareKey = HardwareKey.getHardwareKeyFromString(fileDatabaseHistory.hardwareKey),
                        readOnly = fileDatabaseHistory.readOnly,
                        userVerification = fileDatabaseHistory.userVerification,
                        databaseDecodedPath = fileDatabaseHistory.databaseUri.decodeUri(),
                        databaseAlias = fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistory.databaseAlias),
                        databaseFileExists = fileDatabaseInfo.exists,
                        databaseLastModified = fileDatabaseInfo.getLastModificationString(),
                        databaseSize = fileDatabaseInfo.getSizeString(),
                    )
                }
            }
            databaseFileAddedOrUpdatedResult?.invoke(result)
        }
    }

    fun deleteDatabaseFile(
        databaseFileToDelete: DatabaseFile,
        databaseFileDeletedResult: (DatabaseFile?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                databaseFileToDelete.databaseUri?.let { databaseUri ->
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                        ?.let { fileDatabaseHistory ->
                            val returnValue = databaseFileHistoryDao.delete(fileDatabaseHistory)
                            if (returnValue > 0) {
                                DatabaseFile(
                                    databaseUri = fileDatabaseHistory.databaseUri.parseUri(),
                                    keyFileUri = fileDatabaseHistory.keyFileUri?.parseUri(),
                                    hardwareKey = HardwareKey.getHardwareKeyFromString(
                                        fileDatabaseHistory.hardwareKey,
                                    ),
                                    readOnly = fileDatabaseHistory.readOnly,
                                    userVerification = fileDatabaseHistory.userVerification,
                                    databaseDecodedPath = fileDatabaseHistory.databaseUri.decodeUri(),
                                    databaseAlias = databaseFileToDelete.databaseAlias,
                                )
                            } else {
                                null
                            }
                        }
                }
            }
            databaseFileDeletedResult.invoke(result)
        }
    }

    fun deleteKeyFileByDatabaseUri(
        databaseUri: Uri,
        result: (() -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                databaseFileHistoryDao.deleteKeyFileByDatabaseUri(databaseUri.toString())
            }
            result?.invoke()
        }
    }

    fun deleteAllKeyFiles(result: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                databaseFileHistoryDao.deleteAllKeyFiles()
            }
            result?.invoke()
        }
    }

    fun deleteAll(result: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                databaseFileHistoryDao.deleteAll()
            }
            result?.invoke()
        }
    }

    companion object : SingletonHolderParameter<FileDatabaseHistoryAction, Context>(::FileDatabaseHistoryAction) {
        private val TAG = FileDatabaseHistoryAction::class.java.name
    }
}
