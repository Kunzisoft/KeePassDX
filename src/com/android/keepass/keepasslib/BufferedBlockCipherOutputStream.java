/*
 * Copyright 2009 Brian Pellin.
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
package com.android.keepass.keepasslib;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

public class BufferedBlockCipherOutputStream extends FilterOutputStream {

	OutputStream mOS;
	PaddedBufferedBlockCipher mCipher;
	
	public BufferedBlockCipherOutputStream(OutputStream out) {
		super(out);
		mOS = out;
	}
	
	public BufferedBlockCipherOutputStream(OutputStream out, PaddedBufferedBlockCipher cipher) {
		super(out);
		mOS = out;
		mCipher = cipher;
	}

	@Override
	public void close() throws IOException {
		byte[] block = new byte[2*mCipher.getBlockSize()];
		int bytes;
		try {
			bytes = mCipher.doFinal(block, 0);
		} catch (DataLengthException e) {
			throw new IOException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new IOException("IllegalStateException.");
		} catch (InvalidCipherTextException e) {
			throw new IOException("InvalidCipherText.");
		}
		if ( bytes > 0 ) {
			mOS.write(block, 0, bytes);
		}
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		int outputLen = mCipher.getUpdateOutputSize(count);
		
		if ( outputLen > 0 ) {
			byte[] block = new byte[outputLen];
			int bytes = mCipher.processBytes(buffer, offset, count, block, 0);
			if ( bytes > 0 ) {
				mOS.write(block, 0, bytes);
			}
		}
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		int length = buffer.length;
		int outputLen = mCipher.getUpdateOutputSize(length);
		
		if ( outputLen > 0 ) {
			byte[] block = new byte[outputLen];
			int bytes = mCipher.processBytes(buffer, 0, length, block, 0);
			
			if ( bytes > 0 ) {
				mOS.write(block, 0, bytes);
			}
		}
	}

	@Override
	public void write(int oneByte) throws IOException {
		int outputLen = mCipher.getUpdateOutputSize(1);
		
		if ( outputLen > 0 ) {
			byte[] block = new byte[outputLen];
			int bytes = mCipher.processByte((byte)oneByte, block, 0);
			
			if ( bytes > 0 ) {
				mOS.write(block, 0, bytes);
			}
		}
	}

}
