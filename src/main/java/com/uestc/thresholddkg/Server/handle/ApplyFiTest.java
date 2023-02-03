package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.AllArgsConstructor;
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
 * @date 2023-01-12 22:52
 * send secret si to server who want to reconstruct S
 */
@AllArgsConstructor
public class ApplyFiTest implements HttpHandler {
    private IdpServer idpServer;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String userId="";
        String line;
        while((line=reader.readLine())!=null){
            userId+=line;
        }
        BigInteger secretI= Calculate.addsPow(idpServer.getFgRecv().get(userId),idpServer.getDkgParam().get(userId).getQ());
        BigInteger p=idpServer.getDkgParam().get(userId).getP();
        BigInteger[] pubKey=new BigInteger[]{BigInteger.ONE};
        if(idpServer.getFExpRecv().containsKey(userId))idpServer.getFExpRecv().get(userId).forEach((k,v)->{pubKey[0]=pubKey[0].multiply(v).mod(p);});
        String str=secretI.toString()+"@"+pubKey[0].toString();
        byte[] respContents = str.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
