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

import java.util.UUID;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.keepassdroid.database.exception.InconsistentDBException;

public class PwGroupV4 extends PwGroup {

	private Node node;
	private PwGroup parent;
	
	public PwGroupV4(Node n) throws InconsistentDBException {
		this(n, null);
	}

	public PwGroupV4(Node n, PwGroup p) throws InconsistentDBException {
		node = n;
		parent = p;
		buildTree();
	}
	
	private void buildTree() throws InconsistentDBException {
		NodeList children = node.getChildNodes();
		
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i);
			String name = child.getNodeName();
			if ( name.equalsIgnoreCase("Group") ) {
				PwGroupV4 group = new PwGroupV4(child, this);
				childGroups.add(group);
			} else if ( name.equalsIgnoreCase("Entry") ) {
				PwEntryV4 entry = new PwEntryV4(child);
				childEntries.add(entry);
			}
		}
	}

	@Override
	public PwGroup getParent() {
		return parent;
	}
	
	public void buildChildGroupsRecursive(Vector<PwGroup> list) {
		list.add(this);
		
		for ( int i = 0; i < childGroups.size(); i++) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildGroupsRecursive(list);
			
		}
	}

	public void buildChildEntriesRecursive(Vector<PwEntry> list) {
		for ( int i = 0; i < childEntries.size(); i++ ) {
			list.add(childEntries.get(i));
		}
		
		for ( int i = 0; i < childGroups.size(); i++ ) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildEntriesRecursive(list);
		}
		
	}

	@Override
	public PwGroupId getId() {
		return new PwGroupIdV4(getUUID());
	}

	public UUID getUUID() {
		// TODO: Get UUID from document
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
