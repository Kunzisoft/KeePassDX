package org.bouncycastle.asn1.x509;

/**
 * class for breaking up an X500 Name into it's component tokens, ala
 * java.util.StringTokenizer. We need this class as some of the
 * lightweight Java environment don't support classes like
 * StringTokenizer.
 */
public class X509NameTokenizer
{
    private String          value;
    private int             index;
    private char            seperator;
    private StringBuffer    buf = new StringBuffer();

    public X509NameTokenizer(
        String  oid)
    {
        this(oid, ',');
    }
    
    public X509NameTokenizer(
        String  oid,
        char    seperator)
    {
        this.value = oid;
        this.index = -1;
        this.seperator = seperator;
    }

    public boolean hasMoreTokens()
    {
        return (index != value.length());
    }

    public String nextToken()
    {
        if (index == value.length())
        {
            return null;
        }

        int     end = index + 1;
        boolean quoted = false;
        boolean escaped = false;

        buf.setLength(0);

        while (end != value.length())
        {
            char    c = value.charAt(end);

            if (c == '"')
            {
                if (!escaped)
                {
                    quoted = !quoted;
                }
                else
                {
                    buf.append(c);
                }
                escaped = false;
            }
            else
            {
                if (escaped || quoted)
                {
                    if (c == '#' && buf.charAt(buf.length() - 1) == '=')
                    {
                        buf.append('\\');
                    }
                    else if (c == '+' && seperator != '+')
                    {
                        buf.append('\\');
                    }
                    buf.append(c);
                    escaped = false;
                }
                else if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == seperator)
                {
                    break;
                }
                else
                {
                    buf.append(c);
                }
            }
            end++;
        }

        index = end;
        return buf.toString().trim();
    }
}
