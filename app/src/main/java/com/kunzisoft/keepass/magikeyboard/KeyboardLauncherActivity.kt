package com.kunzisoft.keepass.magikeyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper

class KeyboardLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Database.getInstance().loaded && TimeoutHelper.checkTime(this))
            GroupActivity.launchForKeyboardSelection(this, PreferencesUtil.enableReadOnlyDatabase(this))
        else {
            // Pass extra to get entry
            FileDatabaseSelectActivity.launchForKeyboardSelection(this)
        }
        finish()
        super.onCreate(savedInstanceState)
    }
}
