package com.keepassdroid.password;

import android.net.Uri;

interface UriIntentInitTaskCallback {
    void onPostInitTask(Uri dbUri, Uri keyFileUri, Integer errorStringId);
}
