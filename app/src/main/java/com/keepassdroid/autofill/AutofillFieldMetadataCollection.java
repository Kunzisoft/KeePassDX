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
package com.keepassdroid.autofill;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.autofill.AutofillId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Data structure that stores a collection of {@code AutofillFieldMetadata}s. Contains all of the
 * client's {@code View} hierarchy autofill-relevant metadata.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class AutofillFieldMetadataCollection {

    private final List<AutofillId> mAutofillIds = new ArrayList<>();
    private final HashMap<String, List<AutofillFieldMetadata>> mAutofillHintsToFieldsMap = new HashMap<>();
    private final List<String> mAllAutofillHints = new ArrayList<>();
    private final List<String> mFocusedAutofillHints = new ArrayList<>();
    private int mSize = 0;
    private int mSaveType = 0;

    public void add(AutofillFieldMetadata autofillFieldMetadata) {
        mSaveType |= autofillFieldMetadata.getSaveType();
        mSize++;
        mAutofillIds.add(autofillFieldMetadata.getId());
        List<String> hintsList = Arrays.asList(autofillFieldMetadata.getHints());
        mAllAutofillHints.addAll(hintsList);
        if (autofillFieldMetadata.isFocused()) {
            mFocusedAutofillHints.addAll(hintsList);
        }
        for (String hint : autofillFieldMetadata.getHints()) {
            if (!mAutofillHintsToFieldsMap.containsKey(hint)) {
                mAutofillHintsToFieldsMap.put(hint, new ArrayList<>());
            }
            mAutofillHintsToFieldsMap.get(hint).add(autofillFieldMetadata);
        }
    }

    public int getSaveType() {
        return mSaveType;
    }

    public AutofillId[] getAutofillIds() {
        return mAutofillIds.toArray(new AutofillId[mSize]);
    }

    public List<AutofillFieldMetadata> getFieldsForHint(String hint) {
        return mAutofillHintsToFieldsMap.get(hint);
    }

    public List<String> getFocusedHints() {
        return mFocusedAutofillHints;
    }

    public List<String> getAllHints() {
        return mAllAutofillHints;
    }
}
