package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠卷
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //时间在秒杀之前，还未开始
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            //时间在秒杀结束之后。秒杀结束了
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            //小于1，表示库存不足
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
//        //加锁
//        synchronized (userId.toString().intern()) {
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        boolean tryLock = simpleRedisLock.tryLock(100);
        if (!tryLock) {
            return Result.fail("一个人只允许下一单");
        }
        //获取事物代理对象
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            simpleRedisLock.unLock();
        }
//        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //5.一人一单
        Long userId = UserHolder.getUser().getId();
        //5.1查询订单
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //5.2判断订单是否存在
        if (count>0) {
            return Result.fail("该用户已经下过订单");
        }
        //6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).gt("stock",0)
                .update();
        if (!success) {
            return  Result.fail("库存不足");
        }
        //7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //7.1订单id
        Long orderId = redisIdWorker.nextId("order:");
        voucherOrder.setId(orderId);
        //7.2用户id
        voucherOrder.setUserId(userId);
        //7.3代金卷id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //8.返回订单id
        return Result.ok(orderId);
    }
}
