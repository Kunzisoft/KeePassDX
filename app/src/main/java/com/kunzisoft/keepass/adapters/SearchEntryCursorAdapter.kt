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
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.strikeOut

class SearchEntryCursorAdapter(private val context: Context,
                               private val database: Database)
    : androidx.cursoradapter.widget.CursorAdapter(context, null, FLAG_REGISTER_CONTENT_OBSERVER) {

    private val cursorInflater: LayoutInflater? = context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
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

        val view = cursorInflater!!.inflate(R.layout.item_search_entry, parent, false)
        val viewHolder = ViewHolder()
        viewHolder.imageViewIcon = view.findViewById(R.id.entry_icon)
        viewHolder.textViewTitle = view.findViewById(R.id.entry_text)
        viewHolder.textViewSubTitle = view.findViewById(R.id.entry_subtext)
        view.tag = viewHolder

        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {

        database.getEntryFrom(cursor)?.let { currentEntry ->
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
                text = if (displayUsername && entryUsername.isNotEmpty()) {
                    String.format("(%s)", entryUsername)
                } else {
                    ""
                }
                strikeOut(currentEntry.isCurrentlyExpires)
            }
        }
    }

    private class ViewHolder {
        internal var imageViewIcon: ImageView? = null
        internal var textViewTitle: TextView? = null
        internal var textViewSubTitle: TextView? = null
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        return database.searchEntries(context, constraint.toString())
    }

    fun getEntryFromPosition(position: Int): Entry? {
        var pwEntry: Entry? = null

        val cursor = this.cursor
        if (cursor.moveToFirst() && cursor.move(position)) {
            pwEntry = database.getEntryFrom(cursor)
        }
        return pwEntry
    }

}
