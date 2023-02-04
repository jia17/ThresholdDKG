package com.uestc.thresholddkg.Server.util;

import com.sun.net.httpserver.HttpServer;
import com.uestc.thresholddkg.Server.user.StartPRF;
import com.uestc.thresholddkg.Server.user.startToken;
import lombok.var;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author zhangjia
 * @date 2023-01-07 22:19
 */
public class TestDKG {

    public static void main(String[] args){
        BigInteger q=Prime.generateSophiePrime(512);
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
        byte[] bytes=testSha3("1202hd1nx10w");
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0,32));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,32,64));
        var testHash=(g.modPow(b1,p).multiply(h.modPow(b2,p))).mod(p);
        String temps=null;
        if(testHash.toString().length()<154){//154=512*0.302=*ln(2)/ln(10)
            StringBuilder blank= new StringBuilder();
            for(int i=154-testHash.toString().length();i>0;i--){
                blank.append("0");
            }
            temps=blank.toString()+testHash.toString();
            testHash=new BigInteger(temps);
            System.out.println(temps+" len"+temps.length());
        }
        BigInteger randR=RandomGenerator.genaratePositiveRandom(q);
        //BigInteger testHash=g.modPow(BigInteger.valueOf(123),p).multiply(h).mod(p);
        BigInteger[] hashTemp=new BigInteger[threshold];
        BigInteger HashPowS2=BigInteger.ONE;
        //restore
        Integer[] listRe=new Integer[]{1,2,3,4,5};
        Integer listMuls=1;
        for (Integer val :
                listRe) {
            listMuls*=val;
        }
        BigInteger HashPowS1=testHash.modPow(secret,p);
        testHash=testHash.modPow(randR,p);
        for(int i=0;i<threshold;i++){
            hashTemp[i]=testHash.modPow(Si[listRe[i]-1].mod(q),p);
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
//            BigInteger c=Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),p);
//            BigInteger t=Calculate.modInverse(c,p);
//            BigInteger res=c.multiply(t).mod(p);
//            System.out.println("s  "+tempMul+"  inver"+c+"  self"+t+" res="+res );
           // var temp=Calculate.modPow(testHash,)
            HashPowS2=HashPowS2.multiply(Calculate.modPow(hashTemp[i],
                    Calculate.modInverse(BigInteger.valueOf(tempMul),q).multiply(BigInteger.valueOf(listMuls)).mod(q)
                    ,p)).mod(p);
            tempBigMul=Si[listRe[i]-1].multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
            if(tempMul<0)secret2=(secret2.add(q.add(tempBigMul.negate()))).mod(q);//secret2.add(tempBigMul.negate()).mod(p);//secret2.subtract(tempBigMul);//
            else secret2=secret2.add(tempBigMul).mod(q);
        }
        //secret2=secret2.mod(p);
        secret2=(secret2.multiply(BigInteger.valueOf(listMuls))).mod(q);
        secret=secret.mod(q);
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

        HashPowS2=HashPowS2.modPow(Calculate.modInverse(randR,q),p);
        System.out.println(testHash);
        System.out.println(HashPowS1);
        System.out.println(HashPowS2);
        if(HashPowS1.equals(HashPowS2)) System.out.println("hash sucess");
    }

    public static byte[] testSha3(String passwd){
        byte[] bytes = passwd.getBytes();
        Digest digest = new SHA3Digest(512);
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;//Hex.toHexString(rsData);
    }

    public static HttpServer getUserServ(){
        HttpServer httpServer=null;
        try {
            httpServer=HttpServer.create(new InetSocketAddress("127.0.0.1",8093),0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        httpServer.createContext("/startPrfs",new StartPRF(httpServer));
        httpServer.createContext("/startTokens",new startToken());
        httpServer.setExecutor(null);
        httpServer.start();
        return httpServer;
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
