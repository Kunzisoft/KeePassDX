package com.android.keepass.tests;

import junit.framework.TestCase;

import com.android.keepass.Database;

public class DatabaseTest extends TestCase {
	public void testDatabase()  {
		try {
			Database.LoadData("/sdcard/test1.kdb", "12345", "");
			Database.SaveData("/sdcard/test2.kdb");
		} catch (Exception e) {
			assertTrue(e.getMessage(), true);
		}
	}
}
