package org.bouncycastle.asn1.x509;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

/**
 * This extension may contain further X.500 attributes of the subject. See also
 * RFC 3039.
 * 
 * <pre>
 *     SubjectDirectoryAttributes ::= Attributes
 *     Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
 *     Attribute ::= SEQUENCE 
 *     {
 *       type AttributeType 
 *       values SET OF AttributeValue 
 *     }
 *     
 *     AttributeType ::= OBJECT IDENTIFIER
 *     AttributeValue ::= ANY DEFINED BY AttributeType
 * </pre>
 * 
 * @see org.bouncycastle.asn1.x509.X509Name for AttributeType ObjectIdentifiers.
 */
@SuppressWarnings("unchecked")
public class SubjectDirectoryAttributes 
    extends ASN1Encodable
{
    private Vector attributes = new Vector();

    public static SubjectDirectoryAttributes getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SubjectDirectoryAttributes)
        {
            return (SubjectDirectoryAttributes)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new SubjectDirectoryAttributes((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * 
     * The sequence is of type SubjectDirectoryAttributes:
     * 
     * <pre>
     *      SubjectDirectoryAttributes ::= Attributes
     *      Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
     *      Attribute ::= SEQUENCE 
     *      {
     *        type AttributeType 
     *        values SET OF AttributeValue 
     *      }
     *      
     *      AttributeType ::= OBJECT IDENTIFIER
     *      AttributeValue ::= ANY DEFINED BY AttributeType
     * </pre>
     * 
     * @param seq
     *            The ASN.1 sequence.
     */
    public SubjectDirectoryAttributes(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1Sequence s = ASN1Sequence.getInstance(e.nextElement());
            attributes.addElement(new Attribute(s));
        }
    }

    /**
     * Constructor from a vector of attributes.
     * 
     * The vector consists of attributes of type {@link Attribute Attribute}
     * 
     * @param attributes
     *            The attributes.
     * 
     */
    public SubjectDirectoryAttributes(Vector attributes)
    {
        Enumeration e = attributes.elements();

        while (e.hasMoreElements())
        {
            this.attributes.addElement(e.nextElement());
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *      SubjectDirectoryAttributes ::= Attributes
     *      Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
     *      Attribute ::= SEQUENCE 
     *      {
     *        type AttributeType 
     *        values SET OF AttributeValue 
     *      }
     *      
     *      AttributeType ::= OBJECT IDENTIFIER
     *      AttributeValue ::= ANY DEFINED BY AttributeType
     * </pre>
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        Enumeration e = attributes.elements();

        while (e.hasMoreElements())
        {

            vec.add((Attribute)e.nextElement());
        }

        return new DERSequence(vec);
    }

    /**
     * @return Returns the attributes.
     */
    public Vector getAttributes()
    {
        return attributes;
    }
}
