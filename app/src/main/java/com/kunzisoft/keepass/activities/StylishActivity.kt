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
package com.kunzisoft.keepass.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.NestedAppSettingsFragment.Companion.DATABASE_PREFERENCE_CHANGED
import com.kunzisoft.keepass.settings.PreferencesUtil

/**
 * Stylish Hide Activity that apply a dynamic style and sets FLAG_SECURE to prevent screenshots / from
 * appearing in the recent app preview
 */
abstract class StylishActivity : AppCompatActivity() {

    /* (non-Javadoc) Workaround for HTC Linkify issues
     * @see android.app.Activity#startActivity(android.content.Intent)
     */

    private var isDynamicThemeEnabled = true

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

        isDynamicThemeEnabled = PreferencesUtil.getIsDynamicThemingEnabled(this)

        //Material you theme
        if (DynamicColors.isDynamicColorAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDynamicThemeEnabled) {
                DynamicColors.applyToActivityIfAvailable(this)
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(onScreenshotModePrefListener)
    }

    private val onScreenshotModePrefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key != getString(R.string.enable_screenshot_mode_key)) return@OnSharedPreferenceChangeListener

        setScreenshotMode(PreferencesUtil.isScreenshotModeEnabled(this))
    }

    private fun setScreenshotMode(isEnabled: Boolean) {
        findViewById<View>(R.id.screenshot_mode_banner)?.visibility =
            if (isEnabled) VISIBLE else GONE

        // Several gingerbread devices have problems with FLAG_SECURE
        if (isEnabled) {
            window.clearFlags(FLAG_SECURE)
        } else {
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (DATABASE_PREFERENCE_CHANGED
            || isDynamicThemeEnabled != PreferencesUtil.getIsDynamicThemingEnabled(this)
        ) {
            DATABASE_PREFERENCE_CHANGED = false
            isDynamicThemeEnabled = PreferencesUtil.getIsDynamicThemingEnabled(this)
            Log.d(this.javaClass.name, "Theme change detected, restarting activity")
            recreateActivity()
        }
        setScreenshotMode(PreferencesUtil.isScreenshotModeEnabled(this))
    }

    private fun recreateActivity() {
        // To prevent KitKat bugs
        Handler(Looper.getMainLooper()).post { recreate() }
    }
}
