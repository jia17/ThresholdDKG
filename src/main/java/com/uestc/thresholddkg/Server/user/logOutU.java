package com.uestc.thresholddkg.Server.user;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.communicate.BroadCheckS;
import lombok.AllArgsConstructor;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-02-27 15:53
 */
@AllArgsConstructor
public class logOutU implements HttpHandler {
    private ExecutorService service;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var req=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(req));
        String user="";
        String line;
        String res="";
        while((line=reader.readLine())!=null){
            user+=line;
        }
        if(user!=""){
        user=user.split(":")[1];
        user=user.substring(1,user.length()-2);
        String[] ipPorts = IdpServer.addrS;
        Integer serversNum = ipPorts.length;
        CountDownLatch latch = new CountDownLatch(ipPorts.length);
        System.out.println("logOut "+user);
        ConcurrentHashMap<String,String> failMap=new ConcurrentHashMap<>();
        for (String s:ipPorts) {
            service.submit(BroadCastMsg.builder().failsMap(failMap).mapper("logout").message(user).latch(latch).IpAndPort(s).build());
        }
        }
        byte[] respContents="logOutU".getBytes("UTF-8");;
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin",httpExchange.getRequestHeaders().getFirst("Origin"));
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods","POST");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers","content-type");
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
