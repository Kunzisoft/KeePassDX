/*
 * Copyright 2009-2011 Brian Pellin.
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
package com.keepassdroid.tests;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import junit.framework.TestCase;

import com.keepassdroid.database.PwDate;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.Types;

public class TypesTest extends TestCase {

	public void testReadWriteLongZero() {
		testReadWriteLong((byte) 0);
	}
	
	public void testReadWriteLongMax() {
		testReadWriteLong(Byte.MAX_VALUE);
	}
	
	public void testReadWriteLongMin() {
		testReadWriteLong(Byte.MIN_VALUE);
	}
	
	public void testReadWriteLongRnd() {
		Random rnd = new Random();
		byte[] buf = new byte[1];
		rnd.nextBytes(buf);
		
		testReadWriteLong(buf[0]);
	}
	
	private void testReadWriteLong(byte value) {
		byte[] orig = new byte[8];
		byte[] dest = new byte[8];
		
		setArray(orig, value, 0, 8);
		
		long one = LEDataInputStream.readLong(orig, 0);
		LEDataOutputStream.writeLong(one, dest, 0);
		
		assertArrayEquals(orig, dest);

	}
	
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
				
		int one = LEDataInputStream.readInt(orig, 0);
		
		LEDataOutputStream.writeInt(one, dest, 0);

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
		
		int one = LEDataInputStream.readUShort(orig, 0);
		dest = LEDataOutputStream.writeUShortBuf(one);
		
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
		
		int one = LEDataInputStream.readUShort(orig, 0);
		LEDataOutputStream.writeUShort(one, dest, 0);
		
		assertArrayEquals(orig, dest);

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
		
		assertArrayEquals(orig, dest);
		
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
		assertEquals("Day mismatch: ", 1, actual.get(Calendar.DAY_OF_MONTH));
		assertEquals("Hour mismatch: ", 3, actual.get(Calendar.HOUR_OF_DAY));
		assertEquals("Minute mismatch: ", 4, actual.get(Calendar.MINUTE));
		assertEquals("Second mismatch: ", 5, actual.get(Calendar.SECOND));
	}
	
	public void testUUID() {
		Random rnd = new Random();
		byte[] bUUID = new byte[16];
		rnd.nextBytes(bUUID);
		
		UUID uuid = Types.bytestoUUID(bUUID);
		byte[] eUUID = Types.UUIDtoBytes(uuid);
		
		assertArrayEquals("UUID match failed", bUUID, eUUID);
	}

	public void testULongMax() throws Exception {
		byte[] ulongBytes = new byte[8];
		for (int i = 0; i < ulongBytes.length; i++) {
			ulongBytes[i] = -1;
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LEDataOutputStream leos = new LEDataOutputStream(bos);
		leos.writeLong(Types.ULONG_MAX_VALUE);
		leos.close();

		byte[] uLongMax = bos.toByteArray();

		assertArrayEquals(ulongBytes, uLongMax);
	}
}