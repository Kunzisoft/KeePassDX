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
package com.kunzisoft.keepass.fileselect.database

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.preference.PreferenceManager

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.SingletonHolder
import com.kunzisoft.keepass.utils.UriUtil
import java.lang.ref.WeakReference

import java.util.ArrayList

class FileDatabaseHistory private constructor(private val context: WeakReference<Context>) {

    private val mDatabasesUriList = ArrayList<String>()
    private val mKeyFilesUriList = ArrayList<String>()

    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.get())

    var isEnabled: Boolean = false

    val databaseUriList: List<String>
        get() {
            init()
            return mDatabasesUriList
        }

    private val onSharedPreferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == context.get()?.getString(R.string.recentfile_key)) {
            isEnabled = sharedPreferences.getBoolean(
                    context.get()?.getString(R.string.recentfile_key),
                    context.get()?.resources?.getBoolean(R.bool.recentfile_default) ?: isEnabled)
        }
    }

    init {
        mPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        context.get()?.resources?.let {
            isEnabled = mPreferences.getBoolean(
                    it.getString(R.string.recentfile_key),
                    it.getBoolean(R.bool.recentfile_default))
        }
        mPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    private var init = false
    @Synchronized
    private fun init() {
        if (!init) {
            if (!upgradeFromSQL()) {
                loadPrefs()
            }

            init = true
        }
    }

    private fun upgradeFromSQL(): Boolean {

        try {
            // Check for a database to upgrade from
            if (context.get()?.getDatabasePath(FileDatabaseHelper.DATABASE_NAME)?.exists() != true) {
                return false
            }

            mDatabasesUriList.clear()
            mKeyFilesUriList.clear()

            val helper = FileDatabaseHelper(context.get())
            helper.open()
            val cursor = helper.fetchAllFiles()

            val dbIndex = cursor.getColumnIndex(FileDatabaseHelper.KEY_FILE_FILENAME)
            val keyIndex = cursor.getColumnIndex(FileDatabaseHelper.KEY_FILE_KEYFILE)

            if (cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    mDatabasesUriList.add(cursor.getString(dbIndex))
                    mKeyFilesUriList.add(cursor.getString(keyIndex))
                }
            }

            savePrefs()

            cursor.close()
            helper.close()

        } catch (e: Exception) {
            // If upgrading fails, we'll just give up on it.
        }

        try {
            FileDatabaseHelper.deleteDatabase(context.get())
        } catch (e: Exception) {
            // If we fail to delete it, just move on
        }

        return true
    }

    @JvmOverloads
    fun addDatabaseUri(databaseUri: Uri?, keyFileUri: Uri? = null) {
        if (!isEnabled || databaseUri == null) return

        init()

        // Remove any existing instance of the same filename
        deleteDatabaseUri(databaseUri, false)

        mDatabasesUriList.add(0, databaseUri.toString())

        val key = keyFileUri?.toString() ?: ""
        mKeyFilesUriList.add(0, key)

        trimLists()
        savePrefs()
    }

    fun hasRecentFiles(): Boolean {
        if (!isEnabled) return false

        init()

        return mDatabasesUriList.size > 0
    }

    fun getDatabaseAt(i: Int): String {
        init()
        return mDatabasesUriList[i]
    }

    fun getKeyFileAt(i: Int): String {
        init()
        return mKeyFilesUriList[i]
    }

    private fun loadPrefs() {
        loadList(DB_KEY, mDatabasesUriList)
        loadList(KEY_FILE_KEY, mKeyFilesUriList)
    }

    private fun savePrefs() {
        saveList(DB_KEY, mDatabasesUriList)
        saveList(KEY_FILE_KEY, mKeyFilesUriList)
    }

    private fun loadList(keyPrefix: String, list: MutableList<String>) {
        val size = mPreferences.getInt(keyPrefix, 0)

        list.clear()
        for (i in 0 until size) {
            mPreferences.getString(keyPrefix + "_" + i, "")?.let {
                list.add(it)
            }
        }
    }

    private fun saveList(keyPrefix: String, list: List<String>) {
        val edit = mPreferences.edit()
        val size = list.size
        edit.putInt(keyPrefix, size)

        for (i in 0 until size) {
            edit.putString(keyPrefix + "_" + i, list[i])
        }
        edit.apply()
    }

    @JvmOverloads
    fun deleteDatabaseUri(uri: Uri, save: Boolean = true) {
        init()

        val uriName = uri.toString()
        val fileName = uri.path

        for (i in mDatabasesUriList.indices) {
            val entry = mDatabasesUriList[i]
            if (uriName == entry || fileName == entry) {
                mDatabasesUriList.removeAt(i)
                mKeyFilesUriList.removeAt(i)
                break
            }
        }

        if (save) {
            savePrefs()
        }
    }

    fun getKeyFileUriByDatabaseUri(uri: Uri): Uri? {
        if (!isEnabled)
            return null
        init()
        val size = mDatabasesUriList.size
        for (i in 0 until size) {
            if (uri == UriUtil.parseUriFile(mDatabasesUriList[i])) {
                return UriUtil.parseUriFile(mKeyFilesUriList[i])
            }
        }
        return null
    }

    fun deleteAll() {
        init()

        mDatabasesUriList.clear()
        mKeyFilesUriList.clear()

        savePrefs()
    }

    fun deleteAllKeys() {
        init()

        mKeyFilesUriList.clear()

        val size = mDatabasesUriList.size
        for (i in 0 until size) {
            mKeyFilesUriList.add("")
        }

        savePrefs()
    }

    private fun trimLists() {
        val size = mDatabasesUriList.size
        for (i in FileDatabaseHelper.MAX_FILES until size) {
            mDatabasesUriList.removeAt(i)
            mKeyFilesUriList.removeAt(i)
        }
    }

    companion object : SingletonHolder<FileDatabaseHistory, WeakReference<Context>>(::FileDatabaseHistory) {

        private const val DB_KEY = "recent_databases"
        private const val KEY_FILE_KEY = "recent_keyfiles"
    }
}
