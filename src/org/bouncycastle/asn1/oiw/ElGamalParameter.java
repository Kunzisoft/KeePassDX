package org.bouncycastle.asn1.oiw;

import java.math.*;
import java.util.*;

import org.bouncycastle.asn1.*;

@SuppressWarnings("unchecked")
public class ElGamalParameter
    extends ASN1Encodable
{
    DERInteger      p, g;

    public ElGamalParameter(
        BigInteger  p,
        BigInteger  g)
    {
        this.p = new DERInteger(p);
        this.g = new DERInteger(g);
    }

    public ElGamalParameter(
        ASN1Sequence  seq)
    {
        Enumeration     e = seq.getObjects();

        p = (DERInteger)e.nextElement();
        g = (DERInteger)e.nextElement();
    }

    public BigInteger getP()
    {
        return p.getPositiveValue();
    }

    public BigInteger getG()
    {
        return g.getPositiveValue();
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(p);
        v.add(g);

        return new DERSequence(v);
    }
}
