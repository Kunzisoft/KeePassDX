/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.database.search.EntrySearchHandlerV4;
import com.kunzisoft.keepass.database.search.SearchParametersV4;
import com.kunzisoft.keepass.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private class SprContextV4 {

        public PwDatabaseV4 db;
        public PwEntryV4 entry;
        public Map<String, String> refsCache = new HashMap<>();

        SprContextV4(PwDatabaseV4 db, PwEntryV4 entry) {
            this.db = db;
            this.entry = entry;
        }

        SprContextV4(SprContextV4 source) {
            this.db = source.db;
            this.entry = source.entry;
            this.refsCache = source.refsCache;
        }
    }

    public String compile(String text, PwEntryV4 entry, PwDatabase database) {
        SprContextV4 ctx = new SprContextV4((PwDatabaseV4)database, entry);

        return compileInternal(text, ctx, 0);
    }

    private String compileInternal(String text, SprContextV4 sprContextV4, int recursionLevel) {
        if (text == null) { return ""; }
        if (sprContextV4 == null) { return ""; }
        if (recursionLevel >= MAX_RECURSION_DEPTH) { return ""; }

        return fillRefPlaceholders(text, sprContextV4, recursionLevel);
    }

    private String fillRefPlaceholders(String text, SprContextV4 contextV4, int recursionLevel) {

        if (contextV4.db == null) { return text; }

        int offset = 0;
        for (int i = 0; i < 20; ++i) {
            text = fillRefsUsingCache(text, contextV4);

            int start = StringUtil.INSTANCE.indexOfIgnoreCase(text, STR_REF_START, offset, Locale.ENGLISH);
            if (start < 0) { break; }
            int end = StringUtil.INSTANCE.indexOfIgnoreCase(text, STR_REF_END, start + 1, Locale.ENGLISH);
            if (end <= start) { break; }

            String fullRef = text.substring(start, end - start + 1);
            TargetResult result = findRefTarget(fullRef, contextV4);

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
                            data = found.getNodeId().toString();
                            break;
                        default:
                            offset = start + 1;
                            continue;
                    }

                    SprContextV4 subCtx = new SprContextV4(contextV4);
                    subCtx.entry = found;

                    String innerContent = compileInternal(data, subCtx, recursionLevel + 1);
                    addRefsToCache(fullRef, innerContent, contextV4);
                    text = fillRefsUsingCache(text, contextV4);
                } else {
                    offset = start + 1;
                }
            }

        }

        return text;
    }

    private TargetResult findRefTarget(String fullRef, SprContextV4 contextV4) {
        if (fullRef == null) { return null; }

        fullRef = fullRef.toUpperCase(Locale.ENGLISH);
        if (!fullRef.startsWith(STR_REF_START) || !fullRef.endsWith(STR_REF_END)) {
            return null;
        }

        String ref = fullRef.substring(STR_REF_START.length(), fullRef.length() - STR_REF_START.length() - STR_REF_END.length());
        if (ref.length() <= 4) { return null; }
        if (ref.charAt(1) != '@') { return null; }
        if (ref.charAt(3) != ':') { return null; }

        char scan = Character.toUpperCase(ref.charAt(2));
        char wanted = Character.toUpperCase(ref.charAt(0));

        SearchParametersV4 searchParametersV4 = new SearchParametersV4();
        searchParametersV4.setupNone();

        searchParametersV4.setSearchString(ref.substring(4));
        if (scan == 'T') { searchParametersV4.setSearchInTitles(true); }
        else if (scan == 'U') { searchParametersV4.setSearchInUserNames(true); }
        else if (scan == 'A') { searchParametersV4.setSearchInUrls(true); }
        else if (scan == 'P') { searchParametersV4.setSearchInPasswords(true); }
        else if (scan == 'N') { searchParametersV4.setSearchInNotes(true); }
        else if (scan == 'I') { searchParametersV4.setSearchInUUIDs(true); }
        else if (scan == 'O') { searchParametersV4.setSearchInOther(true); }
        else { return null; }

        List<PwEntryV4> list = new ArrayList<>();
        // TODO type parameter
        searchEntries(contextV4.db.getRootGroup(), searchParametersV4, list);

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

    private String fillRefsUsingCache(String text, SprContextV4 sprContextV4) {
        for (Entry<String, String> entry : sprContextV4.refsCache.entrySet()) {
            text = StringUtil.INSTANCE.replaceAllIgnoresCase(text, entry.getKey(), entry.getValue(), Locale.ENGLISH);
        }

        return text;
    }

    private void searchEntries(PwGroupV4 root, SearchParametersV4 searchParametersV4, List<PwEntryV4> listStorage) {
        if (searchParametersV4 == null)  { return; }
        if (listStorage == null) { return; }

        List<String> terms = StringUtil.INSTANCE.splitStringTerms(searchParametersV4.getSearchString());
        if (terms.size() <= 1 || searchParametersV4.getRegularExpression()) {
            root.doForEachChild(new EntrySearchHandlerV4(searchParametersV4, listStorage), null);
            return;
        }

        // Search longest term first
        Comparator<String> stringLengthComparator = (lhs, rhs) -> lhs.length() - rhs.length();
        Collections.sort(terms, stringLengthComparator);

        String fullSearch = searchParametersV4.getSearchString();
        List<PwEntryV4> childEntries = root.getChildEntries();
        for (int i = 0; i < terms.size(); i ++) {
            List<PwEntryV4> pgNew = new ArrayList<>();

            searchParametersV4.setSearchString(terms.get(i));

            boolean negate = false;
            if (searchParametersV4.getSearchString().startsWith("-")) {
                searchParametersV4.setSearchString(searchParametersV4.getSearchString().substring(1));
                negate = searchParametersV4.getSearchString().length() > 0;
            }

            if (!root.doForEachChild(new EntrySearchHandlerV4(searchParametersV4, pgNew), null)) {
                childEntries = null;
                break;
            }

            List<PwEntryV4> complement = new ArrayList<>();
            if (negate) {
                for (PwEntryV4 entry: childEntries) {
                    if (!pgNew.contains(entry)) {
                        complement.add(entry);
                    }
                }
                childEntries = complement;
            }
            else {
                childEntries = pgNew;
            }
        }

        if (childEntries != null) {
            listStorage.addAll(childEntries);
        }
        searchParametersV4.setSearchString(fullSearch);
    }
}
