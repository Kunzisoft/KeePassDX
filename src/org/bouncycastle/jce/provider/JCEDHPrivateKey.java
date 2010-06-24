package org.bouncycastle.jce.provider;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.DHParameter;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class JCEDHPrivateKey
    implements DHPrivateKey, PKCS12BagAttributeCarrier
{
    static final long serialVersionUID = 311058815616901812L;
    
    BigInteger      x;

    DHParameterSpec dhSpec;

    private PKCS12BagAttributeCarrier attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected JCEDHPrivateKey()
    {
    }

    JCEDHPrivateKey(
        DHPrivateKey    key)
    {
        this.x = key.getX();
        this.dhSpec = key.getParams();
    }

    JCEDHPrivateKey(
        DHPrivateKeySpec    spec)
    {
        this.x = spec.getX();
        this.dhSpec = new DHParameterSpec(spec.getP(), spec.getG());
    }

    JCEDHPrivateKey(
        PrivateKeyInfo  info)
    {
        DHParameter     params = new DHParameter((ASN1Sequence)info.getAlgorithmId().getParameters());
        DERInteger      derX = (DERInteger)info.getPrivateKey();

        this.x = derX.getValue();
        if (params.getL() != null)
        {
            this.dhSpec = new DHParameterSpec(params.getP(), params.getG(), params.getL().intValue());
        }
        else
        {
            this.dhSpec = new DHParameterSpec(params.getP(), params.getG());
        }
    }

    JCEDHPrivateKey(
        DHPrivateKeyParameters  params)
    {
        this.x = params.getX();
        this.dhSpec = new DHParameterSpec(params.getParameters().getP(), params.getParameters().getG(), params.getParameters().getL());
    }

    public String getAlgorithm()
    {
        return "DH";
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        PrivateKeyInfo          info = new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, new DHParameter(dhSpec.getP(), dhSpec.getG(), dhSpec.getL()).getDERObject()), new DERInteger(getX()));

        return info.getDEREncoded();
    }

    public DHParameterSpec getParams()
    {
        return dhSpec;
    }

    public BigInteger getX()
    {
        return x;
    }

    private void readObject(
        ObjectInputStream   in)
        throws IOException, ClassNotFoundException
    {
        x = (BigInteger)in.readObject();

        this.dhSpec = new DHParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), in.readInt());
    }

    private void writeObject(
        ObjectOutputStream  out)
        throws IOException
    {
        out.writeObject(this.getX());
        out.writeObject(dhSpec.getP());
        out.writeObject(dhSpec.getG());
        out.writeInt(dhSpec.getL());
    }

    public void setBagAttribute(
        DERObjectIdentifier oid,
        DEREncodable        attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public DEREncodable getBagAttribute(
        DERObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }
}
