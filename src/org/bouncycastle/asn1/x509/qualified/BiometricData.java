package org.bouncycastle.asn1.x509.qualified;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * The BiometricData object.
 * <pre>
 * BiometricData  ::=  SEQUENCE {
 *       typeOfBiometricData  TypeOfBiometricData,
 *       hashAlgorithm        AlgorithmIdentifier,
 *       biometricDataHash    OCTET STRING,
 *       sourceDataUri        IA5String OPTIONAL  }
 * </pre>
 */
@SuppressWarnings("unchecked")
public class BiometricData 
    extends ASN1Encodable
{
    TypeOfBiometricData typeOfBiometricData;
    AlgorithmIdentifier hashAlgorithm;
    ASN1OctetString     biometricDataHash;
    DERIA5String        sourceDataUri;    
    
    public static BiometricData getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof BiometricData)
        {
            return (BiometricData)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new BiometricData(ASN1Sequence.getInstance(obj));            
        }
        else
        {
            throw new IllegalArgumentException("unknown object in getInstance");
        }
    }                
            
    public BiometricData(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // typeOfBiometricData
        typeOfBiometricData = TypeOfBiometricData.getInstance(e.nextElement());
        // hashAlgorithm
        hashAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        // biometricDataHash
        biometricDataHash = ASN1OctetString.getInstance(e.nextElement());
        // sourceDataUri
        if (e.hasMoreElements())
        {
            sourceDataUri = DERIA5String.getInstance(e.nextElement());
        }
    }
    
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash,
        DERIA5String        sourceDataUri)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = sourceDataUri;
    }
    
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = null;
    }

    public TypeOfBiometricData getTypeOfBiometricData()
    {
        return typeOfBiometricData;
    }
    
    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    
    public ASN1OctetString getBiometricDataHash()
    {
        return biometricDataHash;
    }
    
    public DERIA5String getSourceDataUri()
    {
        return sourceDataUri;
    }
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(typeOfBiometricData);
        seq.add(hashAlgorithm);
        seq.add(biometricDataHash); 
        
        if (sourceDataUri != null)
        {
            seq.add(sourceDataUri);
        }

        return new DERSequence(seq);
    }
}
