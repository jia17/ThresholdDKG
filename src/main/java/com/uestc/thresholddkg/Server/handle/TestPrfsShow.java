package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BoradTest;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.SneakyThrows;
import lombok.var;

import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author zhangjia
 * @date 2023-02-28 13:26
 */
public class TestPrfsShow implements HttpHandler {

    @SneakyThrows
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String userId=httpExchange.getRequestURI().toString().split("=")[1];
        System.out.println(userId);
        SendUri send = SendUri.builder().message(userId+"|"+"Passwd").mapper("startDkg")//ID can't have "|"
                .IpAndPort(IdpServer.addrS[3])
                .build();
        String dkg_Sys=send.SendMsg();
        DKG_SysStr dkg_sysStr=(DKG_SysStr) Convert2Str.Json2obj(dkg_Sys, DKG_SysStr.class);
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        String[] ippStr=IdpServer.addrS;
        int serversNum=ippStr.length;
        final ExecutorService executor = Executors.newFixedThreadPool(serversNum);
        final CountDownLatch latch  = new CountDownLatch(serversNum);//server self has a secretI
        ConcurrentHashMap<String,String> resPri=new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> resPriK=new ConcurrentHashMap<>();
        int serverId=0;
        for (String addr:ippStr) {
            executor.submit(
                    BoradTest.builder().latch(latch).message(userId).mapper("TestPrfsApply").IpAndPort(addr)
                            .resmap(resPriK).resPubMap(resPri).build()
            );
        }
        latch.await();
        executor.shutdown();
        BigInteger q=dkg_system.getQ();
        BigInteger g=dkg_system.getG();
        BigInteger p=dkg_system.getP();
        String sys="<h3 style=\"color:FF6633\">系统参数</h3>"+MessageFormat.format("<h3>p:{0}  q:{1}</h3>",dkg_sysStr.getP(),dkg_sysStr.getQ());
        String[] res=new String[2];
        res[0]=sys+ MessageFormat.format("<h3 style=\"color:FF6633\">用户名:<u>{0}</u></h3>",userId);
        res[0]+="<h4 style=\"color:FF6633\">用户口令prf共享值</h4>";
        res[1]="<h4 style=\"color:FF6633\">口令加密密钥共享值</h4>";
        resPri.forEach((k,v)->{
            res[0]+=MessageFormat.format("<h4>server:{0}  prfi:{1}</h4>",k,v);
            res[1]+=MessageFormat.format("<h4>server:{0}   keyi:{1}</h4>",k,resPriK.get(k));
        });
        res[0]+=res[1];
        Integer threshold=IdpServer.threshold;
        Integer[] Index=new Integer[threshold];
        BigInteger[] value=new BigInteger[threshold];
        Integer[] is={0};
        resPriK.forEach((k,v)->{if(is[0]<threshold){
            int in=Integer.parseInt(String.valueOf(k.charAt(k.length()-2)));
            Index[is[0]]=in;value[is[0]]=new BigInteger(v);is[0]=is[0]+1;}
        });
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
        res[0]+="<h4 style=\"color:FF6633\">口令加密密钥</h4><h4>key  =  "+secret2.toString();
        byte[] respContents = res[0].getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
