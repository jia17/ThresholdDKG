package com.uestc.thresholddkg.Server.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * if n is a prime;Zn*=(1,n-1)
 * @author zhangjia
 * @date 2023-01-07 17:00
 */
public class RandomGenerator {

    public static BigInteger genarateRandom(final int length){
        return new BigInteger(length,new SecureRandom());
    }

    /**
     * generate random a from (0,max-1)
     * @param max
     * @return random from (0,max-1)
     */
    public static BigInteger genarateRandom(final BigInteger max){
        BigInteger temp;
        do{
            temp=genarateRandom(max.bitLength());
        }while(temp.compareTo(max)>=0);
        return temp;
    }

    /**
     * Random val from (1,max-1)
     * @param max
     * @return max>val>1
     */
    public static BigInteger genaratePositiveRandom(final BigInteger max){
        BigInteger temp;
        do{
            temp=genarateRandom(max);
        }while(temp.compareTo(BigInteger.ONE)<0);
        return temp;
    }

    /**
     * Random
     * @param m
     * @return  p from (1,m-1) while gcd(p,m)=1
     */
    public static BigInteger genaratePrimeRandom(final BigInteger m){
        while (true){
            BigInteger p=genaratePositiveRandom(m);
            if(p.gcd(m).equals(BigInteger.ONE))return p;
        }
    }

    /**
     * generate list[] from(0,max-1);sometimes for F,G coefficients in DKG
     * @param size
     * @param max
     * @return list[]
     */
    public static BigInteger[] genaratePrimes(final int size,final BigInteger max){
        BigInteger[] nums=new BigInteger[size];
        for (int i = 0; i <size; i++) {
            nums[i]=genarateRandom(max);
        }
        return nums;
    }
}
