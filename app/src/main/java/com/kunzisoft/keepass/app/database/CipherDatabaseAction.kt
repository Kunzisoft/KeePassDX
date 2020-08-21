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
import com.kunzisoft.keepass.utils.SingletonHolderParameter

class CipherDatabaseAction(applicationContext: Context) {

    private val cipherDatabaseDao =
            AppDatabase
                    .getDatabase(applicationContext)
                    .cipherDatabaseDao()

    fun getCipherDatabase(databaseUri: Uri,
                          cipherDatabaseResultListener: (CipherDatabaseEntity?) -> Unit) {
        IOActionTask(
                {
                    cipherDatabaseDao.getByDatabaseUri(databaseUri.toString())
                },
                {
                    cipherDatabaseResultListener.invoke(it)
                }
        ).execute()
    }

    fun containsCipherDatabase(databaseUri: Uri,
                               contains: (Boolean) -> Unit) {
        getCipherDatabase(databaseUri) {
            contains.invoke(it != null)
        }
    }

    fun addOrUpdateCipherDatabase(cipherDatabaseEntity: CipherDatabaseEntity,
                                  cipherDatabaseResultListener: (() -> Unit)? = null) {
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

    fun deleteByDatabaseUri(databaseUri: Uri,
                            cipherDatabaseResultListener: (() -> Unit)? = null) {
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
        IOActionTask(
                {
                    cipherDatabaseDao.deleteAll()
                }
        ).execute()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction)
}