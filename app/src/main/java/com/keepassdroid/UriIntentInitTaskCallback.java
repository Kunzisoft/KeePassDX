package com.keepassdroid;

import android.net.Uri;

interface UriIntentInitTaskCallback {
    void onPostInitTask(Uri dbUri, Uri keyFileUri, Integer errorStringId);
}
