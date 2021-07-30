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
import android.view.View
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.activities.DatabaseRetrieval
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseSavePreferenceDialogFragmentCompat
    : InputPreferenceDialogFragmentCompat(), DatabaseRetrieval {

    private var mDatabaseAutoSaveEnable = true
    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    protected var mDatabase: Database? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mDatabaseAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            onDatabaseRetrieved(database)
        }
    }

    override fun onDatabaseRetrieved(database: Database?) {
        this.mDatabase = database
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Optional
    }

    protected fun saveColor(oldColor: String,
                            newColor: String) {
        mDatabaseViewModel.saveColor(oldColor, newColor, mDatabaseAutoSaveEnable)
    }

    protected fun saveCompression(oldCompression: CompressionAlgorithm,
                                  newCompression: CompressionAlgorithm) {
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
