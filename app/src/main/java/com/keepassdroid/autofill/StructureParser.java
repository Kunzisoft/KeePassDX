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

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.app.assist.AssistStructure.WindowNode;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.keepassdroid.autofill.dataSource.SharedPrefsDigitalAssetLinksRepository;
import com.keepassdroid.model.FilledAutofillFieldCollection;
import com.kunzisoft.keepass.R;

/**
 * Parser for an AssistStructure object. This is invoked when the Autofill Service receives an
 * AssistStructure from the client Activity, representing its View hierarchy. In this sample, it
 * parses the hierarchy and collects autofill metadata from {@link ViewNode}s along the way.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
final class StructureParser {
    private final AutofillFieldMetadataCollection mAutofillFields =
            new AutofillFieldMetadataCollection();
    private final Context mContext;
    private final AssistStructure mStructure;
    private FilledAutofillFieldCollection mFilledAutofillFieldCollection;

    StructureParser(Context context, AssistStructure structure) {
        mContext = context;
        mStructure = structure;
    }

    /**
     * Traverse AssistStructure and add ViewNode metadata to a flat list.
     */
    public void parseForFill() {
        Log.d(getClass().getName(), "Parsing structure for " + mStructure.getActivityComponent());
        int nodes = mStructure.getWindowNodeCount();
        mFilledAutofillFieldCollection = new FilledAutofillFieldCollection();
        StringBuilder webDomain = new StringBuilder();
        for (int i = 0; i < nodes; i++) {
            WindowNode node = mStructure.getWindowNodeAt(i);
            ViewNode view = node.getRootViewNode();
            parseLocked(view, webDomain);
        }
        if (webDomain.length() > 0) {
            String packageName = mStructure.getActivityComponent().getPackageName();
            boolean valid = SharedPrefsDigitalAssetLinksRepository.getInstance().isValid(mContext,
                    webDomain.toString(), packageName);
            if (!valid) {
                throw new SecurityException(mContext.getString(
                        R.string.invalid_link_association, webDomain, packageName));
            }
            Log.d(getClass().getName(), "Domain " + webDomain + " is valid for " + packageName);
        } else {
            Log.d(getClass().getName(), "no web domain");
        }
    }

    private void parseLocked(ViewNode viewNode, StringBuilder validWebDomain) {
        String webDomain = viewNode.getWebDomain();
        if (webDomain != null) {
            Log.d(getClass().getName(),"child web domain: " + webDomain);
            if (validWebDomain.length() > 0) {
                if (!webDomain.equals(validWebDomain.toString())) {
                    throw new SecurityException("Found multiple web domains: valid= "
                            + validWebDomain + ", child=" + webDomain);
                }
            } else {
                validWebDomain.append(webDomain);
            }
        }

        if (viewNode.getAutofillHints() != null) {
            String[] filteredHints = AutofillHints.filterForSupportedHints(
                    viewNode.getAutofillHints());
            if (filteredHints != null && filteredHints.length > 0) {
                mAutofillFields.add(new AutofillFieldMetadata(viewNode));
            }
        }
        int childrenSize = viewNode.getChildCount();
        if (childrenSize > 0) {
            for (int i = 0; i < childrenSize; i++) {
                parseLocked(viewNode.getChildAt(i), validWebDomain);
            }
        }
    }

    public AutofillFieldMetadataCollection getAutofillFields() {
        return mAutofillFields;
    }

    public FilledAutofillFieldCollection getClientFormData() {
        return mFilledAutofillFieldCollection;
    }
}
