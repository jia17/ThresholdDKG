package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.imageio.IIOParam;
import java.math.BigInteger;
import java.util.Map;

import static com.uestc.thresholddkg.Server.util.TestDKG.getPedersen;

/**
 * @author zhangjia
 * @date 2023-01-09 20:24
 * DKG utils
 */
@Component
public class DKG {

    public static String[] ipAndPort;
    public static Integer threshold;
    @PostConstruct
    public void getCfg(){
        ipAndPort= IdpServer.addrS;threshold=IdpServer.threshold;
    }
    /**
     *
     * @return DKG system param p,q,g,h
     */
    public static DKG_System init(){
        BigInteger q=Prime.generateSophiePrime(512);//1024
        BigInteger p=Prime.getSafePrime(q);
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

}
