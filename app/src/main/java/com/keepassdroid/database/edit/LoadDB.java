/*
 * Copyright 2009-2016 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.edit;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.app.App;
import com.keepassdroid.database.exception.ArcFourException;
import com.keepassdroid.database.exception.InvalidAlgorithmException;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.exception.KeyFileEmptyException;

public class LoadDB extends RunnableOnFinish {
    private Uri mUri;
    private String mPass;
    private Uri mKey;
    private Database mDb;
    private Context mCtx;
    private boolean mRememberKeyfile;

    public LoadDB(Database db, Context ctx, Uri uri, String pass, Uri key, OnFinish finish) {
        super(finish);

        mDb = db;
        mCtx = ctx;
        mUri = uri;
        mPass = pass;
        mKey = key;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        mRememberKeyfile = prefs.getBoolean(ctx.getString(R.string.keyfile_key), ctx.getResources().getBoolean(R.bool.keyfile_default));
    }

    @Override
    public void run() {
        try {
            mDb.LoadData(mCtx, mUri, mPass, mKey, mStatus);

            saveFileData(mUri, mKey);

        } catch (ArcFourException e) {
            finish(false, mCtx.getString(R.string.error_arc4));
            return;
        } catch (InvalidPasswordException e) {
            finish(false, mCtx.getString(R.string.InvalidPassword));
            return;
        } catch (FileNotFoundException e) {
            finish(false, mCtx.getString(R.string.FileNotFound));
            return;
        } catch (IOException e) {
            finish(false, e.getMessage());
            return;
        } catch (KeyFileEmptyException e) {
            finish(false, mCtx.getString(R.string.keyfile_is_empty));
            return;
        } catch (InvalidAlgorithmException e) {
            finish(false, mCtx.getString(R.string.invalid_algorithm));
            return;
        } catch (InvalidKeyFileException e) {
            finish(false, mCtx.getString(R.string.keyfile_does_not_exist));
            return;
        } catch (InvalidDBSignatureException e) {
            finish(false, mCtx.getString(R.string.invalid_db_sig));
            return;
        } catch (InvalidDBVersionException e) {
            finish(false, mCtx.getString(R.string.unsupported_db_version));
            return;
        } catch (InvalidDBException e) {
            finish(false, mCtx.getString(R.string.error_invalid_db));
            return;
        } catch (OutOfMemoryError e) {
            finish(false, mCtx.getString(R.string.error_out_of_memory));
            return;
        }

        finish(true);
    }

    private void saveFileData(Uri uri, Uri key) {
        if ( ! mRememberKeyfile ) {
            key = null;
        }

        App.getFileHistory().createFile(uri, key);
    }



}
