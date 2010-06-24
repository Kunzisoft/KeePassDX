package org.bouncycastle.asn1.util;

import java.io.IOException;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERApplicationSpecific;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BERConstructedSequence;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERConstructedSet;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEREnumerated;
import org.bouncycastle.asn1.DERExternal;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERT61String;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DERUnknownTag;
import org.bouncycastle.asn1.DERVisibleString;
import org.bouncycastle.util.encoders.Hex;

@SuppressWarnings({ "unchecked", "deprecation" })
public class ASN1Dump
{
    private static final String  TAB = "    ";
    private static final int SAMPLE_SIZE = 32;

    /**
     * dump a DER object as a formatted string with indentation
     *
     * @param obj the DERObject to be dumped out.
     */
	static void _dumpAsString(
        String      indent,
        boolean     verbose,
        DERObject   obj,
        StringBuffer    buf)
    {
        String nl = System.getProperty("line.separator");
        if (obj instanceof ASN1Sequence)
        {
            Enumeration     e = ((ASN1Sequence)obj).getObjects();
            String          tab = indent + TAB;

            buf.append(indent);
            if (obj instanceof BERConstructedSequence)
            {
                buf.append("BER ConstructedSequence");
            }
            else if (obj instanceof DERConstructedSequence)
            {
                buf.append("DER ConstructedSequence");
            }
            else if (obj instanceof BERSequence)
            {
                buf.append("BER Sequence");
            }
            else if (obj instanceof DERSequence)
            {
                buf.append("DER Sequence");
            }
            else
            {
                buf.append("Sequence");
            }

            buf.append(nl);

            while (e.hasMoreElements())
            {
                Object  o = e.nextElement();

                if (o == null || o.equals(new DERNull()))
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                }
                else if (o instanceof DERObject)
                {
                    _dumpAsString(tab, verbose, (DERObject)o, buf);
                }
                else
                {
                    _dumpAsString(tab, verbose, ((DEREncodable)o).getDERObject(), buf);
                }
            }
        }
        else if (obj instanceof DERTaggedObject)
        {
            String          tab = indent + TAB;

            buf.append(indent);
            if (obj instanceof BERTaggedObject)
            {
                buf.append("BER Tagged [");
            }
            else
            {
                buf.append("Tagged [");
            }

            DERTaggedObject o = (DERTaggedObject)obj;

            buf.append(Integer.toString(o.getTagNo()));
            buf.append(']');

            if (!o.isExplicit())
            {
                buf.append(" IMPLICIT ");
            }

            buf.append(nl);

            if (o.isEmpty())
            {
                buf.append(tab);
                buf.append("EMPTY");
                buf.append(nl);
            }
            else
            {
                _dumpAsString(tab, verbose, o.getObject(), buf);
            }
        }
        else if (obj instanceof DERConstructedSet)
        {
            Enumeration     e = ((ASN1Set)obj).getObjects();
            String          tab = indent + TAB;

            buf.append(indent);
            buf.append("ConstructedSet");
            buf.append(nl);

            while (e.hasMoreElements())
            {
                Object  o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                }
                else if (o instanceof DERObject)
                {
                    _dumpAsString(tab, verbose, (DERObject)o, buf);
                }
                else
                {
                    _dumpAsString(tab, verbose, ((DEREncodable)o).getDERObject(), buf);
                }
            }
        }
        else if (obj instanceof BERSet)
        {
            Enumeration     e = ((ASN1Set)obj).getObjects();
            String          tab = indent + TAB;

            buf.append(indent);
            buf.append("BER Set");
            buf.append(nl);

            while (e.hasMoreElements())
            {
                Object  o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                }
                else if (o instanceof DERObject)
                {
                    _dumpAsString(tab, verbose, (DERObject)o, buf);
                }
                else
                {
                    _dumpAsString(tab, verbose, ((DEREncodable)o).getDERObject(), buf);
                }
            }
        }
        else if (obj instanceof DERSet)
        {
            Enumeration     e = ((ASN1Set)obj).getObjects();
            String          tab = indent + TAB;

            buf.append(indent);
            buf.append("DER Set");
            buf.append(nl);

            while (e.hasMoreElements())
            {
                Object  o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                }
                else if (o instanceof DERObject)
                {
                    _dumpAsString(tab, verbose, (DERObject)o, buf);
                }
                else
                {
                    _dumpAsString(tab, verbose, ((DEREncodable)o).getDERObject(), buf);
                }
            }
        }
        else if (obj instanceof DERObjectIdentifier)
        {
            buf.append(indent + "ObjectIdentifier(" + ((DERObjectIdentifier)obj).getId() + ")" + nl);
        }
        else if (obj instanceof DERBoolean)
        {
            buf.append(indent + "Boolean(" + ((DERBoolean)obj).isTrue() + ")" + nl);
        }
        else if (obj instanceof DERInteger)
        {
            buf.append(indent + "Integer(" + ((DERInteger)obj).getValue() + ")" + nl);
        }
        else if (obj instanceof BERConstructedOctetString)
        {
            ASN1OctetString oct = (ASN1OctetString)obj;
            buf.append(indent + "BER Constructed Octet String" + "[" + oct.getOctets().length + "] ");
            if (verbose)
            {
                buf.append(dumpBinaryDataAsString(indent, oct.getOctets()));
            }
            else{
                buf.append(nl);
            }
        }
        else if (obj instanceof DEROctetString)
        {
            ASN1OctetString oct = (ASN1OctetString)obj;
            buf.append(indent + "DER Octet String" + "[" + oct.getOctets().length + "] ");
            if (verbose)
            {
                buf.append(dumpBinaryDataAsString(indent, oct.getOctets()));
            }
            else{
                buf.append(nl);
            }
        }
        else if (obj instanceof DERBitString)
        {
            DERBitString bt = (DERBitString)obj;
            buf.append(indent + "DER Bit String" + "[" + bt.getBytes().length + ", " + bt.getPadBits() + "] ");
            if (verbose)
            {
                buf.append(dumpBinaryDataAsString(indent, bt.getBytes()));
            }
            else{
                buf.append(nl);
            }
        }
        else if (obj instanceof DERIA5String)
        {
            buf.append(indent + "IA5String(" + ((DERIA5String)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERUTF8String)
        {
            buf.append(indent + "UTF8String(" + ((DERUTF8String)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERPrintableString)
        {
            buf.append(indent + "PrintableString(" + ((DERPrintableString)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERVisibleString)
        {
            buf.append(indent + "VisibleString(" + ((DERVisibleString)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERBMPString)
        {
            buf.append(indent + "BMPString(" + ((DERBMPString)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERT61String)
        {
            buf.append(indent + "T61String(" + ((DERT61String)obj).getString() + ") " + nl);
        }
        else if (obj instanceof DERUTCTime)
        {
            buf.append(indent + "UTCTime(" + ((DERUTCTime)obj).getTime() + ") " + nl);
        }
        else if (obj instanceof DERGeneralizedTime)
        {
            buf.append(indent + "GeneralizedTime(" + ((DERGeneralizedTime)obj).getTime() + ") " + nl);
        }
        else if (obj instanceof DERUnknownTag)
        {
            buf.append(indent + "Unknown " + Integer.toString(((DERUnknownTag)obj).getTag(), 16) + " " + new String(Hex.encode(((DERUnknownTag)obj).getData())) + nl);
        }
        else if (obj instanceof BERApplicationSpecific)
        {
            buf.append(outputApplicationSpecific("BER", indent, verbose, obj, nl));
        }
        else if (obj instanceof DERApplicationSpecific)
        {
            buf.append(outputApplicationSpecific("DER", indent, verbose, obj, nl));
        }
        else if (obj instanceof DEREnumerated)
        {
            DEREnumerated en = (DEREnumerated) obj;
            buf.append(indent + "DER Enumerated(" + en.getValue() + ")" + nl);
        }
        else if (obj instanceof DERExternal)
        {
            DERExternal ext = (DERExternal) obj;
            buf.append(indent + "External " + nl);
            String          tab = indent + TAB;
            if (ext.getDirectReference() != null)
            {
                buf.append(tab + "Direct Reference: " + ext.getDirectReference().getId() + nl);
            }
            if (ext.getIndirectReference() != null)
            {
                buf.append(tab + "Indirect Reference: " + ext.getIndirectReference().toString() + nl);
            }
            if (ext.getDataValueDescriptor() != null)
            {
                _dumpAsString(tab, verbose, ext.getDataValueDescriptor(), buf);
            }
            buf.append(tab + "Encoding: " + ext.getEncoding() + nl);
            _dumpAsString(tab, verbose, ext.getExternalContent(), buf);
        }
        else
        {
            buf.append(indent + obj.toString() + nl);
        }
    }
    
    private static String outputApplicationSpecific(String type, String indent, boolean verbose, DERObject obj, String nl)
    {
        DERApplicationSpecific app = (DERApplicationSpecific)obj;
        StringBuffer buf = new StringBuffer();

        if (app.isConstructed())
        {
            try
            {
                ASN1Sequence s = ASN1Sequence.getInstance(app.getObject(DERTags.SEQUENCE));
                buf.append(indent + type + " ApplicationSpecific[" + app.getApplicationTag() + "]" + nl);
                for (Enumeration e = s.getObjects(); e.hasMoreElements();)
                {
                    _dumpAsString(indent + TAB, verbose, (DERObject)e.nextElement(), buf);
                }
            }
            catch (IOException e)
            {
                buf.append(e);
            }
            return buf.toString();
        }

        return indent + type + " ApplicationSpecific[" + app.getApplicationTag() + "] (" + new String(Hex.encode(app.getContents())) + ")" + nl;
    }

    /**
     * dump out a DER object as a formatted string, in non-verbose mode.
     *
     * @param obj the DERObject to be dumped out.
     * @return  the resulting string.
     */
    public static String dumpAsString(
        Object   obj)
    {
        return dumpAsString(obj, false);
    }

    /**
     * Dump out the object as a string.
     *
     * @param obj  the object to be dumped
     * @param verbose  if true, dump out the contents of octet and bit strings.
     * @return  the resulting string.
     */
    public static String dumpAsString(
        Object   obj,
        boolean  verbose)
    {
        StringBuffer buf = new StringBuffer();

        if (obj instanceof DERObject)
        {
            _dumpAsString("", verbose, (DERObject)obj, buf);
        }
        else if (obj instanceof DEREncodable)
        {
            _dumpAsString("", verbose, ((DEREncodable)obj).getDERObject(), buf);
        }
        else
        {
            return "unknown object type " + obj.toString();
        }

        return buf.toString();
    }

    private static String dumpBinaryDataAsString(String indent, byte[] bytes)
    {
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();

        indent += TAB;
        
        buf.append(nl);
        for (int i = 0; i < bytes.length; i += SAMPLE_SIZE)
        {
            if (bytes.length - i > SAMPLE_SIZE)
            {
                buf.append(indent);
                buf.append(new String(Hex.encode(bytes, i, SAMPLE_SIZE)));
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, SAMPLE_SIZE));
                buf.append(nl);
            }
            else
            {
                buf.append(indent);
                buf.append(new String(Hex.encode(bytes, i, bytes.length - i)));
                for (int j = bytes.length - i; j != SAMPLE_SIZE; j++)
                {
                    buf.append("  ");
                }
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, bytes.length - i));
                buf.append(nl);
            }
        }
        
        return buf.toString();
    }

    private static String calculateAscString(byte[] bytes, int off, int len)
    {
        StringBuffer buf = new StringBuffer();

        for (int i = off; i != off + len; i++)
        {
            if (bytes[i] >= ' ' && bytes[i] <= '~')
            {
                buf.append((char)bytes[i]);
            }
        }

        return buf.toString();
    }
}
