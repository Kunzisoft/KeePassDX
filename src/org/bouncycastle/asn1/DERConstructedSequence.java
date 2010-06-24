package org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @deprecated use DERSequence.
 */
@SuppressWarnings("unchecked")
public class DERConstructedSequence
    extends ASN1Sequence
{
    public void addObject(
        DEREncodable obj)
    {
        super.addObject(obj);
    }

    public int getSize()
    {
        return size();
    }

    /*
     * A note on the implementation:
     * <p>
     * As DER requires the constructed, definite-length model to
     * be used for structured types, this varies slightly from the
     * ASN.1 descriptions given. Rather than just outputing SEQUENCE,
     * we also have to specify CONSTRUCTED, and the objects length.
     */
    void encode(
        DEROutputStream out)
        throws IOException
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);
        Enumeration             e = this.getObjects();

        while (e.hasMoreElements())
        {
            Object    obj = e.nextElement();

            dOut.writeObject(obj);
        }

        dOut.close();

        byte[]  bytes = bOut.toByteArray();

        out.writeEncoded(SEQUENCE | CONSTRUCTED, bytes);
    }
}
