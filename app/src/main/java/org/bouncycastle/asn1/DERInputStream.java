package org.bouncycastle.asn1;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Don't use this class. It will eventually disappear, use ASN1InputStream.
 * <br>
 * This class is scheduled for removal.
 * @deprecated use ASN1InputStream
 */
public class DERInputStream
    extends FilterInputStream implements DERTags
{
    /**
     * @deprecated use ASN1InputStream
     */
    public DERInputStream(
        InputStream is)
    {
        super(is);
    }

    protected int readLength()
        throws IOException
    {
        int length = read();
        if (length < 0)
        {
            throw new IOException("EOF found when length expected");
        }

        if (length == 0x80)
        {
            return -1;      // indefinite-length encoding
        }

        if (length > 127)
        {
            int size = length & 0x7f;

            if (size > 4)
            {
                throw new IOException("DER length more than 4 bytes");
            }
            
            length = 0;
            for (int i = 0; i < size; i++)
            {
                int next = read();

                if (next < 0)
                {
                    throw new IOException("EOF found reading length");
                }

                length = (length << 8) + next;
            }
            
            if (length < 0)
            {
                throw new IOException("corrupted stream - negative length found");
            }
        }

        return length;
    }

    protected void readFully(
        byte[]  bytes)
        throws IOException
    {
        int     left = bytes.length;

        if (left == 0)
        {
            return;
        }

        while (left > 0)
        {
            int    l = read(bytes, bytes.length - left, left);
            
            if (l < 0)
            {
                throw new EOFException("unexpected end of stream");
            }
            
            left -= l;
        }
    }

    /**
     * build an object given its tag and a byte stream to construct it
     * from.
     */
    protected DERObject buildObject(
        int       tag,
        byte[]    bytes)
        throws IOException
    {
        switch (tag)
        {
        case NULL:
            return null;   
        case SEQUENCE | CONSTRUCTED:
            ByteArrayInputStream    bIn = new ByteArrayInputStream(bytes);
            BERInputStream          dIn = new BERInputStream(bIn);
            DERConstructedSequence  seq = new DERConstructedSequence();

            try
            {
                for (;;)
                {
                    DERObject   obj = dIn.readObject();

                    seq.addObject(obj);
                }
            }
            catch (EOFException ex)
            {
                return seq;
            }
        case SET | CONSTRUCTED:
            bIn = new ByteArrayInputStream(bytes);
            dIn = new BERInputStream(bIn);

            ASN1EncodableVector    v = new ASN1EncodableVector();

            try
            {
                for (;;)
                {
                    DERObject   obj = dIn.readObject();

                    v.add(obj);
                }
            }
            catch (EOFException ex)
            {
                return new DERConstructedSet(v);
            }
        case BOOLEAN:
            return new DERBoolean(bytes);
        case INTEGER:
            return new DERInteger(bytes);
        case ENUMERATED:
            return new DEREnumerated(bytes);
        case OBJECT_IDENTIFIER:
            return new DERObjectIdentifier(bytes);
        case BIT_STRING:
            int     padBits = bytes[0];
            byte[]  data = new byte[bytes.length - 1];

            System.arraycopy(bytes, 1, data, 0, bytes.length - 1);

            return new DERBitString(data, padBits);
        case UTF8_STRING:
            return new DERUTF8String(bytes);
        case PRINTABLE_STRING:
            return new DERPrintableString(bytes);
        case IA5_STRING:
            return new DERIA5String(bytes);
        case T61_STRING:
            return new DERT61String(bytes);
        case VISIBLE_STRING:
            return new DERVisibleString(bytes);
        case UNIVERSAL_STRING:
            return new DERUniversalString(bytes);
        case GENERAL_STRING:
            return new DERGeneralString(bytes);
        case BMP_STRING:
            return new DERBMPString(bytes);
        case OCTET_STRING:
            return new DEROctetString(bytes);
        case UTC_TIME:
            return new DERUTCTime(bytes);
        case GENERALIZED_TIME:
            return new DERGeneralizedTime(bytes);
        default:
            //
            // with tagged object tag number is bottom 5 bits
            //
            if ((tag & TAGGED) != 0)  
            {
                if ((tag & 0x1f) == 0x1f)
                {
                    throw new IOException("unsupported high tag encountered");
                }

                if (bytes.length == 0)        // empty tag!
                {
                    if ((tag & CONSTRUCTED) == 0)
                    {
                        return new DERTaggedObject(false, tag & 0x1f, new DERNull());
                    }
                    else
                    {
                        return new DERTaggedObject(false, tag & 0x1f, new DERConstructedSequence());
                    }
                }

                //
                // simple type - implicit... return an octet string
                //
                if ((tag & CONSTRUCTED) == 0)
                {
                    return new DERTaggedObject(false, tag & 0x1f, new DEROctetString(bytes));
                }

                bIn = new ByteArrayInputStream(bytes);
                dIn = new BERInputStream(bIn);

                DEREncodable dObj = dIn.readObject();

                //
                // explicitly tagged (probably!) - if it isn't we'd have to
                // tell from the context
                //
                if (dIn.available() == 0)
                {
                    return new DERTaggedObject(tag & 0x1f, dObj);
                }

                //
                // another implicit object, we'll create a sequence...
                //
                seq = new DERConstructedSequence();

                seq.addObject(dObj);

                try
                {
                    for (;;)
                    {
                        dObj = dIn.readObject();

                        seq.addObject(dObj);
                    }
                }
                catch (EOFException ex)
                {
                    // ignore --
                }

                return new DERTaggedObject(false, tag & 0x1f, seq);
            }

            return new DERUnknownTag(tag, bytes);
        }
    }

    public DERObject readObject()
        throws IOException
    {
        int tag = read();
        if (tag == -1)
        {
            throw new EOFException();
        }

        int     length = readLength();
        byte[]  bytes = new byte[length];

        readFully(bytes);

        return buildObject(tag, bytes);
    }
}
