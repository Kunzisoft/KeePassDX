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

import com.kunzisoft.keepass.utils.Types;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;


/** Little endian version of the DataInputStream
 * @author bpellin
 *
 */
public class LEDataInputStream extends InputStream {

	public static final long INT_TO_LONG_MASK = 0xffffffffL;
	
	private InputStream baseStream;

	public LEDataInputStream(InputStream in) {
		baseStream = in;
	}
	
	/** Read a 32-bit value and return it as a long, so that it can
	 *  be interpreted as an unsigned integer.
	 * @return
	 * @throws IOException
	 */
	public long readUInt() throws IOException {
		return readUInt(baseStream);
	}
	
	public int readInt() throws IOException {
		return readInt(baseStream);
	}
	
	public long readLong() throws IOException {
		byte[] buf = readBytes(8);
		
		return readLong(buf, 0);
	}
	
	@Override
	public int available() throws IOException {
		return baseStream.available();
	}

	@Override
	public void close() throws IOException {
		baseStream.close();
	}

	@Override
	public void mark(int readlimit) {
		baseStream.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return baseStream.markSupported();
	}

	@Override
	public int read() throws IOException {
		return baseStream.read();
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		return baseStream.read(b, offset, length);
	}

	@Override
	public int read(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return super.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		baseStream.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return baseStream.skip(n);
	}

	public byte[] readBytes(int length) throws IOException {
	    // TODO Exception max length < buffer size
		byte[] buf = new byte[length];

		int count = 0;
		while ( count < length ) {
			int read = read(buf, count, length - count);
			
			// Reached end
			if ( read == -1 ) {
				// Stop early
				byte[] early = new byte[count];
				System.arraycopy(buf, 0, early, 0, count);
				return early;
			}
			
			count += read;
		}
		
		return buf;
	}

    public void readBytes(int length, ActionReadBytes actionReadBytes) throws IOException {
		//MemUtil.readBytes(baseStream, length, actionReadBytes);
        byte[] buffer = new byte[3 * 256];

        int offset = 0;
        int read = 0;
        while ( offset < length && read != -1) {
            int tempLength = buffer.length;
            // If buffer not needed
            if (length - offset < tempLength)
                tempLength = length - offset;
            read = read(buffer, 0, tempLength);
            if (read >= 0 && buffer.length != read) { // TODO Better perf
                byte[] tmpBytes = buffer;
                buffer = Arrays.copyOf(tmpBytes, read);
            }
            actionReadBytes.doAction(buffer);
            offset += read;
        }
    }

	public static int readUShort(InputStream is) throws IOException {
		  byte[] buf = new byte[2];
		  
		  is.read(buf, 0, 2);
		  
		  return readUShort(buf, 0); 
	  }
	
	public int readUShort() throws IOException {
		return readUShort(baseStream);
	}

	/**
	   * Read an unsigned 16-bit value.
	   * 
	   * @param buf
	   * @param offset
	   * @return
	   */
	  public static int readUShort( byte[] buf, int offset ) {
	    return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8);
	  }

	public static long readLong( byte buf[], int offset ) {
		return ((long)buf[offset + 0] & 0xFF) + (((long)buf[offset + 1] & 0xFF) << 8) 
		+ (((long)buf[offset + 2] & 0xFF) << 16) + (((long)buf[offset + 3] & 0xFF) << 24) 
		+ (((long)buf[offset + 4] & 0xFF) << 32) + (((long)buf[offset + 5] & 0xFF) << 40) 
		+ (((long)buf[offset + 6] & 0xFF) << 48) + (((long)buf[offset + 7] & 0xFF) << 56);
	}

	public static long readUInt( byte buf[], int offset ) {
		  return (readInt(buf, offset) & INT_TO_LONG_MASK);
	  }

	public static int readInt(InputStream is) throws IOException {
		  byte[] buf = new byte[4];
	
		  is.read(buf, 0, 4);
		  
		  return readInt(buf, 0);
	  }

	public static long readUInt(InputStream is) throws IOException {
		  return (readInt(is) & INT_TO_LONG_MASK);
	  }

	/**
	   * Read a 32-bit value.
	   * 
	   * @param buf
	   * @param offset
	   * @return
	   */
	  public static int readInt( byte buf[], int offset ) {
	    return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8) + ((buf[offset + 2] & 0xFF) << 16)
	           + ((buf[offset + 3] & 0xFF) << 24);
	  }

	  public UUID readUUID() throws IOException {
		  byte[] buf = readBytes(16);

		  return Types.bytestoUUID(buf);
	  }

}
