package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * Created with IntelliJ IDEA.
 *  Redis缓存封装类
 * @Author: 你的名字
 * @Date: 2023/05/15/21:37
 * @Description:
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    public <R,ID> R queryWithPassThrough(String keyPrefix
            , ID id
            , Class<R> type
            , Function<ID,R> dbFallback
            , Long time, TimeUnit unit){
        String key = keyPrefix + id;
        //从Redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在，返回店铺信息
            return JSONUtil.toBean(json, type);
        }
        //判断是否为空值,不为空值，则表明是空字符串，是写入redis中的数据
        if (json != null) {
            //返回错误信息
            return null;
        }
        //不存在 ，根据id查询数据库
        R r = dbFallback.apply(id);
        //不存在，返回错误
        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //存在，写入redis
        this.set(key,r,time,unit);
        //返回
        return r;

    }
    //自建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    //逻辑过期解决缓存击穿
    public  <R,ID> R  queryWithLogicalExpire(String keyPrefix
            ,ID id,Class<R> type
            ,Function<ID,R> dbFallback
            , Long time, TimeUnit unit){
        String key = CACHE_SHOP_KEY + id;
        //从Redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //存在，返回店铺信息
            return null;
        }
        //4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            //5.1未过期，直接返回店铺信息
            return  r;
        }
        //5.2已过期，需要缓存重建
        //6.缓存重建
        //6.1获取互斥锁
        String lockKey= LOCK_SHOP_KEY+id;
        boolean tryLock = tryLock(lockKey);
        //6.2判断是否获取锁成功
        if (tryLock) {
            //6.3成功，开启独立线程，实现缓存重建
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        //缓存重建
                        R r1 = dbFallback.apply(id);
                        this.setWithLogicalExpire(key,r1,time,unit);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        //释放锁
                        faLock(lockKey);
                    }
                });
        }
        //6.4返回过期的商铺信息
        return r;
    }
    //创建互斥锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        boolean aTrue = BooleanUtil.isTrue(flag);
        return aTrue;
    }
    //释放锁
    private void faLock(String key){
        stringRedisTemplate.delete(key);
    }
}
