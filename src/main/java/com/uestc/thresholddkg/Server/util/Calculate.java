package com.uestc.thresholddkg.Server.util;

import jdk.nashorn.internal.runtime.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import com.squareup.jnagmp.Gmp;
import java.math.BigInteger;
import java.util.Map;

/**
 * calculate pow(x,m) mod(modus)
 * @author zhangjia
 * @date 2023-01-07 18:26
 */
@Slf4j
public class Calculate {

   /* public static final boolean GmpSuccess;
    static {
        GmpSuccess=LoadGmp();
    }

    private static boolean LoadGmp(){
        try {
            Gmp.checkLoaded();
        }catch (Exception e){log.error("GMP load failed");return false;}
        return true;
    }*/

    public static BigInteger modPow(final BigInteger x,final BigInteger m,final BigInteger modus){
        BigInteger res;
        if(m.signum()<0){
            res=(x.modPow(m.negate(),modus)).modInverse(modus);//modInverse(Gmp.modPowInsecure(x, m.negate(), modus),modus);
        }else{
            res=x.modPow(m,modus);//Gmp.modPowInsecure(x, m, modus);
        }
        return res;
    }
    public static BigInteger modInverse(final BigInteger p,final BigInteger n){
        return p.modInverse(n);//Gmp.modInverse(p,n);
    }

    /**
     *
     * @param bigIntegers []
     * @param mod q
     * @return bigInt adds
     */
    public static BigInteger addsPow(final Map<String,BigInteger> map, BigInteger mod){
        final BigInteger[] res = {BigInteger.ZERO};
        map.forEach((k,v)-> res[0] = res[0].add(v).mod(mod));
        return res[0];
    }
}
