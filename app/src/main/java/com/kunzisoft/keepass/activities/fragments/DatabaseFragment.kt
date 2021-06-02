package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.database.element.Database

abstract class DatabaseFragment : StylishFragment() {

    protected var mDatabase: Database? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mDatabase = Database.getInstance()
    }
}