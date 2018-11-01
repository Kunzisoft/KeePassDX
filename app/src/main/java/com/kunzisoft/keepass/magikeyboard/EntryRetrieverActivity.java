package com.kunzisoft.keepass.magikeyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.model.Entry;

public class EntryRetrieverActivity extends AppCompatActivity {

    public static final String TAG = EntryRetrieverActivity.class.getName();

    public static final int ENTRY_REQUEST_CODE = 271;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;
        try {
            intent = new Intent(this,
                    Class.forName("com.kunzisoft.keepass.selection.EntrySelectionAuthActivity"));
            startActivityForResult(intent, ENTRY_REQUEST_CODE);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load the entry retriever", e);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "Retrieve the entry selected");
        if (requestCode == ENTRY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Entry entry = data.getParcelableExtra("com.kunzisoft.keepass.extra.ENTRY_SELECTION_MODE");
                MagikIME.setEntryKey(entry);

                // Show the notification if allowed in Preferences
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_key),
                        getResources().getBoolean(R.bool.keyboard_notification_entry_default))) {
                    Intent notificationIntent = new Intent(this, KeyboardEntryNotificationService.class);
                    startService(notificationIntent);
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.w(TAG, "Entry not retrieved");
            }
        }
        finish();
    }
}
