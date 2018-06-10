/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.autofill;

import android.app.Activity;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.selection.EntrySelectionHelper;

import java.util.ArrayList;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillHelper {

    public static final int AUTOFILL_RESPONSE_REQUEST_CODE = 8165;

    private AssistStructure assistStructure = null;

    public AssistStructure retrieveAssistStructure(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            assistStructure = intent.getParcelableExtra(android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE);
        }
        return assistStructure;
    }

    /**
     * Call retrieveAssistStructure before
     */
    public AssistStructure getAssistStructure() {
        return assistStructure;
    }

    public static void addAssistStructureExtraInIntent(Intent intent, AssistStructure assistStructure) {
        if (assistStructure != null) {
            EntrySelectionHelper.addEntrySelectionModeExtraInIntent(intent);
            intent.putExtra(android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE, assistStructure);
        }
    }

    /**
     * Define if android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE is an extra bundle key present in the Intent
     */
    public static boolean isIntentContainsExtraAssistStructureKey(Intent intent) {
        return (intent != null
                && intent.getExtras() != null
                && intent.getExtras().containsKey(android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE));
    }

    private @Nullable Dataset buildDataset(Context context, PwEntry entry,
                         StructureParser.Result struct) {
        String title = makeEntryTitle(entry);
        RemoteViews views = newRemoteViews(context.getPackageName(), title);
        Dataset.Builder builder = new Dataset.Builder(views);
        builder.setId(entry.getUUID().toString());

        if (entry.getPassword() != null) {
            AutofillValue value = AutofillValue.forText(entry.getPassword());
            struct.password.forEach(id -> builder.setValue(id, value));
        }
        if (entry.getUsername() != null) {
            AutofillValue value = AutofillValue.forText(entry.getUsername());
            List<AutofillId> ids = new ArrayList<>(struct.username);
            if (entry.getUsername().contains("@") || struct.username.isEmpty())
                ids.addAll(struct.email);
            ids.forEach(id -> builder.setValue(id, value));
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException e) {
            // if not value be set
            return null;
        }
    }

    static private String makeEntryTitle(PwEntry entry) {
        if (!entry.getTitle().isEmpty() && !entry.getUsername().isEmpty())
            return String.format("%s (%s)", entry.getTitle(), entry.getUsername());
        if (!entry.getTitle().isEmpty())
            return entry.getTitle();
        if (!entry.getUsername().isEmpty())
            return entry.getUsername();
        if (!entry.getNotes().isEmpty())
            return entry.getNotes().trim();
        return ""; // TODO No title
    }

    /**
     * Method to hit when right key is selected
     */
    public void buildResponseWhenEntrySelected(Activity activity, PwEntry entry) {
        Intent mReplyIntent;
        Intent intent = activity.getIntent();
        if (isIntentContainsExtraAssistStructureKey(intent)) {
            AssistStructure structure = intent.getParcelableExtra(android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE);
            StructureParser.Result result = new StructureParser(structure).parse();

            // New Response
            FillResponse.Builder responseBuilder = new FillResponse.Builder();
            Dataset dataset = buildDataset(activity, entry, result);
            responseBuilder.addDataset(dataset);
            mReplyIntent = new Intent();
            Log.d(activity.getClass().getName(), "Successed Autofill auth.");
            mReplyIntent.putExtra(
                    AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                    responseBuilder.build());
            activity.setResult(Activity.RESULT_OK, mReplyIntent);
        } else {
            Log.w(activity.getClass().getName(), "Failed Autofill auth.");
            activity.setResult(Activity.RESULT_CANCELED);
        }
    }

    /**
     * Utility method to loop and close each activity with return data
     */
    public static void onActivityResultSetResultAndFinish(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTOFILL_RESPONSE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                activity.setResult(resultCode, data);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                activity.setResult(Activity.RESULT_CANCELED);
            }
            activity.finish();
        }
    }

    private static RemoteViews newRemoteViews(String packageName, String remoteViewsText) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.autofill_service_list_item);
        presentation.setTextViewText(R.id.text, remoteViewsText);
        return presentation;
    }
}
