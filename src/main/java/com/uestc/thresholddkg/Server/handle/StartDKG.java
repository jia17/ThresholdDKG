package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-02 10:17
 */
@Setter
@Slf4j
public class StartDKG implements HttpHandler {
    private  String[] ipAndPort;
    private String addr;
    private IdpServer idpServer;
    @PostConstruct
    public void getAddr(){

    }
    public StartDKG(String addr,IdpServer idpServer){this.addr=addr;ipAndPort=IdpServer.addrS;this.idpServer=idpServer;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        log.error("START DKG "+mess);
        String[] IdPass=mess.split("\\|");
        String user=IdPass[0],passwd="";//IdPass[1];
        //String user="alice",passwd="123456";//get from browser ,wait for update
        int serversNum=ipAndPort.length;
        DKG_System param= DKG.init();
        DKG_SysStr dkg_sysStr=new DKG_SysStr(param.getP().toString(),param.getQ().toString(),param.getG().toString(),param.getH().toString());
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutorService service = Executors.newFixedThreadPool(serversNum - 1);
                CountDownLatch latch=new CountDownLatch(serversNum-1);
                ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
                BigInteger[] secrets=new BigInteger[2];
                secrets[0]= RandomGenerator.genaratePositiveRandom(param.getP());
                secrets[1]=RandomGenerator.genaratePositiveRandom(param.getP());
                idpServer.getSecretAndT().put(user,secrets);
                DkgSysMsg message=new DkgSysMsg(dkg_sysStr,user,passwd,idpServer.item);
                idpServer.getDkgParam().put(user,param);
                idpServer.getFlag().put(user,0);
                idpServer.getFgRecv().put(user,new HashMap<>());
                idpServer.getFgRecvFalse().put(user,new HashSet<>());
                idpServer.getFgRecvFTimes().put(user,new ConcurrentHashMap<>());
                DKG.initMapTimes(idpServer.getFgRecvFTimes().get(user));
                var convert=new Convert2Str();
                for (String s : ipAndPort) {
                    //log.warn("send"+s);
                    if (("/" + s).equals(addr)) continue;
                    service.submit(
                            BroadCastMsg.builder().latch(latch).message(convert.Obj2json(message)).mapper("initDkg").IpAndPort(s)
                                    .failsMap(resMap).build()
                    );
                }
                try {
                    latch.await();
                    if(!resMap.isEmpty()){
                        resMap.forEach((key, value) ->service.submit(
                                BroadCastMsg.builder().latch(latch).message(convert.Obj2json(message)).mapper("initDkg").IpAndPort(key)
                                .failsMap(resMap).build()));
                        Thread.sleep(500);
                    }
                    //broad cast
                    Thread generateFG=new Thread(new GenarateFuncBroad(idpServer,user,ipAndPort,idpServer.getServer().getAddress().toString()));
                    generateFG.start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                service.shutdown(); }
                });t.start();
//        ExecutorService service= Executors.newFixedThreadPool(serversNum-1);
//        CountDownLatch countDownLatch=new CountDownLatch(serversNum-1);
//        DKG_System param= DKG.init();
//        DkgSysMsg message=new DkgSysMsg(param,user,passwd,idpServer.item);
//        idpServer.getDkgParam().put(user,param);
//        for (String s : ipAndPort) {
//            if (("/" + s).equals(addr)) continue;
//            var convert=new DkgSystem2Obj();
//            SendUri send = SendUri.builder().message(convert.Obj2json(param)).mapper("initDkg").IpAndPort(s).build();
//            //send.SendMsg();
//            service.submit(send::SendMsg);
//        }
//        service.shutdown();

        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        try {
            var respContents=  Convert2Str.Obj2json(dkg_sysStr).getBytes();
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            } catch (IOException e) {
                 throw new RuntimeException(e);
        }
        httpExchange.close();
        // 关闭处理器, 同时将关闭请求和响应的输入输出流（如果还没关闭）
    }
}
