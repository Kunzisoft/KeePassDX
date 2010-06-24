package org.bouncycastle.asn1.pkcs;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

@SuppressWarnings("unchecked")
public class EncryptedPrivateKeyInfo
    extends ASN1Encodable
{
    private AlgorithmIdentifier algId;
    private ASN1OctetString     data;

    public EncryptedPrivateKeyInfo(
        ASN1Sequence  seq)
    {
        Enumeration e = seq.getObjects();

        algId = AlgorithmIdentifier.getInstance(e.nextElement());
        data = (ASN1OctetString)e.nextElement();
    }

    public EncryptedPrivateKeyInfo(
        AlgorithmIdentifier algId,
        byte[]              encoding)
    {
        this.algId = algId;
        this.data = new DEROctetString(encoding);
    }

    public static EncryptedPrivateKeyInfo getInstance(
        Object  obj)
    {
        if (obj instanceof EncryptedData)
        {
            return (EncryptedPrivateKeyInfo)obj;
        }
        else if (obj instanceof ASN1Sequence)
        { 
            return new EncryptedPrivateKeyInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }
    
    public AlgorithmIdentifier getEncryptionAlgorithm()
    {
        return algId;
    }

    public byte[] getEncryptedData()
    {
        return data.getOctets();
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * EncryptedPrivateKeyInfo ::= SEQUENCE {
     *      encryptionAlgorithm AlgorithmIdentifier {{KeyEncryptionAlgorithms}},
     *      encryptedData EncryptedData
     * }
     *
     * EncryptedData ::= OCTET STRING
     *
     * KeyEncryptionAlgorithms ALGORITHM-IDENTIFIER ::= {
     *          ... -- For local profiles
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(algId);
        v.add(data);

        return new DERSequence(v);
    }
}
