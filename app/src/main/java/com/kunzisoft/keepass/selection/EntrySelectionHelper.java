package com.kunzisoft.keepass.selection;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.model.Entry;
import com.kunzisoft.keepass.model.Field;

public class EntrySelectionHelper {

    public static final int ENTRY_SELECTION_RESPONSE_REQUEST_CODE = 5164;

    public static final String EXTRA_ENTRY_SELECTION_MODE = "com.kunzisoft.keepass.extra.ENTRY_SELECTION_MODE";

    public static void addEntrySelectionModeExtraInIntent(Intent intent) {
        intent.putExtra(EXTRA_ENTRY_SELECTION_MODE, true);
    }

    public static boolean isIntentInEntrySelectionMode(Intent intent) {
        return intent.getBooleanExtra(EXTRA_ENTRY_SELECTION_MODE, false);
    }

    /**
     * Method to hit when right key is selected
     */
    public static void buildResponseWhenEntrySelected(Activity activity, PwEntry entry) {
        Intent mReplyIntent;
        Intent intent = activity.getIntent();
        boolean entrySelectionMode = isIntentInEntrySelectionMode(intent);
        if (entrySelectionMode) {
            mReplyIntent = new Intent();
            Log.d(activity.getClass().getName(), "Reply entry selection");

            Entry entryModel = new Entry();
            entryModel.setTitle(entry.getTitle());
            entryModel.setUsername(entry.getUsername());
            entryModel.setPassword(entry.getPassword());
            entryModel.setUrl(entry.getUrl());
            if (entry.containsCustomFields()) {
                entry.getFields()
                        .doActionToAllCustomProtectedField(
                                (key, value) -> entryModel.addCustomField(
                                        new Field(key, value.toString())));
            }

            mReplyIntent.putExtra(
                    EXTRA_ENTRY_SELECTION_MODE,
                    entryModel);
            activity.setResult(Activity.RESULT_OK, mReplyIntent);
        } else {
            activity.setResult(Activity.RESULT_CANCELED);
        }
    }

    /**
     * Utility method to loop and close each activity with return data
     */
    public static void onActivityResultSetResultAndFinish(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == ENTRY_SELECTION_RESPONSE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                activity.setResult(resultCode, data);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                activity.setResult(Activity.RESULT_CANCELED);
            }
            activity.finish();
        }
    }
}
