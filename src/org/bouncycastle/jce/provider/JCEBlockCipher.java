package org.bouncycastle.jce.provider;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
/*
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.engines.CAST5Engine;
import org.bouncycastle.crypto.engines.CAST6Engine;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.GOST28147Engine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.engines.RC532Engine;
import org.bouncycastle.crypto.engines.RC564Engine;
import org.bouncycastle.crypto.engines.RC6Engine;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.engines.SEEDEngine;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.bouncycastle.crypto.engines.SkipjackEngine;
import org.bouncycastle.crypto.engines.TEAEngine;
*/
import org.bouncycastle.crypto.engines.TwofishEngine;
//import org.bouncycastle.crypto.engines.XTEAEngine;
//import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
//import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.CTSBlockCipher;
/*
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GOFBBlockCipher;
*/
import org.bouncycastle.crypto.modes.OFBBlockCipher;
/*
import org.bouncycastle.crypto.modes.OpenPGPCFBBlockCipher;
import org.bouncycastle.crypto.modes.PGPCFBBlockCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
*/
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
/*
import org.bouncycastle.crypto.paddings.TBCPadding;
import org.bouncycastle.crypto.paddings.X923Padding;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
*/
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
/*
import org.bouncycastle.crypto.params.ParametersWithSBox;
import org.bouncycastle.crypto.params.RC2Parameters;
import org.bouncycastle.crypto.params.RC5Parameters;
import org.bouncycastle.jce.spec.GOST28147ParameterSpec;
*/
import org.bouncycastle.util.Strings;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
/*
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
*/
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

