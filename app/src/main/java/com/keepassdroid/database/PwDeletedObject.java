/*
 * Copyright 2010-2013 Brian Pellin.
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
import java.util.UUID;

public class PwDeletedObject {
	public UUID uuid;
	private Date deletionTime;
	
	public PwDeletedObject() {
		
	}
	
	public PwDeletedObject(UUID u) {
		this(u, new Date());
	}
	
	public PwDeletedObject(UUID u, Date d) {
		uuid = u;
		deletionTime = d;
	}
	
	public Date getDeletionTime() {
		if ( deletionTime == null ) {
			return new Date(System.currentTimeMillis());
		}
		
		return deletionTime;
	}
	
	public void setDeletionTime(Date date) {
		deletionTime = date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o == null) {
			return false;
		}
		else if (!(o instanceof PwDeletedObject)) {
			return false;
		}
		
		PwDeletedObject rhs = (PwDeletedObject) o;
		
		return uuid.equals(rhs.uuid);
	}
}
