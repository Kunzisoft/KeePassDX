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
package com.kunzisoft.keepass.activities;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.stylish.StylishActivity;

import org.joda.time.DateTime;

public class AboutActivity extends StylishActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.menu_about));
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        String version;
        String build;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            build = BuildConfig.BUILD_VERSION;
        } catch (NameNotFoundException e) {
            Log.w(getClass().getSimpleName(), "Unable to get the app or the build version", e);
            version = "Unable to get the app version";
            build = "Unable to get the build version";
        }
        version = getString(R.string.version_label, version);
        TextView versionTextView = findViewById(R.id.activity_about_version);
        versionTextView.setText(version);

        build = getString(R.string.build_label, build);
        TextView buildTextView = findViewById(R.id.activity_about_build);
        buildTextView.setText(build);


        TextView disclaimerText = findViewById(R.id.disclaimer);
        disclaimerText.setText(getString(R.string.disclaimer_formal, new DateTime().getYear()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
