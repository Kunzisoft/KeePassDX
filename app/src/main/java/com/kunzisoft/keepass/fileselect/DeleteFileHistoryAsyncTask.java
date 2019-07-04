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
package com.kunzisoft.keepass.fileselect;

import android.os.AsyncTask;

import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory;

class DeleteFileHistoryAsyncTask extends AsyncTask<FileDatabaseModel, Void, Void> {

    private AfterDeleteFileHistoryListener afterDeleteFileHistoryListener;
    private FileDatabaseHistory fileHistory;
    private FileDatabaseHistoryAdapter adapter;

    DeleteFileHistoryAsyncTask(AfterDeleteFileHistoryListener afterDeleteFileHistoryListener, FileDatabaseHistory fileHistory, FileDatabaseHistoryAdapter adapter) {
        this.afterDeleteFileHistoryListener = afterDeleteFileHistoryListener;
        this.fileHistory = fileHistory;
        this.adapter = adapter;
    }

    protected Void doInBackground(FileDatabaseModel... args) {
        fileHistory.deleteDatabaseUri(args[0].getFileUri());
        return null;
    }

    protected void onPostExecute(Void v) {
        adapter.notifyDataSetChanged();
        if (adapter.getItemCount() == 0) {
            if(afterDeleteFileHistoryListener != null)
                afterDeleteFileHistoryListener.afterDeleteFile();
        }
    }

    public interface AfterDeleteFileHistoryListener {
        void afterDeleteFile();
    }
}
