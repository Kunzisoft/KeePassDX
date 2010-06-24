package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class CommitmentTypeIndication
    extends ASN1Encodable 
{
    private DERObjectIdentifier   commitmentTypeId;
    private ASN1Sequence          commitmentTypeQualifier;
    
    public CommitmentTypeIndication(
        ASN1Sequence seq)
    {
        commitmentTypeId = (DERObjectIdentifier)seq.getObjectAt(0);

        if (seq.size() > 1)
        {
            commitmentTypeQualifier = (ASN1Sequence)seq.getObjectAt(1);
        }
    }

    public CommitmentTypeIndication(
        DERObjectIdentifier commitmentTypeId)
    {
        this.commitmentTypeId = commitmentTypeId;
    }

    public CommitmentTypeIndication(
        DERObjectIdentifier commitmentTypeId,
        ASN1Sequence        commitmentTypeQualifier)
    {
        this.commitmentTypeId = commitmentTypeId;
        this.commitmentTypeQualifier = commitmentTypeQualifier;
    }

    public static CommitmentTypeIndication getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof CommitmentTypeIndication)
        {
            return (CommitmentTypeIndication)obj;
        }

        return new CommitmentTypeIndication(ASN1Sequence.getInstance(obj));
    }

    public DERObjectIdentifier getCommitmentTypeId()
    {
        return commitmentTypeId;
    }
    
    public ASN1Sequence getCommitmentTypeQualifier()
    {
        return commitmentTypeQualifier;
    }
    
    /**
     * <pre>
     * CommitmentTypeIndication ::= SEQUENCE {
     *      commitmentTypeId   CommitmentTypeIdentifier,
     *      commitmentTypeQualifier   SEQUENCE SIZE (1..MAX) OF
     *              CommitmentTypeQualifier OPTIONAL }
     * </pre>
     */ 
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(commitmentTypeId);

        if (commitmentTypeQualifier != null)
        {
            v.add(commitmentTypeQualifier);
        }
        
        return new DERSequence(v);
    }
}