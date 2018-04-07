/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
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
