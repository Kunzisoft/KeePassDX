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
package com.kunzisoft.keepass.utils;

import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwDatabaseV4;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.search.EntrySearchV4;
import com.kunzisoft.keepass.database.search.SearchParametersV4;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

public class SprEngineV4 {
	private static final int MAX_RECURSION_DEPTH = 12;
	private static final String STR_REF_START = "{REF:";
	private static final String STR_REF_END = "}";

	public class TargetResult {
		public PwEntryV4 entry;
		public char wanted;
		
		public TargetResult(PwEntryV4 entry, char wanted) {
			this.entry = entry;
			this.wanted = wanted;
		}
	}

	public String compile(String text, PwEntry entry, PwDatabase database) {
		SprContextV4 ctx = new SprContextV4((PwDatabaseV4)database, (PwEntryV4)entry);
		
		return compileInternal(text, ctx, 0);
	}
	
	private String compileInternal(String text, SprContextV4 ctx, int recursionLevel) {
		if (text == null) { return ""; }
		if (ctx == null) { return ""; }
		if (recursionLevel >= MAX_RECURSION_DEPTH) { return ""; }
		
		return fillRefPlaceholders(text, ctx, recursionLevel);
	}
	
	private String fillRefPlaceholders(String text, SprContextV4 ctx, int recursionLevel) {
		
		if (ctx.db == null) { return text; }
		
		int offset = 0;
		for (int i = 0; i < 20; ++i) {
			text = fillRefsUsingCache(text, ctx);
			
			int start = StrUtil.indexOfIgnoreCase(text, STR_REF_START, offset, Locale.ENGLISH);
			if (start < 0) { break; }
			int end = StrUtil.indexOfIgnoreCase(text, STR_REF_END, start + 1, Locale.ENGLISH);
			if (end <= start) { break; }
			
			String fullRef = text.substring(start, end - start + 1);
			TargetResult result = findRefTarget(fullRef, ctx);
			
			if (result != null) {
                PwEntryV4 found = result.entry;
                char wanted = result.wanted;
                
                if (found != null) {
                	String data;
                	switch (wanted) {
                	case 'T':
                		data = found.getTitle();
                		break;
                	case 'U':
                		data = found.getUsername();
                		break;
                	case 'A':
                		data = found.getUrl();
                		break;
                	case 'P':
                		data = found.getPassword();
                		break;
                	case 'N':
                		data = found.getNotes();
                		break;
                	case 'I':
                		data = found.getUUID().toString();
                		break;
                	default:
                		offset = start + 1;
                		continue;
                	}
                	
                	SprContextV4 subCtx = (SprContextV4) ctx.clone();
                	subCtx.entry = found;
                	
                	String innerContent = compileInternal(data, subCtx, recursionLevel + 1);
                	addRefsToCache(fullRef, innerContent, ctx);
                	text = fillRefsUsingCache(text, ctx);
                } else {
                	offset = start + 1;
                	continue;
                }
			}
			
		}
			
		return text;
	}

	public TargetResult findRefTarget(String fullRef, SprContextV4 ctx) {
		if (fullRef == null) { return null; }
		
		fullRef = fullRef.toUpperCase(Locale.ENGLISH);
		if (!fullRef.startsWith(STR_REF_START) || !fullRef.endsWith(STR_REF_END)) { 
			return null;
		}
		
		String ref = fullRef.substring(STR_REF_START.length(), fullRef.length() - STR_REF_START.length() - STR_REF_END.length());
		if (ref.length() <= 4) { return null; }
		if (ref.charAt(1) != '@') { return null; }
		if (ref.charAt(3) != ':') { return null; }
		
		char scan = Character.MIN_VALUE;
		char wanted = Character.MIN_VALUE;
		
		scan = Character.toUpperCase(ref.charAt(2));
		wanted = Character.toUpperCase(ref.charAt(0));
				
		SearchParametersV4 sp = new SearchParametersV4();
		sp.setupNone();

		sp.searchString = ref.substring(4);
		if (scan == 'T') { sp.searchInTitles = true; }
		else if (scan == 'U') { sp.searchInUserNames = true; }
		else if (scan == 'A') { sp.searchInUrls = true; }
		else if (scan == 'P') { sp.searchInPasswords = true; }
		else if (scan == 'N') { sp.searchInNotes = true; }
		else if (scan == 'I') { sp.searchInUUIDs = true; }
		else if (scan == 'O') { sp.searchInOther = true; }
		else { return null; }
		
		List<PwEntryV4> list = new ArrayList<>();
		// TODO type parameter
        EntrySearchV4 entrySearchV4 = new EntrySearchV4(ctx.db.getRootGroup());
        entrySearchV4.searchEntries(sp, list);
		
		if (list.size() > 0) { 
			return new TargetResult(list.get(0), wanted);
        }
		
		return null;
	}
	
	private void addRefsToCache(String ref, String value, SprContextV4 ctx) {
		if (ref == null) { return; }
		if (value == null) { return; }
		if (ctx == null) { return; }
		
		if (!ctx.refsCache.containsKey(ref)) {
			ctx.refsCache.put(ref, value);
		}
	}
	
	private String fillRefsUsingCache(String text, SprContextV4 ctx) {
		for (Entry<String, String> entry : ctx.refsCache.entrySet()) {
			text = StrUtil.replaceAllIgnoresCase(text, entry.getKey(), entry.getValue(), Locale.ENGLISH);
		}
		
		return text;
		
	}

}
