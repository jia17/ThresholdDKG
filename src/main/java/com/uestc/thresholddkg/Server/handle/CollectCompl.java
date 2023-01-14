package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.Complain;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.*;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-01-11 18:16
 */
@Slf4j
public class CollectCompl implements HttpHandler {

    private IdpServer idpServer;
    public  Map<String, Set<String>> RevCompl;
    private Map<String, ConcurrentSkipListSet<String>> recvInvalid;
    private String[] IpPorts;

    public CollectCompl(IdpServer idpServer1,Map<String, ConcurrentSkipListSet<String>> map){
        idpServer=idpServer1;RevCompl=new HashMap<>();recvInvalid=map;IpPorts=IdpServer.addrS;
    }
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "getCompls".getBytes("UTF-8");
        //log.error(addr+"get");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        var convert=new Convert2Str();
        Complain param=(Complain) convert.Json2obj(tline,Complain.class);
        String userId=param.getUserId();
        if(!RevCompl.containsKey(userId)){RevCompl.put(userId,new HashSet<>());}
        {
            Set<String> complsColl=RevCompl.get(userId);
            if(!complsColl.contains(param.getSendAddr())){
                complsColl.add(param.getSendAddr());
                for (String addrComp:param.getCompls()) {
                    //idpServer.getFgRecvFTimes().get(userId).put(userId,idpServer.getFgRecvFTimes().get(userId).get(addrComp)-1);
                    idpServer.getFgRecvFTimes().get(userId).compute(addrComp,(k,v)->--v);
                    log.warn(httpExchange.getLocalAddress()+" get complain"+userId+addrComp);
                    int times=idpServer.getFgRecvFTimes().get(userId).get(addrComp);
                    if(httpExchange.getLocalAddress().toString().equals("/127.0.0.10:9060")){
                        System.out.println(times);
                    }
                    if(times<0){
                        ConcurrentSkipListSet<String> recvInvU=recvInvalid.get(userId);//more than t,don't invalid;at least add addrComp for verify success
                        for (String ipp:IpPorts) {recvInvU.add("/"+ipp);}
                        idpServer.getFgRecv().get(userId).remove(addrComp);
                        log.warn(httpExchange.getLocalAddress().toString()+" INVALID More t compls to"+addrComp);
                    }
                }
            }
        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
