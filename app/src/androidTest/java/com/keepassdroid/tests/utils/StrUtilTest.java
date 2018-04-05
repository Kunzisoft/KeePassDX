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
package com.keepassdroid.tests.utils;

import java.util.Locale;

import com.keepassdroid.utils.StrUtil;

import junit.framework.TestCase;

public class StrUtilTest extends TestCase {
    private final String text = "AbCdEfGhIj";
    private final String search = "BcDe";
    private final String badSearch = "Ed";

	public void testIndexOfIgnoreCase1() {
		assertEquals(1, StrUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH));
	}

	public void testIndexOfIgnoreCase2() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH), 2);
	}

	public void testIndexOfIgnoreCase3() {
		assertEquals(-1, StrUtil.indexOfIgnoreCase(text, badSearch, Locale.ENGLISH));
	}
	
	private final String repText = "AbCtestingaBc";
	private final String repSearch = "ABc";
	private final String repSearchBad = "CCCCCC";
	private final String repNew = "12345";
	private final String repResult = "12345testing12345";
	public void testReplaceAllIgnoresCase1() {
		assertEquals(repResult, StrUtil.replaceAllIgnoresCase(repText, repSearch, repNew, Locale.ENGLISH));
	}

	public void testReplaceAllIgnoresCase2() {
		assertEquals(repText, StrUtil.replaceAllIgnoresCase(repText, repSearchBad, repNew, Locale.ENGLISH));
	}
}
