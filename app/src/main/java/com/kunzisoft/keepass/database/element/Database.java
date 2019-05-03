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
import com.kunzisoft.keepass.database.NodeHandler;
import com.kunzisoft.keepass.database.cursor.EntryCursorV3;
import com.kunzisoft.keepass.database.cursor.EntryCursorV4;
import com.kunzisoft.keepass.database.exception.*;
import com.kunzisoft.keepass.database.load.ImporterV3;
import com.kunzisoft.keepass.database.load.ImporterV4;
import com.kunzisoft.keepass.database.save.PwDbOutput;
import com.kunzisoft.keepass.database.search.SearchDbHelper;
import com.kunzisoft.keepass.icons.IconDrawableFactory;
import com.kunzisoft.keepass.stream.LEDataInputStream;
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
    private PwVersion version = null;
    // To keep a reference for specific methods provided by version
    private PwDatabaseV3 pwDatabaseV3 = null;
    private PwDatabaseV4 pwDatabaseV4 = null;

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
            setDatabaseV3(new PwDatabaseV3());
        } else {
            PwGroupV4 groupV4 = new PwGroupV4();
            setDatabaseV4(new PwDatabaseV4());

            groupV4.setTitle(dbNameFromPath(databasePath));
            groupV4.setIconStandard(pwDatabaseV4.getIconFactory().getFolderIcon());
            this.pwDatabaseV4.setRootGroup(groupV4);
        }
    }

    private void setDatabaseV3(PwDatabaseV3 pwDatabaseV3) {
        this.pwDatabaseV3 = pwDatabaseV3;
        this.pwDatabaseV4 = null;
        this.pwDatabase = pwDatabaseV3;
        this.version = pwDatabase.getVersion();
    }

    private void setDatabaseV4(PwDatabaseV4 pwDatabaseV4) {
        this.pwDatabaseV3 = null;
        this.pwDatabaseV4 = pwDatabaseV4;
        this.pwDatabase = pwDatabaseV4;
        this.version = pwDatabase.getVersion();
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

    public void loadData(Context ctx, Uri uri, String password, Uri keyfile, ProgressTaskUpdater progressTaskUpdater)
            throws IOException, InvalidDBException {

        mUri = uri;
        readOnly = false;
        if (uri.getScheme().equals("file")) {
            File file = new File(uri.getPath());
            readOnly = !file.canWrite();
        }

        // Pass Uris as InputStreams

        InputStream inputStream, keyFileInputStream;
        try {
            inputStream = UriUtil.getUriInputStream(ctx, uri);
        } catch (Exception e) {
            Log.e("KPD", "Database::loadData", e);
            throw ContentFileNotFoundException.getInstance(uri);
        }

        try {
            keyFileInputStream = UriUtil.getUriInputStream(ctx, keyfile);
        } catch (Exception e) {
            Log.e("KPD", "Database::loadData", e);
            throw ContentFileNotFoundException.getInstance(keyfile);
        }

        // Load Data

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        if ( ! bufferedInputStream.markSupported() ) {
            throw new IOException("Input stream does not support mark.");
        }

        // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
        bufferedInputStream.mark(10);

        // Get the file directory to save the attachments
        int sig1 = LEDataInputStream.readInt(bufferedInputStream);
        int sig2 = LEDataInputStream.readInt(bufferedInputStream);

        bufferedInputStream.reset();  // Return to the start
        // Header of database V3
        if ( PwDbHeaderV3.matchesHeader(sig1, sig2) ) {
            setDatabaseV3(new ImporterV3().openDatabase(bufferedInputStream,
                    password,
                    keyFileInputStream,
                    progressTaskUpdater));
        }

        // Header of database V4
        else if ( PwDbHeaderV4.matchesHeader(sig1, sig2) ) {
            setDatabaseV4(new ImporterV4(ctx.getFilesDir()).openDatabase(bufferedInputStream,
                    password,
                    keyFileInputStream,
                    progressTaskUpdater));
        }

        // Header not recognized
        else {
            throw new InvalidDBSignatureException();
        }

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

    public Boolean isGroupSearchable(GroupVersioned group, Boolean isOmitBackup) {
        switch (version) {
            case V3:
                return pwDatabaseV3.isGroupSearchable(group.getPwGroupV3(), isOmitBackup);
            case V4:
                return pwDatabaseV4.isGroupSearchable(group.getPwGroupV4(), isOmitBackup);
        }
        return false;
    }

    public GroupVersioned search(String str) {
        return search(str, Integer.MAX_VALUE);
    }

    public GroupVersioned search(String str, int max) {
        if (searchHelper == null)
            return null;
        return searchHelper.search(this, str, max);
    }

    public Cursor searchEntry(String query) {
        switch (version) {
            case V3:
                EntryCursorV3 cursorV3 = new EntryCursorV3();
                if (!query.isEmpty()) {
                    GroupVersioned searchResult = search(query, 6);
                    if (searchResult != null) {
                        for (EntryVersioned entry: searchResult.getChildEntries()) {
                            if (!entry.isMetaStream()) { // TODO metastream
                                cursorV3.addEntry(entry.getPwEntryV3());
                            }
                        }
                    }
                }
                return cursorV3;
            case V4:
                EntryCursorV4 cursorv4 = new EntryCursorV4();
                if (!query.isEmpty()) {
                    GroupVersioned searchResult = search(query, 6);
                    if (searchResult != null) {
                        for (EntryVersioned entry: searchResult.getChildEntries()) {
                            if (!entry.isMetaStream()) { // TODO metastream
                                cursorv4.addEntry(entry.getPwEntryV4());
                            }
                        }
                    }
                }
                return cursorv4;
        }
        return null;
    }

    public EntryVersioned getEntryFrom(Cursor cursor) {
        PwIconFactory iconFactory = pwDatabase.getIconFactory();
        EntryVersioned entry = createEntry();
        try {
            switch (version) {
                case V3:
                    ((EntryCursorV3) cursor).populateEntry(entry.getPwEntryV3(), iconFactory);
                    break;
                case V4:
                    // TODO invert field reference manager
					startManageEntry(entry);
                    ((EntryCursorV4) cursor).populateEntry(entry.getPwEntryV4(), iconFactory);
                    stopManageEntry(entry);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be populated", e);
        }
        return entry;
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

	public GroupVersioned getRootGroup() {
        switch (version) {
            case V3:
                return new GroupVersioned(pwDatabaseV3.getRootGroup());
            case V4:
                return new GroupVersioned(pwDatabaseV4.getRootGroup());
        }
        return null;
	}

    public EntryVersioned createEntry() {
        EntryVersioned newEntry = null;
        try {
            switch (version) {
                case V3:
                    newEntry = new EntryVersioned(new PwEntryV3());
                    newEntry.setNodeId(pwDatabaseV3.newEntryId());

                case V4:
                    newEntry = new EntryVersioned(new PwEntryV4());
                    newEntry.setNodeId(pwDatabaseV4.newEntryId());
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwEntry can't be created", e);
        }
        return newEntry;
    }

    public GroupVersioned createGroup() {
        GroupVersioned newPwGroup = null;
        try {
            switch (version) {
                case V3:
                    newPwGroup = new GroupVersioned(new PwGroupV3());
                    newPwGroup.setNodeId(pwDatabaseV3.newGroupId());

                case V4:
                    newPwGroup = new GroupVersioned(new PwGroupV4());
                    newPwGroup.setNodeId(pwDatabaseV4.newGroupId());
            }
        } catch (Exception e) {
            Log.e(TAG, "This version of PwGroup can't be created", e);
        }
        return newPwGroup;
    }

	public EntryVersioned getEntryById(PwNodeId id) {
        PwEntryV3 entryV3 = pwDatabaseV3.getEntryById(id);
        if (entryV3 != null)
            return new EntryVersioned(entryV3);

        PwEntryV4 entryV4 = pwDatabaseV4.getEntryById(id);
        if (entryV4 != null)
            return new EntryVersioned(entryV4);

        return null;
	}

	public GroupVersioned getGroupById(PwNodeId id) {
        if (pwDatabaseV3 != null) {
            PwGroupV3 groupV3 = pwDatabaseV3.getGroupById(id);
            if (groupV3 != null)
                return new GroupVersioned(groupV3);
        }

        if (pwDatabaseV4 != null) {
            PwGroupV4 groupV4 = pwDatabaseV4.getGroupById(id);
            if (groupV4 != null)
                return new GroupVersioned(groupV4);
        }

		return null;
	}

    public void addEntryTo(EntryVersioned entry, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.addEntryTo(entry.getPwEntryV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.addEntryTo(entry.getPwEntryV4(), parent.getPwGroupV4());
        }
    }

    public void removeEntryFrom(EntryVersioned entry, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.removeEntryFrom(entry.getPwEntryV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.removeEntryFrom(entry.getPwEntryV4(), parent.getPwGroupV4());
        }
    }

    public void addGroupTo(GroupVersioned group, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.addGroupTo(group.getPwGroupV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.addGroupTo(group.getPwGroupV4(), parent.getPwGroupV4());
        }
    }

    public void removeGroupFrom(GroupVersioned group, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.removeGroupFrom(group.getPwGroupV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.removeGroupFrom(group.getPwGroupV4(), parent.getPwGroupV4());
        }
    }

    /**
     * @return A duplicate entry with the same values, a new UUID,
     * @param entryToCopy
     * @param newParent
     */
    public @Nullable EntryVersioned copyEntry(EntryVersioned entryToCopy, GroupVersioned newParent) {
        try {
            EntryVersioned entryCopied = null;
            switch (version) {
                case V3:
                    entryCopied = new EntryVersioned(entryToCopy);
                    break;
                case V4:
                    entryCopied = new EntryVersioned(entryToCopy);
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

    public void moveEntry(EntryVersioned entryToMove, GroupVersioned newParent) {
        removeEntryFrom(entryToMove, entryToMove.getParent());
        addEntryTo(entryToMove, newParent);
    }

    public void moveGroup(GroupVersioned groupToMove, GroupVersioned newParent) {
        removeGroupFrom(groupToMove, groupToMove.getParent());
        addGroupTo(groupToMove, newParent);
    }

    public void deleteEntry(EntryVersioned entry) {
    	removeEntryFrom(entry, entry.getParent());
    }

    public void deleteGroup(GroupVersioned group) {
        group.doForEachChildAndForIt(
				new NodeHandler<EntryVersioned>() {
					@Override
					public boolean operate(EntryVersioned entry) {
						deleteEntry(entry);
						return true;
					}
				},
				new NodeHandler<GroupVersioned>() {
					@Override
					public boolean operate(GroupVersioned group) {
						GroupVersioned parent = group.getParent();
						removeGroupFrom(group, parent);
						return true;
					}
				});
    }

    public void undoDeleteEntry(EntryVersioned entry, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.undoDeleteEntryFrom(entry.getPwEntryV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.undoDeleteEntryFrom(entry.getPwEntryV4(), parent.getPwGroupV4());
        }
    }

    public void undoDeleteGroup(GroupVersioned group, GroupVersioned parent) {
        switch (version) {
            case V3:
                pwDatabaseV3.undoDeleteGroupFrom(group.getPwGroupV3(), parent.getPwGroupV3());
            case V4:
                pwDatabaseV4.undoDeleteGroupFrom(group.getPwGroupV4(), parent.getPwGroupV4());
        }
    }

	/**
	 * Determine if RecycleBin is available or not for this version of database
	 * @return true if RecycleBin available
	 */
    public boolean isRecycleBinAvailable() {
        return pwDatabaseV4 != null;
    }

    public boolean isRecycleBinEnabled() {
        if (pwDatabaseV4 != null) {
            return pwDatabaseV4.isRecycleBinEnabled();
        }
		return false;
    }

    public GroupVersioned getRecycleBin() {
        if (pwDatabaseV4 != null) {
            return new GroupVersioned(pwDatabaseV4.getRecycleBin());
        }
		return null;
	}

	public boolean canRecycle(EntryVersioned entry) {
        if (pwDatabaseV4 != null) {
            return pwDatabaseV4.canRecycle(entry.getPwEntryV4());
        }
		return false;
	}

	public boolean canRecycle(GroupVersioned group) {
        if (pwDatabaseV4 != null) {
            return pwDatabaseV4.canRecycle(group.getPwGroupV4());
        }
		return false;
	}

	public void recycle(EntryVersioned entry) {
        if (pwDatabaseV4 != null) {
            pwDatabaseV4.recycle(entry.getPwEntryV4());
        }
	}

	public void recycle(GroupVersioned group) {
        if (pwDatabaseV4 != null) {
            pwDatabaseV4.recycle(group.getPwGroupV4());
        }
	}

    public void undoRecycle(EntryVersioned entry, GroupVersioned parent) {
        if (pwDatabaseV4 != null) {
            pwDatabaseV4.undoRecycle(entry.getPwEntryV4(), parent.getPwGroupV4());
        }
    }

    public void undoRecycle(GroupVersioned group, GroupVersioned parent) {
        if (pwDatabaseV4 != null) {
            pwDatabaseV4.undoRecycle(group.getPwGroupV4(), parent.getPwGroupV4());
        }
    }

	public void startManageEntry(EntryVersioned entry) {
    	if (pwDatabaseV4 != null) {
    	    entry.startToManageFieldReferences(pwDatabaseV4);
		}
	}

	public void stopManageEntry(EntryVersioned entry) {
		if (pwDatabaseV4 != null) {
		    entry.stopToManageFieldReferences();
		}
	}

	public void createBackupOf(EntryVersioned entry) {
		if (pwDatabaseV4 != null) {
		    entry.createBackup(pwDatabaseV4);
		}
	}
}
