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

import com.keepassdroid.model.FilledAutofillField;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the properties associated with an autofill hint in this Autofill Service.
 */
public final class AutofillHintProperties {

    private String mAutofillHint;
    private FakeFieldGenerator mFakeFieldGenerator;
    private Set<Integer> mValidTypes;
    private int mSaveType;
    private int mPartition;

    // TODO Change to real field generator

    public AutofillHintProperties(String autofillHint, int saveType, int partitionNumber,
                                  FakeFieldGenerator fakeFieldGenerator, Integer... validTypes) {
        mAutofillHint = autofillHint;
        mSaveType = saveType;
        mPartition = partitionNumber;
        mFakeFieldGenerator = fakeFieldGenerator;
        mValidTypes = new HashSet<>(Arrays.asList(validTypes));
    }

    /**
     * Generates dummy autofill field data that is relevant to the autofill hint.
     */
    public FilledAutofillField generateFakeField(int seed) {
        return mFakeFieldGenerator.generate(seed);
    }

    /**
     * Returns autofill hint associated with these properties. If you save a field that uses a W3C
     * hint, there is a chance this will return a different but analogous hint, when applicable.
     * For example, W3C has hint 'email' and {@link android.view.View} has hint 'emailAddress', so
     * the W3C hint should map to the hint defined in {@link android.view.View} ('emailAddress').
     */
    public String getAutofillHint() {
        return mAutofillHint;
    }

    /**
     * Returns how this hint maps to a {@link android.service.autofill.SaveInfo} type.
     */
    public int getSaveType() {
        return mSaveType;
    }

    /**
     * Returns which data partition this autofill hint should be a part of. See partitions defined
     * in {@link AutofillHints}.
     */
    public int getPartition() {
        return mPartition;
    }


    /**
     * Sometimes, data for a hint should only be stored as a certain AutofillValue type. For
     * example, it is recommended that data representing a Credit Card Expiration date, annotated
     * with the hint {@link android.view.View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE}, should
     * only be stored as {@link android.view.View.AUTOFILL_TYPE_DATE}.
     */
    public boolean isValidType(int type) {
        return mValidTypes.contains(type);
    }
}
