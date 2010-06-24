package org.bouncycastle.asn1.icao;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

/**
 * The DataGroupHash object.
 * <pre>
 * DataGroupHash  ::=  SEQUENCE {
 *      dataGroupNumber         DataGroupNumber,
 *      dataGroupHashValue     OCTET STRING }
 * 
 * DataGroupNumber ::= INTEGER {
 *         dataGroup1    (1),
 *         dataGroup1    (2),
 *         dataGroup1    (3),
 *         dataGroup1    (4),
 *         dataGroup1    (5),
 *         dataGroup1    (6),
 *         dataGroup1    (7),
 *         dataGroup1    (8),
 *         dataGroup1    (9),
 *         dataGroup1    (10),
 *         dataGroup1    (11),
 *         dataGroup1    (12),
 *         dataGroup1    (13),
 *         dataGroup1    (14),
 *         dataGroup1    (15),
 *         dataGroup1    (16) }
 * 
 * </pre>
 */
@SuppressWarnings("unchecked")
public class DataGroupHash 
    extends ASN1Encodable
{
    DERInteger dataGroupNumber;    
    ASN1OctetString    dataGroupHashValue;
    
    public static DataGroupHash getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof DataGroupHash)
        {
            return (DataGroupHash)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new DataGroupHash(ASN1Sequence.getInstance(obj));            
        }
        else
        {
            throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
        }
    }                
            
    public DataGroupHash(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // dataGroupNumber
        dataGroupNumber = DERInteger.getInstance(e.nextElement());
        // dataGroupHashValue
        dataGroupHashValue = ASN1OctetString.getInstance(e.nextElement());   
    }
    
    public DataGroupHash(
        int dataGroupNumber,        
        ASN1OctetString     dataGroupHashValue)
    {
        this.dataGroupNumber = new DERInteger(dataGroupNumber);
        this.dataGroupHashValue = dataGroupHashValue; 
    }    

    public int getDataGroupNumber()
    {
        return dataGroupNumber.getValue().intValue();
    }
    
    public ASN1OctetString getDataGroupHashValue()
    {
        return dataGroupHashValue;
    }     
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(dataGroupNumber);
        seq.add(dataGroupHashValue);  

        return new DERSequence(seq);
    }
}
