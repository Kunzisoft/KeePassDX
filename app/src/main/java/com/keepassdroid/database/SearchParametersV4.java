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

public class SearchParametersV4 extends SearchParameters implements Cloneable {
	public static SearchParametersV4 DEFAULT = new SearchParametersV4();
	
	public boolean searchInOther = true;
	public boolean searchInUUIDs = false;
	public boolean searchInTags = true;

	@Override
	public Object clone() {
		return super.clone();
	}

	@Override
	public void setupNone() {
		super.setupNone();
		searchInOther = false;
		searchInUUIDs = false;
		searchInTags = false;
	}
}
