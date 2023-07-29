/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AboutActivity
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.utils.UriUtil.isContributingUser
import com.kunzisoft.keepass.utils.UriUtil.openUrl

object MenuUtil {

    fun defaultMenuInflater(context: Context, inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.settings, menu)
        inflater.inflate(R.menu.about, menu)
        if (!context.isContributingUser())
            menu.findItem(R.id.menu_contribute)?.isVisible = false
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    fun onDefaultMenuOptionsItemSelected(activity: Activity,
                                         item: MenuItem,
                                         timeoutEnable: Boolean = false) {
        when (item.itemId) {
            R.id.menu_contribute -> {
                activity.openUrl(R.string.contribution_url)
            }
            R.id.menu_app_settings -> {
                // To avoid flickering when launch settings in a LockingActivity
                SettingsActivity.launch(activity, timeoutEnable)
            }
            R.id.menu_about -> {
                val intent = Intent(activity, AboutActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}
