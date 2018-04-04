/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
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
import tech.jgross.keepass.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;


public class Database {

    private static final String TAG = Database.class.getName();

    private PwDatabase pm;
    private Uri mUri;
    private SearchDbHelper searchHelper;
    private boolean readOnly = false;
    private boolean passwordEncodingError = false;

    private DrawableFactory drawFactory = new DrawableFactory();

    private boolean loaded = false;

    public PwDatabase getPwDatabase() {
        return pm;
    }

    public void setPwDatabase(PwDatabase pm) {
        this.pm = pm;
    }

    public void setUri(Uri mUri) {
        this.mUri = mUri;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isPasswordEncodingError() {
        return passwordEncodingError;
    }

    public DrawableFactory getDrawFactory() {
        return drawFactory;
    }

    public boolean getLoaded() {
        return loaded;
    }

    public void setLoaded() {
        loaded = true;
    }

    public void loadData(Context ctx, InputStream is, String password, InputStream keyInputStream) throws IOException, InvalidDBException {
        loadData(ctx, is, password, keyInputStream, new UpdateStatus(), !Importer.DEBUG);
    }

    public void loadData(Context ctx, Uri uri, String password, Uri keyfile) throws IOException, FileNotFoundException, InvalidDBException {
        loadData(ctx, uri, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
    }

    public void loadData(Context ctx, Uri uri, String password, Uri keyfile, UpdateStatus status) throws IOException, FileNotFoundException, InvalidDBException {
        loadData(ctx, uri, password, keyfile, status, !Importer.DEBUG);
    }

    public void loadData(Context ctx, Uri uri, String password, Uri keyfile, UpdateStatus status, boolean debug) throws IOException, FileNotFoundException, InvalidDBException {
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
            Log.e("KPD", "Database::loadData", e);
            throw ContentFileNotFoundException.getInstance(uri);
        }

        try {
            kfIs = UriUtil.getUriInputStream(ctx, keyfile);
        } catch (Exception e) {
            Log.e("KPD", "Database::loadData", e);
            throw ContentFileNotFoundException.getInstance(keyfile);
        }
        loadData(ctx, is, password, kfIs, status, debug, roundsFix);
    }

    public void loadData(Context ctx, InputStream is, String password, InputStream kfIs, boolean debug) throws IOException, InvalidDBException {
        loadData(ctx, is, password, kfIs, new UpdateStatus(), debug);
    }

    public void loadData(Context ctx, InputStream is, String password, InputStream kfIs, UpdateStatus status, boolean debug) throws IOException, InvalidDBException {
        loadData(ctx, is, password, kfIs, status, debug, 0);
    }

    public void loadData(Context ctx, InputStream is, String password, InputStream kfIs, UpdateStatus status, boolean debug, long roundsFix) throws IOException, InvalidDBException {
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
            try {
                switch (pm.getVersion()) {
                    case V3:
                        PwGroupV3 rootV3 = ((PwDatabaseV3) pm).getRootGroup();
                        ((PwDatabaseV3) pm).populateGlobals(rootV3);
                        passwordEncodingError = !pm.validatePasswordEncoding(password);
                        searchHelper = new SearchDbHelper.SearchDbHelperV3(ctx);
                        break;
                    case V4:
                        PwGroupV4 rootV4 = ((PwDatabaseV4) pm).getRootGroup();
                        ((PwDatabaseV4) pm).populateGlobals(rootV4);
                        passwordEncodingError = !pm.validatePasswordEncoding(password);
                        searchHelper = new SearchDbHelper.SearchDbHelperV4(ctx);
                        break;
                }
                loaded = true;
            } catch (Exception e) {
                Log.e(TAG, "Load can't be performed with this Database version", e);
                loaded = false;
            }
        }
    }

    public PwGroup search(String str) {
        if (searchHelper == null) { return null; }
        try {
            switch (pm.getVersion()) {
                case V3:
                    return ((SearchDbHelper.SearchDbHelperV3) searchHelper).search(((PwDatabaseV3) pm), str);
                case V4:
                    return ((SearchDbHelper.SearchDbHelperV4) searchHelper).search(((PwDatabaseV4) pm), str);
            }
        } catch (Exception e) {
            Log.e(TAG, "Search can't be performed with this SearchHelper", e);
        }
        return null;
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

    public void addEntryTo(PwEntry entry, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).addEntryTo((PwEntryV3) entry, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).addEntryTo((PwEntryV4) entry, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be added in this version of PwGroup", e);
        }
    }

