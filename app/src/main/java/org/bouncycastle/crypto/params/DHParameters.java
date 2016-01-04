package org.bouncycastle.crypto.params;

import org.bouncycastle.crypto.CipherParameters;

import java.math.BigInteger;

public class DHParameters
    implements CipherParameters
{
    private static final int DEFAULT_MINIMUM_LENGTH = 160;

    // not final due to compiler bug in "simpler" JDKs
    private BigInteger              g;
    private BigInteger              p;
    private BigInteger              q;
    private BigInteger              j;
    private int                     m;
    private int                     l;
    private DHValidationParameters  validation;

    private static int getDefaultMParam(
        int lParam)
    {
        if (lParam == 0)
        {
            return DEFAULT_MINIMUM_LENGTH;
        }

        return lParam < DEFAULT_MINIMUM_LENGTH ? lParam : DEFAULT_MINIMUM_LENGTH;
    }

    public DHParameters(
        BigInteger  p,
        BigInteger  g)
    {
        this(p, g, null, 0);
    }

    public DHParameters(
        BigInteger  p,
        BigInteger  g,
        BigInteger  q)
    {
        this(p, g, q, 0);
    }

    public DHParameters(
        BigInteger  p,
        BigInteger  g,
        BigInteger  q,
        int         l)
    {
        this(p, g, q, getDefaultMParam(l), l, null, null);
    }

    public DHParameters(
        BigInteger  p,
        BigInteger  g,
        BigInteger  q,
        int         m,
        int         l)
    {
        this(p, g, q, m, l, null, null);
    }

    public DHParameters(
        BigInteger              p,
        BigInteger              g,
        BigInteger              q,
        BigInteger              j,
        DHValidationParameters  validation)
    {
        this(p, g, q, DEFAULT_MINIMUM_LENGTH, 0, j, validation);
    }

    public DHParameters(
        BigInteger              p,
        BigInteger              g,
        BigInteger              q,
        int                     m,
        int                     l,
        BigInteger              j,
        DHValidationParameters  validation)
    {
        if (l != 0)
        {
            if (l >= p.bitLength())
            {
                throw new IllegalArgumentException("when l value specified, it must be less than bitlength(p)");
            }
            if (l < m)
            {
                throw new IllegalArgumentException("when l value specified, it may not be less than m value");
            }
        }

        this.g = g;
        this.p = p;
        this.q = q;
        this.m = m;
        this.l = l;
        this.j = j;
        this.validation = validation;
    }

    public BigInteger getP()
    {
        return p;
    }

    public BigInteger getG()
    {
        return g;
    }

    public BigInteger getQ()
    {
        return q;
    }

    /**
     * Return the subgroup factor J.
     *
     * @return subgroup factor
     */
    public BigInteger getJ()
    {
        return j;
    }

    /**
     * Return the minimum length of the private value.
     *
     * @return the minimum length of the private value in bits.
     */
    public int getM()
    {
        return m;
    }

    /**
     * Return the private value length in bits - if set, zero otherwise
     *
     * @return the private value length in bits, zero otherwise.
     */
    public int getL()
    {
        return l;
    }

    public DHValidationParameters getValidationParameters()
    {
        return validation;
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof DHParameters))
        {
            return false;
        }

        DHParameters    pm = (DHParameters)obj;

        if (this.getQ() != null)
        {
            if (!this.getQ().equals(pm.getQ()))
            {
                return false;
            }
        }
        else
        {
            if (pm.getQ() != null)
            {
                return false;
            }
        }

        return pm.getP().equals(p) && pm.getG().equals(g);
    }
    
    public int hashCode()
    {
        return getP().hashCode() ^ getG().hashCode() ^ (getQ() != null ? getQ().hashCode() : 0);
    }
}
