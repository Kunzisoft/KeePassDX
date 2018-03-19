package com.keepassdroid.password;


import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;

import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SetPassword;
import com.kunzisoft.keepass.R;

public class AssignPasswordHelper {

    private Context context;

    private String masterPassword;
    private Uri keyfile;

    public AssignPasswordHelper(Context context,
                                String masterPassword,
                                Uri keyfile) {
        this.context = context;
        this.masterPassword = masterPassword;
        this.keyfile = keyfile;
    }

    public void assignPasswordInDatabase(FileOnFinish fileOnFinish) {
        SetPassword sp = new SetPassword(context, App.getDB(), masterPassword, keyfile, new AfterSave(fileOnFinish, new Handler()));
        final ProgressTask pt = new ProgressTask(context, sp, R.string.saving_database);
        boolean valid = sp.validatePassword(context, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                pt.run();
            }
        });

        if (valid) {
            pt.run();
        }
    }

    private class AfterSave extends OnFinish {
        private FileOnFinish mFinish;

        public AfterSave(FileOnFinish finish, Handler handler) {
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
