package org.bouncycastle.asn1.cryptopro;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class GOST3410PublicKeyAlgParameters
    extends ASN1Encodable
{
    private DERObjectIdentifier  publicKeyParamSet;
    private DERObjectIdentifier  digestParamSet;
    private DERObjectIdentifier  encryptionParamSet;
    
    public static GOST3410PublicKeyAlgParameters getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static GOST3410PublicKeyAlgParameters getInstance(
        Object obj)
    {
        if(obj == null || obj instanceof GOST3410PublicKeyAlgParameters)
        {
            return (GOST3410PublicKeyAlgParameters)obj;
        }

        if(obj instanceof ASN1Sequence)
        {
            return new GOST3410PublicKeyAlgParameters((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid GOST3410Parameter: " + obj.getClass().getName());
    }
    
    public GOST3410PublicKeyAlgParameters(
        DERObjectIdentifier  publicKeyParamSet,
        DERObjectIdentifier  digestParamSet)
    {
        this.publicKeyParamSet = publicKeyParamSet;
        this.digestParamSet = digestParamSet;
        this.encryptionParamSet = null;
    }

    public GOST3410PublicKeyAlgParameters(
        DERObjectIdentifier  publicKeyParamSet,
        DERObjectIdentifier  digestParamSet,
        DERObjectIdentifier  encryptionParamSet)
    {
        this.publicKeyParamSet = publicKeyParamSet;
        this.digestParamSet = digestParamSet;
        this.encryptionParamSet = encryptionParamSet;
    }

    public GOST3410PublicKeyAlgParameters(
        ASN1Sequence  seq)
    {
        this.publicKeyParamSet = (DERObjectIdentifier)seq.getObjectAt(0);
        this.digestParamSet = (DERObjectIdentifier)seq.getObjectAt(1);
        
        if (seq.size() > 2)
        {
            this.encryptionParamSet = (DERObjectIdentifier)seq.getObjectAt(2);
        }
    }

    public DERObjectIdentifier getPublicKeyParamSet()
    {
        return publicKeyParamSet;
    }

    public DERObjectIdentifier getDigestParamSet()
    {
        return digestParamSet;
    }

    public DERObjectIdentifier getEncryptionParamSet()
    {
        return encryptionParamSet;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(publicKeyParamSet);
        v.add(digestParamSet);
        
        if (encryptionParamSet != null)
        {
            v.add(encryptionParamSet);
        }

        return new DERSequence(v);
    }
}
