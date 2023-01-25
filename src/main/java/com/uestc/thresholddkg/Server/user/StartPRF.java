package com.uestc.thresholddkg.Server.user;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.user.PrfComm.Hash1BroadGet;
import com.uestc.thresholddkg.Server.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author zhangjia
 * @date 2023-01-23 09:53
 */
@Slf4j
@AllArgsConstructor
public class StartPRF implements HttpHandler {
    private HttpServer userServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String ID="alice";String Passwd="12345678";
//        var Sender=httpExchange.getRequestBody();
//        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
//        String userId="";
//        String line;
//        while((line=reader.readLine())!=null){
//            userId+=line;
//        }
        String[] ipPorts=IdpServer.addrS;
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(ipPorts.length);
        SendUri send = SendUri.builder().message(ID+"|"+"Passwd").mapper("startDkg")//ID can't have "|"
                .IpAndPort(ipPorts[servI])
                .build();
        String dkg_Sys=send.SendMsg();
        DKG_SysStr dkg_sysStr=(DKG_SysStr) Convert2Str.Json2obj(dkg_Sys, DKG_SysStr.class);
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        log.error("PRF "+dkg_system);
        BigInteger p=dkg_system.getP();
        byte[] bytes= DKG.HashSha3(Passwd);
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0,DKG.KeyLen/16));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,DKG.KeyLen/16,DKG.KeyLen/8));
        var pwdHash1=(dkg_system.getG().modPow(b1,p).multiply(dkg_system.getH().modPow(b2,p))).mod(p);
        String temps="";int hash1Len=(int)(DKG.KeyLen*0.30103);
        if(pwdHash1.toString().length()<hash1Len){//154=512*0.302=*ln(2)/ln(10)
            StringBuilder blank= new StringBuilder();
            for(int i=hash1Len-pwdHash1.toString().length();i>0;i--)blank.append("0");
            temps=blank.toString()+pwdHash1.toString();
            pwdHash1=new BigInteger(temps);
        }
        System.out.println(pwdHash1.toString()+" Hash1len"+temps.length());
        BigInteger randR= RandomGenerator.genaratePositiveRandom(dkg_system.getQ());
        final BigInteger pwdHash11= pwdHash1.modPow(randR,p);
        Thread thread0=new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                FutureTask<Boolean> futureTask=new FutureTask<Boolean>(new Hash1BroadGet(ID,pwdHash11.toString(),dkg_system,Calculate.modInverse(randR,dkg_system.getQ()),Passwd));
                Thread thread=new Thread(futureTask);
                thread.start();
                    try {
                        if(futureTask.get()){break;}
                        else{Thread.sleep(2000);}
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }}
        });thread0.start();
        byte[] respContents = "sPrf".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
       // httpExchange.getResponseHeaders().add("Location","http://www.baidu.com");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
