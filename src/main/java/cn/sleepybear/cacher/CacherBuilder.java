package cn.sleepybear.cacher;

import cn.sleepybear.cacher.cache.CacheObject;
import cn.sleepybear.cacher.cache.ExpireWayEnum;
import cn.sleepybear.cacher.loader.CacherValueLoader;
import cn.sleepybear.cacher.loader.ExpireAction;
import cn.sleepybear.cacher.loader.ExpireTimeLoader;

import java.util.concurrent.TimeUnit;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2022/05/04 23:55
 */
public class CacherBuilder<K, V> {
    static final int MAXIMUM_CAPACITY = 1 << 30;

    protected ExpireWayEnum expireWayEnum = ExpireWayEnum.AFTER_CREATE;

    /**
     * 旧缓存是否保持旧的过期方式
     */
    protected boolean keepOldExpireWay = true;

    protected int corePoolSize = 4;
    protected String scheduleName = "schedule-" + System.currentTimeMillis();
    protected long initialDelay = 0L;
    protected long delay = 60000L;
    protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    protected boolean fixRate = true;

    protected int initialCapacity = 64;
    protected float loadFactor = 0.75F;
    protected K nullKey;

    protected boolean showExpireTimeLog = false;
    protected boolean showRemoveInfoLog = false;
    protected boolean showLoadInfoLog = false;

    protected CacherValueLoader<K, V> cacherValueLoader = null;
    protected ExpireTimeLoader<K> expireTimeLoader = null;
    protected ExpireAction<K, CacheObject<V>> expireAction = null;

    public CacherBuilder() {
    }

    public CacherBuilder(CacherBuilder<K, V> copy) {
        this.expireWayEnum = copy.expireWayEnum;
        this.keepOldExpireWay = copy.keepOldExpireWay;
        this.corePoolSize = copy.corePoolSize;
        this.scheduleName = copy.scheduleName;
        this.initialDelay = copy.initialDelay;
        this.delay = copy.delay;
        this.timeUnit = copy.timeUnit;
        this.fixRate = copy.fixRate;
        this.initialCapacity = copy.initialCapacity;
        this.loadFactor = copy.loadFactor;
        this.nullKey = copy.nullKey;
        this.showExpireTimeLog = copy.showExpireTimeLog;
        this.showRemoveInfoLog = copy.showRemoveInfoLog;
        this.showLoadInfoLog = copy.showLoadInfoLog;
        this.cacherValueLoader = copy.cacherValueLoader;
        this.expireTimeLoader = copy.expireTimeLoader;
        this.expireAction = copy.expireAction;
    }

    public CacherBuilder<K, V> expireWay(ExpireWayEnum expireWayEnum) {
        if (expireWayEnum == null) {
            throw new IllegalArgumentException("expireWayEnum can not be null!");
        }
        this.expireWayEnum = expireWayEnum;
        return this;
    }

    public CacherBuilder<K, V> keepExpireWay(boolean old) {
        this.keepOldExpireWay = true;
        return this;
    }

    public CacherBuilder<K, V> corePoolSize(int corePoolSize) {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize <= 0!");
        }
        this.corePoolSize = corePoolSize;
        return this;
    }

    public CacherBuilder<K, V> scheduleName(String scheduleName) {
        if (scheduleName == null || scheduleName.isEmpty()) {
            throw new IllegalArgumentException("scheduleName can not be empty");
        }
        this.scheduleName = scheduleName;
        return this;
    }

    public CacherBuilder<K, V> initialDelay(long initialDelay) {
        if (initialDelay <= 0) {
            throw new IllegalArgumentException("initialDelay <= 0!");
        }
        this.initialDelay = initialDelay;
        return this;
    }

    public CacherBuilder<K, V> delay(long delay, TimeUnit timeUnit) {
        if (delay <= 0) {
            throw new IllegalArgumentException("delay <= 0!");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit can not be null");
        }
        this.delay = delay;
        this.timeUnit = timeUnit;
        return this;
    }

    public CacherBuilder<K, V> scheduleFixedWay(boolean fixRate) {
        this.fixRate = fixRate;
        return this;
    }

    public CacherBuilder<K, V> initialCapacity(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        this.initialCapacity = initialCapacity;
        return this;
    }

    public CacherBuilder<K, V> loadFactor(float loadFactor) {
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        this.loadFactor = loadFactor;
        return this;
    }

    public CacherBuilder<K, V> allowNullKey(K nullKey) {
        this.nullKey = nullKey;
        return this;
    }

    public CacherBuilder<K, V> showExpireTime(boolean showExpireTime) {
        this.showExpireTimeLog = showExpireTime;
        return this;
    }

    public CacherBuilder<K, V> showRemoveInfo(boolean showRemoveInfo) {
        this.showRemoveInfoLog = showRemoveInfo;
        return this;
    }

    public CacherBuilder<K, V> showLoadInfoLog(boolean showLoadInfoLog) {
        this.showLoadInfoLog = showLoadInfoLog;
        return this;
    }

    public CacherBuilder<K, V> showAllLogs() {
        this.showExpireTimeLog = true;
        this.showRemoveInfoLog = true;
        this.showLoadInfoLog = true;
        return this;
    }

    public CacherBuilder<K, V> cacherLoader(long loadExpireTime, CacherValueLoader<K, V> cacherValueLoader) {
        return cacherLoader(loadExpireTime, cacherValueLoader, null);
    }

    public CacherBuilder<K, V> cacherLoader(long loadExpireTime, CacherValueLoader<K, V> cacherValueLoader, ExpireAction<K, CacheObject<V>> expireAction) {
        return cacherLoader(k -> loadExpireTime, cacherValueLoader, expireAction);
    }

    public CacherBuilder<K, V> cacherLoader(ExpireTimeLoader<K> expireTimeLoader, CacherValueLoader<K, V> cacherValueLoader) {
        return cacherLoader(expireTimeLoader, cacherValueLoader, null);
    }

    public CacherBuilder<K, V> cacherLoader(ExpireTimeLoader<K> expireTimeLoader, CacherValueLoader<K, V> cacherValueLoader, ExpireAction<K, CacheObject<V>> expireAction) {
        this.expireTimeLoader = expireTimeLoader;
        this.cacherValueLoader = cacherValueLoader;
        this.expireAction = expireAction;
        return this;
    }

    public CacherBuilder<K, V> cacherLoader(ExpireTimeLoader<K> expireTimeLoader) {
        this.expireTimeLoader = expireTimeLoader;
        return this;
    }

    public CacherBuilder<K, V> cacherLoader(CacherValueLoader<K, V> cacherValueLoader) {
        this.cacherValueLoader = cacherValueLoader;
        return this;
    }

    public CacherBuilder<K, V> cacherLoader(ExpireAction<K, CacheObject<V>> expireAction) {
        this.expireAction = expireAction;
        return this;
    }

    public Cacher<K, V> build() {
        return new Cacher<>(expireWayEnum, keepOldExpireWay, corePoolSize, scheduleName, initialDelay, delay, timeUnit, fixRate, initialCapacity, loadFactor, nullKey, showExpireTimeLog, showRemoveInfoLog, showLoadInfoLog, cacherValueLoader, expireTimeLoader, expireAction);
    }
}
