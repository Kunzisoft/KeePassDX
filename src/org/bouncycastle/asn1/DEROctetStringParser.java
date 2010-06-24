package org.bouncycastle.asn1;

import java.io.InputStream;
import java.io.IOException;

public class DEROctetStringParser
    implements ASN1OctetStringParser
{
    private DefiniteLengthInputStream stream;

    DEROctetStringParser(
        DefiniteLengthInputStream stream)
    {
        this.stream = stream;
    }

    public InputStream getOctetStream()
    {
        return stream;
    }

    public DERObject getDERObject()
    {
        try
        {
            return new DEROctetString(stream.toByteArray());
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException("IOException converting stream to byte array: " + e.getMessage(), e);
        }
    }
}
