package com.hmdp.utils;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: 你的名字
 * @Date: 2023/05/16/21:40
 * @Description:
 */
public interface Ilock {
    //尝试获取锁

    boolean tryLock(long timoutSec);


    //释放锁

    void    unLock();
}
