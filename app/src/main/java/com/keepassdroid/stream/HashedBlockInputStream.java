/*
 * Copyright 2010-2016 Brian Pellin.
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

import com.keepassdroid.utils.Types;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class HashedBlockInputStream extends InputStream {
	
	private final static int HASH_SIZE = 32;

	private LEDataInputStream baseStream;
	private int bufferPos = 0;
	private byte[] buffer = new byte[0];
	private long bufferIndex = 0;
	private boolean atEnd = false;
	
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public HashedBlockInputStream(InputStream is) {
		baseStream = new LEDataInputStream(is);
	}
	
	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		if ( atEnd ) return -1;
		
		int remaining = length;
		
		while ( remaining > 0 ) {
			if ( bufferPos == buffer.length ) {
				// Get more from the source into the buffer
				if ( ! ReadHashedBlock() ) {
					return length - remaining;
				}
				
			}

			// Copy from buffer out
			int copyLen = Math.min(buffer.length - bufferPos, remaining);
			
			System.arraycopy(buffer, bufferPos, b, offset, copyLen);
			
			offset += copyLen;
			bufferPos += copyLen;
			
			remaining -= copyLen;
		}
		
		return length;
	}

	/**
	 * @return false, when the end of the source stream is reached 
	 * @throws IOException 
	 */
	private boolean ReadHashedBlock() throws IOException {
		if ( atEnd ) return false;
		
		bufferPos = 0;
		
		long index = baseStream.readUInt();
		if ( index != bufferIndex ) {
			throw new IOException("Invalid data format");
		}
		bufferIndex++;
		
		byte[] storedHash = baseStream.readBytes(32);
		if ( storedHash == null || storedHash.length != HASH_SIZE) {
			throw new IOException("Invalid data format");
		}
		
		int bufferSize = LEDataInputStream.readInt(baseStream);
		if ( bufferSize < 0 ) {
			throw new IOException("Invalid data format");
		}
		
		if ( bufferSize == 0 ) {
			for (int hash = 0; hash < HASH_SIZE; hash++) {
				if ( storedHash[hash] != 0 ) {
					throw new IOException("Invalid data format");
				}
			}
				
			atEnd = true;
			buffer = new byte[0];
			return false;
		}
		
		buffer = baseStream.readBytes(bufferSize);
		if ( buffer == null || buffer.length != bufferSize ) {
			throw new IOException("Invalid data format");
		}
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 not implemented here.");
		}
		
		byte[] computedHash = md.digest(buffer);
		if ( computedHash == null || computedHash.length != HASH_SIZE ) {
			throw new IOException("Hash wrong size");
		}
		
		if ( ! Arrays.equals(storedHash, computedHash) ) {
			throw new IOException("Hashes didn't match.");
		}

		return true;
	}

	@Override
	public long skip(long n) throws IOException {
		return 0;
	}

	@Override
	public int read() throws IOException {
		if ( atEnd ) return -1;
		
		if ( bufferPos == buffer.length ) {
			if ( ! ReadHashedBlock() ) return -1;
		}
		
		int output = Types.readUByte(buffer, bufferPos);
		bufferPos++;
		
		return output;
	}

	@Override
	public void close() throws IOException {
		baseStream.close();
	}

}
