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
package com.kunzisoft.keepass.activities

import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.widget.TextView

import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.stylish.StylishActivity

import org.joda.time.DateTime

class AboutActivity : StylishActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.about)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.menu_about)
        setSupportActionBar(toolbar)
        assert(supportActionBar != null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        var version: String
        var build: String
        try {
            version = packageManager.getPackageInfo(packageName, 0).versionName
            build = BuildConfig.BUILD_VERSION
        } catch (e: NameNotFoundException) {
            Log.w(javaClass.simpleName, "Unable to get the app or the build version", e)
            version = "Unable to get the app version"
            build = "Unable to get the build version"
        }

        version = getString(R.string.version_label, version)
        val versionTextView = findViewById<TextView>(R.id.activity_about_version)
        versionTextView.text = version

        build = getString(R.string.build_label, build)
        val buildTextView = findViewById<TextView>(R.id.activity_about_build)
        buildTextView.text = build


        val disclaimerText = findViewById<TextView>(R.id.disclaimer)
        disclaimerText.text = getString(R.string.disclaimer_formal, DateTime().year)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
