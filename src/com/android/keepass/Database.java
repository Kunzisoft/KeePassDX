/*
 * Copyright 2009 Brian Pellin.
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
package com.android.keepass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;
import org.phoneid.keepassj2me.Types;

import android.content.Context;

import com.android.keepass.keepasslib.InvalidKeyFileException;
import com.android.keepass.keepasslib.PwManagerOutput;
import com.android.keepass.keepasslib.PwManagerOutputException;
import com.android.keepass.search.SearchDbHelper;

/**
 * @author bpellin
 *
 */
public class Database {
	public HashMap<Integer, WeakReference<PwGroup>> gGroups = new HashMap<Integer, WeakReference<PwGroup>>();
	public HashMap<UUID, WeakReference<PwEntry>> gEntries = new HashMap<UUID, WeakReference<PwEntry>>();
	public HashMap<PwGroup, WeakReference<PwGroup>> gDirty = new HashMap<PwGroup, WeakReference<PwGroup>>();
	public PwGroup gRoot;
	public PwManager mPM;
	public String mFilename;
	public SearchDbHelper searchHelper;
	
	public void LoadData(Context ctx, InputStream is, String password, String keyfile) throws InvalidCipherTextException, IOException, InvalidKeyFileException {
		LoadData(ctx, is, password, keyfile, !ImporterV3.DEBUG);
	}

	public void LoadData(Context ctx, String filename, String password, String keyfile) throws InvalidCipherTextException, IOException, InvalidKeyFileException, FileNotFoundException {
		LoadData(ctx, filename, password, keyfile, !ImporterV3.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, boolean debug) throws InvalidCipherTextException, IOException, InvalidKeyFileException, FileNotFoundException {
		FileInputStream fis;
		fis = new FileInputStream(filename);
		
		LoadData(ctx, fis, password, keyfile, debug);
	
		mFilename = filename;
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, boolean debug) throws InvalidCipherTextException, IOException, InvalidKeyFileException {
		ImporterV3 Importer = new ImporterV3(debug);
		
		mPM = Importer.openDatabase(is, password, keyfile);
		if ( mPM != null ) {
			mPM.constructTree(null);
			populateGlobals(null);
		}

		searchHelper = new SearchDbHelper(ctx);
		searchHelper.open();
		buildSearchIndex(ctx);
		
	}
	
	
	/** Build the search index from the current database
	 * @param ctx
	 */
	private void buildSearchIndex(Context ctx) {
		
		
		for ( int i = 0; i < mPM.entries.size(); i++) {
			PwEntry entry = mPM.entries.get(i);
			searchHelper.insertEntry(entry);
		}
	}
	
	public PwGroup Search(String str) {
		return searchHelper.search(this, str);
	}
	
	public void NewEntry(PwEntry entry) throws IOException, PwManagerOutputException {
		PwGroup parent = entry.parent;
		
		// Add entry to group
		parent.childEntries.add(entry);
		
		// Add entry to PwManager
		mPM.entries.add(entry);
		
		// Commit to disk
		try {
			SaveData();
		} catch (PwManagerOutputException e) {
			UndoNewEntry(entry);
			throw e;
		} catch (IOException e) {
			UndoNewEntry(entry);
			throw e;
		}
		
		// Mark parent group dirty
		gDirty.put(parent, new WeakReference<PwGroup>(parent));

		// Add entry to global
		gEntries.put(Types.bytestoUUID(entry.uuid), new WeakReference<PwEntry>(entry));
		
		// Add entry to search index
		searchHelper.insertEntry(entry);
	}
	
	public void UndoNewEntry(PwEntry entry) {
		// Remove from group
		entry.parent.childEntries.removeElement(entry);
		
		// Remove from manager
		mPM.entries.removeElement(entry);
	}
	
	public void UpdateEntry(PwEntry oldE, PwEntry newE) throws IOException, PwManagerOutputException {
		
		// Keep backup of original values in case save fails
		PwEntry backup = new PwEntry(oldE);
		
		// Update entry with new values
		oldE.assign(newE);
		
		try {
			SaveData();
		} catch (PwManagerOutputException e) {
			UndoUpdateEntry(oldE, backup);
			throw e;
		} catch (IOException e) {
			UndoUpdateEntry(oldE, backup);
			throw e;
		}

		// Mark group dirty if title changes
		if ( ! oldE.title.equals(newE.title) ) {
			PwGroup parent = oldE.parent;
			if ( parent != null ) {
				// Mark parent group dirty
				gDirty.put(parent, new WeakReference<PwGroup>(parent));
			}
		}
		
		// Update search index
		searchHelper.updateEntry(oldE);

	}
	
	public void UndoUpdateEntry(PwEntry old, PwEntry backup) {
		// If we fail to save, back out changes to global structure
		old.assign(backup);
	}
	
	public void SaveData() throws IOException, PwManagerOutputException {
		SaveData(mFilename);
	}
	
	public void SaveData(String filename) throws IOException, PwManagerOutputException {
		File tempFile = new File(filename + ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile);
		PwManagerOutput pmo = new PwManagerOutput(mPM, fos);
		pmo.output();
		fos.close();
		
		File orig = new File(filename);
		orig.delete();
		
		if ( ! tempFile.renameTo(orig) ) {
			throw new IOException("Failed to store database.");
		}
		
		mFilename = filename;
		
	}
	
	private void populateGlobals(PwGroup currentGroup) {
		if (currentGroup == null) {
			Vector<PwGroup> rootChildGroups = mPM.getGrpRoots();
			for (int i = 0; i < rootChildGroups.size(); i++ ){
				PwGroup cur = rootChildGroups.elementAt(i);
				gRoot = cur.parent;
				gGroups.put(cur.groupId, new WeakReference<PwGroup>(cur));
				populateGlobals(cur);
			}
			
			return;
		}
		
		Vector<PwGroup> childGroups = currentGroup.childGroups;
		Vector<PwEntry> childEntries = currentGroup.childEntries;
		
		for (int i = 0; i < childEntries.size(); i++ ) {
			PwEntry cur = childEntries.elementAt(i);
			gEntries.put(Types.bytestoUUID(cur.uuid), new WeakReference<PwEntry>(cur));
		}
		
		for (int i = 0; i < childGroups.size(); i++ ) {
			PwGroup cur = childGroups.elementAt(i);
			gGroups.put(cur.groupId, new WeakReference<PwGroup>(cur));
			populateGlobals(cur);
		}
	}
	
	public void clear() {
		if ( searchHelper != null ) {
			searchHelper.close();
			searchHelper = null;
		}
		gGroups.clear();
		gEntries.clear();
		gRoot = null;
		mPM = null;
		mFilename = null;
	}
}
