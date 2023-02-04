package com.uestc.thresholddkg.Server.user.TokenComm;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.communicate.UserBroadGetTokeni;
import com.uestc.thresholddkg.Server.communicate.UserBroadHash1;
import com.uestc.thresholddkg.Server.pojo.IdHash1;
import com.uestc.thresholddkg.Server.pojo.PubParamToken;
import com.uestc.thresholddkg.Server.pojo.TokenSi;
import com.uestc.thresholddkg.Server.pojo.UserMsg2Serv;
import com.uestc.thresholddkg.Server.user.PrfComm.GetVeriPrf;
import com.uestc.thresholddkg.Server.user.PrfComm.PrfBroad;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import javafx.beans.binding.BooleanBinding;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-02-04 15:57
 */
@AllArgsConstructor
@Slf4j
public class GetTokenSi implements Callable<Boolean> {
    private String[] sendAddrs;
    private UserMsg2Serv userMsg2Serv;
    @Override
    public Boolean call() throws Exception {
        String[] ipPorts= IdpServer.addrS;
        Integer[] AddrIndex=new Integer[sendAddrs.length];
        Set<String> addrSet = new HashSet<>(Arrays.asList(sendAddrs));//遍历乱序,小心
        Map<String,Integer> addrMap=new HashMap<>();
        for(int i=0;i<sendAddrs.length;i++){addrMap.put(sendAddrs[i],i);}
        for(int i=0,t=0;i<ipPorts.length;i++){
           // if(addrMap.contains(ipPorts[i])){AddrIndex[t]=i+1;t++;}
            if(addrMap.containsKey(ipPorts[i])){AddrIndex[addrMap.get(ipPorts[i])]=i+1;}
        }
        userMsg2Serv.setAddrIndex(AddrIndex);
        ExecutorService executor = Executors.newFixedThreadPool(sendAddrs.length);
        CountDownLatch latch=new CountDownLatch(sendAddrs.length);
        ConcurrentHashMap<Integer,String> resMap=new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> lambdaMap=new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> verySet=new ConcurrentSkipListSet<>();
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(ipPorts.length);
        SendUri send = SendUri.builder().message(userMsg2Serv.getUserId()).mapper("getPubParam")
                .IpAndPort(ipPorts[servI])
                .build();
        String PubPara=send.SendMsg();
        if(PubPara.equals("")){log.error("DKG token false");return false;}
        PubParamToken pubParamToken=(PubParamToken) Convert2StrToken.Json2obj(PubPara, PubParamToken.class);
        for (int i=0;i<sendAddrs.length;i++) {
            executor.submit(
                    UserBroadGetTokeni.builder().latch(latch).message(Convert2StrToken.Obj2json(userMsg2Serv)).mapper("sendTokenI").IpAndPort(sendAddrs[i])
                            .resMap(resMap).index(AddrIndex[i]).lambdaMap(lambdaMap).build()
            );
        }
        try {
            latch.await();
            executor.shutdown();
            Integer threshold=IdpServer.threshold;
            if(resMap.size()<threshold){log.error("ToKen size too less");return false;}
            BigInteger p=new BigInteger(pubParamToken.getDkg_sysStr().getP());
            BigInteger q=new BigInteger(pubParamToken.getDkg_sysStr().getQ());
            BigInteger g=new BigInteger(pubParamToken.getDkg_sysStr().getG());
            BigInteger y=new BigInteger(pubParamToken.getY());
            BigInteger gm=g.modPow(new BigInteger(userMsg2Serv.getMsg()),p);
            BigInteger W=BigInteger.ONE;
            Integer listMuls=1;
            for (Integer val : AddrIndex) { listMuls*=val;  }
            for (var val:resMap.entrySet()) {
                TokenSi tokenSi=(TokenSi) Convert2StrToken.Json2obj(val.getValue(), TokenSi.class);
                int tempMul=0;int index=val.getKey();
                tempMul=val.getKey();
                if(((threshold)&1)==0)tempMul*=-1;
                for(int j=0;j<AddrIndex.length;j++){
                    if(index!=AddrIndex[j]){tempMul*=(index-AddrIndex[j]);}
                }
                BigInteger Hi=new BigInteger(userMsg2Serv.getMsgHash()).multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
                if(tempMul<0)Hi=q.add(Hi.negate()).mod(q);
                Hi=Hi.multiply(BigInteger.valueOf(listMuls)).mod(q);
                BigInteger Wi=new BigInteger(tokenSi.getWi());
                BigInteger uW=new BigInteger(tokenSi.getUi()).modPow(Hi,p)
                        .multiply(Wi.modPow(BigInteger.valueOf(2),p)).mod(p);
                if(!uW.equals(gm)){log.error(String.valueOf(index)+" verify tokeni false"+" \n"+tokenSi.getUi()+"\n"+tokenSi.getWi()+" \n"+Hi.toString());
                    //return false;
                     }else{ log.error(String.valueOf(index)+" verify tokeni True");}
                W=W.multiply(new BigInteger(tokenSi.getWi())).mod(p);
            }
            BigInteger YW=y.modPow(new BigInteger(userMsg2Serv.getMsgHash()),p);
            YW=YW.multiply(W.modPow(BigInteger.valueOf(2),p)).mod(p);
            BigInteger msg=new BigInteger(userMsg2Serv.getMsg()).mod(q);//cautious!!!mod
            BigInteger GkM=g.modPow(msg.multiply(BigInteger.valueOf(threshold)).mod(p),p);
            if(!YW.equals(GkM)){
                System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFF YW");//return false;
            }else{ System.out.println("tttttttttttttt");}//Token Success!
            if(YW.equals(BigInteger.ONE)){
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            return true;
        }catch (InterruptedException e){
            executor.shutdown();
            throw new RuntimeException();
        }
    }
}
