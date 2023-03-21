package com.uestc.thresholddkg.Server.Config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

/**
 * @author zhangjia
 * @date 2023-03-20 23:31
 */
@Configuration
public class RedisConfig {
    public static RedissonClient redissonClient;
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化Redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        serializer.setObjectMapper(objectMapper);
        template.setValueSerializer(serializer);
        // 使用StringRedisSerializer来序列化和反序列化Redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redsession(){
        Config config=new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
    //@PostConstruct
    public static RedissonClient getRedission(){
        Config config=new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient=Redisson.create(config);
        return Redisson.create(config);
    }
    //@PostConstruct
    public static RedisTemplate<String,Object> getRedisTemplate(){
        JedisPoolConfig poolConfig =  new JedisPoolConfig();poolConfig.setMaxIdle(10);
        // 最大连接数
        poolConfig.setMaxTotal(25);
        JedisConnectionFactory jedisConnectionFactory=new JedisConnectionFactory( poolConfig);
        RedisStandaloneConfiguration rsConfig = jedisConnectionFactory.getStandaloneConfiguration();
        rsConfig.setHostName("127.0.0.1");
        rsConfig.setPort(6379);
        rsConfig.setPassword("");
        rsConfig.setDatabase(0);
        RedisConnectionFactory factory = jedisConnectionFactory;
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化Redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        serializer.setObjectMapper(objectMapper);
        template.setValueSerializer(serializer);
        // 使用StringRedisSerializer来序列化和反序列化Redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        //template.afterPropertiesSet();
        return template;
    }
}
