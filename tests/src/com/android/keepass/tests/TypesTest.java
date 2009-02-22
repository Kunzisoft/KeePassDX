package com.android.keepass.tests;

import junit.framework.TestCase;
import org.phoneid.keepassj2me.Types;
import static org.junit.Assert.*;

public class TypesTest extends TestCase {

	public void testReadWriteInt() {
		byte[] orig = new byte[8];
		byte[] dest = new byte[8];
		
		for (int i = 0; i < 4; i++ ) {
			orig[i] = 0;
		}
		
		for (int i = 4; i < 8; i++ ) {
			orig[4] = Byte.MAX_VALUE;

		}
		
		int one = Types.readInt(orig, 0);
		int two = Types.readInt(orig, 4);
		
		Types.writeInt(one, dest, 0);
		Types.writeInt(two, dest, 4);

		assertArrayEquals(orig, dest);
		
	}

	
}
 