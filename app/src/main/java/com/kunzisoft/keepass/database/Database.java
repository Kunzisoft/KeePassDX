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
package com.kunzisoft.keepass.database;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory;
import com.kunzisoft.keepass.database.exception.ContentFileNotFoundException;
import com.kunzisoft.keepass.database.exception.InvalidDBException;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;
import com.kunzisoft.keepass.database.load.Importer;
import com.kunzisoft.keepass.database.load.ImporterFactory;
import com.kunzisoft.keepass.database.save.PwDbOutput;
import com.kunzisoft.keepass.icons.IconDrawableFactory;
import com.kunzisoft.keepass.search.SearchDbHelper;
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater;
import com.kunzisoft.keepass.utils.UriUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;


public class Database {

    private static final String TAG = Database.class.getName();

    private PwDatabase pm;
    private Uri mUri;
    private SearchDbHelper searchHelper;
    private boolean readOnly = false;
    private boolean passwordEncodingError = false;

    private IconDrawableFactory drawFactory = new IconDrawableFactory();

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

    public IconDrawableFactory getDrawFactory() {
        return drawFactory;
    }

    public boolean getLoaded() {
        return loaded;
    }

    public void setLoaded() {
        loaded = true;
    }

    public void loadData(Context ctx, Uri uri, String password, Uri keyfile, ProgressTaskUpdater status) throws IOException, FileNotFoundException, InvalidDBException {
        loadData(ctx, uri, password, keyfile, status, !Importer.DEBUG);
    }

    private void loadData(Context ctx, Uri uri, String password, Uri keyfile, ProgressTaskUpdater status, boolean debug) throws IOException, FileNotFoundException, InvalidDBException {
        mUri = uri;
        readOnly = false;
        if (uri.getScheme().equals("file")) {
            File file = new File(uri.getPath());
            readOnly = !file.canWrite();
        }

        passUrisAsInputStreams(ctx, uri, password, keyfile, status, debug);
    }

    private void passUrisAsInputStreams(Context ctx, Uri uri, String password, Uri keyfile, ProgressTaskUpdater status, boolean debug) throws IOException, FileNotFoundException, InvalidDBException {
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
        loadData(ctx, is, password, kfIs, status, debug);
    }

    public void loadData(Context ctx, InputStream is, String password, InputStream keyFileInputStream, boolean debug) throws IOException, InvalidDBException {
        loadData(ctx, is, password, keyFileInputStream, null, debug);
    }

