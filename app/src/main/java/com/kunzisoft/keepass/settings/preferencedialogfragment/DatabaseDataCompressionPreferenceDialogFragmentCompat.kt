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

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.settings.preferencedialogfragment.adapter.ListRadioItemAdapter

class DatabaseDataCompressionPreferenceDialogFragmentCompat
    : DatabaseSavePreferenceDialogFragmentCompat(),
        ListRadioItemAdapter.RadioItemSelectedCallback<CompressionAlgorithm> {

    private var mRecyclerView: RecyclerView? = null
    private var mCompressionAdapter: ListRadioItemAdapter<CompressionAlgorithm>? = null
    private var compressionSelected: CompressionAlgorithm? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.database_data_compression_summary)

        mRecyclerView = view.findViewById(R.id.pref_dialog_list)
        mRecyclerView?.layoutManager = LinearLayoutManager(context)

        activity?.let { activity ->
            mCompressionAdapter = ListRadioItemAdapter<CompressionAlgorithm>(activity)
            mCompressionAdapter?.setRadioItemSelectedCallback(this)
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        setExplanationText(R.string.database_data_compression_summary)

        mRecyclerView?.adapter = mCompressionAdapter

        database?.let {
            compressionSelected = it.compressionAlgorithm
            mCompressionAdapter?.setItems(it.availableCompressionAlgorithms, compressionSelected)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                if (compressionSelected != null) {
                    val newCompression = compressionSelected
                    val oldCompression = database.compressionAlgorithm
                    database.compressionAlgorithm = newCompression

                    if (oldCompression != null && newCompression != null)
                        saveCompression(oldCompression, newCompression)
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
