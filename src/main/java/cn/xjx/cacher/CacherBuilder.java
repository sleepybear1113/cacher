package cn.xjx.cacher;

import cn.xjx.cacher.cache.ExpireWayEnum;

import java.util.concurrent.TimeUnit;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2022/05/04 23:55
 */
public class CacherBuilder {
    static final int MAXIMUM_CAPACITY = 1 << 30;


    private ExpireWayEnum expireWayEnum = ExpireWayEnum.AFTER_CREATE;

    /**
     * 旧缓存是否保持旧的过期方式
     */
    private boolean keepOldExpireWay = true;

    private int corePoolSize = 4;
    private String scheduleName = "schedule-" + System.currentTimeMillis();
    private long initialDelay = 0L;
    private long delay = 60000L;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private boolean fixRate = true;

    private int initialCapacity = 64;
    private float loadFactor = 0.75F;

    private boolean showExpireTime = false;
    private boolean showRemovedKey = false;
    private boolean showRemovedValue = false;

    public CacherBuilder() {
    }

    public CacherBuilder(CacherBuilder copy) {
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
        this.showExpireTime = copy.showExpireTime;
        this.showRemovedKey = copy.showRemovedKey;
        this.showRemovedValue = copy.showRemovedValue;
    }

    public CacherBuilder expireWay(ExpireWayEnum expireWayEnum) {
        if (expireWayEnum == null) {
            throw new IllegalArgumentException("expireWayEnum can not be null!");
        }
        this.expireWayEnum = expireWayEnum;
        return this;
    }

    public CacherBuilder keepExpireWay(boolean old) {
        this.keepOldExpireWay = true;
        return this;
    }

    public CacherBuilder corePoolSize(int corePoolSize) {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize <= 0!");
        }
        this.corePoolSize = corePoolSize;
        return this;
    }

    public CacherBuilder scheduleName(String scheduleName) {
        if (scheduleName == null || scheduleName.length() == 0) {
            throw new IllegalArgumentException("scheduleName can not be empty");
        }
        this.scheduleName = scheduleName;
        return this;
    }

    public CacherBuilder initialDelay(long initialDelay) {
        if (initialDelay <= 0) {
            throw new IllegalArgumentException("initialDelay <= 0!");
        }
        this.initialDelay = initialDelay;
        return this;
    }

    public CacherBuilder delay(long delay, TimeUnit timeUnit) {
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

    public CacherBuilder scheduleFixedWay(boolean fixRate) {
        this.fixRate = fixRate;
        return this;
    }

    public CacherBuilder initialCapacity(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        this.initialCapacity = initialCapacity;
        return this;
    }

    public CacherBuilder loadFactor(float loadFactor) {
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        this.loadFactor = loadFactor;
        return this;
    }

    public CacherBuilder showExpireTime(boolean showExpireTime) {
        this.showExpireTime = showExpireTime;
        return this;
    }

    public CacherBuilder showRemovedKey(boolean showRemovedKey) {
        this.showRemovedKey = showRemovedKey;
        return this;
    }

    public CacherBuilder showRemovedValue(boolean showRemovedValue) {
        this.showRemovedValue = showRemovedValue;
        return this;
    }

    public CacherBuilder showAllLogs() {
        this.showExpireTime = true;
        this.showRemovedKey = true;
        this.showRemovedValue = true;
        return this;
    }

    public <K, V> Cacher<K, V> build() {
        return new Cacher<>(expireWayEnum, keepOldExpireWay, corePoolSize, scheduleName, initialDelay, delay, timeUnit, fixRate, initialCapacity, loadFactor, showExpireTime, showRemovedKey, showRemovedValue);
    }
}
