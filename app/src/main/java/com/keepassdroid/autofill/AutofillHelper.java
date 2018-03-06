/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.autofill;

import android.app.Activity;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.widget.RemoteViews;

import com.keepassdroid.autofill.dataSource.SharedPrefsAutofillRepository;
import com.keepassdroid.model.FilledAutofillFieldCollection;
import com.kunzisoft.keepass.R;

import java.util.HashMap;
import java.util.Set;

public class AutofillHelper {

    public static final int AUTOFILL_RESPONSE_REQUEST_CODE = 8165;

    private AssistStructure assistStructure;

    public void retrieveAssistStructure(Intent intent) {
        if (intent != null && intent.getExtras() != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE);
        }
    }

    /**
     * Call retrieveAssistStructure before
     */
    public AssistStructure getAssistStructure() {
        return assistStructure;
    }

    public static void addAssistStructureExtraInIntent(Intent intent, AssistStructure assistStructure) {
        if (assistStructure != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, assistStructure);
            }
        }
    }

    /**
     * Define if EXTRA_AUTHENTICATION_RESULT is an extra bundle key present in the Intent
     */
    public static boolean isIntentContainsAutofillAuthKey(Intent intent) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (intent != null
                && intent.getExtras() != null
                && intent.getExtras().containsKey(android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE));
    }

    /**
     * Method to hit when right key is selected
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void onAutofillResponse(Activity activity) {
        // TODO Connect this method in each item in GroupActivity
        Intent mReplyIntent = null;
        Intent intent = activity.getIntent();
        if (isIntentContainsAutofillAuthKey(intent)) {
            AssistStructure structure = intent.getParcelableExtra(
                    android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE);
            StructureParser parser = new StructureParser(activity, structure);
            parser.parseForFill();
            AutofillFieldMetadataCollection autofillFields = parser.getAutofillFields();
            mReplyIntent = new Intent();
            HashMap<String, FilledAutofillFieldCollection> clientFormDataMap =
                    SharedPrefsAutofillRepository.getInstance().getFilledAutofillFieldCollection
                            (activity, autofillFields.getFocusedHints(), autofillFields.getAllHints());

            Log.d(activity.getClass().getName(), "Successed Autofill auth.");
            mReplyIntent.putExtra(
                    android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                    AutofillHelper.newResponse
                            (activity, autofillFields, clientFormDataMap));
            activity.setResult(Activity.RESULT_OK, mReplyIntent);
        } else {
            Log.w(activity.getClass().getName(), "Failed Autofill auth.");
            activity.setResult(Activity.RESULT_CANCELED);
        }
    }

    /**
     * Utility method to loop and close each activity with return data
     */
    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTOFILL_RESPONSE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    activity.setResult(Activity.RESULT_OK, data);
                } else {
                    activity.setResult(Activity.RESULT_CANCELED);
                }
            } else
                activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
        }
    }

    /**
     * Wraps autofill data in a LoginCredential Dataset object which can then be sent back to the
     * client View.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Dataset newDataset(Context context,
                                     AutofillFieldMetadataCollection autofillFields,
                                     FilledAutofillFieldCollection filledAutofillFieldCollection) {
        String datasetName = filledAutofillFieldCollection.getDatasetName();
        if (datasetName != null) {
            Dataset.Builder datasetBuilder;
            datasetBuilder = new Dataset.Builder
                    (newRemoteViews(context.getPackageName(), datasetName));
            boolean setValueAtLeastOnce =
                    filledAutofillFieldCollection.applyToFields(autofillFields, datasetBuilder);
            if (setValueAtLeastOnce) {
                return datasetBuilder.build();
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static RemoteViews newRemoteViews(String packageName, String remoteViewsText) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.autofill_service_list_item);
        presentation.setTextViewText(R.id.text, remoteViewsText);
        return presentation;
    }

    /**
     * Wraps autofill data in a Response object (essentially a series of Datasets) which can then
     * be sent back to the client View.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static FillResponse newResponse(Context context,
                                           AutofillFieldMetadataCollection autofillFields,
                                           HashMap<String, FilledAutofillFieldCollection> clientFormDataMap) {
        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        if (clientFormDataMap != null) {
            Set<String> datasetNames = clientFormDataMap.keySet();
            for (String datasetName : datasetNames) {
                FilledAutofillFieldCollection filledAutofillFieldCollection =
                        clientFormDataMap.get(datasetName);
                if (filledAutofillFieldCollection != null) {
                    Dataset dataset = newDataset(context, autofillFields,
                            filledAutofillFieldCollection);
                    if (dataset != null) {
                        responseBuilder.addDataset(dataset);
                    }
                }
            }
        }
        int saveType = autofillFields.getSaveType();
        if (saveType != 0) {
            setFullSaveInfo(responseBuilder, saveType, autofillFields);
            return responseBuilder.build();
        } else {
            Log.d(AutofillHelper.class.getName(), "These fields are not meant to be saved by autofill.");
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void setFullSaveInfo(FillResponse.Builder responseBuilder, int saveType,
                                        AutofillFieldMetadataCollection autofillFields) {
        AutofillId[] autofillIds = autofillFields.getAutofillIds();
        responseBuilder.setSaveInfo(new SaveInfo.Builder(saveType, autofillIds).build());
    }
}
