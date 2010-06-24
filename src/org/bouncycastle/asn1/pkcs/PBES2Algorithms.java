package org.bouncycastle.asn1.pkcs;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * @deprecated - use AlgorithmIdentifier and PBES2Parameters
 */
@SuppressWarnings("unchecked")
public class PBES2Algorithms
    extends AlgorithmIdentifier implements PKCSObjectIdentifiers
{
    private DERObjectIdentifier objectId;
    private KeyDerivationFunc   func;
    private EncryptionScheme    scheme;

    public PBES2Algorithms(
        ASN1Sequence  obj)
    {
        super(obj);

        Enumeration     e = obj.getObjects();

        objectId = (DERObjectIdentifier)e.nextElement();

        ASN1Sequence seq = (ASN1Sequence)e.nextElement();

        e = seq.getObjects();

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

    public DERObjectIdentifier getObjectId()
    {
        return objectId;
    }

    public KeyDerivationFunc getKeyDerivationFunc()
    {
        return func;
    }

    public EncryptionScheme getEncryptionScheme()
    {
        return scheme;
    }

    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        ASN1EncodableVector  subV = new ASN1EncodableVector();

        v.add(objectId);

        subV.add(func);
        subV.add(scheme);
        v.add(new DERSequence(subV));

        return new DERSequence(v);
    }
}
