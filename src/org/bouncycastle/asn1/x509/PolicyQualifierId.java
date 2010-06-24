
package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * PolicyQualifierId, used in the CertificatePolicies
 * X509V3 extension.
 * 
 * <pre>
 *    id-qt          OBJECT IDENTIFIER ::=  { id-pkix 2 }
 *    id-qt-cps      OBJECT IDENTIFIER ::=  { id-qt 1 }
 *    id-qt-unotice  OBJECT IDENTIFIER ::=  { id-qt 2 }
 *  PolicyQualifierId ::=
 *       OBJECT IDENTIFIER (id-qt-cps | id-qt-unotice)
 * </pre>
 */
public class PolicyQualifierId extends DERObjectIdentifier 
{
   private static final String id_qt = "1.3.6.1.5.5.7.2";

   private PolicyQualifierId(String id) 
      {
         super(id);
      }
   
   public static final PolicyQualifierId id_qt_cps =
       new PolicyQualifierId(id_qt + ".1");
   public static final PolicyQualifierId id_qt_unotice =
       new PolicyQualifierId(id_qt + ".2");
}
