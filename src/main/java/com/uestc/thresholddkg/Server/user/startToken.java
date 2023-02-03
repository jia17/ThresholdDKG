package com.uestc.thresholddkg.Server.user;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author zhangjia
 * @date 2023-02-03 20:38
 */
@Slf4j
@AllArgsConstructor
public class startToken implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String user="alicess";

    }
}
