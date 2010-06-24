package org.bouncycastle.jce.provider;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.StreamCipher;
//import org.bouncycastle.crypto.engines.BlowfishEngine;
//import org.bouncycastle.crypto.engines.DESEngine;
//import org.bouncycastle.crypto.engines.DESedeEngine;
//import org.bouncycastle.crypto.engines.HC128Engine;
//import org.bouncycastle.crypto.engines.HC256Engine;
//import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
//import org.bouncycastle.crypto.engines.SkipjackEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;
//import org.bouncycastle.crypto.engines.VMPCEngine;
//import org.bouncycastle.crypto.engines.VMPCKSA3Engine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

@SuppressWarnings("unchecked")
public class JCEStreamCipher
    extends WrapCipherSpi implements PBE
{
    //
    // specs we can handle.
    //
    private Class[]                 availableSpecs =
                                    {
                                        RC2ParameterSpec.class,
                                        RC5ParameterSpec.class,
                                        IvParameterSpec.class,
                                        PBEParameterSpec.class
                                    };

    private StreamCipher       cipher;
    private ParametersWithIV   ivParam;

    private int                     ivLength = 0;

    private PBEParameterSpec        pbeSpec = null;
    private String                  pbeAlgorithm = null;

    protected JCEStreamCipher(
        StreamCipher engine,
        int          ivLength)
    {
        cipher = engine;
        this.ivLength = ivLength;
    }
        
    protected JCEStreamCipher(
        BlockCipher engine,
        int         ivLength)
    {
        this.ivLength = ivLength;

        cipher = new StreamBlockCipher(engine);
    }

    protected int engineGetBlockSize() 
    {
        return 0;
    }

    protected byte[] engineGetIV() 
    {
        return (ivParam != null) ? ivParam.getIV() : null;
    }

    protected int engineGetKeySize(
        Key     key) 
    {
        return key.getEncoded().length * 8;
    }

    protected int engineGetOutputSize(
        int     inputLen) 
    {
        return inputLen;
    }

    protected AlgorithmParameters engineGetParameters() 
    {
        if (engineParams == null)
        {
            if (pbeSpec != null)
            {
                try
                {
                    AlgorithmParameters engineParams = AlgorithmParameters.getInstance(pbeAlgorithm, "BC");
                    engineParams.init(pbeSpec);
                    
                    return engineParams;
                }
                catch (Exception e)
                {
                    return null;
                }
            }
        }
        
        return engineParams;
    }

    /**
     * should never be called.
     */
    protected void engineSetMode(
        String  mode) 
    {
        if (!mode.equalsIgnoreCase("ECB"))
        {
            throw new IllegalArgumentException("can't support mode " + mode);
        }
    }

    /**
     * should never be called.
     */
    protected void engineSetPadding(
        String  padding) 
    throws NoSuchPaddingException
    {
        if (!padding.equalsIgnoreCase("NoPadding"))
        {
            throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    protected void engineInit(
        int                     opmode,
        Key                     key,
        AlgorithmParameterSpec  params,
        SecureRandom            random) 
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        CipherParameters        param;

        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        
        this.engineParams = null;
        
        //
        // basic key check
        //
        if (!(key instanceof SecretKey))
        {
            throw new InvalidKeyException("Key for algorithm " + key.getAlgorithm() + " not suitable for symmetric enryption.");
        }
        
        if (key instanceof JCEPBEKey)
        {
            JCEPBEKey   k = (JCEPBEKey)key;
            
            if (k.getOID() != null)
            {
                pbeAlgorithm = k.getOID().getId();
            }
            else
            {
                pbeAlgorithm = k.getAlgorithm();
            }
            
            if (k.getParam() != null)
            {
                param = k.getParam();                
                pbeSpec = new PBEParameterSpec(k.getSalt(), k.getIterationCount());
            }
            else if (params instanceof PBEParameterSpec)
            {
                param = PBE.Util.makePBEParameters(k, params, cipher.getAlgorithmName());
                pbeSpec = (PBEParameterSpec)params;
            }
            else
            {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            
            if (k.getIvSize() != 0)
            {
                ivParam = (ParametersWithIV)param;
            }
        }
        else if (params == null)
        {
            param = new KeyParameter(key.getEncoded());
        }
        else if (params instanceof IvParameterSpec)
        {
            param = new ParametersWithIV(new KeyParameter(key.getEncoded()), ((IvParameterSpec)params).getIV());
            ivParam = (ParametersWithIV)param;
        }
        else
        {
            throw new IllegalArgumentException("unknown parameter type.");
        }

        if ((ivLength != 0) && !(param instanceof ParametersWithIV))
        {
            SecureRandom    ivRandom = random;

            if (ivRandom == null)
            {
                ivRandom = new SecureRandom();
            }

            if ((opmode == Cipher.ENCRYPT_MODE) || (opmode == Cipher.WRAP_MODE))
            {
                byte[]  iv = new byte[ivLength];

                ivRandom.nextBytes(iv);
                param = new ParametersWithIV(param, iv);
                ivParam = (ParametersWithIV)param;
            }
            else
            {
                throw new InvalidAlgorithmParameterException("no IV set when one expected");
            }
        }

        switch (opmode)
        {
        case Cipher.ENCRYPT_MODE:
        case Cipher.WRAP_MODE:
            cipher.init(true, param);
            break;
        case Cipher.DECRYPT_MODE:
        case Cipher.UNWRAP_MODE:
            cipher.init(false, param);
            break;
        default:
            System.out.println("eeek!");
        }
    }

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
                    continue;
                }
            }

            if (paramSpec == null)
            {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + params.toString());
            }
        }

        engineInit(opmode, key, paramSpec, random);
        engineParams = params;
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
            throw new InvalidKeyException(e.getMessage());
        }
    }

    protected byte[] engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen) 
    {
        byte[]  out = new byte[inputLen];

        cipher.processBytes(input, inputOffset, inputLen, out, 0);

        return out;
    }

    protected int engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset) 
        throws ShortBufferException 
    {
        try
        {
        cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);

        return inputLen;
        }
        catch (DataLengthException e)
        {
            throw new ShortBufferException(e.getMessage());
        }
    }

    protected byte[] engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen) 
    {
        if (inputLen != 0)
        {
            byte[] out = engineUpdate(input, inputOffset, inputLen);

            cipher.reset();
            
            return out;
        }

        cipher.reset();
        
        return new byte[0];
    }

    protected int engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset) 
    {
        if (inputLen != 0)
        {
            cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        }

        cipher.reset();
        
        return inputLen;
    }

    /*
     * The ciphers that inherit from us.
     */

    /**
     * DES
     */
    /*
    static public class DES_CFB8
        extends JCEStreamCipher
    {
        public DES_CFB8()
        {
            super(new CFBBlockCipher(new DESEngine(), 8), 64);
        }
    }
    */

    /**
     * DESede
     */
    /*
    static public class DESede_CFB8
        extends JCEStreamCipher
    {
        public DESede_CFB8()
        {
            super(new CFBBlockCipher(new DESedeEngine(), 8), 64);
        }
    }
    */

    /**
     * SKIPJACK
     */
    /*
    static public class Skipjack_CFB8
        extends JCEStreamCipher
    {
        public Skipjack_CFB8()
        {
            super(new CFBBlockCipher(new SkipjackEngine(), 8), 64);
        }
    }
    */

    /**
     * Blowfish
     */
    /*
    static public class Blowfish_CFB8
        extends JCEStreamCipher
    {
        public Blowfish_CFB8()
        {
            super(new CFBBlockCipher(new BlowfishEngine(), 8), 64);
        }
    }
    */

    /**
     * Twofish
     */
    static public class Twofish_CFB8
        extends JCEStreamCipher
    {
        public Twofish_CFB8()
        {
            super(new CFBBlockCipher(new TwofishEngine(), 8), 128);
        }
    }

    /**
     * DES
     */
    /*
    static public class DES_OFB8
        extends JCEStreamCipher
    {
        public DES_OFB8()
        {
            super(new OFBBlockCipher(new DESEngine(), 8), 64);
        }
    }
    */

    /**
     * DESede
     */
    /*
    static public class DESede_OFB8
        extends JCEStreamCipher
    {
        public DESede_OFB8()
        {
            super(new OFBBlockCipher(new DESedeEngine(), 8), 64);
        }
    }
    */

    /**
     * SKIPJACK
     */
    /*
    static public class Skipjack_OFB8
        extends JCEStreamCipher
    {
        public Skipjack_OFB8()
        {
            super(new OFBBlockCipher(new SkipjackEngine(), 8), 64);
        }
    }
    */

    /**
     * Blowfish
     */
    /*
    static public class Blowfish_OFB8
        extends JCEStreamCipher
    {
        public Blowfish_OFB8()
        {
            super(new OFBBlockCipher(new BlowfishEngine(), 8), 64);
        }
    }
    */

    /**
     * Twofish
     */
    static public class Twofish_OFB8
        extends JCEStreamCipher
    {
        public Twofish_OFB8()
        {
            super(new OFBBlockCipher(new TwofishEngine(), 8), 128);
        }
    }

    /**
     * RC4
     */
    /*
    static public class RC4
        extends JCEStreamCipher
    {
        public RC4()
        {
            super(new RC4Engine(), 0);
        }
    }
    */

    /**
     * PBEWithSHAAnd128BitRC4
     */
    /*
    static public class PBEWithSHAAnd128BitRC4
        extends JCEStreamCipher
    {
        public PBEWithSHAAnd128BitRC4()
        {
            super(new RC4Engine(), 0);
        }
    }
	*/
    /**
     * PBEWithSHAAnd40BitRC4
     */
    /*
    static public class PBEWithSHAAnd40BitRC4
        extends JCEStreamCipher
    {
        public PBEWithSHAAnd40BitRC4()
        {
            super(new RC4Engine(), 0);
        }
    }
    */

    /**
     * Salsa20
     */
    static public class Salsa20
        extends JCEStreamCipher
    {
        public Salsa20()
        {
            super(new Salsa20Engine(), 8);
        }
    }

    /**
     * HC-128
     */
    /*
    static public class HC128
        extends JCEStreamCipher
    {
        public HC128()
        {
            super(new HC128Engine(), 16);
        }
    }
    */

    /**
     * HC-256
     */
    /*
    static public class HC256
        extends JCEStreamCipher
    {
        public HC256()
        {
            super(new HC256Engine(), 32);
        }
    }
    */

    /**
     * VMPC
     */
    /*
    static public class VMPC
        extends JCEStreamCipher
    {
        public VMPC()
        {
            super(new VMPCEngine(), 16);
        }
    }
    */

    /**
     * VMPC-KSA3
     */
    /*
    static public class VMPCKSA3
        extends JCEStreamCipher
    {
        public VMPCKSA3()
        {
            super(new VMPCKSA3Engine(), 16);
        }
    }
    */
}
