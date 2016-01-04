package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.OutputStream;

public class BEROctetStringGenerator
    extends BERGenerator
{
    public BEROctetStringGenerator(OutputStream out) 
        throws IOException
    {
        super(out);
        
        writeBERHeader(DERTags.CONSTRUCTED | DERTags.OCTET_STRING);
    }

    public BEROctetStringGenerator(
        OutputStream out,
        int tagNo,
        boolean isExplicit) 
        throws IOException
    {
        super(out, tagNo, isExplicit);
        
        writeBERHeader(DERTags.CONSTRUCTED | DERTags.OCTET_STRING);
    }
    
    public OutputStream getOctetOutputStream()
    {
        return getOctetOutputStream(new byte[1000]); // limit for CER encoding.
    }

    public OutputStream getOctetOutputStream(
        byte[] buf)
    {
        return new BufferedBEROctetStream(buf);
    }
   
    private class BufferedBEROctetStream
        extends OutputStream
    {
        private byte[] _buf;
        private int    _off;
        private DEROutputStream _derOut;

        BufferedBEROctetStream(
            byte[] buf)
        {
            _buf = buf;
            _off = 0;
            _derOut = new DEROutputStream(_out);
        }
        
        public void write(
            int b)
            throws IOException
        {
            _buf[_off++] = (byte)b;

            if (_off == _buf.length)
            {
                DEROctetString.encode(_derOut, _buf);
                _off = 0;
            }
        }

        public void write(byte[] b, int off, int len) throws IOException
        {
            while (len > 0)
            {
                int numToCopy = Math.min(len, _buf.length - _off);
                System.arraycopy(b, off, _buf, _off, numToCopy);

                _off += numToCopy;
                if (_off < _buf.length)
                {
                    break;
                }

                DEROctetString.encode(_derOut, _buf);
                _off = 0;

                off += numToCopy;
                len -= numToCopy;
            }
        }

        public void close() 
            throws IOException
        {
            if (_off != 0)
            {
                byte[] bytes = new byte[_off];
                System.arraycopy(_buf, 0, bytes, 0, _off);
                
                DEROctetString.encode(_derOut, bytes);
            }
            
             writeBEREnd();
        }
    }
}
