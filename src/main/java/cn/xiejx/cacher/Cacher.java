package cn.xiejx.cacher;

import cn.xiejx.cacher.cache.CacheObject;
import cn.xiejx.cacher.cache.ExpireWayEnum;
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

    private boolean showExpireTime;
    private boolean showRemovedKey;
    private boolean showRemovedValue;

    public Cacher(ExpireWayEnum expireWayEnum, boolean keepOldExpireWay, int corePoolSize, String scheduleName, long initialDelay, long delay, TimeUnit timeUnit, boolean fixRate, int initialCapacity, float loadFactor, boolean showExpireTime, boolean showRemovedKey, boolean showRemovedValue) {
        this.expireWayEnum = expireWayEnum;
        this.keepOldExpireWay = keepOldExpireWay;
        this.showExpireTime = showExpireTime;
        this.showRemovedKey = showRemovedKey;
        this.showRemovedValue = showRemovedValue;
        MAP = new ConcurrentHashMap<>(initialCapacity, loadFactor);
        resetExpireSchedule(corePoolSize, scheduleName, initialDelay, delay, timeUnit, fixRate);
    }

    public void put(K key, V value) {
        put(key, value, null, this.expireWayEnum);
    }

    public void put(K key, V value, Long expireTime) {
        put(key, value, expireTime, this.expireWayEnum);
    }

    public void put(K key, V value, Long expireTime, ExpireWayEnum expireWayEnum) {
        put(key, new CacheObject<>(value, expireTime, expireWayEnum));
    }

    public void put(K key, CacheObject<V> cacheObject) {
        MAP.put(key, cacheObject);
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

    public CacheObject<V> getCacheObject(K key) {
        CacheObject<V> cacheObjectPure = getCacheObjectPure(key);
        return cacheObjectPure == null ? null : cacheObjectPure.getCacheObject();
    }

    public CacheObject<V> getCacheObjectPure(K key) {
        CacheObject<V> cacheObject = MAP.get(key);
        if (cacheObject == null) {
            return null;
        }
        boolean expire = cacheObject.isExpire(this.expireWayEnum, this.keepOldExpireWay);
        if (expire) {
            MAP.remove(key);

            StringBuilder info = new StringBuilder("expire: ");
            if (this.showRemovedKey) {
                info.append("[key = ").append(key).append("].");
            }
            if (this.showRemovedValue) {
                info.append("[value = ").append(cacheObject.getObjPure()).append("].");
            }
            if (this.showRemovedKey || this.showRemovedValue) {
                log.info("[" + this.scheduleName + "] " + info);
            }
            return null;
        }

        return cacheObject;
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
        if (this.showExpireTime) {
            log.info("[" + this.scheduleName + "] begin clear expired...");
        }

        for (K key : MAP.keySet()) {
            getCacheObjectPure(key);
        }
    }

    public V remove(K key) {
        CacheObject<V> cacheObject = removeReturnCacheObject(key);
        return cacheObject == null ? null : cacheObject.getObj();
    }

    public CacheObject<V> removeReturnCacheObject(K key) {
        return MAP.remove(key);
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

    public boolean isShowExpireTime() {
        return showExpireTime;
    }

    public void setShowExpireTime(boolean showExpireTime) {
        this.showExpireTime = showExpireTime;
    }

    public boolean isShowRemovedKey() {
        return showRemovedKey;
    }

    public void setShowRemovedKey(boolean showRemovedKey) {
        this.showRemovedKey = showRemovedKey;
    }

    public boolean isShowRemovedValue() {
        return showRemovedValue;
    }

    public void setShowRemovedValue(boolean showRemovedValue) {
        this.showRemovedValue = showRemovedValue;
    }

    public String getScheduleName() {
        return scheduleName;
    }
}
