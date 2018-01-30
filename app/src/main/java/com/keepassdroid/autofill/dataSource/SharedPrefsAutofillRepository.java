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
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.ArraySet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keepassdroid.model.FilledAutofillFieldCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Singleton autofill data repository that stores autofill fields to SharedPreferences.
 * <p>
 * <p><b>Disclaimer</b>: you should not store sensitive fields like user data unencrypted.
 * This is done here only for simplicity and learning purposes.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class SharedPrefsAutofillRepository implements AutofillDataSource {
    private static final String SHARED_PREF_KEY = "com.example.android.autofill"
            + ".service.datasource.AutofillDataSource";
    private static final String CLIENT_FORM_DATA_KEY = "loginCredentialDatasets";
    private static final String DATASET_NUMBER_KEY = "datasetNumber";
    private static SharedPrefsAutofillRepository sInstance;

    private SharedPrefsAutofillRepository() {
    }

    public static SharedPrefsAutofillRepository getInstance() {
        if (sInstance == null) {
            sInstance = new SharedPrefsAutofillRepository();
        }
        return sInstance;
    }

    @Override
    public HashMap<String, FilledAutofillFieldCollection> getFilledAutofillFieldCollection(
            Context context, List<String> focusedAutofillHints, List<String> allAutofillHints) {
        boolean hasDataForFocusedAutofillHints = false;
        HashMap<String, FilledAutofillFieldCollection> clientFormDataMap = new HashMap<>();
        Set<String> clientFormDataStringSet = getAllAutofillDataStringSet(context);
        for (String clientFormDataString : clientFormDataStringSet) {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            FilledAutofillFieldCollection filledAutofillFieldCollection =
                    gson.fromJson(clientFormDataString, FilledAutofillFieldCollection.class);
            if (filledAutofillFieldCollection != null) {
                if (filledAutofillFieldCollection.helpsWithHints(focusedAutofillHints)) {
                    // Saved data has data relevant to at least 1 of the hints associated with the
                    // View in focus.
                    hasDataForFocusedAutofillHints = true;
                }
                if (filledAutofillFieldCollection.helpsWithHints(allAutofillHints)) {
                    // Saved data has data relevant to at least 1 of these hints associated with any
                    // of the Views in the hierarchy.
                    clientFormDataMap.put(filledAutofillFieldCollection.getDatasetName(),
                            filledAutofillFieldCollection);
                }
            }
        }
        if (hasDataForFocusedAutofillHints) {
            return clientFormDataMap;
        } else {
            return null;
        }
    }

    @Override
    public void saveFilledAutofillFieldCollection(Context context,
            FilledAutofillFieldCollection filledAutofillFieldCollection) {
        String datasetName = "dataset-" + getDatasetNumber(context);
        filledAutofillFieldCollection.setDatasetName(datasetName);
        Set<String> allAutofillData = getAllAutofillDataStringSet(context);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        allAutofillData.add(gson.toJson(filledAutofillFieldCollection));
        saveAllAutofillDataStringSet(context, allAutofillData);
        incrementDatasetNumber(context);
    }

    @Override
    public void clear(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .edit()
                .remove(CLIENT_FORM_DATA_KEY)
                .remove(DATASET_NUMBER_KEY)
                .apply();
    }

    private Set<String> getAllAutofillDataStringSet(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .getStringSet(CLIENT_FORM_DATA_KEY, new ArraySet<String>());
    }

    private void saveAllAutofillDataStringSet(Context context,
            Set<String> allAutofillDataStringSet) {
        context.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(CLIENT_FORM_DATA_KEY, allAutofillDataStringSet)
                .apply();
    }

    /**
     * For simplicity, datasets will be named in the form "dataset-X" where X means
     * this was the Xth dataset saved.
     */
    private int getDatasetNumber(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .getInt(DATASET_NUMBER_KEY, 0);
    }

    /**
     * Every time a dataset is saved, this should be called to increment the dataset number.
     * (only important for this service's dataset naming scheme).
     */
    private void incrementDatasetNumber(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .edit()
                .putInt(DATASET_NUMBER_KEY, getDatasetNumber(context) + 1)
                .apply();
    }
}