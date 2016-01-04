package org.bouncycastle.asn1;

/**
 * the parent class for this will eventually disappear. Use this one!
 */
public class ASN1EncodableVector
    extends DEREncodableVector
{
    // migrating from DEREncodeableVector
    @SuppressWarnings("deprecation")
	public ASN1EncodableVector()
    {
        
    }
}
