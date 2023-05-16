package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: 你的名字
 * @Date: 2023/05/16/21:41
 * @Description:
 */
public class SimpleRedisLock implements Ilock{
    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private StringRedisTemplate stringRedisTemplate;

    private static  final String KEY_LOCK="lock:";

    private static final String ID_LOCK= UUID.randomUUID().toString(true)+"-";


    @Override
    public boolean tryLock(long timoutSec) {
        //获取锁
        String threadId = ID_LOCK+Thread.currentThread().getId();
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(KEY_LOCK + name, threadId, timoutSec, TimeUnit.SECONDS);
        boolean equals = Boolean.TRUE.equals(aBoolean);
        return equals;
    }
    @Override
    public void unLock() {
        //释放锁
        //获取锁的标识
        String threadId = ID_LOCK + Thread.currentThread().getId();
        String id = stringRedisTemplate.opsForValue().get(KEY_LOCK + name);
        //判断锁是否一致，再删除锁
        if (threadId.equals(id)) {
            stringRedisTemplate.delete(KEY_LOCK + name);
        }
    }
}
