package com.kunzisoft.keepass.app.database

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtilDatabase
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo

class FileDatabaseHistoryAction(private val applicationContext: Context) {

    private val databaseFileHistoryDao =
            AppDatabase.getDatabase(applicationContext)
                .fileDatabaseHistoryDao()

    fun getDatabaseFile(databaseUri: Uri,
                        databaseFileResult: (com.kunzisoft.keepass.model.DatabaseFile?) -> Unit) {
        IOActionTask(
            {
                val fileDatabaseHistoryEntity =
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                val fileDatabaseInfo = FileDatabaseInfo(
                    applicationContext,
                    databaseUri)
                DatabaseFile(
                    databaseUri,
                    UriUtilDatabase.parse(fileDatabaseHistoryEntity?.keyFileUri),
                    HardwareKey.getHardwareKeyFromString(fileDatabaseHistoryEntity?.hardwareKey),
                    UriUtilDatabase.decode(fileDatabaseHistoryEntity?.databaseUri),
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
                        keyFileUriResultListener.invoke(UriUtilDatabase.parse(
                            keyFileUri))
                    }
                } ?: keyFileUriResultListener.invoke(null)
            }
        ).execute()
    }

    fun getDatabaseFileList(databaseFileListResult: (List<com.kunzisoft.keepass.model.DatabaseFile>) -> Unit) {
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
                                UriUtilDatabase.parse(fileDatabaseHistoryEntity.databaseUri),
                                UriUtilDatabase.parse(fileDatabaseHistoryEntity.keyFileUri),
                                HardwareKey.getHardwareKeyFromString(fileDatabaseHistoryEntity.hardwareKey),
                                UriUtilDatabase.decode(fileDatabaseHistoryEntity.databaseUri),
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
                               databaseFileAddedOrUpdatedResult: ((com.kunzisoft.keepass.model.DatabaseFile?) -> Unit)? = null) {
        addOrUpdateDatabaseFile(com.kunzisoft.keepass.model.DatabaseFile(
            databaseUri,
            keyFileUri,
            hardwareKey
        ), databaseFileAddedOrUpdatedResult)
    }

    fun addOrUpdateDatabaseFile(databaseFileToAddOrUpdate: com.kunzisoft.keepass.model.DatabaseFile,
                                databaseFileAddedOrUpdatedResult: ((com.kunzisoft.keepass.model.DatabaseFile?) -> Unit)? = null) {
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
                        UriUtilDatabase.parse(fileDatabaseHistory.databaseUri),
                        UriUtilDatabase.parse(fileDatabaseHistory.keyFileUri),
                        HardwareKey.getHardwareKeyFromString(fileDatabaseHistory.hardwareKey),
                        UriUtilDatabase.decode(fileDatabaseHistory.databaseUri),
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

    fun deleteDatabaseFile(databaseFileToDelete: com.kunzisoft.keepass.model.DatabaseFile,
                           databaseFileDeletedResult: (com.kunzisoft.keepass.model.DatabaseFile?) -> Unit) {
        IOActionTask(
            {
                databaseFileToDelete.databaseUri?.let { databaseUri ->
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                        ?.let { fileDatabaseHistory ->
                            val returnValue = databaseFileHistoryDao.delete(fileDatabaseHistory)
                            if (returnValue > 0) {
                                DatabaseFile(
                                    UriUtilDatabase.parse(fileDatabaseHistory.databaseUri),
                                    UriUtilDatabase.parse(fileDatabaseHistory.keyFileUri),
                                    HardwareKey.getHardwareKeyFromString(fileDatabaseHistory.hardwareKey),
                                    UriUtilDatabase.decode(fileDatabaseHistory.databaseUri),
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

    companion object : com.kunzisoft.keepass.utils.SingletonHolderParameter<FileDatabaseHistoryAction, Context>(::FileDatabaseHistoryAction) {
        private val TAG = FileDatabaseHistoryAction::class.java.name
    }
}
