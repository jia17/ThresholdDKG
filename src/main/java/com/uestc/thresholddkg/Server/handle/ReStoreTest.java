package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BoradTest;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.handle.TokenHandle.StartDkgToken;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Prime;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.*;

/**
 * @author zhangjia
 * @date 2023-01-03 13:25
 * test to restore Secret s
 */
@Setter
@Slf4j
public class ReStoreTest implements HttpHandler {
    private  String[] ipAndPort;
    private IdpServer idpServer;
    @PostConstruct
    public void getAddr(){
    }
    private String AddrSelf;
    public ReStoreTest(IdpServer idpServer,String addr){this.idpServer=idpServer;AddrSelf=addr;ipAndPort= IdpServer.addrS;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        log.warn(ipAndPort.toString());
        //var staD=new StartDKG("",idpServer);
        var userId= StartDkgToken.testString;//StartDKG.testString;//="alice";
        String[] ippStr=ipAndPort;
        int serversNum=ippStr.length;
        final ExecutorService executor = Executors.newFixedThreadPool(serversNum - 1);
        final CountDownLatch    latch  = new CountDownLatch(serversNum>>1);//server self has a secretI
        final AtomicInteger failureCounter = new AtomicInteger(0);
        final int maximumFailures = serversNum-(serversNum>>1+1);
        ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> resPubMap=new ConcurrentHashMap<>();
        int serverId=0;
        for(String addr:ippStr){
            serverId++;
            if(("/"+addr).equals(AddrSelf))continue;
            // send = SendUri.builder().message(Integer.toString(serverId)).mapper("test").IpAndPort(addr).build();
            //send.SendMsg();
            executor.submit(
                BoradTest.builder().latch(latch).message(userId).mapper("applyTestRestore").IpAndPort(addr)
                        .failCount(failureCounter).maxFail(maximumFailures).resmap(resMap).resPubMap(resPubMap).build()
            );
        }
        try {
            latch.await();
            if(failureCounter.get()<=maximumFailures){
                Integer threshold=(serversNum>>1)+1;
                BigInteger q=idpServer.getDkgParam().get(userId).getQ();
                BigInteger g=idpServer.getDkgParam().get(userId).getG();
                BigInteger p=idpServer.getDkgParam().get(userId).getP();
                BigInteger secretSelf= Calculate.addsPow(idpServer.getFgRecv().get(userId),q);
                Map<String, BigInteger> map=new HashMap<>();
                resMap.forEach((key, value) -> {
                if(map.size()<(threshold-1))map.put(key,new BigInteger(value));});
                executor.shutdown();

                map.put(AddrSelf,secretSelf);
                Integer[] Index=new Integer[threshold];
                BigInteger[] value=new BigInteger[threshold];
                Integer[] is={0};
                map.forEach((k,v)->{System.out.println("/"+k + " send " + v);int in=Integer.parseInt(String.valueOf(k.charAt(k.length()-2)));
                Index[is[0]]=in;value[is[0]]=v;is[0]=is[0]+1;});

                Integer listMuls=1;
                for (Integer val : Index) { listMuls*=val;  }
                BigInteger secret2=BigInteger.ZERO;
                BigInteger tempBigMul;
                Integer tempMul=0;
                for(int  i=0;i<threshold;i++){
                    tempMul=Index[i];
                    if(((threshold)&1)==0)tempMul*=-1;
                    for(int j=0;j<threshold;j++){
                        if(i!=j){tempMul*=(Index[i]-Index[j]);}
                    }
                    tempBigMul=value[i].multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
                    if(tempMul<0)secret2=(secret2.add(q.add(tempBigMul.negate()))).mod(q);
                    else secret2=secret2.add(tempBigMul).mod(q);
                }
                secret2=(secret2.multiply(BigInteger.valueOf(listMuls))).mod(q);
                /*for(int t=0;t<1000000;t++){
                    if(Prime.isPrime(secret2))break;
                    secret2=secret2.add(BigInteger.ONE).mod(q);
                }*/
                String res="<p>"+secret2+"</p>";
                String[] ress=new String[]{res};final BigInteger secretp=secret2;
                resPubMap.forEach((k,v)->{ress[0]=ress[0]+"<p>"+k+"  "+v+"  "+v.equals(g.modPow(secretp,p).toString())+"</p>";});
                byte[] respContents = ress[0].getBytes("UTF-8");
                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                httpExchange.sendResponseHeaders(200, respContents.length);
                httpExchange.getResponseBody().write(respContents);
                httpExchange.close();
            }else{
                executor.shutdown();
                throw new IOException();
            }
        }catch (InterruptedException e){
            throw new RuntimeException();
        }
    }
}
