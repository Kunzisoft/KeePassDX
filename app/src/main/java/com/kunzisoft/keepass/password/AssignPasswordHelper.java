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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.action.FileOnFinishRunnable;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.database.action.SetPasswordRunnable;
import com.kunzisoft.keepass.dialogs.PasswordEncodingDialogHelper;
import com.kunzisoft.keepass.tasks.ProgressTask;

public class AssignPasswordHelper {

    private Activity context;

    private String masterPassword = null;
    private Uri keyfile = null;

    public AssignPasswordHelper(Activity context,
                                boolean withMasterPassword,
                                String masterPassword,
                                boolean withKeyFile,
                                Uri keyfile) {
        this.context = context;
        if (withMasterPassword)
            this.masterPassword = masterPassword;
        if (withKeyFile)
            this.keyfile = keyfile;
    }

    public void assignPasswordInDatabase(FileOnFinishRunnable fileOnFinish) {
        SetPasswordRunnable sp = new SetPasswordRunnable(context, App.getDB(), masterPassword, keyfile, new AfterSave(fileOnFinish, new Handler()));
        final ProgressTask pt = new ProgressTask(context, sp, R.string.saving_database);

        if (App.getDB().getPwDatabase().validatePasswordEncoding(masterPassword)) {
            pt.run();
        } else {
            PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
            dialog.show(context, (newDialog, which) -> pt.run(), true);
        }
    }

    private class AfterSave extends OnFinishRunnable {
        private FileOnFinishRunnable mFinish;

        public AfterSave(FileOnFinishRunnable finish, Handler handler) {
            super(finish, handler);
            mFinish = finish;
        }

        @Override
        public void run() {
            if ( mSuccess ) {
                if ( mFinish != null ) {
                    mFinish.setFilename(keyfile);
                }
            } else {
                displayMessage(context);
            }
            super.run();
        }
    }
}
