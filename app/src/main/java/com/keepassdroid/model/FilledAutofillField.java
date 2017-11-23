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
package com.keepassdroid.model;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillValue;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.Expose;
import com.keepassdroid.autofill.AutofillHints;

import java.util.Arrays;

import static com.keepassdroid.autofill.AutofillHints.convertToStoredHintNames;
import static com.keepassdroid.autofill.AutofillHints.filterForSupportedHints;

/**
 * JSON serializable data class containing the same data as an {@link AutofillValue}.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class FilledAutofillField {
    @Expose
    private String mTextValue = null;
    @Expose
    private Long mDateValue = null;
    @Expose
    private Boolean mToggleValue = null;

    //TODO add explicit mListValue

    /**
     * Does not need to be serialized into persistent storage, so it's not exposed.
     */
    private String[] mAutofillHints = null;

    public FilledAutofillField(String... hints) {
        mAutofillHints = filterForSupportedHints(hints);
        convertToStoredHintNames(mAutofillHints);
    }

    public void setListValue(CharSequence[] autofillOptions, int listValue) {
        /* Only set list value when a hint is allowed to store list values. */
        Preconditions.checkArgument(
                AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_LIST),
                "List is invalid autofill type for hint(s) - %s",
                Arrays.toString(mAutofillHints));
        if (autofillOptions != null && autofillOptions.length > 0) {
            mTextValue = autofillOptions[listValue].toString();
        } else {
            Log.w(getClass().getName(), "autofillOptions should have at least one entry.");
        }
    }

    public String[] getAutofillHints() {
        return mAutofillHints;
    }

    public String getTextValue() {
        return mTextValue;
    }

    public void setTextValue(CharSequence textValue) {
        /* Only set text value when a hint is allowed to store text values. */
        Preconditions.checkArgument(
                AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_TEXT),
                "Text is invalid autofill type for hint(s) - %s",
                Arrays.toString(mAutofillHints));
        mTextValue = textValue.toString();
    }

    public Long getDateValue() {
        return mDateValue;
    }

    public void setDateValue(Long dateValue) {
        /* Only set date value when a hint is allowed to store date values. */
        Preconditions.checkArgument(
                AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_DATE),
                "Date is invalid autofill type for hint(s) - %s"
                , Arrays.toString(mAutofillHints));
        mDateValue = dateValue;
    }

    public Boolean getToggleValue() {
        return mToggleValue;
    }

    public void setToggleValue(Boolean toggleValue) {
        /* Only set toggle value when a hint is allowed to store toggle values. */
        Preconditions.checkArgument(
                AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_TOGGLE),
                "Toggle is invalid autofill type for hint(s) - %s",
                Arrays.toString(mAutofillHints));
        mToggleValue = toggleValue;
    }

    public boolean isNull() {
        return mTextValue == null && mDateValue == null && mToggleValue == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilledAutofillField that = (FilledAutofillField) o;

        if (mTextValue != null ? !mTextValue.equals(that.mTextValue) : that.mTextValue != null)
            return false;
        if (mDateValue != null ? !mDateValue.equals(that.mDateValue) : that.mDateValue != null)
            return false;
        return mToggleValue != null ? mToggleValue.equals(that.mToggleValue) :
                that.mToggleValue == null;
    }

    @Override
    public int hashCode() {
        int result = mTextValue != null ? mTextValue.hashCode() : 0;
        result = 31 * result + (mDateValue != null ? mDateValue.hashCode() : 0);
        result = 31 * result + (mToggleValue != null ? mToggleValue.hashCode() : 0);
        return result;
    }
}
