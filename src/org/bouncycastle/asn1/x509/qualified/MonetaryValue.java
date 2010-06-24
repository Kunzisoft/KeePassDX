package org.bouncycastle.asn1.x509.qualified;

import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

/**
 * The MonetaryValue object.
 * <pre>
 * MonetaryValue  ::=  SEQUENCE {
 *       currency              Iso4217CurrencyCode,
 *       amount               INTEGER, 
 *       exponent             INTEGER }
 * -- value = amount * 10^exponent
 * </pre>
 */
@SuppressWarnings("unchecked")
public class MonetaryValue 
    extends ASN1Encodable
{
    Iso4217CurrencyCode currency;
    DERInteger          amount;
    DERInteger          exponent;
        
    public static MonetaryValue getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof MonetaryValue)
        {
            return (MonetaryValue)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new MonetaryValue(ASN1Sequence.getInstance(obj));            
        }
        
        throw new IllegalArgumentException("unknown object in getInstance");
    }
        
    public MonetaryValue(
        ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();    
        // currency
        currency = Iso4217CurrencyCode.getInstance(e.nextElement());
        // hashAlgorithm
        amount = DERInteger.getInstance(e.nextElement());
        // exponent
        exponent = DERInteger.getInstance(e.nextElement());            
    }
        
    public MonetaryValue(
        Iso4217CurrencyCode currency, 
        int                 amount, 
        int                 exponent)
    {    
        this.currency = currency;
        this.amount = new DERInteger(amount);
        this.exponent = new DERInteger(exponent);                  
    }                    
             
    public Iso4217CurrencyCode getCurrency()
    {
        return currency;
    }
        
    public BigInteger getAmount()
    {
        return amount.getValue();
    }
        
    public BigInteger getExponent()
    {
        return exponent.getValue();
    }   
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(currency);
        seq.add(amount);
        seq.add(exponent); 
        
        return new DERSequence(seq);
    }
}
