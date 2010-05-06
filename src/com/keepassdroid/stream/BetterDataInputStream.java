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
package com.keepassdroid.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BetterDataInputStream extends DataInputStream {

	public static final long INT_TO_LONG_MASK = 0xffffffffL;

	public BetterDataInputStream(InputStream in) {
		super(in);
	}
	
	/** Read a 32-bit value and return it as a long, so that it can
	 *  be interpreted as an unsigned integer.
	 * @return
	 * @throws IOException
	 */
	public long readUInt() throws IOException {
		return (readInt() & INT_TO_LONG_MASK);
	}
	
	public byte[] readBytes(int length) throws IOException {
		byte[] buf = new byte[length];
		
		int count = 0;
		while ( count < length ) {
			int read = read(buf, count, length - count);
			
			// Reached end
			if ( read == -1 ) {
				// Stop early
				byte[] early = new byte[count];
				System.arraycopy(buf, 0, early, 0, count);
				return early;
			}
			
			count += read;
		}
		
		return buf;
	}

}
