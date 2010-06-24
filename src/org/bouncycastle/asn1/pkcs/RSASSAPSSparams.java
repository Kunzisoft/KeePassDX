package org.bouncycastle.asn1.pkcs;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class RSASSAPSSparams
    extends ASN1Encodable
{
    private AlgorithmIdentifier hashAlgorithm;
    private AlgorithmIdentifier maskGenAlgorithm;
    private DERInteger          saltLength;
    private DERInteger          trailerField;
    
    public final static AlgorithmIdentifier DEFAULT_HASH_ALGORITHM = new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, new DERNull());
    public final static AlgorithmIdentifier DEFAULT_MASK_GEN_FUNCTION = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, DEFAULT_HASH_ALGORITHM);
    public final static DERInteger          DEFAULT_SALT_LENGTH = new DERInteger(20);
    public final static DERInteger          DEFAULT_TRAILER_FIELD = new DERInteger(1);
    
    public static RSASSAPSSparams getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof RSASSAPSSparams)
        {
            return (RSASSAPSSparams)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new RSASSAPSSparams((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }
    
    /**
     * The default version
     */
    public RSASSAPSSparams()
    {
        hashAlgorithm = DEFAULT_HASH_ALGORITHM;
        maskGenAlgorithm = DEFAULT_MASK_GEN_FUNCTION;
        saltLength = DEFAULT_SALT_LENGTH;
        trailerField = DEFAULT_TRAILER_FIELD;
    }
    
    public RSASSAPSSparams(
        AlgorithmIdentifier hashAlgorithm,
        AlgorithmIdentifier maskGenAlgorithm,
        DERInteger          saltLength,
        DERInteger          trailerField)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.maskGenAlgorithm = maskGenAlgorithm;
        this.saltLength = saltLength;
        this.trailerField = trailerField;
    }
    
    public RSASSAPSSparams(
        ASN1Sequence seq)
    {
        hashAlgorithm = DEFAULT_HASH_ALGORITHM;
        maskGenAlgorithm = DEFAULT_MASK_GEN_FUNCTION;
        saltLength = DEFAULT_SALT_LENGTH;
        trailerField = DEFAULT_TRAILER_FIELD;
        
        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)seq.getObjectAt(i);
            
            switch (o.getTagNo())
            {
            case 0:
                hashAlgorithm = AlgorithmIdentifier.getInstance(o, true);
                break;
            case 1:
                maskGenAlgorithm = AlgorithmIdentifier.getInstance(o, true);
                break;
            case 2:
                saltLength = DERInteger.getInstance(o, true);
                break;
            case 3:
                trailerField = DERInteger.getInstance(o, true);
                break;
            default:
                throw new IllegalArgumentException("unknown tag");
            }
        }
    }
    
    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    
    public AlgorithmIdentifier getMaskGenAlgorithm()
    {
        return maskGenAlgorithm;
    }
    
    public DERInteger getSaltLength()
    {
        return saltLength;
    }
    
    public DERInteger getTrailerField()
    {
        return trailerField;
    }
    
    /**
     * <pre>
     * RSASSA-PSS-params ::= SEQUENCE {
     *   hashAlgorithm      [0] OAEP-PSSDigestAlgorithms  DEFAULT sha1,
     *    maskGenAlgorithm   [1] PKCS1MGFAlgorithms  DEFAULT mgf1SHA1,
     *    saltLength         [2] INTEGER  DEFAULT 20,
     *    trailerField       [3] TrailerField  DEFAULT trailerFieldBC
     *  }
     *
     * OAEP-PSSDigestAlgorithms    ALGORITHM-IDENTIFIER ::= {
     *    { OID id-sha1 PARAMETERS NULL   }|
     *    { OID id-sha256 PARAMETERS NULL }|
     *    { OID id-sha384 PARAMETERS NULL }|
     *    { OID id-sha512 PARAMETERS NULL },
     *    ...  -- Allows for future expansion --
     * }
     *
     * PKCS1MGFAlgorithms    ALGORITHM-IDENTIFIER ::= {
     *   { OID id-mgf1 PARAMETERS OAEP-PSSDigestAlgorithms },
     *    ...  -- Allows for future expansion --
     * }
     * 
     * TrailerField ::= INTEGER { trailerFieldBC(1) }
     * </pre>
     * @return the asn1 primitive representing the parameters.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        if (!hashAlgorithm.equals(DEFAULT_HASH_ALGORITHM))
        {
            v.add(new DERTaggedObject(true, 0, hashAlgorithm));
        }
        
        if (!maskGenAlgorithm.equals(DEFAULT_MASK_GEN_FUNCTION))
        {
            v.add(new DERTaggedObject(true, 1, maskGenAlgorithm));
        }
        
        if (!saltLength.equals(DEFAULT_SALT_LENGTH))
        {
            v.add(new DERTaggedObject(true, 2, saltLength));
        }
        
        if (!trailerField.equals(DEFAULT_TRAILER_FIELD))
        {
            v.add(new DERTaggedObject(true, 3, trailerField));
        }
        
        return new DERSequence(v);
    }
}
