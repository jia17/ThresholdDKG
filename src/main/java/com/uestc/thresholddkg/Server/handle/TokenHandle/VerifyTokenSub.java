package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.TestUserMsg;
import com.uestc.thresholddkg.Server.pojo.TokenUser;
import com.uestc.thresholddkg.Server.pojo.userMsgRedis;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import com.uestc.thresholddkg.Server.util.getRedis;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * @author zhangjia
 * @date 2023-02-12 13:45
 */
@AllArgsConstructor
public class VerifyTokenSub implements HttpHandler {
    IdpServer idpServer;


    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        String res=Convert2StrToken.Obj2json(new TokenUser("","","false"));
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        TokenUser tokenUser=new TokenUser();
        if(!mess.equals("")){
            String Sign=mess.split("tokenSub")[1];
            Sign=Sign.substring(3,Sign.length()-2);//cautious  for cookie
            Sign= StringEscapeUtils.unescapeJava(Sign);
            System.out.println("tokenSub"+Sign);
             tokenUser=(TokenUser) Convert2StrToken.Json2obj(Sign, TokenUser.class);
            String user=tokenUser.getUser();
            String resStr=Convert2StrToken.Obj2json(new TokenUser("","","false"));
            userMsgRedis userMsgRedis= getRedis.readUserMsg(user);
            if(userMsgRedis!=null){
            BigInteger MsgHash=new BigInteger(userMsgRedis.getMsgHash());
            try {
                resStr = DKG.AESdecrypt(tokenUser.getSign(),MsgHash.toString());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }}
            res=resStr;
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
