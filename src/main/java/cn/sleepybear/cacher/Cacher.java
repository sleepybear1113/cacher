package cn.sleepybear.cacher;

import cn.sleepybear.cacher.cache.CacheObject;
import cn.sleepybear.cacher.cache.ExpireWayEnum;
import cn.sleepybear.cacher.loader.CacherValueLoader;
import cn.sleepybear.cacher.loader.ExpireAction;
import cn.sleepybear.cacher.loader.ExpireTimeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2022/05/04 20:28
 */
public class Cacher<K, V> implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Cacher.class);

    @Serial
    private static final long serialVersionUID = -8803248750867836882L;

    private K nullKey;

    private final Map<K, CacheObject<V>> MAP;

    private ScheduledExecutorService scheduledExecutorService;

    private ExpireWayEnum expireWayEnum;

    /**
     * 旧缓存是否保持旧的过期方式
     */
    private boolean keepOldExpireWay;

    private int corePoolSize;
    private String scheduleName;
    private long initialDelay;
    private long delay;
    private TimeUnit timeUnit;
    private boolean fixRate;

    private boolean showExpireTimeLog;
    private boolean showRemoveInfoLog;
    private boolean showLoadInfoLog;

    private CacherValueLoader<K, V> cacherValueLoader;

    private ExpireTimeLoader<K> expireTimeLoader;

    private ExpireAction<K, CacheObject<V>> expireAction;

    public Cacher(ExpireWayEnum expireWayEnum, boolean keepOldExpireWay, int corePoolSize, String scheduleName, long initialDelay, long delay, TimeUnit timeUnit, boolean fixRate, int initialCapacity, float loadFactor, K nullKey, boolean showExpireTimeLog, boolean showRemoveInfoLog, boolean showLoadInfoLog, CacherValueLoader<K, V> cacherValueLoader, ExpireTimeLoader<K> expireTimeLoader, ExpireAction<K, CacheObject<V>> expireAction) {
        this.expireWayEnum = expireWayEnum;
        this.keepOldExpireWay = keepOldExpireWay;
        this.showExpireTimeLog = showExpireTimeLog;
        this.showRemoveInfoLog = showRemoveInfoLog;
        this.showLoadInfoLog = showLoadInfoLog;
        this.cacherValueLoader = cacherValueLoader;
        this.expireTimeLoader = expireTimeLoader;
        this.expireAction = expireAction;
        this.nullKey = nullKey;
        MAP = new ConcurrentHashMap<>(initialCapacity, loadFactor);
        resetExpireSchedule(corePoolSize, scheduleName, initialDelay, delay, timeUnit, fixRate);
    }

    public Cacher(CacherBuilder<K, V> c) {
        this(c.expireWayEnum, c.keepOldExpireWay, c.corePoolSize, c.scheduleName, c.initialDelay, c.delay, c.timeUnit, c.fixRate, c.initialCapacity, c.loadFactor, c.nullKey, c.showExpireTimeLog, c.showRemoveInfoLog, c.showLoadInfoLog, c.cacherValueLoader, c.expireTimeLoader, c.expireAction);
    }

    public void put(K key, V value) {
        if (key == null && nullKey != null) {
            key = nullKey;
        }
        put(key, value, null, this.expireWayEnum);
    }

    public void put(K key, V value, Long expireTime) {
        put(key, value, expireTime, this.expireWayEnum);
    }

    public void put(K key, V value, Long expireTime, ExpireWayEnum expireWayEnum) {
        put(key, new CacheObject<>(value, expireTime, expireWayEnum));
    }

    public void put(K key, CacheObject<V> cacheObject) {
        if (key == null) {
            MAP.put(nullKey, cacheObject);
        } else {
            MAP.put(key, cacheObject);
        }
    }

    public void set(K key, V value) {
        set(key, value, null);
    }

    public void set(K key, V value, Long expireTime) {
        set(key, value, expireTime, this.expireWayEnum);
    }

    public void set(K key, V value, Long expireTime, ExpireWayEnum expireWayEnum) {
        CacheObject<V> cacheObject = getCacheObjectPure(key);
        if (cacheObject == null) {
            put(key, value, expireTime, expireWayEnum);
        } else {
            cacheObject.setObj(value, expireTime, expireWayEnum);
        }
    }

    public V get(K key) {
        CacheObject<V> cacheObject = getCacheObject(key);
        if (cacheObject == null) {
            return null;
        }
        return cacheObject.getObjPure();
    }

    public V getIfAbsent(K key, V absentValue) {
        V v = get(key);
        return v == null ? absentValue : v;
    }

    public CacheObject<V> getCacheObject(K key) {
        CacheObject<V> cacheObjectPure = getCacheObjectPure(key);
        return cacheObjectPure == null ? null : cacheObjectPure.getCacheObject();
    }

    public CacheObject<V> getCacheObjectPure(K key) {
        if (key == null && this.nullKey != null) {
            // 如果允许 key 为 null 那么使用默认的 nullKey
            key = nullKey;
        }
        // 先获取 value
        CacheObject<V> cacheObject = MAP.get(key);
        // 判断 value 是否存在
        if (cacheObject != null) {
            // value 存在，则判断是否过期
            boolean expire = cacheObject.isExpire(this.expireWayEnum, this.keepOldExpireWay);
            if (!expire) {
                // 如果没有过期，那么直接返回
                return cacheObject;
            }
        }
        // 下面则是走 load 过程，要么是 value 不存在，要么是过期了

        if (cacheObject != null) {
            // 如果 value 存在，那么就一定是过期的，直接删除就行了
            removeReturnCacheObject(key, true);
            // 打印日志
            if (this.showRemoveInfoLog) {
                log.info("[{}] expire: key = {}, value = {}", this.scheduleName, key, cacheObject.getObjPure().toString());
            }
        }

        // 先 load 获取最新的 value
        CacheObject<V> load = load(key);
        if (load == null) {
            // 如果 load value 为空，那么直接返回就行了
            return null;
        }
        // 回填到 MAP
        put(key, load);
        return load;
    }

    public void resetExpireSchedule() {
        resetExpireSchedule(this.corePoolSize, this.scheduleName, this.initialDelay, this.delay, this.timeUnit, this.fixRate);
    }

    public void resetExpireSchedule(int corePoolSize, String scheduleName, long initialDelay, long delay, TimeUnit timeUnit, boolean fixRate) {
        this.corePoolSize = corePoolSize;
        this.scheduleName = scheduleName;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.timeUnit = timeUnit;
        this.fixRate = fixRate;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        scheduledExecutorService = new ScheduledThreadPoolExecutor(corePoolSize, r -> new Thread(r, scheduleName));
        if (fixRate) {
            scheduledExecutorService.scheduleAtFixedRate(this::expire, initialDelay, delay, timeUnit);
        } else {
            scheduledExecutorService.scheduleWithFixedDelay(this::expire, initialDelay, delay, timeUnit);
        }
    }

    public void shutdownExpireSchedule() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    public void shutdownExpireScheduleNow() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    public void expire() {
        if (this.showExpireTimeLog) {
            log.info("[" + this.scheduleName + "] begin clear expired...");
        }

        for (K key : MAP.keySet()) {
            getCacheObjectPure(key);
        }
    }

    /**
     * 直接删除缓存，不走 loader
     *
     * @param key key
     * @return 缓存对象
     */
    public V remove(K key) {
        return remove(key, false);
    }

    public V remove(K key, boolean useExpireAction) {
        CacheObject<V> cacheObject = removeReturnCacheObject(key, useExpireAction);
        return cacheObject == null ? null : cacheObject.getObj();
    }

    /**
     * 删除缓存
     * @param key key
     * @param useExpireAction 是否走 expireAction
     * @return CacheObject
     */
    public CacheObject<V> removeReturnCacheObject(K key, boolean useExpireAction) {
        CacheObject<V> removed = MAP.remove(key);
        if (removed != null && expireAction != null) {
            // 当缓存删除的时候，执行的操作
            expireAction.expireAction(key, removed, useExpireAction);
        }
        return removed;
    }

    private CacheObject<V> load(K key) {
        if (this.cacherValueLoader == null) {
            return null;
        }
        V value = cacherValueLoader.load(key);
        if (value == null) {
            if (this.showLoadInfoLog) {
                log.info("[{}] load no value, key = {}", this.scheduleName, key);
            }
            return null;
        }
        Long expireTime = expireTimeLoader == null ? null : expireTimeLoader.getLoadExpireTime(key);
        if (this.showLoadInfoLog) {
            log.info("[{}] load key = {}, expireTime = {}, value = {}", this.scheduleName, key, expireTime, value);
        }
        return new CacheObject<>(value, expireTime, this.expireWayEnum);
    }

    public void printAllValues() {
        printAllValues(System.out::println, ",");
    }

    public void printAllValues(Consumer<String> fun, String split) {
        Set<Map.Entry<K, CacheObject<V>>> entries = entrySet();
        StringBuilder info = new StringBuilder();
        for (Map.Entry<K, CacheObject<V>> kv : entries) {
            CacheObject<V> cacheObject = kv.getValue();
            info.append("{key=").append(kv.getKey()).append(", value=").append(cacheObject.getObjPure());
            if (cacheObject.isExpire(this.expireWayEnum, this.keepOldExpireWay)) {
                info.append(", expire");
            }
            info.append("}").append(split);
        }
        fun.accept(info.toString());
    }

    public int size() {
        return MAP.size();
    }

    public void clear() {
        MAP.clear();
    }

    public Set<K> keySet() {
        return MAP.keySet();
    }

    public Set<Map.Entry<K, CacheObject<V>>> entrySet() {
        return MAP.entrySet();
    }

    public ExpireWayEnum getExpireWayEnum() {
        return expireWayEnum;
    }

    public void setExpireWayEnum(ExpireWayEnum expireWayEnum) {
        this.expireWayEnum = expireWayEnum;
    }

    public boolean isKeepOldExpireWay() {
        return keepOldExpireWay;
    }

    public void switchToOldExpireWay() {
        this.keepOldExpireWay = true;
    }

    public void switchToSameExpireWay() {
        this.keepOldExpireWay = false;
    }

    public K getNullKey() {
        return nullKey;
    }

    public void setNullKey(K nullKey) {
        this.nullKey = nullKey;
    }

    public boolean isShowExpireTimeLog() {
        return showExpireTimeLog;
    }

    public void setShowExpireTimeLog(boolean showExpireTimeLog) {
        this.showExpireTimeLog = showExpireTimeLog;
    }

    public boolean isShowRemoveInfoLog() {
        return showRemoveInfoLog;
    }

    public void setShowRemoveInfoLog(boolean showRemoveInfoLog) {
        this.showRemoveInfoLog = showRemoveInfoLog;
    }

    public boolean isShowLoadInfoLog() {
        return showLoadInfoLog;
    }

    public void setShowLoadInfoLog(boolean showLoadInfoLog) {
        this.showLoadInfoLog = showLoadInfoLog;
    }

    public CacherValueLoader<K, V> getCacherValueLoader() {
        return cacherValueLoader;
    }

    public void setCacherValueLoader(CacherValueLoader<K, V> cacherValueLoader) {
        this.cacherValueLoader = cacherValueLoader;
    }

    public ExpireTimeLoader<K> getExpireTimeLoader() {
        return expireTimeLoader;
    }

    public void setExpireTimeLoader(ExpireTimeLoader<K> expireTimeLoader) {
        this.expireTimeLoader = expireTimeLoader;
    }

    public void setLoader(Long loadExpireTime, CacherValueLoader<K, V> cacherValueLoader) {
        setLoader(k -> loadExpireTime, cacherValueLoader);
    }

    public void setLoader(ExpireTimeLoader<K> expireTimeLoader, CacherValueLoader<K, V> cacherValueLoader) {
        this.expireTimeLoader = expireTimeLoader;
        this.cacherValueLoader = cacherValueLoader;
    }

    public void setLoader(ExpireTimeLoader<K> expireTimeLoader, CacherValueLoader<K, V> cacherValueLoader, ExpireAction<K, CacheObject<V>> expireAction) {
        this.expireTimeLoader = expireTimeLoader;
        this.cacherValueLoader = cacherValueLoader;
        this.expireAction = expireAction;
    }

    public ExpireAction<K, CacheObject<V>> getExpireAction() {
        return expireAction;
    }

    public void setExpireAction(ExpireAction<K, CacheObject<V>> expireAction) {
        this.expireAction = expireAction;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    private static String getRandomStr(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int random = (int) (Math.random() * 62);
            if (random < 10) {
                sb.append(random);
            } else if (random < 36) {
                sb.append((char) (random + 55));
            } else {
                sb.append((char) (random + 61));
            }
        }
        return sb.toString();
    }
}
