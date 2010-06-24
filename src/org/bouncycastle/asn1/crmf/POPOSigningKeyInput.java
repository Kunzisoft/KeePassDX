package org.bouncycastle.asn1.crmf;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class POPOSigningKeyInput
    extends ASN1Encodable
{
    private ASN1Encodable        authInfo;
    private SubjectPublicKeyInfo publicKey;

    private POPOSigningKeyInput(ASN1Sequence seq)
    {
        authInfo = (ASN1Encodable)seq.getObjectAt(0);
        publicKey = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(1));
    }

    public static POPOSigningKeyInput getInstance(Object o)
    {
        if (o instanceof POPOSigningKeyInput)
        {
            return (POPOSigningKeyInput)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new POPOSigningKeyInput((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public SubjectPublicKeyInfo getPublicKey()
    {
        return publicKey;
    }

    /**
     * <pre>
     * POPOSigningKeyInput ::= SEQUENCE {
     *        authInfo             CHOICE {
     *                                 sender              [0] GeneralName,
     *                                 -- used only if an authenticated identity has been
     *                                 -- established for the sender (e.g., a DN from a
     *                                 -- previously-issued and currently-valid certificate
     *                                 publicKeyMAC        PKMACValue },
     *                                 -- used if no authenticated GeneralName currently exists for
     *                                 -- the sender; publicKeyMAC contains a password-based MAC
     *                                 -- on the DER-encoded value of publicKey
     *        publicKey           SubjectPublicKeyInfo }  -- from CertTemplate
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(authInfo);
        v.add(publicKey);

        return new DERSequence(v);
    }
}
