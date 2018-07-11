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
package com.kunzisoft.keepass.password;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.action.AssignPasswordInDatabaseRunnable;
import com.kunzisoft.keepass.database.action.FileOnFinishRunnable;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.dialogs.PasswordEncodingDialogHelper;
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;

public class AssignPasswordHelper {

    private AppCompatActivity context;

    private String masterPassword = null;
    private Uri keyfile = null;

    private boolean createProgressDialog;

    public AssignPasswordHelper(AppCompatActivity context,
                                boolean withMasterPassword,
                                String masterPassword,
                                boolean withKeyFile,
                                Uri keyfile) {
        this.context = context;
        if (withMasterPassword)
            this.masterPassword = masterPassword;
        if (withKeyFile)
            this.keyfile = keyfile;

        createProgressDialog = true;
    }

    public void setCreateProgressDialog(boolean createProgressDialog) {
        this.createProgressDialog = createProgressDialog;
    }

    public void assignPasswordInDatabase(FileOnFinishRunnable fileOnFinish) {
        AssignPasswordInDatabaseRunnable assignPasswordInDatabaseRunnable = new AssignPasswordInDatabaseRunnable(
                context,
                App.getDB(),
                masterPassword,
                keyfile,
                new AfterSave(fileOnFinish)
        );
        if (createProgressDialog) {
            assignPasswordInDatabaseRunnable.setUpdateProgressTaskStatus(
                    new UpdateProgressTaskStatus(context,
                            SaveDatabaseProgressTaskDialogFragment.start(
                                    context.getSupportFragmentManager())
                    ));
        }
        Thread taskThread = new Thread(assignPasswordInDatabaseRunnable);

        // Show the progress dialog now or after dialog confirmation
        if (App.getDB().getPwDatabase().validatePasswordEncoding(masterPassword)) {
            taskThread.start();
        } else {
            PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
            dialog.show(context, (newDialog, which) -> taskThread.start(), true);
        }
    }

    private class AfterSave extends OnFinishRunnable {
        private FileOnFinishRunnable mFinish;

        AfterSave(FileOnFinishRunnable finish) {
            super(finish);
            mFinish = finish;
        }

        @Override
        public void run() {
            super.run();

            context.runOnUiThread(() -> {
                if ( mSuccess ) {
                    if ( mFinish != null ) {
                        mFinish.setFilename(keyfile);
                    }
                } else {
                    if ( mMessage != null && mMessage.length() > 0 ) {
                        Toast.makeText(context, mMessage, Toast.LENGTH_LONG).show();
                    }
                }

                // To remove progress task
                ProgressTaskDialogFragment.stop(context);
            });
        }
    }
}
