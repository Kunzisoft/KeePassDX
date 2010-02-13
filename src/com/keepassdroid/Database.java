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
package com.keepassdroid;

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

import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;
import org.phoneid.keepassj2me.Types;

import android.content.Context;
import android.os.Debug;

import com.keepassdroid.keepasslib.InvalidKeyFileException;
import com.keepassdroid.keepasslib.InvalidPasswordException;
import com.keepassdroid.keepasslib.PwManagerOutput;
import com.keepassdroid.keepasslib.PwManagerOutputException;
import com.keepassdroid.search.SearchDbHelper;

/**
 * @author bpellin
 */
public class Database {
	public HashMap<Integer, WeakReference<PwGroup>> gGroups = new HashMap<Integer, WeakReference<PwGroup>>();
	public HashMap<UUID, WeakReference<PwEntry>> gEntries = new HashMap<UUID, WeakReference<PwEntry>>();
	public HashMap<PwGroup, WeakReference<PwGroup>> gDirty = new HashMap<PwGroup, WeakReference<PwGroup>>();
	public PwGroup gRoot;
	public PwManager mPM;
	public String mFilename;
	public SearchDbHelper searchHelper;
	public boolean indexBuilt = false;
	
	private boolean loaded = false;
	
	public boolean Loaded() {
		return loaded;
	}
	
	public void setLoaded() {
		loaded = true;
	}
	
	public void LoadData(Context ctx, InputStream is, String password, String keyfile) throws IOException, InvalidKeyFileException, InvalidPasswordException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), !ImporterV3.DEBUG);
	}

	public void LoadData(Context ctx, String filename, String password, String keyfile) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException {
		LoadData(ctx, filename, password, keyfile, new UpdateStatus(), !ImporterV3.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException {
		LoadData(ctx, filename, password, keyfile, status, !ImporterV3.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException {
		FileInputStream fis;
		fis = new FileInputStream(filename);
		
		LoadData(ctx, fis, password, keyfile, status, debug);
	
		mFilename = filename;
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, boolean debug) throws IOException, InvalidKeyFileException, InvalidPasswordException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), debug);
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, InvalidKeyFileException, InvalidPasswordException {
		ImporterV3 Importer = new ImporterV3(debug);
		
		mPM = Importer.openDatabase(is, password, keyfile, status);
		if ( mPM != null ) {
			mPM.constructTree(null);
			populateGlobals(null);
		}
		
		loaded = true;
	}
	
	
	/** Build the search index from the current database
	 * @param ctx (this should be an App context not an activity constant to avoid leaks)
	 */
	public void buildSearchIndex(Context ctx) {

		Debug.startMethodTracing("search");
		searchHelper = new SearchDbHelper(ctx);
		
		initSearch();
		
		searchHelper.open();
		searchHelper.insertEntry(mPM.entries);
		/*for ( int i = 0; i < mPM.entries.size(); i++) {
			PwEntry entry = mPM.entries.get(i);
			if ( ! entry.isMetaStream() ) {
				searchHelper.insertEntry(entry);
			}
		} */
		searchHelper.close();
		
		indexBuilt = true;
		Debug.stopMethodTracing();
	}
	
	public PwGroup Search(String str) {
		searchHelper.open();
		PwGroup group = searchHelper.search(this, str);
		searchHelper.close();
		
		return group;
		
	}
	
	public void SaveData() throws IOException, PwManagerOutputException {
		SaveData(mFilename);
	}
	
	public void SaveData(String filename) throws IOException, PwManagerOutputException {
		File tempFile = new File(filename + ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile);
		//BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		//PwManagerOutput pmo = new PwManagerOutput(mPM, bos, App.getCalendar());
		PwManagerOutput pmo = new PwManagerOutput(mPM, fos);
		pmo.output();
		//bos.flush();
		//bos.close();
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
		initSearch();
		
		indexBuilt = false;
		gGroups.clear();
		gEntries.clear();
		gRoot = null;
		mPM = null;
		mFilename = null;
		loaded = false;
	}
	
	public void initSearch() {
		if ( searchHelper != null ) {
			searchHelper.open();
			searchHelper.clear();
			searchHelper.close();
		}
	}
	
}
