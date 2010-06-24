package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;

import java.math.BigInteger;
import java.util.Enumeration;

/**
 * Monetary limit for transactions. The QcEuMonetaryLimit QC statement MUST be
 * used in new certificates in place of the extension/attribute MonetaryLimit
 * since January 1, 2004. For the sake of backward compatibility with
 * certificates already in use, components SHOULD support MonetaryLimit (as well
 * as QcEuLimitValue).
 * <p/>
 * Indicates a monetary limit within which the certificate holder is authorized
 * to act. (This value DOES NOT express a limit on the liability of the
 * certification authority).
 * <p/>
 * <pre>
 *    MonetaryLimitSyntax ::= SEQUENCE
 *    {
 *      currency PrintableString (SIZE(3)),
 *      amount INTEGER,
 *      exponent INTEGER
 *    }
 * </pre>
 * <p/>
 * currency must be the ISO code.
 * <p/>
 * value = amount�10*exponent
 */
@SuppressWarnings("unchecked")
public class MonetaryLimit
    extends ASN1Encodable
{
    DERPrintableString currency;
    DERInteger amount;
    DERInteger exponent;

    public static MonetaryLimit getInstance(Object obj)
    {
        if (obj == null || obj instanceof MonetaryLimit)
        {
            return (MonetaryLimit)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new MonetaryLimit(ASN1Sequence.getInstance(obj));
        }

        throw new IllegalArgumentException("unknown object in getInstance");
    }

    private MonetaryLimit(ASN1Sequence seq)
    {
        if (seq.size() != 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        Enumeration e = seq.getObjects();
        currency = DERPrintableString.getInstance(e.nextElement());
        amount = DERInteger.getInstance(e.nextElement());
        exponent = DERInteger.getInstance(e.nextElement());
    }

    /**
     * Constructor from a given details.
     * <p/>
     * <p/>
     * value = amount�10^exponent
     *
     * @param currency The currency. Must be the ISO code.
     * @param amount   The amount
     * @param exponent The exponent
     */
    public MonetaryLimit(String currency, int amount, int exponent)
    {
        this.currency = new DERPrintableString(currency, true);
        this.amount = new DERInteger(amount);
        this.exponent = new DERInteger(exponent);
    }

    public String getCurrency()
    {
        return currency.getString();
    }

    public BigInteger getAmount()
    {
        return amount.getValue();
    }

    public BigInteger getExponent()
    {
        return exponent.getValue();
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *    MonetaryLimitSyntax ::= SEQUENCE
     *    {
     *      currency PrintableString (SIZE(3)),
     *      amount INTEGER,
     *      exponent INTEGER
     *    }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(currency);
        seq.add(amount);
        seq.add(exponent);

        return new DERSequence(seq);
    }

}
