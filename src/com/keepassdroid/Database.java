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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import android.content.Context;
import android.os.Debug;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.exception.Kdb4Exception;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.search.SearchDbHelper;

/**
 * @author bpellin
 */
public class Database {
	public HashMap<PwGroupId, WeakReference<PwGroup>> groups = new HashMap<PwGroupId, WeakReference<PwGroup>>();
	public HashMap<UUID, WeakReference<PwEntry>> entries = new HashMap<UUID, WeakReference<PwEntry>>();
	public HashMap<PwGroup, WeakReference<PwGroup>> dirty = new HashMap<PwGroup, WeakReference<PwGroup>>();
	public PwGroup root;
	public PwDatabase pm;
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
	
	public void LoadData(Context ctx, InputStream is, String password, String keyfile) throws IOException, InvalidKeyFileException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
	}

	public void LoadData(Context ctx, String filename, String password, String keyfile) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {
		LoadData(ctx, filename, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {
		LoadData(ctx, filename, password, keyfile, status, !Importer.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, InvalidKeyFileException, FileNotFoundException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {
		FileInputStream fis;
		fis = new FileInputStream(filename);
		
		LoadData(ctx, fis, password, keyfile, status, debug);
	
		mFilename = filename;
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, boolean debug) throws IOException, InvalidKeyFileException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), debug);
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, InvalidKeyFileException, InvalidPasswordException, InvalidDBSignatureException, Kdb4Exception, InvalidDBVersionException {

		BufferedInputStream bis = new BufferedInputStream(is);
		
		if ( ! bis.markSupported() ) {
			throw new IOException("Input stream does not support mark.");
		}
		
		// We'll end up reading 8 bytes to identify the header. Might as well use two extra.
		bis.mark(10);
		
		Importer imp = ImporterFactory.createImporter(bis, debug);


		/*
		ImporterV3 Importer;
		Importer = (ImporterV3) imp;  // Remove me when V4 support is in
		*/
		
		bis.reset();  // Return to the start
		
		pm = imp.openDatabase(bis, password, keyfile, status);
		if ( pm != null ) {
			root = pm.rootGroup;
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
		searchHelper.insertEntry(pm.getEntries());
		/*for ( int i = 0; i < pm.entries.size(); i++) {
			PwEntryV3 entry = pm.entries.get(i);
			if ( ! entry.isMetaStream() ) {
				searchHelper.insertEntry(entry);
			}
		} */
		searchHelper.close();
		
		indexBuilt = true;
		Debug.stopMethodTracing();
	}
	
	public PwGroupV3 Search(String str) {
		searchHelper.open();
		PwGroupV3 group = searchHelper.search(this, str);
		searchHelper.close();
		
		return group;
		
	}
	
	public void SaveData() throws IOException, PwDbOutputException {
		SaveData(mFilename);
	}
	
	public void SaveData(String filename) throws IOException, PwDbOutputException {
		File tempFile = new File(filename + ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile);
		//BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		//PwDbV3Output pmo = new PwDbV3Output(pm, bos, App.getCalendar());
		PwDbOutput pmo = PwDbOutput.getInstance(pm, fos);
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
			Vector<? extends PwGroup> rootChildGroups = pm.getGrpRoots();
			for (int i = 0; i < rootChildGroups.size(); i++ ){
				PwGroup cur = rootChildGroups.elementAt(i);
				groups.put(cur.getId(), new WeakReference<PwGroup>(cur));
				populateGlobals(cur);
			}
			
			return;
		}
		
		List<PwGroup> childGroups = currentGroup.childGroups;
		List<PwEntry> childEntries = currentGroup.childEntries;
		
		for (int i = 0; i < childEntries.size(); i++ ) {
			PwEntry cur = childEntries.get(i);
			entries.put(cur.getUUID(), new WeakReference<PwEntry>(cur));
		}
		
		for (int i = 0; i < childGroups.size(); i++ ) {
			PwGroup cur = childGroups.get(i);
			groups.put(cur.getId(), new WeakReference<PwGroup>(cur));
			populateGlobals(cur);
		}
	}
	
	public void clear() {
		initSearch();
		
		indexBuilt = false;
		groups.clear();
		entries.clear();
		root = null;
		pm = null;
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
