package cn.bounter.redisson;

import cn.bounter.redisson.annotation.DistLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/bounter")
public class BounterController {

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/lock")
    @DistLock(key = "distlock:#key", timeout = 2)
    public void lock(String key) throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + "获取到锁" + key + new Date());
        Thread.sleep(1000);
    }
}
