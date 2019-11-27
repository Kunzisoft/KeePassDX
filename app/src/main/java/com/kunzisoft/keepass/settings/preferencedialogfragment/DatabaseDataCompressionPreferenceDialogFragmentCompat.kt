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
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseDataCompressionPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
        ListRadioItemAdapter.RadioItemSelectedCallback<CompressionAlgorithm> {

    private var compressionSelected: CompressionAlgorithm? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.database_data_compression_summary)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pref_dialog_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            val compressionAdapter = ListRadioItemAdapter<CompressionAlgorithm>(activity)
            compressionAdapter.setRadioItemSelectedCallback(this)
            recyclerView.adapter = compressionAdapter

            database?.let { database ->
                compressionSelected = database.compressionAlgorithm?.apply {
                    compressionAdapter.setItems(database.availableCompressionAlgorithms, this)
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {

        if (positiveResult) {
            database?.let { database ->
                if (compressionSelected != null) {
                    val newCompression = compressionSelected
                    val oldCompression = database.compressionAlgorithm
                    database.compressionAlgorithm = newCompression

                    if (oldCompression != null && newCompression != null)
                        mProgressDialogThread?.startDatabaseSaveCompression(oldCompression, newCompression, mDatabaseAutoSaveEnable)
                }
            }
        }
    }

    override fun onItemSelected(item: CompressionAlgorithm) {
        this.compressionSelected = item
    }

    companion object {

        fun newInstance(key: String): DatabaseDataCompressionPreferenceDialogFragmentCompat {
            val fragment = DatabaseDataCompressionPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
