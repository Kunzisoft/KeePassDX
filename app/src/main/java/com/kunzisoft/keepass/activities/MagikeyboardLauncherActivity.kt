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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper

/**
 * Activity to select entry in database and populate it in Magikeyboard
 */
class MagikeyboardLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SearchHelper.checkAutoSearchInfo(this,
                Database.getInstance(),
                null,
                {},
                {
                    GroupActivity.launchForEntrySelectionResult(this)
                },
                {
                    // Pass extra to get entry
                    FileDatabaseSelectActivity.launchForEntrySelectionResult(this)
                }
        )
        finish()
        super.onCreate(savedInstanceState)
    }
}
