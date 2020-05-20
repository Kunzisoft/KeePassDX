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
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AboutActivity
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper.READ_ONLY_DEFAULT
import com.kunzisoft.keepass.settings.SettingsActivity

object MenuUtil {

    fun contributionMenuInflater(inflater: MenuInflater, menu: Menu) {
        if (!(BuildConfig.FULL_VERSION && BuildConfig.CLOSED_STORE))
            inflater.inflate(R.menu.contribution, menu)
    }

    fun defaultMenuInflater(inflater: MenuInflater, menu: Menu) {
        contributionMenuInflater(inflater, menu)
        inflater.inflate(R.menu.default_menu, menu)
    }

    fun onContributionItemSelected(context: Context) {
        UriUtil.gotoUrl(context, R.string.contribution_url)
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    fun onDefaultMenuOptionsItemSelected(activity: Activity,
                                         item: MenuItem,
                                         readOnly: Boolean = READ_ONLY_DEFAULT,
                                         timeoutEnable: Boolean = false): Boolean {
        when (item.itemId) {
            R.id.menu_contribute -> {
                onContributionItemSelected(activity)
                return true
            }
            R.id.menu_app_settings -> {
                // To avoid flickering when launch settings in a LockingActivity
                SettingsActivity.launch(activity, readOnly, timeoutEnable)
                return true
            }
            R.id.menu_about -> {
                val intent = Intent(activity, AboutActivity::class.java)
                activity.startActivity(intent)
                return true
            }
            else -> return true
        }
    }
}
