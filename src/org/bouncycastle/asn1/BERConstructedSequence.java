package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;

/**
 * @deprecated use BERSequence
 */
@SuppressWarnings("unchecked")
public class BERConstructedSequence
    extends DERConstructedSequence
{
    /*
     */
    void encode(
        DEROutputStream out)
        throws IOException
    {
        if (out instanceof ASN1OutputStream || out instanceof BEROutputStream)
        {
            out.write(SEQUENCE | CONSTRUCTED);
            out.write(0x80);
            
            Enumeration e = getObjects();
            while (e.hasMoreElements())
            {
                out.writeObject(e.nextElement());
            }
        
            out.write(0x00);
            out.write(0x00);
        }
        else
        {
            super.encode(out);
        }
    }
}
