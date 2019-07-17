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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwEncryptionAlgorithm
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter
import com.kunzisoft.keepass.tasks.ActionRunnable

class DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat(), ListRadioItemAdapter.RadioItemSelectedCallback<PwEncryptionAlgorithm> {

    private var algorithmSelected: PwEncryptionAlgorithm? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.encryption_explanation)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            val encryptionAlgorithmAdapter = ListRadioItemAdapter<PwEncryptionAlgorithm>(activity)
            encryptionAlgorithmAdapter.setRadioItemSelectedCallback(this)
            recyclerView.adapter = encryptionAlgorithmAdapter

            database?.let { database ->
                algorithmSelected = database.encryptionAlgorithm
                if (algorithmSelected != null)
                    encryptionAlgorithmAdapter.setItems(database.availableEncryptionAlgorithms, algorithmSelected!!)
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (database != null && positiveResult && database!!.allowEncryptionAlgorithmModification()) {

            if (algorithmSelected != null) {
                val newAlgorithm = algorithmSelected
                val oldAlgorithm = database?.encryptionAlgorithm
                newAlgorithm?.let {
                    database?.assignEncryptionAlgorithm(it)
                }

                if (oldAlgorithm != null && newAlgorithm != null)
                    actionInUIThreadAfterSaveDatabase = AfterDescriptionSave(newAlgorithm, oldAlgorithm)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    override fun onItemSelected(item: PwEncryptionAlgorithm) {
        this.algorithmSelected = item
    }

    private inner class AfterDescriptionSave(private val mNewAlgorithm: PwEncryptionAlgorithm,
                                             private val mOldAlgorithm: PwEncryptionAlgorithm)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            activity?.let { activity ->
                var algorithmToShow = mNewAlgorithm
                if (!result.isSuccess) {
                    displayMessage(activity)
                    database?.assignEncryptionAlgorithm(mOldAlgorithm)
                    algorithmToShow = mOldAlgorithm
                }
                preference.summary = algorithmToShow.getName(activity.resources)
            }
        }
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
