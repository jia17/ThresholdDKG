package com.uestc.thresholddkg.Server.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author zhangjia
 * @date 2023-01-07 16:43
 * Generate prime
 */
public class Prime {
    static final int CERTAINNTY=100;
    static final BigInteger two=BigInteger.valueOf(2);

    public static boolean isPrime(final BigInteger p){
        return p.isProbablePrime(CERTAINNTY);
    }

    public static BigInteger generatePrime(final int length){
        return BigInteger.probablePrime(length,new SecureRandom());
    }

    /**
     *
     * @param length
     * @return a prime p that 2*p+1 is also a prime
     */
    public static BigInteger generateSophiePrime(final int length){
        while(true){
            BigInteger p=generatePrime(length);
            BigInteger q=getSafePrime(p);
            if(q!=null)return p;
        }
    }

    /**
     *
     * @param length
     * @return a prime p that (p-1)/2 is also a prime
     */
    public static BigInteger generateSafePrime(final int length){
        while(true){
            BigInteger p=generatePrime(length);
            BigInteger q=getSophiePrime(p);
            if(q!=null)return p;
        }
    }

    public static BigInteger getSafePrime(final BigInteger p){
        BigInteger q=(p.multiply(two)).add(BigInteger.ONE);
        if(isPrime(q))return q;
        return null;
    }

    public static BigInteger getSophiePrime(final BigInteger p){
        BigInteger q=(p.subtract(BigInteger.ONE)).divide(two);
        if(isPrime(q))return q;
        return null;
    }
}