@SuppressWarnings("unchecked")
public class JCEBlockCipher extends WrapCipherSpi
    implements PBE
{
    //
    // specs we can handle.
    //
	private Class[]                 availableSpecs =
                                    {
                                        //RC2ParameterSpec.class,
                                        //RC5ParameterSpec.class,
                                        IvParameterSpec.class,
                                        PBEParameterSpec.class,
                                        //GOST28147ParameterSpec.class
                                    };
 
    private BlockCipher             baseEngine;
    private GenericBlockCipher      cipher;
    private ParametersWithIV        ivParam;

    private int                     ivLength = 0;

    private boolean                 padded;
    
    private PBEParameterSpec        pbeSpec = null;
    private String                  pbeAlgorithm = null;
    
    private String                  modeName = null;

    protected JCEBlockCipher(
        BlockCipher engine)
    {
        baseEngine = engine;

        cipher = new BufferedGenericBlockCipher(engine);
    }
        
    protected JCEBlockCipher(
        BlockCipher engine,
        int         ivLength)
    {
        baseEngine = engine;

        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected JCEBlockCipher(
        BufferedBlockCipher engine,
        int                 ivLength)
    {
        baseEngine = engine.getUnderlyingCipher();

        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected int engineGetBlockSize() 
    {
        return baseEngine.getBlockSize();
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
        return cipher.getOutputSize(inputLen);
    }

    protected AlgorithmParameters engineGetParameters() 
    {
        if (engineParams == null)
        {
            if (pbeSpec != null)
            {
                try
                {
                    engineParams = AlgorithmParameters.getInstance(pbeAlgorithm, "BC");
                    engineParams.init(pbeSpec);
                }
                catch (Exception e)
                {
                    return null;
                }
            }
            else if (ivParam != null)
            {
                String  name = cipher.getUnderlyingCipher().getAlgorithmName();

                if (name.indexOf('/') >= 0)
                {
                    name = name.substring(0, name.indexOf('/'));
                }

                try
                {
                    engineParams = AlgorithmParameters.getInstance(name, "BC");
                    engineParams.init(ivParam.getIV());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.toString());
                }
            }
        }

        return engineParams;
    }

    protected void engineSetMode(
        String  mode)
        throws NoSuchAlgorithmException
    {
        modeName = Strings.toUpperCase(mode);

        if (modeName.equals("ECB"))
        {
            ivLength = 0;
            cipher = new BufferedGenericBlockCipher(baseEngine);
        }
        else if (modeName.equals("CBC"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new BufferedGenericBlockCipher(
                            new CBCBlockCipher(baseEngine));
        }
        else if (modeName.startsWith("OFB"))
        {
            ivLength = baseEngine.getBlockSize();
            if (modeName.length() != 3)
            {
                int wordSize = Integer.parseInt(modeName.substring(3));

                cipher = new BufferedGenericBlockCipher(
                                new OFBBlockCipher(baseEngine, wordSize));
            }
            else
            {
                cipher = new BufferedGenericBlockCipher(
                        new OFBBlockCipher(baseEngine, 8 * baseEngine.getBlockSize()));
            }
        }
        else if (modeName.startsWith("CFB"))
        {
            ivLength = baseEngine.getBlockSize();
            if (modeName.length() != 3)
            {
                int wordSize = Integer.parseInt(modeName.substring(3));

                cipher = new BufferedGenericBlockCipher(
                                new CFBBlockCipher(baseEngine, wordSize));
            }
            else
            {
                cipher = new BufferedGenericBlockCipher(
                        new CFBBlockCipher(baseEngine, 8 * baseEngine.getBlockSize()));
            }
        }
        /*
        else if (modeName.startsWith("PGP"))
        {
            boolean inlineIV = modeName.equalsIgnoreCase("PGPCFBwithIV");

            ivLength = baseEngine.getBlockSize();
            cipher = new BufferedGenericBlockCipher(
                new PGPCFBBlockCipher(baseEngine, inlineIV));
        }
        else if (modeName.equalsIgnoreCase("OpenPGPCFB"))
        {
            ivLength = 0;
            cipher = new BufferedGenericBlockCipher(
                new OpenPGPCFBBlockCipher(baseEngine));
        }
        else if (modeName.startsWith("SIC"))
        {
            ivLength = baseEngine.getBlockSize();
            if (ivLength < 16)
            {
                throw new IllegalArgumentException("Warning: SIC-Mode can become a twotime-pad if the blocksize of the cipher is too small. Use a cipher with a block size of at least 128 bits (e.g. AES)");
            }
            cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(
                        new SICBlockCipher(baseEngine)));
        }
        else if (modeName.startsWith("CTR"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(
                        new SICBlockCipher(baseEngine)));
        }
        else if (modeName.startsWith("GOFB"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(
                        new GOFBBlockCipher(baseEngine)));
        }
        else if (modeName.startsWith("CTS"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(new CBCBlockCipher(baseEngine)));
        }
        else if (modeName.startsWith("CCM"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new AEADGenericBlockCipher(new CCMBlockCipher(baseEngine));
        }
        else if (modeName.startsWith("EAX"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new AEADGenericBlockCipher(new EAXBlockCipher(baseEngine));
        }
        else if (modeName.startsWith("GCM"))
        {
            ivLength = baseEngine.getBlockSize();
            cipher = new AEADGenericBlockCipher(new GCMBlockCipher(baseEngine));
        }
        */
        else
        {
            throw new NoSuchAlgorithmException("can't support mode " + mode);
        }
    }

    protected void engineSetPadding(
        String  padding) 
    throws NoSuchPaddingException
    {
        String  paddingName = Strings.toUpperCase(padding);

        if (paddingName.equals("NOPADDING"))
        {
            if (cipher.wrapOnNoPadding())
            {
                cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(cipher.getUnderlyingCipher()));
            }
        }
        else if (paddingName.equals("WITHCTS"))
        {
            cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(cipher.getUnderlyingCipher()));
        }
        else
        {
            padded = true;

            if (isAEADModeName(modeName))
            {
                throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
            }
            else if (paddingName.equals("PKCS5PADDING") || paddingName.equals("PKCS7PADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher());
            }
            /*
            else if (paddingName.equals("ZEROBYTEPADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher(), new ZeroBytePadding());
            }
            else if (paddingName.equals("ISO10126PADDING") || paddingName.equals("ISO10126-2PADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            }
            else if (paddingName.equals("X9.23PADDING") || paddingName.equals("X923PADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher(), new X923Padding());
            }
            else if (paddingName.equals("ISO7816-4PADDING") || paddingName.equals("ISO9797-1PADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher(), new ISO7816d4Padding());
            }
            else if (paddingName.equals("TBCPADDING"))
            {
                cipher = new BufferedGenericBlockCipher(cipher.getUnderlyingCipher(), new TBCPadding());
            }
            */
            else
            {
                throw new NoSuchPaddingException("Padding " + padding + " unknown.");
            }
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
        
        //
        // for RC5-64 we must have some default parameters
        //
        if (params == null && baseEngine.getAlgorithmName().startsWith("RC5-64"))
        {
            throw new InvalidAlgorithmParameterException("RC5 requires an RC5ParametersSpec to be passed in.");
        }

        //
        // a note on iv's - if ivLength is zero the IV gets ignored (we don't use it).
        //
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
                pbeSpec = (PBEParameterSpec)params;
                param = PBE.Util.makePBEParameters(k, params, cipher.getUnderlyingCipher().getAlgorithmName());
            }
            else
            {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }

            if (param instanceof ParametersWithIV)
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
            if (ivLength != 0)
            {
                IvParameterSpec p = (IvParameterSpec)params;

                if (p.getIV().length != ivLength && !isAEADModeName(modeName))
                {
                    throw new InvalidAlgorithmParameterException("IV must be " + ivLength + " bytes long.");
                }

                param = new ParametersWithIV(new KeyParameter(key.getEncoded()), p.getIV());
                ivParam = (ParametersWithIV)param;
            }
            else
            {
                if (modeName != null && modeName.equals("ECB"))
                {
                    throw new InvalidAlgorithmParameterException("ECB mode does not use an IV");
                }
                
                param = new KeyParameter(key.getEncoded());
            }
        }
        /*
        else if (params instanceof GOST28147ParameterSpec)
        {
            GOST28147ParameterSpec    gost28147Param = (GOST28147ParameterSpec)params;

            param = new ParametersWithSBox(
                       new KeyParameter(key.getEncoded()), ((GOST28147ParameterSpec)params).getSbox());

            if (gost28147Param.getIV() != null && ivLength != 0)
            {
                param = new ParametersWithIV(param, gost28147Param.getIV());
                ivParam = (ParametersWithIV)param;
            }
        }
        else if (params instanceof RC2ParameterSpec)
        {
            RC2ParameterSpec    rc2Param = (RC2ParameterSpec)params;

            param = new RC2Parameters(key.getEncoded(), ((RC2ParameterSpec)params).getEffectiveKeyBits());

            if (rc2Param.getIV() != null && ivLength != 0)
            {
                param = new ParametersWithIV(param, rc2Param.getIV());
                ivParam = (ParametersWithIV)param;
            }
        }
        else if (params instanceof RC5ParameterSpec)
        {
            RC5ParameterSpec    rc5Param = (RC5ParameterSpec)params;

            param = new RC5Parameters(key.getEncoded(), ((RC5ParameterSpec)params).getRounds());
            if (baseEngine.getAlgorithmName().startsWith("RC5"))
            {
                if (baseEngine.getAlgorithmName().equals("RC5-32"))
                {
                    if (rc5Param.getWordSize() != 32)
                    {
                        throw new InvalidAlgorithmParameterException("RC5 already set up for a word size of 32 not " + rc5Param.getWordSize() + ".");
                    }
                }
                else if (baseEngine.getAlgorithmName().equals("RC5-64"))
                {
                    if (rc5Param.getWordSize() != 64)
                    {
                        throw new InvalidAlgorithmParameterException("RC5 already set up for a word size of 64 not " + rc5Param.getWordSize() + ".");
                    }
                }
            }
            else
            {
                throw new InvalidAlgorithmParameterException("RC5 parameters passed to a cipher that is not RC5.");
            }
            if ((rc5Param.getIV() != null) && (ivLength != 0))
            {
                param = new ParametersWithIV(param, rc5Param.getIV());
                ivParam = (ParametersWithIV)param;
            }
        }
        */
        else
        {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
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
            else if (cipher.getUnderlyingCipher().getAlgorithmName().indexOf("PGPCFB") < 0)
            {
                throw new InvalidAlgorithmParameterException("no IV set when one expected");
            }
        }

        if (random != null && padded)
        {
            param = new ParametersWithRandom(param, random);
        }

        try
        {
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
                throw new InvalidParameterException("unknown opmode " + opmode + " passed");
            }
        }
        catch (Exception e)
        {
            throw new InvalidKeyException(e.getMessage());
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
                    // try again if possible
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
        int     length = cipher.getUpdateOutputSize(inputLen);

        if (length > 0)
        {
                byte[]  out = new byte[length];

                int len = cipher.processBytes(input, inputOffset, inputLen, out, 0);

                if (len == 0)
                {
                    return null;
                }
                else if (len != out.length)
                {
                    byte[]  tmp = new byte[len];

                    System.arraycopy(out, 0, tmp, 0, len);

                    return tmp;
                }

                return out;
        }

        cipher.processBytes(input, inputOffset, inputLen, null, 0);

        return null;
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
            return cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
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
        throws IllegalBlockSizeException, BadPaddingException
    {
        int     len = 0;
        byte[]  tmp = new byte[engineGetOutputSize(inputLen)];

        if (inputLen != 0)
        {
            len = cipher.processBytes(input, inputOffset, inputLen, tmp, 0);
        }

        try
        {
            len += cipher.doFinal(tmp, len);
        }
        catch (DataLengthException e)
        {
            throw new IllegalBlockSizeException(e.getMessage());
        }
        catch (InvalidCipherTextException e)
        {
            throw new BadPaddingException(e.getMessage());
        }

        if (len == tmp.length)
        {
            return tmp;
        }

        byte[]  out = new byte[len];

        System.arraycopy(tmp, 0, out, 0, len);

        return out;
    }

    protected int engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset) 
        throws IllegalBlockSizeException, BadPaddingException
    {
        int     len = 0;

        if (inputLen != 0)
        {
                len = cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        }

        try
        {
            return (len + cipher.doFinal(output, outputOffset + len));
        }
        catch (DataLengthException e)
        {
            throw new IllegalBlockSizeException(e.getMessage());
        }
        catch (InvalidCipherTextException e)
        {
            throw new BadPaddingException(e.getMessage());
        }
    }

    private boolean isAEADModeName(
        String modeName)
    {
        return "CCM".equals(modeName) || "EAX".equals(modeName) || "GCM".equals(modeName);
    }

    /*
     * The ciphers that inherit from us.
     */

    /**
     * DES
     */
    /*
    static public class DES
        extends JCEBlockCipher
    {
        public DES()
        {
            super(new DESEngine());
        }
    }
    */

    /**
     * DESCBC
     */
    /*
    static public class DESCBC
        extends JCEBlockCipher
    {
        public DESCBC()
        {
            super(new CBCBlockCipher(new DESEngine()), 64);
        }
    }
    */

    /**
     * DESede
     */
    /*
    static public class DESede
        extends JCEBlockCipher
    {
        public DESede()
        {
            super(new DESedeEngine());
        }
    }
    */

    /**
     * DESedeCBC
     */
    /*
    static public class DESedeCBC
        extends JCEBlockCipher
    {
        public DESedeCBC()
        {
            super(new CBCBlockCipher(new DESedeEngine()), 64);
        }
    }
    */

    /**
     *  GOST28147
     */
    /*
    static public class GOST28147
        extends JCEBlockCipher
    {
        public GOST28147()
        {
            super(new GOST28147Engine());
        }
    }
    
    static public class GOST28147cbc
        extends JCEBlockCipher
    {
        public GOST28147cbc()
        {
            super(new CBCBlockCipher(new GOST28147Engine()), 64);
        }
    }
    */
    
    /**
     * SKIPJACK
     */
    /*
    static public class Skipjack
        extends JCEBlockCipher
    {
        public Skipjack()
        {
            super(new SkipjackEngine());
        }
    }
    */

    /**
     * Blowfish
     */
    /*
    static public class Blowfish
        extends JCEBlockCipher
    {
        public Blowfish()
        {
            super(new BlowfishEngine());
        }
    }
    */

    /**
     * Blowfish CBC
     */
    /*
    static public class BlowfishCBC
        extends JCEBlockCipher
    {
        public BlowfishCBC()
        {
            super(new CBCBlockCipher(new BlowfishEngine()), 64);
        }
    }
    */

    /**
     * Twofish
     */
    static public class Twofish
        extends JCEBlockCipher
    {
        public Twofish()
        {
            super(new TwofishEngine());
        }
    }

    /**
     * RC2
     */
    /*
    static public class RC2
        extends JCEBlockCipher
    {
        public RC2()
        {
            super(new RC2Engine());
        }
    }
    */

    /**
     * RC2CBC
     */
    /*
    static public class RC2CBC
        extends JCEBlockCipher
    {
        public RC2CBC()
        {
            super(new CBCBlockCipher(new RC2Engine()), 64);
        }
    }
    */

    /**
     * RC5
     */
    /*
    static public class RC5
        extends JCEBlockCipher
    {
        public RC5()
        {
            super(new RC532Engine());
        }
    }
    */

    /**
     * RC564
     */
    /*
    static public class RC564
        extends JCEBlockCipher
    {
        public RC564()
        {
            super(new RC564Engine());
        }
    }
	*/
    /**
     * RC6
     */
    /*
    static public class RC6
        extends JCEBlockCipher
    {
        public RC6()
        {
            super(new RC6Engine());
        }
    }

    /**
     * AES
     */
    /*
    static public class AES
        extends JCEBlockCipher
    {
        public AES()
        {
            super(new AESFastEngine());
        }
    }

    /**
     * AESCBC
     */
    /*
    static public class AESCBC
        extends JCEBlockCipher
    {
        public AESCBC()
        {
            super(new CBCBlockCipher(new AESFastEngine()), 128);
        }
    }

    /**
     * AESCFB
     */
    /*
    static public class AESCFB
        extends JCEBlockCipher
    {
        public AESCFB()
        {
            super(new CFBBlockCipher(new AESFastEngine(), 128), 128);
        }
    }

    /**
     * AESOFB
     */
    /*
    static public class AESOFB
        extends JCEBlockCipher
    {
        public AESOFB()
        {
            super(new OFBBlockCipher(new AESFastEngine(), 128), 128);
        }
    }

    /**
     * Rijndael
     */
    /*
    static public class Rijndael
        extends JCEBlockCipher
    {
        public Rijndael()
        {
            super(new RijndaelEngine());
        }
    }

    /**
     * Serpent
     */
    /*
    static public class Serpent
        extends JCEBlockCipher
    {
        public Serpent()
        {
            super(new SerpentEngine());
        }
    }


    
    /**
     * CAST5
     */
    /*
    static public class CAST5
        extends JCEBlockCipher
    {
        public CAST5()
        {
            super(new CAST5Engine());
        }
    }

    /**
     * CAST5 CBC
     */
    /*
    static public class CAST5CBC
        extends JCEBlockCipher
    {
        public CAST5CBC()
        {
            super(new CBCBlockCipher(new CAST5Engine()), 64);
        }
    }

    /**
     * CAST6
     */
    /*
    static public class CAST6
        extends JCEBlockCipher
    {
        public CAST6()
        {
            super(new CAST6Engine());
        }
    }

    /**
     * TEA
     */
    /*
    static public class TEA
        extends JCEBlockCipher
    {
        public TEA()
        {
            super(new TEAEngine());
        }
    }

    /**
     * XTEA
     */
    /*
    static public class XTEA
        extends JCEBlockCipher
    {
        public XTEA()
        {
            super(new XTEAEngine());
        }
    }

    /**
     * SEED
     */
    /*
    static public class SEED
        extends JCEBlockCipher
    {
        public SEED()
        {
            super(new SEEDEngine());
        }
    }

    /**
     * PBEWithMD5AndDES
     */
    /*
    static public class PBEWithMD5AndDES
        extends JCEBlockCipher
    {
        public PBEWithMD5AndDES()
        {
            super(new CBCBlockCipher(new DESEngine()));
        }
    }

    /**
     * PBEWithMD5AndRC2
     */
    /*
    static public class PBEWithMD5AndRC2
        extends JCEBlockCipher
    {
        public PBEWithMD5AndRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHA1AndDES
     */
    /*
    static public class PBEWithSHA1AndDES
        extends JCEBlockCipher
    {
        public PBEWithSHA1AndDES()
        {
            super(new CBCBlockCipher(new DESEngine()));
        }
    }

    /**
     * PBEWithSHA1AndRC2
     */
    /*
    static public class PBEWithSHA1AndRC2
        extends JCEBlockCipher
    {
        public PBEWithSHA1AndRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHAAnd3-KeyTripleDES-CBC
     */
    /*
    static public class PBEWithSHAAndDES3Key
        extends JCEBlockCipher
    {
        public PBEWithSHAAndDES3Key()
        {
            super(new CBCBlockCipher(new DESedeEngine()));
        }
    }

    /**
     * PBEWithSHAAnd2-KeyTripleDES-CBC
     */
    /*
    static public class PBEWithSHAAndDES2Key
        extends JCEBlockCipher
    {
        public PBEWithSHAAndDES2Key()
        {
            super(new CBCBlockCipher(new DESedeEngine()));
        }
    }

    /**
     * PBEWithSHAAnd128BitRC2-CBC
     */
    /*
    static public class PBEWithSHAAnd128BitRC2
        extends JCEBlockCipher
    {
        public PBEWithSHAAnd128BitRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHAAnd40BitRC2-CBC
     */
    /*
    static public class PBEWithSHAAnd40BitRC2
        extends JCEBlockCipher
    {
        public PBEWithSHAAnd40BitRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHAAndTwofish-CBC
     */
    /*
    static public class PBEWithSHAAndTwofish
        extends JCEBlockCipher
    {
        public PBEWithSHAAndTwofish()
        {
            super(new CBCBlockCipher(new TwofishEngine()));
        }
    }

    /**
     * PBEWithAES-CBC
     */
    /*
    static public class PBEWithAESCBC
        extends JCEBlockCipher
    {
        public PBEWithAESCBC()
        {
            super(new CBCBlockCipher(new AESFastEngine()));
        }
    }
    */

    static private interface GenericBlockCipher
    {
        public void init(boolean forEncryption, CipherParameters params)
            throws IllegalArgumentException;

        public boolean wrapOnNoPadding();

        public String getAlgorithmName();

        public BlockCipher getUnderlyingCipher();

        public int getOutputSize(int len);

        public int getUpdateOutputSize(int len);

        public int processByte(byte in, byte[] out, int outOff)
            throws DataLengthException;

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
            throws DataLengthException;

        public int doFinal(byte[] out, int outOff)
            throws IllegalStateException, InvalidCipherTextException;
    }

    private static class BufferedGenericBlockCipher
        implements GenericBlockCipher
    {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher cipher)
        {
            this.cipher = cipher;
        }

        BufferedGenericBlockCipher(BlockCipher cipher)
        {
            this.cipher = new PaddedBufferedBlockCipher(cipher);
        }

        /*
        BufferedGenericBlockCipher(BlockCipher cipher, BlockCipherPadding padding)
        {
            this.cipher = new PaddedBufferedBlockCipher(cipher, padding);
        }
        */

        public void init(boolean forEncryption, CipherParameters params)
            throws IllegalArgumentException
        {
            cipher.init(forEncryption, params);
        }

        public boolean wrapOnNoPadding()
        {
            return !(cipher instanceof CTSBlockCipher);
        }

        public String getAlgorithmName()
        {
            return cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public BlockCipher getUnderlyingCipher()
        {
            return cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len)
        {
            return cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len)
        {
            return cipher.getUpdateOutputSize(len);
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException
        {
            return cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException
        {
            return cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException
        {
            return cipher.doFinal(out, outOff);
        }
    }

    /*
    private static class AEADGenericBlockCipher
        implements GenericBlockCipher
    {
        private AEADBlockCipher cipher;

        AEADGenericBlockCipher(AEADBlockCipher cipher)
        {
            this.cipher = cipher;
        }

        public void init(boolean forEncryption, CipherParameters params)
            throws IllegalArgumentException
        {
            cipher.init(forEncryption, params);
        }

        public String getAlgorithmName()
        {
            return cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public boolean wrapOnNoPadding()
        {
            return false;
        }

        public BlockCipher getUnderlyingCipher()
        {
            return cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len)
        {
            return cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len)
        {
            return cipher.getUpdateOutputSize(len);
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException
        {
            return cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException
        {
            return cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException
        {
            return cipher.doFinal(out, outOff);
        }
    }
    */
}
