/*
 * Copyright 2009 Brian Pellin.

This file was derived from

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

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
import java.util.Vector;



/**
 * @author Brian Pellin <bpellin@gmail.com>
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwGroupV3 extends PwGroup {
  public PwGroupV3() {
  }

	public String toString() {
		return name;
	}

	public static final Date NEVER_EXPIRE = PwEntryV3.NEVER_EXPIRE;
	
	/** Size of byte buffer needed to hold this struct. */
	public static final int BUF_SIZE = 124;

	// for tree traversing
	public PwGroupV3 parent = null;

	public int groupId;
	public int imageId;
	public String name;

	public PwDate tCreation;
	public PwDate tLastMod;
	public PwDate tLastAccess;
	public PwDate tExpire;

	public int level; // short

	/** Used by KeePass internally, don't use */
	public int flags;
	
	public void setGroups(Vector<PwGroup> groups) {
		childGroups = groups;
	}
	
	@Override
	public PwGroup getParent() {
		return parent;
	}

	@Override
	public PwGroupId getId() {
		return new PwGroupIdV3(groupId);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Date getLastMod() {
		return tLastMod.getJDate();
	}

}
