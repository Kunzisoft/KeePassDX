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
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.strikeOut
import java.util.*

class SearchEntryCursorAdapter(private val context: Context,
                               private val database: Database)
    : CursorAdapter(context, null, FLAG_REGISTER_CONTENT_OBSERVER) {

    private val cursorInflater: LayoutInflater? = context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    private var mDisplayUsername: Boolean = false
    private var mOmitBackup: Boolean = true
    private val iconColor: Int

    init {
        // Get the icon color
        val taTextColor = context.theme.obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
        this.iconColor = taTextColor.getColor(0, Color.WHITE)
        taTextColor.recycle()

        reInit(context)
    }

    fun reInit(context: Context) {
        this.mDisplayUsername = PreferencesUtil.showUsernamesListEntries(context)
        this.mOmitBackup = PreferencesUtil.omitBackup(context)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {

        val view = cursorInflater!!.inflate(R.layout.item_search_entry, parent, false)
        val viewHolder = ViewHolder()
        viewHolder.imageViewIcon = view.findViewById(R.id.entry_icon)
        viewHolder.textViewTitle = view.findViewById(R.id.entry_text)
        viewHolder.textViewSubTitle = view.findViewById(R.id.entry_subtext)
        viewHolder.textViewPath = view.findViewById(R.id.entry_path)
        view.tag = viewHolder

        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        getEntryFrom(cursor)?.let { currentEntry ->
            val viewHolder = view.tag as ViewHolder

            // Assign image
            viewHolder.imageViewIcon?.let { iconView ->
                database.iconDrawableFactory.assignDatabaseIcon(iconView, currentEntry.icon, iconColor)
            }

            // Assign title
            viewHolder.textViewTitle?.apply {
                text = currentEntry.getVisualTitle()
                strikeOut(currentEntry.isCurrentlyExpires)
            }

            // Assign subtitle
            viewHolder.textViewSubTitle?.apply {
                val entryUsername = currentEntry.username
                text = if (mDisplayUsername && entryUsername.isNotEmpty()) {
                    String.format("(%s)", entryUsername)
                } else {
                    ""
                }
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                strikeOut(currentEntry.isCurrentlyExpires)
            }

            viewHolder.textViewPath?.apply {
                text = currentEntry.getPathString()
            }
        }
    }

    private fun getEntryFrom(cursor: Cursor): Entry? {
        val entryCursor = cursor as EntryCursor
        return database.getEntryById(entryCursor.getNodeId())
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        return searchEntries(context, constraint.toString())
    }

    private fun searchEntries(context: Context, query: String): Cursor? {
        val cursor = EntryCursor()
        val searchGroup = database.createVirtualGroupFromSearch(query,
                mOmitBackup,
                SearchHelper.MAX_SEARCH_ENTRY)
        if (searchGroup != null) {
            // Search in hide entries but not meta-stream
            for (entry in searchGroup.getFilteredChildEntries(Group.ChildFilter.getDefaults(context))) {
                database.startManageEntry(entry)
                cursor.addEntry(entry)
                database.stopManageEntry(entry)
            }
        }
        return cursor
    }

    fun getEntryFromPosition(position: Int): Entry? {
        var pwEntry: Entry? = null

        val cursor = this.cursor
        if (cursor.moveToFirst() && cursor.move(position)) {
            pwEntry = getEntryFrom(cursor)
        }
        return pwEntry
    }

    private class ViewHolder {
        var imageViewIcon: ImageView? = null
        var textViewTitle: TextView? = null
        var textViewSubTitle: TextView? = null
        var textViewPath: TextView? = null
    }

    private class EntryCursor : MatrixCursor(arrayOf(
        ID,
        COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS,
        COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS
    )) {

        private var entryId: Long = 0

        fun addEntry(entry: Entry) {
            addRow(arrayOf(
                entryId,
                entry.nodeId.id.mostSignificantBits,
                entry.nodeId.id.leastSignificantBits
            ))
            entryId++
        }

        fun getNodeId(): NodeId<UUID> {
            return NodeIdUUID(
                UUID(getLong(getColumnIndex(COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                    getLong(getColumnIndex(COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS)))
            )
        }

        companion object {
            const val ID = BaseColumns._ID
            const val COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS = "UUID_most_significant_bits"
            const val COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS = "UUID_least_significant_bits"
        }
    }
}
