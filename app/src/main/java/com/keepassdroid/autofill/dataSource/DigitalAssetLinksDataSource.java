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
 * Helper format
 * <a href="https://developers.google.com/digital-asset-links/">Digital Asset Links</a> needs.
 */
public interface DigitalAssetLinksDataSource {

    /**
     * Checks if the association between a web domain and a package is valid.
     */
    boolean isValid(Context context, String webDomain, String packageName);

    /**
     * Clears all cached data.
     */
    void clear(Context context);
}
