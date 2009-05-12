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
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import com.android.keepass.keepasslib.InvalidKeyFileException;
import com.android.keepass.keepasslib.PwManagerOutput;
import com.android.keepass.keepasslib.PwManagerOutput.PwManagerOutputException;

public class Database {
	public static HashMap<Integer, WeakReference<PwGroup>> gGroups = new HashMap<Integer, WeakReference<PwGroup>>();
	public static HashMap<UUID, WeakReference<PwEntry>> gEntries = new HashMap<UUID, WeakReference<PwEntry>>();
	public static PwGroup gRoot;
	public static PwManager mPM;
	public static String mFilename;
	
	public static void LoadData(String filename, String password, String keyfile) throws InvalidCipherTextException, IOException, InvalidKeyFileException, FileNotFoundException {
		FileInputStream fis;
		fis = new FileInputStream(filename);
		
		ImporterV3 Importer = new ImporterV3();
		
		mPM = Importer.openDatabase(fis, password, keyfile);
		if ( mPM != null ) {
			mPM.constructTree(null);
			populateGlobals(null);
		}
		
		mFilename = filename;
	}
	
	public static void UpdateEntry(PwEntry oldE, PwEntry newE) throws IOException, PwManagerOutputException {
		oldE.assign(newE);
		
		SaveData();
	}
	
	public static void SaveData() throws IOException, PwManagerOutputException {
		SaveData(mFilename);
	}
	
	public static void SaveData(String filename) throws IOException, PwManagerOutputException {
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
	
	private static void populateGlobals(PwGroup currentGroup) {
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
			gEntries.put(UUID.nameUUIDFromBytes(cur.uuid), new WeakReference<PwEntry>(cur));
		}
		
		for (int i = 0; i < childGroups.size(); i++ ) {
			PwGroup cur = childGroups.elementAt(i);
			gGroups.put(cur.groupId, new WeakReference<PwGroup>(cur));
			populateGlobals(cur);
		}
	}
	
	public static void clear() {
		gGroups.clear();
		gEntries.clear();
		gRoot = null;
		mPM = null;
		mFilename = null;
	}


}
