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

import com.kunzisoft.keepass.database.security.ProtectedBinary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BinaryPool {
	private HashMap<Integer, ProtectedBinary> pool = new HashMap<>();
	
	public BinaryPool() {
	}
	
	public BinaryPool(PwGroupV4 rootGroup) {
		build(rootGroup);
	}

	public ProtectedBinary get(int key) {
		return pool.get(key);
	}
	
	public ProtectedBinary put(int key, ProtectedBinary value) {
		return pool.put(key, value);
	}


	public Set<Entry<Integer, ProtectedBinary>> entrySet() {
		return pool.entrySet();
	}

	public void clear() {
		for (Entry<Integer, ProtectedBinary> entry: pool.entrySet())
			entry.getValue().clear();
		pool.clear();
	}

	public Collection<ProtectedBinary> binaries() {
		return pool.values();
	}
	
	private class AddBinaries extends EntryHandler<PwEntryV4> {

		@Override
		public boolean operate(PwEntryV4 entry) {
			for (PwEntryV4 histEntry : entry.getHistory()) {
				add(histEntry.getBinaries());
			}
			add(entry.getBinaries());
			return true;
		}
		
	}
	
	private void add(Map<String, ProtectedBinary> dict) {
		for (ProtectedBinary pb : dict.values()) {
			add(pb);
		}
		
	}
	
	public void add(ProtectedBinary pb) {
        assert(pb != null);
		if (findKey(pb) != -1) return;
		
		pool.put(findUnusedKey(), pb);
	}

	public int findUnusedKey() {
	    int unusedKey = pool.size();
	    while(get(unusedKey) != null)
            unusedKey++;
	    return unusedKey;
    }
	
	public int findKey(ProtectedBinary pb) {
		for (Entry<Integer, ProtectedBinary> pair : pool.entrySet()) {
			if (pair.getValue().equals(pb)) return pair.getKey();
		}
		
		return -1;
	}
	
	private void build(PwGroupV4 rootGroup) {
		EntryHandler eh = new AddBinaries();
		rootGroup.preOrderTraverseTree(null, eh);
	}
}
