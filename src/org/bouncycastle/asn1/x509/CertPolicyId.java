package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DERObjectIdentifier;


/**
 * CertPolicyId, used in the CertificatePolicies and PolicyMappings
 * X509V3 Extensions.
 *
 * <pre>
 *     CertPolicyId ::= OBJECT IDENTIFIER
 * </pre>
 */
public class CertPolicyId extends DERObjectIdentifier 
{
   public CertPolicyId (String id) 
   {
     super(id);
   }
}
