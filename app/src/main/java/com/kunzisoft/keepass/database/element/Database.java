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
package com.kunzisoft.keepass.database.element;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory;
import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;
import com.kunzisoft.keepass.database.cursor.EntryCursorV3;
import com.kunzisoft.keepass.database.cursor.EntryCursorV4;
import com.kunzisoft.keepass.database.exception.ContentFileNotFoundException;
import com.kunzisoft.keepass.database.exception.InvalidDBException;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;
import com.kunzisoft.keepass.database.load.Importer;
import com.kunzisoft.keepass.database.load.ImporterFactory;
import com.kunzisoft.keepass.database.save.PwDbOutput;
import com.kunzisoft.keepass.database.search.SearchDbHelper;
import com.kunzisoft.keepass.icons.IconDrawableFactory;
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.UriUtil;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Database {

    private static final String TAG = Database.class.getName();

    private PwDatabase pwDatabase = null;
    private PwDatabaseV3 pwDatabaseV3 = null;
    private PwDatabaseV4 pwDatabaseV4 = null;
    private PwVersion version = null;

    private Uri mUri = null;
    private SearchDbHelper searchHelper = null;
    private boolean readOnly = false;
    private boolean passwordEncodingError = false;

    private IconDrawableFactory drawFactory = new IconDrawableFactory();

    public boolean loaded = false;

    public Database() {}

    public Database(String databasePath) {
        // TODO Test with kdb extension
        if (isKDBExtension(databasePath)) {
            this.pwDatabaseV3 = new PwDatabaseV3();

            this.pwDatabase = pwDatabaseV3;
        } else {
            PwGroupV4 groupV4 = new PwGroupV4();
            this.pwDatabaseV4 = new PwDatabaseV4();

            groupV4.setTitle(dbNameFromPath(databasePath));
            groupV4.setIconStandard(pwDatabaseV4.getIconFactory().getFolderIcon());
            this.pwDatabaseV4.setRootGroup(groupV4);

            this.pwDatabase = pwDatabaseV4;
        }
		this.version = pwDatabase.getVersion();
    }

    private void retrieveDatabaseVersioned(PwDatabase pwDatabase) {
        this.version = pwDatabase.getVersion();
        switch (version) {
            case V3:
                pwDatabaseV3 = (PwDatabaseV3) pwDatabase;
                break;
            case V4:
                pwDatabaseV4 = (PwDatabaseV4) pwDatabase;
                break;
        }
    }

    private boolean isKDBExtension(String filename) {
        if (filename == null) { return false; }
        int extIdx = filename.lastIndexOf(".");
        if (extIdx == -1) return false;
        return filename.substring(extIdx).equalsIgnoreCase(".kdb");
    }

    private String dbNameFromPath(String dbPath) {
        String filename = URLUtil.guessFileName(dbPath, null, null);
        if (EmptyUtils.isNullOrEmpty(filename)) {
            return "KeePass Database";
        }
        int lastExtDot = filename.lastIndexOf(".");
        if (lastExtDot == -1) {
            return filename;
        }
        return filename.substring(0, lastExtDot);
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

    public PwIconFactory getIconFactory() {
		return pwDatabase.getIconFactory();
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

        // Get the file directory to save the attachments
        Importer databaseImporter = ImporterFactory.createImporter(bis, ctx.getFilesDir(), debug);

        bis.reset();  // Return to the start

        pwDatabase = databaseImporter.openDatabase(bis, password, keyFileInputStream, progressTaskUpdater);
        retrieveDatabaseVersioned(pwDatabase);
        if ( pwDatabase != null ) {
            try {
                passwordEncodingError = !pwDatabase.validatePasswordEncoding(password);
                searchHelper = new SearchDbHelper(ctx);
                loaded = true;
            } catch (Exception e) {
                Log.e(TAG, "Load can't be performed with this Database version", e);
                loaded = false;
            }
        }
    }

    public PwGroupInterface search(String str) {
        return search(str, Integer.MAX_VALUE);
    }

    public PwGroupInterface search(String str, int max) {
        if (searchHelper == null)
            return null;
        return searchHelper.search(pwDatabase, str, max);
    }

    public Cursor searchEntry(String query) {
        switch (version) {
            case V3:
                EntryCursorV3 cursorV3 = new EntryCursorV3();
                if (!query.isEmpty()) {
                    PwGroupInterface searchResult = search(query, 6);
                    if (searchResult != null) {
                        for (PwEntryInterface entry: searchResult.getChildEntries()) {
                            if (!entry.isMetaStream()) { // TODO metastream
                                cursorV3.addEntry((PwEntryV3) entry);
                            }
                        }
                    }
                }
                return cursorV3;
            case V4:
                EntryCursorV4 cursorv4 = new EntryCursorV4();
                if (!query.isEmpty()) {
                    PwGroupInterface searchResult = search(query, 6);
                    if (searchResult != null) {
                        for (PwEntryInterface entry: searchResult.getChildEntries()) {
                            if (!entry.isMetaStream()) { // TODO metastream
                                cursorv4.addEntry((PwEntryV4) entry);
                            }
                        }
                    }
                }
                return cursorv4;
        }
        return null;
    }

    public PwEntryInterface getEntryFrom(Cursor cursor) {
        PwIconFactory iconFactory = pwDatabase.getIconFactory();
        PwEntryInterface pwEntry = createEntry();
        try {
            switch (version) {
                case V3:
                    ((EntryCursorV3) cursor).populateEntry((PwEntryV3) pwEntry, iconFactory);
                    break;
                case V4:
                    // TODO invert field reference manager
					startManageEntry(pwEntry);
                    ((EntryCursorV4) cursor).populateEntry((PwEntryV4) pwEntry, iconFactory);
                    stopManageEntry(pwEntry);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be populated", e);
        }
        return pwEntry;
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
                PwDbOutput pmo = PwDbOutput.getInstance(pwDatabase, fos);
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
                PwDbOutput pmo = PwDbOutput.getInstance(pwDatabase, os);
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

    // TODO Clear database when lock broadcast is receive in backstage
    public void closeAndClear(Context context) {
        drawFactory.clearCache();
        // Delete the cache of the database if present
        if (pwDatabaseV4 != null)
			pwDatabaseV4.clearCache();
        // In all cases, delete all the files in the temp dir
        try {
            FileUtils.cleanDirectory(context.getFilesDir());
        } catch (IOException e) {
            Log.e(TAG, "Unable to clear the directory cache.", e);
        }

        pwDatabase = null;
        pwDatabaseV3 = null;
        pwDatabaseV4 = null;
        mUri = null;
        loaded = false;
        passwordEncodingError = false;
    }

    public String getVersion() {
        return version.toString();
    }

    public boolean containsName() {
        switch (version) {
            default:
                return false;
            case V4:
                return true;
        }
    }

    public String getName() {
        switch (version) {
            default:
                return "";
            case V4:
                return pwDatabaseV4.getName();
        }
    }

    public void assignName(String name) {
        switch (version) {
            case V4:
                pwDatabaseV4.setName(name);
				pwDatabaseV4.setNameChanged(new PwDate());
                break;
        }
    }

    public boolean containsDescription() {
        switch (version) {
            default:
                return false;
            case V4:
                return true;
        }
    }

    public String getDescription() {
        switch (version) {
            default:
                return "";
            case V4:
                return pwDatabaseV4.getDescription();
        }
    }

    public void assignDescription(String description) {
        switch (version) {
            case V4:
				pwDatabaseV4.setDescription(description);
				pwDatabaseV4.setDescriptionChanged(new PwDate());
        }
    }

    public String getDefaultUsername() {
        switch (version) {
            default:
                return "";
            case V4:
                return pwDatabaseV4.getDefaultUserName();
        }
    }

    public void setDefaultUsername(String username) {
        switch (version) {
            case V4:
				pwDatabaseV4.setDefaultUserName(username);
				pwDatabaseV4.setDefaultUserNameChanged(new PwDate());
        }
    }

    public PwEncryptionAlgorithm getEncryptionAlgorithm() {
        return pwDatabase.getEncryptionAlgorithm();
    }

    public List<PwEncryptionAlgorithm> getAvailableEncryptionAlgorithms() {
        return pwDatabase.getAvailableEncryptionAlgorithms();
    }

    public boolean allowEncryptionAlgorithmModification() {
        return getAvailableEncryptionAlgorithms().size() > 1;
    }

    public void assignEncryptionAlgorithm(PwEncryptionAlgorithm algorithm) {
        switch (version) {
            case V4:
				pwDatabaseV4.setEncryptionAlgorithm(algorithm);
				pwDatabaseV4.setDataEngine(algorithm.getCipherEngine());
				pwDatabaseV4.setDataCipher(algorithm.getDataCipher());
        }
    }

    public String getEncryptionAlgorithmName(Resources resources) {
        return pwDatabase.getEncryptionAlgorithm().getName(resources);
    }

    public List<KdfEngine> getAvailableKdfEngines() {
        switch (version) {
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
        switch (version) {
            case V4:
                KdfEngine kdfEngine = pwDatabaseV4.getKdfEngine();
                if (kdfEngine == null)
                    return KdfFactory.aesKdf;
                return kdfEngine;
            default:
            case V3:
                return KdfFactory.aesKdf;
        }
    }

    public void assignKdfEngine(KdfEngine kdfEngine) {
        switch (version) {
            case V4:
                if (pwDatabaseV4.getKdfParameters() == null
                        || !pwDatabaseV4.getKdfParameters().getUUID().equals(kdfEngine.getDefaultParameters().getUUID()))
					pwDatabaseV4.setKdfParameters(kdfEngine.getDefaultParameters());
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
        return pwDatabase.getNumberKeyEncryptionRounds();
    }

    public void setNumberKeyEncryptionRounds(long numberRounds) throws NumberFormatException {
        pwDatabase.setNumberKeyEncryptionRounds(numberRounds);
    }

    public String getMemoryUsageAsString() {
        return Long.toString(getMemoryUsage());
    }

    public long getMemoryUsage() {
        switch (version) {
            case V4:
                return pwDatabaseV4.getMemoryUsage();
        }
        return KdfEngine.UNKNOW_VALUE;
    }

    public void setMemoryUsage(long memory) {
        switch (version) {
            case V4:
				pwDatabaseV4.setMemoryUsage(memory);
        }
    }

    public String getParallelismAsString() {
        return Integer.toString(getParallelism());
    }

    public int getParallelism() {
        switch (version) {
            case V4:
                return pwDatabaseV4.getParallelism();
        }
        return KdfEngine.UNKNOW_VALUE;
    }

    public void setParallelism(int parallelism) {
        switch (version) {
            case V4:
				pwDatabaseV4.setParallelism(parallelism);
        }
    }

    public boolean validatePasswordEncoding(String key) {
    	return pwDatabase.validatePasswordEncoding(key);
	}

	public byte[] getMasterKey() {
		return pwDatabase.getMasterKey();
	}

	public void setMasterKey(byte[] masterKey) {
		pwDatabase.masterKey = masterKey;
	}

	public void retrieveMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {
		pwDatabase.retrieveMasterKey(key, keyInputStream);
	}

	public PwGroupInterface getRootGroup() {
		return pwDatabase.getRootGroup();
	}

    public PwEntryInterface createEntry() {
        try {
            switch (version) {
                case V3:
                    return new PwEntryV3();
                case V4:
                    return new PwEntryV4();
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be created", e);
        }
        return null;
    }

    public PwGroupInterface createGroup() {
        PwGroupInterface newPwGroup = null;
        try {
            switch (version) {
                case V3:
                    newPwGroup = new PwGroupV3();
                case V4:
                    newPwGroup = new PwGroupV4();
            }
            newPwGroup.setNodeId(pwDatabase.newGroupId());
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be created", e);
        }
        return newPwGroup;
    }

	public PwEntryInterface getEntryById(PwNodeId id) {
		return pwDatabase.getEntryById(id);
	}

	public PwGroupInterface getGroupById(PwNodeId id) {
		return pwDatabase.getGroupById(id);
	}

    public void addEntryTo(PwEntryInterface entry, PwGroupInterface parent) {
        try {
            pwDatabase.addEntryTo(entry, parent);
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be added from this version of PwGroup", e);
        }
    }

    public void removeEntryFrom(PwEntryInterface entry, PwGroupInterface parent) {
        try {
            pwDatabase.removeEntryFrom(entry, parent);
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be removed from this version of PwGroup", e);
        }
    }

    public void addGroupTo(PwGroupInterface group, PwGroupInterface parent) {
        try {
            pwDatabase.addGroupTo(group, parent);
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be added in this version of PwGroup", e);
        }
    }

    public void removeGroupFrom(PwGroupInterface group, PwGroupInterface parent) {
        try {
           pwDatabase.removeGroupFrom(group, parent);
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be removed from this version of PwGroup", e);
        }
    }

    public void updateEntry(PwEntryInterface oldEntry, PwEntryInterface newEntry) {
        try {
            switch (version) {
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

    public void updateGroup(PwGroupInterface oldGroup, PwGroupInterface newGroup) {
        try {
            switch (version) {
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
    public @Nullable PwEntryInterface copyEntry(PwEntryInterface entryToCopy, PwGroupInterface newParent) {
        try {
            PwEntryInterface entryCopied = null;
            switch (version) {
                case V3:
                    entryCopied = entryToCopy.duplicate();
                    break;
                case V4:
                    entryCopied = entryToCopy.duplicate();
                    break;
            }
            entryCopied.setNodeId(new PwNodeIdUUID());
            entryCopied.setParent(newParent);
            addEntryTo(entryCopied, newParent);
            return entryCopied;
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be updated", e);
        }
        return null;
    }

    public void moveEntry(PwEntryInterface entryToMove, PwGroupInterface newParent) {
        removeEntryFrom(entryToMove, entryToMove.getParent());
        addEntryTo(entryToMove, newParent);
    }

    public void moveGroup(PwGroupInterface groupToMove, PwGroupInterface newParent) {
        removeGroupFrom(groupToMove, groupToMove.getParent());
        addGroupTo(groupToMove, newParent);
    }

    public void deleteEntry(PwEntryInterface entry) {
    	removeEntryFrom(entry, entry.getParent());
    }

    public void deleteGroup(PwGroupInterface group) {
		PwGroupInterface.doForEachChildAndForRoot(group,
				new EntryHandler<PwEntryInterface>() {
					@Override
					public boolean operate(PwEntryInterface entry) {
						deleteEntry(entry);
						return true;
					}
				},
				new GroupHandler<PwGroupInterface>() {
					@Override
					public boolean operate(PwGroupInterface group) {
						PwGroupInterface parent = group.getParent();
						removeGroupFrom(group, parent);
						return true;
					}
				});
    }

    public void undoDeleteEntry(PwEntryInterface entry, PwGroupInterface parent) {
        pwDatabase.undoDeleteEntryFrom(entry, parent);
    }

    public void undoDeleteGroup(PwGroupInterface group, PwGroupInterface parent) {
        pwDatabase.undoDeleteGroup(group, parent);
    }

	/**
	 * Determine if RecycleBin is available or not for this version of database
	 * @return true if RecycleBin available
	 */
    public boolean isRecycleBinAvailable() {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    return true;
            }
        }
		return false;
    }

    public boolean isRecycleBinEnabled() {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    return pwDatabaseV4.isRecycleBinEnabled();
            }
        }
		return false;
    }

    public PwGroupInterface getRecycleBin() {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    return pwDatabaseV4.getRecycleBin();
            }
        }
		return null;
	}

	public boolean canRecycle(PwEntryInterface entry) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    return pwDatabaseV4.canRecycle(entry);
            }
        }
		return false;
	}

	public boolean canRecycle(PwGroupInterface group) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    return pwDatabaseV4.canRecycle(group);
            }
        }
		return false;
	}

	public void recycle(PwEntryInterface entry) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    pwDatabaseV4.recycle(entry);
                    break;
            }
        }
	}

	public void recycle(PwGroupInterface group) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    pwDatabaseV4.recycle(group);
                    break;
            }
        }
	}

    public void undoRecycle(PwEntryInterface entry, PwGroupInterface parent) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    pwDatabaseV4.undoRecycle(entry, parent);
                    break;
            }
        }
    }

    public void undoRecycle(PwGroupInterface group, PwGroupInterface parent) {
        if (pwDatabaseV4 != null) {
            switch (version) {
                case V4:
                    pwDatabaseV4.undoRecycle(group, parent);
                    break;
            }
        }
    }

	public void startManageEntry(PwEntryInterface entry) {
    	if (pwDatabaseV4 != null) {
			switch (version) {
				case V4:
					((PwEntryV4) entry).startToManageFieldReferences(pwDatabaseV4);
					break;
			}
		}
	}

	public void stopManageEntry(PwEntryInterface entry) {
		if (pwDatabaseV4 != null) {
			switch (version) {
				case V4:
					((PwEntryV4) entry).stopToManageFieldReferences();
					break;
			}
		}
	}

	public void createBackupOf(PwEntryInterface entry) {
		if (pwDatabaseV4 != null) {
			switch (version) {
				case V4:
					((PwEntryV4) entry).createBackup(pwDatabaseV4);
					break;
			}
		}
	}
}
