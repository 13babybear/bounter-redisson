package cn.bounter.redisson.aspect;

import cn.bounter.redisson.annotation.DistLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
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

        //解析key
        String key = distLock.key();
        if (key.contains("#")) {
            key = parseSpEL(key, point);
        }

        //获取分布式锁
        RLock lock = redissonClient.getLock(key);
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

    /**
     * 解析SpEL表达式
     * @param spELStr
     * @param joinPoint
     * @return
     */
    private String parseSpEL(String spELStr, ProceedingJoinPoint joinPoint) {
        //拆分SpEL与非SpEL字符串
        String prefix = null;
        if (spELStr.indexOf("#") > 0) {
            prefix = spELStr.substring(0, spELStr.indexOf("#"));
            spELStr = spELStr.substring(spELStr.indexOf("#"));
        }

        SpelExpressionParser parser = new SpelExpressionParser();
        DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
        // 通过joinPoint获取被注解方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        // 使用spring的DefaultParameterNameDiscoverer获取方法形参名数组
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        // 解析过后的Spring表达式对象
        Expression expression = parser.parseExpression(spELStr);
        // spring的表达式上下文对象
        EvaluationContext context = new StandardEvaluationContext();
        // 通过joinPoint获取被注解方法的形参
        Object[] args = joinPoint.getArgs();
        // 给上下文赋值
        for(int i = 0 ; i < args.length ; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        // 表达式从上下文中计算出实际参数值
        /*如:
            @annotation(key="#student.name")
             method(Student student)
             那么就可以解析出方法形参的某属性值，return “xiaoming”;
          */
        return prefix + expression.getValue(context).toString();
    }
}
