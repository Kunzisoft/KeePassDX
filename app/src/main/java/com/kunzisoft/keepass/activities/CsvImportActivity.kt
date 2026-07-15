/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.EntryMappingIterator
import com.kunzisoft.keepass.viewmodels.CsvImportViewModel

class CsvImportActivity : DatabaseLockActivity() {

    private val viewModel: CsvImportViewModel by viewModels()

    private lateinit var coordinatorLayout: View
    private lateinit var recyclerFieldMapping: RecyclerView
    private lateinit var buttonConfirmImport: Button

    private var parentGroup: Group? = null

    override fun viewToInvalidateTimeout(): View = coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csv_import)

        coordinatorLayout = findViewById(R.id.coordinator_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.import_title)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerFieldMapping = findViewById(R.id.recycler_field_mapping)
        buttonConfirmImport = findViewById(R.id.button_confirm_import)

        val csvUri: Uri? = intent.getParcelableExtraCompat(EXTRA_CSV_URI)
        if (csvUri == null) {
            finish()
            return
        }

        viewModel.setFile(csvUri)

        recyclerFieldMapping.layoutManager = LinearLayoutManager(this)
        viewModel.headers.observe(this) { headers ->
            recyclerFieldMapping.adapter = MappingAdapter(headers)
        }

        buttonConfirmImport.setOnClickListener {
            val database = mDatabase ?: return@setOnClickListener
            val parent = parentGroup ?: return@setOnClickListener
            viewModel.startImport(database, parent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)
        val parentId: NodeId<*>? = intent.getParcelableExtraCompat(EXTRA_PARENT_ID)
        parentId?.let {
            parentGroup = database.getGroupById(it)
        }
    }

    private inner class MappingAdapter(private val headers: List<String>) :
        RecyclerView.Adapter<MappingAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_csv_field_mapping, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(headers[position], position)
        }

        override fun getItemCount() = headers.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val textHeader: TextView = view.findViewById(R.id.text_csv_header)
            private val spinnerFieldType: Spinner = view.findViewById(R.id.spinner_field_type)

            fun bind(header: String, position: Int) {
                textHeader.text = header
                val fieldTypes = EntryMappingIterator.FieldType.values()
                val adapter = ArrayAdapter(
                    itemView.context,
                    android.R.layout.simple_spinner_item,
                    fieldTypes.map { type ->
                        when (type) {
                            EntryMappingIterator.FieldType.IGNORE -> getString(R.string.csv_field_ignore)
                            EntryMappingIterator.FieldType.TITLE -> getString(R.string.csv_field_title)
                            EntryMappingIterator.FieldType.USERNAME -> getString(R.string.csv_field_username)
                            EntryMappingIterator.FieldType.PASSWORD -> getString(R.string.csv_field_password)
                            EntryMappingIterator.FieldType.URL -> getString(R.string.csv_field_url)
                            EntryMappingIterator.FieldType.NOTES -> getString(R.string.csv_field_notes)
                        }
                    }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFieldType.adapter = adapter
                
                val currentMapping = viewModel.mapping.value ?: mutableMapOf()
                spinnerFieldType.setSelection(currentMapping[position]?.ordinal ?: 0)

                spinnerFieldType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        val newMapping = viewModel.mapping.value ?: mutableMapOf()
                        newMapping[position] = fieldTypes[pos]
                        viewModel.mapping.value = newMapping
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PARENT_ID = "EXTRA_PARENT_ID"
        private const val EXTRA_CSV_URI = "EXTRA_CSV_URI"

        fun launch(activity: Activity, parentId: NodeId<*>, csvUri: Uri) {
            val intent = Intent(activity, CsvImportActivity::class.java)
            intent.putExtra(EXTRA_PARENT_ID, parentId)
            intent.putExtra(EXTRA_CSV_URI, csvUri)
            activity.startActivity(intent)
        }
    }
}
