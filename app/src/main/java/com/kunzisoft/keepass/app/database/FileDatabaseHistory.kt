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

import android.arch.persistence.db.SimpleSQLiteQuery
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.kunzisoft.keepass.utils.SingletonHolderParameter

class FileDatabaseHistory(applicationContext: Context) {

    private val databaseFileHistoryDao =
            AppDatabase
                .getDatabase(applicationContext)
                .databaseFileHistoryDao()

    fun getAll(fileHistoryResultListener: (fileDatabaseHistoryResult: List<FileDatabaseHistoryEntity>?) -> Unit) {
        ActionFileHistoryAsyncTask(
                {
                    databaseFileHistoryDao.getAll()
                },
                {
                    fileHistoryResultListener.invoke(it)
                }
        ).execute()
    }

    fun addDatabaseUri(databaseUri: Uri, keyFileUri: Uri? = null) {
        ActionFileHistoryAsyncTask(
                {
                    val newDatabaseFileHistory = FileDatabaseHistoryEntity(
                            databaseUri.toString(),
                            "",
                            keyFileUri?.toString(),
                            System.currentTimeMillis()
                    )
                    // Update values if history element not yet in the database
                    if (databaseFileHistoryDao.getByDatabaseUri(newDatabaseFileHistory.databaseUri) == null) {
                        databaseFileHistoryDao.add(newDatabaseFileHistory)
                    } else {
                        databaseFileHistoryDao.update(newDatabaseFileHistory)
                    }
                }
        ).execute()
    }

    fun getKeyFileUriByDatabaseUri(databaseUri: Uri,
                                   keyFileUriResultListener: (Uri?) -> Unit) {
        ActionFileHistoryAsyncTask(
                {
                    databaseFileHistoryDao.getByDatabaseUri(databaseUri.toString())
                },
                {
                    it?.let { fileHistoryEntity ->
                        fileHistoryEntity.keyFileUri?.let { keyFileUri ->
                            keyFileUriResultListener.invoke(Uri.parse(keyFileUri))
                        }
                    } ?: keyFileUriResultListener.invoke(null)
                }
        ).execute()
    }

    fun deleteDatabaseUri(databaseUri: Uri,
                          fileHistoryDeletedResult: (FileDatabaseHistoryEntity?) -> Unit) {

        val databaseFileHistoryDeleted = FileDatabaseHistoryEntity(
                                            databaseUri.toString(),
                                            "",
                                            null,
                                            0)

        ActionFileHistoryAsyncTask(
                {
                    databaseFileHistoryDao.delete(databaseFileHistoryDeleted)
                },
                {
                    if (it != null && it > 0)
                        fileHistoryDeletedResult.invoke(databaseFileHistoryDeleted)
                    else
                        fileHistoryDeletedResult.invoke(null)
                }
        ).execute()
    }

    fun deleteAllKeyFiles() {
        // TODO replace for unsupported query databaseFileHistoryDao.deleteAllKeyFiles()
        ActionFileHistoryAsyncTask(
                {
                    databaseFileHistoryDao
                            .deleteAllKeyFiles(SimpleSQLiteQuery("REPLACE INTO file_database_history(keyfile_uri) VALUES(null)"))
                }
        ).execute()
    }

    fun deleteAll() {
        ActionFileHistoryAsyncTask(
                {
                    databaseFileHistoryDao.deleteAll()
                }
        ).execute()
    }

    /**
     * Private class to invoke each method in a separate thread
     */
    private class ActionFileHistoryAsyncTask<T>(
            private val action: () -> T ,
            private val afterActionFileHistoryListener: ((fileHistoryResult: T?) -> Unit)? = null
    ) : AsyncTask<Void, Void, T>() {

        override fun doInBackground(vararg args: Void?): T? {
            return action.invoke()
        }

        override fun onPostExecute(result: T?) {
            afterActionFileHistoryListener?.invoke(result)
        }
    }

    companion object : SingletonHolderParameter<FileDatabaseHistory, Context>(::FileDatabaseHistory)
}
