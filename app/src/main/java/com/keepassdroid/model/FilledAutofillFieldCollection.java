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
import android.service.autofill.Dataset;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import com.google.gson.annotations.Expose;
import com.keepassdroid.autofill.AutofillFieldMetadata;
import com.keepassdroid.autofill.AutofillFieldMetadataCollection;
import com.keepassdroid.autofill.AutofillHints;
import com.keepassdroid.autofill.W3cHints;

import java.util.HashMap;
import java.util.List;

/**
 * FilledAutofillFieldCollection is the model that holds all of the data on a client app's page,
 * plus the dataset name associated with it.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class FilledAutofillFieldCollection {
    @Expose
    private final HashMap<String, FilledAutofillField> mHintMap;
    @Expose
    private String mDatasetName;

    public FilledAutofillFieldCollection() {
        this(null, new HashMap<>());
    }

    public FilledAutofillFieldCollection(String datasetName, HashMap<String, FilledAutofillField> hintMap) {
        mHintMap = hintMap;
        mDatasetName = datasetName;
    }

    private static boolean isW3cSectionPrefix(String hint) {
        return hint.startsWith(W3cHints.PREFIX_SECTION);
    }

    private static boolean isW3cAddressType(String hint) {
        switch (hint) {
            case W3cHints.SHIPPING:
            case W3cHints.BILLING:
                return true;
        }
        return false;
    }

    private static boolean isW3cTypePrefix(String hint) {
        switch (hint) {
            case W3cHints.PREFIX_WORK:
            case W3cHints.PREFIX_FAX:
            case W3cHints.PREFIX_HOME:
            case W3cHints.PREFIX_PAGER:
                return true;
        }
        return false;
    }

    private static boolean isW3cTypeHint(String hint) {
        switch (hint) {
            case W3cHints.TEL:
            case W3cHints.TEL_COUNTRY_CODE:
            case W3cHints.TEL_NATIONAL:
            case W3cHints.TEL_AREA_CODE:
            case W3cHints.TEL_LOCAL:
            case W3cHints.TEL_LOCAL_PREFIX:
            case W3cHints.TEL_LOCAL_SUFFIX:
            case W3cHints.TEL_EXTENSION:
            case W3cHints.EMAIL:
            case W3cHints.IMPP:
                return true;
        }
        Log.w(FilledAutofillFieldCollection.class.getName(),"Invalid W3C type hint: " + hint);
        return false;
    }

    /**
     * Returns the name of the {@link Dataset}.
     */
    public String getDatasetName() {
        return mDatasetName;
    }

    /**
     * Sets the {@link Dataset} name.
     */
    public void setDatasetName(String datasetName) {
        mDatasetName = datasetName;
    }

    /**
     * Adds a {@code FilledAutofillField} to the collection, indexed by all of its hints.
     */
    public void add(@NonNull FilledAutofillField filledAutofillField) {
        String[] autofillHints = filledAutofillField.getAutofillHints();
        String nextHint = null;
        for (int i = 0; i < autofillHints.length; i++) {
            String hint = autofillHints[i];
            if (i < autofillHints.length - 1) {
                nextHint = autofillHints[i + 1];
            }
            // First convert the compound W3C autofill hints
            if (isW3cSectionPrefix(hint) && i < autofillHints.length - 1) {
                hint = autofillHints[++i];
                Log.d(getClass().getName(), "Hint is a W3C section prefix; using " + hint + " instead");
                if (i < autofillHints.length - 1) {
                    nextHint = autofillHints[i + 1];
                }
            }
            if (isW3cTypePrefix(hint) && nextHint != null && isW3cTypeHint(nextHint)) {
                hint = nextHint;
                i++;
                Log.d(getClass().getName(), "Hint is a W3C type prefix; using "+ hint +" instead");
            }
            if (isW3cAddressType(hint) && nextHint != null) {
                hint = nextHint;
                i++;
                Log.d(getClass().getName(), "Hint is a W3C address prefix; using " + hint + " instead");
            }

            // Then check if the "actual" hint is supported.
            if (AutofillHints.isValidHint(hint)) {
                mHintMap.put(hint, filledAutofillField);
            } else {
                Log.e(getClass().getName(), "Invalid hint: " + autofillHints[i]);
            }
        }
    }

    /**
     * Populates a {@link Dataset.Builder} with appropriate values for each {@link AutofillId}
     * in a {@code AutofillFieldMetadataCollection}.
     * <p>
     * In other words, it constructs an autofill
     * {@link Dataset.Builder} by applying saved values (from this {@code FilledAutofillFieldCollection})
     * to Views specified in a {@code AutofillFieldMetadataCollection}, which represents the current
     * page the user is on.
     */
    public boolean applyToFields(AutofillFieldMetadataCollection autofillFieldMetadataCollection,
            Dataset.Builder datasetBuilder) {
        boolean setValueAtLeastOnce = false;
        List<String> allHints = autofillFieldMetadataCollection.getAllHints();
        for (int hintIndex = 0; hintIndex < allHints.size(); hintIndex++) {
            String hint = allHints.get(hintIndex);
            List<AutofillFieldMetadata> fillableAutofillFields =
                    autofillFieldMetadataCollection.getFieldsForHint(hint);
            if (fillableAutofillFields == null) {
                continue;
            }
            for (int autofillFieldIndex = 0; autofillFieldIndex < fillableAutofillFields.size(); autofillFieldIndex++) {
                FilledAutofillField filledAutofillField = mHintMap.get(hint);
                if (filledAutofillField == null) {
                    continue;
                }
                AutofillFieldMetadata autofillFieldMetadata = fillableAutofillFields.get(autofillFieldIndex);
                AutofillId autofillId = autofillFieldMetadata.getId();
                int autofillType = autofillFieldMetadata.getAutofillType();
                switch (autofillType) {
                    case View.AUTOFILL_TYPE_LIST:
                        int listValue = autofillFieldMetadata.getAutofillOptionIndex(filledAutofillField.getTextValue());
                        if (listValue != -1) {
                            datasetBuilder.setValue(autofillId, AutofillValue.forList(listValue));
                            setValueAtLeastOnce = true;
                        }
                        break;
                    case View.AUTOFILL_TYPE_DATE:
                        Long dateValue = filledAutofillField.getDateValue();
                        if (dateValue != null) {
                            datasetBuilder.setValue(autofillId, AutofillValue.forDate(dateValue));
                            setValueAtLeastOnce = true;
                        }
                        break;
                    case View.AUTOFILL_TYPE_TEXT:
                        String textValue = filledAutofillField.getTextValue();
                        if (textValue != null) {
                            datasetBuilder.setValue(autofillId, AutofillValue.forText(textValue));
                            setValueAtLeastOnce = true;
                        }
                        break;
                    case View.AUTOFILL_TYPE_TOGGLE:
                        Boolean toggleValue = filledAutofillField.getToggleValue();
                        if (toggleValue != null) {
                            datasetBuilder.setValue(autofillId, AutofillValue.forToggle(toggleValue));
                            setValueAtLeastOnce = true;
                        }
                        break;
                    case View.AUTOFILL_TYPE_NONE:
                    default:
                        Log.w(getClass().getName(), "Invalid autofill type - " + autofillType);
                        break;
                }
            }
        }
        return setValueAtLeastOnce;
    }

    /**
     * Takes in a list of autofill hints (autofillHints), usually associated with a View or set of
     * Views. Returns whether any of the filled fields on the page have at least 1 of these
     * autofillHints.
     */
    public boolean helpsWithHints(List<String> autofillHints) {
        for (int i = 0; i < autofillHints.size(); i++) {
            String autofillHint = autofillHints.get(i);
            if (mHintMap.containsKey(autofillHint) && !mHintMap.get(autofillHint).isNull()) {
                return true;
            }
        }
        return false;
    }
}
