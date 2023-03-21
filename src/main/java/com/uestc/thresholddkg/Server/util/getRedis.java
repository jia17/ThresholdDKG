package com.uestc.thresholddkg.Server.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
    static RedissonClient redisson1;
    @PostConstruct
    void get(){
        redisTemplate1=redisTemplate;
        redisson1=redisson0;
    }
    public static  RedisTemplate<String,Object> getTemplate(){
        return  redisTemplate1;
    }
    public static RedissonClient getRedisson(){
        return redisson1;
    }
}
