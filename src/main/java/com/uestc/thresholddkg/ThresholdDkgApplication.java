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
        IdpServer.InitIdp();
        System.out.println("xxxxx");
    }

}
