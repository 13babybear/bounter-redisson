package cn.bounter.redisson.aspect;

import cn.bounter.redisson.annotation.DistLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 自定义分布式锁切面解析器
 */
@Aspect
@Component
public class DistLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(distLock)")
    public Object lock(ProceedingJoinPoint point, DistLock distLock) throws Throwable {
        Object retVal = null;

        //获取分布式锁
        RLock lock = redissonClient.getLock(distLock.key());
        // 尝试加锁，最多等待0秒，上锁以后10秒自动解锁
        boolean res = lock.tryLock(0, distLock.timeout(), TimeUnit.SECONDS);
        if (res) {
            try {
                retVal = point.proceed();
            } finally {
                lock.unlock();
            }
        }

        return retVal;
    }
}
