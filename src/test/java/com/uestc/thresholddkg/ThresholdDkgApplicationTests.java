package com.uestc.thresholddkg;

import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.var;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class ThresholdDkgApplicationTests {

    @Test
    void contextLoads() throws InterruptedException {
        String a=RandomGenerator.genarateRandom(1024).toString();
        var millisecond = new Date().getTime();
        String b=a+":"+millisecond;
        System.out.println(b.split(":")[1]);
        Thread.sleep(1000);
        System.out.println(new Date().getTime());
    }

}
