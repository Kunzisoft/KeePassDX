/*
 * Copyright 2013-2014 Brian Pellin.
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
package com.keepassdroid.database;

/**
 * @author bpellin
 * Parameters for searching strings in the database.
 *
 */
public class SearchParameters implements Cloneable {
	public static final SearchParameters DEFAULT = new SearchParameters();
	
	public String searchString;
	
	public boolean regularExpression = false;
	public boolean searchInTitles = true;
	public boolean searchInUserNames = true;
	public boolean searchInPasswords = false;
	public boolean searchInUrls = true;
	public boolean searchInGroupNames = false;
	public boolean searchInNotes = true;
	public boolean ignoreCase = true;
	public boolean ignoreExpired = false;
	public boolean respectEntrySearchingDisabled = true;
	public boolean excludeExpired = false;
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public void setupNone() {
		searchInTitles = false;
		searchInUserNames = false;
		searchInPasswords = false;
		searchInUrls = false;
		searchInGroupNames = false;
		searchInNotes = false;
	}
}
