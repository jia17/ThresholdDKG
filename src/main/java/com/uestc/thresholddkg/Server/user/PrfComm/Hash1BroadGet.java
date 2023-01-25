package com.uestc.thresholddkg.Server.user.PrfComm;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.communicate.UserBroadHash1;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.IdHash1;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-01-24 11:50
 */
@Slf4j
public class Hash1BroadGet implements Callable<Boolean>{

    private String[] ipPorts;
    private String Id;
    private String PwdHash1;
    private DKG_System dkg_system;
    private BigInteger randRInv;
    private String pwd;
    public Hash1BroadGet(String id, String pwdHash1, DKG_System dkg_system,BigInteger _randRInv,String pwd){
        Id=id;this.PwdHash1=pwdHash1;ipPorts= IdpServer.addrS;this.dkg_system=dkg_system;randRInv=_randRInv;this.pwd=pwd;
    }
    @Override
    public Boolean call() {
        int serversNum = ipPorts.length;
        int threshold = (serversNum>>1)+1;
        ExecutorService executor = Executors.newFixedThreadPool(serversNum);
        CountDownLatch latch=new CountDownLatch(threshold);
        ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
        final AtomicInteger failureCounter = new AtomicInteger(0);
        final int maximumFailures = serversNum-threshold;
        IdHash1 idHash1=new IdHash1(Id,PwdHash1);
        for (String s : ipPorts) {
            executor.submit(
                    UserBroadHash1.builder().latch(latch).message(Convert2Str.Obj2json(idHash1)).mapper("PrfgetHash1").IpAndPort(s)
                            .resmap(resMap).maxFail(maximumFailures).failCount(failureCounter).build()
            );
        }
        try {
            latch.await();
            if(failureCounter.get()<=maximumFailures){
                Map<String, BigInteger> map=new HashMap<>();
                resMap.forEach((key, value) -> {if(map.size()<(threshold))map.put(key,new BigInteger(value));});
                executor.shutdown();
                BigInteger q=dkg_system.getQ();
                BigInteger p=dkg_system.getP();
                Integer[] PwdIndex=new Integer[threshold];
                BigInteger[] PwdEnc=new BigInteger[threshold];
                Integer[] is={0};
                map.forEach((k,v)->{;PwdIndex[is[0]]=Integer.parseInt(k);PwdEnc[is[0]]=v;is[0]=is[0]+1;});
                Integer listMuls=1;
                for (var val:PwdIndex) { listMuls*=val;}
                BigInteger PwdHashEncS=BigInteger.ONE;
                Integer tempMul=0;
                for(int i=0;i<threshold;i++){
                    tempMul=PwdIndex[i];
                    if((threshold&1)==0)tempMul*=-1;
                    for(int j=0;j<threshold;j++){
                        if(i!=j){tempMul*=(PwdIndex[i]-PwdIndex[j]);}
                    }
                    PwdHashEncS=PwdHashEncS.multiply(Calculate.modPow(PwdEnc[i],
                            Calculate.modInverse(BigInteger.valueOf(tempMul),q).multiply(BigInteger.valueOf(listMuls)).mod(q)
                            ,p)).mod(p);
                }
                PwdHashEncS=PwdHashEncS.modPow(randRInv,p);
                Thread SendPrf=new Thread(new PrfBroad(Id,pwd,PwdHashEncS.toString()));
                SendPrf.start();
                log.error("PwdHash1ENC---"+PwdHashEncS);
                return true;
            }else{
                executor.shutdown();
                log.warn("PRF_HASH1_FAILED_TOO_MUCH");
                return false;
            }
        }catch (InterruptedException e){
            executor.shutdown();
            throw new RuntimeException();
        }
        //executor.shutdown();
    }
}
