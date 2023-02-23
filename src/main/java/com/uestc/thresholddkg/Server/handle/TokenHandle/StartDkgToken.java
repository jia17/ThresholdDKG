package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.DkgCommunicate.GenarateFuncBroad;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServTokenKMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServTokenKWR;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
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
 * @date 2023-02-03 20:45
 */
@Slf4j
@Setter
public class StartDkgToken implements HttpHandler {
    private  String[] ipAndPort;
    private String addr;
    private IdpServer idpServer;
    public static String testString;
    private boolean isPubKey;
    @PostConstruct
    public void getAddr(){

    }
    public StartDkgToken(String addr,IdpServer idpServer){this.addr=addr;ipAndPort=IdpServer.addrS;this.idpServer=idpServer;}
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        /*String user="Bob"+ RandomGenerator.genarateRandom(BigInteger.valueOf(100000)).toString(),passwd="";
        testString=user;
        idpServer.getPubId().add(user);*/

        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        DKG_SysStr dkg_sysStr=null;
        ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
        ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById("userId");//userId for System Token PP;cautious
        if(servPrfsPp!=null){
            dkg_sysStr=new DKG_SysStr(servPrfsPp.getP(),servPrfsPp.getQ(),
                                      servPrfsPp.getG(),servPrfsPp.getH());
        }else{
        log.error("START DKG Token"+mess);
        String[] IdPass=mess.split("\\|");
        //*******cautious******prfs
        String user=IdPass[0],passwd="";//IdPass[1];
        idpServer.getPubId().add(user);

        int serversNum=ipAndPort.length;
        DKG_System param0=new DKG_System();
        DKG_SysStr dkg_sysStr0=new DKG_SysStr();
        param0= DKG.initDLog();//DKG.init();//DKG.initRSA();//
        dkg_sysStr0=new DKG_SysStr(param0.getP().toString(),param0.getQ().toString(),param0.getG().toString(),param0.getH().toString());
        //写入系统DKGSys
        servPrfsPPMapper.insert(ServPrfsPp.builder().userId("userId").p(dkg_sysStr0.getP())
                                          .q(dkg_sysStr0.getQ()).g(dkg_sysStr0.getG()).h(dkg_sysStr0.getH()).build());
        DKG_System param=param0;DKG_SysStr dkg_sysStr1=dkg_sysStr0;dkg_sysStr=dkg_sysStr0;
        Thread t=new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutorService service = idpServer.getService();//Executors.newFixedThreadPool(serversNum - 1);
                CountDownLatch latch=new CountDownLatch(serversNum-1);
                ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
                BigInteger[] secrets=new BigInteger[2];
                secrets[0]= RandomGenerator.genaratePositiveRandom(param.getP());
                secrets[1]=RandomGenerator.genaratePositiveRandom(param.getP());
                idpServer.getSecretAndT().put(user,secrets);
                DkgSysMsg message=new DkgSysMsg(dkg_sysStr1,user,user,idpServer.item);//cautious user as "passwd"
                idpServer.getDkgParam().put(user,param);
                idpServer.getFlag().put(user,0);
                idpServer.getFgRecv().put(user,new HashMap<>());
                idpServer.getFgRecvFalse().put(user,new HashSet<>());
                idpServer.getFgRecvFTimes().put(user,new ConcurrentHashMap<>());
                if(!idpServer.getFExpRecv().containsKey(user)){
                    idpServer.getFExpRecv().put(user,new HashMap<>());
                    idpServer.getFExpFalse().put(user,new HashSet<>());}
                DKG.initMapTimes(idpServer.getFgRecvFTimes().get(user));
                var convert=new Convert2Str();
                for (String s : ipAndPort) {
                    //log.warn("send"+s);
                    if (("/" + s).equals(addr)) continue;
                    service.submit(
                            BroadCastMsg.builder().latch(latch).message(convert.Obj2json(message)).mapper("initDkg").IpAndPort(s)
                                    .failsMap(resMap).build()
                    );
                }
                try {
                    latch.await();
                    if(!resMap.isEmpty()){
                        resMap.forEach((key, value) ->service.submit(
                                BroadCastMsg.builder().latch(latch).message(convert.Obj2json(message)).mapper("initDkg").IpAndPort(key)
                                        .failsMap(resMap).build()));
                        Thread.sleep(500);
                    }
                    //broad cast
                    Thread generateFG=new Thread(new GenarateFuncBroad(idpServer,user,ipAndPort,idpServer.getServer().getAddress().toString()));
                    generateFG.start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //service.shutdown();
                }
        });
        t.start();
        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        try {
            var respContents= (Convert2Str.Obj2json(dkg_sysStr)).getBytes();
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        httpExchange.close();
        // 关闭处理器, 同时将关闭请求和响应的输入输出流（如果还没关闭）
    }
}
