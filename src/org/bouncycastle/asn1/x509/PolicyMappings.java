package org.bouncycastle.asn1.x509;

import java.util.Hashtable;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

/**
 * PolicyMappings V3 extension, described in RFC3280.
 * <pre>
 *    PolicyMappings ::= SEQUENCE SIZE (1..MAX) OF SEQUENCE {
 *      issuerDomainPolicy      CertPolicyId,
 *      subjectDomainPolicy     CertPolicyId }
 * </pre>
 *
 * @see <a href="http://www.faqs.org/rfc/rfc3280.txt">RFC 3280, section 4.2.1.6</a>
 */
@SuppressWarnings("unchecked")
public class PolicyMappings
    extends ASN1Encodable
{
   ASN1Sequence seq = null;

   /**
    * Creates a new <code>PolicyMappings</code> instance.
    *
    * @param seq an <code>ASN1Sequence</code> constructed as specified
    * in RFC 3280
    */
   public PolicyMappings (ASN1Sequence seq) 
      {
         this.seq = seq;
      }

   /**
    * Creates a new <code>PolicyMappings</code> instance.
    *
    * @param mappings a <code>HashMap</code> value that maps
    * <code>String</code> oids
    * to other <code>String</code> oids. 
    */
   public PolicyMappings (Hashtable mappings) 
      {
         ASN1EncodableVector dev = new ASN1EncodableVector();
         Enumeration it = mappings.keys();

         while (it.hasMoreElements())
         {
            String idp = (String) it.nextElement();
            String sdp = (String) mappings.get(idp);
            ASN1EncodableVector dv = new ASN1EncodableVector();
            dv.add(new DERObjectIdentifier(idp));
            dv.add(new DERObjectIdentifier(sdp));
            dev.add(new DERSequence(dv));
         }

         seq = new DERSequence(dev);
      }

   public DERObject toASN1Object() 
      {
         return seq;
      }
}
