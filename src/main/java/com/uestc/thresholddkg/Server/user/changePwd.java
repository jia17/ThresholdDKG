package com.uestc.thresholddkg.Server.user;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.communicate.SendUriHttp;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.pojo.IdPwd;
import com.uestc.thresholddkg.Server.pojo.IdPwdChange;
import com.uestc.thresholddkg.Server.pojo.TokenUser;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;

/**
 * @author zhangjia
 * @date 2023-03-26 15:00
 */
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class changePwd implements HttpHandler {
    private ExecutorService service;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var req=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(req));
        String tline="";
        String line;
        String res="true";
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        if(!tline.equals("")){
            IdPwdChange idPwdChange= new Gson().fromJson(tline, IdPwdChange.class);
            IdPwd idPwd=new IdPwd(idPwdChange.getId(),idPwdChange.getPasswd());
            System.out.println(new Gson().toJson(idPwd,IdPwd.class));
            String resMsg=startToken.getToken(idPwd,service);
            TokenUser pwdCheck=(TokenUser) Convert2StrToken.Json2obj(resMsg, TokenUser.class);
            if(!pwdCheck.getY().equals("")) {
                StartPRF.VSS_Pwd(idPwdChange.getId(),idPwdChange.getNewPwd(),true,service);
                logOutU.SendLogout(idPwdChange.getId(),service);
            }else res="false";
        }
        byte[] respContents = res.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
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
