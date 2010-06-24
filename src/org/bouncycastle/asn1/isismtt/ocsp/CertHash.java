package org.bouncycastle.asn1.isismtt.ocsp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * ISIS-MTT PROFILE: The responder may include this extension in a response to
 * send the hash of the requested certificate to the responder. This hash is
 * cryptographically bound to the certificate and serves as evidence that the
 * certificate is known to the responder (i.e. it has been issued and is present
 * in the directory). Hence, this extension is a means to provide a positive
 * statement of availability as described in T8.[8]. As explained in T13.[1],
 * clients may rely on this information to be able to validate signatures after
 * the expiry of the corresponding certificate. Hence, clients MUST support this
 * extension. If a positive statement of availability is to be delivered, this
 * extension syntax and OID MUST be used.
 * <p/>
 * <p/>
 * <pre>
 *     CertHash ::= SEQUENCE {
 *       hashAlgorithm AlgorithmIdentifier,
 *       certificateHash OCTET STRING
 *     }
 * </pre>
 */
public class CertHash
    extends ASN1Encodable
{

    private AlgorithmIdentifier hashAlgorithm;
    private byte[] certificateHash;

    public static CertHash getInstance(Object obj)
    {
        if (obj == null || obj instanceof CertHash)
        {
            return (CertHash)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new CertHash((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type CertHash:
     * <p/>
     * <pre>
     *     CertHash ::= SEQUENCE {
     *       hashAlgorithm AlgorithmIdentifier,
     *       certificateHash OCTET STRING
     *     }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private CertHash(ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        hashAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        certificateHash = DEROctetString.getInstance(seq.getObjectAt(1)).getOctets();
    }

    /**
     * Constructor from a given details.
     *
     * @param hashAlgorithm   The hash algorithm identifier.
     * @param certificateHash The hash of the whole DER encoding of the certificate.
     */
    public CertHash(AlgorithmIdentifier hashAlgorithm, byte[] certificateHash)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.certificateHash = new byte[certificateHash.length];
        System.arraycopy(certificateHash, 0, this.certificateHash, 0,
            certificateHash.length);
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }

    public byte[] getCertificateHash()
    {
        return certificateHash;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *     CertHash ::= SEQUENCE {
     *       hashAlgorithm AlgorithmIdentifier,
     *       certificateHash OCTET STRING
     *     }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(hashAlgorithm);
        vec.add(new DEROctetString(certificateHash));
        return new DERSequence(vec);
    }
}
