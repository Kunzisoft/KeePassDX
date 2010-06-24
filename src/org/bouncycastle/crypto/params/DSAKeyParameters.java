package org.bouncycastle.crypto.params;

public class DSAKeyParameters
    extends AsymmetricKeyParameter
{
    private DSAParameters    params;

    public DSAKeyParameters(
        boolean         isPrivate,
        DSAParameters   params)
    {
        super(isPrivate);

        this.params = params;
    }   

    public DSAParameters getParameters()
    {
        return params;
    }
}
