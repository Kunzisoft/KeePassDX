package com.kunzisoft.magikeyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class EntryRetrieverActivity extends AppCompatActivity {

    public static final String TAG = EntryRetrieverActivity.class.getName();

    public static final int ENTRY_REQUEST_CODE = 271;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) { // TODO lock for < jelly bean
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "Retrieve the entry selected");
        if (requestCode == ENTRY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // TODO get entry
                Log.e(TAG, data.getSerializableExtra("com.kunzisoft.keepass.extra.ENTRY_SELECTION_MODE").toString());
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.w(TAG, "Entry not retrieved");
            }
        }
        finish();
    }
}
