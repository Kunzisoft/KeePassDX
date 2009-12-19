/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.keepassdroid.tests;

import static org.junit.Assert.assertArrayEquals;

import java.util.Calendar;

import junit.framework.TestCase;

import org.phoneid.keepassj2me.Types;

import com.keepassdroid.keepasslib.PwDate;

public class TypesTest extends TestCase {
	
	public void testReadWriteIntZero() {
		testReadWriteInt((byte) 0);
	}
	
	public void testReadWriteIntMin() {
		testReadWriteInt(Byte.MIN_VALUE);
	}
	
	public void testReadWriteIntMax() {
		testReadWriteInt(Byte.MAX_VALUE);
	}
	
	private void testReadWriteInt(byte value) {
		byte[] orig = new byte[4];
		byte[] dest = new byte[4];
		
		for (int i = 0; i < 4; i++ ) {
			orig[i] = 0;
		}
		
		setArray(orig, value, 0, 4);
				
		int one = Types.readInt(orig, 0);
		
		Types.writeInt(one, dest, 0);

		assertArrayEquals(orig, dest);
		
	}
	
	private void setArray(byte[] buf, byte value, int offset, int size) {
		for (int i = offset; i < offset + size; i++) {
			buf[i] = value;
		}
	}
	
	public void testReadWriteShortOne() {
		byte[] orig = new byte[2];
		byte[] dest = new byte[2];
		
		orig[0] = 0;
		orig[1] = 1;
		
		int one = Types.readShort(orig, 0);
		dest = Types.writeShort(one);
		
		assertArrayEquals(orig, dest);
		
	}
	
	public void testReadWriteShortMin() {
		testReadWriteShort(Byte.MIN_VALUE);
	}
	
	public void testReadWriteShortMax() {
		testReadWriteShort(Byte.MAX_VALUE);
	}
	
	private void testReadWriteShort(byte value) {
		byte[] orig = new byte[2];
		byte[] dest = new byte[2];
		
		setArray(orig, value, 0, 2);
		
		int one = Types.readShort(orig, 0);
		Types.writeShort(one, dest, 0);
	}

	public void testReadWriteByteZero() {
		testReadWriteByte((byte) 0);
	}
	
	public void testReadWriteByteMin() {
		testReadWriteByte(Byte.MIN_VALUE);
	}
	
	public void testReadWriteByteMax() {
		testReadWriteShort(Byte.MAX_VALUE);
	}
	
	private void testReadWriteByte(byte value) {
		byte[] orig = new byte[1];
		byte[] dest = new byte[1];
		
		setArray(orig, value, 0, 1);
		
		int one = Types.readUByte(orig, 0);
		Types.writeUByte(one, dest, 0);
	}
	
	public void testDate() {
		Calendar cal = Calendar.getInstance();
		
		Calendar expected = Calendar.getInstance();
		expected.set(2008, 1, 2, 3, 4, 5);
		
		byte[] buf = PwDate.writeTime(expected.getTime(), cal);
		Calendar actual = Calendar.getInstance();
		actual.setTime(PwDate.readTime(buf, 0, cal));
		
		assertEquals("Year mismatch: ", 2008, actual.get(Calendar.YEAR));
		assertEquals("Month mismatch: ", 1, actual.get(Calendar.MONTH));
		assertEquals("Day mismatch: ", 2, actual.get(Calendar.DAY_OF_MONTH));
		assertEquals("Hour mismatch: ", 3, actual.get(Calendar.HOUR_OF_DAY));
		assertEquals("Minute mismatch: ", 4, actual.get(Calendar.MINUTE));
		assertEquals("Second mismatch: ", 5, actual.get(Calendar.SECOND));
	}
}