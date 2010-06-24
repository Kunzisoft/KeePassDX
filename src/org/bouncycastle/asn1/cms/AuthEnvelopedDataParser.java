package org.bouncycastle.asn1.cms;

import java.io.IOException;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1SequenceParser;
import org.bouncycastle.asn1.ASN1SetParser;
import org.bouncycastle.asn1.ASN1TaggedObjectParser;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERTags;

/**
 * Produce an object suitable for an ASN1OutputStream.
 * 
 * <pre>
 * AuthEnvelopedData ::= SEQUENCE {
 *   version CMSVersion,
 *   originatorInfo [0] IMPLICIT OriginatorInfo OPTIONAL,
 *   recipientInfos RecipientInfos,
 *   authEncryptedContentInfo EncryptedContentInfo,
 *   authAttrs [1] IMPLICIT AuthAttributes OPTIONAL,
 *   mac MessageAuthenticationCode,
 *   unauthAttrs [2] IMPLICIT UnauthAttributes OPTIONAL }
 * </pre>
 */
public class AuthEnvelopedDataParser
{
    private ASN1SequenceParser seq;
    private DERInteger version;
    private DEREncodable nextObject;
    private boolean originatorInfoCalled;

    public AuthEnvelopedDataParser(ASN1SequenceParser seq) throws IOException
    {
        this.seq = seq;

        // TODO
        // "It MUST be set to 0."
        this.version = (DERInteger)seq.readObject();
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public OriginatorInfo getOriginatorInfo()
        throws IOException
    {
        originatorInfoCalled = true;

        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject instanceof ASN1TaggedObjectParser && ((ASN1TaggedObjectParser)nextObject).getTagNo() == 0)
        {
            ASN1SequenceParser originatorInfo = (ASN1SequenceParser) ((ASN1TaggedObjectParser)nextObject).getObjectParser(DERTags.SEQUENCE, false);
            nextObject = null;
            return OriginatorInfo.getInstance(originatorInfo.getDERObject());
        }

        return null;
    }

    public ASN1SetParser getRecipientInfos()
        throws IOException
    {
        if (!originatorInfoCalled)
        {
            getOriginatorInfo();
        }

        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        ASN1SetParser recipientInfos = (ASN1SetParser)nextObject;
        nextObject = null;
        return recipientInfos;
    }

    public EncryptedContentInfoParser getAuthEncryptedContentInfo() 
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            ASN1SequenceParser o = (ASN1SequenceParser) nextObject;
            nextObject = null;
            return new EncryptedContentInfoParser(o);
        }

        return null;
    }

    public ASN1SetParser getAuthAttrs()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject instanceof ASN1TaggedObjectParser)
        {
            DEREncodable o = nextObject;
            nextObject = null;
            return (ASN1SetParser)((ASN1TaggedObjectParser)o).getObjectParser(DERTags.SET, false);
        }

        // TODO
        // "The authAttrs MUST be present if the content type carried in
        // EncryptedContentInfo is not id-data."

        return null;
    }

    public ASN1OctetString getMac()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        DEREncodable o = nextObject;
        nextObject = null;

        return ASN1OctetString.getInstance(o.getDERObject());
    }

    public ASN1SetParser getUnauthAttrs()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            DEREncodable o = nextObject;
            nextObject = null;
            return (ASN1SetParser)((ASN1TaggedObjectParser)o).getObjectParser(DERTags.SET, false);
        }

        return null;
    }
}
