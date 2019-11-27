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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.EncryptionAlgorithm
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
        ListRadioItemAdapter.RadioItemSelectedCallback<EncryptionAlgorithm> {

    private var algorithmSelected: EncryptionAlgorithm? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.encryption_explanation)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            val encryptionAlgorithmAdapter = ListRadioItemAdapter<EncryptionAlgorithm>(activity)
            encryptionAlgorithmAdapter.setRadioItemSelectedCallback(this)
            recyclerView.adapter = encryptionAlgorithmAdapter

            database?.let { database ->
                algorithmSelected = database.encryptionAlgorithm?.apply {
                    encryptionAlgorithmAdapter.setItems(database.availableEncryptionAlgorithms, this)
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {

        if (positiveResult) {
            database?.let { database ->
                if (database.allowEncryptionAlgorithmModification) {
                    if (algorithmSelected != null) {
                        val newAlgorithm = algorithmSelected
                        val oldAlgorithm = database.encryptionAlgorithm
                        database.encryptionAlgorithm = newAlgorithm

                        if (oldAlgorithm != null && newAlgorithm != null)
                            mProgressDialogThread?.startDatabaseSaveEncryption(oldAlgorithm, newAlgorithm, mDatabaseAutoSaveEnable)
                    }
                }
            }
        }
    }

    override fun onItemSelected(item: EncryptionAlgorithm) {
        this.algorithmSelected = item
    }

    companion object {

        fun newInstance(key: String): DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat {
            val fragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
