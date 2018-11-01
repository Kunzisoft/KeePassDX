/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.action;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.exception.ArcFourException;
import com.kunzisoft.keepass.database.exception.ContentFileNotFoundException;
import com.kunzisoft.keepass.database.exception.InvalidAlgorithmException;
import com.kunzisoft.keepass.database.exception.InvalidDBException;
import com.kunzisoft.keepass.database.exception.InvalidDBSignatureException;
import com.kunzisoft.keepass.database.exception.InvalidDBVersionException;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.database.exception.InvalidPasswordException;
import com.kunzisoft.keepass.database.exception.KeyFileEmptyException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class LoadDatabaseRunnable extends RunnableOnFinish {
    private static final String TAG = LoadDatabaseRunnable.class.getName();

    private Context mContext;
    private Database mDatabase;
    private Uri mUri;
    private String mPass;
    private Uri mKey;
    private boolean mRememberKeyfile;

    public LoadDatabaseRunnable(Context context, Database database, Uri uri, String pass, Uri key, OnFinishRunnable finish) {
        super(finish);

        this.mContext = context;
        this.mDatabase = database;
        this.mUri = uri;
        this.mPass = pass;
        this.mKey = key;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mRememberKeyfile = prefs.getBoolean(context.getString(R.string.keyfile_key), context.getResources().getBoolean(R.bool.keyfile_default));
    }

    @Override
    public void run() {
        try {
            mDatabase.loadData(mContext, mUri, mPass, mKey, mStatus);
            saveFileData(mUri, mKey);
        } catch (ArcFourException e) {
            catchError(e, R.string.error_arc4);
            return;
        } catch (InvalidPasswordException e) {
            catchError(e, R.string.invalid_password);
            return;
        } catch (ContentFileNotFoundException e) {
            catchError(e, R.string.file_not_found_content);
            return;
        } catch (FileNotFoundException e) {
            catchError(e, R.string.file_not_found);
            return;
        } catch (IOException e) {
            if (e.getMessage().contains("Hash failed with code"))
                catchError(e, R.string.error_load_database_KDF_memory, true);
            else
                catchError(e, R.string.error_load_database, true);
            return;
        } catch (KeyFileEmptyException e) {
            catchError(e, R.string.keyfile_is_empty);
            return;
        } catch (InvalidAlgorithmException e) {
            catchError(e, R.string.invalid_algorithm);
            return;
        } catch (InvalidKeyFileException e) {
            catchError(e, R.string.keyfile_does_not_exist);
            return;
        } catch (InvalidDBSignatureException e) {
            catchError(e, R.string.invalid_db_sig);
            return;
        } catch (InvalidDBVersionException e) {
            catchError(e, R.string.unsupported_db_version);
            return;
        } catch (InvalidDBException e) {
            catchError(e, R.string.error_invalid_db);
            return;
        } catch (OutOfMemoryError e) {
            catchError(e, R.string.error_out_of_memory);
            return;
        } catch (Exception e) {
            catchError(e, R.string.error_load_database, true);
            return;
        }
        finish(true);
    }

    private void catchError(Throwable e, @StringRes int messageId) {
        catchError(e, messageId, false);
    }

    private void catchError(Throwable e, @StringRes int messageId, boolean addThrowableMessage) {
        String errorMessage = mContext.getString(messageId);
        Log.e(TAG, errorMessage, e);
        if (addThrowableMessage)
            errorMessage = errorMessage + " " + e.getLocalizedMessage();
        finish(false, errorMessage);
    }

    private void saveFileData(Uri uri, Uri key) {
        if ( ! mRememberKeyfile ) {
            key = null;
        }

        App.getFileHistory().createFile(uri, key);
    }

}
