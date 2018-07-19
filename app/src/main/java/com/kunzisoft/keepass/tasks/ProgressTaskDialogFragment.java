/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.tasks;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.utils.Util;

public class ProgressTaskDialogFragment extends DialogFragment implements ProgressTaskUpdater{

    public static final String PROGRESS_TASK_DIALOG_TAG = "progressDialogFragment";

    private static final int UNDEFINED = -1;

    private @StringRes int title = UNDEFINED;
    private @StringRes int message = UNDEFINED;

    private TextView titleView;
    private TextView messageView;
    private ProgressBar progressView;

    public static ProgressTaskDialogFragment start(FragmentManager fragmentManager, @StringRes int titleId)	{
        // Create an instance of the dialog fragment and show it
        ProgressTaskDialogFragment dialog = new ProgressTaskDialogFragment();
        dialog.updateTitle(titleId);
        dialog.show(fragmentManager, PROGRESS_TASK_DIALOG_TAG);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getActivity() != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        @SuppressLint("InflateParams")
        View root = inflater.inflate(R.layout.progress_dialog, null);
        builder.setView(root);

        titleView = root.findViewById(R.id.progress_dialog_title);
        messageView = root.findViewById(R.id.progress_dialog_message);
        progressView = root.findViewById(R.id.progress_dialog_bar);

        updateTitle(title);
        updateMessage(message);

        setCancelable(false);
        Util.lockScreenOrientation(getActivity());

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Util.unlockScreenOrientation(getActivity());
        super.onDismiss(dialog);
    }

    public static void stop(AppCompatActivity activity) {
        Fragment fragmentTask = activity.getSupportFragmentManager().findFragmentByTag(PROGRESS_TASK_DIALOG_TAG);
        if (fragmentTask != null) {
            ProgressTaskDialogFragment loadingDatabaseDialog = (ProgressTaskDialogFragment) fragmentTask;
            loadingDatabaseDialog.dismissAllowingStateLoss();
            Util.unlockScreenOrientation(activity);
        }
    }

    public void setTitle(@StringRes int titleId) {
        this.title = titleId;
    }

    private void updateView(TextView textView, @StringRes int resId) {
        if (textView != null) {
            if (resId == UNDEFINED) {
                textView.setVisibility(View.GONE);
            } else {
                textView.setText(resId);
                textView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateTitle(int resId) {
        this.title = resId;
        updateView(titleView, title);
    }

    @Override
    public void updateMessage(int resId) {
        this.message = resId;
        updateView(messageView, message);
    }
}
