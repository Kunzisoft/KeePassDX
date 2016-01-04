/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.keepassdroid.database.security.ProtectedBinary;

public class BinaryPool {
	private HashMap<String, ProtectedBinary> pool = new HashMap<String, ProtectedBinary>();
	
	public BinaryPool() {
		
	}
	
	public BinaryPool(PwGroupV4 rootGroup) {
		build(rootGroup);
	}

	public ProtectedBinary get(String key) {
		return pool.get(key);
	}
	
	public ProtectedBinary put(String key, ProtectedBinary value) {
		return pool.put(key, value);
	}
	
	public Set<Entry<String, ProtectedBinary>> entrySet() {
		return pool.entrySet();
	}
	
	private class AddBinaries extends EntryHandler<PwEntryV4> {

		@Override
		public boolean operate(PwEntryV4 entry) {
			for (PwEntryV4 histEntry : entry.history) {
				poolAdd(histEntry.binaries);
				
			}
			
			poolAdd(entry.binaries);
			return true;
		}
		
	}
	
	private void poolAdd(Map<String, ProtectedBinary> dict) {
		for (ProtectedBinary pb : dict.values()) {
			poolAdd(pb);
		}
		
	}
	
	private void poolAdd(ProtectedBinary pb) {
		assert(pb != null);
		
		if (poolFind(pb) != null) return;
		
		pool.put(String.valueOf(pool.size()), pb);
	}
	
	public String poolFind(ProtectedBinary pb) {
		for (Entry<String, ProtectedBinary> pair : pool.entrySet()) {
			if (pair.getValue().equals(pb)) return pair.getKey();
		}
		
		return null;
	}
	
	private void build(PwGroupV4 rootGroup) {
		EntryHandler eh = new AddBinaries();
		rootGroup.preOrderTraverseTree(null, eh);
	}
}
