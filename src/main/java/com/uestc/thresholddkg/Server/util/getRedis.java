package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.IdPwd;
import com.uestc.thresholddkg.Server.pojo.userMsgRedis;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.var;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangjia
 * @date 2023-03-21 12:25
 */
@Component
@Getter
@Setter
@NoArgsConstructor
public class getRedis {

    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    RedissonClient redisson0;

    private  static RedisTemplate<String,Object> redisTemplate1;
    private static RedissonClient redisson1;
    private static RReadWriteLock rwlock;
    @PostConstruct
    void get(){
        redisTemplate1=redisTemplate;
        redisson1=redisson0;
        rwlock=redisson1.getReadWriteLock("lock");
    }
    public static  RedisTemplate<String,Object> getTemplate(){
        return  redisTemplate1;
    }
    public static RedissonClient getRedisson(){
        return redisson1;
    }
    public static void writeUserMsg(String userId, userMsgRedis userMsgRedis){
        try {
            boolean res=rwlock.writeLock().tryLock(4,TimeUnit.SECONDS);
            if(res)redisTemplate1.opsForValue().set(userId,userMsgRedis,10,TimeUnit.MINUTES);
            else  System.out.println("Fail to Write Redis");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            rwlock.writeLock().unlock();
        }
    }
    public static userMsgRedis readUserMsg(String userId){
        userMsgRedis res=null;
        try {
            while(!rwlock.readLock().tryLock(2, TimeUnit.SECONDS))Thread.sleep(800);
            res=(userMsgRedis) redisTemplate1.opsForValue().get(userId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            rwlock.readLock().unlock();
        }
        return res;
    }
    public static void removeMsg(String userId){
        try {
            boolean res=rwlock.writeLock().tryLock(4,TimeUnit.SECONDS);
            if(res)redisTemplate1.delete(userId);
            else  System.out.println("Fail to Delete Redis");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            rwlock.writeLock().unlock();
        }
    }
}
