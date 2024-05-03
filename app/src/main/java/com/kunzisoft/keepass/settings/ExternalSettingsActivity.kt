package com.kunzisoft.keepass.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity

abstract class ExternalSettingsActivity : DatabaseModeActivity() {

    private var lockView: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_toolbar)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(retrieveTitle())
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lockView = findViewById(R.id.lock_button)
        lockView?.hide()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, retrievePreferenceFragment())
                .commit()
        }
    }

    @StringRes
    abstract fun retrieveTitle(): Int

    abstract fun retrievePreferenceFragment(): PreferenceFragmentCompat

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onDatabaseBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}