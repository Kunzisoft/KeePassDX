package org.phoneid;

public class PhoneIDUtil {

    /**
    * Compare byte arrays
    */
    public static boolean compare(byte[] array1, byte[] array2) 
    {
	if (array1.length != array2.length)
	    return false;

	for (int i=0; i<array1.length; i++)
	    if (array1[i] != array2[i])
		return false;
	
	return true;
    }

    /**
     * fill byte array
     */
    public static void fill(byte[] array, byte value)
    {
	for (int i=0; i<array.length; i++)
	    array[i] = value;
	return;
    }
}

