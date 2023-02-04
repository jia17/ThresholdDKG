package com.uestc.thresholddkg.Server.user;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.user.PrfComm.Hash1BroadGet;
import com.uestc.thresholddkg.Server.user.TokenComm.GetTokenSi;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.FutureTask;

/**
 * @author zhangjia
 * @date 2023-02-03 20:38
 */
@Slf4j
@AllArgsConstructor
public class startToken implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String user="alicess";
        var msg= TestUserMsg.builder().userId(user).pwd("sasas").man(false).num(333).email("12148688688@qq,com").build();
        var msgStr= Convert2StrToken.Obj2json(msg);
        var bytes=msgStr.getBytes();
        String[] ipPorts= IdpServer.addrS;
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(ipPorts.length);
        SendUri send = SendUri.builder().message(user+"|"+"Passwd").mapper("startDkgT")//ID can't have "|"
                .IpAndPort(ipPorts[servI])
                .build();
        String dkg_Sys=send.SendMsg();
        DKG_SysStr dkg_sysStr=(DKG_SysStr) Convert2Str.Json2obj(dkg_Sys, DKG_SysStr.class);
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        BigInteger p=dkg_system.getP();
        BigInteger msgBigIn=new BigInteger(1,bytes).mod(dkg_system.getP());
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0, bytes.length/2));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,bytes.length/2,bytes.length));
        var msgHash=(dkg_system.getG().modPow(b1,p).multiply(dkg_system.getH().modPow(b2,p))).mod(p);
        Thread thread0=new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    String[] sendAddrs=new String[IdpServer.threshold];
                    boolean[] exists=new boolean[ipPorts.length];
                    for(int i=0;i<sendAddrs.length;){
                        SecureRandom random1=new SecureRandom();
                        int servi= random1.nextInt(ipPorts.length);
                        if(!exists[servi]){sendAddrs[i]=ipPorts[servi];i++;exists[servi]=true;}
                    }
                    FutureTask<Boolean> futureTask=new FutureTask<Boolean>(new GetTokenSi(sendAddrs, UserMsg2Serv.builder().msg(msgBigIn.toString()).msgHash(msgHash.toString()).userId(user).build()));
                    Thread thread=new Thread(futureTask);
                    try {Thread.sleep(1000);} catch (InterruptedException e){
                        throw new RuntimeException(e);}
                    thread.start();
                    try {
                        if(futureTask.get()){break;}
                        else{Thread.sleep(1000);}
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }}
        });thread0.start();
        byte[] respContents = msgHash.toString().getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
