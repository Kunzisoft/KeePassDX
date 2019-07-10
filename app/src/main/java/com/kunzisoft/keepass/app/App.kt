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
package com.kunzisoft.keepass.app

import android.support.multidex.MultiDexApplication
import com.kunzisoft.keepass.compat.PRNGFixes
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.activities.stylish.Stylish

class App : MultiDexApplication() {

    companion object {
        var currentDatabase: Database = Database()
    }

    override fun onCreate() {
        super.onCreate()

        Stylish.init(this)
        PRNGFixes.apply()
    }

    override fun onTerminate() {
        currentDatabase.closeAndClear(applicationContext)
        super.onTerminate()
    }
}
