package com.kunzisoft.keepass.settings

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.database.element.Database

abstract class DatabasePreferenceFragment : PreferenceFragmentCompat() {

    protected var mDatabase: Database? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mDatabase = Database.getInstance()
    }
}