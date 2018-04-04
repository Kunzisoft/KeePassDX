/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.security;

import java.io.Serializable;

public class ProtectedString implements Serializable {
	
	private String string;
	private boolean protect;
	
	public boolean isProtected() {
		return protect;
	}
	
	public int length() {
		if (string == null) {
			return 0;
		}
		
		return string.length();
	}
	
	public ProtectedString() {
		this(false, "");
	}

    public ProtectedString(ProtectedString toCopy) {
        this.string = toCopy.string;
        this.protect = toCopy.protect;
    }
	
	public ProtectedString(boolean enableProtection, String string) {
		protect = enableProtection;
		this.string = string;
		
	}

	@Override
	public String toString() {
		return string;
	}

}
