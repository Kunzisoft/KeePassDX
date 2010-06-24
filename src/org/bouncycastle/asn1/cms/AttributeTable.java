package org.bouncycastle.asn1.cms;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERObjectIdentifier;

@SuppressWarnings("unchecked")
public class AttributeTable
{
    private Hashtable attributes = new Hashtable();

    public AttributeTable(
        Hashtable  attrs)
    {
        attributes = copyTable(attrs);
    }

    public AttributeTable(
        DEREncodableVector v)
    {
        for (int i = 0; i != v.size(); i++)
        {
            Attribute   a = Attribute.getInstance(v.get(i));

            addAttribute(a.getAttrType(), a);
        }
    }

    public AttributeTable(
        ASN1Set    s)
    {
        for (int i = 0; i != s.size(); i++)
        {
            Attribute   a = Attribute.getInstance(s.getObjectAt(i));

            addAttribute(a.getAttrType(), a);
        }
    }

    private void addAttribute(
        DERObjectIdentifier oid,
        Attribute           a)
    {
        Object value = attributes.get(oid);
        
        if (value == null)
        {
            attributes.put(oid, a);
        }
        else
        {
            Vector v;
            
            if (value instanceof Attribute)
            {
                v = new Vector();
                
                v.addElement(value);
                v.addElement(a);
            }
            else
            {
                v = (Vector)value;
            
                v.addElement(a);
            }
            
            attributes.put(oid, v);
        }
    }
    
    /**
     * Return the first attribute matching the OBJECT IDENTIFIER oid.
     * 
     * @param oid type of attribute required.
     * @return first attribute found of type oid.
     */
    public Attribute get(
        DERObjectIdentifier oid)
    {
        Object value = attributes.get(oid);
        
        if (value instanceof Vector)
        {
            return (Attribute)((Vector)value).elementAt(0);
        }
        
        return (Attribute)value;
    }

    /**
     * Return all the attributes matching the OBJECT IDENTIFIER oid. The vector will be 
     * empty if there are no attributes of the required type present.
     * 
     * @param oid type of attribute required.
     * @return a vector of all the attributes found of type oid.
     */
    public ASN1EncodableVector getAll(
        DERObjectIdentifier oid)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        Object value = attributes.get(oid);
        
        if (value instanceof Vector)
        {
            Enumeration e = ((Vector)value).elements();
            
            while (e.hasMoreElements())
            {
                v.add((Attribute)e.nextElement());
            }
        }
        else if (value != null)
        {
            v.add((Attribute)value);
        }
        
        return v;
    }
    
    public Hashtable toHashtable()
    {
        return copyTable(attributes);
    }
    
    public ASN1EncodableVector toASN1EncodableVector()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        Enumeration          e = attributes.elements();
        
        while (e.hasMoreElements())
        {
            Object value = e.nextElement();
            
            if (value instanceof Vector)
            {
                Enumeration en = ((Vector)value).elements();
                
                while (en.hasMoreElements())
                {
                    v.add(Attribute.getInstance(en.nextElement()));
                }
            }
            else
            {
                v.add(Attribute.getInstance(value));
            }
        }
        
        return v;
    }
    
    private Hashtable copyTable(
        Hashtable in)
    {
        Hashtable   out = new Hashtable();
        Enumeration e = in.keys();
        
        while (e.hasMoreElements())
        {
            Object key = e.nextElement();
            
            out.put(key, in.get(key));
        }
        
        return out;
    }
}
