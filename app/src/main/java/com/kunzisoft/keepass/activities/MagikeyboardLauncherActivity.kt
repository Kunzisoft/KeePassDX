/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities

import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper

/**
 * Activity to select entry in database and populate it in Magikeyboard
 */
class MagikeyboardLauncherActivity : DatabaseModeActivity() {

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)
        SearchHelper.checkAutoSearchInfo(this,
            database,
            null,
            { _, _ ->
                // Not called
                // if items found directly returns before calling this activity
            },
            { openedDatabase ->
                // Select if not found
                GroupActivity.launchForKeyboardSelectionResult(this, openedDatabase)
            },
            {
                // Pass extra to get entry
                FileDatabaseSelectActivity.launchForKeyboardSelectionResult(this)
            }
        )
        finish()
    }
}
