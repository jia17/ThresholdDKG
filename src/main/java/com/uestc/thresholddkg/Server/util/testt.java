package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.Config.RedisConfig;
import com.uestc.thresholddkg.Server.pojo.IdPwd;
import com.uestc.thresholddkg.Server.pojo.userMsgRedis;
import lombok.NoArgsConstructor;
import lombok.var;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangjia
 * @date 2023-03-21 12:23
 */
@NoArgsConstructor
@Component
public class testt {


    public void test() throws InterruptedException {
        var redisson= getRedis.getRedisson();
        var redisTemplate=getRedis.getTemplate();
        RReadWriteLock rwlock=redisson.getReadWriteLock("mylock");
        if(redisTemplate==null) System.out.println("NULLL");
        for(int i=0;i<3;i++){
            new Thread(()->{
                try {
                    Thread.sleep(20);
                    boolean lo=rwlock.writeLock().tryLock(8, TimeUnit.SECONDS);
                    if(true)redisTemplate.opsForValue().set("334",new IdPwd("nqipwndqw","iwqpdnwqpd"));
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
                    if(t2!=null)System.out.println(t2.getId()+" "+new Date().getTime());
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

    public void test2() {
        getRedis.writeUserMsg("qwer",new userMsgRedis("sss","sss","cc"));
        System.out.println(getRedis.readUserMsg("qwer").getMsgTime());
       // getRedis.removeMsg("qwer");
       // System.out.println(getRedis.readUserMsg("qwer"));
    }
}
