package com.uestc.thresholddkg.Server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.handle.ReStoreTest;
import com.uestc.thresholddkg.Server.handle.StartDKG;
import com.uestc.thresholddkg.Server.handle.TestHandle;
import com.uestc.thresholddkg.Server.util.TestConv;
import com.uestc.thresholddkg.Server.util.test2;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-02 09:48
 */
@Component
@Slf4j
public class IdpServer  implements ApplicationListener<ContextRefreshedEvent> {
    @Resource
    private  IpAndPort ipAndPort;
    private static String[] addrS;
    @Value("#{'${idpservers.ipandport.ServersIp}'.split(' ')}")
    private String[] configAddr;
    private static String[] IPs;
    private static Integer[] Ports;

    private static HttpsServer[] servers;

    @PostConstruct
    public void getAddr(){
        addrS=ipAndPort.getServersIp();//temp;
    }
    public static void InitIdp(){
        int serverNum=addrS.length;
//        var meg=new TestConv(new BigInteger("1313"),new BigInteger("999999999"),new String[]{"cc","xxxx"},1,false);
//        var convert=new test2();
//        var ss=convert.Obj2json(meg);
//        JSONObject jsonobject = JSONObject.fromObject(ss);
//        var s2=  JSONObject.toBean(jsonobject,TestConv.class);
//        TestConv res=(TestConv) convert.Json2obj(ss);
//        System.out.println(res.getText()[0]);
        IPs=new String[serverNum];
        Ports=new Integer[serverNum];
        String[] addrSplit;int i=0;
        for (i=0;i<serverNum;i++) {
            var addr=addrS[i];
            addrSplit=addr.split(":");
            IPs[i]=addrSplit[0];Ports[i]=Integer.valueOf(addrSplit[1]);
        }
          servers=new HttpsServer[serverNum];
        for(int j=0;j<serverNum;j++){
            try {
                servers[j]= HttpsServer.create(new InetSocketAddress(IPs[j],Ports[j]),0);
                KeyStore ks = KeyStore.getInstance("jks");   //建立证书库
                //System.out.println("1");
                ks.load(new FileInputStream("src/main/resources/serverCert/cert"+Integer.toString(j+1)+".jks"), "123456".toCharArray());  //载入证书
                //System.out.println("2");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());  //建立一个密钥管理工厂
                kmf.init(ks, "123456".toCharArray());  //初始工厂
                SSLContext sslContext = SSLContext.getInstance("SSL");  //建立证书实体
                System.out.println("3::server"+Integer.toString(j));
                sslContext.init(kmf.getKeyManagers(), null, null);   //初始化证书
                HttpsConfigurator httpsConfigurator = new HttpsConfigurator(sslContext);
                servers[j].setHttpsConfigurator(httpsConfigurator);
            } catch (Exception e) {
                e.printStackTrace();
            }
            StartDKG startDKG=new StartDKG();startDKG.setAddr(servers[j].getAddress().toString());
            servers[j].createContext("/startDkg",startDKG);
            ReStoreTest resto=new ReStoreTest();resto.setAddrSelf(servers[j].getAddress().toString());
            servers[j].createContext("/restoreTest",resto);
            servers[j].createContext("/test",new TestHandle(servers[j].getAddress().toString()));
            servers[j].setExecutor(null);
            servers[j].start();
        }
        System.out.println("startSe");
    }

    public static void main(String[] args){
        //InitIdp();
        System.out.println("s");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

    }
    @PreDestroy
    public void cleanup() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(9);
        CountDownLatch latch=new CountDownLatch(9);
        for (int i = 0; i < 9; i++) {
            int finalI = i;
            Runnable worker = new Runnable(){
                @Override
                public void run(){
                    servers[finalI].stop(2);latch.countDown();
                }
            };
            executor.execute(worker);
        }
        latch.await();
        executor.shutdown();
        log.warn("webSocketSinglePool destroyed.");
    }

}
