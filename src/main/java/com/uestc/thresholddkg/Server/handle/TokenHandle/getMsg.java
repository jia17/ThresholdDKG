package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.UserMsg2Serv;
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
 * @date 2023-02-11 23:48
 */
@AllArgsConstructor
public class getMsg implements HttpHandler {
    private IdpServer idpServer;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String msg="";
        String line;
        while((line=reader.readLine())!=null){
            msg+=line;
        }
        var userMsg=(UserMsg2Serv) Convert2StrToken.Json2obj(msg, UserMsg2Serv.class);
        idpServer.getUserMsg().put(userMsg.getUserId(),userMsg.getMsg());
        idpServer.getUserMsgHash().put(userMsg.getUserId(), userMsg.getMsgHash());
        byte[] respContents = "str".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
