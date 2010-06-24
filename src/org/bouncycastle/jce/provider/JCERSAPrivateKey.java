package org.bouncycastle.jce.provider;

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Enumeration;

@SuppressWarnings("unchecked")
public class JCERSAPrivateKey
    implements RSAPrivateKey, PKCS12BagAttributeCarrier
{
    static final long serialVersionUID = 5110188922551353628L;

    private static BigInteger ZERO = BigInteger.valueOf(0);

    protected BigInteger modulus;
    protected BigInteger privateExponent;

    private PKCS12BagAttributeCarrierImpl   attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected JCERSAPrivateKey()
    {
    }

    JCERSAPrivateKey(
        RSAKeyParameters key)
    {
        this.modulus = key.getModulus();
        this.privateExponent = key.getExponent();
    }

    JCERSAPrivateKey(
        RSAPrivateKeySpec spec)
    {
        this.modulus = spec.getModulus();
        this.privateExponent = spec.getPrivateExponent();
    }

    JCERSAPrivateKey(
        RSAPrivateKey key)
    {
        this.modulus = key.getModulus();
        this.privateExponent = key.getPrivateExponent();
    }

    public BigInteger getModulus()
    {
        return modulus;
    }

    public BigInteger getPrivateExponent()
    {
        return privateExponent;
    }

    public String getAlgorithm()
    {
        return "RSA";
    }

    public String getFormat()
    {
        return "PKCS#8";
    }

    public byte[] getEncoded()
    {
        PrivateKeyInfo info = new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, new DERNull()), new RSAPrivateKeyStructure(getModulus(), ZERO, getPrivateExponent(), ZERO, ZERO, ZERO, ZERO, ZERO).getDERObject());

        return info.getDEREncoded();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof RSAPrivateKey))
        {
            return false;
        }

        if (o == this)
        {
            return true;
        }

        RSAPrivateKey key = (RSAPrivateKey)o;

        return getModulus().equals(key.getModulus())
            && getPrivateExponent().equals(key.getPrivateExponent());
    }

    public int hashCode()
    {
        return getModulus().hashCode() ^ getPrivateExponent().hashCode();
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

    private void readObject(
        ObjectInputStream   in)
        throws IOException, ClassNotFoundException
    {
        this.modulus = (BigInteger)in.readObject();
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        
        attrCarrier.readObject(in);

        this.privateExponent = (BigInteger)in.readObject();
    }

    private void writeObject(
        ObjectOutputStream  out)
        throws IOException
    {
        out.writeObject(modulus);

        attrCarrier.writeObject(out);

        out.writeObject(privateExponent);
    }
}
