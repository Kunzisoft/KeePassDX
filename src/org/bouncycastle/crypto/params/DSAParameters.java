package org.bouncycastle.crypto.params;

import java.math.BigInteger;

import org.bouncycastle.crypto.CipherParameters;

public class DSAParameters
    implements CipherParameters
{
    private BigInteger              g;
    private BigInteger              q;
    private BigInteger              p;
    private DSAValidationParameters validation;

    public DSAParameters(
        BigInteger  p,
        BigInteger  q,
        BigInteger  g)
    {
        this.g = g;
        this.p = p;
        this.q = q;
    }   

    public DSAParameters(
        BigInteger              p,
        BigInteger              q,
        BigInteger              g,
        DSAValidationParameters params)
    {
        this.g = g;
        this.p = p;
        this.q = q;
        this.validation = params;
    }   

    public BigInteger getP()
    {
        return p;
    }

    public BigInteger getQ()
    {
        return q;
    }

    public BigInteger getG()
    {
        return g;
    }

    public DSAValidationParameters getValidationParameters()
    {
        return validation;
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof DSAParameters))
        {
            return false;
        }

        DSAParameters    pm = (DSAParameters)obj;

        return (pm.getP().equals(p) && pm.getQ().equals(q) && pm.getG().equals(g));
    }
    
    public int hashCode()
    {
        return getP().hashCode() ^ getQ().hashCode() ^ getG().hashCode();
    }
}
