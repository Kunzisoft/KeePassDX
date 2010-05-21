/*
 * Copyright 2010 Brian Pellin.
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.keepassdroid.database.exception.InconsistentDBException;
import com.keepassdroid.database.exception.InvalidKeyFileException;


public class PwDatabaseV4 extends PwDatabase {

	public UUID dataCipher;
	public PwCompressionAlgorithm compressionAlgorithm;
    public long numKeyEncRounds;
    private Document doc;
    
    //private Vector<PwGroupV4> groups = new Vector<PwGroupV4>();
    private PwGroupV4 rootGroup;
    
	@Override
	public byte[] getMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
		assert( key != null && keyFileName != null );
		
		byte[] fKey;
		
		if ( key.length() > 0 && keyFileName.length() > 0 ) {
			return getCompositeKey(key, keyFileName);
		} else if ( key.length() > 0 ) {
			fKey =  getPasswordKey(key);
		} else if ( keyFileName.length() > 0 ) {
			fKey = getFileKey(keyFileName);
		} else {
			throw new IllegalArgumentException( "Key cannot be empty." );
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

    @Override
	public byte[] getPasswordKey(String key) throws IOException {
		return getPasswordKey(key, "UTF-8");
	}

	@Override
	public Vector<PwGroup> getGroups() {
		Vector<PwGroup> list = new Vector<PwGroup>();
		rootGroup.buildChildGroupsRecursive(list);
		
		return list;
	}

	public void parseDB(Document d) throws InconsistentDBException {
		doc = d;
	
		NodeList list = doc.getElementsByTagName("Root");
		
		int len = list.getLength();
		if ( len < 0 || len > 1 ) {
			throw new InconsistentDBException("Missing root node");
		}
		
		Node root = list.item(1);
		
		rootGroup = new PwGroupV4(root);
	}

	@Override
	public Vector<PwGroup> getGrpRoots() {
		return rootGroup.childGroups;
	}

	@Override
	public Vector<PwEntry> getEntries() {
		Vector<PwEntry> list = new Vector<PwEntry>();
		rootGroup.buildChildEntriesRecursive(list);
		
		return list;
	}

	@Override
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRonuds(long rounds) throws NumberFormatException {
		numKeyEncRounds = rounds;
		
	}

}
