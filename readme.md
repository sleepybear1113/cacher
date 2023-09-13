# 简介
一个简单的缓存工具。

使用 ConcurrentHashMap 实现缓存存取，可以设置创建、访问、修改后若干时间过期。过期时间可以根据每个 key-value 动态设定。
# 构建
基于 Java 17 构建
# 使用
Maven 依赖导入
```xml
<dependency>
    <groupId>cn.sleepybear</groupId>
    <artifactId>cacher</artifactId>
    <version>1.0.4</version>
</dependency>
```
Java 使用
```java
public class CacherTest {
    public void test() {
        // 构建 CacherBuilder，填充相关参数
        CacherBuilder<Integer, String> cacherBuilder = new CacherBuilder<Integer, String>()
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
        String remove = cacher.remove(222);
        // 获取 key = 111 的缓存，过期时间仍为创建开始后 5000 毫秒
        String s1 = cacher.get(111);
        // 获取 key = 333 的缓存，过期时间刷新，从当前开始计时后 3500 毫秒
        String s2 = cacher.get(333);
    }
}
```
如果需要缓存失效的时候重新加载，那么可以如下操作
```java
public class CacherTest {
    public void test() {
        CacherBuilder<Integer, String> cacherBuilder1 = new CacherBuilder<Integer, String>()
                // 设置 loader，过期时间 10000 毫秒，过期后调用 get(key) 刷新缓存
                .cacherLoader(10000L, key -> get(key));

        CacherBuilder<Integer, String> cacherBuilder2 = new CacherBuilder<Integer, String>()
                // 设置 loader，过期时间调用 getKeyExpireTime 方法，过期后调用 get(key) 刷新缓存
                .cacherLoader(key -> getKeyExpireTime(key), key -> get(key));
    }
}
```
# 更新日志
## v1.0.5
- (新增) 允许 put key 为 null。
## v1.0.4
- (优化) 优化 ExpireAction 类。
## v1.0.0
- (更改) 更改包名。
- (新增) 添加部分注释。
- (新增) 新增过期后的操作 ExpireAction 类。
- (升级) 升级 Maven 依赖。
## v0.1.1
- (优化) 更改变量包装类型。
## v0.1.0
- (新增) 缓存过期后的重载 load 方法，使用函数式接口设计。
- (新增) 2 个函数式 load 接口，一个用来获取过期时间，一个用来获取 value。
- (新增) 相关日志、信息输出。