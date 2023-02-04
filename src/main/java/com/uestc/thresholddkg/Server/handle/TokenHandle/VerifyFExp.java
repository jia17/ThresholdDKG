package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.InvalidServBroad;
import com.uestc.thresholddkg.Server.DkgCommunicate.PubKeyBroad;
import com.uestc.thresholddkg.Server.DkgCommunicate.RegetFGval;
import com.uestc.thresholddkg.Server.DkgCommunicate.SendDKGComplain;
import com.uestc.thresholddkg.Server.DkgCommunicate.TokenComm.InvalidFexpBroad;
import com.uestc.thresholddkg.Server.DkgCommunicate.TokenComm.RegetFExp;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.FunctionFExp;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import javax.persistence.Id;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-02-03 21:30
 */
@Slf4j
@AllArgsConstructor
public class VerifyFExp implements HttpHandler {
    IdpServer idpServer;

    @SneakyThrows
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "VeriFEXp".getBytes("UTF-8");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        //log.error(tline);

        var Fexp=(FunctionFExp) Convert2StrToken.Json2obj(tline, FunctionFExp.class);
        String userId=Fexp.getUserId();
        String remoteAddr=Fexp.getSendAddr();
        if(!idpServer.getFgRecv().containsKey(userId)||!idpServer.getFgRecv().get(userId).containsKey(remoteAddr)){// cautious
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();
            Thread.sleep(200);
            Thread ReCall=new Thread(new RegetFExp(idpServer,userId,remoteAddr));
            ReCall.start();
            return;
        }
        DKG_System dkgSys=idpServer.getDkgParam().get(userId);
        BigInteger p=dkgSys.getP();
        BigInteger Fi=idpServer.getFgRecv().get(userId).get(remoteAddr);;
        BigInteger verify1=(Calculate.modPow(dkgSys.getG(),Fi,p)).mod(p);
        boolean satisfy= DKG.VerifyGH(verify1,DKG.str2BigInt(Fexp.getFExp()),Fexp.getServerId(),p);
        int LenQual=idpServer.getFgRecv().get(userId).size();
        if(satisfy){
            //写入server
            idpServer.getFExpRecv().get(userId).put(remoteAddr,new BigInteger(Fexp.getFExp()[0]));
            idpServer.getFExpFalse().get(userId).remove(remoteAddr);
            log.error("Server"+Fexp.getServerId()+"verify true FEXP"+remoteAddr);
            if(idpServer.getFlag().get(userId)==2&&(idpServer.getFExpFalse().get(userId).size()+idpServer.getFExpRecv().get(userId).size())==LenQual){
                idpServer.getFlag().put(userId,3);
            }
            if(idpServer.getFlag().get(userId)==3&&idpServer.getFExpFalse().get(userId).isEmpty()){
                idpServer.getFlag().put(userId,4);
                log.error(idpServer.getServer().getAddress().toString()+"QUAL PUBKey"+idpServer.getFExpRecv().get(userId).size());
            }
        }else{
            var falseSet=idpServer.getFExpFalse().get(userId);
            if(falseSet.contains(remoteAddr)){
                falseSet.remove(remoteAddr);
                idpServer.getFExpRecv().get(userId).remove(remoteAddr);//remove invalid twice
                log.error(remoteAddr+"INVALID twice FExp,"+httpExchange.getLocalAddress());
                Thread InvBroad=new Thread(new InvalidFexpBroad(idpServer,userId,remoteAddr));
                InvBroad.start();
                if(falseSet.isEmpty()){
                    idpServer.getFlag().put(userId,4);
                    log.error(idpServer.getServer().getAddress().toString()+"QUALf FExp:"+idpServer.getFExpRecv().get(userId).size());
                }
            }else{//once false f,g
                falseSet.add(remoteAddr);
                if((falseSet.size()+idpServer.getFExpRecv().get(userId).size())==LenQual){
                    idpServer.getFlag().put(userId,3);
                    Thread ReCall=new Thread(new RegetFExp(idpServer,userId,remoteAddr));
                    ReCall.start();
                }
            }
            log.error("Server"+ Fexp.getServerId()+"verify false FEXP"+remoteAddr);
        }
//        var Fexp=(FunctionFExp) Convert2StrToken.Json2obj(tline, FunctionFExp.class);
//        String userId=Fexp.getUserId();
//        String remoteAddr=Fexp.getSendAddr();
//        if(!idpServer.getFgRecv().containsKey(userId)||!idpServer.getFgRecv().get(userId).containsKey(remoteAddr)){
//            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
//            httpExchange.sendResponseHeaders(200, respContents.length);
//            httpExchange.getResponseBody().write(respContents);
//            httpExchange.close();
//            return;
//        }
//        DKG_System dkgSys=idpServer.getDkgParam().get(userId);
//        BigInteger p=dkgSys.getP();
//        BigInteger Fi=idpServer.getFgRecv().get(userId).get(remoteAddr);;
//        BigInteger verify1=(Calculate.modPow(dkgSys.getG(),Fi,p)).mod(p);
//        boolean satisfy= DKG.VerifyGH(verify1,DKG.str2BigInt(Fexp.getFExp()),Fexp.getServerId(),p);
//        int LenQual=idpServer.getFgRecv().get(userId).size();
//        if(satisfy){
//            //写入server
//            idpServer.getFExpRecv().get(userId).put(remoteAddr,new BigInteger(Fexp.getFExp()[0]));
//            idpServer.getFExpFalse().get(userId).remove(remoteAddr);
//            log.error("Server"+Fexp.getServerId()+"verify true FEXP"+remoteAddr);
//            if(idpServer.getFlag().get(userId)==2&&(idpServer.getFExpFalse().get(userId).size()+idpServer.getFExpRecv().get(userId).size())==LenQual){
//                idpServer.getFlag().put(userId,3);
//            }
//            if(idpServer.getFlag().get(userId)==3&&idpServer.getFExpFalse().get(userId).isEmpty()){
//                idpServer.getFlag().put(userId,4);
//                log.error(idpServer.getServer().getAddress().toString()+"QUAL PUBKey"+idpServer.getFExpRecv().get(userId).size());
//            }
//        }else{
//            var falseSet=idpServer.getFExpFalse().get(userId);
//            if(falseSet.contains(remoteAddr)){
//                falseSet.remove(remoteAddr);
//                idpServer.getFExpRecv().get(userId).remove(remoteAddr);//remove invalid twice
//                log.error(remoteAddr+"INVALID twice FExp,"+httpExchange.getLocalAddress());
//                Thread InvBroad=new Thread(new InvalidFexpBroad(idpServer,userId,remoteAddr));
//                InvBroad.start();
//                if(falseSet.isEmpty()){
//                    idpServer.getFlag().put(userId,4);
//                    log.error(idpServer.getServer().getAddress().toString()+"QUALf FExp:"+idpServer.getFExpRecv().get(userId).size());
//                }
//            }else{//once false f,g
//                falseSet.add(remoteAddr);
//                if((falseSet.size()+idpServer.getFExpRecv().get(userId).size())==LenQual){
//                    idpServer.getFlag().put(userId,3);
//                    Thread ReCall=new Thread(new RegetFExp(idpServer,userId,remoteAddr));
//                    ReCall.start();
//                }
//            }
//            log.error("Server"+ Fexp.getServerId()+"verify false FEXP"+remoteAddr);
//        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
