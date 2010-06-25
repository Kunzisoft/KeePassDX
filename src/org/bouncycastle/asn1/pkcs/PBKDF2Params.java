package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

@SuppressWarnings("unchecked")
public class PBKDF2Params
    extends ASN1Encodable
{
    ASN1OctetString     octStr;
    DERInteger          iterationCount;
    DERInteger          keyLength;

    public static PBKDF2Params getInstance(
        Object  obj)
    {
        if (obj instanceof PBKDF2Params)
        {
            return (PBKDF2Params)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new PBKDF2Params((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }
    
    public PBKDF2Params(
        byte[]  salt,
        int     iterationCount)
    {
        this.octStr = new DEROctetString(salt);
        this.iterationCount = new DERInteger(iterationCount);
    }
    
    public PBKDF2Params(
        ASN1Sequence  seq)
    {
        Enumeration e = seq.getObjects();

        octStr = (ASN1OctetString)e.nextElement();
        iterationCount = (DERInteger)e.nextElement();

        if (e.hasMoreElements())
        {
            keyLength = (DERInteger)e.nextElement();
        }
        else
        {
            keyLength = null;
        }
    }

    public byte[] getSalt()
    {
        return octStr.getOctets();
    }

    public BigInteger getIterationCount()
    {
        return iterationCount.getValue();
    }

    public BigInteger getKeyLength()
    {
        if (keyLength != null)
        {
            return keyLength.getValue();
        }

        return null;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(octStr);
        v.add(iterationCount);

        if (keyLength != null)
        {
            v.add(keyLength);
        }

        return new DERSequence(v);
    }
}
