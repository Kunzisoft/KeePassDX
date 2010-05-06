/*
* Copyright 2010 Brian Pellin.
*
* This file is part of KeePassDroid.
*
* KeePassDroid is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* KeePassDroid is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePassDroid. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.keepassdroid.tests.stream;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;

import com.keepassdroid.stream.HashedBlockInputStream;
import com.keepassdroid.stream.HashedBlockOutputStream;

public class HashedBlock extends TestCase {
	public void testBlockAligned() throws IOException {
		final int blockSize = 1024;
		byte[] orig = new byte[blockSize];
		
		Random rnd = new Random();
		rnd.nextBytes(orig);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		HashedBlockOutputStream output = new HashedBlockOutputStream(bos, blockSize);
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
}
