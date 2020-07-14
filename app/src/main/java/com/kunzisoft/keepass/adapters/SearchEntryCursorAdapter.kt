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
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.cursor.EntryCursorKDB
import com.kunzisoft.keepass.database.cursor.EntryCursorKDBX
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.strikeOut

class SearchEntryCursorAdapter(private val context: Context,
                               private val database: Database)
    : androidx.cursoradapter.widget.CursorAdapter(context, null, FLAG_REGISTER_CONTENT_OBSERVER) {

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
        view.tag = viewHolder

        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        getEntryFrom(cursor)?.let { currentEntry ->
            val viewHolder = view.tag as ViewHolder

            // Assign image
            viewHolder.imageViewIcon?.assignDatabaseIcon(
                    database.drawFactory,
                    currentEntry.icon,
                    iconColor)

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
                strikeOut(currentEntry.isCurrentlyExpires)
            }
        }
    }

    private fun getEntryFrom(cursor: Cursor): Entry? {
        return database.createEntry()?.apply {
            database.startManageEntry(this)
            entryKDB?.let { entryKDB ->
                (cursor as EntryCursorKDB).populateEntry(entryKDB, database.iconFactory)
            }
            entryKDBX?.let { entryKDBX ->
                (cursor as EntryCursorKDBX).populateEntry(entryKDBX, database.iconFactory)
            }
            database.stopManageEntry(this)
        }
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        return searchEntries(context, constraint.toString())
    }

    private fun searchEntries(context: Context, query: String): Cursor? {
        var cursorKDB: EntryCursorKDB? = null
        var cursorKDBX: EntryCursorKDBX? = null

        if (database.type == DatabaseKDB.TYPE)
            cursorKDB = EntryCursorKDB()
        if (database.type == DatabaseKDBX.TYPE)
            cursorKDBX = EntryCursorKDBX()

        val searchGroup = database.createVirtualGroupFromSearch(query,
                mOmitBackup,
                SearchHelper.MAX_SEARCH_ENTRY)
        if (searchGroup != null) {
            // Search in hide entries but not meta-stream
            for (entry in searchGroup.getFilteredChildEntries(Group.ChildFilter.getDefaults(context))) {
                entry.entryKDB?.let {
                    cursorKDB?.addEntry(it)
                }
                entry.entryKDBX?.let {
                    cursorKDBX?.addEntry(it)
                }
            }
        }

        return cursorKDB ?: cursorKDBX
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
        internal var imageViewIcon: ImageView? = null
        internal var textViewTitle: TextView? = null
        internal var textViewSubTitle: TextView? = null
    }
}
