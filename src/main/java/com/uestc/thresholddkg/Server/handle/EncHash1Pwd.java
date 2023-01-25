package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.IdHash1;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.concurrent.Executor;

/**
 * @author zhangjia
 * @date 2023-01-24 16:51
 */
@Slf4j
@AllArgsConstructor
public class EncHash1Pwd implements HttpHandler {
    private IdpServer idpServer;


    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){tline+=line;}
        IdHash1 idHash1=(IdHash1)Convert2Str.Json2obj(tline, IdHash1.class);
        String ID=idHash1.getId();
        //System.out.println(idpServer.getFlag());
        if(!idpServer.getFlag().containsKey(ID)||idpServer.getFlag().get(ID)!=2) {
            //if(idpServer.getFlag().containsKey(ID)) System.out.println(idpServer.getServer().getAddress().toString()+"  "+idpServer.getFlag().get(ID));
            byte[] respContents = "str".getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(500, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();return;
        }
        DKG_System dkg_system=idpServer.getDkgParam().get(ID);
        BigInteger Hash1Pwd=new BigInteger(idHash1.getPwdHash1());
        BigInteger secretI= Calculate.addsPow(idpServer.getFgRecv().get(ID),dkg_system.getQ());
        BigInteger EncHash1Pwd=Hash1Pwd.modPow(secretI,dkg_system.getP());

        byte[] respContents = EncHash1Pwd.toString().getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
