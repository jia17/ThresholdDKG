package com.uestc.thresholddkg.Server.user.VueRest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * @author zhangjia
 * @date 2023-02-13 10:59
 */
public class Redict implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var res="redict";
        byte[] respContents = res.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin","http://127.0.0.1:8083");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods","*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        httpExchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers","content-type");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
