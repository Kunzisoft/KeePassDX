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
package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.view.View;

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;

public abstract class DatabaseSavePreferenceDialogFragmentCompat  extends InputPreferenceDialogFragmentCompat {

    protected Database database;

    private OnFinishRunnable afterSaveDatabase;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        this.database = App.getDB();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getActivity() != null;

            if (database != null && afterSaveDatabase != null) {
                SaveDatabaseRunnable saveDatabaseRunnable = new SaveDatabaseRunnable(getContext(),
                        database,
                        afterSaveDatabase);
                saveDatabaseRunnable.setUpdateProgressTaskStatus(
                        new UpdateProgressTaskStatus(getContext(),
                                SaveDatabaseProgressTaskDialogFragment.start(
                                        getActivity().getSupportFragmentManager())
                        ));
                new Thread(saveDatabaseRunnable).start();
            }
        }
    }

    public void setAfterSaveDatabase(OnFinishRunnable afterSaveDatabase) {
        this.afterSaveDatabase = afterSaveDatabase;
    }
}
