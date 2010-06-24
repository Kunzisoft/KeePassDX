package org.bouncycastle.asn1.x9;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

/**
 * ASN.1 def for Elliptic-Curve Field ID structure. See
 * X9.62, for further details.
 */
public class X9FieldID
    extends ASN1Encodable
    implements X9ObjectIdentifiers
{
    private DERObjectIdentifier     id;
    private DERObject               parameters;

    /**
     * Constructor for elliptic curves over prime fields
     * <code>F<sub>2</sub></code>.
     * @param primeP The prime <code>p</code> defining the prime field.
     */
    public X9FieldID(BigInteger primeP)
    {
        this.id = prime_field;
        this.parameters = new DERInteger(primeP);
    }

    /**
     * Constructor for elliptic curves over binary fields
     * <code>F<sub>2<sup>m</sup></sub></code>.
     * @param m  The exponent <code>m</code> of
     * <code>F<sub>2<sup>m</sup></sub></code>.
     * @param k1 The integer <code>k1</code> where <code>x<sup>m</sup> +
     * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
     * represents the reduction polynomial <code>f(z)</code>.
     * @param k2 The integer <code>k2</code> where <code>x<sup>m</sup> +
     * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
     * represents the reduction polynomial <code>f(z)</code>.
     * @param k3 The integer <code>k3</code> where <code>x<sup>m</sup> +
     * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
     * represents the reduction polynomial <code>f(z)</code>..
     */
    public X9FieldID(int m, int k1, int k2, int k3)
    {
        this.id = characteristic_two_field;
        ASN1EncodableVector fieldIdParams = new ASN1EncodableVector();
        fieldIdParams.add(new DERInteger(m));
        
        if (k2 == 0) 
        {
            fieldIdParams.add(tpBasis);
            fieldIdParams.add(new DERInteger(k1));
        } 
        else 
        {
            fieldIdParams.add(ppBasis);
            ASN1EncodableVector pentanomialParams = new ASN1EncodableVector();
            pentanomialParams.add(new DERInteger(k1));
            pentanomialParams.add(new DERInteger(k2));
            pentanomialParams.add(new DERInteger(k3));
            fieldIdParams.add(new DERSequence(pentanomialParams));
        }
        
        this.parameters = new DERSequence(fieldIdParams);
    }

    public X9FieldID(
        ASN1Sequence  seq)
    {
        this.id = (DERObjectIdentifier)seq.getObjectAt(0);
        this.parameters = (DERObject)seq.getObjectAt(1);
    }

    public DERObjectIdentifier getIdentifier()
    {
        return id;
    }

    public DERObject getParameters()
    {
        return parameters;
    }

    /**
     * Produce a DER encoding of the following structure.
     * <pre>
     *  FieldID ::= SEQUENCE {
     *      fieldType       FIELD-ID.&amp;id({IOSet}),
     *      parameters      FIELD-ID.&amp;Type({IOSet}{&#64;fieldType})
     *  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(this.id);
        v.add(this.parameters);

        return new DERSequence(v);
    }
}
