package org.bouncycastle.asn1.cryptopro;

import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;

import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * table of the available named parameters for GOST 3410-94.
 */
@SuppressWarnings("unchecked")
public class GOST3410NamedParameters
{
    static final Hashtable objIds = new Hashtable();
    static final Hashtable params = new Hashtable();
    static final Hashtable names = new Hashtable();

    static private GOST3410ParamSetParameters cryptoProA = new GOST3410ParamSetParameters(
            1024,
            new BigInteger("127021248288932417465907042777176443525787653508916535812817507265705031260985098497423188333483401180925999995120988934130659205614996724254121049274349357074920312769561451689224110579311248812610229678534638401693520013288995000362260684222750813532307004517341633685004541062586971416883686778842537820383"),
            new BigInteger("68363196144955700784444165611827252895102170888761442055095051287550314083023"),
            new BigInteger("100997906755055304772081815535925224869841082572053457874823515875577147990529272777244152852699298796483356699682842027972896052747173175480590485607134746852141928680912561502802222185647539190902656116367847270145019066794290930185446216399730872221732889830323194097355403213400972588322876850946740663962")
//            validationAlgorithm {
//                    algorithm
//                        id-GostR3410-94-bBis,
//                    parameters
//                        GostR3410-94-ValidationBisParameters: {
//                            x0      1376285941,
//                            c       3996757427
//                        }
//                }

           );
    
    static private GOST3410ParamSetParameters cryptoProB = new GOST3410ParamSetParameters(
            1024,
            new BigInteger("139454871199115825601409655107690713107041707059928031797758001454375765357722984094124368522288239833039114681648076688236921220737322672160740747771700911134550432053804647694904686120113087816240740184800477047157336662926249423571248823968542221753660143391485680840520336859458494803187341288580489525163"),
            new BigInteger("79885141663410976897627118935756323747307951916507639758300472692338873533959"),
            new BigInteger("42941826148615804143873447737955502392672345968607143066798112994089471231420027060385216699563848719957657284814898909770759462613437669456364882730370838934791080835932647976778601915343474400961034231316672578686920482194932878633360203384797092684342247621055760235016132614780652761028509445403338652341")
//    validationAlgorithm {
//            algorithm
//                id-GostR3410-94-bBis,
//            parameters
//                GostR3410-94-ValidationBisParameters: {
//                    x0      1536654555,
//                    c       1855361757,
//                    d       14408629386140014567655
//4902939282056547857802241461782996702017713059974755104394739915140
//6115284791024439062735788342744854120601660303926203867703556828005
//8957203818114895398976594425537561271800850306
//                }
//        }
//}
         );

    static private GOST3410ParamSetParameters cryptoProXchA = new GOST3410ParamSetParameters(
    1024,
    new BigInteger("142011741597563481196368286022318089743276138395243738762872573441927459393512718973631166078467600360848946623567625795282774719212241929071046134208380636394084512691828894000571524625445295769349356752728956831541775441763139384457191755096847107846595662547942312293338483924514339614727760681880609734239"),
    new BigInteger("91771529896554605945588149018382750217296858393520724172743325725474374979801"),
    new BigInteger("133531813272720673433859519948319001217942375967847486899482359599369642528734712461590403327731821410328012529253871914788598993103310567744136196364803064721377826656898686468463277710150809401182608770201615324990468332931294920912776241137878030224355746606283971659376426832674269780880061631528163475887")
   );
    
    static
    {      
        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_A, cryptoProA);       
        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_B, cryptoProB);       
//        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_C, cryptoProC);       
//        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_D, cryptoProD);       
        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_XchA, cryptoProXchA);       
//        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_XchB, cryptoProXchA);   
//        params.put(CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_XchC, cryptoProXchA);
        
        objIds.put("GostR3410-94-CryptoPro-A", CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_A);
        objIds.put("GostR3410-94-CryptoPro-B", CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_B);
        objIds.put("GostR3410-94-CryptoPro-XchA", CryptoProObjectIdentifiers.gostR3410_94_CryptoPro_XchA);
    }

    /**
     * return the GOST3410ParamSetParameters object for the given OID, null if it 
     * isn't present.
     *
     * @param oid an object identifier representing a named parameters, if present.
     */
    public static GOST3410ParamSetParameters getByOID(
        DERObjectIdentifier  oid)
    {
        return (GOST3410ParamSetParameters)params.get(oid);
    }

    /**
     * returns an enumeration containing the name strings for parameters
     * contained in this structure.
     */
    public static Enumeration getNames()
    {
        return objIds.keys();
    }

    public static GOST3410ParamSetParameters getByName(
        String  name)
    {
        DERObjectIdentifier oid = (DERObjectIdentifier)objIds.get(name);

        if (oid != null)
        {
            return (GOST3410ParamSetParameters)params.get(oid);
        }

        return null;
    }

    public static DERObjectIdentifier getOID(String name)
    {
        return (DERObjectIdentifier)objIds.get(name);
    }
}
