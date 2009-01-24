package org.bouncycastle1.util;

/**
 * General array utilities.
 */
public final class Arrays
{
    private Arrays() 
    {
        // static class, hide constructor
    }
    
    public static boolean areEqual(
        byte[]  a,
        byte[]  b)
    {
        if (a == b)
        {
            return true;
        }
        
        if (a.length != b.length)
        {
            return false;
        }

        for (int i = 0; i != a.length; i++)
        {
            if (a[i] != b[i])
            {
                return false;
            }
        }

        return true;
    }
    
    public static void fill(
        byte[] array,
        byte value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }
    
    public static void fill(
        long[] array,
        long value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }

    public static void fill(
        short[] array, 
        short value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }

    public static int hashCode(byte[] data)
    {
        int     value = 0;

        for (int i = 0; i != data.length; i++)
        {
            value ^= (data[i] & 0xff) << (i % 4);
        }

        return value;
    }

    public static byte[] clone(byte[] data)
    {
        byte[] copy = new byte[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }
}
