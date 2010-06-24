package org.bouncycastle.asn1.x509;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

/**
 * The DigestInfo object.
 * <pre>
 * DigestInfo::=SEQUENCE{
 *          digestAlgorithm  AlgorithmIdentifier,
 *          digest OCTET STRING }
 * </pre>
 */
@SuppressWarnings("unchecked")
public class DigestInfo
    extends ASN1Encodable
{
    private byte[]                  digest;
    private AlgorithmIdentifier     algId;

    public static DigestInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static DigestInfo getInstance(
        Object  obj)
    {
        if (obj instanceof DigestInfo)
        {
            return (DigestInfo)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new DigestInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public DigestInfo(
        AlgorithmIdentifier  algId,
        byte[]               digest)
    {
        this.digest = digest;
        this.algId = algId;
    }

    public DigestInfo(
        ASN1Sequence  obj)
    {
        Enumeration             e = obj.getObjects();

        algId = AlgorithmIdentifier.getInstance(e.nextElement());
        digest = ASN1OctetString.getInstance(e.nextElement()).getOctets();
    }

    public AlgorithmIdentifier getAlgorithmId()
    {
        return algId;
    }

    public byte[] getDigest()
    {
        return digest;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(algId);
        v.add(new DEROctetString(digest));

        return new DERSequence(v);
    }
}
