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
package com.kunzisoft.keepass.activities.stylish

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.settings.NestedAppSettingsFragment.Companion.DATABASE_APPEARANCE_PREFERENCE_CHANGED

/**
 * Stylish Hide Activity that apply a dynamic style and sets FLAG_SECURE to prevent screenshots / from
 * appearing in the recent app preview
 */
abstract class StylishActivity : AppCompatActivity() {

    @StyleRes
    private var themeId: Int = 0
    private var customStyle = true

    /* (non-Javadoc) Workaround for HTC Linkify issues
     * @see android.app.Activity#startActivity(android.content.Intent)
     */
    override fun startActivity(intent: Intent) {
        try {
            intent.component?.let {
                if (it.shortClassName == ".HtcLinkifyDispatcherActivity")
                    intent.component = null
            }
            super.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            /* Catch the bad HTC implementation case */
            super.startActivity(Intent.createChooser(intent, null))
        }
    }

    open fun applyCustomStyle(): Boolean {
        return true
    }

    open fun finishActivityIfReloadRequested(): Boolean {
        return false
    }

    open fun reloadActivity() {
        if (!finishActivityIfReloadRequested()) {
            startActivity(intent)
        }
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customStyle = applyCustomStyle()
        if (customStyle) {
            this.themeId = Stylish.getThemeId(this)
            setTheme(themeId)
        }

        // Several gingerbread devices have problems with FLAG_SECURE
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()

        if ((customStyle && Stylish.getThemeId(this) != this.themeId)
            || DATABASE_APPEARANCE_PREFERENCE_CHANGED) {
            DATABASE_APPEARANCE_PREFERENCE_CHANGED = false
            Log.d(this.javaClass.name, "Theme change detected, restarting activity")
            recreateActivity()
        }
    }

    private fun recreateActivity() {
        // To prevent KitKat bugs
        Handler(Looper.getMainLooper()).post { recreate() }
    }
}
