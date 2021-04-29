package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.database.element.Database

abstract class DatabaseFragment : StylishFragment() {

    protected var mDatabase: Database? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabase = Database.getInstance()
    }
}