/*
 * Copyright 2014 Brian Pellin.
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
package com.keepassdroid.tests.utils;

import com.keepassdroid.utils.StrUtil;

import junit.framework.TestCase;

public class StrUtilTest extends TestCase {
    private final String text = "AbCdEfGhIj";
    private final String search = "BcDe";
    private final String badSearch = "Ed";

	public void testIndexOfIgnoreCase1() {
		assertEquals(1, StrUtil.indexOfIgnoreCase(text, search));
	}

	public void testIndexOfIgnoreCase2() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, search), 2);
	}

	public void testIndexOfIgnoreCase3() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, badSearch));
	}
	
	private final String repText = "AbCtestingaBc";
	private final String repSearch = "ABc";
	private final String repSearchBad = "CCCCCC";
	private final String repNew = "12345";
	private final String repResult = "12345testing12345";
	public void testReplaceAllIgnoresCase1() {
		assertEquals(repResult, StrUtil.replaceAllIgnoresCase(repText, repSearch, repNew));
	}

	public void testReplaceAllIgnoresCase2() {
		assertEquals(repText, StrUtil.replaceAllIgnoresCase(repText, repSearchBad, repNew));
	}
}
