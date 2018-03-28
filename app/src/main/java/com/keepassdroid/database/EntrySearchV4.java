package com.keepassdroid.database;

import com.keepassdroid.utils.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntrySearchV4 {

    private PwGroupV4 root;

    public EntrySearchV4(PwGroupV4 root) {
        this.root = root;
    }

    public void searchEntries(SearchParameters sp, List<PwEntryV4> listStorage) {
        if (sp == null)  { return; }
        if (listStorage == null) { return; }

        List<String> terms = StrUtil.splitSearchTerms(sp.searchString);
        if (terms.size() <= 1 || sp.regularExpression) {
            searchEntriesSingle(sp, listStorage);
            return;
        }

        // Search longest term first
        Comparator<String> stringLengthComparator = (lhs, rhs) -> lhs.length() - rhs.length();
        Collections.sort(terms, stringLengthComparator);

        String fullSearch = sp.searchString;
        List<PwEntryV4> pg = root.getChildEntries();
        for (int i = 0; i < terms.size(); i ++) {
            List<PwEntryV4> pgNew = new ArrayList<>();

            sp.searchString = terms.get(i);

            boolean negate = false;
            if (sp.searchString.startsWith("-")) {
                sp.searchString.substring(1); // TODO Verify bug or no need ?
                negate = sp.searchString.length() > 0;
            }

            if (!searchEntriesSingle(sp, pgNew)) {
                pg = null;
                break;
            }

            List<PwEntryV4> complement = new ArrayList<>();
            if (negate) {
                for (PwEntryV4 entry: pg) {
                    if (!pgNew.contains(entry)) {
                        complement.add(entry);
                    }
                }
                pg = complement;
            }
            else {
                pg = pgNew;
            }
        }

        if (pg != null) {
            listStorage.addAll(pg);
        }
        sp.searchString = fullSearch;

    }

    private boolean searchEntriesSingle(SearchParameters spIn, List<PwEntryV4> listStorage) {
        SearchParameters sp = (SearchParameters) spIn.clone();
        EntryHandler<PwEntryV4> eh;
        if (sp.searchString.length() <= 0) {
            eh = new EntrySearchHandlerAll<>(sp, listStorage);
        } else {
            eh = new EntrySearchHandlerV4(sp, listStorage);
        }
        return root.preOrderTraverseTree(null, eh);
    }

}
