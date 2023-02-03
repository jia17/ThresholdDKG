package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.user.PrfComm.Hash1BroadGet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-02-03 21:30
 */
@Slf4j
@AllArgsConstructor
public class GetInvalidFExp implements HttpHandler {
    IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String InvalidMap="";
        String line;
        while((line=reader.readLine())!=null){
            InvalidMap+=line;
        }
        JSONObject jsonobject = JSONObject.fromObject(InvalidMap);
        Map<String,String> mapInvalid=(Map<String,String>) JSONObject.toBean(jsonobject,Map.class);
        mapInvalid.forEach((k,v)->{
            String[] Addrs=v.split("@");;//invalid addr+@+send addr;
            idpServer.getFExpRecv().get(k).remove(Addrs[0]);
            log.error(httpExchange.getLocalAddress()+"Invalid FEXP map Remove"+v+" user:"+k+" size "+idpServer.getFgRecv().get(k).size());
        });
        byte[] respContents = "ReSendF".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
