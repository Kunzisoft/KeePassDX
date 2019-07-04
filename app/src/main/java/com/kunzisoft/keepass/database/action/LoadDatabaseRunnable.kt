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
package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference

class LoadDatabaseRunnable(private val mWeakContext: WeakReference<Context>,
                           private val mDatabase: Database,
                           private val mUri: Uri,
                           private val mPass: String?,
                           private val mKey: Uri?,
                           private val progressTaskUpdater: ProgressTaskUpdater,
                           nestedAction: ActionRunnable)
    : ActionRunnable(nestedAction, executeNestedActionIfResultFalse = true) {

    private val mRememberKeyFile: Boolean
        get() {
            return mWeakContext.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it)
                        .getBoolean(it.getString(R.string.keyfile_key),
                                it.resources.getBoolean(R.bool.keyfile_default))
            } ?: true
        }

    override fun run() {
        try {
            mWeakContext.get()?.let {
                mDatabase.loadData(it, mUri, mPass, mKey, progressTaskUpdater)
                saveFileData(mUri, mKey)
                finishRun(true)
            } ?: finishRun(false, "Context null")
        } catch (e: ArcFourException) {
            catchError(e, R.string.error_arc4)
            return
        } catch (e: InvalidPasswordException) {
            catchError(e, R.string.invalid_password)
            return
        } catch (e: ContentFileNotFoundException) {
            catchError(e, R.string.file_not_found_content)
            return
        } catch (e: FileNotFoundException) {
            catchError(e, R.string.file_not_found)
            return
        } catch (e: IOException) {
            var messageId = R.string.error_load_database
            e.message?.let {
                if (it.contains("Hash failed with code"))
                    messageId = R.string.error_load_database_KDF_memory
            }
            catchError(e, messageId, true)
            return
        } catch (e: KeyFileEmptyException) {
            catchError(e, R.string.keyfile_is_empty)
            return
        } catch (e: InvalidAlgorithmException) {
            catchError(e, R.string.invalid_algorithm)
            return
        } catch (e: InvalidKeyFileException) {
            catchError(e, R.string.keyfile_does_not_exist)
            return
        } catch (e: InvalidDBSignatureException) {
            catchError(e, R.string.invalid_db_sig)
            return
        } catch (e: InvalidDBVersionException) {
            catchError(e, R.string.unsupported_db_version)
            return
        } catch (e: InvalidDBException) {
            catchError(e, R.string.error_invalid_db)
            return
        } catch (e: OutOfMemoryError) {
            catchError(e, R.string.error_out_of_memory)
            return
        } catch (e: Exception) {
            catchError(e, R.string.error_load_database, true)
            return
        }
    }

    private fun catchError(e: Throwable, @StringRes messageId: Int, addThrowableMessage: Boolean = false) {
        var errorMessage = mWeakContext.get()?.getString(messageId)
        Log.e(TAG, errorMessage, e)
        if (addThrowableMessage)
            errorMessage = errorMessage + " " + e.localizedMessage
        finishRun(false, errorMessage)
    }

    private fun saveFileData(uri: Uri, key: Uri?) {
        var keyFileUri = key
        if (!mRememberKeyFile) {
            keyFileUri = null
        }
        FileDatabaseHistory.getInstance(mWeakContext).addDatabaseUri(uri, keyFileUri)
    }

    override fun onFinishRun(isSuccess: Boolean, message: String?) {}

    companion object {
        private val TAG = LoadDatabaseRunnable::class.java.name
    }
}
