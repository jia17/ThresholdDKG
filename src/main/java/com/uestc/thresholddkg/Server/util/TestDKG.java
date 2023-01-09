package com.uestc.thresholddkg.Server.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;

/**
 * @author zhangjia
 * @date 2023-01-07 22:19
 */
public class TestDKG {

    public static void main(String[] args){
        BigInteger q=Prime.generateSophiePrime(1024);
        BigInteger p=Prime.getSafePrime(q);
        BigInteger g=getPedersen(p,q);
        BigInteger h=null;
        do{
            h=getPedersen(p,q);
           }while (h.equals(g));
        int serversNum=9,threshold=5;
        BigInteger[] F=RandomGenerator.genaratePrimes(threshold,p);
        BigInteger[] G=RandomGenerator.genaratePrimes(threshold,p);
        BigInteger secret=RandomGenerator.genaratePositiveRandom(p);
        System.out.println(secret);
        BigInteger tval=RandomGenerator.genaratePositiveRandom(p);
        F[0]=secret;G[0]=tval;
        BigInteger[] Ci=new BigInteger[threshold];
       // Ci[0]=((Calculate.modPow(g,secret,p)).multiply(Calculate.modPow(h,tval,p))).mod(p);
        for (int i = 0; i <threshold ; i++) {
            Ci[i]=((Calculate.modPow(g,F[i],p)).multiply(Calculate.modPow(h,G[i],p))).mod(p);
        }
        BigInteger[] Si=new BigInteger[serversNum],Si2=new BigInteger[serversNum];
        for (int i = 0; i < serversNum; i++) {
            Si[i]=calculateCoeffi(F,BigInteger.valueOf(i+1),q);//cautious
            Si2[i]=calculateCoeffi(G,BigInteger.valueOf(i+1),q);
        }
        for(int i=0;i<serversNum;i++){
            BigInteger gh=((Calculate.modPow(g,Si[i],p)).multiply(Calculate.modPow(h,Si2[i],p))).mod(p);
            BigInteger cik=calculateMuls(Ci,BigInteger.valueOf(i+1),p);
           // System.out.println("verify "+Integer.toString(i));
            if(gh.equals(cik)) System.out.println("true");
            //System.out.println(gh);
            //System.out.println(cik);
        }
        //restore
        Integer[] listRe=new Integer[]{7,2,1,3,9};
        Integer listMuls=1;
        for (Integer val :
                listRe) {
            listMuls*=val;
        }
        BigInteger secret2=BigInteger.ZERO;
        BigInteger tempBigMul;
        Integer tempMul=0;
        //p=q;secret=secret.mod(p); //*real work don't change p as q;be cautious at mod(p) or mod(q)
        for(int i=0;i<threshold;i++){
            tempMul=listRe[i];
            if((threshold&1)==0)tempMul*=-1;
            //tempBigMul=Si[listRe[i]-1].multiply(Calculate.modInverse(BigInteger.valueOf(tempMul),p));
            for(int j=0;j<threshold;j++){
                if(i!=j){tempMul*=(listRe[i]-listRe[j]);
                   // tempBigMul=tempBigMul.multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs((listRe[i]-listRe[j]))),p));
                }
            }
            BigInteger c=Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),p);
            BigInteger t=Calculate.modInverse(c,p);
            BigInteger res=c.multiply(t).mod(p);
            System.out.println("s  "+tempMul+"  inver"+c+"  self"+t+" res="+res );
            tempBigMul=Si[listRe[i]-1].multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),p)).mod(p);
            if(tempMul<0)secret2=(secret2.add(p.add(tempBigMul.negate()))).mod(p);//secret2.add(tempBigMul.negate()).mod(p);//secret2.subtract(tempBigMul);//
            else secret2=secret2.add(tempBigMul).mod(p);
        }
        //secret2=secret2.mod(p);
        secret2=(secret2.multiply(BigInteger.valueOf(listMuls))).mod(p);
        if(secret2.equals(secret)) System.out.println("restore success");
        System.out.println(secret2);
        System.out.println(secret);

        //two phase
        BigInteger[] Ai=new BigInteger[threshold];
        for(int i=0;i<threshold;i++){
            Ai[i]=Calculate.modPow(g,F[i],p);
        }
        //verify g^si=-|Ak
        for(int i=0;i<serversNum;i++){
        BigInteger gs=Calculate.modPow(g,Si[i],p);
        BigInteger Ail=BigInteger.ONE;
        for (int j=0;j<threshold;j++) {
            Ail=Ail.multiply(Calculate.modPow(Ai[j],BigInteger.valueOf(i+1).pow(j),p)).mod(p);
         }
        if(gs.equals(Ail)) System.out.println("verify2 "+Integer.toString(i));
        }
    }

    public static BigInteger getPedersen(BigInteger p,BigInteger q){
        BigInteger res;
        do{
            res= RandomGenerator.genaratePositiveRandom(p);
        }while (res.equals(BigInteger.ONE)||!Calculate.modPow(res,q,p).equals(BigInteger.ONE));
        return res;
    }

    public static BigInteger calculateMuls(BigInteger[] coeffi,BigInteger x,BigInteger modus){
        BigInteger res=BigInteger.ONE;
        for(int i=coeffi.length-1;i>=0;i--){
            res=res.multiply(coeffi[i].modPow((x.pow(i)),modus));
        }
        return res.mod(modus);
    }
    public static BigInteger calculateCoeffi(BigInteger[] coeffi,BigInteger x,BigInteger modus){
        if(coeffi.length<2){
            return null;
        }
        BigInteger res=coeffi[coeffi.length-1];
        for(int i=coeffi.length-2;i>=0;i--){
            res=coeffi[i].add(res.multiply(x));//.mod(modus);
        }
        //res=res.add(coeffi[0]);
        return res.mod(modus);
    }
   /* for(int i=coeffi.length-1;i>=0;i--){
        res=coeffi[i].add(res.multiply(x));
    }*/
}
