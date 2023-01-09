package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.util.TestConv;
import com.uestc.thresholddkg.Server.util.test2;
import lombok.Setter;
import lombok.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-02 10:17
 */
@Component
@Setter
public class StartDKG implements HttpHandler {
    @Value("#{'${idpservers.ipandport.ServersIp}'.split(' ')}")
    private String[] temp;
    private static String[] ipAndPort;
    private String addr;
    //public StartDKG(String _addr){addr=_addr;}
    @PostConstruct
    public void getAddr(){
        ipAndPort=temp;
    }
    @Override
    public void handle(HttpExchange httpExchange){
        int serversNum=ipAndPort.length;//????
        ExecutorService service= Executors.newFixedThreadPool(serversNum-1);
        CountDownLatch countDownLatch=new CountDownLatch(serversNum>>1+1);
        for (String s : ipAndPort) {
            if (("/" + s).equals(addr)) continue;
            var meg=new TestConv(new BigInteger("1313"),new BigInteger("999999999"),new String[]{"cccc","xxxx"},1,false);
            var convert=new test2();
            SendUri send = SendUri.builder().message(convert.Obj2json(meg)).mapper("test").IpAndPort(s).build();
            //send.SendMsg();
            service.submit(send::SendMsg);
        }
        var respContents=  "success".getBytes();
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        // 设置响应code和内容长度
        try {
            httpExchange.sendResponseHeaders(200, respContents.length);
            //  得到返回的数据

        // 设置响应内容
        httpExchange.getResponseBody().write(respContents);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
        // 关闭处理器, 同时将关闭请求和响应的输入输出流（如果还没关闭）
        httpExchange.close();
    }
}
