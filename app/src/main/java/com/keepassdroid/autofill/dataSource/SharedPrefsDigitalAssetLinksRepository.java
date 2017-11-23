/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.keepassdroid.autofill.dataSource;

import android.content.Context;


/**
 * Singleton repository that caches the result of Digital Asset Links checks.
 */
public class SharedPrefsDigitalAssetLinksRepository implements DigitalAssetLinksDataSource {

    private static SharedPrefsDigitalAssetLinksRepository sInstance;

    private SharedPrefsDigitalAssetLinksRepository() {
    }

    public static SharedPrefsDigitalAssetLinksRepository getInstance() {
        if (sInstance == null) {
            sInstance = new SharedPrefsDigitalAssetLinksRepository();
        }
        return sInstance;
    }

    @Override
    public boolean isValid(Context context, String webDomain, String packageName) {
        // TODO: implement caching. It could cache the whole domain -> (packagename, fingerprint),
        // but then either invalidate when the package change or when the DAL association times out
        // (the maxAge is part of the API response), or document that a real-life service
        // should do that.

        return true;
        /*
        String fingerprint = null;
        try {
            fingerprint = SecurityHelper.getFingerprint(context, packageName);
        } catch (Exception e) {
            Log.e(getClass().getName(), "error getting fingerprint for " + packageName);
            return false;
        }
        */
        // TODO Security return SecurityHelper.isValid(webDomain, packageName, fingerprint);
    }

    @Override
    public void clear(Context context) {
        // TODO: implement once if caches results or remove from the interface
    }
}
