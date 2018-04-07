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
package com.kunzisoft.keepass.database.security;

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
