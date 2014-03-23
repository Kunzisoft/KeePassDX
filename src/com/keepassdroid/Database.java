/*
 * Copyright 2009-2014 Brian Pellin.
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
import java.io.SyncFailedException;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.icons.DrawableFactory;
import com.keepassdroid.search.SearchDbHelper;

/**
 * @author bpellin
 */
public class Database {
	public Set<PwGroup> dirty = new HashSet<PwGroup>();
	public PwDatabase pm;
	public String mFilename;
	public SearchDbHelper searchHelper;
	public boolean readOnly = false;
	
	public DrawableFactory drawFactory = new DrawableFactory();
	
	private boolean loaded = false;
	
	public boolean Loaded() {
		return loaded;
	}
	
	public void setLoaded() {
		loaded = true;
	}
	
	public void LoadData(Context ctx, InputStream is, String password, String keyfile) throws IOException, InvalidDBException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
	}

	public void LoadData(Context ctx, String filename, String password, String keyfile) throws IOException, FileNotFoundException, InvalidDBException {
		LoadData(ctx, filename, password, keyfile, new UpdateStatus(), !Importer.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status) throws IOException, FileNotFoundException, InvalidDBException {
		LoadData(ctx, filename, password, keyfile, status, !Importer.DEBUG);
	}
	
	public void LoadData(Context ctx, String filename, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, FileNotFoundException, InvalidDBException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		
		LoadData(ctx, fis, password, keyfile, status, debug);
	
		readOnly = !file.canWrite();
		mFilename = filename;
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, boolean debug) throws IOException, InvalidDBException {
		LoadData(ctx, is, password, keyfile, new UpdateStatus(), debug);
	}

	public void LoadData(Context ctx, InputStream is, String password, String keyfile, UpdateStatus status, boolean debug) throws IOException, InvalidDBException {

		BufferedInputStream bis = new BufferedInputStream(is);
		
		if ( ! bis.markSupported() ) {
			throw new IOException("Input stream does not support mark.");
		}
		
		// We'll end up reading 8 bytes to identify the header. Might as well use two extra.
		bis.mark(10);
		
		Importer imp = ImporterFactory.createImporter(bis, debug);

		bis.reset();  // Return to the start
		
		pm = imp.openDatabase(bis, password, keyfile, status);
		if ( pm != null ) {
			PwGroup root = pm.rootGroup;
			
			pm.populateGlobals(root);
		}
		
		searchHelper = new SearchDbHelper(ctx);
		
		loaded = true;
	}
	
	public PwGroup Search(String str) {
		if (searchHelper == null) { return null; }
		
		PwGroup group = searchHelper.search(this, str);
		
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
		
		// Force data to disk before continuing
		try {
			fos.getFD().sync();
		} catch (SyncFailedException e) {
			// Ignore if fsync fails. We tried.
		}
		
		File orig = new File(filename);
		
		if ( ! tempFile.renameTo(orig) ) {
			throw new IOException("Failed to store database.");
		}
		
		mFilename = filename;
		
	}
	
	public void clear() {
		dirty.clear();
		drawFactory.clear();
		
		pm = null;
		mFilename = null;
		loaded = false;
	}
	
	public void markAllGroupsAsDirty() {
		for ( PwGroup group : pm.getGroups() ) {
			dirty.add(group);
		}
		
		// TODO: This should probably be abstracted out
		// The root group in v3 is not an 'official' group
		if ( pm instanceof PwDatabaseV3 ) {
			dirty.add(pm.rootGroup);		
		}
	}
	
	
}
