package cn.xjx.cacher;

import cn.xjx.cacher.cache.ExpireWayEnum;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2022/05/04 20:28
 */

public class CacherTest {
    private static final Logger log = LoggerFactory.getLogger(CacherTest.class);

    @Test
    public void test() {
        // 构建 CacherBuilder，填充相关参数
        CacherBuilder cacherBuilder = new CacherBuilder()
                // 每隔 10 秒扫一遍 Map 清理过期
                .delay(10, TimeUnit.SECONDS)
                // 运行时展示清理日志
                .showAllLogs();
        // 生成 Cacher 对象
        Cacher<Integer, String> cacher = cacherBuilder.build();
        // 无过期的缓存
        cacher.put(111, "www");
        // 过期时间 3000 毫秒的缓存
        cacher.put(222, "eee", 3000L);
        // 过期时间 3500 毫秒的缓存，每次访问则过期时间都往后延 3500 毫秒
        cacher.put(333, "full", 3500L, ExpireWayEnum.AFTER_ACCESS);
        // 缓存修改为 aaa，过期时间修改为 5000 毫秒
        cacher.set(111, "aaa", 5000L);
        // 删除 key = 222 的缓存
        cacher.remove(222);
        // 获取 key = 111 的缓存，过期时间仍为创建开始后 5000 毫秒
        String s1 = cacher.get(111);
        // 获取 key = 333 的缓存，过期时间刷新，从当前开始计时后 3500 毫秒
        String s2 = cacher.get(333);
    }
}
