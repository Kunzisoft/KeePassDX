/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.keepassdroid.database.exception.ContentFileNotFoundException;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.icons.DrawableFactory;
import com.keepassdroid.search.SearchDbHelper;
import com.keepassdroid.tasks.UpdateStatus;
import com.keepassdroid.utils.UriUtil;
import com.kunzisoft.keepass.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;

/**
 * @author bpellin
 */
public class Database {
    public PwDatabase pm;
    public Uri mUri;
    public SearchDbHelper searchHelper;
    public boolean readOnly = false;
    public boolean passwordEncodingError = false;

    public DrawableFactory drawFactory = new DrawableFactory();

    private boolean loaded = false;

    public boolean Loaded() {
        return loaded;
    }

    public void setLoaded() {
        loaded = true;
    }

    public void LoadData(Context ctx, InputStream is, String password, InputStream keyInputStream) throws IOException, InvalidDBException {
        LoadData(ctx, is, password, keyInputStream, new UpdateStatus(), !Importer.DEBUG);
    }

    public void LoadData(Context ctx, Uri uri, String password, Uri keyfile) throws IOException, FileNotFoundException, InvalidDBException {
        LoadData(ctx, uri, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
    }

    public void LoadData(Context ctx, Uri uri, String password, Uri keyfile, UpdateStatus status) throws IOException, FileNotFoundException, InvalidDBException {
        LoadData(ctx, uri, password, keyfile, status, !Importer.DEBUG);
    }

    public void LoadData(Context ctx, Uri uri, String password, Uri keyfile, UpdateStatus status, boolean debug) throws IOException, FileNotFoundException, InvalidDBException {
        mUri = uri;
        readOnly = false;
        if (uri.getScheme().equals("file")) {
            File file = new File(uri.getPath());
            readOnly = !file.canWrite();
        }

        try {
            passUrisAsInputStreams(ctx, uri, password, keyfile, status, debug, 0);
        } catch (InvalidPasswordException e) {
            // Retry with rounds fix
            try {
                passUrisAsInputStreams(ctx, uri, password, keyfile, status, debug, getFixRounds(ctx));
            } catch (Exception e2) {
                // Rethrow original exception
                throw e;
            }
        }
    }

    private long getFixRounds(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getLong(ctx.getString(R.string.roundsFix_key), ctx.getResources().getInteger(R.integer.roundsFix_default));
    }


    private void passUrisAsInputStreams(Context ctx, Uri uri, String password, Uri keyfile, UpdateStatus status, boolean debug, long roundsFix) throws IOException, FileNotFoundException, InvalidDBException {
        InputStream is, kfIs;
        try {
            is = UriUtil.getUriInputStream(ctx, uri);
        } catch (Exception e) {
            Log.e("KPD", "Database::LoadData", e);
            throw ContentFileNotFoundException.getInstance(uri);
        }

        try {
            kfIs = UriUtil.getUriInputStream(ctx, keyfile);
        } catch (Exception e) {
            Log.e("KPD", "Database::LoadData", e);
            throw ContentFileNotFoundException.getInstance(keyfile);
        }
        LoadData(ctx, is, password, kfIs, status, debug, roundsFix);
    }

    public void LoadData(Context ctx, InputStream is, String password, InputStream kfIs, boolean debug) throws IOException, InvalidDBException {
        LoadData(ctx, is, password, kfIs, new UpdateStatus(), debug);
    }

    public void LoadData(Context ctx, InputStream is, String password, InputStream kfIs, UpdateStatus status, boolean debug) throws IOException, InvalidDBException {
        LoadData(ctx, is, password, kfIs, status, debug, 0);
    }

    public void LoadData(Context ctx, InputStream is, String password, InputStream kfIs, UpdateStatus status, boolean debug, long roundsFix) throws IOException, InvalidDBException {
        BufferedInputStream bis = new BufferedInputStream(is);

        if ( ! bis.markSupported() ) {
            throw new IOException("Input stream does not support mark.");
        }

        // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
        bis.mark(10);

        Importer imp = ImporterFactory.createImporter(bis, debug);

        bis.reset();  // Return to the start

        pm = imp.openDatabase(bis, password, kfIs, status, roundsFix);
        if ( pm != null ) {
            PwGroup root = pm.rootGroup;
            pm.populateGlobals(root);
            LoadData(ctx, pm, password, kfIs, status);
        }
        loaded = true;
    }

    public void LoadData(Context ctx, PwDatabase pm, String password, InputStream keyInputStream, UpdateStatus status) {
        if ( pm != null ) {
            passwordEncodingError = !pm.validatePasswordEncoding(password);
        }
        searchHelper = new SearchDbHelper(ctx);
        loaded = true;
    }

    public PwGroup Search(String str) {
        if (searchHelper == null) { return null; }
        return searchHelper.search(this, str);
    }

    public void SaveData(Context ctx) throws IOException, PwDbOutputException {
        SaveData(ctx, mUri);
    }

    public void SaveData(Context ctx, Uri uri) throws IOException, PwDbOutputException {
        if (uri.getScheme().equals("file")) {
            String filename = uri.getPath();
            File tempFile = new File(filename + ".tmp");
            FileOutputStream fos = new FileOutputStream(tempFile);
            //BufferedOutputStream bos = new BufferedOutputStream(fos);

            //PwDbV3Output pmo = new PwDbV3Output(pm, bos, App.getCalendar());
            PwDbOutput pmo = PwDbOutput.getInstance(pm, fos);
            pmo.output();
            //bos.flush();
            //bos.close();
            fos.close();

            // Force data to disk before continuing
            try {
                fos.getFD().sync();
            } catch (SyncFailedException e) {
                // Ignore if fsync fails. We tried.
            }

            File orig = new File(filename);

            if (!tempFile.renameTo(orig)) {
                throw new IOException("Failed to store database.");
            }
        }
        else {
            OutputStream os;
            try {
                os = ctx.getContentResolver().openOutputStream(uri);
            } catch (Exception e) {
                throw new IOException("Failed to store database.");
            }

            PwDbOutput pmo = PwDbOutput.getInstance(pm, os);
            pmo.output();
            os.close();
        }
        mUri = uri;
    }

    public void clear() {
        drawFactory.clear();

        pm = null;
        mUri = null;
        loaded = false;
        passwordEncodingError = false;
    }
}
