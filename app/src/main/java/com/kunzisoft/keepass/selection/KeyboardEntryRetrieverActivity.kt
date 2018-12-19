package com.kunzisoft.keepass.selection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.fileselect.FileSelectActivity
import com.kunzisoft.keepass.magikeyboard.KeyboardEntryNotificationService
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.Entry
import com.kunzisoft.keepass.selection.EntrySelectionHelper.ENTRY_SELECTION_RESPONSE_REQUEST_CODE
import com.kunzisoft.keepass.selection.EntrySelectionHelper.EXTRA_ENTRY_SELECTION_MODE

class KeyboardEntryRetrieverActivity : AppCompatActivity() {

    companion object {

        val TAG = KeyboardEntryRetrieverActivity::class.java.name!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.getDB().loaded)
            GroupActivity.launchForKeyboardResult(this, true)
        else {
            // Pass extra to get entry
            FileSelectActivity.launchForKeyboardResult(this)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Retrieve the entry selected, requestCode: $requestCode, resultCode: $resultCode")
        if (requestCode == ENTRY_SELECTION_RESPONSE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val entry = data?.getParcelableExtra<Entry>(EXTRA_ENTRY_SELECTION_MODE)
                Log.d(TAG, "Set the entry ${entry?.title} to keyboard")
                MagikIME.setEntryKey(entry)

                // Show the notification if allowed in Preferences
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_key),
                                resources.getBoolean(R.bool.keyboard_notification_entry_default))) {
                    val notificationIntent = Intent(this, KeyboardEntryNotificationService::class.java)
                    startService(notificationIntent)
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.w(TAG, "Entry not retrieved")
            }
        }
        finish()
    }
}
