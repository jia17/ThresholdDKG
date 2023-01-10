package com.uestc.thresholddkg.Server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.uestc.thresholddkg.Server.handle.*;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-02 09:48
 */
@Component
@Slf4j
@Setter
@Getter
public class IdpServer  implements ApplicationListener<ContextRefreshedEvent> {
    public static String[] addrS;
    public static Integer threshold;
    @Value("#{'${idpservers.ipandport.ServersIp}'.split(' ')}")
    public String[] configAddr;
    @Value("#{'${idpservers.threshold}'}")
    public Integer configThreshold;
    private  HttpsServer server=null;
    private Integer serverId;
    public Integer item;

    //DkgParam
    private Map<String,BigInteger[]> secretAndT;
    private Map<String,BigInteger[]> fValue;
    private Map<String,BigInteger[]> gValue;
    private Map<String,BigInteger[]>fParam;
    private Map<String,BigInteger[]> mulsGH;
    private Map<String, DKG_System> DkgParam;
    @PostConstruct
    public void getAddr(){
        addrS=configAddr;threshold=configThreshold;item=1;
    }
    public static IdpServer getIdpServer(int serverId,String ip,int port){
        int serverNum=addrS.length;
//        var meg=new TestConv(new BigInteger("1313"),new BigInteger("999999999"),new String[]{"cc","xxxx"},1,false);
//        var convert=new test2();
//        var ss=convert.Obj2json(meg);
//        JSONObject jsonobject = JSONObject.fromObject(ss);
//        var s2=  JSONObject.toBean(jsonobject,TestConv.class);
//        TestConv res=(TestConv) convert.Json2obj(ss);
//        System.out.println(res.getText()[0]);
        IdpServer idpServers=new IdpServer();
        idpServers.serverId=serverId;
        idpServers.server=null;
        try {
            idpServers.server= HttpsServer.create(new InetSocketAddress(ip,port),0);
            KeyStore ks = KeyStore.getInstance("jks");   //建立证书库
            ks.load(new FileInputStream("src/main/resources/serverCert/cert"+Integer.toString(serverId)+".jks"), "123456".toCharArray());  //载入证书
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());  //建立一个密钥管理工厂
            kmf.init(ks, "123456".toCharArray());  //初始工厂
            SSLContext sslContext = SSLContext.getInstance("SSL");  //建立证书实体
            sslContext.init(kmf.getKeyManagers(), null, null);   //初始化证书
            HttpsConfigurator httpsConfigurator = new HttpsConfigurator(sslContext);
            idpServers.server.setHttpsConfigurator(httpsConfigurator);
           } catch (Exception e) {
            e.printStackTrace();
        }

        idpServers.DkgParam=new HashMap<>();
        idpServers.fParam=new HashMap<>();
        idpServers.mulsGH=new HashMap<>();
        idpServers.gValue=new HashMap<>();
        idpServers.secretAndT=new HashMap<>();
        idpServers.fValue=new HashMap<>();
        idpServers.server.createContext("/startDkg",new StartDKG(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/initDkg",new InitDKG(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/restoreTest",new ReStoreTest(idpServers.server.getAddress().toString()));
        idpServers.server.createContext("/test",new TestHandle(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/verifyGH",new VerifyGH(idpServers));
        ExecutorService executor = Executors.newFixedThreadPool(addrS.length - 1);
        idpServers.server.setExecutor(executor);
        idpServers.server.start();
        System.out.println("startSe"+Integer.toString(serverId));
        return idpServers;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

    }
    @PreDestroy
    public void cleanup() throws InterruptedException {
       try {
           if(server!=null){server.stop(2);
           log.warn("webSocketSinglePool destroyed."+Integer.toString(serverId));}
       }catch (Exception e){e.printStackTrace();}
//        ExecutorService executor = Executors.newFixedThreadPool(9);
//        CountDownLatch latch=new CountDownLatch(9);
//        for (int i = 0; i < 9; i++) {
//            int finalI = i;
//            Runnable worker = new Runnable(){
//                @Override
//                public void run(){
//                    servers[finalI].stop(2);latch.countDown();
//                }
//            };
//            executor.execute(worker);
//        }
//        latch.await();
//        executor.shutdown();
    }

}
