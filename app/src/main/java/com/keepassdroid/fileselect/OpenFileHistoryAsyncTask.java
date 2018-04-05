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
package com.keepassdroid.fileselect;

import android.os.AsyncTask;

class OpenFileHistoryAsyncTask extends AsyncTask<Integer, Void, Void> {

    private AfterOpenFileHistoryListener afterOpenFileHistoryListener;
    private RecentFileHistory fileHistory;
    private String fileName;
    private String keyFile;

    OpenFileHistoryAsyncTask(AfterOpenFileHistoryListener afterOpenFileHistoryListener, RecentFileHistory fileHistory) {
        this.afterOpenFileHistoryListener = afterOpenFileHistoryListener;
        this.fileHistory = fileHistory;
    }

    protected Void doInBackground(Integer... args) {
        int position = args[0];
        fileName = fileHistory.getDatabaseAt(position);
        keyFile = fileHistory.getKeyfileAt(position);
        return null;
    }

    protected void onPostExecute(Void v) {
        afterOpenFileHistoryListener.afterOpenFile(fileName, keyFile);
    }

    public interface AfterOpenFileHistoryListener {
        void afterOpenFile(String fileName, String keyFile);
    }
}
