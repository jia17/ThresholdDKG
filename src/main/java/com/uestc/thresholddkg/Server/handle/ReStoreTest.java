package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BoradTest;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.*;

/**
 * @author zhangjia
 * @date 2023-01-03 13:25
 */
@Setter
@Slf4j
public class ReStoreTest implements HttpHandler {
    private  String[] ipAndPort;
    @PostConstruct
    public void getAddr(){

    }
    private String AddrSelf;
    public ReStoreTest(String addr){AddrSelf=addr;ipAndPort= IdpServer.addrS;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        log.warn(ipAndPort.toString());
        String[] ippStr=ipAndPort;
        int serversNum=ippStr.length;
        final ExecutorService executor = Executors.newFixedThreadPool(serversNum - 1);
        final CountDownLatch    latch  = new CountDownLatch(serversNum>>1+1);
        final AtomicInteger failureCounter = new AtomicInteger(0);
        final int maximumFailures = serversNum-(serversNum>>1+1);
        ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
        int serverId=0;
        for(String addr:ippStr){
            serverId++;
            if(("/"+addr).equals(AddrSelf))continue;
            SendUri send = SendUri.builder().message(Integer.toString(serverId)).mapper("test").IpAndPort(addr).build();
            //send.SendMsg();
            executor.submit(
                BoradTest.builder().latch(latch).message(Integer.toString(serverId)).mapper("test").IpAndPort(addr)
                        .failCount(failureCounter).maxFail(maximumFailures).resmap(resMap).build()
            );
        }
        try {
            latch.await();
            if(failureCounter.get()<=maximumFailures){
                byte[] respContents = "testRes".getBytes("UTF-8");

                resMap.forEach((key, value) -> System.out.println(key + "send" + value));
                executor.shutdown();

                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                httpExchange.sendResponseHeaders(200, respContents.length);
                httpExchange.getResponseBody().write(respContents);
                httpExchange.close();
            }else{
                executor.shutdown();
                throw new IOException();
            }
        }catch (InterruptedException e){
            throw new RuntimeException();
        }
    }
}
