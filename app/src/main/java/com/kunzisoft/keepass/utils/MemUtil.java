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
package com.kunzisoft.keepass.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MemUtil {
	public static byte[] decompress(byte[] input) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.copyStream(gzis, baos);
		
		return baos.toByteArray();
	}
	
	public static byte[] compress(byte[] input) throws IOException {		
		ByteArrayInputStream bais = new ByteArrayInputStream(input);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		Util.copyStream(bais, gzos);
		gzos.close();
		
		return baos.toByteArray();
	}

}
