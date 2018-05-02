package com.kunzisoft.keepass.tasks;

import android.support.v4.app.FragmentManager;

import com.kunzisoft.keepass.R;

public class SaveDatabaseProgressTaskDialogFragment extends ProgressTaskDialogFragment {

    public static SaveDatabaseProgressTaskDialogFragment start(FragmentManager fragmentManager)	{
        // Create an instance of the dialog fragment and show it
        SaveDatabaseProgressTaskDialogFragment dialog = new SaveDatabaseProgressTaskDialogFragment();
        dialog.updateTitle(R.string.saving_database);
        dialog.show(fragmentManager, PROGRESS_TASK_DIALOG_TAG);
        return dialog;
    }
}
