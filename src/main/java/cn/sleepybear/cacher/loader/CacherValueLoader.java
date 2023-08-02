package cn.sleepybear.cacher.loader;

/**
 * 需要自行实现，当缓存过期时，自动加载新的缓存的时候，会调用这个接口，获取缓存的值，然后会将缓存的值放入缓存中
 *
 * @author sleepybear
 * @date 2022/05/07 11:25
 */
@FunctionalInterface
public interface CacherValueLoader<K, V> {

    /**
     * 加载缓存
     *
     * @param key key
     * @return value
     */
    V load(K key);
}
