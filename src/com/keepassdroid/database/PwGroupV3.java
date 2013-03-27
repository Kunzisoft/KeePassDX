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

import java.security.acl.LastOwnerException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;



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

	public PwDate tCreation;
	public PwDate tLastMod;
	public PwDate tLastAccess;
	public PwDate tExpire;

	public int level; // short

	/** Used by KeePass internally, don't use */
	public int flags;
	
	public void setGroups(List<PwGroup> groups) {
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
	public void setId(PwGroupId id) {
		PwGroupIdV3 id3 = (PwGroupIdV3) id;
		groupId = id3.getId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Date getLastMod() {
		return tLastMod.getJDate();
	}

	@Override
	public void setParent(PwGroup prt) {
		parent = (PwGroupV3) prt;
		level = parent.level + 1;
		
	}

	@Override
	public void initNewGroup(String nm, PwGroupId newId) {
		super.initNewGroup(nm, newId);
		
		Date now = Calendar.getInstance().getTime();
		tCreation = new PwDate(now);
		tLastAccess = new PwDate(now);
		tLastMod = new PwDate(now);
		tExpire = new PwDate(PwGroupV3.NEVER_EXPIRE);

	}
	
	public void populateBlankFields(PwDatabaseV3 db) {
		if (icon == null) {
			icon = db.iconFactory.getIcon(1);
		}
		
		if (name == null) {
			name = "";
		}
		
		if (tCreation == null) {
			tCreation = PwEntryV3.DEFAULT_PWDATE;
		}
		
		if (tLastMod == null) {
			tLastMod = PwEntryV3.DEFAULT_PWDATE;
		}
		
		if (tLastAccess == null) {
			tLastAccess = PwEntryV3.DEFAULT_PWDATE;
		}
		
		if (tExpire == null) {
			tExpire = PwEntryV3.DEFAULT_PWDATE;
		}
	}

	@Override
	public void setLastAccessTime(Date date) {
		tLastAccess = new PwDate(date);
	}

	@Override
	public void setLastModificationTime(Date date) {
		tLastMod = new PwDate(date);
	}

}
