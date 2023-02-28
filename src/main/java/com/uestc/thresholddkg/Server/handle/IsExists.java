package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.AllArgsConstructor;

import java.io.IOException;

/**
 * @author zhangjia
 * @date 2023-02-27 10:27
 */
@AllArgsConstructor
public class IsExists implements HttpHandler {
    private String ipPort;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = ipPort.getBytes("UTF-8");
        System.out.println("EXIST "+ipPort);
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
