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
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.support.v4.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.cursor.EntryCursor
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.PwIcon
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.util.*

class SearchEntryCursorAdapter(context: Context, private val database: Database) : CursorAdapter(context, null, FLAG_REGISTER_CONTENT_OBSERVER) {

    private val cursorInflater: LayoutInflater = context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var displayUsername: Boolean = false
    private val iconColor: Int

    init {
        // Get the icon color
        val taTextColor = context.theme.obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
        this.iconColor = taTextColor.getColor(0, Color.WHITE)
        taTextColor.recycle()

        reInit(context)
    }

    fun reInit(context: Context) {
        this.displayUsername = PreferencesUtil.showUsernamesListEntries(context)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {

        val view = cursorInflater.inflate(R.layout.search_entry, parent, false)
        val viewHolder = ViewHolder()
        viewHolder.imageViewIcon = view.findViewById(R.id.entry_icon)
        viewHolder.textViewTitle = view.findViewById(R.id.entry_text)
        viewHolder.textViewSubTitle = view.findViewById(R.id.entry_subtext)
        view.tag = viewHolder

        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {

        // Retrieve elements from cursor
        val uuid = UUID(cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS)))
        val iconFactory = database.iconFactory
        var icon: PwIcon = iconFactory.getIcon(
                UUID(cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS)),
                        cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS))))
        if (icon.isUnknown) {
            icon = iconFactory.getIcon(cursor.getInt(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_STANDARD)))
            if (icon.isUnknown)
                icon = iconFactory.keyIcon
        }
        val title = cursor.getString(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_TITLE))
        val username = cursor.getString(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_USERNAME))
        val url = cursor.getString(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_URL))

        val viewHolder = view.tag as ViewHolder

        // Assign image
        viewHolder.imageViewIcon?.assignDatabaseIcon(database.drawFactory, icon, iconColor)

        // Assign title
        val showTitle = EntryVersioned.getVisualTitle(false, title, username, url, uuid.toString())
        viewHolder.textViewTitle?.text = showTitle
        if (displayUsername && username.isNotEmpty()) {
            viewHolder.textViewSubTitle?.text = String.format("(%s)", username)
        } else {
            viewHolder.textViewSubTitle?.text = ""
        }
    }

    private class ViewHolder {
        internal var imageViewIcon: ImageView? = null
        internal var textViewTitle: TextView? = null
        internal var textViewSubTitle: TextView? = null
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        return database.searchEntry(constraint.toString())
    }

    fun getEntryFromPosition(position: Int): EntryVersioned? {
        var pwEntry: EntryVersioned? = null

        val cursor = this.cursor
        if (cursor.moveToFirst() && cursor.move(position)) {
            pwEntry = database.getEntryFrom(cursor)
        }
        return pwEntry
    }

}