    private void loadData(Context ctx, InputStream is, String password, InputStream keyFileInputStream, ProgressTaskUpdater progressTaskUpdater, boolean debug) throws IOException, InvalidDBException {
        BufferedInputStream bis = new BufferedInputStream(is);

        if ( ! bis.markSupported() ) {
            throw new IOException("Input stream does not support mark.");
        }

        // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
        bis.mark(10);

        Importer databaseImporter = ImporterFactory.createImporter(bis, debug);

        bis.reset();  // Return to the start

        pm = databaseImporter.openDatabase(bis, password, keyFileInputStream, progressTaskUpdater);
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

    public void saveData(Context ctx) throws IOException, PwDbOutputException {
        saveData(ctx, mUri);
    }

    private void saveData(Context ctx, Uri uri) throws IOException, PwDbOutputException {
        String errorMessage = "Failed to store database.";

        if (uri.getScheme().equals("file")) {
            String filename = uri.getPath();
            File tempFile = new File(filename + ".tmp");

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tempFile);
                PwDbOutput pmo = PwDbOutput.getInstance(pm, fos);
                if (pmo != null)
                    pmo.output();
            } catch (Exception e) {
                Log.e(TAG, errorMessage, e);
                throw new IOException(errorMessage, e);
            } finally {
                if (fos != null)
                    fos.close();
            }

            // Force data to disk before continuing
            try {
                fos.getFD().sync();
            } catch (SyncFailedException e) {
                // Ignore if fsync fails. We tried.
            }

            File orig = new File(filename);

            if (!tempFile.renameTo(orig)) {
                throw new IOException(errorMessage);
            }
        }
        else {
            OutputStream os = null;
            try {
                os = ctx.getContentResolver().openOutputStream(uri);
                PwDbOutput pmo = PwDbOutput.getInstance(pm, os);
                if (pmo != null)
                    pmo.output();
            } catch (Exception e) {
                Log.e(TAG, errorMessage, e);
                throw new IOException(errorMessage, e);
            } finally {
                if (os != null)
                    os.close();
            }
        }
        mUri = uri;
    }

    public void clear() {
        drawFactory.clearCache();

        pm = null;
        mUri = null;
        loaded = false;
        passwordEncodingError = false;
    }

    public String getVersion() {
        return getPwDatabase().getVersion().toString();
    }

    public boolean containsName() {
        switch (getPwDatabase().getVersion()) {
            default:
                return false;
            case V4:
                return true;
        }
    }

    public String getName() {
        switch (getPwDatabase().getVersion()) {
            default:
                return "";
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getName();
        }
    }

    public void assignName(String name) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                PwDatabaseV4 databaseV4 = ((PwDatabaseV4) getPwDatabase());
                databaseV4.setName(name);
                databaseV4.setNameChanged(new PwDate());
                break;
        }
    }

    public boolean containsDescription() {
        switch (getPwDatabase().getVersion()) {
            default:
                return false;
            case V4:
                return true;
        }
    }

    public String getDescription() {
        switch (getPwDatabase().getVersion()) {
            default:
                return "";
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getDescription();
        }
    }

    public void assignDescription(String description) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                ((PwDatabaseV4) getPwDatabase()).setDescription(description);
                ((PwDatabaseV4) getPwDatabase()).setDescriptionChanged(new PwDate());
        }
    }

    public String getDefaultUsername() {
        switch (getPwDatabase().getVersion()) {
            default:
                return "";
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getDefaultUserName();
        }
    }

    public void setDefaultUsername(String username) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                ((PwDatabaseV4) getPwDatabase()).setDefaultUserName(username);
                ((PwDatabaseV4) getPwDatabase()).setDefaultUserNameChanged(new PwDate());
        }
    }

    public PwEncryptionAlgorithm getEncryptionAlgorithm() {
        return getPwDatabase().getEncryptionAlgorithm();
    }

    public List<PwEncryptionAlgorithm> getAvailableEncryptionAlgorithms() {
        switch (getPwDatabase().getVersion()) {
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getAvailableEncryptionAlgorithms();
            case V3:
                return ((PwDatabaseV3) getPwDatabase()).getAvailableEncryptionAlgorithms();
        }
        return new ArrayList<>();
    }

    public boolean allowEncryptionAlgorithmModification() {
        return getAvailableEncryptionAlgorithms().size() > 1;
    }

    public void assignEncryptionAlgorithm(PwEncryptionAlgorithm algorithm) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                ((PwDatabaseV4) getPwDatabase()).setEncryptionAlgorithm(algorithm);
                ((PwDatabaseV4) getPwDatabase()).setDataEngine(algorithm.getCipherEngine());
                ((PwDatabaseV4) getPwDatabase()).setDataCipher(algorithm.getDataCipher());
        }
    }

    public String getEncryptionAlgorithmName(Resources resources) {
        return getPwDatabase().getEncryptionAlgorithm().getName(resources);
    }

    public List<KdfEngine> getAvailableKdfEngines() {
        switch (getPwDatabase().getVersion()) {
            case V4:
                return KdfFactory.kdfListV4;
            case V3:
                return KdfFactory.kdfListV3;
        }
        return new ArrayList<>();
    }

    public boolean allowKdfModification() {
        return getAvailableKdfEngines().size() > 1;
    }

    public KdfEngine getKdfEngine() {
        switch (getPwDatabase().getVersion()) {
            case V4:
                KdfEngine kdfEngine = ((PwDatabaseV4) getPwDatabase()).getKdfEngine();
                if (kdfEngine == null)
                    return KdfFactory.aesKdf;
                return kdfEngine;
            default:
            case V3:
                return KdfFactory.aesKdf;
        }
    }

    public void assignKdfEngine(KdfEngine kdfEngine) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                PwDatabaseV4 db = ((PwDatabaseV4) getPwDatabase());
                if (db.getKdfParameters() == null
                        || !db.getKdfParameters().getUUID().equals(kdfEngine.getDefaultParameters().getUUID()))
                    db.setKdfParameters(kdfEngine.getDefaultParameters());
                setNumberKeyEncryptionRounds(kdfEngine.getDefaultKeyRounds());
                setMemoryUsage(kdfEngine.getDefaultMemoryUsage());
                setParallelism(kdfEngine.getDefaultParallelism());
                break;
        }
    }

    public String getKeyDerivationName(Resources resources) {
        KdfEngine kdfEngine = getKdfEngine();
        if (kdfEngine != null) {
            return kdfEngine.getName(resources);
        }
        return "";
    }

    public String getNumberKeyEncryptionRoundsAsString() {
        return Long.toString(getNumberKeyEncryptionRounds());
    }

    public long getNumberKeyEncryptionRounds() {
        return getPwDatabase().getNumberKeyEncryptionRounds();
    }

    public void setNumberKeyEncryptionRounds(long numberRounds) throws NumberFormatException {
        getPwDatabase().setNumberKeyEncryptionRounds(numberRounds);
    }

    public String getMemoryUsageAsString() {
        return Long.toString(getMemoryUsage());
    }

    public long getMemoryUsage() {
        switch (getPwDatabase().getVersion()) {
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getMemoryUsage();
        }
        return KdfEngine.UNKNOW_VALUE;
    }

    public void setMemoryUsage(long memory) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                ((PwDatabaseV4) getPwDatabase()).setMemoryUsage(memory);
        }
    }

    public String getParallelismAsString() {
        return Integer.toString(getParallelism());
    }

    public int getParallelism() {
        switch (getPwDatabase().getVersion()) {
            case V4:
                return ((PwDatabaseV4) getPwDatabase()).getParallelism();
        }
        return KdfEngine.UNKNOW_VALUE;
    }

    public void setParallelism(int parallelism) {
        switch (getPwDatabase().getVersion()) {
            case V4:
                ((PwDatabaseV4) getPwDatabase()).setParallelism(parallelism);
        }
    }

    public PwEntry createEntry(PwGroup parent) {
        PwEntry newPwEntry = null;
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    newPwEntry = new PwEntryV3((PwGroupV3) parent);
                case V4:
                    newPwEntry = new PwEntryV4((PwGroupV4) parent);
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be created", e);
        }
        return newPwEntry;
    }

    public PwGroup createGroup(PwGroup parent) {
        PwGroup newPwGroup = null;
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    newPwGroup = new PwGroupV3((PwGroupV3) parent);
                case V4:
                    newPwGroup = new PwGroupV4((PwGroupV4) parent);
            }
            newPwGroup.setId(pm.newGroupId());
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be created", e);
        }
        return newPwGroup;
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

    public void updateGroup(PwGroup oldGroup, PwGroup newGroup) {
        try {
            switch (getPwDatabase().getVersion()) {
                case V3:
                    ((PwGroupV3) oldGroup).updateWith((PwGroupV3) newGroup);
                    break;
                case V4:
                    ((PwGroupV4) oldGroup).updateWith((PwGroupV4) newGroup);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be updated", e);
        }
    }

    /**
     * @return A duplicate entry with the same values, a new UUID,
     * @param entryToCopy
     * @param newParent
     */
    public @Nullable PwEntry copyEntry(PwEntry entryToCopy, PwGroup newParent) {
        try {
            // TODO encapsulate
            switch (getPwDatabase().getVersion()) {
                case V3:
                    PwEntryV3 entryV3Copied = ((PwEntryV3) entryToCopy).clone();
                    entryV3Copied.setUUID(UUID.randomUUID());
                    entryV3Copied.setParent((PwGroupV3) newParent);
                    addEntryTo(entryV3Copied, newParent);
                    return entryV3Copied;
                case V4:
                    PwEntryV4 entryV4Copied = ((PwEntryV4) entryToCopy).clone();
                    entryV4Copied.setUUID(UUID.randomUUID());
                    entryV4Copied.setParent((PwGroupV4) newParent);
                    addEntryTo(entryV4Copied, newParent);
                    return entryV4Copied;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be updated", e);
        }
        return null;
    }

    public void moveEntry(PwEntry entryToMove, PwGroup newParent) {
        removeEntryFrom(entryToMove, entryToMove.parent);
        addEntryTo(entryToMove, newParent);
    }

    public void moveGroup(PwGroup groupToMove, PwGroup newParent) {
        removeGroupFrom(groupToMove, groupToMove.parent);
        addGroupTo(groupToMove, newParent);
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

    public boolean isRecycleBinAvailable() {
        return getPwDatabase().isRecycleBinAvailable();
    }

    public boolean isRecycleBinEnabled() {
        return getPwDatabase().isRecycleBinEnabled();
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
