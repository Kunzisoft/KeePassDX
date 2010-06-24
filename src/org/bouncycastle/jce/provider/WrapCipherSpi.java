package org.bouncycastle.jce.provider;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
//import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
//import org.bouncycastle.crypto.engines.DESedeEngine;
//import org.bouncycastle.crypto.engines.DESedeWrapEngine;
//import org.bouncycastle.crypto.engines.RC2WrapEngine;
//import org.bouncycastle.crypto.engines.RFC3211WrapEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public abstract class WrapCipherSpi extends CipherSpi
    implements PBE
{
    //
    // specs we can handle.
    //
    @SuppressWarnings("unchecked")
	private Class[]                 availableSpecs =
                                    {
                                        IvParameterSpec.class,
                                        PBEParameterSpec.class,
                                        RC2ParameterSpec.class,
                                        RC5ParameterSpec.class
                                    };

    protected int                     pbeType = PKCS12;
    protected int                     pbeHash = SHA1;
    protected int                     pbeKeySize;
    protected int                     pbeIvSize;

    protected AlgorithmParameters     engineParams = null;

    protected Wrapper                 wrapEngine = null;

    private int                       ivSize;
    private byte[]                    iv;

    protected WrapCipherSpi()
    {
    }

    protected WrapCipherSpi(
        Wrapper wrapEngine)
    {
        this(wrapEngine, 0);
    }

    protected WrapCipherSpi(
        Wrapper wrapEngine,
        int     ivSize)
    {
        this.wrapEngine = wrapEngine;
        this.ivSize = ivSize;
    }

    protected int engineGetBlockSize()
    {
        return 0;
    }

    protected byte[] engineGetIV()
    {
        return (byte[])iv.clone();
    }

    protected int engineGetKeySize(
        Key     key)
    {
        return key.getEncoded().length;
    }

    protected int engineGetOutputSize(
        int     inputLen)
    {
        return -1;
    }

    protected AlgorithmParameters engineGetParameters()
    {
        return null;
    }

    protected void engineSetMode(
        String  mode)
        throws NoSuchAlgorithmException
    {
        throw new NoSuchAlgorithmException("can't support mode " + mode);
    }

    protected void engineSetPadding(
        String  padding)
    throws NoSuchPaddingException
    {
        throw new NoSuchPaddingException("Padding " + padding + " unknown.");
    }

    protected void engineInit(
        int                     opmode,
        Key                     key,
        AlgorithmParameterSpec  params,
        SecureRandom            random)
    throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        CipherParameters        param;

        if (key instanceof JCEPBEKey)
        {
            JCEPBEKey   k = (JCEPBEKey)key;
            
            if (params instanceof PBEParameterSpec)
            {
                param = PBE.Util.makePBEParameters(k, params, wrapEngine.getAlgorithmName());
            }
            else if (k.getParam() != null)
            {
                param = k.getParam();
            }
            else
            {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
        }
        else
        {
            param = new KeyParameter(key.getEncoded());
        }

        if (params instanceof javax.crypto.spec.IvParameterSpec)
        {
            IvParameterSpec iv = (IvParameterSpec) params;
            param = new ParametersWithIV(param, iv.getIV());
        }

        if (param instanceof KeyParameter && ivSize != 0)
        {
            iv = new byte[ivSize];
            random.nextBytes(iv);
            param = new ParametersWithIV(param, iv);
        }

        switch (opmode)
        {
        case Cipher.WRAP_MODE:
            wrapEngine.init(true, param);
            break;
        case Cipher.UNWRAP_MODE:
            wrapEngine.init(false, param);
            break;
        case Cipher.ENCRYPT_MODE:
        case Cipher.DECRYPT_MODE:
            throw new IllegalArgumentException("engine only valid for wrapping");
        default:
            System.out.println("eeek!");
        }
    }

    @SuppressWarnings("unchecked")
	protected void engineInit(
        int                 opmode,
        Key                 key,
        AlgorithmParameters params,
        SecureRandom        random)
    throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        AlgorithmParameterSpec  paramSpec = null;

        if (params != null)
        {
            for (int i = 0; i != availableSpecs.length; i++)
            {
                try
                {
                    paramSpec = params.getParameterSpec(availableSpecs[i]);
                    break;
                }
                catch (Exception e)
                {
                    // try next spec
                }
            }

            if (paramSpec == null)
            {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + params.toString());
            }
        }

        engineParams = params;
        engineInit(opmode, key, paramSpec, random);
    }

    protected void engineInit(
        int                 opmode,
        Key                 key,
        SecureRandom        random)
        throws InvalidKeyException
    {
        try
        {
            engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    protected byte[] engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen)
    {
        throw new RuntimeException("not supported for wrapping");
    }

    protected int engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset)
        throws ShortBufferException
    {
        throw new RuntimeException("not supported for wrapping");
    }

    protected byte[] engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen)
        throws IllegalBlockSizeException, BadPaddingException
    {
        return null;
    }

    protected int engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset)
        throws IllegalBlockSizeException, BadPaddingException
    {
        return 0;
    }

    protected byte[] engineWrap(
        Key     key)
    throws IllegalBlockSizeException, java.security.InvalidKeyException
    {
        byte[] encoded = key.getEncoded();
        if (encoded == null)
        {
            throw new InvalidKeyException("Cannot wrap key, null encoding.");
        }

        try
        {
            if (wrapEngine == null)
            {
                return engineDoFinal(encoded, 0, encoded.length);
            }
            else
            {
                return wrapEngine.wrap(encoded, 0, encoded.length);
            }
        }
        catch (BadPaddingException e)
        {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    protected Key engineUnwrap(
        byte[]  wrappedKey,
        String  wrappedKeyAlgorithm,
        int     wrappedKeyType)
    throws InvalidKeyException
    {
        byte[] encoded;
        try
        {
            if (wrapEngine == null)
            {
                encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            }
            else
            {
                encoded = wrapEngine.unwrap(wrappedKey, 0, wrappedKey.length);
            }
        }
        catch (InvalidCipherTextException e)
        {
            throw new InvalidKeyException(e.getMessage());
        }
        catch (BadPaddingException e)
        {
            throw new InvalidKeyException(e.getMessage());
        }
        catch (IllegalBlockSizeException e2)
        {
            throw new InvalidKeyException(e2.getMessage());
        }

        if (wrappedKeyType == Cipher.SECRET_KEY)
        {
            return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
        }
        else if (wrappedKeyAlgorithm.equals("") && wrappedKeyType == Cipher.PRIVATE_KEY)
        {
            /*
             * The caller doesnt know the algorithm as it is part of
             * the encrypted data.
             */
            ASN1InputStream bIn = new ASN1InputStream(encoded);
            PrivateKey      privKey;

            try
            {
                ASN1Sequence         s = (ASN1Sequence)bIn.readObject();
                PrivateKeyInfo       in = new PrivateKeyInfo(s);

                DERObjectIdentifier  oid = in.getAlgorithmId().getObjectId();

                /*
                if (oid.equals(X9ObjectIdentifiers.id_ecPublicKey))
                {
                    privKey = new JCEECPrivateKey(in);
                }
                else if (oid.equals(CryptoProObjectIdentifiers.gostR3410_94))
                {
                    privKey = new JDKGOST3410PrivateKey(in);
                }
                
                else*/ if (oid.equals(X9ObjectIdentifiers.id_dsa))
                {
                    privKey = new JDKDSAPrivateKey(in);
                }
                else if (oid.equals(PKCSObjectIdentifiers.dhKeyAgreement))
                {
                    privKey = new JCEDHPrivateKey(in);
                }
                else if (oid.equals(X9ObjectIdentifiers.dhpublicnumber))
                {
                    privKey = new JCEDHPrivateKey(in);
                }
                else    // the old standby!
                {
                    privKey = new JCERSAPrivateCrtKey(in);
                }
            }
            catch (Exception e)
            {
                throw new InvalidKeyException("Invalid key encoding.");
            }

            return privKey;
        }
        else
        {
            try
            {
                KeyFactory kf = KeyFactory.getInstance(wrappedKeyAlgorithm, "BC");
                if (wrappedKeyType == Cipher.PUBLIC_KEY)
                {
                    return kf.generatePublic(new X509EncodedKeySpec(encoded));
                }
                else if (wrappedKeyType == Cipher.PRIVATE_KEY)
                {
                    return kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
                }
            }
            catch (NoSuchProviderException e)
            {
                throw new InvalidKeyException("Unknown key type " + e.getMessage());
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new InvalidKeyException("Unknown key type " + e.getMessage());
            }
            catch (InvalidKeySpecException e2)
            {
                throw new InvalidKeyException("Unknown key type " + e2.getMessage());
            }

            throw new InvalidKeyException("Unknown key type " + wrappedKeyType);
        }
    }

    //
    // classes that inherit directly from us
    //


    /*
    public static class DESEDEWrap
        extends WrapCipherSpi
    {
        public DESEDEWrap()
        {
            super(new DESedeWrapEngine());
        }
    }

    public static class RC2Wrap
        extends WrapCipherSpi
    {
        public RC2Wrap()
        {
            super(new RC2WrapEngine());
        }
    }

    public static class RFC3211DESedeWrap
        extends WrapCipherSpi
    {
        public RFC3211DESedeWrap()
        {
            super(new RFC3211WrapEngine(new DESedeEngine()), 8);
        }
    }
    */
}
