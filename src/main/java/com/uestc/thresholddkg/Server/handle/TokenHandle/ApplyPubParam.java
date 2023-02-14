package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.PubParamToken;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import lombok.AllArgsConstructor;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-02-04 16:58
 */
@AllArgsConstructor
public class ApplyPubParam implements HttpHandler {
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
        if((!idpServer.getFlag().containsKey(userId)||idpServer.getFlag().get(userId)!=4)) {
            byte[] respContents = "str".getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(500, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();return;
        }
        DKG_System dkg_system=idpServer.getDkgParam().get(userId);
        DKG_SysStr dkg_sysStr=new DKG_SysStr(dkg_system.getP().toString(),dkg_system.getQ().toString(),dkg_system.getG().toString(),dkg_system.getH().toString());
        BigInteger p=dkg_system.getP();
        BigInteger[] pubKey=new BigInteger[]{BigInteger.ONE};
        if(idpServer.getFExpRecv().containsKey(userId)){
            idpServer.getFExpRecv().get(userId).forEach((k,v)->{pubKey[0]=pubKey[0].multiply(v).mod(p);});
        }
        idpServer.getUserY().put(userId,pubKey[0].toString());
        PubParamToken pubParamToken=PubParamToken.builder().userId(userId).y(pubKey[0].toString()).dkg_sysStr(dkg_sysStr).build();
        String str= Convert2StrToken.Obj2json(pubParamToken);
        byte[] respContents = str.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
