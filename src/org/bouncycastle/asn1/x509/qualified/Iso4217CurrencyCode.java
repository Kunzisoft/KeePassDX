package org.bouncycastle.asn1.x509.qualified;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERPrintableString;

/**
 * The Iso4217CurrencyCode object.
 * <pre>
 * Iso4217CurrencyCode  ::=  CHOICE {
 *       alphabetic              PrintableString (SIZE 3), --Recommended
 *       numeric              INTEGER (1..999) }
 * -- Alphabetic or numeric currency code as defined in ISO 4217
 * -- It is recommended that the Alphabetic form is used
 * </pre>
 */
public class Iso4217CurrencyCode 
    extends ASN1Encodable
    implements ASN1Choice
{
    final int ALPHABETIC_MAXSIZE = 3;
    final int NUMERIC_MINSIZE = 1;
    final int NUMERIC_MAXSIZE = 999;
    
    DEREncodable obj;    
    int          numeric;
    
    public static Iso4217CurrencyCode getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof Iso4217CurrencyCode)
        {
            return (Iso4217CurrencyCode)obj;
        }

        if (obj instanceof DERInteger)
        {
            DERInteger numericobj = DERInteger.getInstance(obj);
            int numeric = numericobj.getValue().intValue();  
            return new Iso4217CurrencyCode(numeric);            
        }
        else
        if (obj instanceof DERPrintableString)
        {
            DERPrintableString alphabetic = DERPrintableString.getInstance(obj);
            return new Iso4217CurrencyCode(alphabetic.getString());
        }
        throw new IllegalArgumentException("unknown object in getInstance");
    }
            
    public Iso4217CurrencyCode(
        int numeric)
    {
        if (numeric > NUMERIC_MAXSIZE || numeric < NUMERIC_MINSIZE)
        {
            throw new IllegalArgumentException("wrong size in numeric code : not in (" +NUMERIC_MINSIZE +".."+ NUMERIC_MAXSIZE +")");
        }
        obj = new DERInteger(numeric);
    }
    
    public Iso4217CurrencyCode(
        String alphabetic)
    {
        if (alphabetic.length() > ALPHABETIC_MAXSIZE)
        {
            throw new IllegalArgumentException("wrong size in alphabetic code : max size is " + ALPHABETIC_MAXSIZE);
        }
        obj = new DERPrintableString(alphabetic);
    }            

    public boolean isAlphabetic()
    {
        return obj instanceof DERPrintableString;
    }
    
    public String getAlphabetic()
    {
        return ((DERPrintableString)obj).getString();
    }
    
    public int getNumeric()
    {
        return ((DERInteger)obj).getValue().intValue();
    }
    
    public DERObject toASN1Object() 
    {    
        return obj.getDERObject();
    }
}
