package com.uestc.thresholddkg.mapperTest;

import com.uestc.thresholddkg.Server.Config.RedisConfig;
import com.uestc.thresholddkg.Server.pojo.IdPwd;
import lombok.var;
import org.junit.jupiter.api.Test;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangjia
 * @date 2023-03-21 10:44
 */
public class testRedis {
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    RedissonClient redisson0;
    @Test
    void test() throws InterruptedException {
        var redisson= RedisConfig.getRedission();
       // redisTemplate=RedisConfig.getRedisTemplate();
        RReadWriteLock rwlock=redisson.getReadWriteLock("mylock");
        if(redisTemplate==null) System.out.println("NULLL");
        for(int i=0;i<3;i++){
            new Thread(()->{
                try {
                    Thread.sleep(20);
                    boolean lo=rwlock.writeLock().tryLock(8, TimeUnit.SECONDS);
                    if(lo)redisTemplate.opsForValue().set("334",new IdPwd("nqipwndqw","iwqpdnwqpd"));
                    else  System.out.println("Nowrite"+new Date().getTime());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    rwlock.writeLock().unlock();
                }
            }).start();
        }
        for(int i=0;i<7;i++){
            new Thread(()->{
                try {
                    Thread.sleep(20);
                    boolean lo=rwlock.readLock().tryLock(8, TimeUnit.SECONDS);
                    var t2=(IdPwd) redisTemplate.opsForValue().get("334");
                    System.out.println(t2.getId()+" "+new Date().getTime());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    rwlock.readLock().unlock();
                }
            }).start();
        }
        Thread.sleep(200);
        var t2=(IdPwd) redisTemplate.opsForValue().get("334");
        String a=(String) redisTemplate.opsForValue().get("ss");
        System.out.println();
    }
}
