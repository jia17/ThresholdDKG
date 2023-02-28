package com.uestc.thresholddkg.Server.util;

import com.sun.net.httpserver.HttpServer;
import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.handle.TokenHandle.LoginOut;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.user.StartPRF;
import com.uestc.thresholddkg.Server.user.logOutU;
import com.uestc.thresholddkg.Server.user.startToken;
import lombok.var;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.imageio.IIOParam;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.uestc.thresholddkg.Server.util.TestDKG.getPedersen;
import static java.lang.Thread.interrupted;

/**
 * @author zhangjia
 * @date 2023-01-09 20:24
 * DKG utils
 */
@Component
public class DKG {

    public static String[] ipAndPort;
    public static Integer threshold;
    public static Integer KeyLen=512;
    @PostConstruct
    public void getCfg(){
        ipAndPort= IdpServer.addrS;threshold=IdpServer.threshold;
    }
    /**
     *
     * @return DKG system param p,q,g,h
     */
    public static DKG_System init(){
        BigInteger q=Prime.generateSophiePrime(KeyLen);//1024//need long time
        BigInteger p=Prime.getSafePrime(q);
        //BigInteger p=Prime.generateSafePrime(KeyLen);
       //BigInteger q=Prime.getSophiePrime(p);
        BigInteger g=getPedersen(p,q);
        BigInteger h=null;
        do{
            h=getPedersen(p,q);
        }while (h.equals(g));
        DKG_System dKG_System = new DKG_System();
        dKG_System.setP(p);
        dKG_System.setQ(q);
        dKG_System.setG(g);
        dKG_System.setH(h);
        return dKG_System;
    }
    public static DKG_System initDLog(){
        int len=512;//256;//         //cautious
        BigInteger p=null,q=null;
//        do{
//            q=Prime.generatePrime(len);
//            p=q.multiply(BigInteger.valueOf(8)).add(BigInteger.ONE);
//        }while (!Prime.isPrime(p));
        try {
            q=generatePrimeParallel(len);
        }catch (InterruptedException e){e.printStackTrace();}
        p=q.multiply(BigInteger.valueOf(8)).add(BigInteger.ONE);
        BigInteger rnd=RandomGenerator.genaratePositiveRandom(p);
        BigInteger g=rnd.modPow(BigInteger.valueOf(8),p);
        BigInteger h=g.modPow(BigInteger.TEN,p);
        DKG_System dKG_System = new DKG_System();
        dKG_System.setP(p);
        dKG_System.setQ(q);
        dKG_System.setG(g);
        dKG_System.setH(h);
        return dKG_System;
    }
    public static DKG_System initRSA(){
        int len=256;
        BigInteger q=Prime.generateSafePrime(len);//1024//need long time
        BigInteger p=Prime.generateSafePrime(len);
        BigInteger p1=Prime.getSophiePrime(p);
        BigInteger q1=Prime.getSophiePrime(q);
        BigInteger n=p.multiply(q);BigInteger m=p1.multiply(q1);
        BigInteger g=getSquare(n);
        BigInteger h=null;
        do{
            h=getSquare(n);
        }while (h.equals(g));
        DKG_System dKG_System = new DKG_System();
        dKG_System.setP(n);
        dKG_System.setQ(m);
        dKG_System.setG(g);
        dKG_System.setH(h);
        return dKG_System;
    }
    public static HttpServer getUserServ(){
        HttpServer httpServer=null;
        var service= new ThreadPoolExecutor(
                14, 20, 5,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        try {
            httpServer=HttpServer.create(new InetSocketAddress("127.0.0.1",8095),0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        httpServer.createContext("/startPrfs",new StartPRF(httpServer,service));
        httpServer.createContext("/startTokens",new startToken(service));
        httpServer.createContext("/logoutU",new logOutU(service));
        httpServer.setExecutor(null);
        httpServer.start();
        return httpServer;
    }

    public static final BigInteger generatePrimeParallel(int len) throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(1);
        ConcurrentSkipListSet<BigInteger> skipListSet=new ConcurrentSkipListSet<>();
        int num=10;//cautious too much threads
        var service= Executors.newCachedThreadPool();//fix ?
        for(int i=0;i<num;i++){
            service.submit(new Runnable() {
                @Override
                public void run() {
                    BigInteger p=null,q=null;
                    do{
                        q=Prime.generatePrime(len);
                        p=q.multiply(BigInteger.valueOf(8)).add(BigInteger.ONE);
                    }while (!Prime.isPrime(p)&&!interrupted());
                    skipListSet.add(q);latch.countDown();
                }
            });
        }
        latch.await();
        service.shutdownNow();
        for (var val:skipListSet) {
            return val;
        }
        return null;
    }
    public static BigInteger getSquare(BigInteger p){
        BigInteger res=null;
        do{
        BigInteger m=RandomGenerator.genarateRandom(p);
        res=m.multiply(m).mod(p);}
        while (res.equals(BigInteger.ONE)||!p.gcd(res).equals(BigInteger.ONE));
        return res;
    }
    public static final String AESencrypt(String plainText,String secret) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator= KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(secret.getBytes());
        keyGenerator.init(secureRandom);
        Key secretKey=keyGenerator.generateKey();
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] p = plainText.getBytes("UTF-8");
            byte[] result = cipher.doFinal(p);
            BASE64Encoder encoder = new BASE64Encoder();
            String encoded = encoder.encode(result);
            return encoded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static final String AESdecrypt(String cipherText,String secret) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator= KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(secret.getBytes());
        keyGenerator.init(secureRandom);
        Key secretKey=keyGenerator.generateKey();
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] c = decoder.decodeBuffer(cipherText);
            byte[] result = cipher.doFinal(c);
            String plainText = new String(result, "UTF-8");
            return plainText;
        } catch (Exception e) {
            System.out.println("error dec");
            throw new RuntimeException(e);
        }
    }
    public static byte[] HashSha3(String passwd){
        byte[] bytes = passwd.getBytes();
        Digest digest = new SHA3Digest(512);
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return rsData;
    }
    public static String HashRipe160(String str){
        byte[] Bytes= str.getBytes();
        Digest digest=new RIPEMD160Digest();
        digest.update(Bytes,0,Bytes.length);
        byte[] rsData=new byte[digest.getDigestSize()];
        digest.doFinal(rsData,0);
        return Hex.toHexString(rsData);
    }

    public static String HashBlake2bSalt(byte[] bytes,byte[] salt){
        Digest digest=new Blake2bDigest(bytes,64,salt,null);
        byte[] rsData=new byte[digest.getDigestSize()];
        digest.doFinal(rsData,0);
        return Hex.toHexString(rsData);
    }
    /**
     *
     * @param secret s
     * @param mod p
     * @return F a0-at
     */
    public static BigInteger[] generateFuncParam(BigInteger secret,BigInteger mod){
        BigInteger[] F=RandomGenerator.genaratePrimes(threshold,mod);
        F[0]=secret;
        return F;
    }

    /**
     * @param Fparam F[1]-F[n]
     * @param mod q
     * @return shares of F,F';(F[1]-F[n])
     */
    public static BigInteger[] FunctionValue(BigInteger[] Fparam,BigInteger mod){
        int serversNum= ipAndPort.length;
        BigInteger[] Fvalue=new BigInteger[serversNum];
        for (int i = 0; i < serversNum; i++) {
            Fvalue[i]=calculateCoeffi(Fparam,BigInteger.valueOf(i+1),mod);
        }
        return Fvalue;
    }

    /**
     *
     * @param g g
     * @param h h
     * @param Fparam f=a0+a1X+a2X^2+..+atX^t
     * @param Gparam g=
     * @param mod p
     * @return  g^ai*h^bi
     */
    public static BigInteger[] FunctionGH(BigInteger g,BigInteger h,BigInteger[] Fparam,BigInteger[] Gparam,BigInteger mod){
        BigInteger[] Ci=new BigInteger[threshold];
        for (int i = 0; i <threshold ; i++) {
            Ci[i]=((Calculate.modPow(g,Fparam[i],mod)).multiply(Calculate.modPow(h,Gparam[i],mod))).mod(mod);
        }
        return Ci;
    }
    /**
     * verify1=((Calculate.modPow(g,Si[i],p)).multiply(Calculate.modPow(h,Si2[i],p))).mod(p);
     * @param verify1 g^si*h^s'i
     * @param Ci    Ci (0,t-1)
     * @param serverId  servers id from (1-n)
     * @param mod  p
     * @return shares is valid
     */
    public static boolean VerifyGH(BigInteger verify1,BigInteger[] Ci,int serverId,BigInteger mod){
        BigInteger cik=calculateMuls(Ci,BigInteger.valueOf(serverId),mod);
        return verify1.equals(cik);
    }

    /**
     *
     * @param servers combine shares servers,eg[1,2,3,4,5]
     * @param mapIdVal   <id,F[id-1]>
     * @param mod q
     * @return secret s mod q
     */
    public static BigInteger Combine(Integer[] servers, Map<Integer,BigInteger> mapIdVal, BigInteger mod){
        Integer ListMuls=1;
        for (Integer val :servers) {
            ListMuls*=val;
        }
        BigInteger secret2=BigInteger.ZERO;
        BigInteger tempBigMul;
        Integer tempMul=0;
        for(int i=0;i<threshold;i++){
            tempMul=servers[i];
            if((threshold&1)==0)tempMul*=-1;
            for(int j=0;j<threshold;j++){
                if(i!=j){tempMul*=(servers[i]-servers[j]); }
            }
            tempBigMul=mapIdVal.get(servers[i]).multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),mod)).mod(mod);
            if(tempMul<0)secret2=(secret2.add(mod.add(tempBigMul.negate()))).mod(mod);
            else secret2=secret2.add(tempBigMul).mod(mod);
        }
        return (secret2.multiply(BigInteger.valueOf(ListMuls))).mod(mod);
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
            res=coeffi[i].add(res.multiply(x));
        }
        return res.mod(modus);
    }

    public static String[] bigInt2Str(BigInteger[] integers){
        String[] str=new String[integers.length];
        for(int i=0;i<integers.length;i++){
            str[i]=integers[i].toString();
        }
        return str;
    }
    public static BigInteger[] str2BigInt(String[] str){
        BigInteger[] integers=new BigInteger[str.length];
        for(int i=0;i<str.length;i++){
            integers[i]=new BigInteger(str[i]);
        }
        return integers;
    }
    /**
     * init map<addr,times>
     */
    public static void initMapTimes(Map<String,Integer> map){
        for (String s:ipAndPort
             ) {
            map.put("/"+s,threshold);
        }
    }
}
