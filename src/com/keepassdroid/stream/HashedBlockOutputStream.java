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

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashedBlockOutputStream extends OutputStream {

	private final static int DEFAULT_BUFFER_SIZE = 1024 * 1024; 
	
	private LEDataOutputStream baseStream;
	private int bufferPos = 0;
	private byte[] buffer;
	private long bufferIndex = 0;
	
	public HashedBlockOutputStream(OutputStream os) {
		init(os, DEFAULT_BUFFER_SIZE);
	}
	
	public HashedBlockOutputStream(OutputStream os, int bufferSize) {
		if ( bufferSize <= 0 ) {
			bufferSize = DEFAULT_BUFFER_SIZE;
		}
		
		init(os, bufferSize);
	}
	
	private void init(OutputStream os, int bufferSize) {
		baseStream = new LEDataOutputStream(os);
		buffer = new byte[bufferSize];
		
	}

	@Override
	public void write(int oneByte) throws IOException {
		byte[] buf = new byte[1];
		buf[0] = (byte)oneByte;
		write(buf, 0, 1);
	}

	@Override
	public void close() throws IOException {
		if ( bufferPos != 0 ) {
			// Write remaining buffered amount
			WriteHashedBlock();
		}
		
		// Write terminating block
		WriteHashedBlock();
		
		flush();
		baseStream.close();
	}

	@Override
	public void flush() throws IOException {
		baseStream.flush();
	}

	@Override
	public void write(byte[] b, int offset, int count) throws IOException {
		while ( count > 0 ) {
			if ( bufferPos == buffer.length ) {
				WriteHashedBlock();
			}
			
			int copyLen = Math.min(buffer.length - bufferPos, count);
			
			System.arraycopy(b, offset, buffer, bufferPos, copyLen);
			
			offset += copyLen;
			bufferPos += copyLen;
			
			count -= copyLen;
		}
	}

	private void WriteHashedBlock() throws IOException {
		baseStream.writeUInt(bufferIndex);
		bufferIndex++;
		
		if ( bufferPos > 0 ) {
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("SHA-256 not implemented here.");
			}
			
			byte[] hash;
			md.update(buffer, 0, bufferPos);
			hash = md.digest();
			/*
			if ( bufferPos == buffer.length) {
				hash = md.digest(buffer);
			} else {
				byte[] b = new byte[bufferPos];
				System.arraycopy(buffer, 0, b, 0, bufferPos);
				hash = md.digest(b);
			}
			*/
			
			baseStream.write(hash);

		} else {
			// Write 32-bits of zeros
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
			baseStream.writeLong(0L);
		}
		
		baseStream.writeInt(bufferPos);
		
		if ( bufferPos > 0 ) {
			baseStream.write(buffer, 0, bufferPos);
		}
		
		bufferPos = 0;
		
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		write(buffer, 0, buffer.length);
	}

}
