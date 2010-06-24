package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class KeyTransRecipientInfo
    extends ASN1Encodable
{
    private DERInteger          version;
    private RecipientIdentifier rid;
    private AlgorithmIdentifier keyEncryptionAlgorithm;
    private ASN1OctetString     encryptedKey;

    public KeyTransRecipientInfo(
        RecipientIdentifier rid,
        AlgorithmIdentifier keyEncryptionAlgorithm,
        ASN1OctetString     encryptedKey)
    {
        if (rid.getDERObject() instanceof ASN1TaggedObject)
        {
            this.version = new DERInteger(2);
        }
        else
        {
            this.version = new DERInteger(0);
        }

        this.rid = rid;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.encryptedKey = encryptedKey;
    }
    
    public KeyTransRecipientInfo(
        ASN1Sequence seq)
    {
        this.version = (DERInteger)seq.getObjectAt(0);
        this.rid = RecipientIdentifier.getInstance(seq.getObjectAt(1));
        this.keyEncryptionAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(2));
        this.encryptedKey = (ASN1OctetString)seq.getObjectAt(3);
    }

    /**
     * return a KeyTransRecipientInfo object from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static KeyTransRecipientInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof KeyTransRecipientInfo)
        {
            return (KeyTransRecipientInfo)obj;
        }
        
        if(obj instanceof ASN1Sequence)
        {
            return new KeyTransRecipientInfo((ASN1Sequence)obj);
        }
        
        throw new IllegalArgumentException(
        "Illegal object in KeyTransRecipientInfo: " + obj.getClass().getName());
    } 

    public DERInteger getVersion()
    {
        return version;
    }

    public RecipientIdentifier getRecipientIdentifier()
    {
        return rid;
    }

    public AlgorithmIdentifier getKeyEncryptionAlgorithm()
    {
        return keyEncryptionAlgorithm;
    }

    public ASN1OctetString getEncryptedKey()
    {
        return encryptedKey;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * KeyTransRecipientInfo ::= SEQUENCE {
     *     version CMSVersion,  -- always set to 0 or 2
     *     rid RecipientIdentifier,
     *     keyEncryptionAlgorithm KeyEncryptionAlgorithmIdentifier,
     *     encryptedKey EncryptedKey 
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(rid);
        v.add(keyEncryptionAlgorithm);
        v.add(encryptedKey);

        return new DERSequence(v);
    }
}
