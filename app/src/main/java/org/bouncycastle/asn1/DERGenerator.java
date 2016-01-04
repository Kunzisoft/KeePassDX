package org.bouncycastle.asn1;

import org.bouncycastle.util.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DERGenerator
    extends ASN1Generator
{       
    private boolean      _tagged = false;
    private boolean      _isExplicit;
    private int          _tagNo;
    
    protected DERGenerator(
        OutputStream out)
    {
        super(out);
    }

    public DERGenerator(
        OutputStream out,
        int          tagNo,
        boolean      isExplicit)
    { 
        super(out);
        
        _tagged = true;
        _isExplicit = isExplicit;
        _tagNo = tagNo;
    }

    private void writeLength(
        OutputStream out,
        int          length)
        throws IOException
    {
        if (length > 127)
        {
            int size = 1;
            int val = length;

            while ((val >>>= 8) != 0)
            {
                size++;
            }

            out.write((byte)(size | 0x80));

            for (int i = (size - 1) * 8; i >= 0; i -= 8)
            {
                out.write((byte)(length >> i));
            }
        }
        else
        {
            out.write((byte)length);
        }
    }

    void writeDEREncoded(
        OutputStream out,
        int          tag,
        byte[]       bytes)
        throws IOException
    {
        out.write(tag);
        writeLength(out, bytes.length);
        out.write(bytes);
    }

    void writeDEREncoded(
        int       tag,
        byte[]    bytes)
        throws IOException
    {
        if (_tagged)
        {
            int tagNum = _tagNo | DERTags.TAGGED;
            
            if (_isExplicit)
            {
                int newTag = _tagNo | DERTags.CONSTRUCTED | DERTags.TAGGED;

                ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                
                writeDEREncoded(bOut, tag, bytes);
                
                writeDEREncoded(_out, newTag, bOut.toByteArray());
            }
            else
            {   
                if ((tag & DERTags.CONSTRUCTED) != 0)
                {
                    writeDEREncoded(_out, tagNum | DERTags.CONSTRUCTED, bytes);
                }
                else
                {
                    writeDEREncoded(_out, tagNum, bytes);
                }
            }
        }
        else
        {
            writeDEREncoded(_out, tag, bytes);
        }
    }
    
    void writeDEREncoded(
        OutputStream out,
        int          tag,
        InputStream  in)
        throws IOException
    {
        writeDEREncoded(out, tag, Streams.readAll(in));
    }
}
