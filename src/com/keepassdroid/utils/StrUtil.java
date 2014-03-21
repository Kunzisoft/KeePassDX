/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid.utils;

import android.annotation.SuppressLint;
import java.util.ArrayList;
import java.util.List;

public class StrUtil {
	public static List<String> splitSearchTerms(String search) {
		List<String> list = new ArrayList<String>();
		if (search == null) { return list; }
		
		StringBuilder sb = new StringBuilder();
		boolean quoted = false;
		
		for (int i = 0; i < search.length(); i++) {
			char ch = search.charAt(i);
			
			if ( ((ch == ' ') || (ch == '\t') || (ch == '\r') || (ch == '\n'))
					&& !quoted) {
				
				int len = sb.length();
				if (len > 0) {
					list.add(sb.toString());
					sb.delete(0, len);
				}
				else if (ch == '\"') { 
					quoted = !quoted;
				}
				else {
					sb.append(ch);
				}
			}
		}
		
		if (sb.length() > 0) {
			list.add(sb.toString());
		}
		
		return list;
	}
	
	@SuppressLint("DefaultLocale")
	public static int indexOfIgnoreCase(String text, String search, int start) {
		if (text == null || search == null) return -1;
		
		return text.toLowerCase().indexOf(search.toLowerCase(), start);
	}
	
	public static int indexOfIgnoreCase(String text, String search) {
		return indexOfIgnoreCase(text, search, 0);
	}
	
	public static String replaceAllIgnoresCase(String text, String find, String newText) {
		if (text == null || find == null || newText == null) { return text; }
		
		int pos = 0;
		while (pos < text.length()) {
			pos = indexOfIgnoreCase(text, find, pos);
			if (pos < 0) { break; }
			
			String before = text.substring(0, pos);
			String after = text.substring(pos + find.length());
			
			text = before.concat(newText).concat(after);
			pos += newText.length();
		}
		
		return text;
	}
	
}
