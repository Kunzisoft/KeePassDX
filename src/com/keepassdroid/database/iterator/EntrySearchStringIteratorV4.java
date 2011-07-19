package com.keepassdroid.database.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import com.keepassdroid.database.PwEntryV4;

public class EntrySearchStringIteratorV4 extends EntrySearchStringIterator {
	
	private String current;
	private Iterator<Entry<String, String>> setIterator;

	public EntrySearchStringIteratorV4(PwEntryV4 entry) {
		setIterator = entry.strings.entrySet().iterator();
		advance();
	}

	@Override
	public boolean hasNext() {
		return current != null;
	}

	@Override
	public String next() {
		if (current == null) {
			throw new NoSuchElementException("Past the end of the list.");
		}
		
		String next = current;
		advance();
		return next;
	}
	
	private void advance() {
		if (!setIterator.hasNext()) {
			current = null;
			return;
		}
		
		Entry<String, String> entry = setIterator.next();
		
		// Skip password entries
		while (entry.getKey().equals(PwEntryV4.STR_PASSWORD)) {
			if (!setIterator.hasNext()) {
				current = null;
				return;
			}
			
			entry = setIterator.next();
		}
		
		current = entry.getValue();
	}

}
