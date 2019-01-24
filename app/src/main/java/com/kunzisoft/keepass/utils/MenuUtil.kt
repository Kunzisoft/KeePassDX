/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.AboutActivity
import com.kunzisoft.keepass.activities.ReadOnlyHelper.READ_ONLY_DEFAULT
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.stylish.StylishActivity


object MenuUtil {

    fun contributionMenuInflater(inflater: MenuInflater, menu: Menu) {
        if (!(BuildConfig.FULL_VERSION && BuildConfig.CLOSED_STORE))
            inflater.inflate(R.menu.contribution, menu)
    }

    fun defaultMenuInflater(inflater: MenuInflater, menu: Menu) {
        contributionMenuInflater(inflater, menu)
        inflater.inflate(R.menu.default_menu, menu)
    }

    fun onContributionItemSelected(activity: StylishActivity): Boolean {
        try {
            Util.gotoUrl(activity, R.string.contribution_url)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    @JvmOverloads
    fun onDefaultMenuOptionsItemSelected(activity: StylishActivity, item: MenuItem, readOnly: Boolean = READ_ONLY_DEFAULT, timeoutEnable: Boolean = false): Boolean {
        when (item.itemId) {
            R.id.menu_contribute -> return onContributionItemSelected(activity)

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
