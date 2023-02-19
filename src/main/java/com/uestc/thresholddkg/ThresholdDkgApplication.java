package com.uestc.thresholddkg;

import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.util.TestDKG;
import lombok.var;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import javax.annotation.Resource;
import java.security.KeyStore;
import java.util.concurrent.*;

@SpringBootApplication
public class ThresholdDkgApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThresholdDkgApplication.class, args);
        initSys();
    }

    static void initSys(){
        String[] serverAddr=IdpServer.addrS;
        String[] addrSplit=new String[2];
        String ip;Integer port;
        IdpServer[] idpServers=new IdpServer[serverAddr.length];
        int i=0;
        ExecutorService service= new ThreadPoolExecutor(//Executors.newFixedThreadPool(20);
                                20, 30, 5,
                                 TimeUnit.SECONDS,
                                 new LinkedBlockingDeque<>(),
                                 Executors.defaultThreadFactory(),
                                 new ThreadPoolExecutor.AbortPolicy());
        for (String addr:serverAddr
        ) {
            addrSplit=addr.split(":");
            ip=addrSplit[0];port=Integer.valueOf(addrSplit[1]);
            idpServers[i]=IdpServer.getIdpServer(i+1,ip,port,service);
            i++;
        }
        var user=TestDKG.getUserServ();
        System.out.println("cc");
    }


}
