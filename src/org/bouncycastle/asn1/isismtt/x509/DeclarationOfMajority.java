package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * A declaration of majority.
 * <p/>
 * <pre>
 *           DeclarationOfMajoritySyntax ::= CHOICE
 *           {
 *             notYoungerThan [0] IMPLICIT INTEGER,
 *             fullAgeAtCountry [1] IMPLICIT SEQUENCE
 *             {
 *               fullAge BOOLEAN DEFAULT TRUE,
 *               country PrintableString (SIZE(2))
 *             }
 *             dateOfBirth [2] IMPLICIT GeneralizedTime
 *           }
 * </pre>
 * <p/>
 * fullAgeAtCountry indicates the majority of the owner with respect to the laws
 * of a specific country.
 */
public class DeclarationOfMajority
    extends ASN1Encodable
    implements ASN1Choice
{
    public static final int notYoungerThan = 0;
    public static final int fullAgeAtCountry = 1;
    public static final int dateOfBirth = 2;

    private ASN1TaggedObject declaration;

    public DeclarationOfMajority(int notYoungerThan)
    {
        declaration = new DERTaggedObject(false, 0, new DERInteger(notYoungerThan));
    }

    public DeclarationOfMajority(boolean fullAge, String country)
    {
        if (country.length() > 2)
        {
            throw new IllegalArgumentException("country can only be 2 characters");
        }

        if (fullAge)
        {
            declaration = new DERTaggedObject(false, 1, new DERSequence(new DERPrintableString(country, true)));
        }
        else
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(DERBoolean.FALSE);
            v.add(new DERPrintableString(country, true));

            declaration = new DERTaggedObject(false, 1, new DERSequence(v));
        }
    }

    public DeclarationOfMajority(DERGeneralizedTime dateOfBirth)
    {
        declaration = new DERTaggedObject(false, 2, dateOfBirth);
    }

    public static DeclarationOfMajority getInstance(Object obj)
    {
        if (obj == null || obj instanceof DeclarationOfMajority)
        {
            return (DeclarationOfMajority)obj;
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return new DeclarationOfMajority((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    private DeclarationOfMajority(ASN1TaggedObject o)
    {
        if (o.getTagNo() > 2)
        {
                throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
        }
        declaration = o;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *           DeclarationOfMajoritySyntax ::= CHOICE
     *           {
     *             notYoungerThan [0] IMPLICIT INTEGER,
     *             fullAgeAtCountry [1] IMPLICIT SEQUENCE
     *             {
     *               fullAge BOOLEAN DEFAULT TRUE,
     *               country PrintableString (SIZE(2))
     *             }
     *             dateOfBirth [2] IMPLICIT GeneralizedTime
     *           }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        return declaration;
    }

    public int getType()
    {
        return declaration.getTagNo();
    }

    /**
     * @return notYoungerThan if that's what we are, -1 otherwise
     */
    public int notYoungerThan()
    {
        if (declaration.getTagNo() != 0)
        {
            return -1;
        }

        return DERInteger.getInstance(declaration, false).getValue().intValue();
    }

    public ASN1Sequence fullAgeAtCountry()
    {
        if (declaration.getTagNo() != 1)
        {
            return null;
        }

        return ASN1Sequence.getInstance(declaration, false);
    }

    public DERGeneralizedTime getDateOfBirth()
    {
        if (declaration.getTagNo() != 2)
        {
            return null;
        }

        return DERGeneralizedTime.getInstance(declaration, false);
    }
}
