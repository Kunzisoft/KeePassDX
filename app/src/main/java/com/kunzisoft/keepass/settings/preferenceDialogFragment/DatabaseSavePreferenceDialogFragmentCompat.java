package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.database.action.SaveDBRunnable;
import com.kunzisoft.keepass.tasks.ProgressTask;

public abstract class DatabaseSavePreferenceDialogFragmentCompat  extends InputPreferenceDialogFragmentCompat {

    protected Database database;

    private OnFinishRunnable afterSaveDatabase;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        database = App.getDB();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            if (database != null && afterSaveDatabase != null) {
                SaveDBRunnable save = new SaveDBRunnable(getContext(), database, afterSaveDatabase);
                ProgressTask pt = new ProgressTask(getActivity(), save, R.string.saving_database);
                pt.run();
            }
        }
    }

    public void setAfterSaveDatabase(OnFinishRunnable afterSaveDatabase) {
        this.afterSaveDatabase = afterSaveDatabase;
    }
}
