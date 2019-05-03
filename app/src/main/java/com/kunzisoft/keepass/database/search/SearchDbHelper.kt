/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search

import android.content.Context
import android.preference.PreferenceManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.NodeHandler
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator
import java.util.*

class SearchDbHelper(private val mContext: Context) {
    private var incrementEntry = 0

    private fun omitBackup(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        return prefs.getBoolean(mContext.getString(R.string.omitbackup_key), mContext.resources.getBoolean(R.bool.omitbackup_default))
    }

    fun search(pm: Database, qStr: String, max: Int): GroupVersioned {

        val searchGroup = pm.createGroup()
        searchGroup!!.title = "\"" + qStr + "\""

        // Search all entries
        val loc = Locale.getDefault()
        val finalQStr = qStr.toLowerCase(loc)
        val isOmitBackup = omitBackup()


        incrementEntry = 0
        pm.rootGroup.doForEachChild(
                object : NodeHandler<EntryVersioned>() {
                    override fun operate(entry: EntryVersioned): Boolean {
                        if (entryContainsString(entry, finalQStr, loc)) {
                            searchGroup.addChildEntry(entry)
                            incrementEntry++
                        }
                        // Stop searching when we have max entries
                        return incrementEntry <= max
                    }
                },
                object : NodeHandler<GroupVersioned>() {
                    override fun operate(group: GroupVersioned): Boolean {
                        return if (pm.isGroupSearchable(group, isOmitBackup)!!) {
                            true
                        } else incrementEntry <= max
                    }
                })

        return searchGroup
    }

    private fun entryContainsString(entry: EntryVersioned, qStr: String, loc: Locale): Boolean {
        // Search all strings in the entry
        val iterator = EntrySearchStringIterator.getInstance(entry)
        while (iterator.hasNext()) {
            val str = iterator.next()
            if (str.isNotEmpty()) {
                val lower = str.toLowerCase(loc)
                if (lower.contains(qStr)) {
                    return true
                }
            }
        }
        return false
    }
}
