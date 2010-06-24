package org.bouncycastle.asn1.tsp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.X509Extensions;

public class TimeStampReq
    extends ASN1Encodable
{
    DERInteger version;

    MessageImprint messageImprint;

    DERObjectIdentifier tsaPolicy;

    DERInteger nonce;

    DERBoolean certReq;

    X509Extensions extensions;

    public static TimeStampReq getInstance(Object o)
    {
        if (o == null || o instanceof TimeStampReq)
        {
            return (TimeStampReq) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new TimeStampReq((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "Unknown object in 'TimeStampReq' factory : "
                        + o.getClass().getName() + ".");
    }

    public TimeStampReq(ASN1Sequence seq)
    {
        int nbObjects = seq.size();

        int seqStart = 0;

        // version
        version = DERInteger.getInstance(seq.getObjectAt(seqStart));

        seqStart++;

        // messageImprint
        messageImprint = MessageImprint.getInstance(seq.getObjectAt(seqStart));

        seqStart++;

        for (int opt = seqStart; opt < nbObjects; opt++)
        {
            // tsaPolicy
            if (seq.getObjectAt(opt) instanceof DERObjectIdentifier)
            {
                tsaPolicy = DERObjectIdentifier.getInstance(seq.getObjectAt(opt));
            }
            // nonce
            else if (seq.getObjectAt(opt) instanceof DERInteger)
            {
                nonce = DERInteger.getInstance(seq.getObjectAt(opt));
            }
            // certReq
            else if (seq.getObjectAt(opt) instanceof DERBoolean)
            {
                certReq = DERBoolean.getInstance(seq.getObjectAt(opt));
            }
            // extensions
            else if (seq.getObjectAt(opt) instanceof ASN1TaggedObject)
            {
                ASN1TaggedObject    tagged = (ASN1TaggedObject)seq.getObjectAt(opt);
                if (tagged.getTagNo() == 0)
                {
                    extensions = X509Extensions.getInstance(tagged, false);
                }
            }
        }
    }

    public TimeStampReq(
        MessageImprint      messageImprint,
        DERObjectIdentifier tsaPolicy,
        DERInteger          nonce,
        DERBoolean          certReq,
        X509Extensions      extensions)
    {
        // default
        version = new DERInteger(1);

        this.messageImprint = messageImprint;
        this.tsaPolicy = tsaPolicy;
        this.nonce = nonce;
        this.certReq = certReq;
        this.extensions = extensions;
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public MessageImprint getMessageImprint()
    {
        return messageImprint;
    }

    public DERObjectIdentifier getReqPolicy()
    {
        return tsaPolicy;
    }

    public DERInteger getNonce()
    {
        return nonce;
    }

    public DERBoolean getCertReq()
    {
        return certReq;
    }

    public X509Extensions getExtensions()
    {
        return extensions;
    }

    /**
     * <pre>
     * TimeStampReq ::= SEQUENCE  {
     *  version                      INTEGER  { v1(1) },
     *  messageImprint               MessageImprint,
     *    --a hash algorithm OID and the hash value of the data to be
     *    --time-stamped
     *  reqPolicy             TSAPolicyId              OPTIONAL,
     *  nonce                 INTEGER                  OPTIONAL,
     *  certReq               BOOLEAN                  DEFAULT FALSE,
     *  extensions            [0] IMPLICIT Extensions  OPTIONAL
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(version);
        v.add(messageImprint);
        
        if (tsaPolicy != null)
        {
            v.add(tsaPolicy);
        }
        
        if (nonce != null)
        {
            v.add(nonce);
        }
        
        if (certReq != null && certReq.isTrue())
        {
            v.add(certReq);
        }
        
        if (extensions != null)
        {
            v.add(new DERTaggedObject(false, 0, extensions));
        }

        return new DERSequence(v);
    }
}
