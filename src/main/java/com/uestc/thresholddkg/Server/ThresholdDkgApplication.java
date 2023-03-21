package com.uestc.thresholddkg.Server;

import com.uestc.thresholddkg.Server.Config.RedisConfig;
import com.uestc.thresholddkg.Server.pojo.IdPwd;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.testt;
import lombok.var;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;


import java.util.Date;
import java.util.concurrent.*;

@SpringBootApplication
public class ThresholdDkgApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThresholdDkgApplication.class, args);
        initSys();
    }

    static void initSys()  {
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
        var user= DKG.getUserServ();
        System.out.println("cc");
        /*try {
            new testt().test();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }
}
