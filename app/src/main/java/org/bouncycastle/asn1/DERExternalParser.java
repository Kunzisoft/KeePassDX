package org.bouncycastle.asn1;

import java.io.IOException;

public class DERExternalParser
    implements DEREncodable
{
    private ASN1StreamParser _parser;

    /**
     * 
     */
    public DERExternalParser(ASN1StreamParser parser)
    {
        this._parser = parser;
    }

    public DEREncodable readObject()
        throws IOException
    {
        return _parser.readObject();
    }
    
    public DERObject getDERObject()
    {
        try 
        {
            return new DERExternal(_parser.readVector());
        }
        catch (IOException ioe) 
        {
            throw new ASN1ParsingException("unable to get DER object", ioe);
        }
        catch (IllegalArgumentException ioe) 
        {
            throw new ASN1ParsingException("unable to get DER object", ioe);
        }
    }
}
