package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.view.View;

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.database.action.SaveDBRunnable;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;

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
            assert getActivity() != null;

            if (database != null && afterSaveDatabase != null) {
                SaveDBRunnable saveDBRunnable = new SaveDBRunnable(getContext(), database, afterSaveDatabase);
                saveDBRunnable.setUpdateProgressTaskStatus(
                        new UpdateProgressTaskStatus(getContext(),
                                SaveDatabaseProgressTaskDialogFragment.start(
                                        getActivity().getSupportFragmentManager())
                        ));
                new Thread(saveDBRunnable).start();
            }
        }
    }

    public void setAfterSaveDatabase(OnFinishRunnable afterSaveDatabase) {
        this.afterSaveDatabase = afterSaveDatabase;
    }
}
