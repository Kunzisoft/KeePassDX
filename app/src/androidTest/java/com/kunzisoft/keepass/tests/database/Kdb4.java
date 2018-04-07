/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.keepass.tests.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import com.keepass.database.PwDatabaseV4;
import com.keepass.database.exception.InvalidDBException;
import com.keepass.database.exception.PwDbOutputException;
import com.keepass.database.load.Importer;
import com.keepass.database.load.ImporterFactory;
import com.keepass.database.load.ImporterV4;
import com.keepass.database.save.PwDbOutput;
import com.keepass.database.save.PwDbV4Output;
import com.keepass.stream.CopyInputStream;
import com.keepass.tests.TestUtil;

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

    public void testSavingKDBXV3() throws IOException, InvalidDBException, PwDbOutputException {
       testSaving("test.kdbx", "12345", "test-out.kdbx");
    }

    public void testSavingKDBXV4() throws IOException, InvalidDBException, PwDbOutputException {
        testSaving("test-kdbxv4.kdbx", "1", "test-kdbxv4-out.kdbx");
    }

    private void testSaving(String inputFile, String password, String outputFile) throws IOException, InvalidDBException, PwDbOutputException {
        Context ctx = getContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open(inputFile, AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4();
        PwDatabaseV4 db = importer.openDatabase(is, password, null);
        is.close();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        PwDbV4Output output =  (PwDbV4Output) PwDbOutput.getInstance(db, bos);
        output.output();

        byte[] data = bos.toByteArray();

        FileOutputStream fos = new FileOutputStream(TestUtil.getSdPath(outputFile), false);

        InputStream bis = new ByteArrayInputStream(data);
        bis = new CopyInputStream(bis, fos);
        importer = new ImporterV4();
        db = importer.openDatabase(bis, password, null);
        bis.close();

        fos.close();

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TestUtil.extractKey(getContext(), "keyfile.key", TestUtil.getSdPath("key"));
        TestUtil.extractKey(getContext(), "binary.key", TestUtil.getSdPath("key-binary"));
    }

    public void testComposite() throws IOException, InvalidDBException {
        Context ctx = getContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("keyfile.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4();
        importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx, TestUtil.getSdPath("key")));

        is.close();

    }

    public void testCompositeBinary() throws IOException, InvalidDBException {
        Context ctx = getContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("keyfile-binary.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4();
        importer.openDatabase(is, "12345", TestUtil.getKeyFileInputStream(ctx,TestUtil.getSdPath("key-binary")));

        is.close();

    }

    public void testKeyfile() throws IOException, InvalidDBException {
        Context ctx = getContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("key-only.kdbx", AssetManager.ACCESS_STREAMING);

        ImporterV4 importer = new ImporterV4();
        importer.openDatabase(is, "", TestUtil.getKeyFileInputStream(ctx, TestUtil.getSdPath("key")));

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
