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
        ActionDatabaseAsyncTask(
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
        ActionDatabaseAsyncTask(
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
        ActionDatabaseAsyncTask(
                {
                    cipherDatabaseDao.deleteByDatabaseUri(databaseUri.toString())
                },
                {
                    cipherDatabaseResultListener?.invoke()
                }
        ).execute()
    }

    fun deleteAll() {
        ActionDatabaseAsyncTask(
                {
                    cipherDatabaseDao.deleteAll()
                }
        ).execute()
    }

    companion object : SingletonHolderParameter<CipherDatabaseAction, Context>(::CipherDatabaseAction)
}