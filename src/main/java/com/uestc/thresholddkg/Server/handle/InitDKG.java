package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangjia
 * @date 2023-01-10 11:14
 */
@Slf4j
public class InitDKG implements HttpHandler {

    private IdpServer idpServer;
    private String addr;
    public InitDKG(String _addr,IdpServer _idp){addr=_addr;idpServer=_idp;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "initSuccess".getBytes("UTF-8");
        //log.error(addr+"get");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        var convert=new Convert2Str();
        DkgSysMsg param=(DkgSysMsg) convert.Json2obj(tline,DkgSysMsg.class);
        if(param.getPasswd()!=" "){idpServer.getPubId().add(param.getPasswd());String user=param.getId();
            if(!idpServer.getFExpRecv().containsKey(user)){
            idpServer.getFExpRecv().put(user,new HashMap<>());
            idpServer.getFExpFalse().put(user,new HashSet<>());}}
        DKG_System dkg_system=new DKG_System(new BigInteger(param.getDkg_sysStr().getP()),
                new BigInteger(param.getDkg_sysStr().getQ()),
                new BigInteger(param.getDkg_sysStr().getG()),
                new BigInteger(param.getDkg_sysStr().getH()));
        idpServer.getDkgParam().put(param.getId(),dkg_system);
        BigInteger[] secrets=new BigInteger[2];
        secrets[0]= RandomGenerator.genaratePositiveRandom(dkg_system.getP());
        secrets[1]=RandomGenerator.genaratePositiveRandom(dkg_system.getP());
        idpServer.getSecretAndT().put(param.getId(),secrets);
        idpServer.getFgRecv().put(param.getId(),new HashMap<>());
        idpServer.getFgRecvFalse().put(param.getId(),new HashSet<>());
        idpServer.getFlag().put(param.getId(),0);
        idpServer.getFgRecvFTimes().put(param.getId(),new ConcurrentHashMap<>());
        DKG.initMapTimes(idpServer.getFgRecvFTimes().get(param.getId()));
        log.warn(addr+" get "+idpServer.getDkgParam().get(param.getId()));
        Thread generateFG=new Thread(new GenarateFuncBroad(idpServer,param.getId(),IdpServer.addrS,idpServer.getServer().getAddress().toString()));
        generateFG.start();
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
