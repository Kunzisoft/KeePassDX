package org.bouncycastle.asn1.util;

import java.io.FileInputStream;

import org.bouncycastle.asn1.ASN1InputStream;

public class Dump
{
    public static void main(
        String args[])
        throws Exception
    {
        FileInputStream fIn = new FileInputStream(args[0]);
        ASN1InputStream bIn = new ASN1InputStream(fIn);
        Object          obj = null;

        while ((obj = bIn.readObject()) != null)
        {
            System.out.println(ASN1Dump.dumpAsString(obj));
        }
    }
}
