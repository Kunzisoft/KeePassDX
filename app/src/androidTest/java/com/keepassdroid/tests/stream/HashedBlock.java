/*
* Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
*
* This file is part of KeePass Libre.
*
* KeePass Libre is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* KeePass Libre is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePass Libre. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.keepassdroid.tests.stream;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import com.keepassdroid.stream.HashedBlockInputStream;
import com.keepassdroid.stream.HashedBlockOutputStream;

public class HashedBlock extends TestCase {
	
	private static Random rand = new Random();

	public void testBlockAligned() throws IOException {
		testSize(1024, 1024);
	}
	
	public void testOffset() throws IOException {
		testSize(1500, 1024);
	}
	
	private void testSize(int blockSize, int bufferSize) throws IOException {
		byte[] orig = new byte[blockSize];
		
		rand.nextBytes(orig);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		HashedBlockOutputStream output = new HashedBlockOutputStream(bos, bufferSize);
		output.write(orig);
		output.close();
		
		byte[] encoded = bos.toByteArray();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
		HashedBlockInputStream input = new HashedBlockInputStream(bis);

		ByteArrayOutputStream decoded = new ByteArrayOutputStream();
		while ( true ) {
			byte[] buf = new byte[1024];
			int read = input.read(buf);
			if ( read == -1 ) {
				break;
			}
			
			decoded.write(buf, 0, read);
		}
		
		byte[] out = decoded.toByteArray();
		
		assertArrayEquals(orig, out);
		
	}
	
	public void testGZIPStream() throws IOException {
		final int testLength = 32000;
		
		byte[] orig = new byte[testLength];
		rand.nextBytes(orig);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		HashedBlockOutputStream hos = new HashedBlockOutputStream(bos);
		GZIPOutputStream zos = new GZIPOutputStream(hos);
		
		zos.write(orig);
		zos.close();
		
		byte[] compressed = bos.toByteArray();
		ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
		HashedBlockInputStream his = new HashedBlockInputStream(bis);
		GZIPInputStream zis = new GZIPInputStream(his);
		
		byte[] uncompressed = new byte[testLength];
		
		int read = 0;
		while (read != -1 && testLength - read > 0) {
			read += zis.read(uncompressed, read, testLength - read);
			
		}
		
		assertArrayEquals("Output not equal to input", orig, uncompressed);
		
		
	}
}
