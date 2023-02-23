package com.uestc.thresholddkg.Server.handle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsWR;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-02 10:17
 */
@Setter
@Slf4j
public class StartDKG implements HttpHandler {
    private  String[] ipAndPort;
    private String addr;
    private IdpServer idpServer;
    public static String testString;
    private boolean isPubKey;
    @PostConstruct
    public void getAddr(){

    }
    public StartDKG(String addr,IdpServer idpServer){this.addr=addr;ipAndPort=IdpServer.addrS;this.idpServer=idpServer;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        //testString="tom"+RandomGenerator.genarateRandom(BigInteger.valueOf(100000)).toString();//***for test****//
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        log.error("START DKG "+mess);
        String[] IdPass=mess.split("\\|");
        //*******cautious******prfs
        String user=IdPass[0],passwd="";//IdPass[1];
        int serversNum=ipAndPort.length;
        ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
        DKG_System param0=new DKG_System();DKG_SysStr dkg_sysStr0=new DKG_SysStr();
        ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById(user);
        if(servPrfsPp==null){
            log.error("user don't register,without prfs_pp");
        }else{
            dkg_sysStr0=new DKG_SysStr(servPrfsPp.getP(),servPrfsPp.getQ(),servPrfsPp.getG(),servPrfsPp.getH());
        }
        DKG_SysStr dkg_sysStr=dkg_sysStr0;
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        try {
            var respContents=  Convert2Str.Obj2json(dkg_sysStr).getBytes();
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            } catch (IOException e) {
                 throw new RuntimeException(e);
        }
        httpExchange.close();
        // 关闭处理器, 同时将关闭请求和响应的输入输出流（如果还没关闭）
    }
}
