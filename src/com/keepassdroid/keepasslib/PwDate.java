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
package com.keepassdroid.keepasslib;

import java.util.Calendar;
import java.util.Date;

import org.phoneid.keepassj2me.Types;

import com.keepassdroid.app.App;

/** Converting from the C Date format to the Java data format is
 *  expensive when done for every record at once.  I use this class to
 *  allow lazy conversions between the formats.
 * @author bpellin
 *
 */
public class PwDate implements Cloneable {
	
	private static final int DATE_SIZE = 5; 
	
	private boolean cDateBuilt = false;
	private boolean jDateBuilt = false;
	
	private Date jDate;
	private byte[] cDate;
	
	public PwDate(byte[] buf, int offset) {
		cDate = new byte[DATE_SIZE];
		System.arraycopy(buf, offset, cDate, 0, DATE_SIZE);
		cDateBuilt = true;
	}
	
	public PwDate(Date date) {
		jDate = date;
		jDateBuilt = true;
	}
	
	public PwDate(long millis) {
		jDate = new Date(millis);
		jDateBuilt = true;
	}
	
	private PwDate() {
		
	}
	
	@Override
	public Object clone() {
		PwDate copy = new PwDate();
		
		if ( cDateBuilt ) {
			byte[] newC = new byte[DATE_SIZE];
			System.arraycopy(cDate, 0, newC, 0, DATE_SIZE);
			copy.cDate = newC;
			copy.cDateBuilt = true;
		}
		
		if ( jDateBuilt ) {
			copy.jDate = (Date) jDate.clone();
			copy.jDateBuilt = true;
		}
			
		return copy;
	}


	
	public Date getJDate() {
		if ( ! jDateBuilt ) {
			jDate = readTime(cDate, 0, App.getCalendar());
			jDateBuilt = true;
		}
		
		return jDate;
	}
	
	public byte[] getCDate() {
		if ( ! cDateBuilt ) {
			cDate = writeTime(jDate, App.getCalendar());
			cDateBuilt = true;
		}
		
		return cDate;
	}
	
	
	/**
	 * Unpack date from 5 byte format. The five bytes at 'offset' are unpacked
	 * to a java.util.Date instance.
	 */
	public static Date readTime(byte[] buf, int offset, Calendar time) {
		int dw1 = Types.readUByte(buf, offset);
		int dw2 = Types.readUByte(buf, offset + 1);
		int dw3 = Types.readUByte(buf, offset + 2);
		int dw4 = Types.readUByte(buf, offset + 3);
		int dw5 = Types.readUByte(buf, offset + 4);

		// Unpack 5 byte structure to date and time
		int year = (dw1 << 6) | (dw2 >> 2);
		int month = ((dw2 & 0x00000003) << 2) | (dw3 >> 6);

		int day = (dw3 >> 1) & 0x0000001F;
		int hour = ((dw3 & 0x00000001) << 4) | (dw4 >> 4);
		int minute = ((dw4 & 0x0000000F) << 2) | (dw5 >> 6);
		int second = dw5 & 0x0000003F;

		if (time == null) {
			time = Calendar.getInstance();
		}
		// File format is a 1 based month, java Calendar uses a zero based month
		time.set(year, month - 1, day, hour, minute, second);

		return time.getTime();

	}

	public static byte[] writeTime(Date date, Calendar cal) {
		if (date == null) {
			return null;
		}

		byte[] buf = new byte[5];
		if (cal == null) {
			cal = Calendar.getInstance();
		}
		cal.setTime(date);

		int year = cal.get(Calendar.YEAR);
		// File format is a 1 based month, java Calendar uses a zero based month
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);

		buf[0] = Types.writeUByte(((year >> 6) & 0x0000003F));
		buf[1] = Types.writeUByte(((year & 0x0000003F) << 2)
				| ((month >> 2) & 0x00000003));
		buf[2] = (byte) (((month & 0x00000003) << 6)
				| ((day & 0x0000001F) << 1) | ((hour >> 4) & 0x00000001));
		buf[3] = (byte) (((hour & 0x0000000F) << 4) | ((minute >> 2) & 0x0000000F));
		buf[4] = (byte) (((minute & 0x00000003) << 6) | (second & 0x0000003F));

		return buf;
	}

}
