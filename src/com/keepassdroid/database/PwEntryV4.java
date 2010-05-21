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

import java.util.Date;

import org.w3c.dom.Node;

public class PwEntryV4 extends PwEntry {
	private Node node;
	
	public PwEntryV4(Node n) {
		node = n;
	}

	@Override
	public void assign(PwEntry source) {
		super.assign(source);
		
		if ( ! (source instanceof PwEntryV4) ) {
			throw new RuntimeException("DB version mix.");
		}
		
		PwEntryV4 src = (PwEntryV4) source;
		assign(src);
	}
	
	private void assign(PwEntryV4 source) {
		node = source.node;
	}

	@Override
	public Object clone() {
		PwEntryV4 newEntry = (PwEntryV4) super.clone();
		
		return newEntry;
	}

	@Override
	public void stampLastAccess() {
		// TODO Implement me
	}

	@Override
	public String getUsername() {
		// TODO Implement me
		return null;
	}

	@Override
	public String getPassword() {
		// TODO Implement me
		return null;
	}

	@Override
	public Date getAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getExpire() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getMod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayTitle() {
		// TOOD: Add special TAN handling for V4?
		return title;
	}

	@Override
	public PwGroupV4 getParent() {
		// TODO Auto-generated method stub
		return null;
	}

}
