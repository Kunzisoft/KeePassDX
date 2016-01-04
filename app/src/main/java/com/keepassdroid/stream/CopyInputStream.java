/*
 * Copyright 2011 Brian Pellin.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class copies everything pulled through its input stream into the 
 * output stream. 
 */
public class CopyInputStream extends InputStream {
	private InputStream is;
	private OutputStream os;
	
	public CopyInputStream(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
		os.close();
	}

	@Override
	public void mark(int readlimit) {
		is.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read() throws IOException {
		int data = is.read();
		
		if (data != -1) {
			os.write(data);
		}
		
		return data;
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		int len = is.read(b, offset, length);
		
		if (len != -1) {
			os.write(b, offset, len);
		}
		
		return len;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int len = is.read(b);
		
		if (len != -1) {
			os.write(b, 0, len);
		}
		
		return len;
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long byteCount) throws IOException {
		return is.skip(byteCount);
	}

}
