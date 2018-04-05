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
package com.kunzisoft.keepass.stream;

import java.io.IOException;
import java.io.OutputStream;

public class CountOutputStream extends OutputStream {
	OutputStream os;
	long bytes = 0;
	
	public CountOutputStream(OutputStream os) {
		this.os = os;
	}


	@Override
	public void close() throws IOException {
		os.close();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}


	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		bytes += count;
		os.write(buffer, offset, count);
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		bytes += buffer.length;
		os.write(buffer);
	}

	@Override
	public void write(int oneByte) throws IOException {
		bytes++;
		os.write(oneByte);
	}
}
