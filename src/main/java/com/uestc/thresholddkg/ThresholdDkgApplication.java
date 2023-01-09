package com.uestc.thresholddkg;

import com.uestc.thresholddkg.Server.Config.IpAndPort;
import com.uestc.thresholddkg.Server.IdpServer;
import lombok.var;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import javax.annotation.Resource;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class ThresholdDkgApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThresholdDkgApplication.class, args);
        CountDownLatch a=new CountDownLatch(2);a.countDown();a.countDown();
        String[] serverAddr=IdpServer.addrS;
        String[] addrSplit=new String[2];
        String ip;Integer port;
        IdpServer[] idpServers=new IdpServer[serverAddr.length];
        int i=0;
        for (String addr:serverAddr
             ) {
                addrSplit=addr.split(":");
                ip=addrSplit[0];port=Integer.valueOf(addrSplit[1]);
                idpServers[i]=IdpServer.getIdpServer(i+1,ip,port);
                i++;
        }
        System.out.println("cc");
    }

}
