package org.bouncycastle.asn1.cryptopro;

import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

@SuppressWarnings("unchecked")
public class ECGOST3410ParamSetParameters
    extends ASN1Encodable
{
    DERInteger      p, q, a, b, x, y;

    public static ECGOST3410ParamSetParameters getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ECGOST3410ParamSetParameters getInstance(
        Object obj)
    {
        if(obj == null || obj instanceof ECGOST3410ParamSetParameters)
        {
            return (ECGOST3410ParamSetParameters)obj;
        }

        if(obj instanceof ASN1Sequence)
        {
            return new ECGOST3410ParamSetParameters((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid GOST3410Parameter: " + obj.getClass().getName());
    }

    public ECGOST3410ParamSetParameters(
        BigInteger a,
        BigInteger b,
        BigInteger p,
        BigInteger q,
        int        x,
        BigInteger y)
    {
        this.a = new DERInteger(a);
        this.b = new DERInteger(b);
        this.p = new DERInteger(p);
        this.q = new DERInteger(q);
        this.x = new DERInteger(x);
        this.y = new DERInteger(y);
    }

    public ECGOST3410ParamSetParameters(
        ASN1Sequence  seq)
    {
        Enumeration     e = seq.getObjects();

        a = (DERInteger)e.nextElement();
        b = (DERInteger)e.nextElement();
        p = (DERInteger)e.nextElement();
        q = (DERInteger)e.nextElement();
        x = (DERInteger)e.nextElement();
        y = (DERInteger)e.nextElement();
    }
    
    public BigInteger getP()
    {
        return p.getPositiveValue();
    }

    public BigInteger getQ()
    {
        return q.getPositiveValue();
    }

    public BigInteger getA()
    {
        return a.getPositiveValue();
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(a);
        v.add(b);
        v.add(p);
        v.add(q);
        v.add(x);
        v.add(y);

        return new DERSequence(v);
    }
}
