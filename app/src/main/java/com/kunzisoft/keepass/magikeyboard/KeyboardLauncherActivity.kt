package com.kunzisoft.keepass.magikeyboard

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.fileselect.FileSelectActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper

class KeyboardLauncherActivity : AppCompatActivity() {

    companion object {
        val TAG = KeyboardLauncherActivity::class.java.name!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.getDB().loaded && TimeoutHelper.checkTime(this))
            GroupActivity.launchForKeyboardSelection(this, PreferencesUtil.enableReadOnlyDatabase(this))
        else {
            // Pass extra to get entry
            FileSelectActivity.launchForKeyboardSelection(this)
        }
        finish()
        super.onCreate(savedInstanceState)
    }
}
