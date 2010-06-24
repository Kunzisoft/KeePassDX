package org.bouncycastle.asn1.pkcs;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

@SuppressWarnings("unchecked")
public class PBES2Parameters
    extends ASN1Encodable
    implements PKCSObjectIdentifiers
{
    private KeyDerivationFunc   func;
    private EncryptionScheme    scheme;

    public PBES2Parameters(
        ASN1Sequence  obj)
    {
        Enumeration e = obj.getObjects();
        ASN1Sequence  funcSeq = (ASN1Sequence)e.nextElement();

        if (funcSeq.getObjectAt(0).equals(id_PBKDF2))
        {
            func = new KeyDerivationFunc(id_PBKDF2, PBKDF2Params.getInstance(funcSeq.getObjectAt(1)));
        }
        else
        {
            func = new KeyDerivationFunc(funcSeq);
        }

        scheme = new EncryptionScheme((ASN1Sequence)e.nextElement());
    }

    public KeyDerivationFunc getKeyDerivationFunc()
    {
        return func;
    }

    public EncryptionScheme getEncryptionScheme()
    {
        return scheme;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(func);
        v.add(scheme);

        return new DERSequence(v);
    }
}
