package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.util.FuncGH2Obj;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.*;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-10 15:52
 */
@Slf4j
public class VerifyGH implements HttpHandler {
    private IdpServer idpServer;
    public VerifyGH(IdpServer _idp){idpServer=_idp;}

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "VerifySuccess".getBytes("UTF-8");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        log.error(tline);
        FuncGH2Obj convert=new FuncGH2Obj();
        FunctionGHvals ghVals=(FunctionGHvals) convert.Json2obj(tline);
  //      String userId=ghVals.getUserId();
 //       DKG_System dkgSys=idpServer.getDkgParam().get(userId);
//        BigInteger p=dkgSys.getP();
//        BigInteger verify1=((Calculate.modPow(dkgSys.getG(),ghVals.getFi(),p)).multiply(Calculate.modPow(dkgSys.getH(),ghVals.getGi(),p))).mod(p);
//        boolean satisfy=DKG.VerifyGH(verify1,ghVals.getGMulsH(),ghVals.getServerId(),p);
//        if(satisfy){
//            //写入server
//            log.error("Server"+ghVals.getServerId()+"verify true "+httpExchange.getLocalAddress().toString());
//        }else{
//            log.error("Server"+ghVals.getServerId()+"verify false "+httpExchange.getLocalAddress().toString());
//        }
                    log.error("Server"+idpServer.getServer().getAddress().toString()+"verify true "+httpExchange.getLocalAddress().toString());

        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
