package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: 你的名字
 * @Date: 2023/05/16/14:13
 * @Description:
 */
@Slf4j
@Component
public class RedisIdWorker {
    private static final Long BEGIN_TIME=1684195200L;
    private static final int COUNT_BITS=32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime nowTime = LocalDateTime.now();
        long second = nowTime.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = second - BEGIN_TIME;

        //2.生成序列号
        //2.1获取当前日期，精确到天
        String date = nowTime.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2自增长
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        //3.拼接并返回

        return timeStamp <<COUNT_BITS  | count;
    }

}
