package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.TestUserMsg;
import com.uestc.thresholddkg.Server.pojo.TokenUser;
import com.uestc.thresholddkg.Server.pojo.userMsgRedis;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import com.uestc.thresholddkg.Server.util.getRedis;
import lombok.AllArgsConstructor;
import lombok.var;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Date;

/**
 * @author zhangjia
 * @date 2023-03-20 20:22
 */
@AllArgsConstructor
public class keepAlive implements HttpHandler {
    private IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        String res= "keepAlive";
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        System.out.println(mess);
        if(!mess.equals("")){
            String tempS=mess.split(":")[1];
            String user=tempS.substring(1,tempS.length()-2);
            userMsgRedis userMsgRedis= getRedis.readUserMsg(user);
            String timeOld=userMsgRedis.getMsgTime();
            if(timeOld!=null){
                long time=Long.parseLong(timeOld);
                userMsgRedis.setMsgTime(String.valueOf(time+1000*600));
                getRedis.writeUserMsg(user,userMsgRedis);
                System.out.println("keepAlive"+user);
            }
        }
        byte[] respContents = res.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin",httpExchange.getRequestHeaders().getFirst("Origin"));
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods","*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        httpExchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers","content-type");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
