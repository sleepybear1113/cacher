package cn.sleepybear.cacher.loader;

/**
 * 这个是个接口，需要自己实现，当缓存过期时，自动加载新的缓存的时候，会调用这个接口，获取缓存的过期时间
 *
 * @author sleepybear
 * @date 2022/05/07 21:49
 */
@FunctionalInterface
public interface ExpireTimeLoader<K> {

    /**
     * 获取缓存的过期时间
     *
     * @param key key
     * @return 毫秒
     */
    Long getLoadExpireTime(K key);
}
