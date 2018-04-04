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
import java.util.Arrays;

public class ProtectedBinary implements Serializable {
	
	public final static ProtectedBinary EMPTY = new ProtectedBinary();
	
	private byte[] data;
	private boolean protect;
	
	public boolean isProtected() {
		return protect;
	}
	
	public int length() {
		if (data == null) {
			return 0;
		}
		
		return data.length;
	}
	
	public ProtectedBinary() {
		this(false, new byte[0]);
		
	}
	
	public ProtectedBinary(boolean enableProtection, byte[] data) {
		protect = enableProtection;
		this.data = data;
		
	}
	
	
	// TODO: replace the byte[] with something like ByteBuffer to make the return
	// value immutable, so we don't have to worry about making deep copies
	public byte[] getData() {
		return data;
	}
	
	public boolean equals(ProtectedBinary rhs) {
		return (protect == rhs.protect) && Arrays.equals(data, rhs.data);
	}

}
