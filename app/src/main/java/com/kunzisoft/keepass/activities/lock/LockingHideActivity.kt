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
package com.kunzisoft.keepass.activities.lock

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager

/**
 * Locking Hide Activity that sets FLAG_SECURE to prevent screenshots, and from
 * appearing in the recent app preview
 */
abstract class LockingHideActivity : LockingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Several gingerbread devices have problems with FLAG_SECURE
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    /* (non-Javadoc) Workaround for HTC Linkify issues
	 * @see android.app.Activity#startActivity(android.content.Intent)
	 */
    override fun startActivity(intent: Intent) {
        try {
            if (intent.component != null && intent.component!!.shortClassName == ".HtcLinkifyDispatcherActivity") {
                intent.component = null
            }
            super.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            /* Catch the bad HTC implementation case */
            super.startActivity(Intent.createChooser(intent, null))
        }

    }
}
