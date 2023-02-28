package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.InvalidServBroad;
import com.uestc.thresholddkg.Server.DkgCommunicate.PubKeyBroad;
import com.uestc.thresholddkg.Server.DkgCommunicate.RegetFGval;
import com.uestc.thresholddkg.Server.DkgCommunicate.SendDKGComplain;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.*;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-01-10 15:52
 */
@Slf4j
public class VerifyGH implements HttpHandler {
    private IdpServer idpServer;
    private String[] ipPorts;
    private Map<String, ConcurrentSkipListSet<String>> recvInvalid;

    public VerifyGH(IdpServer _idp,Map<String, ConcurrentSkipListSet<String>> recvInvalid){
        idpServer=_idp;this.ipPorts=IdpServer.addrS;
        this.recvInvalid=recvInvalid;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "VerifySuccess".getBytes("UTF-8");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        //log.error(tline);
        FunctionGHvals ghVals=(FunctionGHvals) Convert2Str.Json2obj(tline, FunctionGHvals.class);
        String userId=ghVals.getUserId();
        String remoteAddr=ghVals.getSendAddr();
        DKG_System dkgSys=idpServer.getDkgParam().get(userId);
        BigInteger p=dkgSys.getP();
        BigInteger verify1=((Calculate.modPow(dkgSys.getG(),new BigInteger(ghVals.getFi()),p)).multiply(Calculate.modPow(dkgSys.getH(),new BigInteger(ghVals.getGi()),p))).mod(p);
        boolean satisfy=DKG.VerifyGH(verify1,DKG.str2BigInt(ghVals.getGMulsH()),ghVals.getServerId(),p);
        if(!recvInvalid.containsKey(userId))recvInvalid.put(userId,new ConcurrentSkipListSet<>());
        if(satisfy){
            //写入server
            //if invalid ,don't add
            if(!recvInvalid.get(userId).contains(remoteAddr))idpServer.getFgRecv().get(userId).put(remoteAddr,new BigInteger(ghVals.getFi()));
            idpServer.getFgRecvFalse().get(userId).remove(remoteAddr);
            log.error("Server"+ghVals.getServerId()+"verify true "+remoteAddr);
            if(idpServer.getFlag().get(userId)==0&&(idpServer.getFgRecvFalse().get(userId).size()+idpServer.getFgRecv().get(userId).size())== ipPorts.length){
                idpServer.getFlag().put(userId,1);
                //broadcast complain And Recall
                Thread bComp=new Thread(new SendDKGComplain(idpServer,userId));
                bComp.start();
                Thread ReCall=new Thread(new RegetFGval(idpServer,userId));
                ReCall.start();
            }
            if(idpServer.getFlag().get(userId)==1&&idpServer.getFgRecvFalse().get(userId).isEmpty()){
                //CollectCompl.RevCompl.remove(userId);
                idpServer.getFlag().put(userId,2);
                log.error(idpServer.getServer().getAddress().toString()+"QUALs"+idpServer.getFgRecv().get(userId).size());
                Thread pub=new Thread(new PubKeyBroad(idpServer,userId));
                if(idpServer.getPubId().contains(userId))pub.start();
            }
        }else{
            var falseSet=idpServer.getFgRecvFalse().get(userId);
            //twice false f,g
            if(falseSet.contains(remoteAddr)){
                falseSet.remove(remoteAddr);
                idpServer.getFgRecv().get(userId).remove(remoteAddr);//remove invalid twice
                idpServer.getFExpRecv().get(userId).remove(remoteAddr);
                log.error(remoteAddr+"INVALID twice,"+httpExchange.getLocalAddress());
                //Send Invalid addr
                Thread InvBroad=new Thread(new InvalidServBroad(idpServer,userId,remoteAddr,recvInvalid));
                InvBroad.start();
                if(falseSet.isEmpty()){
                   // CollectCompl.RevCompl.remove(userId);//remove useless Map
                    idpServer.getFlag().put(userId,2);
                    log.error(idpServer.getServer().getAddress().toString()+"QUALf :"+idpServer.getFgRecv().get(userId).size());
                Thread pub=new Thread(new PubKeyBroad(idpServer,userId));
                if(idpServer.getPubId().contains(userId))pub.start();}
            }else{//once false f,g
                falseSet.add(remoteAddr);
                //recv a complaint about (user,remoteAddr)
                int times=idpServer.getFgRecvFTimes().get(userId).get(remoteAddr)-1;
                idpServer.getFgRecvFTimes().get(userId).put(remoteAddr,times);//2.26 cautious
                if((falseSet.size()+idpServer.getFgRecv().get(userId).size())== ipPorts.length){
                    idpServer.getFlag().put(userId,1);
                    //broadcast complain
                    Thread bComp=new Thread(new SendDKGComplain(idpServer,userId));
                    bComp.start();
                    Thread ReCall=new Thread(new RegetFGval(idpServer,userId));
                    ReCall.start();
                }
            }
            log.error("Server"+ghVals.getServerId()+"verify false "+remoteAddr);
        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
