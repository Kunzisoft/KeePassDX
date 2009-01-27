/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
 */package com.android.keepass;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.bouncycastle1.crypto.InvalidCipherTextException;
import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

public class Database {
	public static HashMap<Integer, WeakReference<PwGroup>> gGroups = new HashMap<Integer, WeakReference<PwGroup>>();
	public static HashMap<UUID, WeakReference<PwEntry>> gEntries = new HashMap<UUID, WeakReference<PwEntry>>();
	public static PwGroup gRoot;
	private static PwManager mPM;
	
	public static int LoadData(String filename, String password) {
		FileInputStream fis;
		try {
			fis = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			return R.string.FileNotFound;
		}
		
		ImporterV3 Importer = new ImporterV3();
	
		try {
			mPM = Importer.openDatabase(fis, password);
			if ( mPM != null ) {
				mPM.constructTree(null);
				populateGlobals(null);
			}
		} catch (InvalidCipherTextException e) {
			return R.string.InvalidPassword;
		} catch (IOException e) {
			return -1;
		}
		
		return 0;
		
	}
	
	private static void populateGlobals(PwGroup currentGroup) {
		if (currentGroup == null) {
			Vector rootChildGroups = mPM.getGrpRoots();
			for (int i = 0; i < rootChildGroups.size(); i++ ){
				PwGroup cur = (PwGroup) rootChildGroups.elementAt(i);
				gRoot = cur.parent;
				gGroups.put(cur.groupId, new WeakReference<PwGroup>(cur));
				populateGlobals(cur);
				return;
			}
		}
		
		Vector childGroups = currentGroup.childGroups;
		Vector childEntries = currentGroup.childEntries;
		
		for (int i = 0; i < childEntries.size(); i++ ) {
			PwEntry cur = (PwEntry) childEntries.elementAt(i);
			gEntries.put(UUID.nameUUIDFromBytes(cur.uuid), new WeakReference<PwEntry>(cur));
		}
		
		for (int i = 0; i < childGroups.size(); i++ ) {
			PwGroup cur = (PwGroup) childGroups.elementAt(i);
			gGroups.put(cur.groupId, new WeakReference<PwGroup>(cur));
			populateGlobals(cur);
		}
	}
	
	public static void clear() {
		gGroups.clear();
		gEntries.clear();
		gRoot = null;
		mPM = null;
	}


}
