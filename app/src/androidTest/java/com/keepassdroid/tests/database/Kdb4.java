/*
 * Copyright 2010-2016 Brian Pellin.
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
package com.keepassdroid.tests.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.load.ImporterFactory;
import com.keepassdroid.database.load.ImporterV4;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.database.save.PwDbV4Output;
import com.keepassdroid.stream.CopyInputStream;
import com.keepassdroid.tests.TestUtil;

public class Kdb4 extends AndroidTestCase {

	public void testDetection() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);
		
		Importer importer = ImporterFactory.createImporter(is);
		
		assertTrue(importer instanceof ImporterV4);
		is.close();
		
	}
	
	public void testParsing() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		importer.openDatabase(is, "12345", null);
		
		is.close();
		
		
	}
	
	public void testSaving() throws IOException, InvalidDBException, PwDbOutputException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		PwDatabaseV4 db = importer.openDatabase(is, "12345", null);
		is.close();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		PwDbV4Output output =  (PwDbV4Output) PwDbOutput.getInstance(db, bos);
		output.output();
		
		byte[] data = bos.toByteArray();
		
		FileOutputStream fos = new FileOutputStream("/sdcard/test-out.kdbx", false);
		
		InputStream bis = new ByteArrayInputStream(data);
		bis = new CopyInputStream(bis, fos);
		importer = new ImporterV4();
		db = importer.openDatabase(bis, "12345", null);
		bis.close();
		
		fos.close();
		
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		TestUtil.extractKey(getContext(), "keyfile.key", "/sdcard/key");
		TestUtil.extractKey(getContext(), "binary.key", "/sdcard/key-binary");
	}

	public void testComposite() throws IOException, InvalidDBException {
		Context ctx = getContext();

		AssetManager am = ctx.getAssets();
		InputStream is = am.open("keyfile.kdbx", AssetManager.ACCESS_STREAMING);

		ImporterV4 importer = new ImporterV4();
		importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx,"/sdcard/key"));

		is.close();

	}

	public void testCompositeBinary() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("keyfile-binary.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx,"/sdcard/key-binary"));
		
		is.close();
		
	}
	
	public void testKeyfile() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("key-only.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		importer.openDatabase(is, "", TestUtil.getKeyFileInputStream(ctx, "/sdcard/key"));
		
		is.close();
		
		
	}

	public void testNoGzip() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("no-encrypt.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();
		importer.openDatabase(is, "12345", null);
		
		is.close();
		
		
	}
	
}