    public void removeEntryFrom(PwEntry entry, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).removeEntryFrom((PwEntryV3) entry, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).removeEntryFrom((PwEntryV4) entry, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be removed from this version of PwGroup", e);
        }
    }

    public void addGroupTo(PwGroup group, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).addGroupTo((PwGroupV3) group, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).addGroupTo((PwGroupV4) group, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be added in this version of PwGroup", e);
        }
    }

    public void removeGroupFrom(PwGroup group, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).removeGroupFrom((PwGroupV3) group, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).removeGroupFrom((PwGroupV4) group, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be removed from this version of PwGroup", e);
        }
    }

    public boolean canRecycle(PwEntry entry) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    return ((PwDatabaseV3) getPwDatabase()).canRecycle((PwEntryV3) entry);
                case V4:
                    return ((PwDatabaseV4) getPwDatabase()).canRecycle((PwEntryV4) entry);
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be recycled", e);
        }
        return false;
    }

    public boolean canRecycle(PwGroup group) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    return ((PwDatabaseV3) getPwDatabase()).canRecycle((PwGroupV3) group);
                case V4:
                    return ((PwDatabaseV4) getPwDatabase()).canRecycle((PwGroupV4) group);
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be recycled", e);
        }
        return false;
    }

    public void recycle(PwEntry entry) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).recycle((PwEntryV3) entry);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).recycle((PwEntryV4) entry);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be recycled", e);
        }
    }

    public void recycle(PwGroup group) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).recycle((PwGroupV3) group);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).recycle((PwGroupV4) group);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be recycled", e);
        }
    }

    public void updateEntry(PwEntry oldEntry, PwEntry newEntry) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwEntryV3) oldEntry).updateWith((PwEntryV3) newEntry);
                    break;
                case V4:
                    ((PwEntryV4) oldEntry).updateWith((PwEntryV4) newEntry);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be updated", e);
        }
    }

    public void deleteEntry(PwEntry entry) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).deleteEntry((PwEntryV3) entry);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).deleteEntry((PwEntryV4) entry);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be deleted", e);
        }
    }

    public void deleteGroup(PwGroup group) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).deleteGroup((PwGroupV3) group);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).deleteGroup((PwGroupV4) group);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be deleted", e);
        }
    }

    public boolean isRecycleBinAvailabledAndEnabled() {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    return ((PwDatabaseV3) getPwDatabase()).isRecycleBinAvailable() &&
                            ((PwDatabaseV3) getPwDatabase()).isRecycleBinEnabled();
                case V4:
                    return ((PwDatabaseV4) getPwDatabase()).isRecycleBinAvailable() &&
                            ((PwDatabaseV4) getPwDatabase()).isRecycleBinEnabled();
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of database don't know if the Recyclebin is available", e);
        }
        return false;
    }

    public void undoRecycle(PwEntry entry, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).undoRecycle((PwEntryV3) entry, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).undoRecycle((PwEntryV4) entry, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of database can't undo Recycle of this version of PwEntry", e);
        }
    }

    public void undoRecycle(PwGroup group, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).undoRecycle((PwGroupV3) group, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).undoRecycle((PwGroupV4) group, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of database can't undo Recycle of this version of PwGroup", e);
        }
    }

    public void undoDeleteEntry(PwEntry entry, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).undoDeleteEntry((PwEntryV3) entry, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).undoDeleteEntry((PwEntryV4) entry, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of database can't undo the deletion of this version of PwEntry", e);
        }
    }

    public void undoDeleteGroup(PwGroup group, PwGroup parent) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwDatabaseV3) getPwDatabase()).undoDeleteGroup((PwGroupV3) group, (PwGroupV3) parent);
                    break;
                case V4:
                    ((PwDatabaseV4) getPwDatabase()).undoDeleteGroup((PwGroupV4) group, (PwGroupV4) parent);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of database can't undo the deletion of this version of PwGroup", e);
        }
    }
}
