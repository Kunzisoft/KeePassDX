package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1SequenceParser;
import org.bouncycastle.asn1.ASN1SetParser;
import org.bouncycastle.asn1.ASN1TaggedObjectParser;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.io.IOException;

/**
 * Produce an object suitable for an ASN1OutputStream.
 * <pre>
 * AuthenticatedData ::= SEQUENCE {
 *       version CMSVersion,
 *       originatorInfo [0] IMPLICIT OriginatorInfo OPTIONAL,
 *       recipientInfos RecipientInfos,
 *       macAlgorithm MessageAuthenticationCodeAlgorithm,
 *       digestAlgorithm [1] DigestAlgorithmIdentifier OPTIONAL,
 *       encapContentInfo EncapsulatedContentInfo,
 *       authAttrs [2] IMPLICIT AuthAttributes OPTIONAL,
 *       mac MessageAuthenticationCode,
 *       unauthAttrs [3] IMPLICIT UnauthAttributes OPTIONAL }
 *
 * AuthAttributes ::= SET SIZE (1..MAX) OF Attribute
 *
 * UnauthAttributes ::= SET SIZE (1..MAX) OF Attribute
 *
 * MessageAuthenticationCode ::= OCTET STRING
 * </pre>
 */
public class AuthenticatedDataParser
{
    private ASN1SequenceParser seq;
    private DERInteger version;
    private DEREncodable nextObject;
    private boolean originatorInfoCalled;

    public AuthenticatedDataParser(
        ASN1SequenceParser seq)
        throws IOException
    {
        this.seq = seq;
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

    public AlgorithmIdentifier getMacAlgorithm()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            ASN1SequenceParser o = (ASN1SequenceParser)nextObject;
            nextObject = null;
            return AlgorithmIdentifier.getInstance(o.getDERObject());
        }

        return null;
    }

    public ContentInfoParser getEnapsulatedContentInfo()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            ASN1SequenceParser o = (ASN1SequenceParser)nextObject;
            nextObject = null;
            return new ContentInfoParser(o);
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