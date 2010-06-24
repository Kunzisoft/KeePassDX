package org.bouncycastle.asn1.isismtt.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers;
import org.bouncycastle.asn1.x500.DirectoryString;

import java.util.Enumeration;

/**
 * Names of authorities which are responsible for the administration of title
 * registers.
 * 
 * <pre>
 *             NamingAuthority ::= SEQUENCE 
 *             {
 *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
 *               namingAuthorityUrl IA5String OPTIONAL,
 *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
 *             }
 * </pre>
 * @see org.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
 * 
 */
@SuppressWarnings("unchecked")
public class NamingAuthority
    extends ASN1Encodable
{

    /**
     * Profession OIDs should always be defined under the OID branch of the
     * responsible naming authority. At the time of this writing, the work group
     * �Recht, Wirtschaft, Steuern� (�Law, Economy, Taxes�) is registered as the
     * first naming authority under the OID id-isismtt-at-namingAuthorities.
     */
    public static final DERObjectIdentifier id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern =
        new DERObjectIdentifier(ISISMTTObjectIdentifiers.id_isismtt_at_namingAuthorities + ".1");

    private DERObjectIdentifier namingAuthorityId;
    private String namingAuthorityUrl;
    private DirectoryString namingAuthorityText;

    public static NamingAuthority getInstance(Object obj)
    {
        if (obj == null || obj instanceof NamingAuthority)
        {
            return (NamingAuthority)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new NamingAuthority((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    public static NamingAuthority getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * <p/>
     * <pre>
     *             NamingAuthority ::= SEQUENCE
     *             {
     *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *               namingAuthorityUrl IA5String OPTIONAL,
     *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *             }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private NamingAuthority(ASN1Sequence seq)
    {

        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        Enumeration e = seq.getObjects();

        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERObjectIdentifier)
            {
                namingAuthorityId = (DERObjectIdentifier)o;
            }
            else if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }

        }
    }

    /**
     * @return Returns the namingAuthorityId.
     */
    public DERObjectIdentifier getNamingAuthorityId()
    {
        return namingAuthorityId;
    }

    /**
     * @return Returns the namingAuthorityText.
     */
    public DirectoryString getNamingAuthorityText()
    {
        return namingAuthorityText;
    }

    /**
     * @return Returns the namingAuthorityUrl.
     */
    public String getNamingAuthorityUrl()
    {
        return namingAuthorityUrl;
    }

    /**
     * Constructor from given details.
     * <p/>
     * All parameters can be combined.
     *
     * @param namingAuthorityId   ObjectIdentifier for naming authority.
     * @param namingAuthorityUrl  URL for naming authority.
     * @param namingAuthorityText Textual representation of naming authority.
     */
    public NamingAuthority(DERObjectIdentifier namingAuthorityId,
                           String namingAuthorityUrl, DirectoryString namingAuthorityText)
    {
        this.namingAuthorityId = namingAuthorityId;
        this.namingAuthorityUrl = namingAuthorityUrl;
        this.namingAuthorityText = namingAuthorityText;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *             NamingAuthority ::= SEQUENCE
     *             {
     *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *               namingAuthorityUrl IA5String OPTIONAL,
     *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *             }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (namingAuthorityId != null)
        {
            vec.add(namingAuthorityId);
        }
        if (namingAuthorityUrl != null)
        {
            vec.add(new DERIA5String(namingAuthorityUrl, true));
        }
        if (namingAuthorityText != null)
        {
            vec.add(namingAuthorityText);
        }
        return new DERSequence(vec);
    }
}
