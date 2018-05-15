/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.fileselect;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.kunzisoft.keepass.compat.StorageAF;
import com.kunzisoft.keepass.utils.Interaction;
import com.kunzisoft.keepass.utils.UriUtil;

import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;

public class KeyFileHelper {

    private static final String TAG = "KeyFileHelper";

    public static final String OPEN_INTENTS_FILE_BROWSE = "org.openintents.action.PICK_FILE";

    private static final int GET_CONTENT = 25745;
    private static final int OPEN_DOC = 25845;
    private static final int FILE_BROWSE = 25645;

    private Activity activity;
    private Fragment fragment;

    public KeyFileHelper(Activity context) {
        this.activity = context;
        this.fragment = null;
    }

    public KeyFileHelper(Fragment context) {
        this.activity = context.getActivity();
        this.fragment = context;
    }

    public class OpenFileOnClickViewListener implements View.OnClickListener {

        private ClickDataUriCallback dataUri;

        OpenFileOnClickViewListener(ClickDataUriCallback dataUri) {
            this.dataUri = dataUri;
        }

        @Override
        public void onClick(View v) {
            try {
                if (StorageAF.useStorageFramework(activity)) {
                    openActivityWithActionOpenDocument();
                } else {
                    openActivityWithActionGetContent();
                }
            } catch (Exception e) {
                Log.e(TAG,"Enable to start the file picker activity", e);

                // Open File picker if can't open activity
                Uri uri = null;
                if (dataUri != null)
                    uri = dataUri.onRequestIntentFilePicker();
                if(lookForOpenIntentsFilePicker(uri))
                    showBrowserDialog();
            }
        }
    }

    private void openActivityWithActionOpenDocument() {
        Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        if (fragment != null)
            fragment.startActivityForResult(i, OPEN_DOC);
        else
            activity.startActivityForResult(i, OPEN_DOC);
    }

    private void openActivityWithActionGetContent() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if(fragment != null)
            fragment.startActivityForResult(i, GET_CONTENT);
        else
            activity.startActivityForResult(i, GET_CONTENT);
    }

    public OpenFileOnClickViewListener getOpenFileOnClickViewListener() {
        return new OpenFileOnClickViewListener(null);
    }

    public OpenFileOnClickViewListener getOpenFileOnClickViewListener(ClickDataUriCallback dataUri) {
        return new OpenFileOnClickViewListener(dataUri);
    }

    private boolean lookForOpenIntentsFilePicker(@Nullable Uri dataUri) {
        boolean showBrowser = false;
        try {
            if (Interaction.isIntentAvailable(activity, OPEN_INTENTS_FILE_BROWSE)) {
                Intent i = new Intent(OPEN_INTENTS_FILE_BROWSE);
                // Get file path parent if possible
                if (dataUri != null
                        && dataUri.toString().length() > 0
                        && dataUri.getScheme().equals("file")) {
                        i.setData(dataUri);
                } else {
                    Log.w(getClass().getName(), "Unable to read the URI");
                }
                if(fragment != null)
                    fragment.startActivityForResult(i, FILE_BROWSE);
                else
                    activity.startActivityForResult(i, FILE_BROWSE);
            } else {
                showBrowser = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Enable to start OPEN_INTENTS_FILE_BROWSE", e);
            showBrowser = true;
        }
        return showBrowser;
    }

    /**
     * Show Browser dialog to select file picker app
     */
    private void showBrowserDialog() {
        try {
            BrowserDialog browserDialog = new BrowserDialog();
            if (fragment != null && fragment.getFragmentManager() != null)
                browserDialog.show(fragment.getFragmentManager(), "browserDialog");
            else if (activity.getFragmentManager() != null)
                browserDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), "browserDialog");
        } catch (Exception e) {
            Log.e(TAG, "Can't open BrowserDialog", e);
        }
    }

    /**
     * To use in onActivityResultCallback in Fragment or Activity
     * @param keyFileCallback Callback retrieve from data
     * @return true if requestCode was captured, false elsechere
     */
    public boolean onActivityResultCallback(
            int requestCode,
            int resultCode,
            Intent data,
            KeyFileCallback keyFileCallback) {

        switch (requestCode) {
            case FILE_BROWSE:
                if (resultCode == RESULT_OK) {
                    String filename = data.getDataString();
                    Uri keyUri = null;
                    if (filename != null) {
                        keyUri = UriUtil.parseDefaultFile(filename);
                    }
                    if (keyFileCallback != null)
                        keyFileCallback.onKeyFileResultCallback(keyUri);
                }
                return true;
            case GET_CONTENT:
            case OPEN_DOC:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            if (StorageAF.useStorageFramework(activity)) {
                                try {
                                    // try to persist read and write permissions
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        ContentResolver resolver = activity.getContentResolver();
                                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    }
                                } catch (Exception e) {
                                    // nop
                                }
                            }
                            if (requestCode == GET_CONTENT) {
                                uri = UriUtil.translate(activity, uri);
                            }
                            if (keyFileCallback != null)
                                keyFileCallback.onKeyFileResultCallback(uri);
                        }
                    }
                }
                return true;
        }
        return false;
    }

    public interface KeyFileCallback {
        void onKeyFileResultCallback(Uri uri);
    }

    public interface ClickDataUriCallback {
        Uri onRequestIntentFilePicker();
    }

}
