package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author zhangjia
 * @date 2023-01-02 16:23
 */
@Slf4j
public class TestHandle implements HttpHandler {

    private String addr;
    private IdpServer idpServer;
    private static Set<String> TestSSet;
    public TestHandle(String _addr, IdpServer _idpServer){addr=_addr;idpServer=_idpServer;TestSSet=new HashSet<>();}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String testRes=addr+ new Random().toString()+"ccc";
        TestSSet.add(testRes);
        testRes=TestSSet.toString();
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        //System.out.println(addr+"get"+tline);
        log.error(httpExchange.getRemoteAddress().toString()+"get"+tline);
        //var convert=new test2();
        //test Json2obj
        /*TestConv res=(TestConv) convert.Json2obj(tline);
        System.out.println(res.getText()[0]);*/
        byte[] respContents = tline.getBytes("UTF-8");

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
