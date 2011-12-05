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


/** Little Endian version of the DataOutputStream
 * @author bpellin
 *
 */
public class LEDataOutputStream extends OutputStream {

	private OutputStream baseStream; 
	
	public LEDataOutputStream(OutputStream out) {
		baseStream = out;
	}
	
	public void writeUInt(long uint) throws IOException {
		baseStream.write(LEDataOutputStream.writeIntBuf((int) uint));
	}

	@Override
	public void close() throws IOException {
		baseStream.close();
	}

	@Override
	public void flush() throws IOException {
		baseStream.flush();
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		baseStream.write(buffer, offset, count);
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		baseStream.write(buffer);
	}

	@Override
	public void write(int oneByte) throws IOException {
		baseStream.write(oneByte);
	}
	
	public void writeLong(long val) throws IOException {
		byte[] buf = new byte[8];
		
		writeLong(val, buf, 0);
		baseStream.write(buf);
	}
	
	public void writeInt(int val) throws IOException {
		byte[] buf = new byte[4];
		writeInt(val, buf, 0);
		
		baseStream.write(buf);
	}
	
	public void writeUShort(int val) throws IOException {
		byte[] buf = new byte[2];
		writeUShort(val, buf, 0);
		baseStream.write(buf);
	}

	public static byte[] writeIntBuf(int val) {
		  byte[] buf = new byte[4];
		  writeInt(val, buf, 0);
	
		  return buf;
	  }

	public static byte[] writeUShortBuf(int val) {
		  byte[] buf = new byte[2];
		  
		  writeUShort(val, buf, 0);
		  
		  return buf;
	  }

	/** Write an unsigned 16-bit value
	   * 
	   * @param val
	   * @param buf
	   * @param offset
	   */
	  public static void writeUShort(int val, byte[] buf, int offset) {
		  buf[offset + 0] = (byte)(val & 0x00FF);
		  buf[offset + 1] = (byte)((val & 0xFF00) >> 8);
	  }

	/**
	   * Write a 32-bit value.
	   * 
	   * @param val
	   * @param buf
	   * @param offset
	   */
	  public static void writeInt( int val, byte[] buf, int offset ) {
	    buf[offset + 0] = (byte)(val & 0xFF);
	    buf[offset + 1] = (byte)((val >>> 8) & 0xFF);
	    buf[offset + 2] = (byte)((val >>> 16) & 0xFF);
	    buf[offset + 3] = (byte)((val >>> 24) & 0xFF);
	  }
	  
	  public static byte[] writeLongBuf(long val) {
		  byte[] buf = new byte[8];
		  writeLong(val, buf, 0);
		  return buf;
	  }

	public static void writeLong( long val, byte[] buf, int offset ) {
		buf[offset + 0] = (byte)(val & 0xFF);
		buf[offset + 1] = (byte)((val >>> 8) & 0xFF);
		buf[offset + 2] = (byte)((val >>> 16) & 0xFF);
		buf[offset + 3] = (byte)((val >>> 24) & 0xFF);
		buf[offset + 4] = (byte)((val >>> 32) & 0xFF);
		buf[offset + 5] = (byte)((val >>> 40) & 0xFF);
		buf[offset + 6] = (byte)((val >>> 48) & 0xFF);
		buf[offset + 7] = (byte)((val >>> 56) & 0xFF);
	}

}
