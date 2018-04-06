package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.edit.OnFinish;
import com.kunzisoft.keepass.database.edit.SaveDB;
import com.kunzisoft.keepass.tasks.ProgressTask;

public abstract class DatabaseSavePreferenceDialogFragmentCompat  extends InputPreferenceDialogFragmentCompat {

    protected Database database;

    private OnFinish afterSaveDatabase;

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
                SaveDB save = new SaveDB(getContext(), database, afterSaveDatabase);
                ProgressTask pt = new ProgressTask(getContext(), save, R.string.saving_database);
                pt.run();
            }
        }
    }

    public void setAfterSaveDatabase(OnFinish afterSaveDatabase) {
        this.afterSaveDatabase = afterSaveDatabase;
    }
}
