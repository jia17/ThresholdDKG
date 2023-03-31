package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import com.uestc.thresholddkg.Server.util.getRedis;
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
 * @date 2023-02-27 15:47
 */
@AllArgsConstructor
public class LoginOut implements HttpHandler {

    private IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String user="";
        String line;
        while((line=reader.readLine())!=null){
            user+=line;
        }
        byte[] respContents = "logout".getBytes("UTF-8");
        //logout?user=nii
        /*idpServer.getUserMsgHash().remove(user);
        idpServer.getUserMsg().remove(user);
        idpServer.getMsgTime().remove(user);*/
        getRedis.removeMsg(user);
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
