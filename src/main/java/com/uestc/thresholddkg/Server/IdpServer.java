package com.uestc.thresholddkg.Server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.uestc.thresholddkg.Server.DkgCommunicate.TokenComm.InvalidFexpBroad;
import com.uestc.thresholddkg.Server.handle.*;
import com.uestc.thresholddkg.Server.handle.PrfsHandle.verifyPrfI;
import com.uestc.thresholddkg.Server.handle.TokenHandle.*;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

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
    public static ConcurrentMap<String,Integer> addr2Index;
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
    private Map<String,Map<String,BigInteger>> fgRecv;//QUAL fvalue
    private Map<String, Set<String>>fgRecvFalse;
    private Map<String, ConcurrentMap<String,Integer>> fgRecvFTimes;
    private Map<String,Integer> flag;//0 wait fg;1 complain;2 success QUAL;4 get pubKey

    private Map<String,String> PrfVerify;
    private Map<String,String> PrfHi;
    //pubKey
    private Set<String> pubId;
    private Map<String,Map<String,BigInteger>> fExpRecv;//g^ai
    private Map<String,Set<String>> fExpFalse;

    private Map<String,String> userMsg;
    private Map<String,String> userMsgHash;
    private Map<String,String> userY;

    private ExecutorService service;
    @PostConstruct
    public void getAddr(){
        addrS=configAddr;threshold=configThreshold;item=1;
        addr2Index=new ConcurrentHashMap<>();
        for(int i=0;i<configAddr.length;i++){addr2Index.put(configAddr[i],i+1);}
    }
    public static IdpServer getIdpServer(int serverId,String ip,int port,ExecutorService executor){
        int serverNum=addrS.length;
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
        idpServers.service= new ThreadPoolExecutor(
                14, 20, 5,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        idpServers.DkgParam=new HashMap<>();
        idpServers.fParam=new HashMap<>();
        idpServers.mulsGH=new HashMap<>();
        idpServers.gValue=new HashMap<>();
        idpServers.secretAndT=new HashMap<>();
        idpServers.fValue=new HashMap<>();
        idpServers.fgRecvFalse=new HashMap<>();
        idpServers.fgRecv=new HashMap<>();
        idpServers.flag=new HashMap<>();
        idpServers.fgRecvFTimes=new HashMap<>();
        idpServers.PrfHi=new HashMap<>();
        idpServers.PrfVerify=new HashMap<>();
        idpServers.pubId=new HashSet<>();
        idpServers.userMsg=new HashMap<>();idpServers.userMsgHash=new HashMap<>();idpServers.userY=new HashMap<>();
        idpServers.fExpFalse=new HashMap<>();idpServers.fExpRecv=new HashMap<>();
        idpServers.server.createContext("/startDkg",new StartDKG(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/initDkg",new InitDKG(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/restoreTest",new ReStoreTest(idpServers,idpServers.server.getAddress().toString()));
        idpServers.server.createContext("/test",new TestHandle(idpServers.server.getAddress().toString(),idpServers));
        Map<String, ConcurrentSkipListSet<String>> RecvdInvMap=new HashMap<>();
        idpServers.server.createContext("/verifyGH",new VerifyGH(idpServers,RecvdInvMap));
        idpServers.server.createContext("/collComplain",new CollectCompl(idpServers,RecvdInvMap));
        idpServers.server.createContext("/pushFval2",new ReSendF(idpServers));
        idpServers.server.createContext("/invalidAddr",new GetInvalid(idpServers,RecvdInvMap));
        idpServers.server.createContext("/applyTestRestore",new ApplyFiTest(idpServers));
        //idpServers.server.createContext("/startPrf",new StartPRF(idpServers));
        idpServers.server.createContext("/getPrfs",new GetPrfs(idpServers));
        idpServers.server.createContext("/startDkgT",new StartDkgToken(idpServers.server.getAddress().toString(),idpServers));
        idpServers.server.createContext("/verifyFExp",new VerifyFExp(idpServers));
        idpServers.server.createContext("/resendFExp",new ReSendFExp(idpServers));
        idpServers.server.createContext("/invalidAddrFExp",new GetInvalidFExp(idpServers));
        idpServers.server.createContext("/getPubParam",new ApplyPubParam(idpServers));
        idpServers.server.createContext("/sendTokenI",new ApplyTokenI(idpServers));
        idpServers.server.createContext("/verifyToken",new VerifyToken(idpServers));
        idpServers.server.createContext("/getMsg",new getMsg(idpServers));
        idpServers.server.createContext("/verifyTokenSub",new VerifyTokenSub(idpServers));
        idpServers.server.createContext("/verifyGetPrfI",new verifyPrfI(idpServers));
        idpServers.server.createContext("/isExist",new IsExists(ip+":"+port));
        idpServers.server.createContext("/logout",new LoginOut(idpServers));
        idpServers.server.createContext("/showprf",new TestPrfsShow());
        idpServers.server.createContext("/TestPrfsApply",new TestPrfsApply(idpServers));
        idpServers.server.createContext("/showtoken",new TestTokenShow(idpServers));
        idpServers.server.createContext("/TestTokenApply",new TestTokenApply(idpServers));
        //ExecutorService executor = Executors.newFixedThreadPool(addrS.length - 1);//cautious
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
    }

}
