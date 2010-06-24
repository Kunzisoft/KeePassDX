package org.bouncycastle.asn1.x509;

import java.io.IOException;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

/**
 * The object that contains the public key stored in a certficate.
 * <p>
 * The getEncoded() method in the public keys in the JCE produces a DER
 * encoded one of these.
 */
@SuppressWarnings("unchecked")
public class SubjectPublicKeyInfo
    extends ASN1Encodable
{
    private AlgorithmIdentifier     algId;
    private DERBitString            keyData;

    public static SubjectPublicKeyInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static SubjectPublicKeyInfo getInstance(
        Object  obj)
    {
        if (obj instanceof SubjectPublicKeyInfo)
        {
            return (SubjectPublicKeyInfo)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SubjectPublicKeyInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        DEREncodable        publicKey)
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }

    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        byte[]              publicKey)
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }

    public SubjectPublicKeyInfo(
        ASN1Sequence  seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }

        Enumeration         e = seq.getObjects();

        this.algId = AlgorithmIdentifier.getInstance(e.nextElement());
        this.keyData = DERBitString.getInstance(e.nextElement());
    }

    public AlgorithmIdentifier getAlgorithmId()
    {
        return algId;
    }

    /**
     * for when the public key is an encoded object - if the bitstring
     * can't be decoded this routine throws an IOException.
     *
     * @exception IOException - if the bit string doesn't represent a DER
     * encoded object.
     */
    public DERObject getPublicKey()
        throws IOException
    {
        ASN1InputStream         aIn = new ASN1InputStream(keyData.getBytes());

        return aIn.readObject();
    }

    /**
     * for when the public key is raw bits...
     */
    public DERBitString getPublicKeyData()
    {
        return keyData;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SubjectPublicKeyInfo ::= SEQUENCE {
     *                          algorithm AlgorithmIdentifier,
     *                          publicKey BIT STRING }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(algId);
        v.add(keyData);

        return new DERSequence(v);
    }
}
