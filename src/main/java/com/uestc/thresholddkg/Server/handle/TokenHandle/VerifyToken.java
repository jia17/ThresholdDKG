package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang.StringEscapeUtils;
import sun.text.normalizer.ICUData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-02-11 16:22
 */
@Slf4j
@AllArgsConstructor
public class VerifyToken implements HttpHandler {
    IdpServer idpServer;

    @SneakyThrows
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        String res="false";
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        if(!mess.equals("")){
            String Sign=mess.split("token")[1];
            Sign=Sign.substring(3,Sign.length()-2);
            Sign=StringEscapeUtils.unescapeJava(Sign);
            System.out.println("Sign"+Sign);
            TokenUser tokenUser=(TokenUser) Convert2StrToken.Json2obj(Sign, TokenUser.class);
            String user=tokenUser.getUser();
            BigInteger y=new BigInteger(tokenUser.getY());
            DKG_System dkg_system=idpServer.getDkgParam().get(user);
            BigInteger Msg=new BigInteger(idpServer.getUserMsg().get(user));
            BigInteger MsgHash=new BigInteger(idpServer.getUserMsgHash().get(user));
            BigInteger p=dkg_system.getP();
            BigInteger q=dkg_system.getQ();
            BigInteger YW=y.modPow(MsgHash,p);
            YW=YW.multiply(new BigInteger(tokenUser.getSign()).modPow(BigInteger.valueOf(2),p)).mod(p);
            BigInteger msg=Msg.mod(q);
            BigInteger GkM=dkg_system.getG().modPow(msg.multiply(BigInteger.valueOf(IdpServer.threshold)).mod(p),p);
            if(GkM.equals(YW)){
                //return subToken for App
                var tokenSub= TestUserMsg.builder().userId(user).pwd(RandomGenerator.genarateRandom(512).toString()).build();
                var resStr= DKG.AESencrypt(Convert2StrToken.Obj2json(tokenSub),MsgHash.toString());
                var token=new TokenUser(user,resStr,"");
                res=Convert2StrToken.Obj2json(token);}
        }
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
