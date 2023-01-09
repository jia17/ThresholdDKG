package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.util.TestConv;
import com.uestc.thresholddkg.Server.util.test2;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Random;

/**
 * @author zhangjia
 * @date 2023-01-02 16:23
 */
@Slf4j
public class TestHandle implements HttpHandler {

    private String addr;
    public TestHandle(String _addr){addr=_addr;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String testRes=addr+ new Random().toString()+"ccc";
        byte[] respContents = testRes.getBytes("UTF-8");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        //System.out.println(addr+"get"+tline);
        log.error(addr+"get"+tline);
        var convert=new test2();
        //test Json2obj
        /*TestConv res=(TestConv) convert.Json2obj(tline);
        System.out.println(res.getText()[0]);*/

        // 设置响应头
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        // 设置响应code和内容长度
        httpExchange.sendResponseHeaders(200, respContents.length);
        // 设置响应内容
        httpExchange.getResponseBody().write(respContents);

        // 关闭处理器, 同时将关闭请求和响应的输入输出流（如果还没关闭）
        httpExchange.close();
    }
}
