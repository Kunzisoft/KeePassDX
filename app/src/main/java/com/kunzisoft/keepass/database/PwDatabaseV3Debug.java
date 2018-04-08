/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.database;

public class PwDatabaseV3Debug extends PwDatabaseV3 {

	private byte[] postHeader;
	private PwDbHeaderV3 dbHeader;
	
	@Override
	public void copyEncrypted(byte[] buf, int offset, int size) {
		postHeader = new byte[size];
		System.arraycopy(buf, offset, postHeader, 0, size);
	}

	@Override
	public void copyHeader(PwDbHeaderV3 header) {
		dbHeader = header;
	}

    public byte[] getPostHeader() {
        return postHeader;
    }

    public PwDbHeaderV3 getDbHeader() {
        return dbHeader;
    }
}
