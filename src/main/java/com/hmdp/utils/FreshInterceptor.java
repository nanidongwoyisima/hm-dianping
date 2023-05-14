package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: 你的名字
 * @Date: 2023/05/14/22:19
 * @Description:
 */
public class FreshInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public FreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //拦截前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //从session中获取用户
//        HttpSession session = request.getSession();
//        Object user = session.getAttribute("user");
        // TODO 从请求头token中获取用户
        String token = request.getHeader("authorization");
        if (StringUtil.isNullOrEmpty(token)) {
            //没有 ，进行拦截
            return true;
        }
        String userKey = LOGIN_USER_KEY+token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);
        // TODO 基于token判断用户是否存在
        //判断用户是否存在
        if (userMap.isEmpty()) {
            //没有 ，进行拦截
            return true;
        }
//        //有，保存到ThreadLocal中
//        UserHolder.saveUser((UserDTO) user);
        // TODO 将查询到的hash对象转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //将userDTO存储RhreadLocal中
        UserHolder.saveUser(userDTO);
        // TODO 刷新token
        stringRedisTemplate.expire(userKey,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行 返回true
        return true;
    }

    //拦截后
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //删除数据
        UserHolder.removeUser();
    }
}
