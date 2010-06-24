package org.bouncycastle.util.io;

import java.io.IOException;

@SuppressWarnings("serial")
public class StreamOverflowException
    extends IOException
{
    public StreamOverflowException(String msg)
    {
        super(msg);
    }
}
