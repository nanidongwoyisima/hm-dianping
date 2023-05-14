package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
        //缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(
                CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

//        //逻辑过期解决缓存击穿
//        Shop shop= cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("商铺信息不存在");
        }
        //返回
        return Result.ok(shop);
    }

//    //自建线程池
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
//
//    //逻辑过期解决缓存击穿
//    public  Shop  queryWithLogicalExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //从Redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //存在，返回店铺信息
//            return null;
//        }
//        //4.命中，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //5.判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())){
//            //5.1未过期，直接返回店铺信息
//            return  shop;
//        }
//        //5.2已过期，需要缓存重建
//        //6.缓存重建
//        //6.1获取互斥锁
//        String lockKey= LOCK_SHOP_KEY+id;
//        boolean tryLock = tryLock(lockKey);
//        //6.2判断是否获取锁成功
//        if (tryLock) {
//            //6.3成功，开启独立线程，实现缓存重建
//            if (expireTime.isAfter(LocalDateTime.now())) {
//                CACHE_REBUILD_EXECUTOR.submit(() -> {
//                    try {
//                        //缓存重建
//                        this.savaShop2Redis(id, 30L);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    } finally {
//                        //释放锁
//                        faLock(lockKey);
//                    }
//                });
//            }
//        }
//
//        //6.4返回过期的商铺信息
//        return shop;
//    }
    //缓存穿透
//    public  Shop queryWithPassThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //从Redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在，返回店铺信息
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断是否为空值,不为空值，则表明是空字符串，是写入redis中的数据
//        if (shopJson != null) {
//            return null;
//        }
//        //不存在 ，根据id查询数据库
//        Shop shop = getById(id);
//        //不存在，返回错误
//        if (shop == null) {
//            //将空值写入redis
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            //返回错误信息
//            return null;
//        }
//        //存在，写入redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //返回
//        return shop;
//
//    }
        //互斥锁解决缓存击穿
//    public  Shop queryWithMutex(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //从Redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在，返回店铺信息
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断是否为空值,不为空值，则表明是空字符串，是写入redis中的数据
//        if (shopJson != null) {
//            return null;
//        }
//        //获取互斥锁
//        String lockKey="lock:key"+id;
//        Shop shop = null;
//        try {
//            boolean tryLock = tryLock(lockKey);
//            //判断是否获取成功
//            if (!tryLock) {
//                //失败，则休眠并重试
//                Thread.sleep(200);
//                return  queryWithMutex(id);
//            }//成功 ，根据id查询数据库
//                if (StrUtil.isNotBlank(shopJson)) {
//                    //存在，返回店铺信息
//                    return JSONUtil.toBean(shopJson, Shop.class);
//            }
//            shop = getById(id);
//            //模拟重建的延时
//            Thread.sleep(1000);
//            //不存在，返回错误
//            if (shop == null) {
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                //返回错误信息
//                return null;
//            }
//            //存在，写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            //释放互斥锁
//            faLock(lockKey);
//        }
//        //返回
//        return shop;
//
//    }
//    public void savaShop2Redis(Long id,Long Seconds){
//        //查询店铺数据
//        Shop shop = getById(id);
//        //封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(Seconds));
//        //将逻辑过期时间存入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY
//        +id,JSONUtil.toJsonStr(redisData));
//    }
//    //创建互斥锁
//    private boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        boolean aTrue = BooleanUtil.isTrue(flag);
//        return aTrue;
//    }
//    //释放锁
//    private void faLock(String key){
//        stringRedisTemplate.delete(key);
//    }
    @Override
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("商铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除redis缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return Result.ok();
    }
}
