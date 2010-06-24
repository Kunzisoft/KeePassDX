package org.bouncycastle.asn1.smime;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

/**
 * Handler for creating a vector S/MIME Capabilities
 */
public class SMIMECapabilityVector
{
    private ASN1EncodableVector    capabilities = new ASN1EncodableVector();

    public void addCapability(
        DERObjectIdentifier capability)
    {
        capabilities.add(new DERSequence(capability));
    }

    public void addCapability(
        DERObjectIdentifier capability,
        int                 value)
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(capability);
        v.add(new DERInteger(value));

        capabilities.add(new DERSequence(v));
    }

    public void addCapability(
        DERObjectIdentifier capability,
        DEREncodable        params)
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(capability);
        v.add(params);

        capabilities.add(new DERSequence(v));
    }

    public DEREncodableVector toDEREncodableVector()
    {
        return capabilities;
    }
}
