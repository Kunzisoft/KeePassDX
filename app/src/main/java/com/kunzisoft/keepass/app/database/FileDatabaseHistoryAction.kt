/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.app.database

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.utils.SingletonHolderParameter
import com.kunzisoft.keepass.utils.UriUtil

class FileDatabaseHistoryAction(applicationContext: Context) {

    private val databaseFileHistoryDao =
            AppDatabase
                .getDatabase(applicationContext)
                .fileDatabaseHistoryDao()

    fun getFileDatabaseHistory(databaseUri: Uri,
                               fileHistoryResultListener: (fileDatabaseHistoryResult: FileDatabaseHistoryEntity?) -> Unit) {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                },
                {
                    fileHistoryResultListener.invoke(it)
                }
        ).execute()
    }

    fun getKeyFileUriByDatabaseUri(databaseUri: Uri,
                                   keyFileUriResultListener: (Uri?) -> Unit) {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                },
                {
                    it?.let { fileHistoryEntity ->
                        fileHistoryEntity.keyFileUri?.let { keyFileUri ->
                            keyFileUriResultListener.invoke(UriUtil.parse(keyFileUri))
                        }
                    } ?: keyFileUriResultListener.invoke(null)
                }
        ).execute()
    }

    fun getAllFileDatabaseHistories(fileHistoryResultListener: (fileDatabaseHistoryResult: List<FileDatabaseHistoryEntity>?) -> Unit) {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.getAll()
                },
                {
                    fileHistoryResultListener.invoke(it)
                }
        ).execute()
    }

    fun addOrUpdateDatabaseUri(databaseUri: Uri, keyFileUri: Uri? = null) {
        addOrUpdateFileDatabaseHistory(FileDatabaseHistoryEntity(
                databaseUri.toString(),
                "",
                keyFileUri?.toString(),
                System.currentTimeMillis()
        ), true)
    }

    fun addOrUpdateFileDatabaseHistory(fileDatabaseHistory: FileDatabaseHistoryEntity, unmodifiedAlias: Boolean = false) {
        ActionDatabaseAsyncTask(
                {
                    val fileDatabaseHistoryRetrieve = databaseFileHistoryDao.getByDatabaseUri(fileDatabaseHistory.databaseUri)

                    if (unmodifiedAlias) {
                        fileDatabaseHistory.databaseAlias = fileDatabaseHistoryRetrieve?.databaseAlias ?: ""
                    }
                    // Update values if history element not yet in the database
                    if (fileDatabaseHistoryRetrieve == null) {
                        databaseFileHistoryDao.add(fileDatabaseHistory)
                    } else {
                        databaseFileHistoryDao.update(fileDatabaseHistory)
                    }
                }
        ).execute()
    }

    fun deleteFileDatabaseHistory(fileDatabaseHistory: FileDatabaseHistoryEntity,
                                  fileHistoryDeletedResult: (FileDatabaseHistoryEntity?) -> Unit) {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.delete(fileDatabaseHistory)
                },
                {
                    if (it != null && it > 0)
                        fileHistoryDeletedResult.invoke(fileDatabaseHistory)
                    else
                        fileHistoryDeletedResult.invoke(null)
                }
        ).execute()
    }

    fun deleteAllKeyFiles() {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.deleteAllKeyFiles()
                }
        ).execute()
    }

    fun deleteAll() {
        ActionDatabaseAsyncTask(
                {
                    databaseFileHistoryDao.deleteAll()
                }
        ).execute()
    }

    companion object : SingletonHolderParameter<FileDatabaseHistoryAction, Context>(::FileDatabaseHistoryAction)
}
