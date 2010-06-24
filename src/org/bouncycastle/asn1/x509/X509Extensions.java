package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

@SuppressWarnings("unchecked")
public class X509Extensions
    extends ASN1Encodable
{
    /**
     * Subject Directory Attributes
     */
    public static final DERObjectIdentifier SubjectDirectoryAttributes = new DERObjectIdentifier("2.5.29.9");
    
    /**
     * Subject Key Identifier 
     */
    public static final DERObjectIdentifier SubjectKeyIdentifier = new DERObjectIdentifier("2.5.29.14");

    /**
     * Key Usage 
     */
    public static final DERObjectIdentifier KeyUsage = new DERObjectIdentifier("2.5.29.15");

    /**
     * Private Key Usage Period 
     */
    public static final DERObjectIdentifier PrivateKeyUsagePeriod = new DERObjectIdentifier("2.5.29.16");

    /**
     * Subject Alternative Name 
     */
    public static final DERObjectIdentifier SubjectAlternativeName = new DERObjectIdentifier("2.5.29.17");

    /**
     * Issuer Alternative Name 
     */
    public static final DERObjectIdentifier IssuerAlternativeName = new DERObjectIdentifier("2.5.29.18");

    /**
     * Basic Constraints 
     */
    public static final DERObjectIdentifier BasicConstraints = new DERObjectIdentifier("2.5.29.19");

    /**
     * CRL Number 
     */
    public static final DERObjectIdentifier CRLNumber = new DERObjectIdentifier("2.5.29.20");

    /**
     * Reason code 
     */
    public static final DERObjectIdentifier ReasonCode = new DERObjectIdentifier("2.5.29.21");

    /**
     * Hold Instruction Code 
     */
    public static final DERObjectIdentifier InstructionCode = new DERObjectIdentifier("2.5.29.23");

    /**
     * Invalidity Date 
     */
    public static final DERObjectIdentifier InvalidityDate = new DERObjectIdentifier("2.5.29.24");

    /**
     * Delta CRL indicator 
     */
    public static final DERObjectIdentifier DeltaCRLIndicator = new DERObjectIdentifier("2.5.29.27");

    /**
     * Issuing Distribution Point 
     */
    public static final DERObjectIdentifier IssuingDistributionPoint = new DERObjectIdentifier("2.5.29.28");

    /**
     * Certificate Issuer 
     */
    public static final DERObjectIdentifier CertificateIssuer = new DERObjectIdentifier("2.5.29.29");

    /**
     * Name Constraints 
     */
    public static final DERObjectIdentifier NameConstraints = new DERObjectIdentifier("2.5.29.30");

    /**
     * CRL Distribution Points 
     */
    public static final DERObjectIdentifier CRLDistributionPoints = new DERObjectIdentifier("2.5.29.31");

    /**
     * Certificate Policies 
     */
    public static final DERObjectIdentifier CertificatePolicies = new DERObjectIdentifier("2.5.29.32");

    /**
     * Policy Mappings 
     */
    public static final DERObjectIdentifier PolicyMappings = new DERObjectIdentifier("2.5.29.33");

    /**
     * Authority Key Identifier 
     */
    public static final DERObjectIdentifier AuthorityKeyIdentifier = new DERObjectIdentifier("2.5.29.35");

    /**
     * Policy Constraints 
     */
    public static final DERObjectIdentifier PolicyConstraints = new DERObjectIdentifier("2.5.29.36");

    /**
     * Extended Key Usage 
     */
    public static final DERObjectIdentifier ExtendedKeyUsage = new DERObjectIdentifier("2.5.29.37");

    /**
     * Freshest CRL
     */
    public static final DERObjectIdentifier FreshestCRL = new DERObjectIdentifier("2.5.29.46");
     
    /**
     * Inhibit Any Policy
     */
    public static final DERObjectIdentifier InhibitAnyPolicy = new DERObjectIdentifier("2.5.29.54");

    /**
     * Authority Info Access
     */
    public static final DERObjectIdentifier AuthorityInfoAccess = new DERObjectIdentifier("1.3.6.1.5.5.7.1.1");

    /**
     * Subject Info Access
     */
    public static final DERObjectIdentifier SubjectInfoAccess = new DERObjectIdentifier("1.3.6.1.5.5.7.1.11");
    
    /**
     * Logo Type
     */
    public static final DERObjectIdentifier LogoType = new DERObjectIdentifier("1.3.6.1.5.5.7.1.12");

    /**
     * BiometricInfo
     */
    public static final DERObjectIdentifier BiometricInfo = new DERObjectIdentifier("1.3.6.1.5.5.7.1.2");
    
    /**
     * QCStatements
     */
    public static final DERObjectIdentifier QCStatements = new DERObjectIdentifier("1.3.6.1.5.5.7.1.3");

    /**
     * Audit identity extension in attribute certificates.
     */
    public static final DERObjectIdentifier AuditIdentity = new DERObjectIdentifier("1.3.6.1.5.5.7.1.4");
    
    /**
     * NoRevAvail extension in attribute certificates.
     */
    public static final DERObjectIdentifier NoRevAvail = new DERObjectIdentifier("2.5.29.56");

    /**
     * TargetInformation extension in attribute certificates.
     */
    public static final DERObjectIdentifier TargetInformation = new DERObjectIdentifier("2.5.29.55");
    
    private Hashtable               extensions = new Hashtable();
    private Vector                  ordering = new Vector();

    public static X509Extensions getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static X509Extensions getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof X509Extensions)
        {
            return (X509Extensions)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new X509Extensions((ASN1Sequence)obj);
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     *
     * the extensions are a list of constructed sequences, either with (OID, OctetString) or (OID, Boolean, OctetString)
     */
    public X509Extensions(
        ASN1Sequence  seq)
    {
        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1Sequence            s = ASN1Sequence.getInstance(e.nextElement());

            if (s.size() == 3)
            {
                extensions.put(s.getObjectAt(0), new X509Extension(DERBoolean.getInstance(s.getObjectAt(1)), ASN1OctetString.getInstance(s.getObjectAt(2))));
            }
            else if (s.size() == 2)
            {
                extensions.put(s.getObjectAt(0), new X509Extension(false, ASN1OctetString.getInstance(s.getObjectAt(1))));
            }
            else
            {
                throw new IllegalArgumentException("Bad sequence size: " + s.size());
            }

            ordering.addElement(s.getObjectAt(0));
        }
    }

    /**
     * constructor from a table of extensions.
     * <p>
     * it's is assumed the table contains OID/String pairs.
     */
    public X509Extensions(
        Hashtable  extensions)
    {
        this(null, extensions);
    }

    /**
     * Constructor from a table of extensions with ordering.
     * <p>
     * It's is assumed the table contains OID/String pairs.
     */
    public X509Extensions(
        Vector      ordering,
        Hashtable   extensions)
    {
        Enumeration e;

        if (ordering == null)
        {
            e = extensions.keys();
        }
        else
        {
            e = ordering.elements();
        }

        while (e.hasMoreElements())
        {
            this.ordering.addElement(e.nextElement()); 
        }

        e = this.ordering.elements();

        while (e.hasMoreElements())
        {
            DERObjectIdentifier     oid = (DERObjectIdentifier)e.nextElement();
            X509Extension           ext = (X509Extension)extensions.get(oid);

            this.extensions.put(oid, ext);
        }
    }

    /**
     * Constructor from two vectors
     * 
     * @param objectIDs a vector of the object identifiers.
     * @param values a vector of the extension values.
     */
    public X509Extensions(
        Vector      objectIDs,
        Vector      values)
    {
        Enumeration e = objectIDs.elements();

        while (e.hasMoreElements())
        {
            this.ordering.addElement(e.nextElement()); 
        }

        int count = 0;
        
        e = this.ordering.elements();

        while (e.hasMoreElements())
        {
            DERObjectIdentifier     oid = (DERObjectIdentifier)e.nextElement();
            X509Extension           ext = (X509Extension)values.elementAt(count);

            this.extensions.put(oid, ext);
            count++;
        }
    }
    
    /**
     * return an Enumeration of the extension field's object ids.
     */
    public Enumeration oids()
    {
        return ordering.elements();
    }

    /**
     * return the extension represented by the object identifier
     * passed in.
     *
     * @return the extension if it's present, null otherwise.
     */
    public X509Extension getExtension(
        DERObjectIdentifier oid)
    {
        return (X509Extension)extensions.get(oid);
    }

    /**
     * <pre>
     *     Extensions        ::=   SEQUENCE SIZE (1..MAX) OF Extension
     *
     *     Extension         ::=   SEQUENCE {
     *        extnId            EXTENSION.&amp;id ({ExtensionSet}),
     *        critical          BOOLEAN DEFAULT FALSE,
     *        extnValue         OCTET STRING }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector     vec = new ASN1EncodableVector();
        Enumeration             e = ordering.elements();

        while (e.hasMoreElements())
        {
            DERObjectIdentifier     oid = (DERObjectIdentifier)e.nextElement();
            X509Extension           ext = (X509Extension)extensions.get(oid);
            ASN1EncodableVector     v = new ASN1EncodableVector();

            v.add(oid);

            if (ext.isCritical())
            {
                v.add(new DERBoolean(true));
            }

            v.add(ext.getValue());

            vec.add(new DERSequence(v));
        }

        return new DERSequence(vec);
    }

    public boolean equivalent(
        X509Extensions other)
    {
        if (extensions.size() != other.extensions.size())
        {
            return false;
        }

        Enumeration     e1 = extensions.keys();

        while (e1.hasMoreElements())
        {
            Object  key = e1.nextElement();

            if (!extensions.get(key).equals(other.extensions.get(key)))
            {
                return false;
            }
        }

        return true;
    }
}
