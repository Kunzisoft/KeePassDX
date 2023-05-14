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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.kunzisoft.androidclearchroma.ChromaUtil
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseSavePreferenceDialogFragmentCompat
    : InputPreferenceDialogFragmentCompat(), DatabaseRetrieval {

    private var mDatabaseAutoSaveEnable = true
    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabase: ContextualDatabase? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mDatabaseAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDatabaseViewModel.database.observe(this) { database ->
            onDatabaseRetrieved(database)
        }
    }

    override fun onResume() {
        super.onResume()
        onDatabaseRetrieved(mDatabase)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        this.mDatabase = database
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Not used
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        onDialogClosed(mDatabase, positiveResult)
    }

    open fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        // To inherit to save element in database
    }

    protected fun saveColor(oldColor: Int?,
                            newColor: Int?) {
        val oldColorString = if (oldColor != null)
                ChromaUtil.getFormattedColorString(oldColor, false)
            else
                ""
        val newColorString = if (newColor != null)
            ChromaUtil.getFormattedColorString(newColor, false)
        else
            ""
        mDatabaseViewModel.saveColor(oldColorString, newColorString, mDatabaseAutoSaveEnable)
    }

    protected fun saveCompression(oldCompression: CompressionAlgorithm,
                                  newCompression: CompressionAlgorithm
    ) {
        mDatabaseViewModel.saveCompression(oldCompression, newCompression, mDatabaseAutoSaveEnable)
    }

    protected fun saveDefaultUsername(oldUsername: String,
                                      newUsername: String) {
        mDatabaseViewModel.saveDefaultUsername(oldUsername, newUsername, mDatabaseAutoSaveEnable)
    }

    protected fun saveDescription(oldDescription: String,
                                  newDescription: String) {
        mDatabaseViewModel.saveDescription(oldDescription, newDescription, mDatabaseAutoSaveEnable)
    }

    protected fun saveEncryption(oldEncryption: EncryptionAlgorithm,
                                 newEncryptionAlgorithm: EncryptionAlgorithm) {
        mDatabaseViewModel.saveEncryption(oldEncryption, newEncryptionAlgorithm, mDatabaseAutoSaveEnable)
    }

    protected fun saveKeyDerivation(oldKeyDerivation: KdfEngine,
                                    newKeyDerivation: KdfEngine) {
        mDatabaseViewModel.saveKeyDerivation(oldKeyDerivation, newKeyDerivation, mDatabaseAutoSaveEnable)
    }

    protected fun saveName(oldName: String,
                           newName: String) {
        mDatabaseViewModel.saveName(oldName, newName, mDatabaseAutoSaveEnable)
    }

    protected fun saveRecycleBin(oldGroup: Group?,
                                 newGroup: Group?) {
        mDatabaseViewModel.saveRecycleBin(oldGroup, newGroup, mDatabaseAutoSaveEnable)
    }

    protected fun removeUnlinkedData() {
        mDatabaseViewModel.removeUnlinkedData(mDatabaseAutoSaveEnable)
    }

    protected fun saveTemplatesGroup(oldGroup: Group?,
                                     newGroup: Group?) {
        mDatabaseViewModel.saveTemplatesGroup(oldGroup, newGroup, mDatabaseAutoSaveEnable)
    }

    protected fun saveMaxHistoryItems(oldNumber: Int,
                                      newNumber: Int) {
        mDatabaseViewModel.saveMaxHistoryItems(oldNumber, newNumber, mDatabaseAutoSaveEnable)
    }

    protected fun saveMaxHistorySize(oldNumber: Long,
                                     newNumber: Long) {
        mDatabaseViewModel.saveMaxHistorySize(oldNumber, newNumber, mDatabaseAutoSaveEnable)
    }

    protected fun saveMemoryUsage(oldNumber: Long,
                                  newNumber: Long) {
        mDatabaseViewModel.saveMemoryUsage(oldNumber, newNumber, mDatabaseAutoSaveEnable)
    }

    protected fun saveParallelism(oldNumber: Long,
                                  newNumber: Long) {
        mDatabaseViewModel.saveParallelism(oldNumber, newNumber, mDatabaseAutoSaveEnable)
    }

    protected fun saveIterations(oldNumber: Long,
                                 newNumber: Long) {
        mDatabaseViewModel.saveIterations(oldNumber, newNumber, mDatabaseAutoSaveEnable)
    }

    companion object {
        private const val TAG = "DbSavePrefDialog"
    }
}
