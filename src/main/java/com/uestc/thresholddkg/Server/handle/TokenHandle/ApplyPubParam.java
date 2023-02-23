package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.ServTokenKey;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServTokenKMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServTokenKWR;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.PubParamToken;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import lombok.AllArgsConstructor;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-02-04 16:58
 */
@AllArgsConstructor
public class ApplyPubParam implements HttpHandler {
    private IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String userId="";
        String line;
        while((line=reader.readLine())!=null){
            userId+=line;
        }
        ServTokenKMapper tokenKMapper= ServTokenKWR.getMapper();
        ServTokenKey tokenKey=tokenKMapper.selectById(idpServer.getServerId());
        if(tokenKey==null) {
            byte[] respContents = "str".getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(500, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();return;
        }
        ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
        ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById("userId");
        var dkg_sysStr=new DKG_SysStr(servPrfsPp.getP(),servPrfsPp.getQ(),
                servPrfsPp.getG(),servPrfsPp.getH());
        PubParamToken pubParamToken=PubParamToken.builder().userId(userId).y(tokenKey.getPubKey()).dkg_sysStr(dkg_sysStr).build();
        String str= Convert2StrToken.Obj2json(pubParamToken);
        byte[] respContents = str.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
