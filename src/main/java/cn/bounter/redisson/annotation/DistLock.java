package cn.bounter.redisson.annotation;

import java.lang.annotation.*;


/**
 * 自定义分布式锁注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistLock {

    /**
     * 自定义key
     * @return
     */
    String key() default "distlock";

    /**
     * 锁过期时间,单位：秒
     * @return
     */
    long timeout() default 10;
}
