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
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.SingletonHolderParameter
import com.kunzisoft.keepass.utils.decodeUri
import com.kunzisoft.keepass.utils.parseUri
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo

class FileDatabaseHistoryAction(private val applicationContext: Context) {

    private val databaseFileHistoryDao =
            AppDatabase.getDatabase(applicationContext).fileDatabaseHistoryDao()

    fun getDatabaseFile(databaseUri: Uri,
                        databaseFileResult: (DatabaseFile?) -> Unit) {
        IOActionTask(
            {
                val fileDatabaseHistoryEntity =
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                val fileDatabaseInfo = FileDatabaseInfo(
                    applicationContext,
                    databaseUri)
                DatabaseFile(
                    databaseUri,
                    fileDatabaseHistoryEntity?.keyFileUri?.parseUri(),
                    HardwareKey.getHardwareKeyFromString(fileDatabaseHistoryEntity?.hardwareKey),
                    fileDatabaseHistoryEntity?.databaseUri?.decodeUri(),
                    fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistoryEntity?.databaseAlias
                        ?: ""),
                    fileDatabaseInfo.exists,
                    fileDatabaseInfo.getLastModificationString(),
                    fileDatabaseInfo.getSizeString()
                )
            },
            {
                databaseFileResult.invoke(it)
            }
        ).execute()
    }

    fun getKeyFileUriByDatabaseUri(databaseUri: Uri,
                                   keyFileUriResultListener: (Uri?) -> Unit) {
        IOActionTask(
            {
                databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
            },
            {
                it?.let { fileHistoryEntity ->
                    fileHistoryEntity.keyFileUri?.let { keyFileUri ->
                        keyFileUriResultListener.invoke(keyFileUri.parseUri())
                    }
                } ?: keyFileUriResultListener.invoke(null)
            }
        ).execute()
    }

    fun getDatabaseFileList(databaseFileListResult: (List<DatabaseFile>) -> Unit) {
        IOActionTask(
            {
                val hideBrokenLocations =
                    PreferencesUtil.hideBrokenLocations(
                        applicationContext)
                // Show only uri accessible
                val databaseFileListLoaded = ArrayList<DatabaseFile>()
                databaseFileHistoryDao.getAll().forEach { fileDatabaseHistoryEntity ->
                    val fileDatabaseInfo = FileDatabaseInfo(
                        applicationContext,
                        fileDatabaseHistoryEntity.databaseUri)
                    if (hideBrokenLocations && fileDatabaseInfo.exists
                        || !hideBrokenLocations
                    ) {
                        databaseFileListLoaded.add(
                            DatabaseFile(
                                fileDatabaseHistoryEntity.databaseUri.parseUri(),
                                fileDatabaseHistoryEntity.keyFileUri?.parseUri(),
                                HardwareKey.getHardwareKeyFromString(fileDatabaseHistoryEntity.hardwareKey),
                                fileDatabaseHistoryEntity.databaseUri.decodeUri(),
                                fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistoryEntity.databaseAlias),
                                fileDatabaseInfo.exists,
                                fileDatabaseInfo.getLastModificationString(),
                                fileDatabaseInfo.getSizeString()
                            )
                        )
                    }
                }
                databaseFileListLoaded
            },
            { databaseFileList ->
                databaseFileList?.let {
                    databaseFileListResult.invoke(it)
                }
            }
        ).execute()
    }

    fun addOrUpdateDatabaseUri(databaseUri: Uri,
                               keyFileUri: Uri? = null,
                               hardwareKey: HardwareKey? = null,
                               databaseFileAddedOrUpdatedResult: ((DatabaseFile?) -> Unit)? = null) {
        addOrUpdateDatabaseFile(DatabaseFile(
            databaseUri,
            keyFileUri,
            hardwareKey
        ), databaseFileAddedOrUpdatedResult)
    }

    fun addOrUpdateDatabaseFile(databaseFileToAddOrUpdate: DatabaseFile,
                                databaseFileAddedOrUpdatedResult: ((DatabaseFile?) -> Unit)? = null) {
        IOActionTask(
            {
                databaseFileToAddOrUpdate.databaseUri?.let { databaseUri ->
                    // Try to get info in database first
                    val fileDatabaseHistoryRetrieve =
                        databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())

                    // Complete alias if not exists
                    val fileDatabaseHistory =
                        FileDatabaseHistoryEntity(
                            databaseUri.toString(),
                            databaseFileToAddOrUpdate.databaseAlias
                                ?: fileDatabaseHistoryRetrieve?.databaseAlias
                                ?: "",
                            databaseFileToAddOrUpdate.keyFileUri?.toString(),
                            databaseFileToAddOrUpdate.hardwareKey?.value,
                            System.currentTimeMillis()
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
                        FileDatabaseInfo(applicationContext,
                            fileDatabaseHistory.databaseUri)
                    DatabaseFile(
                        fileDatabaseHistory.databaseUri.parseUri(),
                        fileDatabaseHistory.keyFileUri?.parseUri(),
                        HardwareKey.getHardwareKeyFromString(fileDatabaseHistory.hardwareKey),
                        fileDatabaseHistory.databaseUri.decodeUri(),
                        fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistory.databaseAlias),
                        fileDatabaseInfo.exists,
                        fileDatabaseInfo.getLastModificationString(),
                        fileDatabaseInfo.getSizeString()
                    )
                }
            },
            {
                databaseFileAddedOrUpdatedResult?.invoke(it)
            }
        ).execute()
    }

    fun deleteDatabaseFile(databaseFileToDelete: DatabaseFile,
                           databaseFileDeletedResult: (DatabaseFile?) -> Unit) {
        IOActionTask(
            {
                databaseFileToDelete.databaseUri?.let { databaseUri ->
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                        ?.let { fileDatabaseHistory ->
                            val returnValue = databaseFileHistoryDao.delete(fileDatabaseHistory)
                            if (returnValue > 0) {
                                DatabaseFile(
                                    fileDatabaseHistory.databaseUri.parseUri(),
                                    fileDatabaseHistory.keyFileUri?.parseUri(),
                                    HardwareKey.getHardwareKeyFromString(fileDatabaseHistory.hardwareKey),
                                    fileDatabaseHistory.databaseUri.decodeUri(),
                                    databaseFileToDelete.databaseAlias
                                )
                            } else {
                                null
                            }
                        }
                }
            },
            {
                databaseFileDeletedResult.invoke(it)
            }
        ).execute()
    }

    fun deleteKeyFileByDatabaseUri(databaseUri: Uri,
                                   result: (() ->Unit)? = null) {
        IOActionTask(
            {
                databaseFileHistoryDao.deleteKeyFileByDatabaseUri(databaseUri.toString())
            },
            {
                result?.invoke()
            }
        ).execute()
    }

    fun deleteAllKeyFiles(result: (() ->Unit)? = null) {
        IOActionTask(
            {
                databaseFileHistoryDao.deleteAllKeyFiles()
            },
            {
                result?.invoke()
            }
        ).execute()
    }

    fun deleteAll(result: (() ->Unit)? = null) {
        IOActionTask(
            {
                databaseFileHistoryDao.deleteAll()
            },
            {
                result?.invoke()
            }
        ).execute()
    }

    companion object : SingletonHolderParameter<FileDatabaseHistoryAction, Context>(::FileDatabaseHistoryAction) {
        private val TAG = FileDatabaseHistoryAction::class.java.name
    }
}
